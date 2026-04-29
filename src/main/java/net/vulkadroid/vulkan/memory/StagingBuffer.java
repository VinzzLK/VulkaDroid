package net.vulkadroid.vulkan.memory;

import net.vulkadroid.vulkan.device.DeviceManager;
import net.vulkadroid.vulkan.queue.CommandPool;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class StagingBuffer {

    private final Allocation allocation;
    private final long capacity;
    private boolean mapped = false;
    private ByteBuffer mappedBuffer;

    public StagingBuffer(Allocation allocation, long capacity) {
        this.allocation = allocation;
        this.capacity = capacity;
    }

    public ByteBuffer map() {
        if (!mapped) {
            mappedBuffer = allocation.map();
            mapped = true;
        }
        return mappedBuffer;
    }

    public void unmap() {
        if (mapped) {
            allocation.unmap();
            mapped = false;
            mappedBuffer = null;
        }
    }

    public void copyToBuffer(long dstBuffer, long size, CommandPool pool) {
        VkCommandBuffer cmd = pool.beginSingleTime();
        try (MemoryStack stack = stackPush()) {
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack);
            copyRegion.srcOffset(0);
            copyRegion.dstOffset(0);
            copyRegion.size(size);
            vkCmdCopyBuffer(cmd, allocation.handle, dstBuffer, copyRegion);
        }
        pool.endSingleTime(cmd, DeviceManager.getTransferQueue());
    }

    public void copyToImage(long dstImage, int width, int height, CommandPool pool) {
        VkCommandBuffer cmd = pool.beginSingleTime();
        try (MemoryStack stack = stackPush()) {
            // Transition image to transfer destination
            transitionImageLayout(cmd, dstImage,
                VK_IMAGE_LAYOUT_UNDEFINED,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, stack);

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);
            region.bufferImageHeight(0);
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent().set(width, height, 1);

            vkCmdCopyBufferToImage(cmd, allocation.handle, dstImage,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

            // Transition to shader read
            transitionImageLayout(cmd, dstImage,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, stack);
        }
        pool.endSingleTime(cmd, DeviceManager.getTransferQueue());
    }

    private void transitionImageLayout(VkCommandBuffer cmd, long image,
                                       int oldLayout, int newLayout, MemoryStack stack) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
        barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.oldLayout(oldLayout);
        barrier.newLayout(newLayout);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.image(image);
        barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        barrier.subresourceRange().baseMipLevel(0);
        barrier.subresourceRange().levelCount(1);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);

        int srcStage, dstStage;
        if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
            barrier.srcAccessMask(0);
            barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
        } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL &&
                   newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
        } else {
            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
        }

        vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0,
            null, null, barrier);
    }

    public void destroy() {
        unmap();
        allocation.free();
    }

    public long getBufferHandle() { return allocation.handle; }
    public long getCapacity() { return capacity; }
}
