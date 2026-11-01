package org.example.process;

import org.example.bean.ShopSummary;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Shop 历史 Bootstrap 计算结果。
 */
public class ShopBootstrapResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long readCount;
    private final long bootstrapCount;
    private final long updateCount;
    private final long uniqueOrderCount;

    private final String earliestCreateTime;
    private final String latestCreateTime;

    private final long orderCount;
    private final long paidOrderCount;

    private final BigDecimal orderAmount;
    private final BigDecimal paidAmount;

    private final List<ShopSummary> summaries;

    public ShopBootstrapResult(
            long readCount,
            long bootstrapCount,
            long updateCount,
            long uniqueOrderCount,
            String earliestCreateTime,
            String latestCreateTime,
            long orderCount,
            long paidOrderCount,
            BigDecimal orderAmount,
            BigDecimal paidAmount,
            List<ShopSummary> summaries) {

        this.readCount = readCount;
        this.bootstrapCount = bootstrapCount;
        this.updateCount = updateCount;
        this.uniqueOrderCount = uniqueOrderCount;
        this.earliestCreateTime = earliestCreateTime;
        this.latestCreateTime = latestCreateTime;
        this.orderCount = orderCount;
        this.paidOrderCount = paidOrderCount;
        this.orderAmount = orderAmount == null
                ? BigDecimal.ZERO
                : orderAmount;
        this.paidAmount = paidAmount == null
                ? BigDecimal.ZERO
                : paidAmount;
        this.summaries = summaries == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(summaries);
    }

    public long getReadCount() {
        return readCount;
    }

    public long getBootstrapCount() {
        return bootstrapCount;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public long getUniqueOrderCount() {
        return uniqueOrderCount;
    }

    public String getEarliestCreateTime() {
        return earliestCreateTime;
    }

    public String getLatestCreateTime() {
        return latestCreateTime;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public long getPaidOrderCount() {
        return paidOrderCount;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public List<ShopSummary> getSummaries() {
        return summaries;
    }
}