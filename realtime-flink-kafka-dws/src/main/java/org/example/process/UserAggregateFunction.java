package org.example.process;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.example.bean.UserAccumulator;
import org.example.bean.UserDelta;

/**
 * User DWS 用户主题增量聚合函数。
 *
 * 输入：
 * UserDelta
 *
 * 累加器：
 * UserAccumulator
 *
 * 输出：
 * UserAccumulator
 *
 * 窗口粒度由外部 UserApp 决定：
 *
 * userId + 1分钟
 *
 * 当前 V1 指标：
 *
 * orderCount
 * orderShopCount
 * orderAmount
 * paidOrderCount
 * paidShopCount
 * paidAmount
 */
public class UserAggregateFunction
        implements AggregateFunction<
        UserDelta,
        UserAccumulator,
        UserAccumulator> {

    /**
     * 创建新的窗口累加器。
     */
    @Override
    public UserAccumulator createAccumulator() {

        return new UserAccumulator();
    }

    /**
     * 将一条 UserDelta 加入窗口累加器。
     */
    @Override
    public UserAccumulator add(
            UserDelta delta,
            UserAccumulator accumulator) {

        if (delta == null) {
            return accumulator;
        }

        /*
         * 当前窗口是按照 userId keyBy 的，
         * 因此正常情况下所有 Delta 的 userId 相同。
         *
         * 这里保留 userId，
         * 后续 UserWindowProcessFunction 生成 UserSummary 时使用。
         */
        if (accumulator.getUserId() == null) {
            accumulator.setUserId(
                    delta.getUserId()
            );
        }

        // ------------------------------------------------------------
        // 1. 下单指标
        // ------------------------------------------------------------

        if (delta.getOrderCount() > 0L) {

            accumulator.addOrderCount(
                    delta.getOrderCount()
            );

            accumulator.addOrderAmount(
                    delta.getOrderAmount()
            );

            /*
             * Set 去重。
             *
             * 同一用户一分钟内：
             *
             * 在同一家店下3单
             *
             * orderCount = 3
             * orderShopCount = 1
             */
            accumulator.addOrderShopId(
                    delta.getShopId()
            );
        }

        // ------------------------------------------------------------
        // 2. 支付指标
        // ------------------------------------------------------------

        if (delta.getPaidOrderCount() > 0L) {

            accumulator.addPaidOrderCount(
                    delta.getPaidOrderCount()
            );

            accumulator.addPaidAmount(
                    delta.getPaidAmount()
            );

            accumulator.addPaidShopId(
                    delta.getShopId()
            );
        }

        return accumulator;
    }

    /**
     * 返回当前窗口聚合结果。
     */
    @Override
    public UserAccumulator getResult(
            UserAccumulator accumulator) {

        return accumulator;
    }

    /**
     * 合并两个累加器。
     *
     * 当前开发阶段并行度为1，
     * 普通滚动窗口通常不会依赖 merge，
     * 但这里仍然实现完整的合并逻辑。
     */
    @Override
    public UserAccumulator merge(
            UserAccumulator a,
            UserAccumulator b) {

        if (a == null) {
            return b;
        }

        if (b == null) {
            return a;
        }

        if (a.getUserId() == null) {
            a.setUserId(
                    b.getUserId()
            );
        }

        // ------------------------------------------------------------
        // 下单指标合并
        // ------------------------------------------------------------

        a.addOrderCount(
                b.getOrderCount()
        );

        a.addOrderAmount(
                b.getOrderAmount()
        );

        if (b.getOrderShopIds() != null) {
            a.getOrderShopIds()
                    .addAll(
                            b.getOrderShopIds()
                    );
        }

        // ------------------------------------------------------------
        // 支付指标合并
        // ------------------------------------------------------------

        a.addPaidOrderCount(
                b.getPaidOrderCount()
        );

        a.addPaidAmount(
                b.getPaidAmount()
        );

        if (b.getPaidShopIds() != null) {
            a.getPaidShopIds()
                    .addAll(
                            b.getPaidShopIds()
                    );
        }

        return a;
    }
}