package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.ShopAccumulator;
import org.example.bean.ShopSummary;

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
 * Shop 历史 Bootstrap 计算器。
 *
 * 处理流程：
 *
 * 1. 读取历史 DWD OrderInfo；
 * 2. 按 orderId 保留最终快照；
 * 3. 根据 createTime 生成 SHOP_ORDER 历史贡献；
 * 4. 根据 payTime 生成 SHOP_PAY 历史贡献；
 * 5. 按 shopId + minute 聚合；
 * 6. 使用 Set<Long> 精确计算用户 UV；
 * 7. 输出 ShopSummary。
 *
 * 该过程不依赖 Flink Watermark。
 */
public final class ShopBootstrapCalculator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Shanghai");

    private ShopBootstrapCalculator() {
    }

    public static ShopBootstrapResult calculate(
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
         * 后出现的 UPDATE 会覆盖之前的 bootstrap / UPDATE。
         */
        Map<Long, DwdOrderInfo> latestOrders =
                new LinkedHashMap<>();

        for (DwdOrderInfo order : records) {

            readCount++;

            if (order == null ||
                    order.getOrderId() == null) {
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
         * shopId + minute
         *
         * TreeMap 保证最终输出具有稳定顺序。
         */
        TreeMap<ShopMinuteKey, ShopAccumulator> byWindow =
                new TreeMap<>();

        LocalDateTime earliest = null;
        LocalDateTime latest = null;

        for (DwdOrderInfo order : latestOrders.values()) {

            if (order.getShopId() == null) {
                continue;
            }

            /*
             * ============================
             * SHOP_ORDER
             * ============================
             */
            LocalDateTime createTime =
                    parse(order.getCreateTime());

            if (createTime != null) {

                if (earliest == null ||
                        createTime.isBefore(earliest)) {
                    earliest = createTime;
                }

                if (latest == null ||
                        createTime.isAfter(latest)) {
                    latest = createTime;
                }

                addOrder(
                        byWindow,
                        order,
                        createTime
                );
            }

            /*
             * ============================
             * SHOP_PAY
             * ============================
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

        List<ShopSummary> summaries =
                new ArrayList<>();

        long totalOrderCount = 0L;
        long totalPaidOrderCount = 0L;

        BigDecimal totalOrderAmount =
                BigDecimal.ZERO;

        BigDecimal totalPaidAmount =
                BigDecimal.ZERO;

        for (Map.Entry<ShopMinuteKey, ShopAccumulator> entry
                : byWindow.entrySet()) {

            ShopMinuteKey key = entry.getKey();
            ShopAccumulator accumulator =
                    entry.getValue();

            ShopSummary summary =
                    new ShopSummary();

            summary.setShopId(
                    key.shopId
            );

            summary.setWindowStart(
                    FORMATTER.format(key.windowStart)
            );

            summary.setWindowEnd(
                    FORMATTER.format(
                            key.windowStart.plusMinutes(1)
                    )
            );

            summary.setOrderCount(
                    accumulator.getOrderCount()
            );

            summary.setOrderUserCount(
                    accumulator.getOrderUserIds().size()
            );

            summary.setOrderAmount(
                    accumulator.getOrderAmount()
            );

            summary.setPaidOrderCount(
                    accumulator.getPaidOrderCount()
            );

            summary.setPaidUserCount(
                    accumulator.getPaidUserIds().size()
            );

            summary.setPaidAmount(
                    accumulator.getPaidAmount()
            );

            summaries.add(summary);

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

        return new ShopBootstrapResult(
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
     * SHOP_ORDER 历史贡献。
     */
    private static void addOrder(
            TreeMap<ShopMinuteKey, ShopAccumulator> byWindow,
            DwdOrderInfo order,
            LocalDateTime eventTime) {

        ShopAccumulator accumulator =
                getAccumulator(
                        byWindow,
                        order.getShopId(),
                        eventTime
                );

        accumulator.setOrderCount(
                accumulator.getOrderCount() + 1L
        );

        accumulator.setOrderAmount(
                accumulator.getOrderAmount()
                        .add(safeAmount(
                                order.getRealTotalMoney()
                        ))
        );

        if (order.getUserId() != null) {
            accumulator
                    .getOrderUserIds()
                    .add(order.getUserId());
        }
    }

    /**
     * SHOP_PAY 历史贡献。
     */
    private static void addPay(
            TreeMap<ShopMinuteKey, ShopAccumulator> byWindow,
            DwdOrderInfo order,
            LocalDateTime eventTime) {

        ShopAccumulator accumulator =
                getAccumulator(
                        byWindow,
                        order.getShopId(),
                        eventTime
                );

        accumulator.setPaidOrderCount(
                accumulator.getPaidOrderCount() + 1L
        );

        accumulator.setPaidAmount(
                accumulator.getPaidAmount()
                        .add(safeAmount(
                                order.getRealTotalMoney()
                        ))
        );

        if (order.getUserId() != null) {
            accumulator
                    .getPaidUserIds()
                    .add(order.getUserId());
        }
    }

    /**
     * 获取 shopId + minute 对应的聚合桶。
     */
    private static ShopAccumulator getAccumulator(
            TreeMap<ShopMinuteKey, ShopAccumulator> byWindow,
            Long shopId,
            LocalDateTime eventTime) {

        LocalDateTime windowStart =
                eventTime.truncatedTo(
                        ChronoUnit.MINUTES
                );

        ShopMinuteKey key =
                new ShopMinuteKey(
                        shopId,
                        windowStart
                );

        ShopAccumulator accumulator =
                byWindow.get(key);

        if (accumulator == null) {

            accumulator =
                    new ShopAccumulator();

            byWindow.put(
                    key,
                    accumulator
            );
        }

        return accumulator;
    }

    private static BigDecimal safeAmount(
            BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    private static LocalDateTime parse(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {
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
     * Shop Bootstrap 内部窗口 Key。
     *
     * 排序：
     * 1. windowStart
     * 2. shopId
     *
     * 这样输出 Kafka 时，同一分钟不同店铺会保持稳定顺序。
     */
    private static final class ShopMinuteKey
            implements Comparable<ShopMinuteKey> {

        private final Long shopId;
        private final LocalDateTime windowStart;

        private ShopMinuteKey(
                Long shopId,
                LocalDateTime windowStart) {

            this.shopId = shopId;
            this.windowStart = windowStart;
        }

        @Override
        public int compareTo(
                ShopMinuteKey other) {

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
                    shopId,
                    other.shopId
            );
        }
    }
}