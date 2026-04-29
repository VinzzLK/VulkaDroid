package net.vulkadroid.vulkan.memory;

import net.vulkadroid.vulkan.device.DeviceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.vulkan.VK10.*;

public class Allocation {
    public final long handle;  // VkBuffer or VkImage
    public final long memory;  // VkDeviceMemory
    public final long size;
    public final boolean isImage;

    public Allocation(long handle, long memory, long size, boolean isImage) {
        this.handle = handle;
        this.memory = memory;
        this.size = size;
        this.isImage = isImage;
    }

    public ByteBuffer map() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pData = stack.mallocPointer(1);
            int result = vkMapMemory(DeviceManager.getDevice(), memory, 0, size, 0, pData);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to map memory. VkResult: " + result);
            }
            return memByteBuffer(pData.get(0), (int) size);
        }
    }

    public void unmap() {
        vkUnmapMemory(DeviceManager.getDevice(), memory);
    }

    public void free() {
        MemoryManager.freeAllocation(this);
    }
}
