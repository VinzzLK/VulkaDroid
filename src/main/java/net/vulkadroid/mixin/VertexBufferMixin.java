package net.vulkadroid.mixin;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkadroid.Initializer;
import net.vulkadroid.vulkan.Renderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// MC 1.21.1 signature: drawWithShader(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, ShaderInstance)
// PoseStack DIHAPUS, diganti Matrix4f pertama sebagai modelViewMatrix
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
    private void onDrawWithShader(Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
            ShaderInstance shader, CallbackInfo ci) {
        if (!Initializer.isInitialized() || !Renderer.isFrameStarted()) return;
        ci.cancel();
    }
}
