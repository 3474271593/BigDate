// 文件路径：src/main/java/org/example/process/TradeDeltaCalculator.java
package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeDelta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DWS交易变化事件计算器。
 *
 * 根据订单前后两个版本，计算真正需要进入DWS窗口的业务事件。
 *
 * 当前支持：
 *
 * 1. 订单第一次出现：
 *    产生ORDER_CREATE事件；
 *
 * 2. 订单第一次出现时已经支付：
 *    同时产生ORDER_CREATE和ORDER_PAY事件；
 *
 * 3. 未支付 -> 已支付：
 *    只产生ORDER_PAY事件；
 *
 * 4. 已支付 -> 已支付的普通UPDATE：
 *    不产生重复支付事件；
 *
 * 5. 未支付 -> 未支付的普通UPDATE：
 *    不产生新的业务事件。
 */
public class TradeDeltaCalculator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Shanghai");

    private TradeDeltaCalculator() {
    }

    /**
     * 计算订单CDC变化产生的DWS业务事件。
     *
     * @param previous 上一个订单版本，第一次出现时为null
     * @param current 当前订单版本
     * @return 业务变化事件列表
     */
    public static List<TradeDelta> calculate(
            DwdOrderInfo previous,
            DwdOrderInfo current
    ) {

        List<TradeDelta> result =
                new ArrayList<>();

        if (current == null || current.getOrderId() == null) {
            return result;
        }

        /*
         * 场景1：
         * 订单第一次进入DWS。
         *
         * 无论当前是否支付，
         * 首先都代表产生过一张订单。
         */
        if (previous == null) {

            TradeDelta orderCreate =
                    createOrderCreateDelta(current);

            if (orderCreate != null) {
                result.add(orderCreate);
            }

            /*
             * 历史bootstrap数据中，
             * 第一次看到订单时可能已经是已支付状态。
             *
             * 此时除了ORDER_CREATE，
             * 还必须补一个ORDER_PAY事件。
             */
            if (isPaid(current)) {

                TradeDelta orderPay =
                        createOrderPayDelta(current);

                if (orderPay != null) {
                    result.add(orderPay);
                }
            }

            return result;
        }

        /*
         * 场景2：
         *
         * previous：未支付
         * current ：已支付
         *
         * 这是一次真正的支付状态变化。
         */
        if (!isPaid(previous) && isPaid(current)) {

            TradeDelta orderPay =
                    createOrderPayDelta(current);

            if (orderPay != null) {
                result.add(orderPay);
            }
        }

        /*
         * 其他UPDATE当前不产生指标变化：
         *
         * 未支付 -> 未支付
         * 已支付 -> 已支付
         * 修改地址
         * 修改其他非交易指标字段
         */
        return result;
    }

    /**
     * 创建下单事件。
     */
    private static TradeDelta createOrderCreateDelta(
            DwdOrderInfo order
    ) {

        Long eventTs =
                parseEventTime(order.getCreateTime());

        /*
         * 没有有效createTime时，
         * 当前不伪造processTs作为业务时间。
         */
        if (eventTs == null) {
            return null;
        }

        TradeDelta delta =
                createBaseDelta(order);

        delta.setOrderCountDelta(1L);
        delta.setPaidOrderCountDelta(0L);
        delta.setPaidAmountDelta(BigDecimal.ZERO);
        delta.setEventType("ORDER_CREATE");
        delta.setEventTime(order.getCreateTime());
        delta.setEventTs(eventTs);

        return delta;
    }

    /**
     * 创建支付事件。
     */
    private static TradeDelta createOrderPayDelta(
            DwdOrderInfo order
    ) {

        /*
         * 支付事件必须有有效payTime。
         *
         * 不能使用createTime或processTs代替支付时间。
         */
        Long eventTs =
                parseEventTime(order.getPayTime());

        if (eventTs == null) {
            return null;
        }

        TradeDelta delta =
                createBaseDelta(order);

        delta.setOrderCountDelta(0L);
        delta.setPaidOrderCountDelta(1L);

        delta.setPaidAmountDelta(
                order.getRealTotalMoney() == null
                        ? BigDecimal.ZERO
                        : order.getRealTotalMoney()
        );

        delta.setEventType("ORDER_PAY");
        delta.setEventTime(order.getPayTime());
        delta.setEventTs(eventTs);

        return delta;
    }

    /**
     * 创建公共字段。
     */
    private static TradeDelta createBaseDelta(
            DwdOrderInfo order
    ) {

        TradeDelta delta =
                new TradeDelta();

        delta.setOrderId(
                order.getOrderId()
        );

        delta.setUserId(
                order.getUserId()
        );

        delta.setShopId(
                order.getShopId()
        );

        return delta;
    }

    /**
     * 判断订单是否已支付。
     *
     * 支付状态始终以isPay为准。
     */
    private static boolean isPaid(
            DwdOrderInfo order
    ) {

        return order != null
                && Integer.valueOf(1).equals(
                order.getIsPay()
        );
    }

    /**
     * 将业务时间转换成毫秒时间戳。
     *
     * 输入格式：
     * yyyy-MM-dd HH:mm:ss
     */
    private static Long parseEventTime(
            String time
    ) {

        if (time == null || time.trim().isEmpty()) {
            return null;
        }

        try {

            LocalDateTime localDateTime =
                    LocalDateTime.parse(
                            time,
                            DATE_TIME_FORMATTER
                    );

            return localDateTime
                    .atZone(ZONE_ID)
                    .toInstant()
                    .toEpochMilli();

        } catch (Exception e) {

            return null;
        }
    }
}