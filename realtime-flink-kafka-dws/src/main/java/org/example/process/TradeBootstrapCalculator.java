package org.example.process;

import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeSummary;

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

/** Builds minute summaries from each order's final snapshot, independent of watermarks. */
public final class TradeBootstrapCalculator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private TradeBootstrapCalculator() {
    }

    public static TradeBootstrapResult calculate(Iterable<DwdOrderInfo> records) {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }

        long readCount = 0L;
        long bootstrapCount = 0L;
        long updateCount = 0L;
        Map<Long, DwdOrderInfo> latestOrders = new LinkedHashMap<>();

        for (DwdOrderInfo order : records) {
            readCount++;
            if (order == null || order.getOrderId() == null) {
                continue;
            }
            if ("bootstrap".equalsIgnoreCase(order.getOpType())) {
                bootstrapCount++;
            } else if ("UPDATE".equalsIgnoreCase(order.getOpType())) {
                updateCount++;
            }
            latestOrders.put(order.getOrderId(), order);
        }

        TreeMap<Long, TradeSummary> byMinute = new TreeMap<>();
        LocalDateTime earliest = null;
        LocalDateTime latest = null;

        for (DwdOrderInfo order : latestOrders.values()) {
            LocalDateTime createTime = parse(order.getCreateTime());
            if (createTime != null) {
                earliest = earliest == null || createTime.isBefore(earliest) ? createTime : earliest;
                latest = latest == null || createTime.isAfter(latest) ? createTime : latest;
                add(byMinute, createTime, 1L, 0L, BigDecimal.ZERO);
            }

            if (Integer.valueOf(1).equals(order.getIsPay())) {
                LocalDateTime payTime = parse(order.getPayTime());
                if (payTime != null) {
                    add(byMinute, payTime, 0L, 1L,
                            order.getRealTotalMoney() == null
                                    ? BigDecimal.ZERO : order.getRealTotalMoney());
                }
            }
        }

        long orderCount = 0L;
        long paidOrderCount = 0L;
        BigDecimal paidAmount = BigDecimal.ZERO;
        List<TradeSummary> summaries = new ArrayList<>(byMinute.values());
        for (TradeSummary summary : summaries) {
            orderCount += summary.getOrderCount();
            paidOrderCount += summary.getPaidOrderCount();
            paidAmount = paidAmount.add(summary.getPaidAmount());
        }

        return new TradeBootstrapResult(
                readCount,
                bootstrapCount,
                updateCount,
                latestOrders.size(),
                earliest == null ? null : FORMATTER.format(earliest),
                latest == null ? null : FORMATTER.format(latest),
                orderCount,
                paidOrderCount,
                paidAmount,
                summaries
        );
    }

    private static void add(TreeMap<Long, TradeSummary> byMinute,
                            LocalDateTime eventTime,
                            long orderDelta,
                            long paidDelta,
                            BigDecimal amountDelta) {
        LocalDateTime start = eventTime.truncatedTo(ChronoUnit.MINUTES);
        long key = start.atZone(ZONE_ID).toInstant().toEpochMilli();
        TradeSummary summary = byMinute.get(key);
        if (summary == null) {
            summary = new TradeSummary();
            summary.setWindowStart(FORMATTER.format(start));
            summary.setWindowEnd(FORMATTER.format(start.plusMinutes(1)));
            summary.setOrderCount(0L);
            summary.setPaidOrderCount(0L);
            summary.setPaidAmount(BigDecimal.ZERO);
            byMinute.put(key, summary);
        }
        summary.setOrderCount(summary.getOrderCount() + orderDelta);
        summary.setPaidOrderCount(summary.getPaidOrderCount() + paidDelta);
        summary.setPaidAmount(summary.getPaidAmount().add(amountDelta));
    }

    private static LocalDateTime parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }
}
