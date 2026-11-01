// 文件路径：src/main/java/org/example/bean/TradeDelta.java
package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DWS交易指标变化事件。
 *
 * 该对象表示一次订单CDC变化对DWS交易指标产生的实际影响。
 *
 * 例如：
 *
 * 1. 新订单：
 *    orderCountDelta = 1
 *    paidOrderCountDelta = 0
 *    paidAmountDelta = 0
 *    eventType = ORDER_CREATE
 *    eventTime = createTime
 *
 * 2. 未支付 -> 已支付：
 *    orderCountDelta = 0
 *    paidOrderCountDelta = 1
 *    paidAmountDelta = realTotalMoney
 *    eventType = ORDER_PAY
 *    eventTime = payTime
 *
 * 3. 只修改地址：
 *    不产生有效TradeDelta。
 */
public class TradeDelta implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 店铺ID。
     */
    private Long shopId;

    /**
     * 订单量变化。
     */
    private long orderCountDelta;

    /**
     * 支付订单量变化。
     */
    private long paidOrderCountDelta;

    /**
     * 支付金额变化。
     */
    private BigDecimal paidAmountDelta = BigDecimal.ZERO;

    /**
     * 事件类型。
     *
     * 当前阶段：
     * ORDER_CREATE
     * ORDER_PAY
     */
    private String eventType;

    /**
     * 业务事件时间字符串。
     *
     * ORDER_CREATE使用createTime。
     * ORDER_PAY使用payTime。
     */
    private String eventTime;

    /**
     * 业务事件时间戳，单位毫秒。
     *
     * 后续Flink Event Time和Watermark直接使用该字段。
     */
    private long eventTs;

    public TradeDelta() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public long getOrderCountDelta() {
        return orderCountDelta;
    }

    public void setOrderCountDelta(long orderCountDelta) {
        this.orderCountDelta = orderCountDelta;
    }

    public long getPaidOrderCountDelta() {
        return paidOrderCountDelta;
    }

    public void setPaidOrderCountDelta(long paidOrderCountDelta) {
        this.paidOrderCountDelta = paidOrderCountDelta;
    }

    public BigDecimal getPaidAmountDelta() {
        return paidAmountDelta;
    }

    public void setPaidAmountDelta(BigDecimal paidAmountDelta) {
        this.paidAmountDelta =
                paidAmountDelta == null
                        ? BigDecimal.ZERO
                        : paidAmountDelta;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public long getEventTs() {
        return eventTs;
    }

    public void setEventTs(long eventTs) {
        this.eventTs = eventTs;
    }

    @Override
    public String toString() {
        return "TradeDelta{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", shopId=" + shopId +
                ", orderCountDelta=" + orderCountDelta +
                ", paidOrderCountDelta=" + paidOrderCountDelta +
                ", paidAmountDelta=" + paidAmountDelta +
                ", eventType='" + eventType + '\'' +
                ", eventTime='" + eventTime + '\'' +
                ", eventTs=" + eventTs +
                '}';
    }
}