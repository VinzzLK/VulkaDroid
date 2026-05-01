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

// class_4588 = MultiBufferSource (INTERFACE, bukan BufferSource subclass)
// class_4597$class_4598 = RenderBuffers$BufferSource (SALAH)
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    @Inject(method = "renderBreakingTexture", at = @At("HEAD"), require = 0)
    private void onRenderBreaking(BlockState state, BlockPos pos,
            BlockAndTintGetter level,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        PipelineManager.bindPipeline("terrain_translucent");
    }
}
