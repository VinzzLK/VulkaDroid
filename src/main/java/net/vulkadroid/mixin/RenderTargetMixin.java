package net.vulkadroid.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.vulkadroid.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public class RenderTargetMixin {
    @Inject(method = "createBuffers", at = @At("HEAD"), cancellable = true)
    private void onCreateBuffers(int w, int h, boolean bl, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        RenderTarget self = (RenderTarget)(Object)this;
        self.width = w; self.height = h;
        ci.cancel();
    }
    @Inject(method = "bindWrite", at = @At("HEAD"), cancellable = true)
    private void onBindWrite(boolean bl, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        ci.cancel();
    }
    @Inject(method = "unbindWrite", at = @At("HEAD"), cancellable = true)
    private void onUnbindWrite(CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        ci.cancel();
    }
    @Inject(method = "bindRead", at = @At("HEAD"), cancellable = true)
    private void onBindRead(CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        ci.cancel();
    }
    @Inject(method = "destroyBuffers", at = @At("HEAD"), cancellable = true)
    private void onDestroyBuffers(CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        ci.cancel();
    }
    @Inject(method = "blitToScreen", at = @At("HEAD"), cancellable = true)
    private void onBlitToScreen(int w, int h, boolean bl, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        ci.cancel();
    }
}
