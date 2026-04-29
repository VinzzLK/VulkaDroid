package net.vulkadroid.gl;

import net.vulkadroid.vulkan.VRenderSystem;

/**
 * Intercepts GL11 calls that slip through Mixin coverage.
 */
public class GL11M {

    public static void glEnable(int cap)  { dispatchEnable(cap, true); }
    public static void glDisable(int cap) { dispatchEnable(cap, false); }

    private static void dispatchEnable(int cap, boolean enable) {
        switch (cap) {
            case 0x0B44 -> { if (enable) VRenderSystem.enableDepthTest();    else VRenderSystem.disableDepthTest(); }    // GL_DEPTH_TEST
            case 0x0BE2 -> { if (enable) VRenderSystem.enableBlend();        else VRenderSystem.disableBlend(); }        // GL_BLEND
            case 0x0B44+1 -> { /* GL_STENCIL_TEST */ }
            case 0x0B41 -> { if (enable) VRenderSystem.enableCull();         else VRenderSystem.disableCull(); }         // GL_CULL_FACE
            case 0x8037 -> { if (enable) VRenderSystem.enablePolygonOffset(); else VRenderSystem.disablePolygonOffset(); } // GL_POLYGON_OFFSET_FILL
            case 0x0C11 -> { if (enable) VRenderSystem.enableScissorTest();  else VRenderSystem.disableScissorTest(); }  // GL_SCISSOR_TEST
            case 0x0BF2 -> { if (enable) VRenderSystem.enableColorLogicOp(); else VRenderSystem.disableColorLogicOp(); } // GL_COLOR_LOGIC_OP
            default -> {} // Unknown cap — ignore
        }
    }

    public static void glDepthFunc(int func)                 { VRenderSystem.depthFunc(func); }
    public static void glDepthMask(boolean flag)             { VRenderSystem.depthMask(flag); }
    public static void glBlendFunc(int src, int dst)         { VRenderSystem.blendFunc(src, dst); }
    public static void glCullFace(int face)                  { VRenderSystem.cullFace(face); }
    public static void glPolygonOffset(float f, float u)     { VRenderSystem.polygonOffset(f, u); }
    public static void glScissor(int x, int y, int w, int h) { VRenderSystem.scissorBox(x, y, w, h); }
    public static void glViewport(int x, int y, int w, int h){ VRenderSystem.viewport(x, y, w, h); }
    public static void glColorMask(boolean r, boolean g, boolean b, boolean a) { VRenderSystem.colorMask(r, g, b, a); }
    public static void glLogicOp(int op)                     { VRenderSystem.logicOp(op); }
    public static void glClear(int mask)                     { VRenderSystem.clear(mask, false); }
    public static void glClearColor(float r, float g, float b, float a) { VRenderSystem.clearColor(r, g, b, a); }
    public static void glClearDepth(double d)                { VRenderSystem.clearDepth(d); }
}
