package net.vulkadroid.mixin;

import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.vulkadroid.render.PipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(ItemStack stack, ItemDisplayContext displayContext, boolean bl,
                          PoseStack poseStack, MultiBufferSource bufferSource, int i, int j,
                          net.minecraft.class_1087 arg, CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        PipelineManager.bindPipeline("entity");
    }
}