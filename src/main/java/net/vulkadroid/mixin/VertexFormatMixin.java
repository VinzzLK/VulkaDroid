package net.vulkadroid.mixin;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexFormat.class)
public class VertexFormatMixin {
    @Inject(method = "setupBufferState", at = @At("HEAD"), cancellable = true)
    private void onSetupBufferState(CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        ci.cancel();
    }
    @Inject(method = "clearBufferState", at = @At("HEAD"), cancellable = true)
    private void onClearBufferState(CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        ci.cancel();
    }
}
