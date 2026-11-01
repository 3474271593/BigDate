package org.example.bean;

import java.io.Serializable;

/**
 * Trade source record with the Kafka position needed for one-time state warm-up.
 */
public class TradeSourceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private DwdOrderInfo order;
    private int partition;
    private long offset;

    public TradeSourceRecord() {
    }

    public TradeSourceRecord(DwdOrderInfo order, int partition, long offset) {
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
}
