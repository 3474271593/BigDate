package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeSummary;

import java.math.BigDecimal;

/**
 * 订单CDC前后状态变化计算器。
 *
 * 作用：
 * 根据同一订单的上一版本状态和当前版本状态，
 * 计算当前CDC事件真正应该给DWS指标带来的增量。
 *
 * 当前规则：
 * 1. 首次出现订单：订单量 +1。
 * 2. 首次出现时已经支付：订单量 +1、支付订单量 +1、支付金额增加。
 * 3. 未支付 -> 已支付：支付订单量 +1、支付金额增加。
 * 4. 已支付 -> 已支付的普通UPDATE：不重复统计。
 */
public class TradeChangeCalculator {

    private TradeChangeCalculator() {
    }

    public static TradeSummary calculate(DwdOrderInfo previous, DwdOrderInfo current) {
        TradeSummary delta = new TradeSummary();

        if (current == null) {
            return delta;
        }

        // 场景1：该orderId第一次进入DWS
        if (previous == null) {
            delta.setOrderCount(1L);

            if (isPaid(current)) {
                delta.setPaidOrderCount(1L);
                delta.setPaidAmount(getPaidAmount(current));
            }

            return delta;
        }

        // 场景2：订单从未支付变为已支付
        if (!isPaid(previous) && isPaid(current)) {
            delta.setPaidOrderCount(1L);
            delta.setPaidAmount(getPaidAmount(current));
        }

        // 其他UPDATE当前不产生新的交易指标
        return delta;
    }

    private static boolean isPaid(DwdOrderInfo order) {
        return order != null && Integer.valueOf(1).equals(order.getIsPay());
    }

    private static BigDecimal getPaidAmount(DwdOrderInfo order) {
        if (order.getRealTotalMoney() == null) {
            return BigDecimal.ZERO;
        }

        return order.getRealTotalMoney();
    }
}