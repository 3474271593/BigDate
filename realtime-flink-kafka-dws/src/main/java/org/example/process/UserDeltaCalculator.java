// 文件路径：src/main/java/org/example/process/UserDeltaCalculator.java
package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.UserDelta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User DWS 用户主题业务变化计算器。
 *
 * 根据同一个 orderId 的：
 *
 * previous
 * current
 *
 * 判断是否产生：
 *
 * USER_ORDER
 * USER_PAY
 *
 * 业务规则：
 *
 * 1. 第一次看到未支付订单
 *      -> USER_ORDER
 *
 * 2. 第一次看到已支付订单
 *      -> USER_ORDER + USER_PAY
 *
 * 3. 已存在未支付订单变为已支付
 *      -> USER_PAY
 *
 * 4. 普通 UPDATE
 *      -> 不产生 UserDelta
 *
 * 5. 已支付订单再次 UPDATE
 *      -> 不重复产生 USER_PAY
 *
 * 本类只负责业务变化识别，
 * 不负责 Flink State，
 * 不负责 Window，
 * 不负责店铺去重。
 */
public final class UserDeltaCalculator {

    public static final String EVENT_USER_ORDER =
            "USER_ORDER";

    public static final String EVENT_USER_PAY =
            "USER_PAY";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    private UserDeltaCalculator() {
    }

    /**
     * 根据 previous / current 计算 UserDelta。
     */
    public static List<UserDelta> calculate(
            DwdOrderInfo previous,
            DwdOrderInfo current) {

        // ------------------------------------------------------------
        // 1. 基础保护
        // ------------------------------------------------------------

        if (current == null
                || current.getOrderId() == null
                || current.getUserId() == null) {

            return Collections.emptyList();
        }

        List<UserDelta> result =
                new ArrayList<>();

        // ------------------------------------------------------------
        // 2. 第一次看到订单
        // ------------------------------------------------------------

        if (previous == null) {

            /*
             * 第一次看到订单：
             *
             * createTime 合法时，
             * 产生 USER_ORDER。
             */
            UserDelta orderDelta =
                    createOrderDelta(current);

            if (orderDelta != null) {
                result.add(orderDelta);
            }

            /*
             * 如果第一次看到订单时已经支付：
             *
             * 同时产生 USER_PAY。
             */
            if (isPaid(current)) {

                UserDelta payDelta =
                        createPayDelta(current);

                if (payDelta != null) {
                    result.add(payDelta);
                }
            }

            return result;
        }

        // ------------------------------------------------------------
        // 3. 已存在订单：识别 未支付 -> 已支付
        // ------------------------------------------------------------

        boolean previousPaid =
                isPaid(previous);

        boolean currentPaid =
                isPaid(current);

        if (!previousPaid && currentPaid) {

            UserDelta payDelta =
                    createPayDelta(current);

            if (payDelta != null) {
                result.add(payDelta);
            }
        }

        /*
         * 其他普通 UPDATE：
         *
         * User V1 不产生指标变化。
         */
        return result;
    }

    /**
     * 创建 USER_ORDER。
     */
    private static UserDelta createOrderDelta(
            DwdOrderInfo order) {

        Long eventTs =
                parseEventTimestamp(
                        order.getCreateTime()
                );

        /*
         * createTime 非法时，
         * 无法形成正确的 Event-Time 下单事实。
         */
        if (eventTs == null) {
            return null;
        }

        UserDelta delta =
                createBaseDelta(order);

        delta.setEventType(
                EVENT_USER_ORDER
        );

        delta.setEventTs(
                eventTs
        );

        delta.setOrderCount(
                1L
        );

        delta.setOrderAmount(
                safeMoney(
                        order.getRealTotalMoney()
                )
        );

        delta.setPaidOrderCount(
                0L
        );

        delta.setPaidAmount(
                BigDecimal.ZERO
        );

        return delta;
    }

    /**
     * 创建 USER_PAY。
     */
    private static UserDelta createPayDelta(
            DwdOrderInfo order) {

        /*
         * 支付事实必须满足：
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

        UserDelta delta =
                createBaseDelta(order);

        delta.setEventType(
                EVENT_USER_PAY
        );

        delta.setEventTs(
                eventTs
        );

        delta.setOrderCount(
                0L
        );

        delta.setOrderAmount(
                BigDecimal.ZERO
        );

        delta.setPaidOrderCount(
                1L
        );

        delta.setPaidAmount(
                safeMoney(
                        order.getRealTotalMoney()
                )
        );

        return delta;
    }

    /**
     * 创建 UserDelta 公共字段。
     */
    private static UserDelta createBaseDelta(
            DwdOrderInfo order) {

        UserDelta delta =
                new UserDelta();

        /*
         * User DWS 的主聚合 Key。
         */
        delta.setUserId(
                order.getUserId()
        );

        /*
         * 保留 shopId。
         *
         * 后续窗口中用于计算：
         *
         * orderShopCount
         * paidShopCount
         */
        delta.setShopId(
                order.getShopId()
        );

        return delta;
    }

    /**
     * 判断订单是否已支付。
     *
     * 支付状态以 isPay 为准，
     * 不能仅通过 payTime 判断。
     *
     * 原始数据中可能存在：
     *
     * isPay = 0
     * 但 payTime != null
     *
     * 这种数据不能视为已支付。
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
     * 与现有 ShopDeltaCalculator 保持相同时间处理方式。
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