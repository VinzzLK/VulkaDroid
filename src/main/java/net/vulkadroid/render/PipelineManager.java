package net.vulkadroid.render;

import net.vulkadroid.Initializer;
import net.vulkadroid.vulkan.Renderer;
import net.vulkadroid.vulkan.shader.GraphicsPipeline;
import net.vulkadroid.vulkan.shader.PipelineState;
import net.vulkadroid.vulkan.framebuffer.SwapChain;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

public class PipelineManager {

    private static final Map<String, GraphicsPipeline> pipelines = new HashMap<>();
    private static String currentPipeline = null;

    public static void initialize() {
        registerBuiltinPipelines();
    }

    private static void registerBuiltinPipelines() {
        // Terrain solid
        PipelineState solidState = new PipelineState();
        solidState.depthTestEnabled  = true;
        solidState.depthWriteEnabled = true;
        solidState.cullEnabled       = true;
        solidState.blendEnabled      = false;

        // Terrain translucent
        PipelineState translucentState = new PipelineState();
        translucentState.depthTestEnabled  = true;
        translucentState.depthWriteEnabled = false;
        translucentState.blendEnabled      = true;
        translucentState.blendSrcFactor    = VK_BLEND_FACTOR_SRC_ALPHA;
        translucentState.blendDstFactor    = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;

        // GUI / overlay
        PipelineState guiState = new PipelineState();
        guiState.depthTestEnabled  = false;
        guiState.depthWriteEnabled = false;
        guiState.cullEnabled       = false;
        guiState.blendEnabled      = true;
        guiState.blendSrcFactor    = VK_BLEND_FACTOR_SRC_ALPHA;
        guiState.blendDstFactor    = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;

        // Entity
        PipelineState entityState = new PipelineState();
        entityState.depthTestEnabled  = true;
        entityState.depthWriteEnabled = true;
        entityState.cullEnabled       = false;
        entityState.blendEnabled      = false;

        // We defer actual pipeline object creation until shaders are available
        // For now register state templates
        Initializer.LOGGER.info("PipelineManager initialized with {} state templates", 4);
    }

    public static void bindPipeline(String name) {
        if (name == null || name.equals(currentPipeline)) return;
        GraphicsPipeline pipeline = pipelines.get(name);
        if (pipeline != null) {
            VkCommandBuffer cmd = Renderer.getCurrentCommandBuffer();
            if (cmd != null) {
                pipeline.bind(cmd);
                currentPipeline = name;
            }
        }
    }

    public static void register(String name, GraphicsPipeline pipeline) {
        GraphicsPipeline old = pipelines.put(name, pipeline);
        if (old != null) old.destroy();
    }

    public static void onResize() {
        // Pipelines with dynamic state don't need recreation on resize
        // Only pipelines with baked viewport need rebuild
        currentPipeline = null;
    }

    public static void cleanup() {
        pipelines.values().forEach(GraphicsPipeline::destroy);
        pipelines.clear();
        GraphicsPipeline.destroyCache();
        currentPipeline = null;
    }

    public static GraphicsPipeline get(String name) { return pipelines.get(name); }
    public static String getCurrentPipeline() { return currentPipeline; }
}
