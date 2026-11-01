package org.example.process;

import org.apache.flink.util.OutputTag;
import org.example.bean.TradeDelta;

/**
 * DWS交易主题迟到数据配置。
 *
 * 统一维护交易主题迟到数据Side Output标签。
 */
public final class TradeLateDataConfig {

    /**
     * 迟到交易数据Side Output标签。
     *
     * 使用匿名子类保留TradeDelta泛型类型信息。
     */
    private static final OutputTag<TradeDelta> LATE_DATA_OUTPUT_TAG =
            new OutputTag<TradeDelta>("trade-late-data") {
            };

    /**
     * 工具类禁止实例化。
     */
    private TradeLateDataConfig() {
    }

    /**
     * 获取迟到交易数据Side Output标签。
     */
    public static OutputTag<TradeDelta> getLateDataOutputTag() {
        return LATE_DATA_OUTPUT_TAG;
    }
}