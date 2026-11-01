// 文件路径：src/main/java/org/example/bean/TradeSummary.java
package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DWS交易主题窗口聚合结果。
 *
 * 一条TradeSummary代表一个时间窗口内的交易汇总指标。
 *
 * 例如：
 *
 * windowStart = 2019-06-22 17:28:00
 * windowEnd   = 2019-06-22 17:29:00
 *
 * orderCount     = 10
 * paidOrderCount = 6
 * paidAmount     = 588.50
 */
public class TradeSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 窗口开始时间。
     */
    private String windowStart;

    /**
     * 窗口结束时间。
     */
    private String windowEnd;

    /**
     * 窗口内新增订单数。
     */
    private long orderCount;

    /**
     * 窗口内支付订单数。
     */
    private long paidOrderCount;

    /**
     * 窗口内支付金额。
     */
    private BigDecimal paidAmount = BigDecimal.ZERO;

    public TradeSummary() {
    }

    public String getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(String windowStart) {
        this.windowStart = windowStart;
    }

    public String getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(String windowEnd) {
        this.windowEnd = windowEnd;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public long getPaidOrderCount() {
        return paidOrderCount;
    }

    public void setPaidOrderCount(long paidOrderCount) {
        this.paidOrderCount = paidOrderCount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount == null
                ? BigDecimal.ZERO
                : paidAmount;
    }

    @Override
    public String toString() {
        return "TradeSummary{" +
                "windowStart='" + windowStart + '\'' +
                ", windowEnd='" + windowEnd + '\'' +
                ", orderCount=" + orderCount +
                ", paidOrderCount=" + paidOrderCount +
                ", paidAmount=" + paidAmount +
                '}';
    }
}