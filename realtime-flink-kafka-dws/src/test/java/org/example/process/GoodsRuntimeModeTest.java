package org.example.process;

import org.junit.Assert;
import org.junit.Test;

public class GoodsRuntimeModeTest {
    @Test
    public void parsesWarmupAndRealtimeIgnoringCase() {
        Assert.assertEquals(GoodsRuntimeMode.WARMUP, GoodsRuntimeMode.parse("warmup"));
        Assert.assertEquals(GoodsRuntimeMode.REALTIME, GoodsRuntimeMode.parse("REALTIME"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingMode() {
        GoodsRuntimeMode.parse(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownMode() {
        GoodsRuntimeMode.parse("latest");
    }

    @Test
    public void realtimeRequiresRestorePath() {
        Assert.assertEquals(
                "file:///F:/flink-checkpoints/goods/job/chk-1",
                GoodsRuntimeMode.REALTIME.validateRestorePath(
                        " file:///F:/flink-checkpoints/goods/job/chk-1 "
                )
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void realtimeRejectsMissingRestorePath() {
        GoodsRuntimeMode.REALTIME.validateRestorePath(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void warmupRejectsRestorePath() {
        GoodsRuntimeMode.WARMUP.validateRestorePath(
                "file:///F:/flink-checkpoints/goods/job/chk-1"
        );
    }

    @Test
    public void warmupAllowsNoRestorePath() {
        Assert.assertNull(GoodsRuntimeMode.WARMUP.validateRestorePath(null));
    }
}
