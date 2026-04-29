package net.vulkadroid.vulkan.framebuffer;

import net.vulkadroid.Initializer;
import net.vulkadroid.vulkan.device.DeviceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class RenderPass {

    private static long renderPass = VK_NULL_HANDLE;
    private static long[] framebuffers;
    private static long depthImage = VK_NULL_HANDLE;
    private static long depthImageMemory = VK_NULL_HANDLE;
    private static long depthImageView = VK_NULL_HANDLE;

    public static void initialize() {
        createRenderPass();
        createDepthResources();
        createFramebuffers();
    }

    private static void createRenderPass() {
        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(2, stack);
            VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(SwapChain.getImageFormat());
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(findDepthFormat());
            depthAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentReference.Buffer colorRef = VkAttachmentReference.calloc(1, stack);
            colorRef.get(0).attachment(0);
            colorRef.get(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkAttachmentReference depthRef = VkAttachmentReference.calloc(stack);
            depthRef.attachment(1);
            depthRef.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorRef);
            subpass.pDepthStencilAttachment(depthRef);

            VkSubpassDependency.Buffer dependencies = VkSubpassDependency.calloc(2, stack);
            dependencies.get(0).srcSubpass(VK_SUBPASS_EXTERNAL);
            dependencies.get(0).dstSubpass(0);
            dependencies.get(0).srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT |
                VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependencies.get(0).srcAccessMask(0);
            dependencies.get(0).dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT |
                VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependencies.get(0).dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT |
                VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
            dependencies.get(1).srcSubpass(0);
            dependencies.get(1).dstSubpass(VK_SUBPASS_EXTERNAL);
            dependencies.get(1).srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependencies.get(1).srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            dependencies.get(1).dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT);
            dependencies.get(1).dstAccessMask(0);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);
            renderPassInfo.pDependencies(dependencies);

            LongBuffer pRenderPass = stack.mallocLong(1);
            int result = vkCreateRenderPass(DeviceManager.getDevice(), renderPassInfo, null, pRenderPass);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass. VkResult: " + result);
            }
            renderPass = pRenderPass.get(0);
        }
    }

    private static void createDepthResources() {
        int depthFormat = findDepthFormat();
        int w = SwapChain.getWidth();
        int h = SwapChain.getHeight();
        if (w <= 0 || h <= 0) return;

        try (MemoryStack stack = stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().set(w, h, 1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(depthFormat);
            imageInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pImage = stack.mallocLong(1);
            vkCreateImage(DeviceManager.getDevice(), imageInfo, null, pImage);
            depthImage = pImage.get(0);

            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(DeviceManager.getDevice(), depthImage, memReqs);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memReqs.size());
            allocInfo.memoryTypeIndex(net.vulkadroid.vulkan.memory.MemoryManager.findMemoryType(
                memReqs.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));

            LongBuffer pMemory = stack.mallocLong(1);
            vkAllocateMemory(DeviceManager.getDevice(), allocInfo, null, pMemory);
            depthImageMemory = pMemory.get(0);
            vkBindImageMemory(DeviceManager.getDevice(), depthImage, depthImageMemory, 0);

            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(depthImage);
            viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(depthFormat);
            viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(1);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);

            LongBuffer pView = stack.mallocLong(1);
            vkCreateImageView(DeviceManager.getDevice(), viewInfo, null, pView);
            depthImageView = pView.get(0);
        }
    }

    private static void createFramebuffers() {
        long[] imageViews = SwapChain.getImageViews();
        // Early return kalau SwapChain belum siap
        if (imageViews == null || imageViews.length == 0) {
            Initializer.LOGGER.warn("SwapChain image views not ready — skipping framebuffer creation");
            return;
        }
        framebuffers = new long[imageViews.length];
        try (MemoryStack stack = stackPush()) {
            for (int i = 0; i < imageViews.length; i++) {
                LongBuffer attachments = stack.longs(imageViews[i], depthImageView);
                VkFramebufferCreateInfo fbInfo = VkFramebufferCreateInfo.calloc(stack);
                fbInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
                fbInfo.renderPass(renderPass);
                fbInfo.pAttachments(attachments);
                fbInfo.width(SwapChain.getWidth());
                fbInfo.height(SwapChain.getHeight());
                fbInfo.layers(1);

                LongBuffer pFb = stack.mallocLong(1);
                int result = vkCreateFramebuffer(DeviceManager.getDevice(), fbInfo, null, pFb);
                if (result != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer " + i + ". VkResult: " + result);
                }
                framebuffers[i] = pFb.get(0);
            }
        }
    }

    public static void recreateFramebuffers() {
        destroyFramebuffers();
        createDepthResources();
        createFramebuffers();
    }

    private static void destroyFramebuffers() {
        if (framebuffers != null) {
            for (long fb : framebuffers) {
                if (fb != VK_NULL_HANDLE) vkDestroyFramebuffer(DeviceManager.getDevice(), fb, null);
            }
            framebuffers = null;
        }
        if (depthImageView != VK_NULL_HANDLE) {
            vkDestroyImageView(DeviceManager.getDevice(), depthImageView, null);
            depthImageView = VK_NULL_HANDLE;
        }
        if (depthImage != VK_NULL_HANDLE) {
            vkDestroyImage(DeviceManager.getDevice(), depthImage, null);
            depthImage = VK_NULL_HANDLE;
        }
        if (depthImageMemory != VK_NULL_HANDLE) {
            vkFreeMemory(DeviceManager.getDevice(), depthImageMemory, null);
            depthImageMemory = VK_NULL_HANDLE;
        }
    }

    public static int findDepthFormat() {
        int[] candidates = {
            VK_FORMAT_D32_SFLOAT,
            VK_FORMAT_D32_SFLOAT_S8_UINT,
            VK_FORMAT_D24_UNORM_S8_UINT
        };
        for (int format : candidates) {
            try (MemoryStack stack = stackPush()) {
                VkFormatProperties props = VkFormatProperties.malloc(stack);
                vkGetPhysicalDeviceFormatProperties(DeviceManager.getPhysicalDevice(), format, props);
                if ((props.optimalTilingFeatures() & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
                    return format;
                }
            }
        }
        throw new RuntimeException("No supported depth format found!");
    }

    public static void cleanup() {
        destroyFramebuffers();
        if (renderPass != VK_NULL_HANDLE) {
            vkDestroyRenderPass(DeviceManager.getDevice(), renderPass, null);
            renderPass = VK_NULL_HANDLE;
        }
    }

    public static long getHandle() { return renderPass; }
    public static long[] getFramebuffers() { return framebuffers; }
    public static long getFramebuffer(int index) { return framebuffers[index]; }
}