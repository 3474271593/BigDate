package org.example.bean;

import java.io.Serializable;

/**
 * User DWS Kafka Source Record。
 *
 * 除了保存反序列化后的 DwdOrderInfo，
 * 还保留该消息在 Kafka 中的 partition 和 offset。
 *
 * 用途：
 *
 * 1. UserBootstrapApp 判断历史读取边界；
 * 2. UserApp 判断 realtime boundary；
 * 3. realtime boundary 之前只 warmup previous-order-state；
 * 4. boundary 之后才真正产生 UserDelta。
 *
 * User V1 当前固定边界：
 *
 * partition 0: [0, 20030)
 * partition 1: [0, 0)
 * partition 2: [0, 0)
 *
 * 注意：
 * 具体边界值由 UserBootstrapConfig 管理，
 * 本类只负责携带 Kafka partition / offset，
 * 不在这里写死边界。
 */
public class UserSourceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Kafka 消息反序列化后的 DWD 订单。
     */
    private DwdOrderInfo order;

    /**
     * Kafka partition。
     */
    private int partition;

    /**
     * Kafka offset。
     */
    private long offset;

    public UserSourceRecord() {
    }

    public UserSourceRecord(
            DwdOrderInfo order,
            int partition,
            long offset) {

        this.order = order;
        this.partition = partition;
        this.offset = offset;
    }

    public DwdOrderInfo getOrder() {
        return order;
    }

    public void setOrder(
            DwdOrderInfo order) {

        this.order = order;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(
            int partition) {

        this.partition = partition;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(
            long offset) {

        this.offset = offset;
    }

    @Override
    public String toString() {

        return "UserSourceRecord{" +
                "order=" + order +
                ", partition=" + partition +
                ", offset=" + offset +
                '}';
    }
}