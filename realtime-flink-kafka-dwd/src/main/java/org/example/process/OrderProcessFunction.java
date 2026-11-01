package org.example.process;

import com.alibaba.fastjson.JSONObject;
import org.example.bean.DwdOrderInfo;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * orders 表 DWD 清洗处理类。
 *
 * 当前功能：
 *
 * 1. 校验 ODS 数据结构
 * 2. 校验 orderId 主键
 * 3. 转换并校验金额字段
 * 4. 检查订单金额一致性
 * 5. 校验 orderStatus
 * 6. 校验 isPay
 * 7. 清洗 payTime
 * 8. 校验 dataFlag
 * 9. 校验时间格式
 * 10. 检查 createTime / deliveryTime / receiveTime 顺序
 * 11. 生成 qualityWarning
 * 12. 保留 ODS 操作类型 bootstrap / insert / update / delete
 *
 * 注意：
 *
 * 当前非法数据通过 IllegalArgumentException 抛出。
 *
 * 下一阶段会改造成：
 *
 * 正常数据 -> dwd_order_info
 * 异常数据 -> dwd_dirty_data
 *
 * 避免一条脏数据导致 Flink Job 失败。
 */
public class OrderProcessFunction {

    /**
     * 金额允许误差。
     *
     * 差值 <= 0.01：
     * 允许。
     *
     * 差值 > 0.01：
     * 添加 AMOUNT_MISMATCH。
     */
    private static final BigDecimal AMOUNT_TOLERANCE =
            new BigDecimal("0.01");

    /**
     * orders 表时间统一格式。
     */
    private static final String DATE_TIME_PATTERN =
            "yyyy-MM-dd HH:mm:ss";


    /**
     * 工具类不需要创建对象。
     */
    private OrderProcessFunction() {
    }


