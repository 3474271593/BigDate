// 文件路径：src/main/java/org/example/bean/DwdOrderGoods.java
package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DWS层使用的DWD订单商品明细对象。
 *
 * 数据来源：
 * Kafka Topic：dwd_order_goods
 *
 * 这里只保留商品DWS主题当前需要使用的字段。
 */
public class DwdOrderGoods implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单商品明细ID。
     */
    private Long ogId;

    /**
     * 订单ID。
     *
     * 用于和dwd_order_info进行关联。
     */
    private Long orderId;

    /**
     * 商品ID。
     */
    private Long goodsId;

    /**
     * 商品名称。
     *
     * 商品维度补全失败时可能为空，
     * 不能因为名称为空而丢弃商品事实数据。
     */
    private String goodsName;

    /**
     * 商品数量。
     */
    private Integer goodsNum;

    /**
     * 商品单价。
     */
    private BigDecimal goodsPrice;

    /**
     * 商品明细金额。
     *
     * DWD已经计算：
     * goodsAmount = goodsPrice * goodsNum
     */
    private BigDecimal goodsAmount;

    /**
     * 商品规格ID。
     */
    private Long goodsSpecId;

    /**
     * 商品规格名称。
     */
    private String goodsSpecNames;

    /**
     * 商品类型。
     */
    private Integer goodsType;

    /**
     * 佣金比例。
     */
    private BigDecimal commissionRate;

    /**
     * 商品图片。
     */
    private String goodsImg;

    /**
     * CDC操作类型。
     *
     * 例如：
     * bootstrap
     * INSERT
     * UPDATE
     */
    private String opType;

    /**
     * DWD处理时间戳。
     *
     * 注意：
     * 该字段不是业务Event Time。
     * 商品主题最终使用订单createTime/payTime作为业务时间。
     */
    private Long processTs;

    /**
     * DWD数据质量告警。
     *
     * 例如：
     * GOODS_NAME_DIM_MISSING
     */
    private String qualityWarning;

    public DwdOrderGoods() {
    }

    public Long getOgId() {
        return ogId;
    }

    public void setOgId(Long ogId) {
        this.ogId = ogId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public Integer getGoodsNum() {
        return goodsNum;
    }

    public void setGoodsNum(Integer goodsNum) {
        this.goodsNum = goodsNum;
    }

    public BigDecimal getGoodsPrice() {
        return goodsPrice;
    }

    public void setGoodsPrice(BigDecimal goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

    public BigDecimal getGoodsAmount() {
        return goodsAmount;
    }

    public void setGoodsAmount(BigDecimal goodsAmount) {
        this.goodsAmount = goodsAmount;
    }

    public Long getGoodsSpecId() {
        return goodsSpecId;
    }

    public void setGoodsSpecId(Long goodsSpecId) {
        this.goodsSpecId = goodsSpecId;
    }

    public String getGoodsSpecNames() {
        return goodsSpecNames;
    }

    public void setGoodsSpecNames(String goodsSpecNames) {
        this.goodsSpecNames = goodsSpecNames;
    }

    public Integer getGoodsType() {
        return goodsType;
    }

    public void setGoodsType(Integer goodsType) {
        this.goodsType = goodsType;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public String getGoodsImg() {
        return goodsImg;
    }

    public void setGoodsImg(String goodsImg) {
        this.goodsImg = goodsImg;
    }

    public String getOpType() {
        return opType;
    }

    public void setOpType(String opType) {
        this.opType = opType;
    }

    public Long getProcessTs() {
        return processTs;
    }

    public void setProcessTs(Long processTs) {
        this.processTs = processTs;
    }

    public String getQualityWarning() {
        return qualityWarning;
    }

    public void setQualityWarning(String qualityWarning) {
        this.qualityWarning = qualityWarning;
    }

    @Override
    public String toString() {
        return "DwdOrderGoods{" +
                "ogId=" + ogId +
                ", orderId=" + orderId +
                ", goodsId=" + goodsId +
                ", goodsName='" + goodsName + '\'' +
                ", goodsNum=" + goodsNum +
                ", goodsPrice=" + goodsPrice +
                ", goodsAmount=" + goodsAmount +
                ", goodsSpecId=" + goodsSpecId +
                ", goodsSpecNames='" + goodsSpecNames + '\'' +
                ", goodsType=" + goodsType +
                ", commissionRate=" + commissionRate +
                ", goodsImg='" + goodsImg + '\'' +
                ", opType='" + opType + '\'' +
                ", processTs=" + processTs +
                ", qualityWarning='" + qualityWarning + '\'' +
                '}';
    }
}