package net.vulkadroid.gl;

import net.vulkadroid.vulkan.VRenderSystem;

/**
 * GL30 shim — framebuffer operations.
 */
public class GL30M {

    public static int glGenFramebuffers()               { return VkGlFramebuffer.genFramebuffer(); }
    public static void glBindFramebuffer(int tgt, int id){ VkGlFramebuffer.bindFramebuffer(id); }
    public static void glDeleteFramebuffers(int id)     { VkGlFramebuffer.deleteFramebuffer(id); }

    public static int glGenRenderbuffers()              { return 1; } // Stub — depth is Vulkan-managed
    public static void glBindRenderbuffer(int tgt, int id) {}
    public static void glRenderbufferStorage(int tgt, int fmt, int w, int h) {}
    public static void glFramebufferRenderbuffer(int fbtgt, int attachment, int rbtgt, int id) {}
    public static void glFramebufferTexture2D(int fbtgt, int attachment, int texTgt, int texId, int level) {}
    public static int glCheckFramebufferStatus(int target) { return 0x8CD5; } // GL_FRAMEBUFFER_COMPLETE

    public static int glGenVertexArrays()               { return 1; } // VAO stub
    public static void glBindVertexArray(int id)        {}
    public static void glDeleteVertexArrays(int id)     {}
}
