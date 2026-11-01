// 文件路径：src/main/java/org/example/bean/UserAccumulator.java
package org.example.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * User DWS 用户主题窗口累加器。
 *
 * 窗口粒度：
 *
 * userId + 1分钟
 *
 * 负责累计：
 *
 * 1. 下单订单数
 * 2. 下单店铺集合
 * 3. 下单金额
 * 4. 支付订单数
 * 5. 支付店铺集合
 * 6. 支付金额
 *
 * 最终：
 *
 * orderShopCount = orderShopIds.size()
 * paidShopCount  = paidShopIds.size()
 */
public class UserAccumulator implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前用户ID。
     */
    private Long userId;

    /**
     * 下单订单数。
     */
    private long orderCount;

    /**
     * 下单涉及的店铺集合。
     *
     * 使用 Set 保证同一家店在一个窗口内只统计一次。
     */
    private Set<Long> orderShopIds;

    /**
     * 下单金额。
     */
    private BigDecimal orderAmount;

    /**
     * 支付订单数。
     */
    private long paidOrderCount;

    /**
     * 支付涉及的店铺集合。
     */
    private Set<Long> paidShopIds;

    /**
     * 支付金额。
     */
    private BigDecimal paidAmount;

    public UserAccumulator() {
        this.orderCount = 0L;
        this.orderShopIds = new HashSet<>();
        this.orderAmount = BigDecimal.ZERO;

        this.paidOrderCount = 0L;
        this.paidShopIds = new HashSet<>();
        this.paidAmount = BigDecimal.ZERO;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public Set<Long> getOrderShopIds() {
        return orderShopIds;
    }

    public void setOrderShopIds(Set<Long> orderShopIds) {
        this.orderShopIds =
                orderShopIds == null
                        ? new HashSet<>()
                        : orderShopIds;
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

    public Set<Long> getPaidShopIds() {
        return paidShopIds;
    }

    public void setPaidShopIds(Set<Long> paidShopIds) {
        this.paidShopIds =
                paidShopIds == null
                        ? new HashSet<>()
                        : paidShopIds;
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

    /**
     * 增加下单订单数。
     */
    public void addOrderCount(long value) {
        this.orderCount += value;
    }

    /**
     * 增加支付订单数。
     */
    public void addPaidOrderCount(long value) {
        this.paidOrderCount += value;
    }

    /**
     * 增加一个下单店铺。
     */
    public void addOrderShopId(Long shopId) {
        if (shopId != null && shopId > 0) {
            this.orderShopIds.add(shopId);
        }
    }

    /**
     * 增加一个支付店铺。
     */
    public void addPaidShopId(Long shopId) {
        if (shopId != null && shopId > 0) {
            this.paidShopIds.add(shopId);
        }
    }

    /**
     * 累加下单金额。
     */
    public void addOrderAmount(BigDecimal amount) {
        if (amount != null) {
            this.orderAmount =
                    this.orderAmount.add(amount);
        }
    }

    /**
     * 累加支付金额。
     */
    public void addPaidAmount(BigDecimal amount) {
        if (amount != null) {
            this.paidAmount =
                    this.paidAmount.add(amount);
        }
    }

    /**
     * 获取下单涉及的不同店铺数。
     */
    public long getOrderShopCount() {
        return orderShopIds == null
                ? 0L
                : orderShopIds.size();
    }

    /**
     * 获取支付涉及的不同店铺数。
     */
    public long getPaidShopCount() {
        return paidShopIds == null
                ? 0L
                : paidShopIds.size();
    }

    @Override
    public String toString() {
        return "UserAccumulator{" +
                "userId=" + userId +
                ", orderCount=" + orderCount +
                ", orderShopIds=" + orderShopIds +
                ", orderAmount=" + orderAmount +
                ", paidOrderCount=" + paidOrderCount +
                ", paidShopIds=" + paidShopIds +
                ", paidAmount=" + paidAmount +
                '}';
    }
}