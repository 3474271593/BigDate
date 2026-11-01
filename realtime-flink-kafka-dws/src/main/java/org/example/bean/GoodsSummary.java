// 文件路径：src/main/java/org/example/bean/GoodsSummary.java
package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DWS商品主题一分钟窗口聚合结果。
 */
public class GoodsSummary implements Serializable {

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
     * 商品ID。
     */
    private Long goodsId;

    /**
     * 商品名称。
     */
    private String goodsName;

    /**
     * 下单订单数。
     */
    private long orderCount;

    /**
     * 下单商品件数。
     */
    private long orderGoodsNum;

    /**
     * 下单商品金额。
     */
    private BigDecimal orderAmount = BigDecimal.ZERO;

    /**
     * 支付订单数。
     */
    private long paidOrderCount;

    /**
     * 支付商品件数。
     */
    private long paidGoodsNum;

    /**
     * 支付商品金额。
     */
    private BigDecimal paidAmount = BigDecimal.ZERO;

    public GoodsSummary() {
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

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public long getOrderGoodsNum() {
        return orderGoodsNum;
    }

    public void setOrderGoodsNum(long orderGoodsNum) {
        this.orderGoodsNum = orderGoodsNum;
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

    public long getPaidGoodsNum() {
        return paidGoodsNum;
    }

    public void setPaidGoodsNum(long paidGoodsNum) {
        this.paidGoodsNum = paidGoodsNum;
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
        return "GoodsSummary{" +
                "windowStart='" + windowStart + '\'' +
                ", windowEnd='" + windowEnd + '\'' +
                ", goodsId=" + goodsId +
                ", goodsName='" + goodsName + '\'' +
                ", orderCount=" + orderCount +
                ", orderGoodsNum=" + orderGoodsNum +
                ", orderAmount=" + orderAmount +
                ", paidOrderCount=" + paidOrderCount +
                ", paidGoodsNum=" + paidGoodsNum +
                ", paidAmount=" + paidAmount +
                '}';
    }
}