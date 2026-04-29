package net.vulkadroid.mixin;

import net.minecraft.client.Minecraft;
import net.vulkadroid.Initializer;
import net.vulkadroid.render.Drawer;
import net.vulkadroid.render.PipelineManager;
import net.vulkadroid.vulkan.Renderer;
import net.vulkadroid.vulkan.Vulkan;
import net.vulkadroid.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onGameInit(CallbackInfo ci) {
        if (Initializer.isInitialized()) {
            Renderer.initialize();
            PipelineManager.initialize();
            Drawer.initialize();
            Initializer.LOGGER.info("VulkaDroid render systems initialized");
        }
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTickHead(boolean bl, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        Renderer.beginFrame();
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void onRunTickTail(boolean bl, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        Renderer.endFrame();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onGameShutdown(CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        VTextureSelector.cleanup();
        Drawer.cleanup();
        PipelineManager.cleanup();
        Renderer.cleanup();
        Vulkan.cleanup();
    }

    @Inject(method = "resizeDisplay", at = @At("TAIL"))
    private void onResizeDisplay(CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        Minecraft mc = (Minecraft)(Object)this;
        net.vulkadroid.config.Config.setWindowSize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        Renderer.handleResize();
    }
}
