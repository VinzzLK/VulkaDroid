package net.vulkadroid.render.chunk;

import net.minecraft.world.phys.AABB;
import net.minecraft.client.renderer.culling.Frustum;
import java.util.concurrent.atomic.AtomicBoolean;

public class RenderSection {

    public final int chunkX, chunkY, chunkZ;
    private final AtomicBoolean dirty     = new AtomicBoolean(true);
    private final AtomicBoolean compiling = new AtomicBoolean(false);
    private boolean empty = false;

    private long solidVertexOffset = -1, solidIndexOffset = -1;
    private int  solidIndexCount   = 0;
    private long transVertexOffset = -1, transIndexOffset = -1;
    private int  transIndexCount   = 0;

    public final float minX, minY, minZ, maxX, maxY, maxZ;

    public RenderSection(int cx, int cy, int cz) {
        this.chunkX = cx; this.chunkY = cy; this.chunkZ = cz;
        this.minX = cx * 16f; this.minY = cy * 16f; this.minZ = cz * 16f;
        this.maxX = minX + 16f; this.maxY = minY + 16f; this.maxZ = minZ + 16f;
    }

    public boolean isDirty()     { return dirty.get(); }
    public void markDirty()      { dirty.set(true); }
    public boolean isCompiling() { return compiling.get(); }
    public boolean isEmpty()     { return empty; }

    public boolean tryStartCompile() { return !compiling.getAndSet(true); }

    public void finishCompile(long svOff, long siOff, int siCount,
                               long tvOff, long tiOff, int tiCount, boolean wasEmpty) {
        solidVertexOffset = svOff; solidIndexOffset = siOff; solidIndexCount = siCount;
        transVertexOffset = tvOff; transIndexOffset = tiOff; transIndexCount = tiCount;
        empty = wasEmpty;
        dirty.set(false);
        compiling.set(false);
    }

    public boolean isInFrustum(Frustum frustum) {
        if (frustum == null) return true;
        return frustum.isVisible(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    public long getSolidVertexOffset()  { return solidVertexOffset; }
    public long getSolidIndexOffset()   { return solidIndexOffset; }
    public int  getSolidIndexCount()    { return solidIndexCount; }
    public long getTransVertexOffset()  { return transVertexOffset; }
    public long getTransIndexOffset()   { return transIndexOffset; }
    public int  getTransIndexCount()    { return transIndexCount; }
}
