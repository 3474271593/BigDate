// 文件路径：src/main/java/org/example/process/UserSourceRecordDeserializer.java
package org.example.process;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.util.serialization.KeyedDeserializationSchema;
import org.example.bean.DwdOrderInfo;
import org.example.bean.UserSourceRecord;

import java.nio.charset.StandardCharsets;

/**
 * User DWS Kafka Source 反序列化器。
 *
 * 功能：
 *
 * 1. 将 dwd_order_info 中的 Kafka JSON 转换为 DwdOrderInfo；
 * 2. 保留 Kafka partition；
 * 3. 保留 Kafka offset。
 *
 * partition + offset 后续用于：
 *
 * - UserBootstrapApp 控制历史读取边界；
 * - UserApp 判断历史 warmup 与实时数据的分界点。
 *
 * JSON 字段解析统一复用 DwdOrderInfoDeserializer，
 * 保证 User、Shop、Trade 对 dwd_order_info
 * 的解析语义保持一致。
 */
public class UserSourceRecordDeserializer
        implements KeyedDeserializationSchema<UserSourceRecord> {

    @Override
    public UserSourceRecord deserialize(
            byte[] messageKey,
            byte[] message,
            String topic,
            int partition,
            long offset) {

        if (message == null) {
            return null;
        }

        DwdOrderInfo order =
                DwdOrderInfoDeserializer.deserialize(
                        new String(
                                message,
                                StandardCharsets.UTF_8
                        )
                );

        if (order == null) {
            return null;
        }

        return new UserSourceRecord(
                order,
                partition,
                offset
        );
    }

    @Override
    public boolean isEndOfStream(
            UserSourceRecord nextElement) {

        return false;
    }

    @Override
    public TypeInformation<UserSourceRecord> getProducedType() {

        return TypeInformation.of(
                UserSourceRecord.class
        );
    }
}