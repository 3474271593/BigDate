package org.example;

import org.example.bean.ShopSummary;
import org.example.process.ShopBootstrapCalculator;
import org.example.process.ShopBootstrapHistoryReader;
import org.example.process.ShopBootstrapResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shop Bootstrap 只读演练程序。
 *
 * 功能：
 * 1. 从固定 Kafka 历史边界读取 dwd_order_info；
 * 2. 按 orderId 保留最终快照；
 * 3. 计算 Shop 历史分钟聚合结果；
 * 4. 打印统计信息和少量样例。
 *
 * 注意：
 * - 不写 Kafka；
 * - 不提交 Kafka Offset；
 * - 不修改 Flink Checkpoint；
 * - 不启动正式 Shop 实时任务。
 */
public final class ShopBootstrapDryRunApp {

    private ShopBootstrapDryRunApp() {
    }

    public static void main(String[] args) {

        System.out.println(
                "========================================"
        );
        System.out.println(
                "SHOP BOOTSTRAP DRY RUN START"
        );
        System.out.println(
                "========================================"
        );

        /*
         * 1. 读取固定历史边界。
         */
        List<org.example.bean.DwdOrderInfo> records =
                ShopBootstrapHistoryReader.readHistory();

        /*
         * 2. 计算 Shop 历史基线。
         */
        ShopBootstrapResult result =
                ShopBootstrapCalculator.calculate(records);

        /*
         * 3. 基础一致性校验。
         */
        validate(result);

        /*
         * 4. 输出统计。
         */
        printStatistics(result);

        /*
         * 5. 打印前 10 个 ShopSummary 样例。
         */
        printSamples(
                result.getSummaries(),
                10
        );

        System.out.println(
                "========================================"
        );
        System.out.println(
                "SHOP BOOTSTRAP DRY RUN SUCCESS"
        );
        System.out.println(
                "========================================"
        );
    }

    private static void validate(
            ShopBootstrapResult result) {

        /*
         * 当前固定历史边界：
         *
         * P0 [0, 20028)
         * P1 [0, 0)
         * P2 [0, 0)
         *
         * 所以读取记录数必须为 20028。
         */
        require(
                "read records",
                20015L,
                result.getReadCount()
        );

        /*
         * 根据 Trade Bootstrap 已验证的同一份
         * dwd_order_info 历史数据：
         *
         * 最终唯一 orderId = 20015。
         *
         * Shop 使用的是同一历史边界和同一订单流，
         * 因此最终快照数量也必须一致。
         */
        require(
                "unique orders",
                20015L,
                result.getUniqueOrderCount()
        );

        /*
         * 对于当前数据，只要最终订单存在合法 createTime
         * 和 shopId，就应该形成 SHOP_ORDER。
         *
         * 暂时不把这里强制写死为 20015，
         * 后面通过真实结果检查数据质量。
         */
        if (result.getOrderCount() <= 0L) {
            throw new IllegalStateException(
                    "No Shop order metric was generated"
            );
        }

        if (result.getSummaries().isEmpty()) {
            throw new IllegalStateException(
                    "No ShopSummary was generated"
            );
        }
    }

    private static void require(
            String name,
            long expected,
            long actual) {

        if (expected != actual) {
            throw new IllegalStateException(
                    name
                            + " mismatch: expected="
                            + expected
                            + ", actual="
                            + actual
            );
        }
    }

    private static void printStatistics(
            ShopBootstrapResult result) {

        Set<Long> shopIds =
                new HashSet<>();

        for (ShopSummary summary :
                result.getSummaries()) {

            if (summary.getShopId() != null) {
                shopIds.add(
                        summary.getShopId()
                );
            }
        }

        System.out.println();
        System.out.println(
                "========== Shop Bootstrap Statistics =========="
        );

        System.out.println(
                "读取总数="
                        + result.getReadCount()
        );

        System.out.println(
                "bootstrap="
                        + result.getBootstrapCount()
        );

        System.out.println(
                "UPDATE="
                        + result.getUpdateCount()
        );

        System.out.println(
                "最终唯一orderId="
                        + result.getUniqueOrderCount()
        );

        System.out.println(
                "历史最早createTime="
                        + result.getEarliestCreateTime()
        );

        System.out.println(
                "历史最晚createTime="
                        + result.getLatestCreateTime()
        );

        System.out.println(
                "店铺数量="
                        + shopIds.size()
        );

        System.out.println(
                "ShopSummary窗口数量="
                        + result.getSummaries().size()
        );

        System.out.println(
                "orderCount="
                        + result.getOrderCount()
        );

        System.out.println(
                "orderAmount="
                        + result.getOrderAmount()
                        .toPlainString()
        );

        System.out.println(
                "paidOrderCount="
                        + result.getPaidOrderCount()
        );

        System.out.println(
                "paidAmount="
                        + result.getPaidAmount()
                        .toPlainString()
        );

        System.out.println(
                "================================================"
        );
    }

    private static void printSamples(
            List<ShopSummary> summaries,
            int limit) {

        System.out.println();
        System.out.println(
                "========== ShopSummary Samples =========="
        );

        int count =
                Math.min(
                        limit,
                        summaries.size()
                );

        for (int i = 0; i < count; i++) {

            System.out.println(
                    "[" + (i + 1) + "] "
                            + summaries.get(i)
            );
        }

        System.out.println(
                "========================================="
        );
    }
}
