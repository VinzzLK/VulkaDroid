package net.vulkadroid.mixin;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.vulkadroid.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class LightTextureMixin {
    @Shadow private DynamicTexture lightTexture;
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) { }
}
