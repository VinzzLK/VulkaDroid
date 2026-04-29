package net.vulkadroid.render.chunk;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.vulkadroid.Initializer;
import net.vulkadroid.render.PipelineManager;
import net.vulkadroid.render.chunk.build.TaskDispatcher;
import net.vulkadroid.vulkan.Renderer;
import org.joml.Matrix4f;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class WorldRenderer {

    private static double cameraX, cameraY, cameraZ;
    private static Frustum currentFrustum;
    private static final ConcurrentLinkedQueue<RenderSection> dirtyQueue = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger sectionCount = new AtomicInteger(0);

    private static net.vulkadroid.render.chunk.buffer.DrawBuffers solidBuffers;
    private static net.vulkadroid.render.chunk.buffer.DrawBuffers translucentBuffers;

    public static void initialize() {
        solidBuffers       = new net.vulkadroid.render.chunk.buffer.DrawBuffers(64 * 1024 * 1024);
        translucentBuffers = new net.vulkadroid.render.chunk.buffer.DrawBuffers(32 * 1024 * 1024);
        Initializer.LOGGER.info("WorldRenderer initialized");
    }

    public static void renderLayer(RenderType renderType, double x, double y, double z,
                                   Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        if (!Renderer.isFrameStarted()) return;

        boolean isSolid = renderType == RenderType.solid()
                       || renderType == RenderType.cutout()
                       || renderType == RenderType.cutoutMipped();

        PipelineManager.bindPipeline(isSolid ? "terrain_solid" : "terrain_translucent");

        net.vulkadroid.render.chunk.buffer.DrawBuffers buffers = isSolid ? solidBuffers : translucentBuffers;
        if (buffers != null) buffers.flushAndDraw();
    }

    public static void updateFrustum(Frustum frustum) {
        currentFrustum = frustum;
    }

    public static void updateCameraPosition(double x, double y, double z) {
        cameraX = x; cameraY = y; cameraZ = z;
    }

    public static void onCameraReposition(double x, double z) { }

    public static void markSectionDirty(int cx, int cy, int cz) {
        TaskDispatcher.scheduleBuild(cx, cy, cz);
    }

    public static void onChunkLoaded(int cx, int cz) {
        for (int cy = -4; cy < 20; cy++) TaskDispatcher.scheduleBuild(cx, cy, cz);
    }

    public static void onWorldChanged() {
        dirtyQueue.clear();
        sectionCount.set(0);
        if (solidBuffers != null)       solidBuffers.reset();
        if (translucentBuffers != null) translucentBuffers.reset();
        Initializer.LOGGER.info("WorldRenderer reset for new world");
    }

    public static void cleanup() {
        TaskDispatcher.shutdown();
        if (solidBuffers != null)       { solidBuffers.destroy();       solidBuffers = null; }
        if (translucentBuffers != null) { translucentBuffers.destroy(); translucentBuffers = null; }
    }

    public static double getCameraX()  { return cameraX; }
    public static double getCameraY()  { return cameraY; }
    public static double getCameraZ()  { return cameraZ; }
    public static Frustum getFrustum() { return currentFrustum; }
}
