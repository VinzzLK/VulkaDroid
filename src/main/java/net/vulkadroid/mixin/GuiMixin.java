package net.vulkadroid.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.vulkadroid.render.PipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        PipelineManager.bindPipeline("gui");
    }
}
