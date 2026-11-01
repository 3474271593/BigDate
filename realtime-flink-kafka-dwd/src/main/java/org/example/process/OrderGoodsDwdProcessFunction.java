package org.example.process;

import com.alibaba.fastjson.JSONObject;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.example.bean.DirtyData;
import org.example.bean.DwdOrderGoods;

/**
 * order_goods DWD处理函数。
 *
 * 正常数据：
 * 输出到主流 DwdOrderGoods。
 *
 * 异常数据：
 * 输出到 DirtyData 侧输出流。
 */
public class OrderGoodsDwdProcessFunction
        extends ProcessFunction<JSONObject, DwdOrderGoods> {

    /**
     * order_goods脏数据侧输出Tag。
     */
    public static final OutputTag<DirtyData> DIRTY_TAG =
            new OutputTag<DirtyData>("order-goods-dirty") {
            };

    @Override
    public void processElement(
            JSONObject odsJson,
            Context ctx,
            Collector<DwdOrderGoods> out
    ) {

        try {
            /*
             * 调用已经通过20～27条测试的清洗逻辑。
             */
            DwdOrderGoods result =
                    OrderGoodsProcessFunction.cleanOrderGoods(odsJson);

            /*
             * 正常数据进入主流。
             */
            out.collect(result);

        } catch (Exception e) {

            /*
             * 清洗失败的数据不直接丢弃，
             * 转换成DirtyData进入侧输出。
             */
            DirtyData dirtyData =
                    DirtyDataBuilder.build(
                            odsJson,
                            e
                    );

            ctx.output(
                    DIRTY_TAG,
                    dirtyData
            );
        }
    }
}