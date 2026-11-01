package org.example.process;

import com.alibaba.fastjson.JSON;
import org.example.bean.TradeSummary;

/**
 * DWS交易汇总结果JSON序列化器。
 *
 * 作用：
 *
 * TradeSummary
 *      ↓
 * JSON
 *      ↓
 * Kafka：dws_trade_summary
 *
 * 当前只负责序列化，不负责Kafka发送。
 */
public final class TradeSummarySerializer {

    private TradeSummarySerializer() {
    }

    /**
     * 将TradeSummary转换为Kafka需要的JSON字符串。
     *
     * @param tradeSummary DWS交易窗口汇总结果
     * @return JSON字符串
     */
    public static String serialize(TradeSummary tradeSummary) {

        if (tradeSummary == null) {
            return null;
        }

        return JSON.toJSONString(tradeSummary);
    }
}