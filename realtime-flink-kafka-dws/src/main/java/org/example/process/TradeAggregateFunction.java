// 文件路径：src/main/java/org/example/process/TradeAggregateFunction.java
package org.example.process;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.example.bean.TradeDelta;
import org.example.bean.TradeSummary;

import java.math.BigDecimal;

/**
 * DWS交易主题增量聚合函数。
 *
 * 输入：
 * TradeDelta
 *
 * 累加器：
 * TradeSummary
 *
 * 输出：
 * TradeSummary
 *
 * 注意：
 * 本函数不再直接处理DwdOrderInfo。
 *
 * CDC重复问题已经在上游：
 *
 * TradeChangeProcessFunction
 *
 * 中通过ValueState解决。
 *
 * 因此这里收到的TradeDelta都是已经识别后的
 * 有效业务指标变化。
 */
public class TradeAggregateFunction
        implements AggregateFunction<TradeDelta, TradeSummary, TradeSummary> {

    /**
     * 创建窗口累加器。
     */
    @Override
    public TradeSummary createAccumulator() {

        TradeSummary accumulator =
                new TradeSummary();

        accumulator.setOrderCount(0L);
        accumulator.setPaidOrderCount(0L);
        accumulator.setPaidAmount(BigDecimal.ZERO);

        return accumulator;
    }

    /**
     * 将一条TradeDelta累加到窗口指标。
     */
    @Override
    public TradeSummary add(
            TradeDelta delta,
            TradeSummary accumulator
    ) {

        if (delta == null) {
            return accumulator;
        }

        /*
         * 累加订单变化量。
         */
        accumulator.setOrderCount(
                accumulator.getOrderCount()
                        + delta.getOrderCountDelta()
        );

        /*
         * 累加支付订单变化量。
         */
        accumulator.setPaidOrderCount(
                accumulator.getPaidOrderCount()
                        + delta.getPaidOrderCountDelta()
        );

        /*
         * 累加支付金额变化量。
         */
        BigDecimal currentAmount =
                accumulator.getPaidAmount() == null
                        ? BigDecimal.ZERO
                        : accumulator.getPaidAmount();

        BigDecimal deltaAmount =
                delta.getPaidAmountDelta() == null
                        ? BigDecimal.ZERO
                        : delta.getPaidAmountDelta();

        accumulator.setPaidAmount(
                currentAmount.add(deltaAmount)
        );

        return accumulator;
    }

    /**
     * 返回最终累加结果。
     */
    @Override
    public TradeSummary getResult(
            TradeSummary accumulator
    ) {

        return accumulator;
    }

    /**
     * 合并两个累加器。
     *
     * 当前滚动窗口通常不会直接使用这个方法，
     * 但AggregateFunction接口要求实现。
     */
    @Override
    public TradeSummary merge(
            TradeSummary a,
            TradeSummary b
    ) {

        TradeSummary result =
                new TradeSummary();

        result.setOrderCount(
                a.getOrderCount()
                        + b.getOrderCount()
        );

        result.setPaidOrderCount(
                a.getPaidOrderCount()
                        + b.getPaidOrderCount()
        );

        BigDecimal amountA =
                a.getPaidAmount() == null
                        ? BigDecimal.ZERO
                        : a.getPaidAmount();

        BigDecimal amountB =
                b.getPaidAmount() == null
                        ? BigDecimal.ZERO
                        : b.getPaidAmount();

        result.setPaidAmount(
                amountA.add(amountB)
        );

        return result;
    }
}