package org.example.process;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.util.serialization.KeyedDeserializationSchema;
import org.example.bean.DwdOrderInfo;
import org.example.bean.TradeSourceRecord;

import java.nio.charset.StandardCharsets;

/** Kafka deserializer that keeps partition and offset for Trade state warm-up. */
public class TradeSourceRecordDeserializer
        implements KeyedDeserializationSchema<TradeSourceRecord> {

    @Override
    public TradeSourceRecord deserialize(byte[] messageKey, byte[] message,
                                         String topic, int partition, long offset) {
        if (message == null) {
            return null;
        }
        DwdOrderInfo order = DwdOrderInfoDeserializer.deserialize(
                new String(message, StandardCharsets.UTF_8)
        );
        return order == null ? null : new TradeSourceRecord(order, partition, offset);
    }

    @Override
    public boolean isEndOfStream(TradeSourceRecord nextElement) {
        return false;
    }

    @Override
    public TypeInformation<TradeSourceRecord> getProducedType() {
        return TypeInformation.of(TradeSourceRecord.class);
    }
}
