// 文件路径：src/main/java/org/example/process/GoodsDeltaCalculator.java
package org.example.process;

import org.example.bean.DwdOrderGoods;
import org.example.bean.DwdOrderInfo;
import org.example.bean.GoodsDelta;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 商品主题 Delta 计算器。
 *
 * 负责把：
 *
 * DwdOrderInfo + DwdOrderGoods
 *
 * 转换为商品主题的指标变化量。
 *
 * 当前支持三类事件：
 *
 * 1. GOODS_ORDER
 *    商品下单事件
 *    Event Time = order_info.createTime
 *
 * 2. GOODS_PAY
 *    商品支付事件
 *    Event Time = order_info.payTime
 *
 * 3. GOODS_UPDATE_CORRECTION
 *    商品明细 UPDATE 修正事件
 *    Event Time = order_goods.processTs
 *
 * 注意：
 *
 * processTs 只用于商品明细修正事件。
 * 不允许使用 processTs 替代订单 createTime / payTime。
 */
public class GoodsDeltaCalculator {

    private static final TimeZone BUSINESS_TIME_ZONE =
            TimeZone.getTimeZone("Asia/Shanghai");

    private static final String TIME_PATTERN =
            "yyyy-MM-dd HH:mm:ss";

    /**
     * 创建商品下单 Delta。
     *
     * 一个 order_goods 明细第一次参与统计时：
     *
     * orderCountDelta = 1
     * orderGoodsNumDelta = goodsNum
     * orderAmountDelta = goodsAmount
     *
     * 支付指标全部为0。
     */
    public GoodsDelta createOrderDelta(
            DwdOrderInfo order,
            DwdOrderGoods goods) {

        if (!isValidJoin(order, goods)) {
            return null;
        }

        Long eventTs =
                parseBusinessTime(
                        order.getCreateTime()
                );

        if (eventTs == null) {
            return null;
        }

        GoodsDelta delta =
                createBaseDelta(
                        order,
                        goods
                );

        delta.setOrderCountDelta(1L);

        delta.setOrderGoodsNumDelta(
                safeLong(
                        goods.getGoodsNum()
                )
        );

        delta.setOrderAmountDelta(
                safeDecimal(
                        goods.getGoodsAmount()
                )
        );

        delta.setPaidOrderCountDelta(0L);
        delta.setPaidGoodsNumDelta(0L);
        delta.setPaidAmountDelta(BigDecimal.ZERO);

        delta.setEventType(
                "GOODS_ORDER"
        );

        delta.setEventTime(
                order.getCreateTime()
        );

        delta.setEventTs(
                eventTs
        );

        return delta;
    }

    /**
     * 创建商品支付 Delta。
     *
     * 只有：
     *
     * isPay = 1
     * 并且 payTime 合法
     *
     * 才产生 GOODS_PAY。
     *
     * 下单指标全部为0。
     */
    public GoodsDelta createPayDelta(
            DwdOrderInfo order,
            DwdOrderGoods goods) {

        if (!isValidJoin(order, goods)) {
            return null;
        }

        if (!Integer.valueOf(1).equals(
                order.getIsPay()
        )) {
            return null;
        }

        Long eventTs =
                parseBusinessTime(
                        order.getPayTime()
                );

        if (eventTs == null) {
            return null;
        }

        GoodsDelta delta =
                createBaseDelta(
                        order,
                        goods
                );

        delta.setOrderCountDelta(0L);
        delta.setOrderGoodsNumDelta(0L);
        delta.setOrderAmountDelta(BigDecimal.ZERO);

        delta.setPaidOrderCountDelta(1L);

        delta.setPaidGoodsNumDelta(
                safeLong(
                        goods.getGoodsNum()
                )
        );

        delta.setPaidAmountDelta(
                safeDecimal(
                        goods.getGoodsAmount()
                )
        );

        delta.setEventType(
                "GOODS_PAY"
        );

        delta.setEventTime(
                order.getPayTime()
        );

        delta.setEventTs(
                eventTs
        );

        return delta;
    }

