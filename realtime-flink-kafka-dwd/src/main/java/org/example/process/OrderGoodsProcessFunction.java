package org.example.process;

import com.alibaba.fastjson.JSONObject;
import org.example.bean.DwdOrderGoods;

import java.math.BigDecimal;

/**
 * order_goods DWD清洗逻辑。
 *
 * 负责：
 *
 * 1. 读取统一后的ODS数据
 * 2. 校验核心业务字段
 * 3. 类型转换
 * 4. 计算商品明细金额
 * 5. 转换为DwdOrderGoods
 *
 * 当前硬错误规则：
 *
 * ogId <= 0
 * orderId <= 0
 * goodsId <= 0
 * goodsNum <= 0
 * goodsPrice < 0
 *
 * goodsName不再作为硬错误字段。
 *
 * 原因：
 * goodsName属于商品描述字段。
 * 当goodsName缺失，但goodsId等核心交易事实字段有效时，
 * 仍然保留该订单商品事实。
 *
 * 后续通过goodsId关联Redis商品维度：
 *
 * dim:goods:<goodsId>
 *
 * 尝试补全goodsName。
 *
 * 当前不会对goodsType做枚举限制，
 * 因为原始SQL中没有明确标注真实业务枚举。
 */
public class OrderGoodsProcessFunction {

    private OrderGoodsProcessFunction() {
    }

    /**
     * 清洗一条order_goods数据。
     *
     * 输入结构：
     *
     * {
     *     "table": "order_goods",
     *     "type": "bootstrap/INSERT/UPDATE/DELETE",
     *     "data": {
     *         ...
     *     }
     * }
     *
     * @param odsJson 统一结构后的ODS数据
     * @return DwdOrderGoods
     */
    public static DwdOrderGoods cleanOrderGoods(JSONObject odsJson) {

        // ============================================================
        // 1. 校验ODS对象
        // ============================================================

        if (odsJson == null) {
            throw new IllegalArgumentException("ODS数据不能为空");
        }

        // ============================================================
        // 2. 校验业务表
        // ============================================================

        String table = odsJson.getString("table");

        if (!"order_goods".equals(table)) {
            throw new IllegalArgumentException(
                    "非法业务表，期望order_goods，实际：" + table
            );
        }

        // ============================================================
        // 3. 获取data
        // ============================================================

        JSONObject data = odsJson.getJSONObject("data");

        if (data == null) {
            throw new IllegalArgumentException("order_goods.data不能为空");
        }

        // ============================================================
        // 4. 解析核心主键字段
        // ============================================================

        Long ogId = getLong(data, "ogId");
        Long orderId = getLong(data, "orderId");
        Long goodsId = getLong(data, "goodsId");

        // ============================================================
        // 5. 校验ogId
        // ============================================================

        if (ogId == null || ogId <= 0) {
            throw new IllegalArgumentException(
                    "非法ogId：" + ogId
            );
        }

        // ============================================================
        // 6. 校验orderId
        // ============================================================

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException(
                    "非法orderId：" + orderId
            );
        }

        // ============================================================
        // 7. 校验goodsId
        // ============================================================

        if (goodsId == null || goodsId <= 0) {
            throw new IllegalArgumentException(
                    "非法goodsId：" + goodsId
            );
        }

        // ============================================================
        // 8. 校验商品数量
        // ============================================================

        Long goodsNum = getLong(data, "goodsNum");

        if (goodsNum == null || goodsNum <= 0) {
            throw new IllegalArgumentException(
                    "非法goodsNum：" + goodsNum
            );
        }

        // ============================================================
        // 9. 校验商品价格
        // ============================================================

        BigDecimal goodsPrice = getBigDecimal(data, "goodsPrice");

        if (goodsPrice == null) {
            throw new IllegalArgumentException(
                    "goodsPrice不能为空"
            );
        }

