// 文件路径：src/main/java/org/example/process/ShopDeltaCalculator.java
package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.ShopDelta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shop 主题业务变化计算器。
 *
 * 根据同一个 orderId 的：
 *
 * previous
 * current
 *
 * 判断是否产生：
 *
 * SHOP_ORDER
 * SHOP_PAY
 *
 * 本类只负责业务变化识别，
 * 不负责 Flink State，
 * 不负责 Window，
 * 不负责 UV。
 */
public final class ShopDeltaCalculator {

    public static final String EVENT_SHOP_ORDER =
            "SHOP_ORDER";

    public static final String EVENT_SHOP_PAY =
            "SHOP_PAY";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    private ShopDeltaCalculator() {
    }

    /**
     * 根据 previous/current 计算 ShopDelta。
     */
    public static List<ShopDelta> calculate(
            DwdOrderInfo previous,
            DwdOrderInfo current) {

        // ------------------------------------------------------------
        // 1. 基础保护
        // ------------------------------------------------------------

        if (current == null
                || current.getOrderId() == null
                || current.getShopId() == null) {

            return Collections.emptyList();
        }

        List<ShopDelta> result =
                new ArrayList<>();

        // ------------------------------------------------------------
        // 2. 第一次看到订单
        // ------------------------------------------------------------

        if (previous == null) {

            /*
             * 新订单：
             *
             * createTime 合法时，
             * 产生 SHOP_ORDER。
             */
            ShopDelta orderDelta =
                    createOrderDelta(current);

            if (orderDelta != null) {
                result.add(orderDelta);
            }

            /*
             * 如果第一次看到订单时已经支付：
             *
             * 同时产生 SHOP_PAY。
             */
            if (isPaid(current)) {

                ShopDelta payDelta =
                        createPayDelta(current);

                if (payDelta != null) {
                    result.add(payDelta);
                }
            }

            return result;
        }

        // ------------------------------------------------------------
        // 3. 已有订单：识别未支付 -> 已支付
        // ------------------------------------------------------------

        boolean previousPaid =
                isPaid(previous);

        boolean currentPaid =
                isPaid(current);

        if (!previousPaid && currentPaid) {

            ShopDelta payDelta =
                    createPayDelta(current);

            if (payDelta != null) {
                result.add(payDelta);
            }
        }

        /*
         * 其他普通 UPDATE：
         *
         * 第一版 Shop 不产生指标变化。
         */
        return result;
    }

    /**
     * 创建 SHOP_ORDER。
     */
    private static ShopDelta createOrderDelta(
            DwdOrderInfo order) {

        Long eventTs =
                parseEventTimestamp(
                        order.getCreateTime()
                );

        /*
         * 没有合法 createTime，
         * 就不能形成正确的 Event-Time 下单事实。
         */
        if (eventTs == null) {
            return null;
        }

        ShopDelta delta =
                createBaseDelta(order);

        delta.setOrderCountDelta(1L);

        delta.setOrderAmountDelta(
                safeMoney(
                        order.getRealTotalMoney()
                )
        );

        delta.setPaidOrderCountDelta(0L);
        delta.setPaidAmountDelta(BigDecimal.ZERO);

        delta.setEventType(
                EVENT_SHOP_ORDER
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
     * 创建 SHOP_PAY。
     */
    private static ShopDelta createPayDelta(
            DwdOrderInfo order) {

        /*
         * 支付事实必须同时满足：
         *
         * isPay == 1
         * payTime 合法
         */
        if (!isPaid(order)) {
            return null;
        }

        Long eventTs =
                parseEventTimestamp(
                        order.getPayTime()
                );

        if (eventTs == null) {
            return null;
        }

        ShopDelta delta =
                createBaseDelta(order);

        delta.setOrderCountDelta(0L);
        delta.setOrderAmountDelta(BigDecimal.ZERO);

        delta.setPaidOrderCountDelta(1L);

        delta.setPaidAmountDelta(
                safeMoney(
                        order.getRealTotalMoney()
                )
        );

        delta.setEventType(
                EVENT_SHOP_PAY
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
     * 创建 Delta 的公共字段。
     */
    private static ShopDelta createBaseDelta(
            DwdOrderInfo order) {

        ShopDelta delta =
                new ShopDelta();

        delta.setOrderId(
                order.getOrderId()
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
     * 判断订单是否已经支付。
     *
     * 注意：
     *
     * 支付状态以 isPay 为准，
     * 不能仅通过 payTime 推断。
     */
    private static boolean isPaid(
            DwdOrderInfo order) {

        return order != null
                && Integer.valueOf(1)
                .equals(order.getIsPay());
    }

    /**
     * 金额 null 保护。
     */
    private static BigDecimal safeMoney(
            BigDecimal money) {

        return money == null
                ? BigDecimal.ZERO
                : money;
    }

    /**
     * 将业务时间：
     *
     * yyyy-MM-dd HH:mm:ss
     *
     * 转换为毫秒 Event Timestamp。
     *
     * 当前项目业务数据没有时区字段，
     * 使用系统默认时区与当前 Trade 开发环境保持一致。
     */
    private static Long parseEventTimestamp(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return null;
        }

        try {

            LocalDateTime dateTime =
                    LocalDateTime.parse(
                            value.trim(),
                            DATE_TIME_FORMATTER
                    );

            return dateTime
                    .atZone(
                            ZoneId.systemDefault()
                    )
                    .toInstant()
                    .toEpochMilli();

        } catch (DateTimeParseException e) {

            return null;
        }
    }
}