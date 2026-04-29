package net.vulkadroid.mixin;

import net.minecraft.client.renderer.ViewArea;
import net.vulkadroid.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ViewArea.class)
public class ViewAreaMixin {
    @Inject(method = "repositionCamera", at = @At("TAIL"))
    private void onRepositionCamera(double x, double z, CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        WorldRenderer.onCameraReposition(x, z);
    }
}
