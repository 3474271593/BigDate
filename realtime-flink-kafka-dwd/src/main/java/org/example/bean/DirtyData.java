package org.example.bean;

import java.io.Serializable;

/**
 * DWD 脏数据对象。
 *
 * 作用：
 *
 * 当 ODS 数据在清洗过程中发生异常时，
 * 不直接把异常数据丢掉，
 * 而是把：
 *
 * 1. 来源表
 * 2. 操作类型
 * 3. 异常类型
 * 4. 异常信息
 * 5. 原始数据
 * 6. 处理时间
 *
 * 保存下来。
 *
 * 后续将写入 Kafka：
 *
 * dwd_dirty_data
 */
public class DirtyData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 来源表。
     *
     * 例如：
     *
     * orders
     * order_goods
     * order_refunds
     */
    private String tableName;

    /**
     * ODS 操作类型。
     *
     * 例如：
     *
     * bootstrap
     * insert
     * update
     * delete
     */
    private String opType;

    /**
     * 异常类型。
     *
     * 例如：
     *
     * IllegalArgumentException
     * NumberFormatException
     */
    private String errorType;

    /**
     * 具体异常信息。
     *
     * 例如：
     *
     * orderId非法，orderId=0
     */
    private String errorMessage;

    /**
     * Kafka 中原始 ODS JSON。
     *
     * 必须保留，
     * 方便后续定位到底是哪一条原始消息出错。
     */
    private String rawData;

    /**
     * DWD 处理时间。
     *
     * System.currentTimeMillis()
     */
    private Long processTs;


    public DirtyData() {
    }


    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }


    public String getOpType() {
        return opType;
    }

    public void setOpType(String opType) {
        this.opType = opType;
    }


    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }


    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }


    public Long getProcessTs() {
        return processTs;
    }

    public void setProcessTs(Long processTs) {
        this.processTs = processTs;
    }


    /**
     * 方便本地 print() 查看。
     */
    @Override
    public String toString() {
        return "DirtyData{" +
                "tableName='" + tableName + '\'' +
                ", opType='" + opType + '\'' +
                ", errorType='" + errorType + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", rawData='" + rawData + '\'' +
                ", processTs=" + processTs +
                '}';
    }
}