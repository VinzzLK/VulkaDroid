package net.vulkadroid.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.vulkadroid.Initializer;
import net.vulkadroid.vulkan.Renderer;
import net.vulkadroid.vulkan.texture.VTextureSelector;
import net.vulkadroid.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_SRGB;

@Mixin(DynamicTexture.class)
public class DynamicTextureMixin {

    @Shadow
    private NativeImage pixels;

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true, remap = false)
    private void onUpload(CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        
        if (this.pixels == null) { 
            ci.cancel(); 
            return; 
        }

        int w = this.pixels.getWidth();
        int h = this.pixels.getHeight();
        
        DynamicTexture self = (DynamicTexture)(Object)this;
        int glId = self.getId();

        VulkanImage existing = VTextureSelector.getTextureById(glId);
        if (existing == null || existing.getWidth() != w || existing.getHeight() != h) {
            if (existing != null) existing.destroy();
            existing = new VulkanImage(w, h, VK_FORMAT_R8G8B8A8_SRGB);
            VTextureSelector.registerTexture(glId, existing);
        }

        // Fix: Convert NativeImage ke ByteBuffer manual
        ByteBuffer pixelsBuffer = ByteBuffer.allocateDirect(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = this.pixels.getPixelRGBA(x, y);
                pixelsBuffer.put((byte)((pixel >> 16) & 0xFF)); // R
                pixelsBuffer.put((byte)((pixel >> 8) & 0xFF));  // G
                pixelsBuffer.put((byte)(pixel & 0xFF));         // B
                pixelsBuffer.put((byte)((pixel >> 24) & 0xFF)); // A
            }
        }
        pixelsBuffer.flip();
        
        if (Renderer.getGraphicsCommandPool() != null) {
            existing.upload(pixelsBuffer, Renderer.getGraphicsCommandPool());
        }
        ci.cancel();
    }
}