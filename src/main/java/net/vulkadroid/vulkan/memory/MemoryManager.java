package net.vulkadroid.vulkan.memory;

import net.vulkadroid.Initializer;
import net.vulkadroid.vulkan.device.DeviceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.concurrent.atomic.AtomicLong;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class MemoryManager {

    private static VkPhysicalDevice physicalDevice;
    private static VkDevice device;

    private static final AtomicLong totalAllocated = new AtomicLong(0);
    private static final AtomicLong deviceLocalAllocated = new AtomicLong(0);

    // Memory type indices cached for fast lookup
    private static int deviceLocalMemoryType = -1;
    private static int hostVisibleCoherentMemoryType = -1;
    private static int hostVisibleCachedMemoryType = -1;

    public static void initialize(VkPhysicalDevice physDev, VkDevice dev, VkInstance instance) {
        physicalDevice = physDev;
        device = dev;
        cacheMemoryTypes();
        Initializer.LOGGER.info("MemoryManager initialized. Device-local type: {}, Host-visible type: {}",
            deviceLocalMemoryType, hostVisibleCoherentMemoryType);
    }

    private static void cacheMemoryTypes() {
        VkPhysicalDeviceMemoryProperties memProps = DeviceManager.getMemoryProperties();

        for (int i = 0; i < memProps.memoryTypeCount(); i++) {
            int flags = memProps.memoryTypes(i).propertyFlags();

            if (deviceLocalMemoryType == -1 &&
                (flags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0 &&
                (flags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) == 0) {
                deviceLocalMemoryType = i;
            }

            if (hostVisibleCoherentMemoryType == -1 &&
                (flags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0 &&
                (flags & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0) {
                hostVisibleCoherentMemoryType = i;
            }

            if (hostVisibleCachedMemoryType == -1 &&
                (flags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0 &&
                (flags & VK_MEMORY_PROPERTY_HOST_CACHED_BIT) != 0) {
                hostVisibleCachedMemoryType = i;
            }
        }
    }

    public static Allocation allocateBuffer(long size, int usage, int memoryProperties) {
        try (MemoryStack stack = stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(usage);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.mallocLong(1);
            int result = vkCreateBuffer(device, bufferInfo, null, pBuffer);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create buffer. VkResult: " + result);
            }
            long buffer = pBuffer.get(0);

            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, buffer, memReqs);

            int memTypeIndex = findMemoryType(memReqs.memoryTypeBits(), memoryProperties);
            long memory = allocateMemory(memReqs.size(), memTypeIndex);

            result = vkBindBufferMemory(device, buffer, memory, 0);
            if (result != VK_SUCCESS) {
                vkDestroyBuffer(device, buffer, null);
                vkFreeMemory(device, memory, null);
                throw new RuntimeException("Failed to bind buffer memory. VkResult: " + result);
            }

            totalAllocated.addAndGet(memReqs.size());
            if ((memoryProperties & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0) {
                deviceLocalAllocated.addAndGet(memReqs.size());
            }

            return new Allocation(buffer, memory, memReqs.size(), false);
        }
    }

    public static Allocation allocateImage(int width, int height, int format, int tiling,
                                           int usage, int memoryProperties) {
        try (MemoryStack stack = stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(format);
            imageInfo.tiling(tiling);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);

            LongBuffer pImage = stack.mallocLong(1);
            int result = vkCreateImage(device, imageInfo, null, pImage);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image. VkResult: " + result);
            }
            long image = pImage.get(0);

            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, image, memReqs);

            int memTypeIndex = findMemoryType(memReqs.memoryTypeBits(), memoryProperties);
            long memory = allocateMemory(memReqs.size(), memTypeIndex);

            result = vkBindImageMemory(device, image, memory, 0);
            if (result != VK_SUCCESS) {
                vkDestroyImage(device, image, null);
                vkFreeMemory(device, memory, null);
                throw new RuntimeException("Failed to bind image memory. VkResult: " + result);
            }

            totalAllocated.addAndGet(memReqs.size());
            return new Allocation(image, memory, memReqs.size(), true);
        }
    }

    public static StagingBuffer createStagingBuffer(long size) {
        Allocation alloc = allocateBuffer(size,
            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        return new StagingBuffer(alloc, size);
    }

    private static long allocateMemory(long size, int memoryTypeIndex) {
        try (MemoryStack stack = stackPush()) {
            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(size);
            allocInfo.memoryTypeIndex(memoryTypeIndex);

            LongBuffer pMemory = stack.mallocLong(1);
            int result = vkAllocateMemory(device, allocInfo, null, pMemory);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate memory (size=" + size +
                    ", type=" + memoryTypeIndex + "). VkResult: " + result);
            }
            return pMemory.get(0);
        }
    }

    public static int findMemoryType(int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProps = DeviceManager.getMemoryProperties();
        for (int i = 0; i < memProps.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 &&
                (memProps.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }
        throw new RuntimeException("Failed to find suitable memory type! typeFilter=" +
            Integer.toBinaryString(typeFilter) + " properties=" + properties);
    }

    public static void freeAllocation(Allocation alloc) {
        if (alloc == null) return;
        totalAllocated.addAndGet(-alloc.size);
        if (alloc.isImage) {
            vkDestroyImage(device, alloc.handle, null);
        } else {
            vkDestroyBuffer(device, alloc.handle, null);
        }
        vkFreeMemory(device, alloc.memory, null);
    }

    public static long getTotalAllocatedBytes() { return totalAllocated.get(); }
    public static long getDeviceLocalAllocatedBytes() { return deviceLocalAllocated.get(); }

    public static void cleanup() {
        // All allocations should be freed by their owners
        Initializer.LOGGER.info("MemoryManager cleanup. Remaining allocated: {} bytes", totalAllocated.get());
    }

    public static int getDeviceLocalMemoryType() { return deviceLocalMemoryType; }
    public static int getHostVisibleCoherentMemoryType() { return hostVisibleCoherentMemoryType; }
}
