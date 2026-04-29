package net.vulkadroid.gl;

import net.vulkadroid.vulkan.framebuffer.RenderPass;
import net.vulkadroid.vulkan.framebuffer.SwapChain;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shim that maps GL framebuffer IDs to Vulkan framebuffer handles.
 */
public class VkGlFramebuffer {

    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private static final Map<Integer, Long> fbMap = new HashMap<>();
    private static int boundFb = 0;

    public static int genFramebuffer() {
        return idCounter.getAndIncrement();
    }

    public static void bindFramebuffer(int id) {
        boundFb = id;
        // In Vulkan, framebuffer binding is implicit from render pass — no action needed
    }

    public static void deleteFramebuffer(int id) {
        fbMap.remove(id);
    }

    public static int getBoundFramebuffer() {
        return boundFb;
    }

    public static long getVulkanFramebuffer(int id, int imageIndex) {
        // Always return the current swapchain framebuffer
        long[] fbs = RenderPass.getFramebuffers();
        if (fbs != null && imageIndex >= 0 && imageIndex < fbs.length) {
            return fbs[imageIndex];
        }
        return 0L;
    }
}
