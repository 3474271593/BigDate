package org.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.bean.GoodsSummary;
import org.example.process.GoodsBootstrapCalculator;
import org.example.process.GoodsBootstrapHistoryReader;
import org.example.process.GoodsBootstrapResult;
import org.example.process.GoodsSummarySerializer;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

/** One-time non-Flink Goods historical baseline initializer. */
public final class GoodsBootstrapApp {
    private static final String BOOTSTRAP_SERVERS =
            "192.168.13.232:9092,192.168.13.133:9092,192.168.13.131:9092";
    private static final String OUTPUT_TOPIC = "dws_goods_summary";

    private GoodsBootstrapApp() {
    }

    public static void main(String[] args) throws Exception {
        GoodsBootstrapHistoryReader.GoodsBootstrapHistory history =
                GoodsBootstrapHistoryReader.readHistory();
        GoodsBootstrapResult result = GoodsBootstrapCalculator.calculate(
                history.getOrders(),
                history.getDetails()
        );
        validate(result);
        printStatistics(result);
        writeSummaries(result.getSummaries());
        System.out.println("GOODS BOOTSTRAP SUCCESS");
    }

    private static void validate(GoodsBootstrapResult result) {
        if (result.getOrderReadCount() <= 0L) {
            throw new IllegalStateException("Goods Bootstrap order history is empty");
        }
        if (result.getDetailReadCount() <= 0L) {
            throw new IllegalStateException("Goods Bootstrap detail history is empty");
        }
        if (result.getSummaries().isEmpty()) {
            throw new IllegalStateException("Goods Bootstrap summaries are empty");
        }
    }

    private static void writeSummaries(List<GoodsSummary> summaries)
            throws Exception {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", BOOTSTRAP_SERVERS);
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());
        properties.setProperty("acks", "all");

        try (KafkaProducer<String, String> producer =
                     new KafkaProducer<>(properties)) {
            long written = 0L;
            for (GoodsSummary summary : summaries) {
                String value = GoodsSummarySerializer.serialize(summary);
                if (value == null) {
                    throw new IllegalStateException(
                            "Failed to serialize GoodsSummary: " + summary
                    );
                }
                String key = summary.getGoodsId() + "|" + summary.getWindowStart();
                Future<RecordMetadata> future = producer.send(
                        new ProducerRecord<>(OUTPUT_TOPIC, key, value)
                );
                future.get();
                written++;
            }
            producer.flush();
            if (written != summaries.size()) {
                throw new IllegalStateException(
                        "Written count mismatch: expected=" + summaries.size()
                                + ", actual=" + written
                );
            }
        }
    }

    private static void printStatistics(GoodsBootstrapResult result) {
        System.out.println("========== Goods Bootstrap Statistics ==========");
        System.out.println("order records=" + result.getOrderReadCount());
        System.out.println("detail records=" + result.getDetailReadCount());
        System.out.println("unique orders=" + result.getUniqueOrderCount());
        System.out.println("unique details=" + result.getUniqueDetailCount());
        System.out.println("summary windows=" + result.getSummaries().size());
        System.out.println("calculation mode=FINAL_SNAPSHOT_NO_CORRECTION_REPLAY");
        System.out.println("================================================");
    }
}
