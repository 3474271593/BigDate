package org.example.process;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.example.bean.DwdOrderInfo;
import org.example.bean.UserDelta;
import org.example.bean.UserSourceRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User 实时主题历史状态预热处理函数。
 *
 * 作用：
 *
 * 1. Job 第一次启动时从 dwd_order_info earliest 开始读取；
 * 2. Bootstrap 历史边界之前的数据只用于恢复订单 ValueState；
 * 3. 历史数据不产生 UserDelta，避免重复计算已经写入
 *    dws_user_summary 的历史基线；
 * 4. 到达 Bootstrap 边界后才正式产生实时 UserDelta。
 *
 * 当前 User 历史边界：
 *
 * partition 0 : [0, 20030)
 * partition 1 : [0, 0)
 * partition 2 : [0, 0)
 *
 * 即：
 *
 * P0 offset < 20030
 *      -> 只恢复 previous-order-state
 *
 * P0 offset >= 20030
 *      -> 实时计算
 *
 * P1/P2 offset >= 0
 *      -> 实时计算
 */
public class UserWarmupProcessFunction
        extends KeyedProcessFunction<Long, UserSourceRecord, UserDelta> {

    /**
     * 每个 Kafka Partition 对应的实时起始 Offset。
     *
     * 当前：
     *
     * P0 -> 20030
     * P1 -> 0
     * P2 -> 0
     */
    private final Map<Integer, Long> realtimeStartOffsets;

    /**
     * 每个 orderId 对应的上一版本订单状态。
     *
     * 上游必须：
     *
     * keyBy(orderId)
     *
     * 因此每个 orderId 都维护自己的 ValueState。
     */
    private transient ValueState<DwdOrderInfo> previousOrderState;

    public UserWarmupProcessFunction(
            Map<Integer, Long> realtimeStartOffsets) {

        if (realtimeStartOffsets == null) {

            throw new IllegalArgumentException(
                    "realtimeStartOffsets must not be null"
            );
        }

        /*
         * 创建防御性副本并设为只读，
         * 防止运行期间 Bootstrap 边界被意外修改。
         */
        this.realtimeStartOffsets =
                Collections.unmodifiableMap(
                        new HashMap<>(
                                realtimeStartOffsets
                        )
                );
    }

    @Override
    public void open(
            Configuration parameters) {

        /*
         * 与 Shop 使用相同的状态名称。
         *
         * 这个状态属于 User Job 自己的算子，
         * 用于保存每个 orderId 的上一版本订单。
         */
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
            UserSourceRecord record,
            Context context,
            Collector<UserDelta> out)
            throws Exception {

        /*
         * 防御性校验。
         *
         * Warmup 状态是按照 orderId 保存的，
         * 因此没有 orderId 的数据无法进入状态。
         */
        if (record == null
                || record.getOrder() == null
                || record.getOrder().getOrderId() == null) {

            return;
        }

        DwdOrderInfo current =
                record.getOrder();

        /*
         * 根据消息所在 Kafka Partition，
         * 获取该 Partition 的实时起始 Offset。
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
         * 这些订单已经由 UserBootstrapApp 聚合后写入：
         *
         * dws_user_summary
         *
         * 所以这里只恢复订单最终状态，
         * 绝对不能再次产生 UserDelta。
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
         * 获取当前 orderId 上一个版本的状态。
         */
        DwdOrderInfo previous =
                previousOrderState.value();

        /*
         * previous + current
         *
         *      ↓
         *
         * UserDeltaCalculator
         *
         *      ↓
         *
         * USER_ORDER / USER_PAY
         */
        List<UserDelta> deltas =
                UserDeltaCalculator.calculate(
                        previous,
                        current
                );

        if (deltas != null) {

            for (UserDelta delta : deltas) {

                if (delta != null) {

                    out.collect(
                            delta
                    );
                }
            }
        }

        /*
         * 无论本次是否产生 UserDelta，
         * 当前订单状态都必须成为下一次 CDC
         * 到来时的 previous。
         */
        previousOrderState.update(
                current
        );
    }
}