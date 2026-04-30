package net.vulkadroid.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.vulkadroid.Initializer;
import net.vulkadroid.vulkan.VRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PENTING: Semua method HARUS cek isInitialized() sebelum cancel.
 * Kalau Vulkan gagal init dan fallback ke OpenGL, jangan intercept/cancel
 * GL calls — biarkan libmobileglues.so yang handle. Tanpa guard ini,
 * GL state corrupt → SIGSEGV di glTexImage2D.
 */
@Mixin(GlStateManager.class)
public class GlStateManagerMixin {

    @Inject(method = "_blendFunc", at = @At("HEAD"), cancellable = true, remap = false)
    private static void blendFunc(int src, int dst, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.blendFunc(src, dst);
        ci.cancel();
    }

    @Inject(method = "_blendFuncSeparate", at = @At("HEAD"), cancellable = true, remap = false)
    private static void blendFuncSeparate(int srcRGB, int dstRGB, int srcA, int dstA, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.blendFuncSeparate(srcRGB, dstRGB, srcA, dstA);
        ci.cancel();
    }

    @Inject(method = "_depthFunc", at = @At("HEAD"), cancellable = true, remap = false)
    private static void depthFunc(int func, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.depthFunc(func);
        ci.cancel();
    }

    @Inject(method = "_depthMask", at = @At("HEAD"), cancellable = true, remap = false)
    private static void depthMask(boolean flag, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.depthMask(flag);
        ci.cancel();
    }

    @Inject(method = "_colorMask", at = @At("HEAD"), cancellable = true, remap = false)
    private static void colorMask(boolean r, boolean g, boolean b, boolean a, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.colorMask(r, g, b, a);
        ci.cancel();
    }

    @Inject(method = "_polygonOffset", at = @At("HEAD"), cancellable = true, remap = false)
    private static void polygonOffset(float factor, float units, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.polygonOffset(factor, units);
        ci.cancel();
    }

    @Inject(method = "_activeTexture", at = @At("HEAD"), cancellable = true, remap = false)
    private static void activeTexture(int unit, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.activeTexture(unit);
        ci.cancel();
    }

    @Inject(method = "_bindTexture", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bindTexture(int id, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.bindTexture(id);
        ci.cancel();
    }

    @Inject(method = "_deleteTexture", at = @At("HEAD"), cancellable = true, remap = false)
    private static void deleteTexture(int id, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        net.vulkadroid.vulkan.texture.VTextureSelector.unregisterTexture(id);
        ci.cancel();
    }

    @Inject(method = "_stencilFunc", at = @At("HEAD"), cancellable = true, remap = false)
    private static void stencilFunc(int func, int ref, int mask, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.stencilFunc(func, ref, mask);
        ci.cancel();
    }

    @Inject(method = "_stencilMask", at = @At("HEAD"), cancellable = true, remap = false)
    private static void stencilMask(int mask, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.stencilMask(mask);
        ci.cancel();
    }

    @Inject(method = "_stencilOp", at = @At("HEAD"), cancellable = true, remap = false)
    private static void stencilOp(int sfail, int dpfail, int dppass, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.stencilOp(sfail, dpfail, dppass);
        ci.cancel();
    }

    @Inject(method = "_logicOp", at = @At("HEAD"), cancellable = true, remap = false)
    private static void logicOp(int op, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VRenderSystem.logicOp(op);
        ci.cancel();
    }
}
