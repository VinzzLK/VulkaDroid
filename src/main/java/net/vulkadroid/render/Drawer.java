package net.vulkadroid.render;

import net.vulkadroid.vulkan.Renderer;
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

public class Drawer {

    private static final int IMMEDIATE_BUFFER_SIZE = 8 * 1024 * 1024; // 8 MB for immediate mode quads

    private static Allocation vertexBuffer;
    private static Allocation indexBuffer;
    private static ByteBuffer mappedVertexBuffer;
    private static ByteBuffer mappedIndexBuffer;
    private static int vertexCount = 0;
    private static int indexCount  = 0;

    private static CommandPool uploadPool;

    public static void initialize() {
        uploadPool = new CommandPool(
            DeviceManager.getTransferQueueFamily(),
            VK_COMMAND_POOL_CREATE_TRANSIENT_BIT
        );

        vertexBuffer = MemoryManager.allocateBuffer(
            IMMEDIATE_BUFFER_SIZE,
            VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        );

        indexBuffer = MemoryManager.allocateBuffer(
            IMMEDIATE_BUFFER_SIZE / 2,
            VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        );

        mappedVertexBuffer = vertexBuffer.map();
        mappedIndexBuffer  = indexBuffer.map();
    }

    public static void uploadAndDraw(ByteBuffer vertices, ByteBuffer indices,
                                     int vertCount, int idxCount) {
        if (vertexBuffer == null) return;
        VkCommandBuffer cmd = Renderer.getCurrentCommandBuffer();
        if (cmd == null) return;

        // Upload vertex data
        mappedVertexBuffer.position(0);
        mappedVertexBuffer.put(vertices);
        vertices.rewind();

        // Upload index data
        mappedIndexBuffer.position(0);
        mappedIndexBuffer.put(indices);
        indices.rewind();

        // Bind and draw
        try (MemoryStack stack = stackPush()) {
            LongBuffer pBuffers = stack.longs(vertexBuffer.handle);
            LongBuffer pOffsets = stack.longs(0);
            vkCmdBindVertexBuffers(cmd, 0, pBuffers, pOffsets);
            vkCmdBindIndexBuffer(cmd, indexBuffer.handle, 0, VK_INDEX_TYPE_UINT32);
        }
        vkCmdDrawIndexed(cmd, idxCount, 1, 0, 0, 0);
    }

    public static void drawDirect(long vkBuffer, long offset, int vertCount) {
        VkCommandBuffer cmd = Renderer.getCurrentCommandBuffer();
        if (cmd == null) return;
        try (MemoryStack stack = stackPush()) {
            vkCmdBindVertexBuffers(cmd, 0, stack.longs(vkBuffer), stack.longs(offset));
        }
        vkCmdDraw(cmd, vertCount, 1, 0, 0);
    }

    public static void drawIndexedDirect(long vkVertexBuffer, long vkIndexBuffer,
                                          int indexCount, int instanceCount) {
        VkCommandBuffer cmd = Renderer.getCurrentCommandBuffer();
        if (cmd == null) return;
        try (MemoryStack stack = stackPush()) {
            vkCmdBindVertexBuffers(cmd, 0, stack.longs(vkVertexBuffer), stack.longs(0L));
            vkCmdBindIndexBuffer(cmd, vkIndexBuffer, 0, VK_INDEX_TYPE_UINT32);
        }
        vkCmdDrawIndexed(cmd, indexCount, instanceCount, 0, 0, 0);
    }

    public static void drawIndirect(long indirectBuffer, long offset, int drawCount, int stride) {
        VkCommandBuffer cmd = Renderer.getCurrentCommandBuffer();
        if (cmd == null) return;
        vkCmdDrawIndirect(cmd, indirectBuffer, offset, drawCount, stride);
    }

    public static void drawIndexedIndirect(long indirectBuffer, long offset, int drawCount, int stride) {
        VkCommandBuffer cmd = Renderer.getCurrentCommandBuffer();
        if (cmd == null) return;
        vkCmdDrawIndexedIndirect(cmd, indirectBuffer, offset, drawCount, stride);
    }

    public static void cleanup() {
        if (mappedVertexBuffer != null) { vertexBuffer.unmap(); mappedVertexBuffer = null; }
        if (mappedIndexBuffer  != null) { indexBuffer.unmap();  mappedIndexBuffer  = null; }
        if (vertexBuffer != null) { vertexBuffer.free(); vertexBuffer = null; }
        if (indexBuffer  != null) { indexBuffer.free();  indexBuffer  = null; }
        if (uploadPool != null)   { uploadPool.destroy(); uploadPool = null; }
    }

    public static Allocation getVertexBuffer() { return vertexBuffer; }
    public static CommandPool getUploadPool()   { return uploadPool; }
}
