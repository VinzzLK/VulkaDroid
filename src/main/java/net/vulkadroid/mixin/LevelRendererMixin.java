package net.vulkadroid.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.ChunkPos;
import net.vulkadroid.Initializer;
import net.vulkadroid.render.chunk.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderChunkLayer", at = @At("HEAD"), cancellable = true)
    private void onRenderChunkLayer(
            RenderType renderType,
            double x, double y, double z,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        WorldRenderer.renderLayer(renderType, x, y, z, frustumMatrix, projectionMatrix);
        ci.cancel();
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevelHead(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        float[] proj = new float[16];
        float[] view = new float[16];
        projectionMatrix.get(proj);
        modelViewMatrix.get(view);
        net.vulkadroid.vulkan.VRenderSystem.setProjectionMatrix(proj);
        net.vulkadroid.vulkan.VRenderSystem.setModelViewMatrix(view);
    }

    @Inject(method = "allChanged", at = @At("TAIL"))
    private void onAllChanged(CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        WorldRenderer.onWorldChanged();
    }

    @Inject(method = "onChunkLoaded", at = @At("TAIL"))
    private void onChunkLoaded(ChunkPos chunkPos, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        WorldRenderer.onChunkLoaded(chunkPos.x, chunkPos.z);
    }

    // 1.21.1: boolean bl dihapus dari setSectionDirty
    @Inject(method = "setSectionDirty", at = @At("TAIL"))
    private void onSectionDirty(int cx, int cy, int cz, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        WorldRenderer.markSectionDirty(cx, cy, cz);
    }
}