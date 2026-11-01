package org.example.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.example.bean.DirtyData;
import org.example.bean.DwdOrderInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * OrderDwdProcessFunction 测试。
 *
 * 测试目标：
 *
 * 1. 正常订单进入主流
 * 2. 异常订单进入 DirtyData 侧输出流
 *
 * 注意：
 *
 * 这里主要测试的是“分流逻辑”，
 * 而不是再次测试所有订单字段清洗规则。
 *
 * 因为订单字段清洗规则已经由：
 *
 * OrderProcessFunctionTest
 *
 * 中的 11 条测试负责。
 */
public class OrderDwdProcessFunctionTest {


    /**
     * 测试 13：
     *
     * 一条完全合法的 orders 数据，
     * 应该进入 Flink 主流。
     *
     * 预期：
     *
     * normalList.size() = 1
     *
     * dirtyList.size() = 0
     */
    @Test
    public void shouldSendValidOrderToMainOutput() throws Exception {

        // ============================================================
        // 1. 准备一条正常订单
        // ============================================================

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"2001\","
                        + "\"orderNo\":\"ORDER_2001\","
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
                JSON.parseObject(
                        json
                );


        // ============================================================
        // 2. 创建待测试的 Flink ProcessFunction
        // ============================================================

        OrderDwdProcessFunction function =
                new OrderDwdProcessFunction();


        // ============================================================
        // 3. 准备两个集合
        // ============================================================

        /*
         * normalList：
         *
         * 模拟 Flink 主输出流。
         */
        List<DwdOrderInfo> normalList =
                new ArrayList<>();


        /*
         * dirtyList：
         *
         * 模拟 Flink Side Output。
         */
        List<DirtyData> dirtyList =
                new ArrayList<>();


        // ============================================================
        // 4. 创建主流 Collector
        // ============================================================

        Collector<DwdOrderInfo> collector =
                new Collector<DwdOrderInfo>() {

                    @Override
                    public void collect(
                            DwdOrderInfo record) {

                        normalList.add(
                                record
                        );
                    }


                    @Override
                    public void close() {

                        // 测试中不需要处理
                    }
                };


        // ============================================================
        // 5. 创建测试 Context
        // ============================================================

        ProcessFunction<JSONObject, DwdOrderInfo>.Context context =
                function.new Context() {

                    @Override
                    public Long timestamp() {

                        return null;
                    }


                    @Override
                    public TimerService timerService() {

                        /*
                         * 当前订单分流逻辑不使用定时器。
                         */
                        return null;
                    }


                    @Override
                    public <X> void output(
                            OutputTag<X> outputTag,
                            X value) {

                        /*
                         * 如果写入的是 DirtyData Side Output，
                         * 就把结果保存到 dirtyList。
                         */
                        if (OrderDwdProcessFunction
                                .DIRTY_TAG
                                .getId()
                                .equals(
                                        outputTag.getId()
                                )) {

                            dirtyList.add(
                                    (DirtyData) value
                            );
                        }
                    }
                };


        // ============================================================
        // 6. 执行 processElement
        // ============================================================

        function.processElement(
                odsJson,
                context,
                collector
        );


        // ============================================================
        // 7. 验证主流
        // ============================================================

        Assert.assertEquals(
                1,
                normalList.size()
        );


        DwdOrderInfo order =
                normalList.get(
                        0
                );


        Assert.assertEquals(
                Long.valueOf(2001L),
                order.getOrderId()
        );


        Assert.assertEquals(
                "bootstrap",
                order.getOpType()
        );


        // ============================================================
        // 8. 正常订单不能进入脏数据流
        // ============================================================

        Assert.assertEquals(
                0,
                dirtyList.size()
        );
    }


