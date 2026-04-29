package net.vulkadroid.android;

import net.vulkadroid.Initializer;
import net.vulkadroid.config.Config;

public class Adreno650Optimizer {

    /**
     * Full Adreno 650 (Snapdragon 870) optimizations.
     * Tile-Based Deferred Rendering (TBDR) aware settings.
     */
    public static void apply() {
        // Maximize chunk threads — Snapdragon 870 has 8 cores (1x3.2 + 3x2.42 + 4x1.8)
        // Use 4 threads for chunk building — leave cores for game logic
        System.setProperty("vulkadroid.chunkThreads", "4");

        // Enable indirect draw calls — Adreno 650 has good support
        System.setProperty("vulkadroid.indirectDraw", "true");

        // TBDR: prefer LOAD_OP_DONT_CARE / STORE_OP_DONT_CARE wherever possible
        // Already handled in RenderPass.java via VK_ATTACHMENT_STORE_OP_DONT_CARE for depth
        System.setProperty("vulkadroid.tbdrOptimize", "true");

        // Adreno 650 benefits from larger command buffers — batch more draw calls
        System.setProperty("vulkadroid.commandBatchSize", "2048");

        // Use FIFO-free mailbox present mode for lowest latency
        System.setProperty("vulkadroid.preferMailbox", "true");

        // VK_IMAGE_TILING_OPTIMAL is always preferred on Adreno (reduces bandwidth)
        System.setProperty("vulkadroid.alwaysOptimalTiling", "true");

        // Disable MSAA — bandwidth expensive on mobile TBDR
        System.setProperty("vulkadroid.msaa", "false");

        // Enable pipeline cache — reduces shader compilation stutter
        System.setProperty("vulkadroid.pipelineCache", "true");

        Initializer.LOGGER.info("Adreno 650 (TBDR) optimizations: indirectDraw=ON, mailbox=ON, MSAA=OFF, batchSize=2048");
    }

    /**
     * Generic Android optimizations for non-Adreno-650 devices.
     */
    public static void applyGeneric() {
        System.setProperty("vulkadroid.chunkThreads", "2");
        System.setProperty("vulkadroid.indirectDraw", "false");
        System.setProperty("vulkadroid.commandBatchSize", "512");
        System.setProperty("vulkadroid.msaa", "false");
        Initializer.LOGGER.info("Generic Android optimizations applied");
    }

    public static boolean isTbdrOptimized() {
        return Boolean.getBoolean("vulkadroid.tbdrOptimize");
    }

    public static int getCommandBatchSize() {
        return Integer.getInteger("vulkadroid.commandBatchSize", 512);
    }
}
