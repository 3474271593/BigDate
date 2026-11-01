package org.example.process;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.example.bean.DwdOrderInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 * orders -> DWD 清洗逻辑测试。
 *
 * 当前测试规则：
 *
 * 1. 未支付订单存在 payTime：
 *    保留 rawPayTime，
 *    清空 payTime，
 *    增加 UNPAID_WITH_PAY_TIME 警告。
 *
 * 2. orderId <= 0：
 *    属于非法主键，
 *    不允许进入正常 DWD。
 *
 * 3. goodsMoney < 0：
 *    属于非法金额，
 *    不允许进入正常 DWD。
 *
 * 4. goodsMoney + deliverMoney 与 totalMoney 差值超过 0.01：
 *    数据仍保留，
 *    但增加 AMOUNT_MISMATCH 质量警告。
 */
public class OrderProcessFunctionTest {

    /**
     * 测试 1：
     *
     * 未支付订单即使源系统存在 payTime，
     * DWD 也不能将其认定为真实支付时间。
     */
    @Test
    public void shouldClearPayTimeWhenOrderIsUnpaid() {

        // =========================
        // 1. 构造测试 ODS 数据
        // =========================

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"522\","
                        + "\"orderNo\":\"201907140001\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"-2\","
                        + "\"goodsMoney\":\"55.00\","
                        + "\"deliverMoney\":\"0.00\","
                        + "\"totalMoney\":\"55.00\","
                        + "\"realTotalMoney\":\"55.00\","
                        + "\"payType\":\"0\","
                        + "\"isPay\":\"0\","
                        + "\"payTime\":\"2019-07-14 18:08:47\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";

        JSONObject odsJson =
                JSON.parseObject(json);


        // =========================
        // 2. 执行订单清洗
        // =========================

        DwdOrderInfo result =
                OrderProcessFunction.cleanOrder(odsJson);


        // =========================
        // 3. 验证支付状态
        // =========================

        Assert.assertEquals(
                Integer.valueOf(0),
                result.getIsPay()
        );


        // =========================
        // 4. 原始支付时间必须保留
        // =========================

        Assert.assertEquals(
                "2019-07-14 18:08:47",
                result.getRawPayTime()
        );


        // =========================
        // 5. 清洗后的支付时间必须为空
        // =========================

        Assert.assertNull(
                result.getPayTime()
        );


        // =========================
        // 6. 必须产生质量警告
        // =========================

        Assert.assertEquals(
                "UNPAID_WITH_PAY_TIME",
                result.getQualityWarning()
        );


        // =========================
        // 7. ODS 操作类型必须保留
        // =========================

        Assert.assertEquals(
                "bootstrap",
                result.getOpType()
        );
    }


    /**
     * 测试 2：
     *
     * orderId = 0 属于非法订单主键。
     *
     * 当前阶段应该抛出 IllegalArgumentException。
     *
     * 后续接入 dirty data 后，
     * 会改成发送到 dwd_dirty_data。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectOrderWhenOrderIdIsZero() {

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"0\","
                        + "\"orderNo\":\"TEST001\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"0\","
                        + "\"goodsMoney\":\"100.00\","
                        + "\"deliverMoney\":\"10.00\","
                        + "\"totalMoney\":\"110.00\","
                        + "\"realTotalMoney\":\"110.00\","
                        + "\"isPay\":\"0\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";

        JSONObject odsJson =
                JSON.parseObject(json);

        OrderProcessFunction.cleanOrder(
                odsJson
        );
    }


    /**
     * 测试 3：
     *
     * 商品金额不能为负数。
     *
     * goodsMoney = -100.00
     *
     * 属于明显非法数据，
     * 当前应该抛出 IllegalArgumentException。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectOrderWhenGoodsMoneyIsNegative() {

        // =========================
        // 1. 构造负金额订单
        // =========================

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"1001\","
                        + "\"orderNo\":\"TEST_NEGATIVE_AMOUNT\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"0\","
                        + "\"goodsMoney\":\"-100.00\","
                        + "\"deliverMoney\":\"10.00\","
                        + "\"totalMoney\":\"-90.00\","
                        + "\"realTotalMoney\":\"-90.00\","
                        + "\"isPay\":\"0\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";


        // =========================
        // 2. 转换为 JSONObject
        // =========================

        JSONObject odsJson =
                JSON.parseObject(json);


        // =========================
        // 3. 执行清洗
        //
        // 当前测试要求这里必须抛异常。
        // =========================

        OrderProcessFunction.cleanOrder(
                odsJson
        );
    }


    /**
     * 测试 4：
     *
     * 商品金额 + 运费
     * 与订单总金额存在明显差异时，
     *
     * 不直接删除订单，
     * 而是保留订单并增加质量警告。
     *
     * 测试数据：
     *
     * goodsMoney   = 100.00
     * deliverMoney = 10.00
     *
     * 正常应该：
     * 100.00 + 10.00 = 110.00
     *
     * 但源数据：
     * totalMoney = 120.00
     *
     * 差值 = 10.00 > 0.01
     */
    @Test
    public void shouldAddWarningWhenOrderAmountMismatch() {

        // =========================
        // 1. 构造金额不一致订单
        // =========================

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"1002\","
                        + "\"orderNo\":\"TEST_AMOUNT_MISMATCH\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"0\","
                        + "\"goodsMoney\":\"100.00\","
                        + "\"deliverMoney\":\"10.00\","
                        + "\"totalMoney\":\"120.00\","
                        + "\"realTotalMoney\":\"120.00\","
                        + "\"payType\":\"0\","
                        + "\"isPay\":\"0\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";


        // =========================
        // 2. 转换为 JSONObject
        // =========================

        JSONObject odsJson =
                JSON.parseObject(json);


        // =========================
        // 3. 执行 DWD 清洗
        // =========================

        DwdOrderInfo result =
                OrderProcessFunction.cleanOrder(
                        odsJson
                );


        // =========================
        // 4. 数据应该继续保留
        // =========================

        Assert.assertEquals(
                Long.valueOf(1002L),
                result.getOrderId()
        );


        // =========================
        // 5. 应该增加金额异常警告
        // =========================

        Assert.assertEquals(
                "AMOUNT_MISMATCH",
                result.getQualityWarning()
        );
    }

