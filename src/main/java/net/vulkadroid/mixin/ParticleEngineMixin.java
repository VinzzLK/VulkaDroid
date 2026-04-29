package net.vulkadroid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.particle.ParticleEngine;
import net.vulkadroid.render.PipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
            LightTexture lightTexture, Camera camera, float partialTick,
            Frustum frustum, CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        PipelineManager.bindPipeline("terrain_translucent");
    }
}
