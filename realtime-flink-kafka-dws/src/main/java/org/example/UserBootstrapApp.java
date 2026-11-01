// 文件路径：src/main/java/org/example/UserBootstrapApp.java
package org.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.bean.DwdOrderInfo;
import org.example.bean.UserSummary;
import org.example.process.UserBootstrapCalculator;
import org.example.process.UserBootstrapHistoryReader;
import org.example.process.UserBootstrapResult;
import org.example.process.UserSummarySerializer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

/**
 * User DWS 历史基线初始化程序。
 *
 * 执行流程：
 *
 * dwd_order_info
 *      ↓
 * 固定历史边界读取
 *      ↓
 * 按 orderId 保留最终快照
 *      ↓
 * userId + minute 聚合
 *      ↓
 * 严格校验历史统计
 *      ↓
 * dws_user_summary
 *
 * 注意：
 *
 * 1. 本程序是一次性 Bootstrap；
 * 2. 正式成功后不要重复运行；
 * 3. 实时增量由后续 UserApp 接管；
 * 4. Bootstrap 边界之后的数据不由本程序处理。
 */
public final class UserBootstrapApp {

    private static final String BOOTSTRAP_SERVERS =
            "192.168.13.232:9092,"
                    + "192.168.13.133:9092,"
                    + "192.168.13.131:9092";

    private static final String OUTPUT_TOPIC =
            "dws_user_summary";

    /*
     * ================================================================
     * UserBootstrapDryRunApp 已验证的正式历史基线
     * ================================================================
     *
     * 历史边界：
     *
     * P0 [0, 20030)
     * P1 [0, 0)
     * P2 [0, 0)
     */

    private static final long EXPECTED_READ_COUNT =
            20015L;

    private static final long EXPECTED_BOOTSTRAP_COUNT =
            20015L;

    private static final long EXPECTED_UPDATE_COUNT =
            0L;

    private static final long EXPECTED_UNIQUE_ORDER_COUNT =
            20015L;

    private static final long EXPECTED_ORDER_COUNT =
            20015L;

    private static final long EXPECTED_PAID_ORDER_COUNT =
            19999L;

    private static final int EXPECTED_SUMMARY_COUNT =
            1994;

    private static final BigDecimal EXPECTED_ORDER_AMOUNT =
            new BigDecimal("9985570708.91");

    private static final BigDecimal EXPECTED_PAID_AMOUNT =
            new BigDecimal("9984940947.39");

    private UserBootstrapApp() {
    }

    public static void main(String[] args)
            throws Exception {

        System.out.println(
                "========================================"
        );

        System.out.println(
                "USER BOOTSTRAP START"
        );

        System.out.println(
                "========================================"
        );

        /*
         * ============================================================
         * 1. 读取固定历史范围
         * ============================================================
         */
        List<DwdOrderInfo> records =
                UserBootstrapHistoryReader.readHistory();

        /*
         * ============================================================
         * 2. 计算历史 User 基线
         * ============================================================
         */
        UserBootstrapResult result =
                UserBootstrapCalculator.calculate(
                        records
                );

        /*
         * ============================================================
         * 3. 写 Kafka 前必须通过严格校验
         * ============================================================
         */
        validate(
                result
        );

        /*
         * ============================================================
         * 4. 打印即将写入的数据统计
         * ============================================================
         */
        printStatistics(
                result
        );

        /*
         * ============================================================
         * 5. 正式写入 Kafka
         * ============================================================
         *
         * 只有上面的所有校验全部通过，
         * 才会执行到这里。
         */
        writeSummaries(
                result.getSummaries()
        );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "USER BOOTSTRAP SUCCESS"
        );

