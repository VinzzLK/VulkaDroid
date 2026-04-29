package net.vulkadroid.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.vulkadroid.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void onBlurEffect(float partialTick, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        ci.cancel();
    }

    @Inject(method = "loadEffect", at = @At("HEAD"), cancellable = true)
    private void onLoadEffect(ResourceLocation loc, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        Initializer.LOGGER.debug("Post-process effect skipped: {}", loc);
        ci.cancel();
    }
}
