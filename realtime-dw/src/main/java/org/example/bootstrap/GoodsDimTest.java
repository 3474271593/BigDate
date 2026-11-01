package org.example.bootstrap;

import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

public class GoodsDimTest {

    public static void main(String[] args) {

        String mysqlUrl =
                "jdbc:mysql://192.168.13.232:3306/shop"
                        + "?useUnicode=true"
                        + "&characterEncoding=UTF-8"
                        + "&useSSL=false"
                        + "&serverTimezone=Asia/Shanghai";

        String mysqlUser = "root";
        String mysqlPassword = System.getenv("MYSQL_PASSWORD");
        String redisHost = "192.168.13.232";
        int redisPort = 6379;
        String redisPassword = System.getenv("REDIS_PASSWORD");

        try (
                Connection connection =
                        DriverManager.getConnection(
                                mysqlUrl,
                                mysqlUser,
                                mysqlPassword
                        );

                Jedis jedis =
                        new Jedis(redisHost, redisPort)
        ) {

            // Redis认证
            jedis.auth(redisPassword);

            System.out.println("MySQL连接成功");
            System.out.println("Redis连接成功：" + jedis.ping());


            // ==============================
            // 只读取 goods 表第一条数据
            // ==============================
            String sql ="SELECT * FROM goods";
//                    "SELECT * FROM goods LIMIT 1";


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(sql);

                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {

                    System.out.println("goods表没有数据");
                    return;
                }

                // ==============================
                // 获取 goods 主键
                // ==============================
                Object goodsId =
                        resultSet.getObject("goodsId");

                if (goodsId == null) {

                    System.out.println("goodsId为空");
                    return;
                }


                // Redis Key
                String redisKey =
                        "dim:goods:" + goodsId;

                // ==============================
                // 自动获取当前行所有字段
                // ==============================
                ResultSetMetaData metaData =
                        resultSet.getMetaData();

                int columnCount =
                        metaData.getColumnCount();

                Map<String, String> goodsMap =
                        new HashMap<>();


                for (int i = 1; i <= columnCount; i++) {

                    // 获取字段名
                    String columnName =
                            metaData.getColumnLabel(i);

                    // 获取字段值
                    Object value =
                            resultSet.getObject(i);

                    // Redis Hash不存null
                    goodsMap.put(
                            columnName,
                            value == null
                                    ? ""
                                    : value.toString()
                    );
                }

                jedis.hset(
                        redisKey,
                        goodsMap
                );


                System.out.println(
                        "写入Redis成功"
                );

                System.out.println(
                        "Redis Key：" + redisKey
                );

                System.out.println(
                        "字段数量：" + goodsMap.size()
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "执行失败："
            );

            e.printStackTrace();
        }
    }
}