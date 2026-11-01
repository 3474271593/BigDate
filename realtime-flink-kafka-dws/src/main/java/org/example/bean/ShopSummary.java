package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DWS 店铺主题一分钟窗口汇总结果。
 *
 * 粒度：
 *
 * shopId + 1 minute
 *
 * 第一版指标：
 *
 * orderCount
 * orderUserCount
 * orderAmount
 * paidOrderCount
 * paidUserCount
 * paidAmount
 */
public class ShopSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 店铺 ID。
     */
    private Long shopId;

    /**
     * 窗口开始时间。
     *
     * 例如：
     * 2019-06-22 17:28:00
     */
    private String windowStart;

    /**
     * 窗口结束时间。
     *
     * 例如：
     * 2019-06-22 17:29:00
     */
    private String windowEnd;

    /**
     * 下单订单数。
     */
    private long orderCount;

    /**
     * 下单用户数 UV。
     */
    private long orderUserCount;

    /**
     * 下单金额。
     */
    private BigDecimal orderAmount = BigDecimal.ZERO;

    /**
     * 支付订单数。
     */
    private long paidOrderCount;

    /**
     * 支付用户数 UV。
     */
    private long paidUserCount;

    /**
     * 支付金额。
     */
    private BigDecimal paidAmount = BigDecimal.ZERO;

    public ShopSummary() {
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
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

    public long getOrderUserCount() {
        return orderUserCount;
    }

    public void setOrderUserCount(long orderUserCount) {
        this.orderUserCount = orderUserCount;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount =
                orderAmount == null
                        ? BigDecimal.ZERO
                        : orderAmount;
    }

    public long getPaidOrderCount() {
        return paidOrderCount;
    }

    public void setPaidOrderCount(long paidOrderCount) {
        this.paidOrderCount = paidOrderCount;
    }

    public long getPaidUserCount() {
        return paidUserCount;
    }

    public void setPaidUserCount(long paidUserCount) {
        this.paidUserCount = paidUserCount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount =
                paidAmount == null
                        ? BigDecimal.ZERO
                        : paidAmount;
    }

    @Override
    public String toString() {
        return "ShopSummary{" +
                "shopId=" + shopId +
                ", windowStart='" + windowStart + '\'' +
                ", windowEnd='" + windowEnd + '\'' +
                ", orderCount=" + orderCount +
                ", orderUserCount=" + orderUserCount +
                ", orderAmount=" + orderAmount +
                ", paidOrderCount=" + paidOrderCount +
                ", paidUserCount=" + paidUserCount +
                ", paidAmount=" + paidAmount +
                '}';
    }
}