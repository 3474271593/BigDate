// 文件路径：src/main/java/org/example/process/TradeChangeProcessFunction.java
package org.example.process;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeDelta;

import java.util.List;

/**
 * DWS订单状态变化处理函数。
 *
 * 核心职责：
 *
 * 1. 按orderId维护上一版本订单状态；
 * 2. 比较previous和current；
 * 3. 识别真正的业务变化；
 * 4. 输出TradeDelta业务事件；
 * 5. 更新ValueState。
 *
 * 当前支持：
 *
 * 新订单：
 *      ↓
 * ORDER_CREATE
 *
 * 历史首次出现且已经支付：
 *      ↓
 * ORDER_CREATE
 * ORDER_PAY
 *
 * 未支付 -> 已支付：
 *      ↓
 * ORDER_PAY
 *
 * 普通UPDATE：
 *      ↓
 * 不输出事件
 */
public class TradeChangeProcessFunction
        extends KeyedProcessFunction<Long, DwdOrderInfo, TradeDelta> {

    /**
     * 保存当前orderId上一版本订单。
     *
     * 因为上游已经：
     *
     * keyBy(DwdOrderInfo::getOrderId)
     *
     * 所以每一个orderId拥有独立的ValueState。
     */
    private transient ValueState<DwdOrderInfo> previousOrderState;

    @Override
    public void open(Configuration parameters) {

        ValueStateDescriptor<DwdOrderInfo> descriptor =
                new ValueStateDescriptor<>(
                        "previous-order-state",
                        DwdOrderInfo.class
                );

        previousOrderState =
                getRuntimeContext().getState(descriptor);
    }

    @Override
    public void processElement(
            DwdOrderInfo current,
            Context context,
            Collector<TradeDelta> out
    ) throws Exception {

        /*
         * 基础保护。
         *
         * 正常情况下App前面的filter已经过滤，
         * 这里再次保护状态算子。
         */
        if (current == null || current.getOrderId() == null) {
            return;
        }

        // 读取当前orderId的上一版本状态。
        DwdOrderInfo previous =
                previousOrderState.value();

        /*
         * 根据previous/current计算真正的业务变化事件。
         *
         * 返回值可能是：
         *
         * []
         *
         * [ORDER_CREATE]
         *
         * [ORDER_PAY]
         *
         * [ORDER_CREATE, ORDER_PAY]
         */
        List<TradeDelta> deltas =
                TradeDeltaCalculator.calculate(
                        previous,
                        current
                );

        /*
         * 只输出真正有业务意义的事件。
         *
         * 与旧版不同：
         *
         * 修改userAddress之类的普通UPDATE，
         * 不再输出：
         *
         * TradeSummary{0,0,0}
         *
         * 而是完全不向下游发送数据。
         */
        for (TradeDelta delta : deltas) {

            if (delta != null) {
                out.collect(delta);
            }
        }

        /*
         * 无论本次有没有产生业务指标变化，
         * 都必须更新订单状态。
         *
         * 这样下一条相同orderId的CDC记录到达时，
         * 才能与最新版本比较。
         */
        previousOrderState.update(current);
    }
}