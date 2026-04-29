package net.vulkadroid.mixin;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkadroid.Initializer;
import net.vulkadroid.vulkan.Renderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public class VertexBufferMixin {
    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    private void onUpload(MeshData meshData, CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        ci.cancel();
    }
    @Inject(method = "bind", at = @At("HEAD"), cancellable = true)
    private void onBind(CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        ci.cancel();
    }
    @Inject(method = "drawWithShader", at = @At("HEAD"), cancellable = true)
    private void onDrawWithShader(PoseStack poseStack, Matrix4f projectionMatrix,
            ShaderInstance shader, CallbackInfo ci) {
        if (!Initializer.isInitialized() || !Renderer.isFrameStarted()) return;
        ci.cancel();
    }
}
