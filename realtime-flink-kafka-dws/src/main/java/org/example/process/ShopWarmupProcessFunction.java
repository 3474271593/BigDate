package org.example.process;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.example.bean.DwdOrderInfo;
import org.example.bean.ShopDelta;
import org.example.bean.ShopSourceRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shop 实时主题历史状态预热处理函数。
 *
 * 作用：
 *
 * 1. Job 第一次启动时从 dwd_order_info earliest 开始读取；
 * 2. Bootstrap 历史边界之前的数据只用于恢复订单 ValueState；
 * 3. 历史数据不产生 ShopDelta，避免重复计算已经写入
 *    dws_shop_summary 的历史基线；
 * 4. 到达 Bootstrap 边界后才正式产生实时 ShopDelta。
 *
 * 当前历史边界：
 *
 * partition 0 : [0, 20028)
 * partition 1 : [0, 0)
 * partition 2 : [0, 0)
 *
 * 即：
 *
 * P0 offset < 20028
 *      -> 只恢复 previous-order-state
 *
 * P0 offset >= 20028
 *      -> 实时计算
 *
 * P1/P2 offset >= 0
 *      -> 实时计算
 */
public class ShopWarmupProcessFunction
        extends KeyedProcessFunction<Long, ShopSourceRecord, ShopDelta> {

    /**
     * 每个 Kafka Partition 对应的实时起始 Offset。
     *
     * 例如：
     *
     * P0 -> 20028
     * P1 -> 0
     * P2 -> 0
     */
    private final Map<Integer, Long> realtimeStartOffsets;

    /**
     * 每个 orderId 对应的上一版本订单状态。
     *
     * 因为上游会：
     *
     * keyBy(orderId)
     *
     * 所以每个 Key 都维护自己的订单状态。
     */
    private transient ValueState<DwdOrderInfo> previousOrderState;

    public ShopWarmupProcessFunction(
            Map<Integer, Long> realtimeStartOffsets) {

        if (realtimeStartOffsets == null) {
            throw new IllegalArgumentException(
                    "realtimeStartOffsets must not be null"
            );
        }

        /*
         * 创建副本并设为只读，
         * 防止运行期间边界被意外修改。
         */
        this.realtimeStartOffsets =
                Collections.unmodifiableMap(
                        new HashMap<>(
                                realtimeStartOffsets
                        )
                );
    }

    @Override
    public void open(Configuration parameters) {

        ValueStateDescriptor<DwdOrderInfo> descriptor =
                new ValueStateDescriptor<>(
                        "previous-order-state",
                        DwdOrderInfo.class
                );

        previousOrderState =
                getRuntimeContext().getState(
                        descriptor
                );
    }

    @Override
    public void processElement(
            ShopSourceRecord record,
            Context context,
            Collector<ShopDelta> out)
            throws Exception {

        /*
         * 防御性校验。
         */
        if (record == null
                || record.getOrder() == null
                || record.getOrder().getOrderId() == null) {

            return;
        }

        DwdOrderInfo current =
                record.getOrder();

        /*
         * 根据当前消息所在 Kafka Partition
         * 找到该 Partition 的实时起始 Offset。
         */
        Long realtimeStart =
                realtimeStartOffsets.get(
                        record.getPartition()
                );

        if (realtimeStart == null) {

            throw new IllegalStateException(
                    "Missing realtime start offset for partition "
                            + record.getPartition()
            );
        }

        /*
         * ============================================================
         * 历史 Bootstrap 范围
         * ============================================================
         *
         * 这些订单已经被 ShopBootstrapApp 聚合后写入：
         *
         * dws_shop_summary
         *
         * 因此这里只恢复订单状态，
         * 绝对不能再次产生 ShopDelta。
         */
        if (record.getOffset() < realtimeStart) {

            previousOrderState.update(
                    current
            );

            return;
        }

        /*
         * ============================================================
         * 实时范围
         * ============================================================
         *
         * 获取当前 orderId 的上一版本状态。
         */
        DwdOrderInfo previous =
                previousOrderState.value();

        /*
         * previous + current
         *
         *      ↓
         *
         * ShopDeltaCalculator
         *
         *      ↓
         *
         * SHOP_ORDER / SHOP_PAY
         */
        List<ShopDelta> deltas =
                ShopDeltaCalculator.calculate(
                        previous,
                        current
                );

        if (deltas != null) {

            for (ShopDelta delta : deltas) {

                if (delta != null) {
                    out.collect(delta);
                }
            }
        }

        /*
         * 无论本次是否产生 Delta，
         * 当前订单状态都必须成为下一次 CDC
         * 到来时的 previous。
         */
        previousOrderState.update(
                current
        );
    }
}