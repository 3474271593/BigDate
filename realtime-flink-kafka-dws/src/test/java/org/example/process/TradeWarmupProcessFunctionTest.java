package org.example.process;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeDelta;
import org.example.bean.TradeSourceRecord;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TradeWarmupProcessFunctionTest {

    @Test
    public void shouldWarmStateBeforeBoundaryAndEmitOnlyRealtimePaymentChange() throws Exception {
        Map<Integer, Long> boundaries = new HashMap<>();
        boundaries.put(0, 20028L);
        TradeWarmupProcessFunction function = new TradeWarmupProcessFunction(boundaries);
        KeyedProcessOperator<Long, TradeSourceRecord, TradeDelta> operator =
                new KeyedProcessOperator<>(function);
        KeyedOneInputStreamOperatorTestHarness<Long, TradeSourceRecord, TradeDelta> harness =
                new KeyedOneInputStreamOperatorTestHarness<>(
                        operator,
                        record -> record.getOrder().getOrderId(),
                        Types.LONG
                );

        try {
            harness.open();
            harness.processElement(record(0, 10L, order(7L, 0, null)), 1L);
            Assert.assertTrue(values(harness).isEmpty());

            harness.processElement(record(0, 20028L,
                    order(7L, 1, "2019-09-05 19:53:20")), 2L);
            List<TradeDelta> output = values(harness);
            Assert.assertEquals(1, output.size());
            Assert.assertEquals("ORDER_PAY", output.get(0).getEventType());
        } finally {
            harness.close();
        }
    }

    @Test
    public void shouldTreatOffsetAtBoundaryAsRealtimeForEmptyPartition() throws Exception {
        Map<Integer, Long> boundaries = new HashMap<>();
        boundaries.put(1, 0L);
        TradeWarmupProcessFunction function = new TradeWarmupProcessFunction(boundaries);
        KeyedProcessOperator<Long, TradeSourceRecord, TradeDelta> operator =
                new KeyedProcessOperator<>(function);
        KeyedOneInputStreamOperatorTestHarness<Long, TradeSourceRecord, TradeDelta> harness =
                new KeyedOneInputStreamOperatorTestHarness<>(
                        operator,
                        record -> record.getOrder().getOrderId(),
                        Types.LONG
                );

        try {
            harness.open();
            harness.processElement(record(1, 0L, order(8L, 0, null)), 1L);
            List<TradeDelta> output = values(harness);
            Assert.assertEquals(1, output.size());
            Assert.assertEquals("ORDER_CREATE", output.get(0).getEventType());
        } finally {
            harness.close();
        }
    }

    private List<TradeDelta> values(
            KeyedOneInputStreamOperatorTestHarness<Long, TradeSourceRecord, TradeDelta> harness) {
        return harness.extractOutputStreamRecords().stream()
                .map(record -> record.getValue())
                .collect(Collectors.toList());
    }

    private TradeSourceRecord record(int partition, long offset, DwdOrderInfo order) {
        return new TradeSourceRecord(order, partition, offset);
    }

    private DwdOrderInfo order(Long id, int isPay, String payTime) {
        DwdOrderInfo order = new DwdOrderInfo();
        order.setOrderId(id);
        order.setShopId(1L);
        order.setUserId(2L);
        order.setCreateTime("2019-09-05 19:50:00");
        order.setIsPay(isPay);
        order.setPayTime(payTime);
        order.setRealTotalMoney(new BigDecimal("88.80"));
        return order;
    }
}
