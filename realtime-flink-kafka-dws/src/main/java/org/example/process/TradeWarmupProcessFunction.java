package org.example.process;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeDelta;
import org.example.bean.TradeSourceRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Restores previous-order-state from the fixed bootstrap range without emitting it.
 */
public class TradeWarmupProcessFunction
        extends KeyedProcessFunction<Long, TradeSourceRecord, TradeDelta> {

    private final Map<Integer, Long> realtimeStartOffsets;
    private transient ValueState<DwdOrderInfo> previousOrderState;

    public TradeWarmupProcessFunction(Map<Integer, Long> realtimeStartOffsets) {
        if (realtimeStartOffsets == null) {
            throw new IllegalArgumentException("realtimeStartOffsets must not be null");
        }
        this.realtimeStartOffsets = Collections.unmodifiableMap(
                new HashMap<>(realtimeStartOffsets)
        );
    }

    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<DwdOrderInfo> descriptor =
                new ValueStateDescriptor<>(
                        "previous-order-state",
                        DwdOrderInfo.class
                );
        previousOrderState = getRuntimeContext().getState(descriptor);
    }

    @Override
    public void processElement(
            TradeSourceRecord record,
            Context context,
            Collector<TradeDelta> out) throws Exception {

        if (record == null || record.getOrder() == null
                || record.getOrder().getOrderId() == null) {
            return;
        }

        DwdOrderInfo current = record.getOrder();
        Long realtimeStart = realtimeStartOffsets.get(record.getPartition());
        if (realtimeStart == null) {
            throw new IllegalStateException(
                    "Missing realtime start offset for partition " + record.getPartition()
            );
        }

        if (record.getOffset() < realtimeStart) {
            previousOrderState.update(current);
            return;
        }

        DwdOrderInfo previous = previousOrderState.value();
        List<TradeDelta> deltas = TradeDeltaCalculator.calculate(previous, current);
        for (TradeDelta delta : deltas) {
            if (delta != null) {
                out.collect(delta);
            }
        }
        previousOrderState.update(current);
    }
}
