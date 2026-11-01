// 文件路径：src/main/java/org/example/bean/ShopAccumulator.java
package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Shop 主题一分钟窗口内部累加器。
 *
 * 用于累计：
 *
 * 1. 下单订单数
 * 2. 下单用户集合（用于精确 UV）
 * 3. 下单金额
 * 4. 支付订单数
 * 5. 支付用户集合（用于精确 UV）
 * 6. 支付金额
 *
 * 注意：
 * 这是窗口内部状态，不是最终 Kafka 输出对象。
 */
public class ShopAccumulator implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 下单订单数。
     */
    private long orderCount;

    /**
     * 下单用户集合。
     *
     * 使用 Set 去重，同一个用户在同一个店铺、
     * 同一个一分钟窗口内下多笔订单，只计算一个 UV。
     */
    private Set<Long> orderUserIds = new HashSet<>();

    /**
     * 下单金额。
     */
    private BigDecimal orderAmount = BigDecimal.ZERO;

    /**
     * 支付订单数。
     */
    private long paidOrderCount;

    /**
     * 支付用户集合。
     */
    private Set<Long> paidUserIds = new HashSet<>();

    /**
     * 支付金额。
     */
    private BigDecimal paidAmount = BigDecimal.ZERO;

    public ShopAccumulator() {
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public Set<Long> getOrderUserIds() {
        return orderUserIds;
    }

    public void setOrderUserIds(Set<Long> orderUserIds) {
        this.orderUserIds =
                orderUserIds == null
                        ? new HashSet<>()
                        : orderUserIds;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount =
                orderAmount == null
                        ? BigDecimal.ZERO
                        : orderAmount;
    }

    public long getPaidOrderCount() {
        return paidOrderCount;
    }

    public void setPaidOrderCount(long paidOrderCount) {
        this.paidOrderCount = paidOrderCount;
    }

    public Set<Long> getPaidUserIds() {
        return paidUserIds;
    }

    public void setPaidUserIds(Set<Long> paidUserIds) {
        this.paidUserIds =
                paidUserIds == null
                        ? new HashSet<>()
                        : paidUserIds;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount =
                paidAmount == null
                        ? BigDecimal.ZERO
                        : paidAmount;
    }

    @Override
    public String toString() {
        return "ShopAccumulator{" +
                "orderCount=" + orderCount +
                ", orderUserIds=" + orderUserIds +
                ", orderAmount=" + orderAmount +
                ", paidOrderCount=" + paidOrderCount +
                ", paidUserIds=" + paidUserIds +
                ", paidAmount=" + paidAmount +
                '}';
    }
}