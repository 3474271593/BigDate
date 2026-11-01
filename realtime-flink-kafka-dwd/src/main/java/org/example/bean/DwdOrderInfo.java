package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DWD 订单事实明细实体。
 *
 * 数据来源：
 * MySQL orders
 *      ↓
 * Canal / Bootstrap
 *      ↓
 * Kafka ods_shop_db
 *      ↓
 * Flink 清洗
 *      ↓
 * DwdOrderInfo
 *
 * 设计原则：
 * 1. 保留订单分析需要的核心业务字段。
 * 2. 不保留手机号、详细地址等敏感信息。
 * 3. 金额统一使用 BigDecimal。
 * 4. 原始支付时间和清洗后的支付时间分开保存。
 * 5. 可疑业务数据不直接删除，通过 qualityWarning 标记。
 */
public class DwdOrderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================
    // 1. 订单基础字段
    // =========================

    /**
     * 订单主键。
     *
     * orders.orderId
     *
     * DWD 中订单事实的核心业务主键。
     */
    private Long orderId;

    /**
     * 订单编号。
     *
     * orders.orderNo
     *
     * 注意：
     * orderNo 不作为 DWD 唯一主键，
     * 订单唯一性以 orderId 为准。
     */
    private String orderNo;

    /**
     * 店铺 ID。
     */
    private Long shopId;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 订单状态。
     *
     * -3：用户拒收
     * -2：未付款订单
     * -1：用户取消
     *  0：待发货
     *  1：配送中
     *  2：用户确认收货
     */
    private Integer orderStatus;


    // =========================
    // 2. 金额字段
    // =========================

    /**
     * 商品总金额。
     */
    private BigDecimal goodsMoney;

    /**
     * 运费。
     */
    private BigDecimal deliverMoney;

    /**
     * 订单总金额。
     *
     * 一般包含商品金额和运费。
     */
    private BigDecimal totalMoney;

    /**
     * 实际订单金额。
     *
     * 可能经过优惠、折扣等业务处理，
     * 因此不能强制要求等于 totalMoney。
     */
    private BigDecimal realTotalMoney;

    /**
     * 应支付金额。
     */
    private BigDecimal needPay;

    /**
     * 佣金金额。
     */
    private BigDecimal commissionFee;

    /**
     * 积分抵扣金额。
     */
    private BigDecimal scoreMoney;

    /**
     * 使用积分。
     */
    private Integer useScore;

    /**
     * 总支付手续费。
     */
    private BigDecimal totalPayFee;


    // =========================
    // 3. 支付字段
    // =========================

    /**
     * 支付方式。
     *
     * 0：未知
     * 1：支付宝
     * 2：微信
     * 3：现金
     * 4：其他
     */
    private Integer payType;

    /**
     * 是否支付。
     *
     * 0：未支付
     * 1：已支付
     *
     * DWD 判断是否支付时，
     * 以该字段作为主要业务依据。
     */
    private Integer isPay;

    /**
     * 源系统原始支付时间。
     *
     * 即使出现：
     *
     * isPay = 0
     * payTime != null
     *
     * 仍然保留源数据，方便数据追踪。
     */
    private String rawPayTime;

    /**
     * DWD 清洗后的支付时间。
     *
     * 正常情况下：
     *
     * isPay = 1
     *      ↓
     * payTime = rawPayTime
     *
     * isPay = 0
     *      ↓
     * payTime = null
     */
    private String payTime;

    /**
     * 支付流水号。
     */
    private String tradeNo;


    // =========================
    // 4. 配送字段
    // =========================

    /**
     * 配送方式。
     *
     * 0：送货上门
     * 1：自提
     */
    private Integer deliverType;

    /**
     * 收货区域 ID。
     */
    private Long areaId;

    /**
     * 区域路径。
     */
    private String areaIdPath;

    /**
     * 发货时间。
     */
    private String deliveryTime;

    /**
     * 收货时间。
     */
    private String receiveTime;


    // =========================
    // 5. 订单来源及类型
    // =========================

    /**
     * 订单来源。
     *
     * 0：商城
     * 1：微信
     * 2：手机版
     * 3：安卓 App
     * 4：苹果 App
     */
    private Integer orderSrc;

    /**
     * 订单类型。
     */
    private Integer orderType;

    /**
     * 订单业务编码。
     */
    private String orderCode;


    // =========================
    // 6. 退款 / 评价 / 完结状态
    // =========================

    /**
     * 是否退款。
     *
     * 0：否
     * 1：是
     */
    private Integer isRefund;

    /**
     * 是否评价。
     *
     * 0：未评价
     * 1：已评价
     */
    private Integer isAppraise;

    /**
     * 是否完结。
     *
     * 0：未完结
     * 1：已完结
     */
    private Integer isClosed;


    // =========================
    // 7. 数据状态及时间
    // =========================

    /**
     * 数据有效标志。
     *
     * 1：有效
     * -1：删除
     */
    private Integer dataFlag;

    /**
     * 订单创建时间。
     */
    private String createTime;


    // =========================
    // 8. ODS / DWD 元数据
    // =========================

    /**
     * ODS 操作类型。
     *
     * 当前历史初始化数据为：
     *
     * bootstrap
     *
     * 后续实时 CDC 可能出现：
     *
     * insert
     * update
     * delete
     */
    private String opType;

    /**
     * 数据质量警告。
     *
     * 示例：
     *
     * UNPAID_WITH_PAY_TIME
     *
     * 多个警告后续可以使用逗号分隔。
     */
    private String qualityWarning;

    /**
     * Flink DWD 处理时间。
     *
     * 使用毫秒时间戳。
     */
    private Long processTs;


    // ============================================================
    // Getter / Setter
    // ============================================================

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
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

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public BigDecimal getGoodsMoney() {
        return goodsMoney;
    }

    public void setGoodsMoney(BigDecimal goodsMoney) {
        this.goodsMoney = goodsMoney;
    }

    public BigDecimal getDeliverMoney() {
        return deliverMoney;
    }

    public void setDeliverMoney(BigDecimal deliverMoney) {
        this.deliverMoney = deliverMoney;
    }

    public BigDecimal getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(BigDecimal totalMoney) {
        this.totalMoney = totalMoney;
    }

    public BigDecimal getRealTotalMoney() {
        return realTotalMoney;
    }

    public void setRealTotalMoney(BigDecimal realTotalMoney) {
        this.realTotalMoney = realTotalMoney;
    }

    public BigDecimal getNeedPay() {
        return needPay;
    }

    public void setNeedPay(BigDecimal needPay) {
        this.needPay = needPay;
    }

    public BigDecimal getCommissionFee() {
        return commissionFee;
    }

    public void setCommissionFee(BigDecimal commissionFee) {
        this.commissionFee = commissionFee;
    }

    public BigDecimal getScoreMoney() {
        return scoreMoney;
    }

    public void setScoreMoney(BigDecimal scoreMoney) {
        this.scoreMoney = scoreMoney;
    }

    public Integer getUseScore() {
        return useScore;
    }

    public void setUseScore(Integer useScore) {
        this.useScore = useScore;
    }

    public BigDecimal getTotalPayFee() {
        return totalPayFee;
    }

    public void setTotalPayFee(BigDecimal totalPayFee) {
        this.totalPayFee = totalPayFee;
    }

    public Integer getPayType() {
        return payType;
    }

    public void setPayType(Integer payType) {
        this.payType = payType;
    }

    public Integer getIsPay() {
        return isPay;
    }

    public void setIsPay(Integer isPay) {
        this.isPay = isPay;
    }

    public String getRawPayTime() {
        return rawPayTime;
    }

    public void setRawPayTime(String rawPayTime) {
        this.rawPayTime = rawPayTime;
    }

    public String getPayTime() {
        return payTime;
    }

    public void setPayTime(String payTime) {
        this.payTime = payTime;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public Integer getDeliverType() {
        return deliverType;
    }

    public void setDeliverType(Integer deliverType) {
        this.deliverType = deliverType;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getAreaIdPath() {
        return areaIdPath;
    }

    public void setAreaIdPath(String areaIdPath) {
        this.areaIdPath = areaIdPath;
    }

    public String getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public String getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(String receiveTime) {
        this.receiveTime = receiveTime;
    }

    public Integer getOrderSrc() {
        return orderSrc;
    }

    public void setOrderSrc(Integer orderSrc) {
        this.orderSrc = orderSrc;
    }

    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Integer getIsRefund() {
        return isRefund;
    }

    public void setIsRefund(Integer isRefund) {
        this.isRefund = isRefund;
    }

    public Integer getIsAppraise() {
        return isAppraise;
    }

    public void setIsAppraise(Integer isAppraise) {
        this.isAppraise = isAppraise;
    }

    public Integer getIsClosed() {
        return isClosed;
    }

    public void setIsClosed(Integer isClosed) {
        this.isClosed = isClosed;
    }

    public Integer getDataFlag() {
        return dataFlag;
    }

    public void setDataFlag(Integer dataFlag) {
        this.dataFlag = dataFlag;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
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


    // ============================================================
    // toString
    // ============================================================

    @Override
    public String toString() {
        return "DwdOrderInfo{" +
                "orderId=" + orderId +
                ", orderNo='" + orderNo + '\'' +
                ", shopId=" + shopId +
                ", userId=" + userId +
                ", orderStatus=" + orderStatus +
                ", goodsMoney=" + goodsMoney +
                ", deliverMoney=" + deliverMoney +
                ", totalMoney=" + totalMoney +
                ", realTotalMoney=" + realTotalMoney +
                ", needPay=" + needPay +
                ", commissionFee=" + commissionFee +
                ", scoreMoney=" + scoreMoney +
                ", useScore=" + useScore +
                ", totalPayFee=" + totalPayFee +
                ", payType=" + payType +
                ", isPay=" + isPay +
                ", rawPayTime='" + rawPayTime + '\'' +
                ", payTime='" + payTime + '\'' +
                ", tradeNo='" + tradeNo + '\'' +
                ", deliverType=" + deliverType +
                ", areaId=" + areaId +
                ", areaIdPath='" + areaIdPath + '\'' +
                ", deliveryTime='" + deliveryTime + '\'' +
                ", receiveTime='" + receiveTime + '\'' +
                ", orderSrc=" + orderSrc +
                ", orderType=" + orderType +
                ", orderCode='" + orderCode + '\'' +
                ", isRefund=" + isRefund +
                ", isAppraise=" + isAppraise +
                ", isClosed=" + isClosed +
                ", dataFlag=" + dataFlag +
                ", createTime='" + createTime + '\'' +
                ", opType='" + opType + '\'' +
                ", qualityWarning='" + qualityWarning + '\'' +
                ", processTs=" + processTs +
                '}';
    }
}