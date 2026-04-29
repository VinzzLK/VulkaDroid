package net.vulkadroid.mixin;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.vulkadroid.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractTexture.class)
public class AbstractTextureMixin {
    @Inject(method = "releaseId", at = @At("HEAD"))
    private void onReleaseId(CallbackInfo ci) {
        VTextureSelector.unregisterTexture(((AbstractTexture)(Object)this).getId());
    }
}