        /*
         * 价格等于0暂时允许。
         *
         * 原因：
         * 当前业务表没有明确说明0元商品非法，
         * 不排除赠品、活动商品等情况。
         *
         * 只有负数金额作为硬错误。
         */
        if (goodsPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "goodsPrice不能为负数：" + goodsPrice
            );
        }

        // ============================================================
        // 10. 读取商品名称
        // ============================================================

        /*
         * goodsName不再作为硬错误字段。
         *
         * 即使goodsName为空，只要：
         *
         * ogId
         * orderId
         * goodsId
         * goodsNum
         * goodsPrice
         *
         * 等核心交易事实字段有效，
         * 就保留该条order_goods事实。
         *
         * 后续通过goodsId查询Redis商品维度：
         *
         * dim:goods:<goodsId>
         *
         * 尝试补全goodsName。
         */
        String goodsName = normalizeNullableString(
                data.getString("goodsName")
        );

        // ============================================================
        // 11. 计算商品明细金额
        // ============================================================

        /*
         * 金额必须使用BigDecimal。
         *
         * 示例：
         *
         * goodsNum = 5
         * goodsPrice = 20.90
         *
         * goodsAmount = 104.50
         */
        BigDecimal goodsAmount = goodsPrice.multiply(
                BigDecimal.valueOf(goodsNum)
        );

        // ============================================================
        // 12. 构建DWD对象
        // ============================================================

        DwdOrderGoods result = new DwdOrderGoods();

        result.setOgId(ogId);
        result.setOrderId(orderId);
        result.setGoodsId(goodsId);
        result.setGoodsNum(goodsNum);

        result.setGoodsPrice(goodsPrice);
        result.setGoodsAmount(goodsAmount);

        result.setGoodsSpecId(
                getInteger(data, "goodsSpecId")
        );

        result.setGoodsSpecNames(
                data.getString("goodsSpecNames")
        );

        /*
         * goodsName允许为空。
         *
         * 当前阶段保留清洗后的原始值，
         * 下一阶段再通过Redis DIM补全。
         */
        result.setGoodsName(
                goodsName
        );

        result.setGoodsImg(
                data.getString("goodsImg")
        );

        result.setExtraJson(
                data.getString("extraJson")
        );

        result.setGoodsType(
                getInteger(data, "goodsType")
        );

        result.setCommissionRate(
                getBigDecimal(data, "commissionRate")
        );

        result.setGoodsCode(
                data.getString("goodsCode")
        );

        result.setPromotionJson(
                data.getString("promotionJson")
        );

        // ============================================================
        // 13. 保留CDC操作类型
        // ============================================================

        result.setOpType(
                odsJson.getString("type")
        );

        // ============================================================
        // 14. 记录Flink处理时间
        // ============================================================

        result.setProcessTs(
                System.currentTimeMillis()
        );

        return result;
    }

    /**
     * 规范化允许为空的字符串字段。
     *
     * null：
     * 保持null。
     *
     * 空字符串或纯空格：
     * 统一转换为空字符串。
     *
     * 正常字符串：
     * 去除首尾空格后保留。
     */
    private static String normalizeNullableString(String value) {

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    /**
     * 安全读取Long。
     */
    private static Long getLong(
            JSONObject data,
            String fieldName
    ) {

        Object value = data.get(fieldName);

        if (value == null) {
            return null;
        }

        try {
            return Long.valueOf(
                    String.valueOf(value)
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + "不是合法Long：" + value,
                    e
            );
        }
    }

    /**
     * 安全读取Integer。
     */
    private static Integer getInteger(
            JSONObject data,
            String fieldName
    ) {

        Object value = data.get(fieldName);

        if (value == null) {
            return null;
        }

        try {
            return Integer.valueOf(
                    String.valueOf(value)
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + "不是合法Integer：" + value,
                    e
            );
        }
    }

    /**
     * 安全读取BigDecimal。
     *
     * 金额和比率字段统一使用BigDecimal，
     * 避免double浮点精度问题。
     */
    private static BigDecimal getBigDecimal(
            JSONObject data,
            String fieldName
    ) {

        Object value = data.get(fieldName);

        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(
                    String.valueOf(value)
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + "不是合法BigDecimal：" + value,
                    e
            );
        }
    }
}