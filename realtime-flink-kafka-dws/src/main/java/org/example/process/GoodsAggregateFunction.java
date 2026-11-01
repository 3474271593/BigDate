// 文件路径：src/main/java/org/example/process/GoodsAggregateFunction.java
package org.example.process;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.example.bean.GoodsDelta;
import org.example.bean.GoodsSummary;

import java.math.BigDecimal;

/**
 * 商品主题窗口增量聚合函数。
 *
 * 输入：
 * GoodsDelta
 *
 * 输出：
 * GoodsSummary
 *
 * 聚合指标：
 * 1. 下单订单数
 * 2. 下单商品件数
 * 3. 下单金额
 * 4. 支付订单数
 * 5. 支付商品件数
 * 6. 支付金额
 *
 * 支持正负 Delta，因此 GOODS_UPDATE_CORRECTION
 * 可以直接参与窗口聚合。
 */
public class GoodsAggregateFunction
        implements AggregateFunction<
        GoodsDelta,
        GoodsSummary,
        GoodsSummary> {

    /**
     * 创建窗口累加器。
     */
    @Override
    public GoodsSummary createAccumulator() {

        GoodsSummary summary =
                new GoodsSummary();

        summary.setOrderCount(0L);
        summary.setOrderGoodsNum(0L);
        summary.setOrderAmount(BigDecimal.ZERO);

        summary.setPaidOrderCount(0L);
        summary.setPaidGoodsNum(0L);
        summary.setPaidAmount(BigDecimal.ZERO);

        return summary;
    }

    /**
     * 每来一条 GoodsDelta，
     * 将其增量累加到 GoodsSummary。
     */
    @Override
    public GoodsSummary add(
            GoodsDelta delta,
            GoodsSummary accumulator) {

        if (delta == null) {
            return accumulator;
        }

        /*
         * 商品基本信息。
         *
         * goodsId原则上不会为空，
         * 因为Join层已经做过校验。
         */
        if (delta.getGoodsId() != null) {
            accumulator.setGoodsId(
                    delta.getGoodsId()
            );
        }

        /*
         * goodsName可能因为DIM查询失败暂时为空。
         *
         * 因此只有非空名称才覆盖，
         * 避免后续空名称覆盖已经存在的有效名称。
         */
        if (delta.getGoodsName() != null
                && !delta.getGoodsName()
                .trim()
                .isEmpty()) {

            accumulator.setGoodsName(
                    delta.getGoodsName()
            );
        }

        /*
         * 下单订单数。
         */
        accumulator.setOrderCount(
                accumulator.getOrderCount()
                        + delta.getOrderCountDelta()
        );

        /*
         * 下单商品件数。
         *
         * Correction可能是负数。
         */
        accumulator.setOrderGoodsNum(
                accumulator.getOrderGoodsNum()
                        + delta.getOrderGoodsNumDelta()
        );

        /*
         * 下单金额。
         */
        accumulator.setOrderAmount(
                safeDecimal(
                        accumulator.getOrderAmount()
                ).add(
                        safeDecimal(
                                delta.getOrderAmountDelta()
                        )
                )
        );

        /*
         * 支付订单数。
         */
        accumulator.setPaidOrderCount(
                accumulator.getPaidOrderCount()
                        + delta.getPaidOrderCountDelta()
        );

        /*
         * 支付商品件数。
         */
        accumulator.setPaidGoodsNum(
                accumulator.getPaidGoodsNum()
                        + delta.getPaidGoodsNumDelta()
        );

        /*
         * 支付金额。
         */
        accumulator.setPaidAmount(
                safeDecimal(
                        accumulator.getPaidAmount()
                ).add(
                        safeDecimal(
                                delta.getPaidAmountDelta()
                        )
                )
        );

        return accumulator;
    }

    /**
     * 返回最终窗口聚合结果。
     */
    @Override
    public GoodsSummary getResult(
            GoodsSummary accumulator) {

        return accumulator;
    }

    /**
     * Session Window等场景可能需要合并Accumulator。
     *
     * 当前虽然使用1分钟Tumbling Window，
     * 仍然完整实现merge。
     */
    @Override
    public GoodsSummary merge(
            GoodsSummary a,
            GoodsSummary b) {

        GoodsSummary result =
                new GoodsSummary();

        /*
         * goodsId。
         */
        if (a.getGoodsId() != null) {
            result.setGoodsId(
                    a.getGoodsId()
            );
        } else {
            result.setGoodsId(
                    b.getGoodsId()
            );
        }

        /*
         * 优先保留有效goodsName。
         */
        if (a.getGoodsName() != null
                && !a.getGoodsName()
                .trim()
                .isEmpty()) {

            result.setGoodsName(
                    a.getGoodsName()
            );

        } else {

            result.setGoodsName(
                    b.getGoodsName()
            );
        }

        result.setOrderCount(
                a.getOrderCount()
                        + b.getOrderCount()
        );

        result.setOrderGoodsNum(
                a.getOrderGoodsNum()
                        + b.getOrderGoodsNum()
        );

        result.setOrderAmount(
                safeDecimal(
                        a.getOrderAmount()
                ).add(
                        safeDecimal(
                                b.getOrderAmount()
                        )
                )
        );

        result.setPaidOrderCount(
                a.getPaidOrderCount()
                        + b.getPaidOrderCount()
        );

        result.setPaidGoodsNum(
                a.getPaidGoodsNum()
                        + b.getPaidGoodsNum()
        );

        result.setPaidAmount(
                safeDecimal(
                        a.getPaidAmount()
                ).add(
                        safeDecimal(
                                b.getPaidAmount()
                        )
                )
        );

        return result;
    }

    /**
     * BigDecimal空值保护。
     */
    private BigDecimal safeDecimal(
            BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}