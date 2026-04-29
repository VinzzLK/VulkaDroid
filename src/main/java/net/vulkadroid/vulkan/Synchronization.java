package net.vulkadroid.vulkan;

import net.vulkadroid.vulkan.device.DeviceManager;
import net.vulkadroid.vulkan.framebuffer.SwapChain;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Synchronization {

    // 3 frames in flight for mobile (enough to keep GPU busy without too much latency)
    public static final int MAX_FRAMES_IN_FLIGHT = 3;

    private static long[] imageAvailableSemaphores;
    private static long[] renderFinishedSemaphores;
    private static long[] inFlightFences;
    private static long[] imagesInFlight;

    private static int currentFrame = 0;

    public static void initialize() {
        int imageCount = SwapChain.getImageCount();
        imageAvailableSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
        renderFinishedSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
        inFlightFences           = new long[MAX_FRAMES_IN_FLIGHT];
        imagesInFlight           = new long[imageCount];

        try (MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo semInfo = VkSemaphoreCreateInfo.calloc(stack);
            semInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT); // Start signaled so first frame doesn't wait

            LongBuffer pHandle = stack.mallocLong(1);
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                if (vkCreateSemaphore(DeviceManager.getDevice(), semInfo, null, pHandle) != VK_SUCCESS ||
                    (imageAvailableSemaphores[i] = pHandle.get(0)) == VK_NULL_HANDLE) {
                    throw new RuntimeException("Failed to create imageAvailable semaphore " + i);
                }
                if (vkCreateSemaphore(DeviceManager.getDevice(), semInfo, null, pHandle) != VK_SUCCESS ||
                    (renderFinishedSemaphores[i] = pHandle.get(0)) == VK_NULL_HANDLE) {
                    throw new RuntimeException("Failed to create renderFinished semaphore " + i);
                }
                if (vkCreateFence(DeviceManager.getDevice(), fenceInfo, null, pHandle) != VK_SUCCESS ||
                    (inFlightFences[i] = pHandle.get(0)) == VK_NULL_HANDLE) {
                    throw new RuntimeException("Failed to create inFlight fence " + i);
                }
            }
        }
        // All images start with no fence assigned
        for (int i = 0; i < imageCount; i++) imagesInFlight[i] = VK_NULL_HANDLE;
    }

    public static void waitForCurrentFrame() {
        vkWaitForFences(DeviceManager.getDevice(), inFlightFences[currentFrame], true, Long.MAX_VALUE);
    }

    public static void resetCurrentFence() {
        vkResetFences(DeviceManager.getDevice(), inFlightFences[currentFrame]);
    }

    public static void waitForImageFence(int imageIndex) {
        if (imagesInFlight[imageIndex] != VK_NULL_HANDLE) {
            vkWaitForFences(DeviceManager.getDevice(), imagesInFlight[imageIndex], true, Long.MAX_VALUE);
        }
        imagesInFlight[imageIndex] = inFlightFences[currentFrame];
    }

    public static void advance() {
        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
    }

    public static void cleanup() {
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            if (imageAvailableSemaphores != null && imageAvailableSemaphores[i] != VK_NULL_HANDLE)
                vkDestroySemaphore(DeviceManager.getDevice(), imageAvailableSemaphores[i], null);
            if (renderFinishedSemaphores != null && renderFinishedSemaphores[i] != VK_NULL_HANDLE)
                vkDestroySemaphore(DeviceManager.getDevice(), renderFinishedSemaphores[i], null);
            if (inFlightFences != null && inFlightFences[i] != VK_NULL_HANDLE)
                vkDestroyFence(DeviceManager.getDevice(), inFlightFences[i], null);
        }
    }

    public static long getCurrentImageAvailableSemaphore() { return imageAvailableSemaphores[currentFrame]; }
    public static long getCurrentRenderFinishedSemaphore() { return renderFinishedSemaphores[currentFrame]; }
    public static long getCurrentInFlightFence() { return inFlightFences[currentFrame]; }
    public static int getCurrentFrame() { return currentFrame; }
}