    /**
     * 测试 14：
     *
     * orderId = 0
     *
     * 属于非法订单。
     *
     * 原来的：
     *
     * OrderProcessFunction.cleanOrder()
     *
     * 会抛出 IllegalArgumentException。
     *
     * 但是新的 Flink ProcessFunction
     * 不应该继续把异常抛出去导致任务失败，
     *
     * 而应该：
     *
     * 捕获异常
     *
     * ↓
     *
     * DirtyDataBuilder
     *
     * ↓
     *
     * DIRTY_TAG Side Output
     */
    @Test
    public void shouldSendInvalidOrderToDirtySideOutput()
            throws Exception {

        // ============================================================
        // 1. 准备非法订单
        // ============================================================

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"0\","
                        + "\"orderNo\":\"DIRTY_ORDER_001\","
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
                JSON.parseObject(
                        json
                );


        // ============================================================
        // 2. 创建 ProcessFunction
        // ============================================================

        OrderDwdProcessFunction function =
                new OrderDwdProcessFunction();


        // ============================================================
        // 3. 主流结果
        // ============================================================

        List<DwdOrderInfo> normalList =
                new ArrayList<>();


        // ============================================================
        // 4. 脏数据结果
        // ============================================================

        List<DirtyData> dirtyList =
                new ArrayList<>();


        // ============================================================
        // 5. 主流 Collector
        // ============================================================

        Collector<DwdOrderInfo> collector =
                new Collector<DwdOrderInfo>() {

                    @Override
                    public void collect(
                            DwdOrderInfo record) {

                        normalList.add(
                                record
                        );
                    }


                    @Override
                    public void close() {

                        // 测试不需要处理
                    }
                };


        // ============================================================
        // 6. Side Output Context
        // ============================================================

        ProcessFunction<JSONObject, DwdOrderInfo>.Context context =
                function.new Context() {

                    @Override
                    public Long timestamp() {

                        return null;
                    }


                    @Override
                    public TimerService timerService() {

                        return null;
                    }


                    @Override
                    public <X> void output(
                            OutputTag<X> outputTag,
                            X value) {

                        if (OrderDwdProcessFunction
                                .DIRTY_TAG
                                .getId()
                                .equals(
                                        outputTag.getId()
                                )) {

                            dirtyList.add(
                                    (DirtyData) value
                            );
                        }
                    }
                };


        // ============================================================
        // 7. 执行 Flink ProcessFunction
        // ============================================================

        /*
         * 关键点：
         *
         * 虽然 orderId = 0，
         *
         * 但这里不应该再向测试方法外抛异常。
         *
         * OrderDwdProcessFunction
         * 应该在内部 catch。
         */
        function.processElement(
                odsJson,
                context,
                collector
        );


        // ============================================================
        // 8. 非法订单不能进入正常DWD
        // ============================================================

        Assert.assertEquals(
                0,
                normalList.size()
        );


        // ============================================================
        // 9. 应该产生一条 DirtyData
        // ============================================================

        Assert.assertEquals(
                1,
                dirtyList.size()
        );


        DirtyData dirtyData =
                dirtyList.get(
                        0
                );


        // ============================================================
        // 10. 检查来源表
        // ============================================================

        Assert.assertEquals(
                "orders",
                dirtyData.getTableName()
        );


        // ============================================================
        // 11. 检查ODS操作类型
        // ============================================================

        Assert.assertEquals(
                "bootstrap",
                dirtyData.getOpType()
        );


        // ============================================================
        // 12. 检查异常类型
        // ============================================================

        Assert.assertEquals(
                "IllegalArgumentException",
                dirtyData.getErrorType()
        );


        // ============================================================
        // 13. 检查异常原因
        // ============================================================

        Assert.assertTrue(
                dirtyData
                        .getErrorMessage()
                        .contains(
                                "orderId非法"
                        )
        );


        // ============================================================
        // 14. 检查原始数据有没有保留
        // ============================================================

        Assert.assertTrue(
                dirtyData
                        .getRawData()
                        .contains(
                                "\"orderId\":\"0\""
                        )
        );
    }
}