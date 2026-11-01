// 文件路径：src/main/java/org/example/bean/DwdOrderInfo.java
package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DWS层使用的DWD订单对象。
 *
 * 数据来源：
 * Kafka Topic：dwd_order_info
 *
 * 注意：
 * DWS项目不直接依赖DWD项目中的Java Bean，
 * Kafka JSON作为DWD与DWS之间的数据契约。
 */
public class DwdOrderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 店铺ID。
     */
    private Long shopId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 实际订单金额。
     */
    private BigDecimal realTotalMoney;

    /**
     * 下单时间。
     *
     * 后续ORDER_CREATE事件使用该字段作为Event Time。
     */
    private String createTime;

    /**
     * 是否支付。
     *
     * 0：未支付
     * 1：已支付
     *
     * DWS判断支付状态时以isPay为准，
     * 不根据payTime反推支付状态。
     */
    private Integer isPay;

    /**
     * 有效支付时间。
     *
     * DWD已经完成数据质量处理：
     * isPay=0时，即使原始数据存在支付时间，
     * 正式payTime也不会作为有效支付时间输出。
     */
    private String payTime;

    /**
     * CDC操作类型。
     *
     * 例如：
     * bootstrap
     * UPDATE
     */
    private String opType;

    /**
     * DWD处理时间戳。
     *
     * 该字段不是业务事件时间，
     * 后续窗口统计不会使用它代替createTime/payTime。
     */
    private Long processTs;

    public DwdOrderInfo() {
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

    public BigDecimal getRealTotalMoney() {
        return realTotalMoney;
    }

    public void setRealTotalMoney(BigDecimal realTotalMoney) {
        this.realTotalMoney = realTotalMoney;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Integer getIsPay() {
        return isPay;
    }

    public void setIsPay(Integer isPay) {
        this.isPay = isPay;
    }

    public String getPayTime() {
        return payTime;
    }

    public void setPayTime(String payTime) {
        this.payTime = payTime;
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

    @Override
    public String toString() {
        return "DwdOrderInfo{" +
                "orderId=" + orderId +
                ", shopId=" + shopId +
                ", userId=" + userId +
                ", realTotalMoney=" + realTotalMoney +
                ", createTime='" + createTime + '\'' +
                ", isPay=" + isPay +
                ", payTime='" + payTime + '\'' +
                ", opType='" + opType + '\'' +
                ", processTs=" + processTs +
                '}';
    }
}