    /**
     * 测试 5：
     *
     * createTime 是订单核心时间字段。
     * 如果时间格式无法按照 yyyy-MM-dd HH:mm:ss 解析，
     * 当前阶段应该判定为非法数据。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectOrderWhenCreateTimeFormatIsInvalid() {

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"1003\","
                        + "\"orderNo\":\"TEST_INVALID_TIME\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"0\","
                        + "\"goodsMoney\":\"100.00\","
                        + "\"deliverMoney\":\"10.00\","
                        + "\"totalMoney\":\"110.00\","
                        + "\"realTotalMoney\":\"110.00\","
                        + "\"isPay\":\"0\","
                        + "\"createTime\":\"2019/07/14 18:00:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";

        JSONObject odsJson =
                JSON.parseObject(json);

        OrderProcessFunction.cleanOrder(
                odsJson
        );
    }

    /**
     * 测试 6：
     *
     * 如果 deliveryTime 早于 createTime，
     * 说明订单时间关系异常。
     *
     * 这种记录仍然保留，
     * 但必须增加：
     *
     * DELIVERY_BEFORE_CREATE
     */
    @Test
    public void shouldAddWarningWhenDeliveryTimeBeforeCreateTime() {

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"1004\","
                        + "\"orderNo\":\"TEST_TIME_ORDER\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"1\","
                        + "\"goodsMoney\":\"100.00\","
                        + "\"deliverMoney\":\"10.00\","
                        + "\"totalMoney\":\"110.00\","
                        + "\"realTotalMoney\":\"110.00\","
                        + "\"isPay\":\"1\","
                        + "\"payTime\":\"2019-07-14 18:05:00\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"deliveryTime\":\"2019-07-14 17:00:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";

        JSONObject odsJson =
                JSON.parseObject(json);

        DwdOrderInfo result =
                OrderProcessFunction.cleanOrder(
                        odsJson
                );

        /*
         * 订单本身仍然保留。
         */
        Assert.assertEquals(
                Long.valueOf(1004L),
                result.getOrderId()
        );

        /*
         * 但必须标记时间顺序异常。
         */
        Assert.assertEquals(
                "DELIVERY_BEFORE_CREATE",
                result.getQualityWarning()
        );
    }
    /**
     * 测试 7：
     *
     * receiveTime 早于 deliveryTime。
     *
     * 正常业务顺序应该是：
     *
     * createTime
     *      ↓
     * deliveryTime
     *      ↓
     * receiveTime
     *
     * 如果收货时间早于发货时间，
     * 说明业务时间顺序异常。
     *
     * 这种数据仍然保留，
     * 但必须增加：
     *
     * RECEIVE_BEFORE_DELIVERY
     */
    @Test
    public void shouldAddWarningWhenReceiveTimeBeforeDeliveryTime() {

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"1005\","
                        + "\"orderNo\":\"TEST_RECEIVE_BEFORE_DELIVERY\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"2\","
                        + "\"goodsMoney\":\"100.00\","
                        + "\"deliverMoney\":\"10.00\","
                        + "\"totalMoney\":\"110.00\","
                        + "\"realTotalMoney\":\"110.00\","
                        + "\"isPay\":\"1\","
                        + "\"payTime\":\"2019-07-14 18:05:00\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"deliveryTime\":\"2019-07-14 19:00:00\","
                        + "\"receiveTime\":\"2019-07-14 18:30:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";

        JSONObject odsJson =
                JSON.parseObject(json);

        DwdOrderInfo result =
                OrderProcessFunction.cleanOrder(
                        odsJson
                );

        // 数据本身仍然保留。
        Assert.assertEquals(
                Long.valueOf(1005L),
                result.getOrderId()
        );

        // 必须产生收货时间异常警告。
        Assert.assertEquals(
                "RECEIVE_BEFORE_DELIVERY",
                result.getQualityWarning()
        );
    }


