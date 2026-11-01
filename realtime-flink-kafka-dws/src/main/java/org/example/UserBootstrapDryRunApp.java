// 文件路径：src/main/java/org/example/UserBootstrapDryRunApp.java
package org.example;

import org.example.bean.DwdOrderInfo;
import org.example.bean.UserSummary;
import org.example.process.UserBootstrapCalculator;
import org.example.process.UserBootstrapHistoryReader;
import org.example.process.UserBootstrapResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User Bootstrap 只读演练程序。
 *
 * 功能：
 * 1. 从固定 Kafka 历史边界读取 dwd_order_info；
 * 2. 按 orderId 保留最终快照；
 * 3. 计算 User 历史分钟聚合结果；
 * 4. 打印统计信息和少量样例。
 *
 * 注意：
 * - 不写 Kafka；
 * - 不提交 Kafka Offset；
 * - 不修改 Flink Checkpoint；
 * - 不启动正式 User 实时任务。
 */
public final class UserBootstrapDryRunApp {

    private UserBootstrapDryRunApp() {
    }

    public static void main(String[] args) {

        System.out.println(
                "========================================"
        );

        System.out.println(
                "USER BOOTSTRAP DRY RUN START"
        );

        System.out.println(
                "========================================"
        );

        /*
         * ============================================================
         * 1. 读取固定历史边界
         * ============================================================
         */
        List<DwdOrderInfo> records =
                UserBootstrapHistoryReader.readHistory();

        /*
         * ============================================================
         * 2. 计算 User 历史基线
         * ============================================================
         */
        UserBootstrapResult result =
                UserBootstrapCalculator.calculate(
                        records
                );

        /*
         * ============================================================
         * 3. 基础一致性校验
         * ============================================================
         */
        validate(
                result
        );

        /*
         * ============================================================
         * 4. 输出统计
         * ============================================================
         */
        printStatistics(
                result
        );

        /*
         * ============================================================
         * 5. 打印前 10 个 UserSummary 样例
         * ============================================================
         */
        printSamples(
                result.getSummaries(),
                10
        );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "USER BOOTSTRAP DRY RUN SUCCESS"
        );

        System.out.println(
                "========================================"
        );
    }

    /**
     * Dry Run 基础一致性校验。
     */
    private static void validate(
            UserBootstrapResult result) {

        /*
         * 当前固定 User 历史边界：
         *
         * P0 [0, 20030)
         * P1 [0, 0)
         * P2 [0, 0)
         *
         * 所以历史读取记录数必须是 20030。
         */
        require(
                "read records",
                20015L,
                result.getReadCount()
        );

        /*
         * 当前 dwd_order_info：
         *
         * 虽然历史区间中包含 UPDATE，
         * 但按 orderId 折叠最终状态后，
         * 唯一订单数量仍应为 20015。
         */
        require(
                "unique orders",
                20015L,
                result.getUniqueOrderCount()
        );

        /*
         * User 主题需要合法 userId + createTime
         * 才能够产生 USER_ORDER。
         *
         * 这里暂时不把 orderCount 强制写死，
         * 先通过真实历史数据检查数据质量。
         */
        if (result.getOrderCount() <= 0L) {

            throw new IllegalStateException(
                    "No User order metric was generated"
            );
        }

        /*
         * 必须至少产生一条 UserSummary。
         */
        if (result.getSummaries().isEmpty()) {

            throw new IllegalStateException(
                    "No UserSummary was generated"
            );
        }
    }

    /**
     * 固定值校验。
     */
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

    /**
     * 打印 User Bootstrap 统计信息。
     */
    private static void printStatistics(
            UserBootstrapResult result) {

        /*
         * 统计历史 UserSummary 中出现过的用户。
         *
         * 注意：
         * 这里只用于 Dry Run 数据检查，
         * 不作为正式 ADS 指标。
         */
        Set<Long> userIds =
                new HashSet<>();

        for (UserSummary summary :
                result.getSummaries()) {

            if (summary.getUserId() != null) {

                userIds.add(
                        summary.getUserId()
                );
            }
        }

        System.out.println();

        System.out.println(
                "========== User Bootstrap Statistics =========="
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
                "用户数量="
                        + userIds.size()
        );

        System.out.println(
                "UserSummary窗口数量="
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

    /**
     * 打印少量 UserSummary 样例。
     */
    private static void printSamples(
            List<UserSummary> summaries,
            int limit) {

        System.out.println();

        System.out.println(
                "========== UserSummary Samples =========="
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
