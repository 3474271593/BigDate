// 文件路径：src/main/java/org/example/bean/UserDelta.java
package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * User DWS 用户主题增量事件。
 *
 * 一个 UserDelta 表示订单状态变化对用户主题产生的一次业务贡献。
 *
 * 当前 V1 支持两种事件：
 *
 * USER_ORDER
 *      用户下单
 *      Event Time = createTime
 *
 * USER_PAY
 *      用户支付
 *      Event Time = payTime
 *
 * 后续按照：
 *
 * userId + 1分钟
 *
 * 进行窗口聚合。
 */
public class UserDelta implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID。
     *
     * User DWS 的聚合 Key。
     */
    private Long userId;

    /**
     * 店铺ID。
     *
     * 用于计算：
     *
     * orderShopCount
     * paidShopCount
     */
    private Long shopId;

    /**
     * 事件类型。
     *
     * USER_ORDER
     * USER_PAY
     */
    private String eventType;

    /**
     * Event Time，毫秒时间戳。
     *
     * USER_ORDER -> createTime
     * USER_PAY   -> payTime
     */
    private long eventTs;

    /**
     * 下单贡献。
     *
     * USER_ORDER = 1
     * USER_PAY   = 0
     */
    private long orderCount;

    /**
     * 下单金额贡献。
     */
    private BigDecimal orderAmount;

    /**
     * 支付订单贡献。
     *
     * USER_ORDER = 0
     * USER_PAY   = 1
     */
    private long paidOrderCount;

    /**
     * 支付金额贡献。
     */
    private BigDecimal paidAmount;

    public UserDelta() {
        this.orderAmount = BigDecimal.ZERO;
        this.paidAmount = BigDecimal.ZERO;
    }

    public UserDelta(
            Long userId,
            Long shopId,
            String eventType,
            long eventTs,
            long orderCount,
            BigDecimal orderAmount,
            long paidOrderCount,
            BigDecimal paidAmount) {

        this.userId = userId;
        this.shopId = shopId;
        this.eventType = eventType;
        this.eventTs = eventTs;
        this.orderCount = orderCount;

        this.orderAmount =
                orderAmount == null
                        ? BigDecimal.ZERO
                        : orderAmount;

        this.paidOrderCount = paidOrderCount;

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

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getEventTs() {
        return eventTs;
    }

    public void setEventTs(long eventTs) {
        this.eventTs = eventTs;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
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
        return "UserDelta{" +
                "userId=" + userId +
                ", shopId=" + shopId +
                ", eventType='" + eventType + '\'' +
                ", eventTs=" + eventTs +
                ", orderCount=" + orderCount +
                ", orderAmount=" + orderAmount +
                ", paidOrderCount=" + paidOrderCount +
                ", paidAmount=" + paidAmount +
                '}';
    }
}