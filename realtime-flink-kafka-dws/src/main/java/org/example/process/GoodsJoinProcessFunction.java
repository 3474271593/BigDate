package org.example.process;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;
import org.example.bean.DwdOrderGoods;
import org.example.bean.DwdOrderInfo;
import org.example.bean.GoodsDelta;

/**
 * 商品主题订单 + 商品明细关联处理。
 *
 * 两条输入流都按照 orderId KeyBy：
 *
 * input1：DwdOrderInfo
 * input2：DwdOrderGoods
 *
 * 主要解决：
 *
 * 1. order_info 先到，order_goods 后到；
 * 2. order_goods 先到，order_info 后到；
 * 3. 一个订单包含多个 order_goods；
 * 4. 未支付 -> 已支付；
 * 5. 第一次看到订单时已经支付；
 * 6. 重复订单 UPDATE 不重复统计；
 * 7. 重复 order_goods 不重复统计；
 * 8. order_goods 数量/金额 UPDATE 时产生差值修正。
 *
 * 商品 UPDATE 修正规则：
 *
 * previous:
 * goodsNum = 1
 * goodsAmount = 11.60
 *
 * current:
 * goodsNum = 3
 * goodsAmount = 34.80
 *
 * 输出：
 *
 * GOODS_UPDATE_CORRECTION
 * orderGoodsNumDelta = +2
 * orderAmountDelta = +23.20
 *
 * 如果订单已经支付：
 *
 * paidGoodsNumDelta = +2
 * paidAmountDelta = +23.20
 *
 * 注意：
 *
 * orderCountDelta = 0
 * paidOrderCountDelta = 0
 *
 * 因为 UPDATE 并没有新增订单或新增支付。
 */
