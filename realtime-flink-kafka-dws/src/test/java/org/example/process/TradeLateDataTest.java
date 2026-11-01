package org.example.process;

import org.apache.flink.util.OutputTag;
import org.example.bean.TradeDelta;
import org.junit.Assert;
import org.junit.Test;

/**
 * DWS交易主题迟到数据测试。
 *
 * 目标：
 * 验证交易主题提供独立的迟到数据Side Output标签，
 * 后续超过Watermark的TradeDelta统一进入该输出。
 */
public class TradeLateDataTest {

    @Test
    public void shouldProvideTradeLateDataOutputTag() {

        OutputTag<TradeDelta> outputTag =
                TradeLateDataConfig.getLateDataOutputTag();

        Assert.assertNotNull(
                outputTag
        );

        Assert.assertEquals(
                "trade-late-data",
                outputTag.getId()
        );
    }
}