    /**
     * 创建商品明细 UPDATE 修正 Delta。
     *
     * 例如：
     *
     * old:
     * goodsNum = 1
     * goodsAmount = 11.60
     *
     * current:
     * goodsNum = 3
     * goodsAmount = 34.80
     *
     * correction:
     * orderGoodsNumDelta = +2
     * orderAmountDelta = +23.20
     *
     * 如果订单已经支付：
     *
     * paidGoodsNumDelta = +2
     * paidAmountDelta = +23.20
     *
     * UPDATE 不代表新增订单，因此：
     *
     * orderCountDelta = 0
     * paidOrderCountDelta = 0
     *
     * 当前源表 order_goods 没有业务 updateTime，
     * 因此修正事件使用 current.processTs。
     */
    public GoodsDelta createCorrectionDelta(
            DwdOrderInfo order,
            DwdOrderGoods previous,
            DwdOrderGoods current) {

        if (order == null
                || previous == null
                || current == null) {

            return null;
        }

        if (order.getOrderId() == null
                || previous.getOrderId() == null
                || current.getOrderId() == null
                || previous.getOgId() == null
                || current.getOgId() == null
                || previous.getGoodsId() == null
                || current.getGoodsId() == null) {

            return null;
        }

        /*
         * 三者必须属于同一个订单。
         */
        if (!order.getOrderId().equals(
                previous.getOrderId()
        )) {
            return null;
        }

        if (!order.getOrderId().equals(
                current.getOrderId()
        )) {
            return null;
        }

        /*
         * Correction 必须针对同一个 order_goods 明细。
         */
        if (!previous.getOgId().equals(
                current.getOgId()
        )) {
            return null;
        }

        /*
         * 当前第一版商品模型要求：
         *
         * 同一个 ogId 的 goodsId 不发生迁移。
         *
         * 我们已经对当前 Kafka 历史数据进行了检查，
         * 没有发现同一 ogId 对应多个 goodsId。
         *
         * 如果未来出现 goodsId 变化，
         * 应采用：
         *
         * 旧商品撤销 + 新商品增加
         *
         * 而不是简单差值修正。
         */
        if (!previous.getGoodsId().equals(
                current.getGoodsId()
        )) {
            return null;
        }

        if (current.getProcessTs() == null
                || current.getProcessTs() <= 0L) {

            return null;
        }

        long goodsNumDelta =
                safeLong(
                        current.getGoodsNum()
                )
                        -
                        safeLong(
                                previous.getGoodsNum()
                        );

        BigDecimal amountDelta =
                safeDecimal(
                        current.getGoodsAmount()
                ).subtract(
                        safeDecimal(
                                previous.getGoodsAmount()
                        )
                );

        /*
         * 数量和金额都没有发生变化，
         * 例如仅 goodsName 被维度补全，
         * 不产生指标修正事件。
         */
        if (goodsNumDelta == 0L
                && amountDelta.compareTo(
                BigDecimal.ZERO
        ) == 0) {

            return null;
        }

        GoodsDelta delta =
                createBaseDelta(
                        order,
                        current
                );

        /*
         * UPDATE 不是新增订单。
         */
        delta.setOrderCountDelta(0L);

        delta.setOrderGoodsNumDelta(
                goodsNumDelta
        );

        delta.setOrderAmountDelta(
                amountDelta
        );

        /*
         * 如果订单已经支付，
         * 商品数量/金额的变化同时修正支付指标。
         *
         * 但是 paidOrderCountDelta 仍然是0，
         * 因为这不是一次新的支付。
         */
        if (Integer.valueOf(1).equals(
                order.getIsPay()
        )) {

            delta.setPaidOrderCountDelta(0L);

            delta.setPaidGoodsNumDelta(
                    goodsNumDelta
            );

            delta.setPaidAmountDelta(
                    amountDelta
            );

        } else {

            delta.setPaidOrderCountDelta(0L);
            delta.setPaidGoodsNumDelta(0L);
            delta.setPaidAmountDelta(BigDecimal.ZERO);
        }

        delta.setEventType(
                "GOODS_UPDATE_CORRECTION"
        );

        /*
         * 源表没有 updateTime。
         *
         * 因此 Correction 使用 DWD processTs。
         */
        delta.setEventTs(
                current.getProcessTs()
        );

        delta.setEventTime(
                formatBusinessTime(
                        current.getProcessTs()
                )
        );

        return delta;
    }

    /**
     * 创建公共字段。
     */
    private GoodsDelta createBaseDelta(
            DwdOrderInfo order,
            DwdOrderGoods goods) {

        GoodsDelta delta =
                new GoodsDelta();

        delta.setOrderId(
                order.getOrderId()
        );

        delta.setOgId(
                goods.getOgId()
        );

        delta.setGoodsId(
                goods.getGoodsId()
        );

        delta.setGoodsName(
                goods.getGoodsName()
        );

        delta.setShopId(
                order.getShopId()
        );

        delta.setUserId(
                order.getUserId()
        );

        return delta;
    }

    /**
     * 校验 order_info 与 order_goods
     * 是否可以进行关联。
     */
    private boolean isValidJoin(
            DwdOrderInfo order,
            DwdOrderGoods goods) {

        if (order == null
                || goods == null) {

            return false;
        }

        if (order.getOrderId() == null
                || goods.getOrderId() == null
                || goods.getOgId() == null
                || goods.getGoodsId() == null) {

            return false;
        }

        return order.getOrderId().equals(
                goods.getOrderId()
        );
    }

    /**
     * Integer -> long。
     *
     * goodsNum 当前 Bean 为 Integer。
     */
    private long safeLong(
            Integer value) {

        return value == null
                ? 0L
                : value.longValue();
    }

    /**
     * BigDecimal null 安全处理。
     */
    private BigDecimal safeDecimal(
            BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    /**
     * 解析业务时间。
     *
     * Asia/Shanghai
     * yyyy-MM-dd HH:mm:ss
     */
    private Long parseBusinessTime(
            String time) {

        if (time == null
                || time.trim().isEmpty()) {

            return null;
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        TIME_PATTERN
                );

        formatter.setLenient(false);

        formatter.setTimeZone(
                BUSINESS_TIME_ZONE
        );

        try {

            Date date =
                    formatter.parse(
                            time
                    );

            return date.getTime();

        } catch (ParseException e) {

            return null;
        }
    }

    /**
     * processTs -> 可读时间。
     *
     * 主要用于 GOODS_UPDATE_CORRECTION。
     */
    private String formatBusinessTime(
            Long timestamp) {

        if (timestamp == null
                || timestamp <= 0L) {

            return null;
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        TIME_PATTERN
                );

        formatter.setTimeZone(
                BUSINESS_TIME_ZONE
        );

        return formatter.format(
                new Date(timestamp)
        );
    }
}