package net.vulkadroid.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.vulkadroid.vulkan.VRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Supplier;

@Mixin(value = RenderSystem.class, remap = false)
public class RenderSystemMixin {

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true, remap = false)
    private static void clear(int mask, boolean checkError, CallbackInfo ci) {
        VRenderSystem.clear(mask, checkError); 
        ci.cancel();
    }

    @Inject(method = "clearColor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void clearColor(float r, float g, float b, float a, CallbackInfo ci) {
        VRenderSystem.clearColor(r, g, b, a); 
        ci.cancel();
    }

    @Inject(method = "depthFunc", at = @At("HEAD"), cancellable = true, remap = false)
    private static void depthFunc(int func, CallbackInfo ci) {
        VRenderSystem.depthFunc(func); 
        ci.cancel();
    }

    @Inject(method = "depthMask", at = @At("HEAD"), cancellable = true, remap = false)
    private static void depthMask(boolean flag, CallbackInfo ci) {
        VRenderSystem.depthMask(flag); 
        ci.cancel();
    }

    @Inject(method = "blendEquation", at = @At("HEAD"), cancellable = true, remap = false)
    private static void blendEquation(int mode, CallbackInfo ci) {
        VRenderSystem.blendEquation(mode); 
        ci.cancel();
    }

    @Inject(method = "polygonOffset", at = @At("HEAD"), cancellable = true, remap = false)
    private static void polygonOffset(float factor, float units, CallbackInfo ci) {
        VRenderSystem.polygonOffset(factor, units); 
        ci.cancel();
    }

    @Inject(method = "viewport", at = @At("HEAD"), cancellable = true, remap = false)
    private static void viewport(int x, int y, int w, int h, CallbackInfo ci) {
        VRenderSystem.viewport(x, y, w, h); 
        ci.cancel();
    }

    @Inject(method = "activeTexture", at = @At("HEAD"), cancellable = true, remap = false)
    private static void activeTexture(int unit, CallbackInfo ci) {
        VRenderSystem.activeTexture(unit); 
        ci.cancel();
    }

    @Inject(method = "bindTexture", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bindTexture(int id, CallbackInfo ci) {
        VRenderSystem.bindTexture(id); 
        ci.cancel();
    }

    @Inject(method = "setShader", at = @At("HEAD"), cancellable = true, remap = false)
    private static void setShader(Supplier<ShaderInstance> shader, CallbackInfo ci) {
        VRenderSystem.setShader(shader); 
        ci.cancel();
    }

    @Inject(method = "setShaderTexture(IILnet/minecraft/resources/ResourceLocation;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void setShaderTexture(int index, int id, CallbackInfo ci) {
        VRenderSystem.setShaderTexture(index, id); 
        ci.cancel();
    }

    @Inject(method = "colorMask", at = @At("HEAD"), cancellable = true, remap = false)
    private static void colorMask(boolean r, boolean g, boolean b, boolean a, CallbackInfo ci) {
        VRenderSystem.colorMask(r, g, b, a); 
        ci.cancel();
    }
}