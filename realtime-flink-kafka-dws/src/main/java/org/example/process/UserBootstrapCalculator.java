package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.UserAccumulator;
import org.example.bean.UserSummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * User 历史 Bootstrap 计算器。
 *
 * 处理流程：
 *
 * 1. 读取历史 DWD OrderInfo；
 * 2. 按 orderId 保留最终快照；
 * 3. 根据 createTime 生成 USER_ORDER 历史贡献；
 * 4. 根据最终 isPay + payTime 生成 USER_PAY 历史贡献；
 * 5. 按 userId + minute 聚合；
 * 6. 使用 Set<Long> 精确计算店铺数；
 * 7. 输出 UserSummary。
 *
 * 该过程不依赖 Flink Watermark。
 */
public final class UserBootstrapCalculator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 与已经验证的 Shop Bootstrap 保持一致，
     * 使用 Asia/Shanghai 进行窗口 Key 的稳定排序。
     */
    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Shanghai");

    private UserBootstrapCalculator() {
    }

    public static UserBootstrapResult calculate(
            Iterable<DwdOrderInfo> records) {

        if (records == null) {
            throw new IllegalArgumentException(
                    "records must not be null"
            );
        }

        long readCount = 0L;
        long bootstrapCount = 0L;
        long updateCount = 0L;

        /*
         * orderId -> 最终订单快照
         *
         * Kafka 历史记录按照消费顺序进入这里。
         * 同一个 orderId 后出现的 UPDATE 会覆盖
         * 之前的 bootstrap / UPDATE。
         */
        Map<Long, DwdOrderInfo> latestOrders =
                new LinkedHashMap<>();

        for (DwdOrderInfo order : records) {

            readCount++;

            if (order == null
                    || order.getOrderId() == null) {
                continue;
            }

            if ("bootstrap".equalsIgnoreCase(
                    order.getOpType())) {

                bootstrapCount++;

            } else if ("UPDATE".equalsIgnoreCase(
                    order.getOpType())) {

                updateCount++;
            }

            latestOrders.put(
                    order.getOrderId(),
                    order
            );
        }

        /*
         * key:
         *
         * userId + minute
         *
         * TreeMap 保证历史 UserSummary
         * 具有稳定输出顺序。
         */
        TreeMap<UserMinuteKey, UserAccumulator> byWindow =
                new TreeMap<>();

        LocalDateTime earliest = null;
        LocalDateTime latest = null;

        /*
         * 注意：
         *
         * 必须遍历最终订单快照，
         * 不能直接遍历所有 CDC 记录产生指标，
         * 否则 UPDATE 会造成重复统计。
         */
        for (DwdOrderInfo order : latestOrders.values()) {

            /*
             * User 主题的统计主体就是 userId。
             *
             * userId == null 时无法进入
             * userId + minute 粒度。
             */
            if (order.getUserId() == null) {
                continue;
            }

            /*
             * ============================================================
             * USER_ORDER
             * ============================================================
             */

            LocalDateTime createTime =
                    parse(order.getCreateTime());

            if (createTime != null) {

                if (earliest == null
                        || createTime.isBefore(earliest)) {

                    earliest = createTime;
                }

                if (latest == null
                        || createTime.isAfter(latest)) {

                    latest = createTime;
                }

                addOrder(
                        byWindow,
                        order,
                        createTime
                );
            }

            /*
             * ============================================================
             * USER_PAY
             * ============================================================
             *
             * 支付事实以 isPay = 1 为准。
             *
             * 只有最终订单状态为已支付，
             * 并且 payTime 合法，
             * 才产生 USER_PAY。
             */
            if (Integer.valueOf(1)
                    .equals(order.getIsPay())) {

                LocalDateTime payTime =
                        parse(order.getPayTime());

                if (payTime != null) {

                    addPay(
                            byWindow,
                            order,
                            payTime
                    );
                }
            }
        }

        /*
         * ================================================================
         * UserAccumulator -> UserSummary
         * ================================================================
         */

        List<UserSummary> summaries =
                new ArrayList<>();

        long totalOrderCount = 0L;
        long totalPaidOrderCount = 0L;

        BigDecimal totalOrderAmount =
                BigDecimal.ZERO;

        BigDecimal totalPaidAmount =
                BigDecimal.ZERO;

        for (Map.Entry<UserMinuteKey, UserAccumulator> entry
                : byWindow.entrySet()) {

            UserMinuteKey key =
                    entry.getKey();

            UserAccumulator accumulator =
                    entry.getValue();

            UserSummary summary =
                    new UserSummary();

            summary.setUserId(
                    key.userId
            );

            summary.setWindowStart(
                    FORMATTER.format(
                            key.windowStart
                    )
            );

            summary.setWindowEnd(
                    FORMATTER.format(
                            key.windowStart.plusMinutes(1)
                    )
            );

            /*
             * 下单指标
             */
            summary.setOrderCount(
                    accumulator.getOrderCount()
            );

            summary.setOrderShopCount(
                    accumulator.getOrderShopCount()
            );

            summary.setOrderAmount(
                    accumulator.getOrderAmount()
            );

            /*
             * 支付指标
             */
            summary.setPaidOrderCount(
                    accumulator.getPaidOrderCount()
            );

            summary.setPaidShopCount(
                    accumulator.getPaidShopCount()
            );

            summary.setPaidAmount(
                    accumulator.getPaidAmount()
            );

            summaries.add(
                    summary
            );

            /*
             * Bootstrap 全局校验指标。
             *
             * 注意：
             * 这里只累计订单数和金额。
             *
             * orderShopCount / paidShopCount
             * 不能简单跨窗口相加解释成历史唯一店铺数。
             */
            totalOrderCount +=
                    summary.getOrderCount();

            totalPaidOrderCount +=
                    summary.getPaidOrderCount();

            totalOrderAmount =
                    totalOrderAmount.add(
                            summary.getOrderAmount()
                    );

            totalPaidAmount =
                    totalPaidAmount.add(
                            summary.getPaidAmount()
                    );
        }

        return new UserBootstrapResult(
                readCount,
                bootstrapCount,
                updateCount,
                latestOrders.size(),
                earliest == null
                        ? null
                        : FORMATTER.format(earliest),
                latest == null
                        ? null
                        : FORMATTER.format(latest),
                totalOrderCount,
                totalPaidOrderCount,
                totalOrderAmount,
                totalPaidAmount,
                summaries
        );
    }

    /**
     * USER_ORDER 历史贡献。
     */
    private static void addOrder(
            TreeMap<UserMinuteKey, UserAccumulator> byWindow,
            DwdOrderInfo order,
            LocalDateTime eventTime) {

        UserAccumulator accumulator =
                getAccumulator(
                        byWindow,
                        order.getUserId(),
                        eventTime
                );

        accumulator.addOrderCount(
                1L
        );

        accumulator.addOrderAmount(
                safeAmount(
                        order.getRealTotalMoney()
                )
        );

        /*
         * shopId 允许为空。
         *
         * 用户订单事实仍然成立，
         * 只是 null shopId 不能贡献店铺去重指标。
         *
         * UserAccumulator 内部已经对 null 做保护。
         */
        accumulator.addOrderShopId(
                order.getShopId()
        );
    }

    /**
     * USER_PAY 历史贡献。
     */
    private static void addPay(
            TreeMap<UserMinuteKey, UserAccumulator> byWindow,
            DwdOrderInfo order,
            LocalDateTime eventTime) {

        UserAccumulator accumulator =
                getAccumulator(
                        byWindow,
                        order.getUserId(),
                        eventTime
                );

        accumulator.addPaidOrderCount(
                1L
        );

        accumulator.addPaidAmount(
                safeAmount(
                        order.getRealTotalMoney()
                )
        );

        accumulator.addPaidShopId(
                order.getShopId()
        );
    }

    /**
     * 获取：
     *
     * userId + minute
     *
     * 对应的 UserAccumulator。
     */
    private static UserAccumulator getAccumulator(
            TreeMap<UserMinuteKey, UserAccumulator> byWindow,
            Long userId,
            LocalDateTime eventTime) {

        LocalDateTime windowStart =
                eventTime.truncatedTo(
                        ChronoUnit.MINUTES
                );

        UserMinuteKey key =
                new UserMinuteKey(
                        userId,
                        windowStart
                );

        UserAccumulator accumulator =
                byWindow.get(key);

        if (accumulator == null) {

            accumulator =
                    new UserAccumulator();

            accumulator.setUserId(
                    userId
            );

            byWindow.put(
                    key,
                    accumulator
            );
        }

        return accumulator;
    }

    /**
     * null 金额统一按 0 处理。
     */
    private static BigDecimal safeAmount(
            BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    /**
     * 解析业务时间。
     *
     * null / 空字符串 / 非法时间：
     * 返回 null，不产生对应业务贡献。
     */
    private static LocalDateTime parse(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return null;
        }

        try {

            return LocalDateTime.parse(
                    value,
                    FORMATTER
            );

        } catch (Exception ignored) {

            return null;
        }
    }

    /**
     * User Bootstrap 内部窗口 Key。
     *
     * 粒度：
     *
     * userId + minute
     *
     * 排序：
     *
     * 1. windowStart
     * 2. userId
     *
     * 这样输出 Kafka 时，
     * 同一分钟不同用户保持稳定顺序。
     */
    private static final class UserMinuteKey
            implements Comparable<UserMinuteKey> {

        private final Long userId;
        private final LocalDateTime windowStart;

        private UserMinuteKey(
                Long userId,
                LocalDateTime windowStart) {

            this.userId =
                    userId;

            this.windowStart =
                    windowStart;
        }

        @Override
        public int compareTo(
                UserMinuteKey other) {

            long thisTs =
                    windowStart
                            .atZone(ZONE_ID)
                            .toInstant()
                            .toEpochMilli();

            long otherTs =
                    other.windowStart
                            .atZone(ZONE_ID)
                            .toInstant()
                            .toEpochMilli();

            int timeCompare =
                    Long.compare(
                            thisTs,
                            otherTs
                    );

            if (timeCompare != 0) {
                return timeCompare;
            }

            return Long.compare(
                    userId,
                    other.userId
            );
        }
    }
}