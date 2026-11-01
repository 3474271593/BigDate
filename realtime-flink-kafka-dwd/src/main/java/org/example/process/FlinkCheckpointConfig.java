package org.example.process;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

//flink重跑机制
public class FlinkCheckpointConfig {

    private static final long CHECKPOINT_INTERVAL = 30_000L;
    private static final long CHECKPOINT_TIMEOUT = 60_000L;
    private static final int MAX_CONCURRENT_CHECKPOINTS = 1;
    private static final int TOLERABLE_CHECKPOINT_FAILURE_NUMBER = 1;

    private FlinkCheckpointConfig() {
    }

    public static void configure(StreamExecutionEnvironment env) {
        // 每30秒触发一次Checkpoint
        env.enableCheckpointing(CHECKPOINT_INTERVAL, CheckpointingMode.EXACTLY_ONCE);

        CheckpointConfig checkpointConfig = env.getCheckpointConfig();

        // 单次Checkpoint超过60秒则判定失败
        checkpointConfig.setCheckpointTimeout(CHECKPOINT_TIMEOUT);

        // 同一时间最多只允许一个Checkpoint执行
        checkpointConfig.setMaxConcurrentCheckpoints(MAX_CONCURRENT_CHECKPOINTS);

        // 允许连续出现1次Checkpoint失败，不立即让整个Flink任务失败
        checkpointConfig.setTolerableCheckpointFailureNumber(TOLERABLE_CHECKPOINT_FAILURE_NUMBER);
    }
}