public class GoodsJoinProcessFunction
        extends KeyedCoProcessFunction<
        Long,
        DwdOrderInfo,
        DwdOrderGoods,
        GoodsDelta> {

    private final GoodsRuntimeMode runtimeMode;

    /**
     * 当前 orderId 对应的最新订单状态。
     */
    private transient ValueState<DwdOrderInfo>
            orderInfoState;

    /**
     * 当前订单下面每个 ogId 的最新商品状态。
     *
     * key：
     * ogId
     *
     * value：
     * DwdOrderGoods
     *
     * 这个 State 不只是用于等待 Join，
     * 同时用于保存商品上一版本，
     * 从而计算：
     *
     * previous -> current
     *
     * 的 UPDATE 差值。
     */
    private transient MapState<Long, DwdOrderGoods>
            orderGoodsState;

    /**
     * 记录某个 ogId 是否已经产生 GOODS_ORDER。
     *
     * 防止订单普通 UPDATE、
     * 商品重复消息等情况造成重复下单统计。
     */
    private transient MapState<Long, Boolean>
            orderEmittedState;

    /**
     * 记录某个 ogId 是否已经产生 GOODS_PAY。
     *
     * 防止订单重复 UPDATE 导致重复支付统计。
     */
    private transient MapState<Long, Boolean>
            payEmittedState;

    /**
     * 商品 Delta 纯业务计算器。
     */
    private transient GoodsDeltaCalculator calculator;

    public GoodsJoinProcessFunction() {
        this(GoodsRuntimeMode.REALTIME);
    }

    public GoodsJoinProcessFunction(
            GoodsRuntimeMode runtimeMode) {

        if (runtimeMode == null) {
            throw new IllegalArgumentException(
                    "runtimeMode must not be null"
            );
        }
        this.runtimeMode = runtimeMode;
    }

    @Override
    public void open(
            Configuration parameters)
            throws Exception {

        super.open(parameters);

        /*
         * 保存最新订单状态。
         */
        ValueStateDescriptor<DwdOrderInfo>
                orderDescriptor =
                new ValueStateDescriptor<>(
                        "goods-order-info-state",
                        DwdOrderInfo.class
                );

        orderInfoState =
                getRuntimeContext()
                        .getState(
                                orderDescriptor
                        );

        /*
         * 保存每个 ogId 最新的商品状态。
         *
         * 注意：
         * UPDATE 时必须先读取 previous，
         * 计算完成以后才能覆盖为 current。
         */
        MapStateDescriptor<Long, DwdOrderGoods>
                goodsDescriptor =
                new MapStateDescriptor<>(
                        "goods-order-goods-state",
                        Long.class,
                        DwdOrderGoods.class
                );

        orderGoodsState =
                getRuntimeContext()
                        .getMapState(
                                goodsDescriptor
                        );

        /*
         * 下单事件防重 State。
         */
        MapStateDescriptor<Long, Boolean>
                orderEmittedDescriptor =
                new MapStateDescriptor<>(
                        "goods-order-emitted-state",
                        Long.class,
                        Boolean.class
                );

        orderEmittedState =
                getRuntimeContext()
                        .getMapState(
                                orderEmittedDescriptor
                        );

        /*
         * 支付事件防重 State。
         */
        MapStateDescriptor<Long, Boolean>
                payEmittedDescriptor =
                new MapStateDescriptor<>(
                        "goods-pay-emitted-state",
                        Long.class,
                        Boolean.class
                );

        payEmittedState =
                getRuntimeContext()
                        .getMapState(
                                payEmittedDescriptor
                        );

        calculator =
                new GoodsDeltaCalculator();
    }

    /**
     * input1：
     *
     * DwdOrderInfo
     */
    @Override
    public void processElement1(
            DwdOrderInfo order,
            Context ctx,
            Collector<GoodsDelta> out)
            throws Exception {

        if (order == null
                || order.getOrderId() == null) {

            return;
        }

        if (runtimeMode == GoodsRuntimeMode.WARMUP) {
            warmupOrder(order);
            return;
        }

        /*
         * 更新订单最新状态。
         *
         * 例如：
         *
         * 第一次：
         * isPay = 0
         *
         * 后续：
         * isPay = 1
         */
        orderInfoState.update(
                order
        );

        /*
         * 如果 order_goods 比 order_info 更早到，
         * 商品已经保存在 orderGoodsState。
         *
         * 此时订单一到，
         * 对所有等待中的商品执行事件判断。
         */
        Iterable<DwdOrderGoods> goodsValues =
                orderGoodsState.values();

        if (goodsValues == null) {
            return;
        }

        for (DwdOrderGoods goods : goodsValues) {

            if (goods == null) {
                continue;
            }

            emitInitialEventsIfNecessary(
                    order,
                    goods,
                    out
            );
        }
    }

    /**
     * input2：
     *
     * DwdOrderGoods
     */
    @Override
    public void processElement2(
            DwdOrderGoods current,
            Context ctx,
            Collector<GoodsDelta> out)
            throws Exception {

        if (current == null
                || current.getOrderId() == null
                || current.getOgId() == null
                || current.getGoodsId() == null) {

            return;
        }

        if (runtimeMode == GoodsRuntimeMode.WARMUP) {
            warmupGoods(current);
            return;
        }

        /*
         * 关键：
         *
         * 一定要在覆盖 State 之前读取 previous。
         *
         * 如果先：
         *
         * orderGoodsState.put(ogId, current)
         *
         * 再 get，
         * previous 就丢失了，
         * 无法计算 UPDATE 差值。
         */
        DwdOrderGoods previous =
                orderGoodsState.get(
                        current.getOgId()
                );

        DwdOrderInfo order =
                orderInfoState.value();

        /*
         * previous == null：
         *
         * 说明这是当前 State 第一次看到这个 ogId。
         *
         * 如果订单已经存在，
         * 可以立即产生 GOODS_ORDER / GOODS_PAY。
         *
         * 如果订单还没到，
         * 暂时只保存商品，
         * 等 processElement1() 中完成关联。
         */
        if (previous == null) {

            if (order != null) {

                emitInitialEventsIfNecessary(
                        order,
                        current,
                        out
                );
            }

            /*
             * 第一次商品版本保存进 State。
             */
            orderGoodsState.put(
                    current.getOgId(),
                    current
            );

            return;
        }

        /*
         * previous != null：
         *
         * 说明同一个 ogId 已经出现过。
         *
         * 这里可能是：
         *
         * 1. 完全重复消息；
         * 2. goodsName 等非指标字段变化；
         * 3. goodsNum / goodsAmount 发生 UPDATE。
         *
         * 如果订单已经存在，
         * 尝试产生 Correction。
         */
        if (order != null) {

            /*
             * 防御性处理：
             *
             * 如果之前因为到达顺序问题，
             * 初始 GOODS_ORDER / GOODS_PAY
             * 还没有产生，
             * 先补齐初始事件。
             *
             * 注意这里使用 previous，
             * 因为 previous 才代表已经进入 State
             * 的上一版本商品事实。
             */
            emitInitialEventsIfNecessary(
                    order,
                    previous,
                    out
            );

            /*
             * 只有初始下单事件已经成功产生以后，
             * 才允许对该商品做 Correction。
             *
             * 否则可能出现：
             *
             * 只有 +差值
             * 却没有原始商品事实
             *
             * 导致指标不完整。
             */
            if (Boolean.TRUE.equals(
                    orderEmittedState.get(
                            current.getOgId()
                    )
            )) {

                GoodsDelta correction =
                        calculator.createCorrectionDelta(
                                order,
                                previous,
                                current
                        );

                if (correction != null) {

                    out.collect(
                            correction
                    );
                }
            }
        }

        /*
         * 所有 previous -> current 的计算完成以后，
         * 最后才能覆盖 State。
         */
        orderGoodsState.put(
                current.getOgId(),
                current
        );
    }

    /**
     * Rebuilds the original four Join states without emitting GoodsDelta.
     */
    private void warmupOrder(
            DwdOrderInfo order)
            throws Exception {

        orderInfoState.update(order);

        Iterable<DwdOrderGoods> goodsValues =
                orderGoodsState.values();

        if (goodsValues == null) {
            return;
        }

        for (DwdOrderGoods goods : goodsValues) {
            markBootstrapEvents(order, goods);
        }
    }

    /**
     * Keeps the newest historical version of each ogId and restores the
     * emitted flags represented by the Bootstrap baseline.
     */
    private void warmupGoods(
            DwdOrderGoods goods)
            throws Exception {

        orderGoodsState.put(goods.getOgId(), goods);

        DwdOrderInfo order = orderInfoState.value();
        if (order != null) {
            markBootstrapEvents(order, goods);
        }
    }

    private void markBootstrapEvents(
            DwdOrderInfo order,
            DwdOrderGoods goods)
            throws Exception {

        if (order == null
                || goods == null
                || goods.getOgId() == null) {
            return;
        }

        if (calculator.createOrderDelta(order, goods) != null) {
            orderEmittedState.put(goods.getOgId(), true);
        }

        if (calculator.createPayDelta(order, goods) != null) {
            payEmittedState.put(goods.getOgId(), true);
        }
    }

    /**
     * 根据当前订单状态，
     * 为一个商品明细补齐初始事件。
     *
     * 可能产生：
     *
     * GOODS_ORDER
     *
     * 或：
     *
     * GOODS_ORDER
     * GOODS_PAY
     */
    private void emitInitialEventsIfNecessary(
            DwdOrderInfo order,
            DwdOrderGoods goods,
            Collector<GoodsDelta> out)
            throws Exception {

        if (order == null
                || goods == null
                || goods.getOgId() == null) {

            return;
        }

        Long ogId =
                goods.getOgId();

        /*
         * 第一次成功产生 GOODS_ORDER。
         */
        Boolean orderEmitted =
                orderEmittedState.get(
                        ogId
                );

        if (!Boolean.TRUE.equals(
                orderEmitted
        )) {

            GoodsDelta orderDelta =
                    calculator.createOrderDelta(
                            order,
                            goods
                    );

            if (orderDelta != null) {

                out.collect(
                        orderDelta
                );

                /*
                 * 只有真正成功输出以后，
                 * 才标记为已产生。
                 */
                orderEmittedState.put(
                        ogId,
                        true
                );
            }
        }

        /*
         * 第一次成功产生 GOODS_PAY。
         *
         * createPayDelta 内部会判断：
         *
         * isPay == 1
         * payTime 合法
         */
        Boolean payEmitted =
                payEmittedState.get(
                        ogId
                );

        if (!Boolean.TRUE.equals(
                payEmitted
        )) {

            GoodsDelta payDelta =
                    calculator.createPayDelta(
                            order,
                            goods
                    );

            if (payDelta != null) {

                out.collect(
                        payDelta
                );

                /*
                 * 只有真正输出支付事件后，
                 * 才标记。
                 */
                payEmittedState.put(
                        ogId,
                        true
                );
            }
        }
    }
}
