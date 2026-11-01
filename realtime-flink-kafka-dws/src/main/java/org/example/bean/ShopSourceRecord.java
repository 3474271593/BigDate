package org.example.bean;

import java.io.Serializable;

/**
 * Shop DWS Kafka Source Record。
 *
 * 除了保存反序列化后的 DwdOrderInfo，
 * 还保留该消息在 Kafka 中的 partition 和 offset。
 *
 * 用途：
 * 1. ShopBootstrapApp 判断历史读取边界；
 * 2. ShopApp 判断 realtime boundary；
 * 3. realtime boundary 之前只 warmup previous-order-state；
 * 4. boundary 之后才真正产生 ShopDelta。
 */
public class ShopSourceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private DwdOrderInfo order;

    private int partition;

    private long offset;

    public ShopSourceRecord() {
    }

    public ShopSourceRecord(
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

    public void setOrder(DwdOrderInfo order) {
        this.order = order;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "ShopSourceRecord{" +
                "order=" + order +
                ", partition=" + partition +
                ", offset=" + offset +
                '}';
    }
}