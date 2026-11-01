package org.example.bootstrap;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.sql.*;
import java.util.Properties;
//传历史事实数据表到kafka Topic ods_shop_db
public class FactBootstrap {

    private static final String MYSQL_URL =
            "jdbc:mysql://192.168.13.232:3306/shop"
                    + "?useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&useSSL=false"
                    + "&serverTimezone=Asia/Shanghai";

    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = System.getenv("MYSQL_PASSWORD");
    private static final String KAFKA_BOOTSTRAP_SERVERS = "192.168.13.232:9092";
    private static final String ODS_TOPIC = "ods_shop_db";

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA_BOOTSTRAP_SERVERS
        );
        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName()
        );
        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName()
        );
        try (
                Connection connection =
                        DriverManager.getConnection(
                                MYSQL_URL,
                                MYSQL_USER,
                                MYSQL_PASSWORD
                        );

                KafkaProducer<String, String> producer =
                        new KafkaProducer<>(properties)
        ) {

            loadTable(
                    connection,
                    producer,
                    "orders",
                    "orderId"
            );

            loadTable(
                    connection,
                    producer,
                    "order_goods",
                    "ogId"
            );

            loadTable(
                    connection,
                    producer,
                    "order_refunds",
                    "id"
            );

            producer.flush();
            System.out.println("历史事实数据初始化到Kafka完成");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 将指定事实表写入Kafka ODS
     */
    private static void loadTable(
            Connection connection,
            KafkaProducer<String, String> producer,
            String tableName,
            String primaryKey
    ) throws Exception {
        String sql = "SELECT * FROM `" + tableName + "`";
        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            int count = 0;

            while (resultSet.next()) {

                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"table\":\"").append(tableName).append("\",");
                json.append("\"type\":\"bootstrap\",");
                json.append("\"data\":{");

                for (
                        int i = 1;
                        i <= columnCount;
                        i++
                ) {

                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    json.append("\"").append(columnName).append("\":");

                    if (value == null) {
                        json.append("null");
                    } else {
                        json.append("\"").append(escapeJson(value.toString())).append("\"");
                    }

                    if (i < columnCount) {
                        json.append(",");
                    }
                }

                json.append("}");
                json.append("}");
                String key = resultSet.getObject(primaryKey).toString();
                producer.send(new ProducerRecord<>(ODS_TOPIC, key, json.toString()));
                count++;

                if (count % 1000 == 0) {
                    producer.flush();
                    System.out.println(tableName + " 已发送：" + count);
                }
            }
            producer.flush();
            System.out.println(tableName + " 历史数据发送完成，共：" + count + " 条");
        }
    }


    /**
     * 简单处理JSON特殊字符
     */
    private static String escapeJson(
            String value
    ) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}