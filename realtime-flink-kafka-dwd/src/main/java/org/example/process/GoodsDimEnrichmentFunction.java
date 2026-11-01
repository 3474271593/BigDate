package org.example.process;

import org.example.bean.DwdOrderGoods;

/**
 * 商品DIM维度补全规则。
 *
 * 该类不负责连接Redis，
 * 只负责决定DIM查询结果如何应用到DWD订单商品事实。
 */
public class GoodsDimEnrichmentFunction {

    public static final String GOODS_NAME_DIM_MISSING = "GOODS_NAME_DIM_MISSING";
    public static final String GOODS_NAME_DIM_LOOKUP_FAILED = "GOODS_NAME_DIM_LOOKUP_FAILED";

    private GoodsDimEnrichmentFunction() {
    }

    /**
     * 使用DIM商品名称补全DWD订单商品事实。
     *
     * 规则：
     *
     * 1. 原始goodsName存在：
     *    保留订单商品快照，不覆盖。
     *
     * 2. 原始goodsName缺失，DIM存在goodsName：
     *    使用DIM名称补全。
     *
     * 3. 原始goodsName缺失，DIM也没有名称：
     *    保留交易事实，并记录质量告警。
     */
    public static DwdOrderGoods enrichGoodsName(DwdOrderGoods orderGoods, String dimGoodsName) {
        if (orderGoods == null) {
            return null;
        }

        if (!isBlank(orderGoods.getGoodsName())) {
            return orderGoods;
        }

        if (isBlank(dimGoodsName)) {
            orderGoods.setQualityWarning(GOODS_NAME_DIM_MISSING);
            return orderGoods;
        }

        orderGoods.setGoodsName(dimGoodsName.trim());
        orderGoods.setQualityWarning(null);

        return orderGoods;
    }

    /**
     * Redis查询失败时记录质量告警。
     *
     * Redis故障不能导致有效交易事实进入Dirty。
     */
    public static DwdOrderGoods markLookupFailed(DwdOrderGoods orderGoods) {
        if (orderGoods == null) {
            return null;
        }

        if (isBlank(orderGoods.getGoodsName())) {
            orderGoods.setQualityWarning(GOODS_NAME_DIM_LOOKUP_FAILED);
        }

        return orderGoods;
    }

    /**
     * 判断字符串是否为空。
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}