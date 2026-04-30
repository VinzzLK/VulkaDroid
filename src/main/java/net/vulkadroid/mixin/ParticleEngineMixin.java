package net.vulkadroid.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.particle.ParticleEngine;
import net.vulkadroid.render.PipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// MC 1.21.1 signature: render(LightTexture, Camera, float)
// PoseStack, BufferSource, Frustum DIHAPUS (beda dari versi lama)
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        PipelineManager.bindPipeline("terrain_translucent");
    }
}
