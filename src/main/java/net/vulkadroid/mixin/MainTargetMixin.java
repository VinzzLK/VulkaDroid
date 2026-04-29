package net.vulkadroid.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.vulkadroid.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public abstract class MainTargetMixin {
    
    @Inject(method = "blitToScreen(II)V", at = @At("HEAD"), cancellable = true)
    private void onBlitToScreen(int width, int height, CallbackInfo ci) {
        ci.cancel();
    }
}
