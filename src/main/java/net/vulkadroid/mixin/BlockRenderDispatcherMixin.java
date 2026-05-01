package net.vulkadroid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkadroid.render.PipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * renderBreakingTexture signature in 1.21.1 Mojang mappings:
 * void renderBreakingTexture(BlockState state, BlockPos pos, BlockAndTintGetter level,
 *     PoseStack poseStack, VertexConsumer vertexConsumer, CallbackInfo ci)
 *
 * class_4588 = VertexConsumer (correct parameter type)
 * class_4597 = MultiBufferSource (WRONG - this is what caused InvalidInjectionException)
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    @Inject(
        method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
        at = @At("HEAD"),
        require = 0,
        cancellable = false
    )
    private void onRenderBreaking(BlockState state, BlockPos pos,
            BlockAndTintGetter level,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        PipelineManager.bindPipeline("terrain_translucent");
    }
}
