package org.example.process;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.junit.Assert;
import org.junit.Test;

public class FlinkCheckpointConfigTest {

    @Test
    public void shouldEnableCheckpointWithExpectedSettings() {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        FlinkCheckpointConfig.configure(env);

        Assert.assertEquals(30000L, env.getCheckpointConfig().getCheckpointInterval());
        Assert.assertEquals(CheckpointingMode.EXACTLY_ONCE, env.getCheckpointConfig().getCheckpointingMode());
        Assert.assertEquals(60000L, env.getCheckpointConfig().getCheckpointTimeout());
        Assert.assertEquals(1, env.getCheckpointConfig().getMaxConcurrentCheckpoints());
        Assert.assertEquals(1, env.getCheckpointConfig().getTolerableCheckpointFailureNumber());
    }
}