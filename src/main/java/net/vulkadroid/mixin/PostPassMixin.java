package net.vulkadroid.mixin;

import net.minecraft.client.renderer.PostPass;
import net.vulkadroid.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostPass.class)  // Ganti dari targets ke value
public class PostPassMixin {
    @Inject(method = "process", at = @At("HEAD"), cancellable = true, remap = false)
    private void onProcess(float partialTick, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        ci.cancel();
    }
}