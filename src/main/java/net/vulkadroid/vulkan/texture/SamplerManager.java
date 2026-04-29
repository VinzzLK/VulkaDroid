package net.vulkadroid.vulkan.texture;

import net.vulkadroid.vulkan.device.DeviceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class SamplerManager {

    public record SamplerKey(int magFilter, int minFilter, int addressMode, boolean anisotropy) {}

    private static final Map<SamplerKey, Long> samplers = new HashMap<>();

    public static long getSampler(int magFilter, int minFilter, int addressMode, boolean anisotropy) {
        SamplerKey key = new SamplerKey(magFilter, minFilter, addressMode, anisotropy);
        return samplers.computeIfAbsent(key, k -> createSampler(magFilter, minFilter, addressMode, anisotropy));
    }

    private static long createSampler(int magFilter, int minFilter, int addressMode, boolean anisotropy) {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo info = VkSamplerCreateInfo.calloc(stack);
            info.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            info.magFilter(magFilter);
            info.minFilter(minFilter);
            info.addressModeU(addressMode);
            info.addressModeV(addressMode);
            info.addressModeW(addressMode);
            info.anisotropyEnable(anisotropy && DeviceManager.getDeviceFeatures().samplerAnisotropy());
            if (anisotropy) {
                info.maxAnisotropy(DeviceManager.getDeviceProperties().limits().maxSamplerAnisotropy());
            }
            info.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
            info.unnormalizedCoordinates(false);
            info.compareEnable(false);
            info.compareOp(VK_COMPARE_OP_ALWAYS);
            info.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
            info.minLod(0.0f);
            info.maxLod(16.0f);

            LongBuffer pSampler = stack.mallocLong(1);
            int result = vkCreateSampler(DeviceManager.getDevice(), info, null, pSampler);
            if (result != VK_SUCCESS)
                throw new RuntimeException("Failed to create sampler. VkResult: " + result);
            return pSampler.get(0);
        }
    }

    public static void cleanup() {
        for (long sampler : samplers.values()) {
            vkDestroySampler(DeviceManager.getDevice(), sampler, null);
        }
        samplers.clear();
    }
}
