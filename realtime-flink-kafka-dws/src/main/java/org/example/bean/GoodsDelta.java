// 文件路径：src/main/java/org/example/bean/GoodsDelta.java
package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品主题指标变化事件。
 *
 * 一个GoodsDelta表示某个商品对DWS商品指标产生的一次实际变化。
 *
 * 当前事件类型：
 *
 * GOODS_ORDER：
 * 下单商品指标增加。
 *
 * GOODS_PAY：
 * 支付商品指标增加。
 */
public class GoodsDelta implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 商品明细ID。
     */
    private Long ogId;

    /**
     * 商品ID。
     */
    private Long goodsId;

    /**
     * 商品名称。
     */
    private String goodsName;

    /**
     * 店铺ID。
     */
    private Long shopId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 下单订单数变化。
     */
    private long orderCountDelta;

    /**
     * 下单商品件数变化。
     */
    private long orderGoodsNumDelta;

    /**
     * 下单商品金额变化。
     */
    private BigDecimal orderAmountDelta = BigDecimal.ZERO;

    /**
     * 支付订单数变化。
     */
    private long paidOrderCountDelta;

    /**
     * 支付商品件数变化。
     */
    private long paidGoodsNumDelta;

    /**
     * 支付商品金额变化。
     */
    private BigDecimal paidAmountDelta = BigDecimal.ZERO;

    /**
     * 事件类型。
     *
     * GOODS_ORDER
     * GOODS_PAY
     */
    private String eventType;

    /**
     * 业务事件时间。
     *
     * GOODS_ORDER使用订单createTime。
     * GOODS_PAY使用订单payTime。
     */
    private String eventTime;

    /**
     * 业务事件时间戳，单位毫秒。
     */
    private long eventTs;

    public GoodsDelta() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOgId() {
        return ogId;
    }

    public void setOgId(Long ogId) {
        this.ogId = ogId;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
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

    public long getOrderGoodsNumDelta() {
        return orderGoodsNumDelta;
    }

    public void setOrderGoodsNumDelta(long orderGoodsNumDelta) {
        this.orderGoodsNumDelta = orderGoodsNumDelta;
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

    public long getPaidGoodsNumDelta() {
        return paidGoodsNumDelta;
    }

    public void setPaidGoodsNumDelta(long paidGoodsNumDelta) {
        this.paidGoodsNumDelta = paidGoodsNumDelta;
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
        return "GoodsDelta{" +
                "orderId=" + orderId +
                ", ogId=" + ogId +
                ", goodsId=" + goodsId +
                ", goodsName='" + goodsName + '\'' +
                ", shopId=" + shopId +
                ", userId=" + userId +
                ", orderCountDelta=" + orderCountDelta +
                ", orderGoodsNumDelta=" + orderGoodsNumDelta +
                ", orderAmountDelta=" + orderAmountDelta +
                ", paidOrderCountDelta=" + paidOrderCountDelta +
                ", paidGoodsNumDelta=" + paidGoodsNumDelta +
                ", paidAmountDelta=" + paidAmountDelta +
                ", eventType='" + eventType + '\'' +
                ", eventTime='" + eventTime + '\'' +
                ", eventTs=" + eventTs +
                '}';
    }
}