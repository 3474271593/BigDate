package org.example.process;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.util.serialization.KeyedDeserializationSchema;
import org.example.bean.DwdOrderInfo;
import org.example.bean.ShopSourceRecord;

import java.nio.charset.StandardCharsets;

/**
 * Shop DWS Kafka Source 反序列化器。
 *
 * 功能：
 * 1. 将 dwd_order_info 中的 Kafka JSON 转换为 DwdOrderInfo；
 * 2. 保留 Kafka partition；
 * 3. 保留 Kafka offset。
 *
 * partition + offset 后续用于：
 * - ShopBootstrapApp 控制历史读取边界；
 * - ShopApp 判断历史 warmup 与实时数据的分界点。
 *
 * JSON 字段解析统一复用 DwdOrderInfoDeserializer，
 * 保证 Shop 与 Trade 对 dwd_order_info 的解析语义一致。
 */
public class ShopSourceRecordDeserializer
        implements KeyedDeserializationSchema<ShopSourceRecord> {

    @Override
    public ShopSourceRecord deserialize(
            byte[] messageKey,
            byte[] message,
            String topic,
            int partition,
            long offset) {

        if (message == null) {
            return null;
        }

        DwdOrderInfo order = DwdOrderInfoDeserializer.deserialize(
                new String(message, StandardCharsets.UTF_8)
        );

        if (order == null) {
            return null;
        }

        return new ShopSourceRecord(
                order,
                partition,
                offset
        );
    }

    @Override
    public boolean isEndOfStream(ShopSourceRecord nextElement) {
        return false;
    }

    @Override
    public TypeInformation<ShopSourceRecord> getProducedType() {
        return TypeInformation.of(ShopSourceRecord.class);
    }
}