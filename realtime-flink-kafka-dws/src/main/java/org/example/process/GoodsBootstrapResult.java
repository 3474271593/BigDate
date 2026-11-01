package org.example.process;

import org.example.bean.GoodsSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GoodsBootstrapResult {
    private final long orderReadCount;
    private final long detailReadCount;
    private final int uniqueOrderCount;
    private final int uniqueDetailCount;
    private final List<GoodsSummary> summaries;

    public GoodsBootstrapResult(long orderReadCount, long detailReadCount,
                                int uniqueOrderCount, int uniqueDetailCount,
                                List<GoodsSummary> summaries) {
        this.orderReadCount = orderReadCount;
        this.detailReadCount = detailReadCount;
        this.uniqueOrderCount = uniqueOrderCount;
        this.uniqueDetailCount = uniqueDetailCount;
        this.summaries = Collections.unmodifiableList(new ArrayList<>(summaries));
    }

    public long getOrderReadCount() {
        return orderReadCount;
    }

    public long getDetailReadCount() {
        return detailReadCount;
    }

    public int getUniqueOrderCount() {
        return uniqueOrderCount;
    }

    public int getUniqueDetailCount() {
        return uniqueDetailCount;
    }

    public List<GoodsSummary> getSummaries() {
        return summaries;
    }
}
