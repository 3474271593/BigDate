package org.example.process;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.example.bean.DwdOrderGoods;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * order_goods Redis商品DIM补全函数。
 *
 * Redis结构：
 * Key：dim:goods:<goodsId>
 * Type：Hash
 * 商品名称字段：goodsName
 *
 * 示例：
 * HGET dim:goods:100125 goodsName
 */
public class GoodsDimRedisEnrichmentFunction extends RichMapFunction<DwdOrderGoods, DwdOrderGoods> {

    private static final long serialVersionUID = 1L;

    /**
     * Redis连接配置。
     */
    private static final String REDIS_HOST = "master";
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");

    /**
     * Redis连接超时时间。
     */
    private static final int REDIS_TIMEOUT_MS = 5000;

    /**
     * Jedis连接不能参与Flink序列化，
     * 因此使用transient，并在open()中初始化。
     */
    private transient Jedis jedis;

    /**
     * 每个Flink算子实例启动时创建Redis连接。
     */
    @Override
    public void open(Configuration parameters) {
        connectRedis();
    }

    /**
     * 创建Jedis客户端。
     *
     * 单独抽取该方法主要有两个目的：
     * 1. 生产环境统一创建Jedis连接；
     * 2. 单元测试可以覆盖该方法，模拟Redis连接断开和恢复。
     */
    protected Jedis createJedis() {
        return new Jedis(REDIS_HOST, REDIS_PORT, REDIS_TIMEOUT_MS);
    }

    /**
     * 建立Redis连接，并完成认证和PING检查。
     */
    private void connectRedis() {
        closeJedis();

        Jedis newJedis = null;

        try {
            newJedis = createJedis();

            newJedis.auth(REDIS_PASSWORD);

            String pong = newJedis.ping();

            if (!"PONG".equalsIgnoreCase(pong)) {
                throw new IllegalStateException("Redis PING失败：" + pong);
            }

            jedis = newJedis;

            System.out.println("Redis连接成功：" + REDIS_HOST + ":" + REDIS_PORT);
        } catch (Exception e) {
            if (newJedis != null) {
                try {
                    newJedis.close();
                } catch (Exception ignored) {
                    // Redis连接创建失败时，关闭异常不影响原始异常。
                }
            }

            jedis = null;
            throw e;
        }
    }

    /**
     * 查询Redis中的商品名称。
     */
    private String queryGoodsName(String redisKey) {
        return jedis.hget(redisKey, "goodsName");
    }

    /**
     * 对DWD订单商品事实进行商品DIM补全。
     */
    @Override
    public DwdOrderGoods map(DwdOrderGoods orderGoods) {

        if (orderGoods == null) {
            return null;
        }

        /*
         * order_goods自身已经存在goodsName时，
         * 它代表下单时的商品快照。
         *
         * 不访问Redis，也不使用当前DIM覆盖历史快照。
         */
        if (!GoodsDimEnrichmentFunction.isBlank(orderGoods.getGoodsName())) {
            return orderGoods;
        }

        Long goodsId = orderGoods.getGoodsId();

        /*
         * goodsId在前面的DWD清洗阶段已经进行了硬校验。
         *
         * 这里再次防御性检查，避免异常查询Redis。
         */
        if (goodsId == null || goodsId <= 0) {
            return GoodsDimEnrichmentFunction.markLookupFailed(orderGoods);
        }

        String redisKey = "dim:goods:" + goodsId;

        try {
            /*
             * 第一次正常查询Redis。
             */
            String dimGoodsName = queryGoodsName(redisKey);

            return GoodsDimEnrichmentFunction.enrichGoodsName(orderGoods, dimGoodsName);
        } catch (JedisConnectionException firstException) {
            /*
             * JedisConnectionException表示当前Redis连接已经不可用，
             * 例如：
             *
             * Unexpected end of stream
             * Connection reset
             * Socket closed
             *
             * 此时关闭旧连接，重新建立连接，并且只重试一次当前查询。
             */
            System.err.println(
                    "Redis连接异常，准备重新连接，goodsId="
                            + goodsId
                            + ", redisKey="
                            + redisKey
                            + ", error="
                            + firstException.getMessage()
            );

            try {
                connectRedis();

                String dimGoodsName = queryGoodsName(redisKey);

                System.out.println(
                        "Redis重新连接并查询成功，goodsId="
                                + goodsId
                                + ", redisKey="
                                + redisKey
                );

                return GoodsDimEnrichmentFunction.enrichGoodsName(orderGoods, dimGoodsName);
            } catch (Exception retryException) {
                /*
                 * 重连或者第二次查询仍然失败。
                 *
                 * 不无限重试，避免Redis故障导致Flink算子永久阻塞。
                 */
                printLookupFailure(goodsId, redisKey, retryException);

                return GoodsDimEnrichmentFunction.markLookupFailed(orderGoods);
            }
        } catch (Exception e) {
            /*
             * 非连接类Redis异常不进行自动重连。
             *
             * 事实数据仍然保留，并增加质量告警。
             */
            printLookupFailure(goodsId, redisKey, e);

            return GoodsDimEnrichmentFunction.markLookupFailed(orderGoods);
        }
    }

    /**
     * 打印Redis DIM查询失败日志。
     */
    private void printLookupFailure(Long goodsId, String redisKey, Exception e) {
        System.err.println(
                "Redis维度查询失败，goodsId="
                        + goodsId
                        + ", redisKey="
                        + redisKey
        );

        e.printStackTrace();
    }

    /**
     * 安全关闭当前Jedis连接。
     */
    private void closeJedis() {
        if (jedis != null) {
            try {
                jedis.close();
            } catch (Exception e) {
                System.err.println("关闭Redis连接失败：" + e.getMessage());
            } finally {
                jedis = null;
            }
        }
    }

    /**
     * Flink算子关闭时释放Redis连接。
     */
    @Override
    public void close() {
        closeJedis();
    }
}
