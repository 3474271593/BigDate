package org.example.process;

import com.alibaba.fastjson.JSONObject;

/**
 * order_goods 历史数据回灌过滤器。
 *
 * 本次历史回灌只处理：
 *
 * table = order_goods
 * type  = bootstrap
 *
 * 其他表以及 INSERT、UPDATE、DELETE 等实时 CDC 数据全部忽略。
 */
public class OrderGoodsBackfillFilter {

    /**
     * 工具类不允许实例化。
     */
    private OrderGoodsBackfillFilter() {
    }

    /**
     * 判断当前 ODS 数据是否属于需要回灌的 order_goods 历史数据。
     *
     * @param odsJson ODS 数据
     * @return true：需要回灌；false：忽略
     */
    public static boolean shouldBackfill(JSONObject odsJson) {

        if (odsJson == null) {
            return false;
        }

        String table = odsJson.getString("table");
        String type = odsJson.getString("type");

        return "order_goods".equals(table)
                && "bootstrap".equalsIgnoreCase(type);
    }
}