        System.out.println(
                "========================================"
        );
    }

    /**
     * 正式 Bootstrap 写入 Kafka 前的严格校验。
     */
    private static void validate(
            UserBootstrapResult result) {

        require(
                "readCount",
                EXPECTED_READ_COUNT,
                result.getReadCount()
        );

        require(
                "bootstrapCount",
                EXPECTED_BOOTSTRAP_COUNT,
                result.getBootstrapCount()
        );

        require(
                "updateCount",
                EXPECTED_UPDATE_COUNT,
                result.getUpdateCount()
        );

        require(
                "uniqueOrderCount",
                EXPECTED_UNIQUE_ORDER_COUNT,
                result.getUniqueOrderCount()
        );

        require(
                "orderCount",
                EXPECTED_ORDER_COUNT,
                result.getOrderCount()
        );

        require(
                "paidOrderCount",
                EXPECTED_PAID_ORDER_COUNT,
                result.getPaidOrderCount()
        );

        require(
                "summaryCount",
                EXPECTED_SUMMARY_COUNT,
                result.getSummaries().size()
        );

        requireMoney(
                "orderAmount",
                EXPECTED_ORDER_AMOUNT,
                result.getOrderAmount()
        );

        requireMoney(
                "paidAmount",
                EXPECTED_PAID_AMOUNT,
                result.getPaidAmount()
        );

        if (result.getSummaries().isEmpty()) {

            throw new IllegalStateException(
                    "User Bootstrap summaries are empty"
            );
        }
    }

    private static void require(
            String name,
            long expected,
            long actual) {

        if (expected != actual) {

            throw new IllegalStateException(
                    name
                            + " mismatch: expected="
                            + expected
                            + ", actual="
                            + actual
            );
        }
    }

    private static void requireMoney(
            String name,
            BigDecimal expected,
            BigDecimal actual) {

        if (actual == null
                || expected.compareTo(actual) != 0) {

            throw new IllegalStateException(
                    name
                            + " mismatch: expected="
                            + expected.toPlainString()
                            + ", actual="
                            + (actual == null
                            ? "null"
                            : actual.toPlainString())
            );
        }
    }

    /**
     * 将历史 UserSummary 写入 Kafka。
     *
     * Kafka Key：
     *
     * userId|windowStart
     *
     * 因为 User DWS 的业务粒度是：
     *
     * userId + 1 minute
     */
    private static void writeSummaries(
            List<UserSummary> summaries)
            throws Exception {

        Properties properties =
                new Properties();

        properties.setProperty(
                "bootstrap.servers",
                BOOTSTRAP_SERVERS
        );

        properties.setProperty(
                "key.serializer",
                StringSerializer.class.getName()
        );

        properties.setProperty(
                "value.serializer",
                StringSerializer.class.getName()
        );

        /*
         * Bootstrap 是一次性历史基线，
         * 要求 Broker 完整确认写入。
         */
        properties.setProperty(
                "acks",
                "all"
        );

        try (KafkaProducer<String, String> producer =
                     new KafkaProducer<>(properties)) {

            long written =
                    0L;

            for (UserSummary summary :
                    summaries) {

                String value =
                        UserSummarySerializer.serialize(
                                summary
                        );

                if (value == null) {

                    throw new IllegalStateException(
                            "Failed to serialize UserSummary: "
                                    + summary
                    );
                }

                /*
                 * User DWS 粒度：
                 *
                 * userId + minute
                 */
                String key =
                        summary.getUserId()
                                + "|"
                                + summary.getWindowStart();

                ProducerRecord<String, String> record =
                        new ProducerRecord<>(
                                OUTPUT_TOPIC,
                                key,
                                value
                        );

                /*
                 * 当前历史基线只有 1992 条。
                 *
                 * Bootstrap 主动等待每一条 Broker ACK，
                 * 用少量性能换初始化过程的确定性。
                 */
                Future<RecordMetadata> future =
                        producer.send(
                                record
                        );

                future.get();

                written++;
            }

            producer.flush();

            if (written != summaries.size()) {

                throw new IllegalStateException(
                        "Written summary count mismatch: expected="
                                + summaries.size()
                                + ", actual="
                                + written
                );
            }

            System.out.println(
                    "Kafka写入完成="
                            + written
            );
        }
    }

    /**
     * 打印正式 Bootstrap 数据统计。
     */
    private static void printStatistics(
            UserBootstrapResult result) {

        System.out.println();

        System.out.println(
                "========== User Bootstrap Statistics =========="
        );

        System.out.println(
                "读取总数="
                        + result.getReadCount()
        );

        System.out.println(
                "bootstrap="
                        + result.getBootstrapCount()
        );

        System.out.println(
                "UPDATE="
                        + result.getUpdateCount()
        );

        System.out.println(
                "最终唯一orderId="
                        + result.getUniqueOrderCount()
        );

        System.out.println(
                "历史最早createTime="
                        + result.getEarliestCreateTime()
        );

        System.out.println(
                "历史最晚createTime="
                        + result.getLatestCreateTime()
        );

        System.out.println(
                "UserSummary窗口数量="
                        + result.getSummaries().size()
        );

        System.out.println(
                "orderCount="
                        + result.getOrderCount()
        );

        System.out.println(
                "orderAmount="
                        + result.getOrderAmount()
                        .toPlainString()
        );

        System.out.println(
                "paidOrderCount="
                        + result.getPaidOrderCount()
        );

        System.out.println(
                "paidAmount="
                        + result.getPaidAmount()
                        .toPlainString()
        );

        System.out.println(
                "================================================"
        );

        System.out.println();
    }
}
