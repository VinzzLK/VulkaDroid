package net.vulkadroid.gl;

import net.vulkadroid.vulkan.texture.VTextureSelector;
import net.vulkadroid.vulkan.texture.VulkanImage;

import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_SRGB;

/**
 * Shim that maps OpenGL texture IDs to VulkanImage objects.
 * GL code calls genTextures(), which returns a fake ID.
 * Actual Vulkan image is created on first upload.
 */
public class VkGlTexture {

    private static final AtomicInteger idCounter = new AtomicInteger(1);

    public static int genTexture() {
        return idCounter.getAndIncrement();
    }

    public static void texImage2D(int id, int width, int height, java.nio.ByteBuffer pixels) {
        VulkanImage existing = VTextureSelector.getTextureById(id);
        if (existing == null || existing.getWidth() != width || existing.getHeight() != height) {
            if (existing != null) existing.destroy();
            existing = new VulkanImage(width, height, VK_FORMAT_R8G8B8A8_SRGB);
            VTextureSelector.registerTexture(id, existing);
        }
        if (pixels != null && net.vulkadroid.vulkan.Renderer.getGraphicsCommandPool() != null) {
            existing.upload(pixels, net.vulkadroid.vulkan.Renderer.getGraphicsCommandPool());
        }
    }

    public static void deleteTexture(int id) {
        VTextureSelector.unregisterTexture(id);
    }

    public static void bindTexture(int unit, int id) {
        VTextureSelector.bindTexture(unit, id);
    }
}
