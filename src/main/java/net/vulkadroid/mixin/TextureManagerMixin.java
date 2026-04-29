package net.vulkadroid.mixin;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.vulkadroid.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class TextureManagerMixin {

    @Inject(method = "register(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/texture/AbstractTexture;)V",
            at = @At("TAIL"))
    private void onRegister(ResourceLocation loc, AbstractTexture texture, CallbackInfo ci) {
        VTextureSelector.registerLocation(loc, texture.getId());
    }

    @Inject(method = "release", at = @At("HEAD"))
    private void onRelease(ResourceLocation loc, CallbackInfo ci) {
        VTextureSelector.removeLocation(loc);
    }
}