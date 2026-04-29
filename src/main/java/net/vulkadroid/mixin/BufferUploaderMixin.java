package net.vulkadroid.mixin;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import net.vulkadroid.render.Drawer;
import net.vulkadroid.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferUploader.class)
public class BufferUploaderMixin {

    @Inject(method = "drawWithShader", at = @At("HEAD"), cancellable = true)
    private static void drawWithShader(MeshData meshData, CallbackInfo ci) {
        if (!net.vulkadroid.Initializer.isInitialized() || !Renderer.isFrameStarted()) return;

        MeshData.DrawState drawState = meshData.drawState();
        ByteBuffer vertexBuffer = meshData.vertexBuffer();
        ByteBuffer indexBuffer  = meshData.indexBuffer();

        if (vertexBuffer == null || drawState.indexCount() == 0) { ci.cancel(); return; }

        if (indexBuffer == null) {
            int vertexCount = drawState.vertexCount();
            int quadCount   = vertexCount / 4;
            ByteBuffer genIdx = ByteBuffer.allocate(quadCount * 6 * 4);
            java.nio.IntBuffer ib = genIdx.asIntBuffer();
            for (int q = 0; q < quadCount; q++) {
                int base = q * 4;
                ib.put(base).put(base+1).put(base+2).put(base+2).put(base+3).put(base);
            }
            genIdx.rewind();
            Drawer.uploadAndDraw(vertexBuffer, genIdx, vertexCount, quadCount * 6);
        } else {
            Drawer.uploadAndDraw(vertexBuffer, indexBuffer, drawState.vertexCount(), drawState.indexCount());
        }
        ci.cancel();
    }
}
