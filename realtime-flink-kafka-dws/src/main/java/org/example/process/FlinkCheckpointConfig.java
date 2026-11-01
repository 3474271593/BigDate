// 文件路径：src/main/java/org/example/process/FlinkCheckpointConfig.java
package org.example.process;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.concurrent.TimeUnit;

/**
 * DWS Flink Checkpoint统一配置。
 *
 * 负责：
 *
 * 1. 定期保存Kafka Source消费位置；
 * 2. 保存Keyed State；
 * 3. 保存Window State / Timer；
 * 4. Job故障后提供状态恢复基础；
 * 5. 支持不同DWS Job使用独立Checkpoint目录。
 */
public final class FlinkCheckpointConfig {

    /**
     * Checkpoint间隔：30秒。
     */
    private static final long CHECKPOINT_INTERVAL =
            30_000L;

    /**
     * Checkpoint超时时间：60秒。
     */
    private static final long CHECKPOINT_TIMEOUT =
            60_000L;

    /**
     * 两次Checkpoint之间至少间隔10秒。
     */
    private static final long MIN_PAUSE_BETWEEN_CHECKPOINTS =
            10_000L;

    /**
     * Trade Job默认Checkpoint目录。
     *
     * 这个路径必须继续保留，
     * 因为现有App.java调用：
     *
     * FlinkCheckpointConfig.configure(env);
     *
     * 已经用于Trade Job。
     */
    private static final String DEFAULT_CHECKPOINT_PATH =
            "hdfs:///flink/checkpoints/dws-trade";

    private FlinkCheckpointConfig() {
    }

    public static String resolveCheckpointPath(
            String defaultPath) {

        String configuredPath =
                System.getProperty("flink.checkpoint.path");

        if (configuredPath == null
                || configuredPath.trim().isEmpty()) {

            return defaultPath;
        }

        return configuredPath.trim();
    }

    /**
     * 默认配置。
     *
     * 当前专门用于原有Trade Job。
     *
     * 原来的App.java不需要做任何修改。
     *
     * @param env Flink执行环境
     */
    public static void configure(
            StreamExecutionEnvironment env) {

        configure(
                env,
                DEFAULT_CHECKPOINT_PATH
        );
    }

    /**
     * 指定Checkpoint目录进行配置。
     *
     * Goods、Shop等独立Job可以使用自己的目录。
     *
     * 例如：
     *
     * hdfs:///flink/checkpoints/dws-goods
     *
     * @param env            Flink执行环境
     * @param checkpointPath Checkpoint存储路径
     */
    public static void configure(
            StreamExecutionEnvironment env,
            String checkpointPath) {

        if (env == null) {
            throw new IllegalArgumentException(
                    "StreamExecutionEnvironment不能为空"
            );
        }

        if (checkpointPath == null
                || checkpointPath.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Checkpoint路径不能为空"
            );
        }

        // ============================================================
        // 1. 开启Checkpoint
        // ============================================================

        env.enableCheckpointing(
                CHECKPOINT_INTERVAL,
                CheckpointingMode.EXACTLY_ONCE
        );

        CheckpointConfig checkpointConfig =
                env.getCheckpointConfig();

        // ============================================================
        // 2. Checkpoint超时
        // ============================================================

        checkpointConfig.setCheckpointTimeout(
                CHECKPOINT_TIMEOUT
        );

        // ============================================================
        // 3. 两次Checkpoint最小间隔
        // ============================================================

        checkpointConfig.setMinPauseBetweenCheckpoints(
                MIN_PAUSE_BETWEEN_CHECKPOINTS
        );

        // ============================================================
        // 4. 同时只允许一个Checkpoint
        // ============================================================

        checkpointConfig.setMaxConcurrentCheckpoints(
                1
        );

        // ============================================================
        // 5. Checkpoint失败时不直接终止Job
        // ============================================================

        checkpointConfig.setFailOnCheckpointingErrors(
                false
        );

        // ============================================================
        // 6. Cancel Job后保留Checkpoint
        // ============================================================

        /*
         * RETAIN_ON_CANCELLATION：
         *
         * 手动Cancel Job以后，
         * 已经完成的Checkpoint不会立即删除。
         *
         * 后续可以使用：
         *
         * flink run -s <checkpoint>
         *
         * 恢复Job。
         */
        checkpointConfig.enableExternalizedCheckpoints(
                CheckpointConfig
                        .ExternalizedCheckpointCleanup
                        .RETAIN_ON_CANCELLATION
        );

        // ============================================================
        // 7. State Backend + Checkpoint目录
        // ============================================================

        /*
         * 当前Windows开发环境使用FsStateBackend。
         *
         * Trade：
         *
         * hdfs:///flink/checkpoints/dws-trade
         *
         * Goods：
         *
         * hdfs:///flink/checkpoints/dws-goods
         *
         * Linux/Hadoop集群统一使用HDFS保存Checkpoint。
         */
        env.setStateBackend(
                new FsStateBackend(
                        resolveCheckpointPath(
                                checkpointPath
                        )
                )
        );

        // ============================================================
        // 8. Job故障自动重启
        // ============================================================

        /*
         * 最多重启3次，
         * 每次间隔10秒。
         */
        env.setRestartStrategy(
                RestartStrategies.fixedDelayRestart(
                        3,
                        org.apache.flink.api.common.time.Time.of(
                                10,
                                TimeUnit.SECONDS
                        )
                )
        );
    }
}
