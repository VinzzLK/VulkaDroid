package net.vulkadroid.vulkan.shader;

import net.vulkadroid.vulkan.device.DeviceManager;
import net.vulkadroid.vulkan.texture.VTextureSelector;
import net.vulkadroid.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSets {

    private static long descriptorPool = VK_NULL_HANDLE;
    private static final int MAX_SETS = 256;
    private static final List<Long> allocatedSets = new ArrayList<>();

    public static void initialize() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);
            poolSizes.get(0).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(MAX_SETS * 8);
            poolSizes.get(1).type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(MAX_SETS);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT);
            poolInfo.maxSets(MAX_SETS);
            poolInfo.pPoolSizes(poolSizes);

            LongBuffer pPool = stack.mallocLong(1);
            int result = vkCreateDescriptorPool(DeviceManager.getDevice(), poolInfo, null, pPool);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool. VkResult: " + result);
            }
            descriptorPool = pPool.get(0);
        }
    }

    public static long allocate(long descriptorSetLayout) {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(descriptorPool);
            allocInfo.pSetLayouts(stack.longs(descriptorSetLayout));

            LongBuffer pSet = stack.mallocLong(1);
            int result = vkAllocateDescriptorSets(DeviceManager.getDevice(), allocInfo, pSet);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor set. VkResult: " + result);
            }
            long set = pSet.get(0);
            allocatedSets.add(set);
            return set;
        }
    }

    public static void updateTextureBinding(long descriptorSet, int binding, int textureUnit) {
        VulkanImage img = VTextureSelector.getTexture(textureUnit);
        if (img == null) return;

        try (MemoryStack stack = stackPush()) {
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            imageInfo.imageView(img.getImageView());
            imageInfo.sampler(img.getSampler());

            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(1, stack);
            write.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            write.dstSet(descriptorSet);
            write.dstBinding(binding);
            write.dstArrayElement(textureUnit);
            write.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            write.pImageInfo(imageInfo);

            vkUpdateDescriptorSets(DeviceManager.getDevice(), write, null);
        }
    }

    public static void cleanup() {
        allocatedSets.clear();
        if (descriptorPool != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(DeviceManager.getDevice(), descriptorPool, null);
            descriptorPool = VK_NULL_HANDLE;
        }
    }

    public static long getPool() { return descriptorPool; }
}