    /**
     * 将一条 ODS orders JSON
     * 清洗成为 DWD 订单对象。
     *
     * ODS实际结构：
     *
     * {
     *     "table":"orders",
     *     "type":"bootstrap",
     *     "data":{
     *         ...
     *     }
     * }
     *
     * @param odsJson ODS数据
     * @return DwdOrderInfo
     */
    public static DwdOrderInfo cleanOrder(JSONObject odsJson) {

        // ============================================================
        // 1. ODS 数据不能为空
        // ============================================================

        if (odsJson == null) {

            throw new IllegalArgumentException(
                    "ODS订单数据不能为空"
            );
        }


        // ============================================================
        // 2. 获取 ODS 元数据
        // ============================================================

        String table =
                odsJson.getString(
                        "table"
                );

        String opType =
                odsJson.getString(
                        "type"
                );


        /*
         * 当前处理类只处理 orders 表。
         */
        if (!"orders".equals(table)) {

            throw new IllegalArgumentException(
                    "当前数据不是 orders 表，table=" + table
            );
        }


        // ============================================================
        // 3. 获取 data
        // ============================================================

        JSONObject data =
                odsJson.getJSONObject(
                        "data"
                );


        if (data == null) {

            throw new IllegalArgumentException(
                    "orders 数据中的 data 不能为空"
            );
        }


        // ============================================================
        // 4. 创建 DWD 订单对象
        // ============================================================

        DwdOrderInfo order =
                new DwdOrderInfo();


        // ============================================================
        // 5. orderId 主键
        // ============================================================

        Long orderId =
                getLong(
                        data,
                        "orderId"
                );


        /*
         * orderId 是订单核心主键。
         *
         * 必须：
         *
         * != null
         * > 0
         */
        if (orderId == null
                || orderId <= 0) {

            throw new IllegalArgumentException(
                    "orderId非法，orderId=" + orderId
            );
        }


        order.setOrderId(
                orderId
        );


        // ============================================================
        // 6. 基础字段
        // ============================================================

        order.setOrderNo(
                getString(
                        data,
                        "orderNo"
                )
        );


        order.setShopId(
                getLong(
                        data,
                        "shopId"
                )
        );


        order.setUserId(
                getLong(
                        data,
                        "userId"
                )
        );


        // ============================================================
        // 7. orderStatus
        // ============================================================

        Integer orderStatus =
                getInteger(
                        data,
                        "orderStatus"
                );


        /*
         * orderStatus 合法状态：
         *
         * -3 用户拒收
         * -2 未付款订单
         * -1 用户取消
         *  0 待发货
         *  1 配送中
         *  2 用户确认收货
         */
        validateAllowedInteger(
                orderStatus,
                "orderStatus",
                -3,
                -2,
                -1,
                0,
                1,
                2
        );


        order.setOrderStatus(
                orderStatus
        );


        // ============================================================
        // 8. 获取金额字段
        // ============================================================

        BigDecimal goodsMoney =
                getBigDecimal(
                        data,
                        "goodsMoney"
                );


        BigDecimal deliverMoney =
                getBigDecimal(
                        data,
                        "deliverMoney"
                );


        BigDecimal totalMoney =
                getBigDecimal(
                        data,
                        "totalMoney"
                );


        BigDecimal realTotalMoney =
                getBigDecimal(
                        data,
                        "realTotalMoney"
                );


        BigDecimal needPay =
                getBigDecimal(
                        data,
                        "needPay"
                );


        BigDecimal commissionFee =
                getBigDecimal(
                        data,
                        "commissionFee"
                );


        BigDecimal scoreMoney =
                getBigDecimal(
                        data,
                        "scoreMoney"
                );


        BigDecimal totalPayFee =
                getBigDecimal(
                        data,
                        "totalPayFee"
                );


        // ============================================================
        // 9. 金额不能为负数
        // ============================================================

        validateNonNegative(
                goodsMoney,
                "goodsMoney"
        );


        validateNonNegative(
                deliverMoney,
                "deliverMoney"
        );


        validateNonNegative(
                totalMoney,
                "totalMoney"
        );


        validateNonNegative(
                realTotalMoney,
                "realTotalMoney"
        );


        validateNonNegative(
                needPay,
                "needPay"
        );


        validateNonNegative(
                commissionFee,
                "commissionFee"
        );


        validateNonNegative(
                scoreMoney,
                "scoreMoney"
        );


        validateNonNegative(
                totalPayFee,
                "totalPayFee"
        );


        // ============================================================
        // 10. 保存金额字段
        // ============================================================

        order.setGoodsMoney(
                goodsMoney
        );


        order.setDeliverMoney(
                deliverMoney
        );


        order.setTotalMoney(
                totalMoney
        );


        order.setRealTotalMoney(
                realTotalMoney
        );


        order.setNeedPay(
                needPay
        );


        order.setCommissionFee(
                commissionFee
        );


        order.setScoreMoney(
                scoreMoney
        );


        order.setUseScore(
                getInteger(
                        data,
                        "useScore"
                )
        );


        order.setTotalPayFee(
                totalPayFee
        );


        // ============================================================
        // 11. 金额一致性检查
        // ============================================================

        /*
         * 基础关系：
         *
         * goodsMoney + deliverMoney
         *
         * 与：
         *
         * totalMoney
         *
         * 如果差值 > 0.01：
         *
         * 不删除订单。
         *
         * 添加：
         *
         * AMOUNT_MISMATCH
         *
         * 注意：
         *
         * 电商业务可能存在优惠、
         * 促销、积分等业务，
         * 所以这里只做质量警告，
         * 不作为硬错误。
         */
        if (goodsMoney != null
                && deliverMoney != null
                && totalMoney != null) {

            BigDecimal calculatedTotal =
                    goodsMoney.add(
                            deliverMoney
                    );


            BigDecimal difference =
                    calculatedTotal
                            .subtract(
                                    totalMoney
                            )
                            .abs();


            if (difference.compareTo(
                    AMOUNT_TOLERANCE
            ) > 0) {

                addQualityWarning(
                        order,
                        "AMOUNT_MISMATCH"
                );
            }
        }


        // ============================================================
        // 12. 支付状态 isPay
        // ============================================================

        Integer isPay =
                getInteger(
                        data,
                        "isPay"
                );


        /*
         * isPay：
         *
         * 0 = 未支付
         * 1 = 已支付
         */
        validateAllowedInteger(
                isPay,
                "isPay",
                0,
                1
        );


        order.setIsPay(
                isPay
        );


        // ============================================================
        // 13. payType
        // ============================================================

        order.setPayType(
                getInteger(
                        data,
                        "payType"
                )
        );


        // ============================================================
        // 14. 获取原始 payTime
        // ============================================================

        String rawPayTime =
                getString(
                        data,
                        "payTime"
                );


        /*
         * rawPayTime：
         *
         * 永远保存源系统原始值。
         *
         * 即使源数据不合理，
         * 仍然可以保留用于数据问题排查。
         */
        order.setRawPayTime(
                rawPayTime
        );


        // ============================================================
        // 15. 清洗 payTime
        // ============================================================

        /*
         * 支付状态以 isPay 为准。
         *
         * isPay = 1：
         * 保留 payTime。
         *
         * isPay = 0：
         * 清洗后的 payTime 设置 null。
         */
        if (Integer.valueOf(1).equals(
                isPay
        )) {

            order.setPayTime(
                    rawPayTime
            );

        } else {

            order.setPayTime(
                    null
            );
        }


        // ============================================================
        // 16. 支付状态与支付时间冲突
        // ============================================================

        /*
         * 实际ODS数据中存在：
         *
         * isPay = 0
         *
         * 但：
         *
         * payTime != null
         *
         * 我们不能根据 payTime 推断订单已经支付。
         *
         * 仍然以 isPay 为准。
         *
         * 这种情况：
         *
         * 原始时间保存到 rawPayTime
         * payTime 清空
         *
         * 同时增加：
         *
         * UNPAID_WITH_PAY_TIME
         */
        if (Integer.valueOf(0).equals(
                isPay
        )
                && rawPayTime != null
                && !rawPayTime.trim().isEmpty()) {

            addQualityWarning(
                    order,
                    "UNPAID_WITH_PAY_TIME"
            );
        }


        // ============================================================
        // 17. 其他业务字段
        // ============================================================

        order.setTradeNo(
                getString(
                        data,
                        "tradeNo"
                )
        );


        order.setDeliverType(
                getInteger(
                        data,
                        "deliverType"
                )
        );


        order.setAreaId(
                getLong(
                        data,
                        "areaId"
                )
        );


        order.setAreaIdPath(
                getString(
                        data,
                        "areaIdPath"
                )
        );


        order.setOrderSrc(
                getInteger(
                        data,
                        "orderSrc"
                )
        );


        order.setOrderType(
                getInteger(
                        data,
                        "orderType"
                )
        );


        order.setOrderCode(
                getString(
                        data,
                        "orderCode"
                )
        );


        order.setIsRefund(
                getInteger(
                        data,
                        "isRefund"
                )
        );


        order.setIsAppraise(
                getInteger(
                        data,
                        "isAppraise"
                )
        );


        order.setIsClosed(
                getInteger(
                        data,
                        "isClosed"
                )
        );


        // ============================================================
        // 18. dataFlag
        // ============================================================

        Integer dataFlag =
                getInteger(
                        data,
                        "dataFlag"
                );


        /*
         * dataFlag：
         *
         *  1 = 有效
         * -1 = 删除
         *
         * 注意：
         *
         * -1 是合法业务状态，
         * 不是脏数据。
         */
        validateAllowedInteger(
                dataFlag,
                "dataFlag",
                -1,
                1
        );


        order.setDataFlag(
                dataFlag
        );


        // ============================================================
        // 19. 获取时间字段
        // ============================================================

        String createTime =
                getString(
                        data,
                        "createTime"
                );


        String deliveryTime =
                getString(
                        data,
                        "deliveryTime"
                );


        String receiveTime =
                getString(
                        data,
                        "receiveTime"
                );


        // ============================================================
        // 20. createTime 解析
        // ============================================================

        /*
         * createTime 如果存在，
         * 必须严格符合：
         *
         * yyyy-MM-dd HH:mm:ss
         */
        Date createDate =
                parseDateTime(
                        createTime,
                        "createTime"
                );


        // ============================================================
        // 21. deliveryTime 解析
        // ============================================================

        Date deliveryDate =
                parseDateTime(
                        deliveryTime,
                        "deliveryTime"
                );


        // ============================================================
        // 22. receiveTime 解析
        // ============================================================

        Date receiveDate =
                parseDateTime(
                        receiveTime,
                        "receiveTime"
                );


        // ============================================================
        // 23. deliveryTime < createTime
        // ============================================================

        /*
         * 正常业务：
         *
         * createTime
         *     ↓
         * deliveryTime
         *
         * 如果：
         *
         * deliveryTime < createTime
         *
         * 不删除订单。
         *
         * 增加：
         *
         * DELIVERY_BEFORE_CREATE
         */
        if (createDate != null
                && deliveryDate != null
                && deliveryDate.before(
                createDate
        )) {

            addQualityWarning(
                    order,
                    "DELIVERY_BEFORE_CREATE"
            );
        }


        // ============================================================
        // 24. receiveTime < deliveryTime
        // ============================================================

        /*
         * 正常业务：
         *
         * deliveryTime
         *      ↓
         * receiveTime
         *
         * 如果：
         *
         * receiveTime < deliveryTime
         *
         * 数据仍然保留。
         *
         * 增加：
         *
         * RECEIVE_BEFORE_DELIVERY
         */
        if (deliveryDate != null
                && receiveDate != null
                && receiveDate.before(
                deliveryDate
        )) {

            addQualityWarning(
                    order,
                    "RECEIVE_BEFORE_DELIVERY"
            );
        }


        // ============================================================
        // 25. 保存时间字段
        // ============================================================

        order.setCreateTime(
                createTime
        );


        order.setDeliveryTime(
                deliveryTime
        );


        order.setReceiveTime(
                receiveTime
        );


        // ============================================================
        // 26. 保存ODS操作类型
        // ============================================================

        /*
         * 不修改源系统操作类型。
         *
         * 当前历史初始化数据是：
         *
         * bootstrap
         *
         * 后续CDC可能还有：
         *
         * insert
         * update
         * delete
         *
         * 均按原值保存。
         */
        order.setOpType(
                opType
        );


        // ============================================================
        // 27. DWD处理时间
        // ============================================================

        order.setProcessTs(
                System.currentTimeMillis()
        );


        // ============================================================
        // 28. 返回清洗后的DWD订单
        // ============================================================

        return order;
    }


