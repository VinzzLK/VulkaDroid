package net.vulkadroid.mixin;

import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkadroid.render.PipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    @Inject(method = "renderBreakingTexture", at = @At("HEAD"))
    private void onRenderBreaking(BlockState state, BlockPos pos,
            net.minecraft.world.level.BlockAndTintGetter level,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            net.minecraft.client.resources.model.BakedModel model,
            CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        PipelineManager.bindPipeline("terrain_translucent");
    }
}
