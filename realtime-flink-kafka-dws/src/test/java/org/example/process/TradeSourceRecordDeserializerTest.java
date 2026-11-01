package org.example.process;

import org.example.bean.TradeSourceRecord;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class TradeSourceRecordDeserializerTest {

    @Test
    public void shouldPreserveKafkaPartitionAndOffset() throws Exception {
        String json = "{\"orderId\":7,\"createTime\":\"2019-09-05 19:50:00\","
                + "\"isPay\":0,\"opType\":\"bootstrap\"}";

        TradeSourceRecord record = new TradeSourceRecordDeserializer().deserialize(
                null,
                json.getBytes(StandardCharsets.UTF_8),
                "dwd_order_info",
                2,
                123L
        );

        Assert.assertNotNull(record);
        Assert.assertEquals(2, record.getPartition());
        Assert.assertEquals(123L, record.getOffset());
        Assert.assertEquals(Long.valueOf(7L), record.getOrder().getOrderId());
    }

    @Test
    public void shouldDropMalformedOrderJson() throws Exception {
        TradeSourceRecord record = new TradeSourceRecordDeserializer().deserialize(
                null,
                "not-json".getBytes(StandardCharsets.UTF_8),
                "dwd_order_info",
                0,
                1L
        );
        Assert.assertNull(record);
    }
}
