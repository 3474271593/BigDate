// 文件路径：src/main/java/org/example/process/ShopAggregateFunction.java
package org.example.process;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.example.bean.ShopAccumulator;
import org.example.bean.ShopDelta;

import java.math.BigDecimal;
import java.util.HashSet;

/**
 * Shop 主题一分钟窗口增量聚合函数。
 *
 * 输入：
 * ShopDelta
 *
 * 累加器：
 * ShopAccumulator
 *
 * 主要完成：
 *
 * orderCount
 * orderUserIds
 * orderAmount
 * paidOrderCount
 * paidUserIds
 * paidAmount
 *
 * 最终 UV 不在这里转换成 long，
 * 而是在 ShopWindowProcessFunction 中通过 Set.size() 得到。
 */
public class ShopAggregateFunction
        implements AggregateFunction<
        ShopDelta,
        ShopAccumulator,
        ShopAccumulator> {

    @Override
    public ShopAccumulator createAccumulator() {
        return new ShopAccumulator();
    }

    @Override
    public ShopAccumulator add(
            ShopDelta delta,
            ShopAccumulator accumulator) {

        if (delta == null) {
            return accumulator;
        }

        // ============================================================
        // 1. 下单指标
        // ============================================================

        accumulator.setOrderCount(
                accumulator.getOrderCount()
                        + delta.getOrderCountDelta()
        );

        accumulator.setOrderAmount(
                safeMoney(accumulator.getOrderAmount())
                        .add(
                                safeMoney(
                                        delta.getOrderAmountDelta()
                                )
                        )
        );

        /*
         * 只有真正产生下单事实时，
         * userId 才进入 order UV 集合。
         *
         * null userId 不作为合法 UV。
         */
        if (delta.getOrderCountDelta() != 0
                && delta.getUserId() != null) {

            accumulator
                    .getOrderUserIds()
                    .add(delta.getUserId());
        }

        // ============================================================
        // 2. 支付指标
        // ============================================================

        accumulator.setPaidOrderCount(
                accumulator.getPaidOrderCount()
                        + delta.getPaidOrderCountDelta()
        );

        accumulator.setPaidAmount(
                safeMoney(accumulator.getPaidAmount())
                        .add(
                                safeMoney(
                                        delta.getPaidAmountDelta()
                                )
                        )
        );

        /*
         * 同理：
         *
         * 只有真正产生支付事实时，
         * userId 才进入支付 UV 集合。
         */
        if (delta.getPaidOrderCountDelta() != 0
                && delta.getUserId() != null) {

            accumulator
                    .getPaidUserIds()
                    .add(delta.getUserId());
        }

        return accumulator;
    }

    @Override
    public ShopAccumulator getResult(
            ShopAccumulator accumulator) {

        return accumulator;
    }

    @Override
    public ShopAccumulator merge(
            ShopAccumulator left,
            ShopAccumulator right) {

        ShopAccumulator result =
                new ShopAccumulator();

        // ============================================================
        // 订单数
        // ============================================================

        result.setOrderCount(
                left.getOrderCount()
                        + right.getOrderCount()
        );

        // ============================================================
        // 下单 UV
        // ============================================================

        HashSet<Long> orderUsers =
                new HashSet<>();

        if (left.getOrderUserIds() != null) {
            orderUsers.addAll(
                    left.getOrderUserIds()
            );
        }

        if (right.getOrderUserIds() != null) {
            orderUsers.addAll(
                    right.getOrderUserIds()
            );
        }

        result.setOrderUserIds(
                orderUsers
        );

        // ============================================================
        // 下单金额
        // ============================================================

        result.setOrderAmount(
                safeMoney(left.getOrderAmount())
                        .add(
                                safeMoney(
                                        right.getOrderAmount()
                                )
                        )
        );

        // ============================================================
        // 支付订单数
        // ============================================================

        result.setPaidOrderCount(
                left.getPaidOrderCount()
                        + right.getPaidOrderCount()
        );

        // ============================================================
        // 支付 UV
        // ============================================================

        HashSet<Long> paidUsers =
                new HashSet<>();

        if (left.getPaidUserIds() != null) {
            paidUsers.addAll(
                    left.getPaidUserIds()
            );
        }

        if (right.getPaidUserIds() != null) {
            paidUsers.addAll(
                    right.getPaidUserIds()
            );
        }

        result.setPaidUserIds(
                paidUsers
        );

        // ============================================================
        // 支付金额
        // ============================================================

        result.setPaidAmount(
                safeMoney(left.getPaidAmount())
                        .add(
                                safeMoney(
                                        right.getPaidAmount()
                                )
                        )
        );

        return result;
    }

    private BigDecimal safeMoney(
            BigDecimal money) {

        return money == null
                ? BigDecimal.ZERO
                : money;
    }
}