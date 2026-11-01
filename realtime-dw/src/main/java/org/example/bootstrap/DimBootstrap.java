package org.example.bootstrap;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

//ResultSetMetaData去动态读取当前表实际有哪些字段
public class DimBootstrap {
    private static final String MYSQL_URL =
            "jdbc:mysql://192.168.13.232:3306/shop"
                    + "?useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&useSSL=false"
                    + "&serverTimezone=Asia/Shanghai";

    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = System.getenv("MYSQL_PASSWORD");
    private static final String REDIS_HOST = "192.168.13.232";
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");


    public static void main(String[] args) {
        Connection connection = null;
        Jedis jedis = null;
        try {
            // 1. 连接 MySQL
            connection = DriverManager.getConnection(
                    MYSQL_URL,
                    MYSQL_USER,
                    MYSQL_PASSWORD
            );
            System.out.println("MySQL连接成功");
            // 2. 连接 Redis
            jedis = new Jedis(
                    REDIS_HOST,
                    REDIS_PORT
            );
            jedis.auth(REDIS_PASSWORD);
            System.out.println(
                    "Redis连接成功：" + jedis.ping()
            );
            System.out.println();
            System.out.println("开始初始化普通维度表");
            // 3. 普通维度表
            loadHashDimension(
                    connection,
                    jedis,
                    "goods",
                    "goodsId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "shops",
                    "shopId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "users",
                    "userId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "goods_cats",
                    "catId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "brands",
                    "brandId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "areas",
                    "areaId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "goods_specs",
                    "id"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "spec_cats",
                    "catId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "spec_items",
                    "itemId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "attributes",
                    "attrId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "user_ranks",
                    "rankId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "shop_cats",
                    "catId"
            );

            loadHashDimension(
                    connection,
                    jedis,
                    "payments",
                    "id"
            );

            System.out.println();
            System.out.println("开始初始化关系维度表");
            // 4. 关系维度表
            loadRelationSet(
                    connection,
                    jedis,
                    "cat_brands",
                    "catId",
                    "brandId"
            );
            loadRelationSet(
                    connection,
                    jedis,
                    "cat_shops",
                    "catId",
                    "shopId"
            );
            /*
             * goods_attributes 比较特殊
             * 一个商品可能有多个属性
             * 用：
             * dim:goods_attributes:{goodsId}
             * Set中保存关系表主键id
             * 同时：
             * dim:goods_attributes_row:{id}
             * 保存完整关系记录
             */
            loadGoodsAttributes(
                    connection,
                    jedis
            );

            System.out.println();
            System.out.println("所有维度初始化完成");

        } catch (Exception e) {
            System.out.println("维度初始化失败");
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (connection != null) {
                    connection.close();
                }
                if (jedis != null) {
                    jedis.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 将普通维度表初始化到 Redis Hash
     *
     * Redis结构：
     *
     * dim:goods:1
     * dim:shops:1
     * dim:users:1
     *
     * @param connection MySQL连接
     * @param jedis Redis连接
     * @param tableName MySQL表名
     * @param primaryKey 主键字段
     */
    private static void loadHashDimension(
            Connection connection,
            Jedis jedis,
            String tableName,
            String primaryKey
    ) throws SQLException {

        System.out.println();
        System.out.println("开始初始化：" + tableName);

        String sql = "SELECT * FROM `" + tableName + "`";

        int count = 0;
        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
                Pipeline pipeline = jedis.pipelined()
        ) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (resultSet.next()) {
                Object primaryKeyValue = resultSet.getObject(primaryKey);
                if (primaryKeyValue == null) {
                    System.out.println("跳过主键为空数据：" + tableName);
                    continue;
                }
                // Redis Key
                String redisKey = "dim:" + tableName + ":" + primaryKeyValue;
                Map<String, String> row = new HashMap<>();

                // 自动读取所有字段
                for (
                        int i = 1;
                        i <= columnCount;
                        i++
                ) {

                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    row.put(
                            columnName,
                            value == null ? "" : value.toString()
                    );
                }

                // Pipeline写入Redis
                pipeline.hset(redisKey, row);

                count++;
                // 每500条提交一次
                if (count % 500 == 0) {
                    pipeline.sync();
                    System.out.println(tableName + " 已写入：" + count);
                }
            }
            // 提交最后不足500条的数据
            pipeline.sync();
        }

        System.out.println(tableName + " 初始化完成，共写入：" + count + " 条");
    }

    /**
     * 加载关系表到Redis Set
     *
     * 例如：
     *
     * cat_brands
     *
     * dim:cat_brands:10
     *
     * Set:
     * 2
     * 3
     * 5
     *
     * @param connection MySQL连接
     * @param jedis Redis连接
     * @param tableName 关系表名
     * @param leftKey 左侧字段
     * @param rightKey 右侧字段
     */
    private static void loadRelationSet(
            Connection connection,
            Jedis jedis,
            String tableName,
            String leftKey,
            String rightKey
    ) throws SQLException {

        System.out.println();
        System.out.println("开始初始化关系表：" + tableName);
        String sql = "SELECT `" + leftKey + "`, `" + rightKey + "` FROM `" + tableName + "`";

        int count = 0;
        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
                Pipeline pipeline = jedis.pipelined()
        ) {
            while (resultSet.next()) {
                Object leftValue = resultSet.getObject(leftKey);
                Object rightValue = resultSet.getObject(rightKey);
                if (
                        leftValue == null || rightValue == null
                ) {
                    continue;
                }
                String redisKey = "dim:" + tableName + ":" + leftValue;
                pipeline.sadd(
                        redisKey,
                        rightValue.toString()
                );
                count++;
                if (count % 500 == 0) {
                    pipeline.sync();
                    System.out.println(tableName + " 已处理：" + count);
                }
            }
            pipeline.sync();
        }

        System.out.println(tableName + " 初始化完成，共处理：" + count + " 条关系"
        );
    }

    /**
     * 加载商品属性关系表
     *
     * 这里同时保存：
     *
     * 1.
     * dim:goods_attributes:{goodsId}
     *
     * Set保存关系记录id
     *
     * 2.
     * dim:goods_attributes_row:{id}
     *
     * Hash保存完整关系记录
     */
    private static void loadGoodsAttributes(
            Connection connection,
            Jedis jedis
    ) throws SQLException {

        String tableName = "goods_attributes";

        System.out.println();
        System.out.println("开始初始化关系表：" + tableName);

        String sql =
                "SELECT * FROM `goods_attributes`";
        int count = 0;
        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
                Pipeline pipeline = jedis.pipelined()
        ) {

            ResultSetMetaData metaData = resultSet.getMetaData();

            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Object id = resultSet.getObject("id");
                Object goodsId = resultSet.getObject("goodsId");
                if (
                        id == null || goodsId == null
                ) {
                    continue;
                }

                // 保存商品 -> 属性关系id
                String relationKey = "dim:goods_attributes:" + goodsId;
                pipeline.sadd(relationKey, id.toString());

                // 保存完整关系记录
                String rowKey = "dim:goods_attributes_row:" + id;
                Map<String, String> row = new HashMap<>();

                for (int i = 1;
                        i <= columnCount;
                        i++
                ) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value == null ? "" : value.toString());
                }

                pipeline.hset(rowKey, row);
                count++;

                if (count % 500 == 0) {
                    pipeline.sync();
                    System.out.println(tableName + " 已处理：" + count);
                }
            }
            pipeline.sync();
        }

        System.out.println(tableName + " 初始化完成，共处理：" + count + " 条");
    }
}