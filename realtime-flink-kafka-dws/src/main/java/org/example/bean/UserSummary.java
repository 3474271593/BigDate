package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * User DWS 用户主题一分钟窗口汇总结果。
 *
 * 统计粒度：
 *
 * userId + 1分钟
 *
 * 当前 V1 指标：
 *
 * orderCount
 * orderShopCount
 * orderAmount
 * paidOrderCount
 * paidShopCount
 * paidAmount
 */
public class UserSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 窗口开始时间。
     *
     * 格式：
     * yyyy-MM-dd HH:mm:ss
     */
    private String windowStart;

    /**
     * 窗口结束时间。
     *
     * 格式：
     * yyyy-MM-dd HH:mm:ss
     */
    private String windowEnd;

    /**
     * 用户在当前窗口内的下单订单数。
     */
    private long orderCount;

    /**
     * 用户在当前窗口内下单涉及的不同店铺数。
     */
    private long orderShopCount;

    /**
     * 用户在当前窗口内的下单金额。
     */
    private BigDecimal orderAmount;

    /**
     * 用户在当前窗口内的支付订单数。
     */
    private long paidOrderCount;

    /**
     * 用户在当前窗口内支付涉及的不同店铺数。
     */
    private long paidShopCount;

    /**
     * 用户在当前窗口内的支付金额。
     */
    private BigDecimal paidAmount;

    public UserSummary() {
        this.orderAmount = BigDecimal.ZERO;
        this.paidAmount = BigDecimal.ZERO;
    }

    public UserSummary(
            Long userId,
            String windowStart,
            String windowEnd,
            long orderCount,
            long orderShopCount,
            BigDecimal orderAmount,
            long paidOrderCount,
            long paidShopCount,
            BigDecimal paidAmount) {

        this.userId = userId;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.orderCount = orderCount;
        this.orderShopCount = orderShopCount;

        this.orderAmount =
                orderAmount == null
                        ? BigDecimal.ZERO
                        : orderAmount;

        this.paidOrderCount = paidOrderCount;
        this.paidShopCount = paidShopCount;

        this.paidAmount =
                paidAmount == null
                        ? BigDecimal.ZERO
                        : paidAmount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public long getOrderShopCount() {
        return orderShopCount;
    }

    public void setOrderShopCount(long orderShopCount) {
        this.orderShopCount = orderShopCount;
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

    public long getPaidShopCount() {
        return paidShopCount;
    }

    public void setPaidShopCount(long paidShopCount) {
        this.paidShopCount = paidShopCount;
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
        return "UserSummary{" +
                "userId=" + userId +
                ", windowStart='" + windowStart + '\'' +
                ", windowEnd='" + windowEnd + '\'' +
                ", orderCount=" + orderCount +
                ", orderShopCount=" + orderShopCount +
                ", orderAmount=" + orderAmount +
                ", paidOrderCount=" + paidOrderCount +
                ", paidShopCount=" + paidShopCount +
                ", paidAmount=" + paidAmount +
                '}';
    }
}