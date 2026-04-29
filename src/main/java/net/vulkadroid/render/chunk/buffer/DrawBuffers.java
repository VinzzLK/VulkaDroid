package net.vulkadroid.render.chunk.buffer;

import net.vulkadroid.render.Drawer;
import net.vulkadroid.vulkan.Renderer;
import net.vulkadroid.vulkan.device.DeviceManager;
import net.vulkadroid.vulkan.memory.Allocation;
import net.vulkadroid.vulkan.memory.MemoryManager;
import net.vulkadroid.vulkan.memory.StagingBuffer;
import net.vulkadroid.vulkan.queue.CommandPool;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class DrawBuffers {

    private Allocation vertexBuffer;
    private Allocation indexBuffer;
    private final long capacity;

    private long vertexWriteOffset = 0;
    private long indexWriteOffset  = 0;
    private int  pendingIndexCount = 0;

    private final ReentrantLock lock = new ReentrantLock();
    private CommandPool uploadPool;
    private boolean dirty = false;

    public DrawBuffers(long capacity) {
        this.capacity = capacity;
        allocate();
    }

    private void allocate() {
        vertexBuffer = MemoryManager.allocateBuffer(
            capacity,
            VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
        );
        indexBuffer = MemoryManager.allocateBuffer(
            capacity / 2,
            VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
        );
        uploadPool = new CommandPool(
            DeviceManager.getTransferQueueFamily(),
            VK_COMMAND_POOL_CREATE_TRANSIENT_BIT
        );
    }

    public long upload(ByteBuffer vertices, ByteBuffer indices) {
        lock.lock();
        try {
            long vSize = vertices.remaining();
            long iSize = indices.remaining();

            if (vertexWriteOffset + vSize > capacity ||
                indexWriteOffset  + iSize > capacity / 2) {
                reset(); // Ring buffer wrap-around
            }

            long vOffset = vertexWriteOffset;

            StagingBuffer vStaging = MemoryManager.createStagingBuffer(vSize);
            vStaging.map().put(vertices); vertices.rewind();
            vStaging.unmap();
            vStaging.copyToBuffer(vertexBuffer.handle, vSize, uploadPool);
            vStaging.destroy();

            StagingBuffer iStaging = MemoryManager.createStagingBuffer(iSize);
            iStaging.map().put(indices); indices.rewind();
            iStaging.unmap();
            iStaging.copyToBuffer(indexBuffer.handle, iSize, uploadPool);
            iStaging.destroy();

            vertexWriteOffset += vSize;
            indexWriteOffset  += iSize;
            pendingIndexCount += iSize / 4; // uint32 indices
            dirty = true;

            return vOffset;
        } finally {
            lock.unlock();
        }
    }

    public void flushAndDraw() {
        if (!dirty || pendingIndexCount == 0) return;
        if (!Renderer.isFrameStarted()) return;

        Drawer.drawIndexedDirect(
            vertexBuffer.handle,
            indexBuffer.handle,
            pendingIndexCount,
            1
        );
        dirty = false;
    }

    public void reset() {
        lock.lock();
        try {
            vertexWriteOffset = 0;
            indexWriteOffset  = 0;
            pendingIndexCount = 0;
            dirty = false;
        } finally {
            lock.unlock();
        }
    }

    public void destroy() {
        reset();
        if (uploadPool != null)   { uploadPool.destroy();   uploadPool = null; }
        if (vertexBuffer != null) { vertexBuffer.free();    vertexBuffer = null; }
        if (indexBuffer != null)  { indexBuffer.free();     indexBuffer = null; }
    }

    public long getCapacity()         { return capacity; }
    public long getVertexWriteOffset(){ return vertexWriteOffset; }
}