    // ============================================================
    // 时间处理工具
    // ============================================================

    /**
     * 严格解析时间。
     *
     * 正确：
     *
     * 2019-07-14 18:00:00
     *
     * 错误：
     *
     * 2019/07/14 18:00:00
     * 2019-02-30 18:00:00
     * abc
     *
     * 如果 value 为 null：
     *
     * 返回 null。
     *
     * @param value     时间字符串
     * @param fieldName 字段名称
     * @return Date
     */
    private static Date parseDateTime(
            String value,
            String fieldName) {

        /*
         * deliveryTime、
         * receiveTime 等字段允许为空。
         */
        if (value == null) {

            return null;
        }


        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        DATE_TIME_PATTERN
                );


        /*
         * 禁止自动修正非法日期。
         *
         * 例如：
         *
         * 2019-02-30
         *
         * 不允许自动转换成三月份。
         */
        dateFormat.setLenient(
                false
        );


        try {

            Date date =
                    dateFormat.parse(
                            value
                    );


            /*
             * 再格式化一次进行完整比较。
             *
             * 避免类似：
             *
             * 2019-07-14 18:00:00abc
             *
             * 被部分解析成功。
             */
            String formatted =
                    dateFormat.format(
                            date
                    );


            if (!value.equals(
                    formatted
            )) {

                throw new IllegalArgumentException(
                        fieldName
                                + "格式非法，"
                                + fieldName
                                + "="
                                + value
                                + "，要求格式="
                                + DATE_TIME_PATTERN
                );
            }


            return date;


        } catch (ParseException e) {

            throw new IllegalArgumentException(
                    fieldName
                            + "格式非法，"
                            + fieldName
                            + "="
                            + value
                            + "，要求格式="
                            + DATE_TIME_PATTERN,
                    e
            );
        }
    }


    // ============================================================
    // 金额校验工具
    // ============================================================

    /**
     * 校验金额不能为负数。
     *
     * null 当前允许。
     *
     * 后续如果确定某个金额字段必须存在，
     * 再通过测试增加非空限制。
     */
    private static void validateNonNegative(
            BigDecimal value,
            String fieldName) {

        if (value == null) {

            return;
        }


        if (value.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalArgumentException(
                    fieldName
                            + "不能为负数，"
                            + fieldName
                            + "="
                            + value
            );
        }
    }


    // ============================================================
    // Integer枚举校验工具
    // ============================================================

    /**
     * 判断 Integer 字段是否属于允许值。
     *
     * 例如：
     *
     * isPay：
     *
     * 0、1
     *
     * orderStatus：
     *
     * -3、-2、-1、0、1、2
     *
     * dataFlag：
     *
     * -1、1
     *
     * 当前 null 不拦截。
     *
     * 因为目前测试只要求验证：
     *
     * “有值但值不合法”
     *
     * 后续需要必填校验时再单独增加测试。
     */
    private static void validateAllowedInteger(
            Integer value,
            String fieldName,
            int... allowedValues) {

        /*
         * 当前允许字段为空。
         */
        if (value == null) {

            return;
        }


        /*
         * 遍历允许值。
         */
        for (int allowedValue : allowedValues) {

            if (value == allowedValue) {

                return;
            }
        }


        /*
         * 没有匹配任何合法值。
         */
        throw new IllegalArgumentException(
                fieldName
                        + "非法，"
                        + fieldName
                        + "="
                        + value
        );
    }


    // ============================================================
    // 数据质量警告工具
    // ============================================================

    /**
     * 添加 qualityWarning。
     *
     * 一条订单允许同时存在多个警告。
     *
     * 例如：
     *
     * AMOUNT_MISMATCH
     *
     * +
     *
     * UNPAID_WITH_PAY_TIME
     *
     * 最终：
     *
     * AMOUNT_MISMATCH,UNPAID_WITH_PAY_TIME
     */
    private static void addQualityWarning(
            DwdOrderInfo order,
            String warning) {

        String oldWarning =
                order.getQualityWarning();


        /*
         * 第一个 warning。
         */
        if (oldWarning == null
                || oldWarning.trim().isEmpty()) {

            order.setQualityWarning(
                    warning
            );

            return;
        }


        /*
         * 已存在 warning，
         * 使用逗号继续追加。
         */
        order.setQualityWarning(
                oldWarning
                        + ","
                        + warning
        );
    }


    // ============================================================
    // String 工具
    // ============================================================

    /**
     * 获取 String。
     *
     * null：
     * 返回 null
     *
     * ""：
     * 返回 null
     *
     * "   "：
     * 返回 null
     */
    private static String getString(
            JSONObject data,
            String fieldName) {

        String value =
                data.getString(
                        fieldName
                );


        if (value == null) {

            return null;
        }


        value =
                value.trim();


        return value.isEmpty()
                ? null
                : value;
    }


    // ============================================================
    // Long 工具
    // ============================================================

    /**
     * 将字段转换为 Long。
     */
    private static Long getLong(
            JSONObject data,
            String fieldName) {

        String value =
                getString(
                        data,
                        fieldName
                );


        if (value == null) {

            return null;
        }


        return Long.valueOf(
                value
        );
    }


    // ============================================================
    // Integer 工具
    // ============================================================

    /**
     * 将字段转换为 Integer。
     */
    private static Integer getInteger(
            JSONObject data,
            String fieldName) {

        String value =
                getString(
                        data,
                        fieldName
                );


        if (value == null) {

            return null;
        }


        return Integer.valueOf(
                value
        );
    }


    // ============================================================
    // BigDecimal 工具
    // ============================================================

    /**
     * 将金额字符串转换为 BigDecimal。
     *
     * 金额字段不能使用 double。
     *
     * 例如：
     *
     * "100.01"
     *
     * ->
     *
     * BigDecimal("100.01")
     */
    private static BigDecimal getBigDecimal(
            JSONObject data,
            String fieldName) {

        String value =
                getString(
                        data,
                        fieldName
                );


        if (value == null) {

            return null;
        }


        return new BigDecimal(
                value
        );
    }
}