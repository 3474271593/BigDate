package org.example.process;

import com.alibaba.fastjson.JSONObject;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.example.bean.DirtyData;
import org.example.bean.DwdOrderInfo;

/**
 * orders 表 DWD Flink 分流处理类。
 *
 * 职责：
 *
 * 1. 接收 ODS orders JSON
 * 2. 调用 OrderProcessFunction.cleanOrder() 执行真正的清洗
 * 3. 正常数据输出到 Flink 主流
 * 4. 异常数据转换成 DirtyData
 * 5. DirtyData 输出到 Side Output
 *
 * 注意：
 *
 * 这个类不负责具体字段清洗规则。
 *
 * 字段清洗规则统一由：
 *
 * OrderProcessFunction
 *
 * 负责。
 */
public class OrderDwdProcessFunction
        extends ProcessFunction<JSONObject, DwdOrderInfo> {


    /**
     * Dirty Data 侧输出标签。
     *
     * 后面可以通过：
     *
     * SingleOutputStreamOperator<DwdOrderInfo> orderStream
     *
     * orderStream.getSideOutput(DIRTY_TAG)
     *
     * 获取脏数据流。
     */
    public static final OutputTag<DirtyData> DIRTY_TAG =
            new OutputTag<DirtyData>(
                    "dwd-dirty-data"
            ) {
            };


    /**
     * Flink 每接收到一条 JSONObject，
     * 都会进入这里。
     *
     * @param value Flink 当前处理的 ODS JSON
     * @param ctx   ProcessFunction 上下文
     * @param out   主输出流 Collector
     */
    @Override
    public void processElement(
            JSONObject value,
            Context ctx,
            Collector<DwdOrderInfo> out) {


        try {

            // ========================================================
            // 1. 执行订单 DWD 清洗
            // ========================================================

            /*
             * 所有具体清洗规则仍然放在：
             *
             * OrderProcessFunction.cleanOrder()
             *
             * 例如：
             *
             * orderId 校验
             * 金额校验
             * isPay 校验
             * payTime 清洗
             * 时间格式校验
             * 时间顺序检查
             * orderStatus 校验
             * dataFlag 校验
             */
            DwdOrderInfo order =
                    OrderProcessFunction.cleanOrder(
                            value
                    );


            // ========================================================
            // 2. 正常数据进入主流
            // ========================================================

            /*
             * 如果 cleanOrder() 没有发生异常，
             * 说明这条数据属于正常 DWD 数据。
             *
             * 输出：
             *
             * DwdOrderInfo
             *
             * 后续这个主流最终会写入：
             *
             * dwd_order_info
             */
            out.collect(
                    order
            );


        } catch (Exception e) {

            // ========================================================
            // 3. 捕获清洗异常
            // ========================================================

            /*
             * 这里非常关键。
             *
             * 以前：
             *
             * cleanOrder()
             *      ↓
             * IllegalArgumentException
             *      ↓
             * 异常继续往外抛
             *
             * 如果直接运行在 Flink 中，
             * 可能导致 Task 失败并重启。
             *
             *
             * 现在：
             *
             * cleanOrder()
             *      ↓
             * Exception
             *      ↓
             * catch
             *      ↓
             * DirtyData
             *      ↓
             * Side Output
             */


            // ========================================================
            // 4. 将异常数据转换成 DirtyData
            // ========================================================

            DirtyData dirtyData =
                    DirtyDataBuilder.build(
                            value,
                            e
                    );


            // ========================================================
            // 5. 输出到 Side Output
            // ========================================================

            /*
             * 注意：
             *
             * 这里不能使用：
             *
             * out.collect(dirtyData)
             *
             * 因为主流的数据类型是：
             *
             * DwdOrderInfo
             *
             * 而 DirtyData 类型不同。
             *
             * 所以必须通过：
             *
             * ctx.output()
             *
             * 输出到 Side Output。
             */
            ctx.output(
                    DIRTY_TAG,
                    dirtyData
            );
        }
    }
}