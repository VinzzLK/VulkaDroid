package net.vulkadroid.vulkan.queue;

import net.vulkadroid.vulkan.device.DeviceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class CommandPool {

    private long pool;
    private final int queueFamilyIndex;

    public CommandPool(int queueFamilyIndex, int flags) {
        this.queueFamilyIndex = queueFamilyIndex;
        try (MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo createInfo = VkCommandPoolCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            createInfo.queueFamilyIndex(queueFamilyIndex);
            createInfo.flags(flags);

            LongBuffer pPool = stack.mallocLong(1);
            int result = vkCreateCommandPool(DeviceManager.getDevice(), createInfo, null, pPool);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool. VkResult: " + result);
            }
            pool = pPool.get(0);
        }
    }

    public VkCommandBuffer allocatePrimary() {
        return allocate(VK_COMMAND_BUFFER_LEVEL_PRIMARY, 1)[0];
    }

    public VkCommandBuffer[] allocate(int level, int count) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(pool);
            allocInfo.level(level);
            allocInfo.commandBufferCount(count);

            PointerBuffer pBuffers = stack.mallocPointer(count);
            int result = vkAllocateCommandBuffers(DeviceManager.getDevice(), allocInfo, pBuffers);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers. VkResult: " + result);
            }

            VkCommandBuffer[] buffers = new VkCommandBuffer[count];
            for (int i = 0; i < count; i++) {
                buffers[i] = new VkCommandBuffer(pBuffers.get(i), DeviceManager.getDevice());
            }
            return buffers;
        }
    }

    public VkCommandBuffer beginSingleTime() {
        VkCommandBuffer cmd = allocatePrimary();
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            vkBeginCommandBuffer(cmd, beginInfo);
        }
        return cmd;
    }

    public void endSingleTime(VkCommandBuffer cmd, VkQueue queue) {
        vkEndCommandBuffer(cmd);
        try (MemoryStack stack = stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            PointerBuffer pCmd = stack.mallocPointer(1).put(cmd.address()).flip();
            submitInfo.pCommandBuffers(pCmd);
            vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(queue);
        }
        vkFreeCommandBuffers(DeviceManager.getDevice(), pool, cmd);
    }

    public void reset() {
        vkResetCommandPool(DeviceManager.getDevice(), pool, 0);
    }

    public void free(VkCommandBuffer cmd) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pCmd = stack.mallocPointer(1).put(cmd.address()).flip();
            vkFreeCommandBuffers(DeviceManager.getDevice(), pool, pCmd);
        }
    }

    public void destroy() {
        if (pool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(DeviceManager.getDevice(), pool, null);
            pool = VK_NULL_HANDLE;
        }
    }

    public long getHandle() { return pool; }
    public int getQueueFamilyIndex() { return queueFamilyIndex; }
}
