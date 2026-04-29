package net.vulkadroid.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.vulkadroid.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class FrustumMixin {
    @Inject(method = "prepare", at = @At("TAIL"))
    private void onPrepare(double x, double y, double z, CallbackInfo ci) {
        WorldRenderer.updateFrustum((Frustum)(Object)this);
    }
}
