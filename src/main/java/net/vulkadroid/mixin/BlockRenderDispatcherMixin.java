package net.vulkadroid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkadroid.render.PipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// MC 1.21.1 signature: renderBreakingTexture(BlockState, BlockPos, BlockAndTintGetter, PoseStack, MultiBufferSource.BufferSource)
// BakedModel DIHAPUS (tidak ada di 1.21.1), MultiBufferSource → MultiBufferSource.BufferSource
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    @Inject(method = "renderBreakingTexture", at = @At("HEAD"))
    private void onRenderBreaking(BlockState state, BlockPos pos,
            BlockAndTintGetter level,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        PipelineManager.bindPipeline("terrain_translucent");
    }
}
