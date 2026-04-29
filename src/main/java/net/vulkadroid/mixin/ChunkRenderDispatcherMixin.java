package net.vulkadroid.mixin;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.vulkadroid.render.chunk.build.TaskDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// In 1.21.1, ChunkRenderDispatcher was renamed to SectionRenderDispatcher
@Mixin(SectionRenderDispatcher.class)
public class ChunkRenderDispatcherMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        TaskDispatcher.initialize();
    }

    @Inject(method = "dispose", at = @At("HEAD"))
    private void onDispose(CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized()) return;
        TaskDispatcher.shutdown();
    }
}
