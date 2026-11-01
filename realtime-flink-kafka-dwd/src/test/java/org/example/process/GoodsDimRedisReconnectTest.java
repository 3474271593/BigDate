package org.example.process;

import org.example.bean.DwdOrderGoods;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class GoodsDimRedisReconnectTest {

    @Test
    public void shouldReconnectAndRetryWhenRedisConnectionIsBroken() throws Exception {
        TestGoodsDimRedisEnrichmentFunction function = new TestGoodsDimRedisEnrichmentFunction();

        DwdOrderGoods orderGoods = new DwdOrderGoods();
        orderGoods.setGoodsId(100125L);
        orderGoods.setGoodsName("");

        function.open(null);

        DwdOrderGoods result = function.map(orderGoods);

        Assert.assertEquals("测试商品", result.getGoodsName());
        Assert.assertEquals(2, function.getCreateCount());

        function.close();
    }

    private static class TestGoodsDimRedisEnrichmentFunction extends GoodsDimRedisEnrichmentFunction {

        private int createCount = 0;

        @Override
        protected Jedis createJedis() {
            createCount++;

            if (createCount == 1) {
                return new FirstFailureJedis();
            }

            return new SuccessJedis();
        }

        public int getCreateCount() {
            return createCount;
        }
    }

    private static class FirstFailureJedis extends Jedis {

        @Override
        public String auth(String password) {
            return "OK";
        }

        @Override
        public String ping() {
            return "PONG";
        }

        @Override
        public String hget(String key, String field) {
            throw new JedisConnectionException("模拟Redis连接中断");
        }

        @Override
        public void close() {
        }
    }

    private static class SuccessJedis extends Jedis {

        @Override
        public String auth(String password) {
            return "OK";
        }

        @Override
        public String ping() {
            return "PONG";
        }

        @Override
        public String hget(String key, String field) {
            return "测试商品";
        }

        @Override
        public void close() {
        }
    }
}