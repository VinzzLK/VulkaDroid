package net.vulkadroid.vulkan.texture;

import net.vulkadroid.vulkan.device.DeviceManager;
import net.vulkadroid.vulkan.memory.Allocation;
import net.vulkadroid.vulkan.memory.MemoryManager;
import net.vulkadroid.vulkan.memory.StagingBuffer;
import net.vulkadroid.vulkan.queue.CommandPool;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanImage {

    private long image        = VK_NULL_HANDLE;
    private long imageView    = VK_NULL_HANDLE;
    private long sampler      = VK_NULL_HANDLE;
    private Allocation memory;

    private final int width;
    private final int height;
    private final int format;
    private int mipLevels = 1;
    private int currentLayout = VK_IMAGE_LAYOUT_UNDEFINED;

    public VulkanImage(int width, int height, int format) {
        this.width  = width;
        this.height = height;
        this.format = format;
        create();
    }

    private void create() {
        memory = MemoryManager.allocateImage(width, height, format,
            VK_IMAGE_TILING_OPTIMAL,
            VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        image = memory.handle;

        createImageView();
        createSampler();
    }

    private void createImageView() {
        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(image);
            viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(format);
            viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(mipLevels);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);

            LongBuffer pView = stack.mallocLong(1);
            int result = vkCreateImageView(DeviceManager.getDevice(), viewInfo, null, pView);
            if (result != VK_SUCCESS)
                throw new RuntimeException("Failed to create texture image view. VkResult: " + result);
            imageView = pView.get(0);
        }
    }

    private void createSampler() {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerInfo.magFilter(VK_FILTER_NEAREST); // Minecraft uses nearest for blocks
            samplerInfo.minFilter(VK_FILTER_LINEAR);
            samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);

            if (DeviceManager.getDeviceFeatures().samplerAnisotropy()) {
                samplerInfo.anisotropyEnable(true);
                samplerInfo.maxAnisotropy(
                    DeviceManager.getDeviceProperties().limits().maxSamplerAnisotropy());
            }

            samplerInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);
            samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST);
            samplerInfo.mipLodBias(0.0f);
            samplerInfo.minLod(0.0f);
            samplerInfo.maxLod((float) mipLevels);

            LongBuffer pSampler = stack.mallocLong(1);
            int result = vkCreateSampler(DeviceManager.getDevice(), samplerInfo, null, pSampler);
            if (result != VK_SUCCESS)
                throw new RuntimeException("Failed to create texture sampler. VkResult: " + result);
            sampler = pSampler.get(0);
        }
    }

    public void upload(ByteBuffer pixels, CommandPool pool) {
        long size = (long) width * height * 4; // RGBA
        StagingBuffer staging = MemoryManager.createStagingBuffer(size);
        ByteBuffer mapped = staging.map();
        mapped.put(pixels);
        pixels.rewind();
        staging.unmap();
        staging.copyToImage(image, width, height, pool);
        staging.destroy();
        currentLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    }

    public void destroy() {
        if (sampler    != VK_NULL_HANDLE) { vkDestroySampler(DeviceManager.getDevice(), sampler, null);    sampler = VK_NULL_HANDLE; }
        if (imageView  != VK_NULL_HANDLE) { vkDestroyImageView(DeviceManager.getDevice(), imageView, null); imageView = VK_NULL_HANDLE; }
        if (memory != null) { memory.free(); memory = null; image = VK_NULL_HANDLE; }
    }

    public long getImage()     { return image; }
    public long getImageView() { return imageView; }
    public long getSampler()   { return sampler; }
    public int  getWidth()     { return width; }
    public int  getHeight()    { return height; }
    public int  getFormat()    { return format; }
}
