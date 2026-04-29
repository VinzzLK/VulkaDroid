package net.vulkadroid.vulkan.framebuffer;

import net.vulkadroid.Initializer;
import net.vulkadroid.config.Config;
import net.vulkadroid.vulkan.Vulkan;
import net.vulkadroid.vulkan.device.DeviceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class SwapChain {

    private static long swapChain = VK_NULL_HANDLE;
    private static long[] images;
    private static long[] imageViews;
    private static int imageFormat;
    private static VkExtent2D extent;
    private static int imageCount;

    public static void initialize() {
        if (Vulkan.getSurface() == VK_NULL_HANDLE) {
            Initializer.LOGGER.warn("No surface available, skipping SwapChain creation");
            return;
        }
        create();
    }

    public static void create() {
        try (MemoryStack stack = stackPush()) {
            long surface = Vulkan.getSurface();
            VkPhysicalDevice physDevice = DeviceManager.getPhysicalDevice();

            // Query surface capabilities
            VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physDevice, surface, capabilities);

            // Choose surface format
            VkSurfaceFormatKHR surfaceFormat = chooseSurfaceFormat(physDevice, surface, stack);
            imageFormat = surfaceFormat.format();

            // Choose present mode
            int presentMode = choosePresentMode(physDevice, surface, stack);

            // Choose extent
            extent = chooseExtent(capabilities, stack);

            // Image count (triple buffering for mobile, or 2 minimum)
            int minImages = capabilities.minImageCount();
            int maxImages = capabilities.maxImageCount();
            imageCount = Math.min(Math.max(3, minImages), maxImages == 0 ? Integer.MAX_VALUE : maxImages);

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surface);
            createInfo.minImageCount(imageCount);
            createInfo.imageFormat(imageFormat);
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT);
            createInfo.preTransform(capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);
            createInfo.oldSwapchain(VK_NULL_HANDLE);

            int graphicsFamily = DeviceManager.getGraphicsQueueFamily();
            int presentFamily  = DeviceManager.getPresentQueueFamily();
            if (graphicsFamily != presentFamily) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                IntBuffer families = stack.ints(graphicsFamily, presentFamily);
                createInfo.pQueueFamilyIndices(families);
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            LongBuffer pSwapChain = stack.mallocLong(1);
            int result = vkCreateSwapchainKHR(DeviceManager.getDevice(), createInfo, null, pSwapChain);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain. VkResult: " + result);
            }
            swapChain = pSwapChain.get(0);

            // Retrieve swap chain images
            IntBuffer pCount = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(DeviceManager.getDevice(), swapChain, pCount, null);
            imageCount = pCount.get(0);
            LongBuffer pImages = stack.mallocLong(imageCount);
            vkGetSwapchainImagesKHR(DeviceManager.getDevice(), swapChain, pCount, pImages);

            images = new long[imageCount];
            for (int i = 0; i < imageCount; i++) images[i] = pImages.get(i);

            createImageViews();
            Initializer.LOGGER.info("SwapChain created: {}x{}, {} images, format {}",
                extent.width(), extent.height(), imageCount, imageFormat);
        }
    }

    private static void createImageViews() {
        imageViews = new long[imageCount];
        try (MemoryStack stack = stackPush()) {
            for (int i = 0; i < imageCount; i++) {
                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
                viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                viewInfo.image(images[i]);
                viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
                viewInfo.format(imageFormat);
                viewInfo.components()
                    .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);
                viewInfo.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);

                LongBuffer pView = stack.mallocLong(1);
                int result = vkCreateImageView(DeviceManager.getDevice(), viewInfo, null, pView);
                if (result != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create image view " + i + ". VkResult: " + result);
                }
                imageViews[i] = pView.get(0);
            }
        }
    }

    private static VkSurfaceFormatKHR chooseSurfaceFormat(VkPhysicalDevice physDevice,
                                                           long surface, MemoryStack stack) {
        IntBuffer pCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physDevice, surface, pCount, null);
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.malloc(pCount.get(0), stack);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physDevice, surface, pCount, formats);

        // Prefer SRGB
        for (VkSurfaceFormatKHR format : formats) {
            if (format.format() == VK_FORMAT_B8G8R8A8_SRGB &&
                format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                // Untuk LWJGL 3.x, cukup return format langsung
                return format;
            }
        }
        
        // Fallback to first available format
        return formats.get(0);
    }

    private static int choosePresentMode(VkPhysicalDevice physDevice, long surface, MemoryStack stack) {
        IntBuffer pCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfacePresentModesKHR(physDevice, surface, pCount, null);
        IntBuffer modes = stack.mallocInt(pCount.get(0));
        vkGetPhysicalDeviceSurfacePresentModesKHR(physDevice, surface, pCount, modes);

        boolean vsync = Config.isVsyncEnabled();
        
        if (!vsync) {
            // Prefer mailbox (triple buffer, low latency)
            for (int i = 0; i < pCount.get(0); i++) {
                if (modes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                    return VK_PRESENT_MODE_MAILBOX_KHR;
                }
            }
            // Then immediate mode
            for (int i = 0; i < pCount.get(0); i++) {
                if (modes.get(i) == VK_PRESENT_MODE_IMMEDIATE_KHR) {
                    return VK_PRESENT_MODE_IMMEDIATE_KHR;
                }
            }
        }
        
        // FIFO (vsync) is always available
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    private static VkExtent2D chooseExtent(VkSurfaceCapabilitiesKHR capabilities, MemoryStack stack) {
        if (capabilities.currentExtent().width() != 0xFFFFFFFF) {
            VkExtent2D result = VkExtent2D.malloc(stack);
            result.set(capabilities.currentExtent());
            return result;
        }
        
        int width = Math.max(capabilities.minImageExtent().width(),
                    Math.min(capabilities.maxImageExtent().width(), Config.getWindowWidth()));
        int height = Math.max(capabilities.minImageExtent().height(),
                    Math.min(capabilities.maxImageExtent().height(), Config.getWindowHeight()));
        
        VkExtent2D result = VkExtent2D.malloc(stack);
        result.set(width, height);
        return result;
    }

    public static void recreate() {
        DeviceManager.waitIdle();
        cleanup();
        create();
        RenderPass.recreateFramebuffers();
    }

    public static void cleanup() {
        if (imageViews != null) {
            for (long view : imageViews) {
                if (view != VK_NULL_HANDLE) {
                    vkDestroyImageView(DeviceManager.getDevice(), view, null);
                }
            }
            imageViews = null;
        }
        if (swapChain != VK_NULL_HANDLE) {
            vkDestroySwapchainKHR(DeviceManager.getDevice(), swapChain, null);
            swapChain = VK_NULL_HANDLE;
        }
    }

    // Getters
    public static long getHandle() { 
        return swapChain; 
    }
    
    public static long[] getImages() { 
        return images; 
    }
    
    public static long[] getImageViews() { 
        return imageViews; 
    }
    
    public static int getImageFormat() { 
        return imageFormat; 
    }
    
    public static int getWidth() { 
        return extent != null ? extent.width() : 0; 
    }
    
    public static int getHeight() { 
        return extent != null ? extent.height() : 0; 
    }
    
    public static int getImageCount() { 
        return imageCount; 
    }
    
    public static VkExtent2D getExtent() { 
        return extent; 
    }
}