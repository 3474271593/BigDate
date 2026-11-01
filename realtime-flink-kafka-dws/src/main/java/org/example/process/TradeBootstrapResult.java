package org.example.process;

import org.example.bean.TradeSummary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result and validation statistics for a deterministic Trade bootstrap calculation. */
public class TradeBootstrapResult {

    private final long readCount;
    private final long bootstrapCount;
    private final long updateCount;
    private final int uniqueOrderCount;
    private final String earliestCreateTime;
    private final String latestCreateTime;
    private final long orderCount;
    private final long paidOrderCount;
    private final BigDecimal paidAmount;
    private final List<TradeSummary> summaries;

    public TradeBootstrapResult(long readCount, long bootstrapCount, long updateCount,
                                int uniqueOrderCount, String earliestCreateTime,
                                String latestCreateTime, long orderCount,
                                long paidOrderCount, BigDecimal paidAmount,
                                List<TradeSummary> summaries) {
        this.readCount = readCount;
        this.bootstrapCount = bootstrapCount;
        this.updateCount = updateCount;
        this.uniqueOrderCount = uniqueOrderCount;
        this.earliestCreateTime = earliestCreateTime;
        this.latestCreateTime = latestCreateTime;
        this.orderCount = orderCount;
        this.paidOrderCount = paidOrderCount;
        this.paidAmount = paidAmount;
        this.summaries = Collections.unmodifiableList(new ArrayList<>(summaries));
    }

    public long getReadCount() { return readCount; }
    public long getBootstrapCount() { return bootstrapCount; }
    public long getUpdateCount() { return updateCount; }
    public int getUniqueOrderCount() { return uniqueOrderCount; }
    public String getEarliestCreateTime() { return earliestCreateTime; }
    public String getLatestCreateTime() { return latestCreateTime; }
    public long getOrderCount() { return orderCount; }
    public long getPaidOrderCount() { return paidOrderCount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public List<TradeSummary> getSummaries() { return summaries; }
}
