package net.vulkadroid.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.vulkadroid.render.PipelineManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private <T extends Entity> void onRender(T entity, double x, double y, double z,
            float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        PipelineManager.bindPipeline("entity");
    }
}
