package org.example;

import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.DriverManager;

public class App {

    public static void main(String[] args) {
        testMySQL();
        System.out.println("------------------------------");
        testRedis();
    }


    /**
     * 测试 MySQL 是否可以正常连接
     */
    private static void testMySQL() {

        // MySQL服务器地址
        String url =
                "jdbc:mysql://192.168.13.232:3306/shop"
                        + "?useUnicode=true"
                        + "&characterEncoding=UTF-8"
                        + "&useSSL=false"
                        + "&serverTimezone=Asia/Shanghai";

        // MySQL用户名
        String username = "root";

        // 修改成你的MySQL密码
        String password = System.getenv("MYSQL_PASSWORD");

        System.out.println("开始测试 MySQL...");

        try (
                Connection connection =
                        DriverManager.getConnection(
                                url,
                                username,
                                password
                        )
        ) {

            System.out.println("MySQL连接成功！");

            System.out.println(
                    "数据库：" +
                            connection.getCatalog()
            );

        } catch (Exception e) {

            System.out.println("MySQL连接失败！");

            e.printStackTrace();
        }
    }


    /**
     * 测试 Redis 是否可以正常连接
     */
    private static void testRedis() {
        // Redis服务器
        String host = "192.168.13.232";
        // Redis端口
        int port = 6379;
        // Redis密码
        String password = System.getenv("REDIS_PASSWORD");
        System.out.println("开始测试 Redis...");
        try (
                Jedis jedis =
                        new Jedis(host, port)
        ) {
            // Redis密码认证
            jedis.auth(password);
            // 发送PING命令
            String result = jedis.ping();
            System.out.println(
                    "Redis连接成功！"
            );
            System.out.println(
                    "Redis PING结果：" + result
            );
        } catch (Exception e) {
            System.out.println(
                    "Redis连接失败！"
            );
            e.printStackTrace();
        }
    }
}