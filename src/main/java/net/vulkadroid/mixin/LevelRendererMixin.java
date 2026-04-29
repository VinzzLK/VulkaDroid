package net.vulkadroid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.vulkadroid.Initializer;
import net.vulkadroid.render.chunk.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // remap = true (default) → Loom akan generate refmap entry:
    // Mojang "renderChunkLayer" → intermediary "method_XXXXX"
    @Inject(method = "renderChunkLayer", at = @At("HEAD"), cancellable = true)
    private void onRenderChunkLayer(RenderType renderType,
            double x, double y, double z,
            Matrix4f frustumMatrix, Matrix4f projectionMatrix,
            CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        WorldRenderer.renderLayer(renderType, x, y, z, frustumMatrix, projectionMatrix);
        ci.cancel();
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevelHead(PoseStack poseStack, float partialTick,
            long finishNanoTime, boolean renderBlockOutline,
            Camera camera, GameRenderer gameRenderer,
            LightTexture lightTexture, Matrix4f projectionMatrix,
            CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        float[] proj = new float[16];
        float[] view = new float[16];
        projectionMatrix.get(proj);
        poseStack.last().pose().get(view);
        net.vulkadroid.vulkan.VRenderSystem.setProjectionMatrix(proj);
        net.vulkadroid.vulkan.VRenderSystem.setModelViewMatrix(view);
    }

    @Inject(method = "allChanged", at = @At("TAIL"))
    private void onAllChanged(CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        WorldRenderer.onWorldChanged();
    }

    @Inject(method = "onChunkLoaded", at = @At("TAIL"))
    private void onChunkLoaded(int cx, int cz, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        WorldRenderer.onChunkLoaded(cx, cz);
    }

    @Inject(method = "setSectionDirty", at = @At("TAIL"))
    private void onSectionDirty(int cx, int cy, int cz, boolean bl, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        WorldRenderer.markSectionDirty(cx, cy, cz);
    }
}
