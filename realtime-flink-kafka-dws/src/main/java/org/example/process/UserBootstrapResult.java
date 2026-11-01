package org.example.process;

import org.example.bean.UserSummary;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * User 历史 Bootstrap 计算结果。
 *
 * 保存两类信息：
 *
 * 1. Bootstrap 执行统计
 *    - Kafka读取数量
 *    - bootstrap数量
 *    - update数量
 *    - 唯一订单数量
 *    - 历史时间范围
 *    - 总订单/支付指标
 *
 * 2. 最终生成的 UserSummary 列表
 */
public class UserBootstrapResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 从 Kafka 历史区间实际读取的记录数。
     */
    private final long readCount;

    /**
     * opType = BOOTSTRAP 的记录数。
     */
    private final long bootstrapCount;

    /**
     * opType = UPDATE 的记录数。
     */
    private final long updateCount;

    /**
     * 按 orderId 折叠后的唯一订单数。
     */
    private final long uniqueOrderCount;

    /**
     * 历史订单最早 createTime。
     */
    private final String earliestCreateTime;

    /**
     * 历史订单最晚 createTime。
     */
    private final String latestCreateTime;

    /**
     * User 历史订单总数。
     */
    private final long orderCount;

    /**
     * User 历史已支付订单总数。
     */
    private final long paidOrderCount;

    /**
     * User 历史下单总金额。
     */
    private final BigDecimal orderAmount;

    /**
     * User 历史支付总金额。
     */
    private final BigDecimal paidAmount;

    /**
     * 最终按照：
     *
     * userId + 1分钟
     *
     * 聚合产生的历史 UserSummary。
     */
    private final List<UserSummary> summaries;

    public UserBootstrapResult(
            long readCount,
            long bootstrapCount,
            long updateCount,
            long uniqueOrderCount,
            String earliestCreateTime,
            String latestCreateTime,
            long orderCount,
            long paidOrderCount,
            BigDecimal orderAmount,
            BigDecimal paidAmount,
            List<UserSummary> summaries) {

        this.readCount = readCount;
        this.bootstrapCount = bootstrapCount;
        this.updateCount = updateCount;
        this.uniqueOrderCount = uniqueOrderCount;

        this.earliestCreateTime =
                earliestCreateTime;

        this.latestCreateTime =
                latestCreateTime;

        this.orderCount =
                orderCount;

        this.paidOrderCount =
                paidOrderCount;

        this.orderAmount =
                orderAmount == null
                        ? BigDecimal.ZERO
                        : orderAmount;

        this.paidAmount =
                paidAmount == null
                        ? BigDecimal.ZERO
                        : paidAmount;

        this.summaries =
                summaries == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(
                        summaries
                );
    }

    public long getReadCount() {
        return readCount;
    }

    public long getBootstrapCount() {
        return bootstrapCount;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public long getUniqueOrderCount() {
        return uniqueOrderCount;
    }

    public String getEarliestCreateTime() {
        return earliestCreateTime;
    }

    public String getLatestCreateTime() {
        return latestCreateTime;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public long getPaidOrderCount() {
        return paidOrderCount;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public List<UserSummary> getSummaries() {
        return summaries;
    }
}