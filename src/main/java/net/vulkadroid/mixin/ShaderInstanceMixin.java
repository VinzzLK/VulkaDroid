package net.vulkadroid.mixin;

import net.minecraft.client.renderer.ShaderInstance;
import net.vulkadroid.render.PipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public class ShaderInstanceMixin {
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void onApply(CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        PipelineManager.bindPipeline(((ShaderInstance)(Object)this).getName());
        ci.cancel();
    }
    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    private void onClear(CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        ci.cancel();
    }
}
