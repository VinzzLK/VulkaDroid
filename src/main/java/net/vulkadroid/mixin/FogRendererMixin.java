package net.vulkadroid.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.vulkadroid.vulkan.VRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void onSetupFog(Camera camera,
            FogRenderer.FogMode fogMode,
            float renderDistance, boolean bl, float partialTick, CallbackInfo ci) {
    }

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void onSetupColor(Camera camera, float partialTick,
            ClientLevel level, int renderDistance,
            float darkenWorldAmount, CallbackInfo ci) {
    }
}
