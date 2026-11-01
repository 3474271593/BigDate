package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DWS 店铺主题业务变化事件。
 *
 * ShopDelta 并不是最终的店铺统计结果，
 * 而是订单状态变化后产生的店铺指标增量。
 *
 * 当前支持两种事件：
 *
 * 1. SHOP_ORDER
 *    表示产生一笔新的下单事实。
 *
 * 2. SHOP_PAY
 *    表示产生一笔新的支付事实。
 *
 * Event Time：
 *
 * SHOP_ORDER -> createTime
 * SHOP_PAY   -> payTime
 *
 * processTs 不能替代业务 Event Time。
 */
public class ShopDelta implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单 ID。
     */
    private Long orderId;

    /**
     * 店铺 ID。
     *
     * 后续窗口统计：
     *
     * keyBy(ShopDelta::getShopId)
     */
    private Long shopId;

    /**
     * 用户 ID。
     *
     * 后续用于计算：
     *
     * orderUserCount
     * paidUserCount
     *
     * 即店铺窗口 UV。
     */
    private Long userId;

    /**
     * 下单订单数增量。
     *
     * SHOP_ORDER = 1
     * SHOP_PAY   = 0
     */
    private long orderCountDelta;

    /**
     * 下单金额增量。
     *
     * SHOP_ORDER：
     * realTotalMoney
     *
     * SHOP_PAY：
     * 0
     */
    private BigDecimal orderAmountDelta = BigDecimal.ZERO;

    /**
     * 支付订单数增量。
     *
     * SHOP_ORDER = 0
     * SHOP_PAY   = 1
     */
    private long paidOrderCountDelta;

    /**
     * 支付金额增量。
     *
     * SHOP_ORDER：
     * 0
     *
     * SHOP_PAY：
     * realTotalMoney
     */
    private BigDecimal paidAmountDelta = BigDecimal.ZERO;

    /**
     * 业务事件类型。
     *
     * SHOP_ORDER
     * SHOP_PAY
     */
    private String eventType;

    /**
     * 原始业务事件时间。
     *
     * SHOP_ORDER -> createTime
     * SHOP_PAY   -> payTime
     */
    private String eventTime;

    /**
     * 解析后的 Event Time 毫秒时间戳。
     *
     * 后续 Flink Watermark 使用。
     */
    private long eventTs;

    public ShopDelta() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public long getOrderCountDelta() {
        return orderCountDelta;
    }

    public void setOrderCountDelta(long orderCountDelta) {
        this.orderCountDelta = orderCountDelta;
    }

    public BigDecimal getOrderAmountDelta() {
        return orderAmountDelta;
    }

    public void setOrderAmountDelta(BigDecimal orderAmountDelta) {
        this.orderAmountDelta =
                orderAmountDelta == null
                        ? BigDecimal.ZERO
                        : orderAmountDelta;
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
        return "ShopDelta{" +
                "orderId=" + orderId +
                ", shopId=" + shopId +
                ", userId=" + userId +
                ", orderCountDelta=" + orderCountDelta +
                ", orderAmountDelta=" + orderAmountDelta +
                ", paidOrderCountDelta=" + paidOrderCountDelta +
                ", paidAmountDelta=" + paidAmountDelta +
                ", eventType='" + eventType + '\'' +
                ", eventTime='" + eventTime + '\'' +
                ", eventTs=" + eventTs +
                '}';
    }
}