package org.example.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.example.bean.DirtyData;
import org.junit.Assert;
import org.junit.Test;

/**
 * DWD 脏数据构造测试。
 *
 * 当前测试目标：
 *
 * 当 orders 清洗发生异常时，
 * 能否把异常订单转换成 DirtyData。
 */
public class DirtyDataBuilderTest {

    /**
     * 测试 12：
     *
     * orderId = 0 属于非法订单。
     *
     * cleanOrder() 会抛：
     *
     * IllegalArgumentException
     *
     * 现在要求：
     *
     * 把这个异常转换成 DirtyData，
     * 而不是直接丢掉原始数据。
     */
    @Test
    public void shouldBuildDirtyDataWhenOrderIdIsInvalid() {

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"0\","
                        + "\"orderNo\":\"DIRTY_TEST_001\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"0\","
                        + "\"goodsMoney\":\"100.00\","
                        + "\"deliverMoney\":\"10.00\","
                        + "\"totalMoney\":\"110.00\","
                        + "\"realTotalMoney\":\"110.00\","
                        + "\"isPay\":\"0\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";

        JSONObject odsJson =
                JSON.parseObject(json);

        try {

            /*
             * 这里故意执行原来的订单清洗。
             *
             * 因为：
             *
             * orderId = 0
             *
             * 所以这里应该抛异常。
             */
            OrderProcessFunction.cleanOrder(
                    odsJson
            );

            /*
             * 如果没有抛异常，
             * 说明测试数据本身有问题。
             */
            Assert.fail(
                    "orderId=0 应该触发订单清洗异常"
            );

        } catch (Exception e) {

            /*
             * 新功能：
             *
             * 把原始JSON + 异常
             * 转换为 DirtyData。
             *
             * 注意：
             *
             * DirtyDataBuilder 目前还没有创建，
             * 下一步才实现。
             */
            DirtyData dirtyData =
                    DirtyDataBuilder.build(
                            odsJson,
                            e
                    );


            // =========================
            // 1. 来源表
            // =========================

            Assert.assertEquals(
                    "orders",
                    dirtyData.getTableName()
            );


            // =========================
            // 2. ODS操作类型
            // =========================

            Assert.assertEquals(
                    "bootstrap",
                    dirtyData.getOpType()
            );


            // =========================
            // 3. 异常类型
            // =========================

            Assert.assertEquals(
                    "IllegalArgumentException",
                    dirtyData.getErrorType()
            );


            // =========================
            // 4. 异常信息
            // =========================

            Assert.assertTrue(
                    dirtyData
                            .getErrorMessage()
                            .contains(
                                    "orderId非法"
                            )
            );


            // =========================
            // 5. 原始数据不能丢
            // =========================

            Assert.assertTrue(
                    dirtyData
                            .getRawData()
                            .contains(
                                    "\"orderId\":\"0\""
                            )
            );


            // =========================
            // 6. 必须存在处理时间
            // =========================

            Assert.assertNotNull(
                    dirtyData.getProcessTs()
            );

            Assert.assertTrue(
                    dirtyData.getProcessTs() > 0
            );
        }
    }
}