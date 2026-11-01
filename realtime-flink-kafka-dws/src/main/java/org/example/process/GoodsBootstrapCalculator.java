package org.example.process;

import org.example.bean.DwdOrderGoods;
import org.example.bean.DwdOrderInfo;
import org.example.bean.GoodsSummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Builds the minimal Goods historical baseline from final DWD snapshots. */
public final class GoodsBootstrapCalculator {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private GoodsBootstrapCalculator() {
    }

    public static GoodsBootstrapResult calculate(
            Iterable<DwdOrderInfo> orderRecords,
            Iterable<DwdOrderGoods> detailRecords) {

        Map<Long, DwdOrderInfo> orders = new LinkedHashMap<>();
        Map<Long, DwdOrderGoods> details = new LinkedHashMap<>();
        long orderReadCount = 0L;
        long detailReadCount = 0L;

        if (orderRecords != null) {
            for (DwdOrderInfo order : orderRecords) {
                orderReadCount++;
                if (order != null && order.getOrderId() != null) {
                    orders.put(order.getOrderId(), order);
                }
            }
        }
        if (detailRecords != null) {
            for (DwdOrderGoods detail : detailRecords) {
                detailReadCount++;
                if (detail != null && detail.getOgId() != null) {
                    details.put(detail.getOgId(), detail);
                }
            }
        }

        TreeMap<MinuteGoodsKey, GoodsSummary> byWindow = new TreeMap<>();
        for (DwdOrderGoods detail : details.values()) {
            DwdOrderInfo order = orders.get(detail.getOrderId());
            if (order == null || detail.getGoodsId() == null) {
                continue;
            }

            LocalDateTime createTime = parse(order.getCreateTime());
            if (createTime != null) {
                GoodsSummary summary = summary(byWindow, detail, createTime);
                summary.setOrderCount(summary.getOrderCount() + 1L);
                summary.setOrderGoodsNum(summary.getOrderGoodsNum() + safeLong(detail.getGoodsNum()));
                summary.setOrderAmount(summary.getOrderAmount().add(safeDecimal(detail.getGoodsAmount())));
            }

            if (Integer.valueOf(1).equals(order.getIsPay())) {
                LocalDateTime payTime = parse(order.getPayTime());
                if (payTime != null) {
                    GoodsSummary summary = summary(byWindow, detail, payTime);
                    summary.setPaidOrderCount(summary.getPaidOrderCount() + 1L);
                    summary.setPaidGoodsNum(summary.getPaidGoodsNum() + safeLong(detail.getGoodsNum()));
                    summary.setPaidAmount(summary.getPaidAmount().add(safeDecimal(detail.getGoodsAmount())));
                }
            }
        }

        List<GoodsSummary> summaries = new ArrayList<>(byWindow.values());
        return new GoodsBootstrapResult(
                orderReadCount,
                detailReadCount,
                orders.size(),
                details.size(),
                summaries
        );
    }

    private static GoodsSummary summary(
            Map<MinuteGoodsKey, GoodsSummary> byWindow,
            DwdOrderGoods detail,
            LocalDateTime eventTime) {

        LocalDateTime start = eventTime.truncatedTo(ChronoUnit.MINUTES);
        MinuteGoodsKey key = new MinuteGoodsKey(detail.getGoodsId(), start);
        GoodsSummary value = byWindow.get(key);
        if (value == null) {
            value = new GoodsSummary();
            value.setGoodsId(detail.getGoodsId());
            value.setGoodsName(detail.getGoodsName());
            value.setWindowStart(FORMATTER.format(start));
            value.setWindowEnd(FORMATTER.format(start.plusMinutes(1L)));
            byWindow.put(key, value);
        } else if (detail.getGoodsName() != null) {
            value.setGoodsName(detail.getGoodsName());
        }
        return value;
    }

    private static LocalDateTime parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static long safeLong(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private static BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static final class MinuteGoodsKey
            implements Comparable<MinuteGoodsKey> {
        private final Long goodsId;
        private final LocalDateTime minute;

        private MinuteGoodsKey(Long goodsId, LocalDateTime minute) {
            this.goodsId = goodsId;
            this.minute = minute;
        }

        @Override
        public int compareTo(MinuteGoodsKey other) {
            int timeComparison = minute.compareTo(other.minute);
            return timeComparison != 0
                    ? timeComparison
                    : goodsId.compareTo(other.goodsId);
        }
    }
}
