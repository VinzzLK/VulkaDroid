package net.vulkadroid.vulkan;

import net.vulkadroid.Initializer;
import net.vulkadroid.render.PipelineManager;
import net.vulkadroid.vulkan.device.DeviceManager;
import net.vulkadroid.vulkan.framebuffer.RenderPass;
import net.vulkadroid.vulkan.framebuffer.SwapChain;
import net.vulkadroid.vulkan.queue.CommandPool;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Renderer {

    private static CommandPool graphicsCommandPool;
    private static VkCommandBuffer[] commandBuffers;
    private static int currentImageIndex = -1;
    private static boolean frameStarted = false;
    private static boolean swapChainOutOfDate = false;

    // Clear values: sky blue for color, 1.0 for depth
    private static float CLEAR_R = 0.53f;
    private static float CLEAR_G = 0.81f;
    private static float CLEAR_B = 0.98f; private static float CLEAR_A = 1.0f; private static float CLEAR_DEPTH = 1.0f;

    public static void initialize() {
        graphicsCommandPool = new CommandPool(
            DeviceManager.getGraphicsQueueFamily(),
            VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT
        );
        createCommandBuffers();
    }

    private static void createCommandBuffers() {
        commandBuffers = graphicsCommandPool.allocate(
            VK_COMMAND_BUFFER_LEVEL_PRIMARY,
            SwapChain.getImageCount()
        );
    }

    public static boolean beginFrame() {
        if (SwapChain.getHandle() == VK_NULL_HANDLE) return false;

        Synchronization.waitForCurrentFrame();

        try (MemoryStack stack = stackPush()) {
            IntBuffer pImageIndex = stack.mallocInt(1);
            int result = vkAcquireNextImageKHR(
                DeviceManager.getDevice(),
                SwapChain.getHandle(),
                Long.MAX_VALUE,
                Synchronization.getCurrentImageAvailableSemaphore(),
                VK_NULL_HANDLE,
                pImageIndex
            );

            if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                swapChainOutOfDate = true;
                handleResize();
                return false;
            } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
                Initializer.LOGGER.error("Failed to acquire swap chain image. VkResult: {}", result);
                return false;
            }

            currentImageIndex = pImageIndex.get(0);
            Synchronization.waitForImageFence(currentImageIndex);
            Synchronization.resetCurrentFence();

            // Begin command buffer
            VkCommandBuffer cmd = commandBuffers[currentImageIndex];
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            vkBeginCommandBuffer(cmd, beginInfo);

            // Begin render pass
            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(RenderPass.getHandle());
            renderPassInfo.framebuffer(RenderPass.getFramebuffer(currentImageIndex));
            renderPassInfo.renderArea().offset().set(0, 0);
            renderPassInfo.renderArea().extent().set(SwapChain.getWidth(), SwapChain.getHeight());

            VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
            clearValues.get(0).color().float32(0, CLEAR_R).float32(1, CLEAR_G).float32(2, CLEAR_B).float32(3, 1.0f);
            clearValues.get(1).depthStencil().set(1.0f, 0);
            renderPassInfo.pClearValues(clearValues);

            vkCmdBeginRenderPass(cmd, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            // Set dynamic viewport and scissor
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0).y(0)
                .width(SwapChain.getWidth()).height(SwapChain.getHeight())
                .minDepth(0.0f).maxDepth(1.0f);
            vkCmdSetViewport(cmd, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(0, 0);
            scissor.extent().set(SwapChain.getWidth(), SwapChain.getHeight());
            vkCmdSetScissor(cmd, 0, scissor);

            frameStarted = true;
            return true;
        }
    }

    public static void endFrame() {
        if (!frameStarted) return;

        VkCommandBuffer cmd = commandBuffers[currentImageIndex];
        vkCmdEndRenderPass(cmd);
        int result = vkEndCommandBuffer(cmd);
        if (result != VK_SUCCESS) {
            Initializer.LOGGER.error("Failed to record command buffer. VkResult: {}", result);
        }

        try (MemoryStack stack = stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stack.longs(Synchronization.getCurrentImageAvailableSemaphore()));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(cmd));
            submitInfo.pSignalSemaphores(stack.longs(Synchronization.getCurrentRenderFinishedSemaphore()));

            result = vkQueueSubmit(DeviceManager.getGraphicsQueue(), submitInfo,
                Synchronization.getCurrentInFlightFence());
            if (result != VK_SUCCESS) {
                Initializer.LOGGER.error("Failed to submit draw command buffer. VkResult: {}", result);
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(stack.longs(Synchronization.getCurrentRenderFinishedSemaphore()));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(SwapChain.getHandle()));
            presentInfo.pImageIndices(stack.ints(currentImageIndex));

            result = vkQueuePresentKHR(DeviceManager.getPresentQueue(), presentInfo);
            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || swapChainOutOfDate) {
                swapChainOutOfDate = false;
                handleResize();
            } else if (result != VK_SUCCESS) {
                Initializer.LOGGER.error("Failed to present swap chain image. VkResult: {}", result);
            }
        }

        Synchronization.advance();
        frameStarted = false;
        currentImageIndex = -1;
    }

    public static void handleResize() {
        DeviceManager.waitIdle();
        SwapChain.recreate();
        PipelineManager.onResize();
        swapChainOutOfDate = false;
        Initializer.LOGGER.info("Swapchain recreated: {}x{}", SwapChain.getWidth(), SwapChain.getHeight());
    }

    public static void cleanup() {
        DeviceManager.waitIdle();
        if (graphicsCommandPool != null) {
            graphicsCommandPool.destroy();
            graphicsCommandPool = null;
        }
    }

    public static VkCommandBuffer getCurrentCommandBuffer() {
        return commandBuffers != null && currentImageIndex >= 0 ? commandBuffers[currentImageIndex] : null;
    }

    public static int getCurrentImageIndex() { return currentImageIndex; }
    public static void setClearColor(float r, float g, float b, float a) { CLEAR_R=r; CLEAR_G=g; CLEAR_B=b; CLEAR_A=a; }
    public static void setClearDepth(float d) { CLEAR_DEPTH=d; }
    public static boolean isFrameStarted() { return frameStarted; }
    public static CommandPool getGraphicsCommandPool() { return graphicsCommandPool; }
}