    /**
     * 测试 8：
     *
     * 金额差值刚好为 0.01 时，
     * 应该认为在允许误差范围内。
     *
     * 实际业务数据中已经出现过类似情况：
     *
     * goodsMoney   = 505.19
     * deliverMoney = 10.10
     *
     * 相加：
     * 515.29
     *
     * totalMoney：
     * 515.30
     *
     * 差值：
     * 0.01
     *
     * 因为当前容差就是 0.01，
     * 所以不应该标记 AMOUNT_MISMATCH。
     */
    @Test
    public void shouldNotAddAmountWarningWhenDifferenceIsExactlyOneCent() {

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"1006\","
                        + "\"orderNo\":\"TEST_AMOUNT_TOLERANCE\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"0\","
                        + "\"goodsMoney\":\"505.19\","
                        + "\"deliverMoney\":\"10.10\","
                        + "\"totalMoney\":\"515.30\","
                        + "\"realTotalMoney\":\"515.30\","
                        + "\"isPay\":\"0\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";

        JSONObject odsJson =
                JSON.parseObject(json);

        DwdOrderInfo result =
                OrderProcessFunction.cleanOrder(
                        odsJson
                );

        // 金额差值刚好 0.01，不应该产生金额异常警告。
        Assert.assertNull(
                result.getQualityWarning()
        );
    }

    /**
     * 测试 9：
     *
     * orderStatus 只允许：
     *
     * -3 用户拒收
     * -2 未付款
     * -1 用户取消
     *  0 待发货
     *  1 配送中
     *  2 用户确认收货
     *
     * 如果出现 99 这种未知状态，
     * 说明源数据不符合订单状态定义，
     * 不应该作为正常 DWD 数据继续处理。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectOrderWhenOrderStatusIsInvalid() {

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"1007\","
                        + "\"orderNo\":\"TEST_INVALID_ORDER_STATUS\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"99\","
                        + "\"goodsMoney\":\"100.00\","
                        + "\"deliverMoney\":\"10.00\","
                        + "\"totalMoney\":\"110.00\","
                        + "\"realTotalMoney\":\"110.00\","
                        + "\"isPay\":\"0\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";

        JSONObject odsJson =
                JSON.parseObject(json);

        OrderProcessFunction.cleanOrder(
                odsJson
        );
    }

    /**
     * 测试 10：
     *
     * isPay 只允许：
     *
     * 0 = 未支付
     * 1 = 已支付
     *
     * 如果出现 isPay = 2，
     * 属于支付状态字段非法。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectOrderWhenIsPayIsInvalid() {

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"1008\","
                        + "\"orderNo\":\"TEST_INVALID_IS_PAY\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"0\","
                        + "\"goodsMoney\":\"100.00\","
                        + "\"deliverMoney\":\"10.00\","
                        + "\"totalMoney\":\"110.00\","
                        + "\"realTotalMoney\":\"110.00\","
                        + "\"isPay\":\"2\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"dataFlag\":\"1\""
                        + "}"
                        + "}";

        JSONObject odsJson =
                JSON.parseObject(json);

        OrderProcessFunction.cleanOrder(
                odsJson
        );
    }
    /**
     * 测试 11：
     *
     * dataFlag 只允许：
     *
     *  1 = 有效
     * -1 = 删除
     *
     * 注意：
     * -1 是合法业务状态，
     * 不是脏数据。
     *
     * 如果出现 0，
     * 才属于未知状态。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectOrderWhenDataFlagIsInvalid() {

        String json =
                "{"
                        + "\"table\":\"orders\","
                        + "\"type\":\"bootstrap\","
                        + "\"data\":{"
                        + "\"orderId\":\"1009\","
                        + "\"orderNo\":\"TEST_INVALID_DATA_FLAG\","
                        + "\"shopId\":\"1\","
                        + "\"userId\":\"10\","
                        + "\"orderStatus\":\"0\","
                        + "\"goodsMoney\":\"100.00\","
                        + "\"deliverMoney\":\"10.00\","
                        + "\"totalMoney\":\"110.00\","
                        + "\"realTotalMoney\":\"110.00\","
                        + "\"isPay\":\"0\","
                        + "\"createTime\":\"2019-07-14 18:00:00\","
                        + "\"dataFlag\":\"0\""
                        + "}"
                        + "}";

        JSONObject odsJson =
                JSON.parseObject(json);

        OrderProcessFunction.cleanOrder(
                odsJson
        );
    }
}