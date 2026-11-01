package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DWD订单商品明细实体。
 *
 * 数据粒度：
 * 一条记录 = 一条订单商品明细。
 *
 * 主键：
 * ogId
 *
 * 注意：
 * orderId + goodsId 不能作为唯一键，
 * 因为真实业务数据中存在相同 orderId、goodsId，
 * 但 ogId 不同的多条商品明细。
 */
public class DwdOrderGoods implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单商品明细主键。
     */
    private Long ogId;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 商品ID。
     */
    private Long goodsId;

    /**
     * 商品数量。
     */
    private Long goodsNum;

    /**
     * 商品下单价格。
     */
    private BigDecimal goodsPrice;

    /**
     * 商品明细金额。
     *
     * goodsAmount = goodsNum * goodsPrice
     */
    private BigDecimal goodsAmount;

    /**
     * 商品规格ID。
     */
    private Integer goodsSpecId;

    /**
     * 商品规格名称。
     */
    private String goodsSpecNames;

    /**
     * 商品名称。
     *
     * 优先使用order_goods中的订单商品快照名称。
     *
     * 当原始goodsName缺失时，
     * 后续通过goodsId关联Redis商品DIM尝试补全。
     */
    private String goodsName;

    /**
     * 商品图片。
     */
    private String goodsImg;

    /**
     * 扩展JSON。
     */
    private String extraJson;

    /**
     * 商品类型。
     *
     * 当前原始表没有明确业务枚举注释，
     * 因此DWD层只保留原值，不做枚举推断。
     */
    private Integer goodsType;

    /**
     * 商品佣金比例。
     */
    private BigDecimal commissionRate;

    /**
     * 商品编码。
     */
    private String goodsCode;

    /**
     * 促销信息JSON。
     */
    private String promotionJson;

    /**
     * ODS操作类型。
     *
     * 例如：
     * bootstrap
     * INSERT
     * UPDATE
     * DELETE
     */
    private String opType;

    /**
     * 数据质量告警。
     *
     * 当前可能值：
     *
     * GOODS_NAME_DIM_MISSING
     * Redis中不存在对应商品名称。
     *
     * GOODS_NAME_DIM_LOOKUP_FAILED
     * Redis维度查询发生异常。
     *
     * 注意：
     * 质量告警不等于Dirty。
     * 出现质量告警时仍然保留交易事实。
     */
    private String qualityWarning;

    /**
     * Flink处理时间戳。
     */
    private Long processTs;

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

    public Long getGoodsNum() {
        return goodsNum;
    }

    public void setGoodsNum(Long goodsNum) {
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

    public Integer getGoodsSpecId() {
        return goodsSpecId;
    }

    public void setGoodsSpecId(Integer goodsSpecId) {
        this.goodsSpecId = goodsSpecId;
    }

    public String getGoodsSpecNames() {
        return goodsSpecNames;
    }

    public void setGoodsSpecNames(String goodsSpecNames) {
        this.goodsSpecNames = goodsSpecNames;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getGoodsImg() {
        return goodsImg;
    }

    public void setGoodsImg(String goodsImg) {
        this.goodsImg = goodsImg;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
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

    public String getGoodsCode() {
        return goodsCode;
    }

    public void setGoodsCode(String goodsCode) {
        this.goodsCode = goodsCode;
    }

    public String getPromotionJson() {
        return promotionJson;
    }

    public void setPromotionJson(String promotionJson) {
        this.promotionJson = promotionJson;
    }

    public String getOpType() {
        return opType;
    }

    public void setOpType(String opType) {
        this.opType = opType;
    }

    public String getQualityWarning() {
        return qualityWarning;
    }

    public void setQualityWarning(String qualityWarning) {
        this.qualityWarning = qualityWarning;
    }

    public Long getProcessTs() {
        return processTs;
    }

    public void setProcessTs(Long processTs) {
        this.processTs = processTs;
    }

    @Override
    public String toString() {
        return "DwdOrderGoods{" +
                "ogId=" + ogId +
                ", orderId=" + orderId +
                ", goodsId=" + goodsId +
                ", goodsNum=" + goodsNum +
                ", goodsPrice=" + goodsPrice +
                ", goodsAmount=" + goodsAmount +
                ", goodsSpecId=" + goodsSpecId +
                ", goodsSpecNames='" + goodsSpecNames + '\'' +
                ", goodsName='" + goodsName + '\'' +
                ", goodsImg='" + goodsImg + '\'' +
                ", extraJson='" + extraJson + '\'' +
                ", goodsType=" + goodsType +
                ", commissionRate=" + commissionRate +
                ", goodsCode='" + goodsCode + '\'' +
                ", promotionJson='" + promotionJson + '\'' +
                ", opType='" + opType + '\'' +
                ", qualityWarning='" + qualityWarning + '\'' +
                ", processTs=" + processTs +
                '}';
    }
}