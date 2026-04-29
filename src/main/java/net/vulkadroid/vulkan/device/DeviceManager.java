package net.vulkadroid.vulkan.device;

import net.vulkadroid.Initializer;
import net.vulkadroid.vulkan.Vulkan;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.*;

public class DeviceManager {

    private static VkPhysicalDevice physicalDevice;
    private static VkDevice device;
    private static String deviceName;
    private static VkPhysicalDeviceProperties deviceProperties;
    private static VkPhysicalDeviceFeatures deviceFeatures;
    private static VkPhysicalDeviceMemoryProperties memoryProperties;

    public static final int GRAPHICS_QUEUE_FAMILY = 0;
    public static final int PRESENT_QUEUE_FAMILY  = 1;
    public static final int TRANSFER_QUEUE_FAMILY = 2;

    private static int graphicsQueueFamily = -1;
    private static int presentQueueFamily  = -1;
    private static int transferQueueFamily = -1;

    private static VkQueue graphicsQueue;
    private static VkQueue presentQueue;
    private static VkQueue transferQueue;

    private static final String[] REQUIRED_DEVICE_EXTENSIONS = {
        VK_KHR_SWAPCHAIN_EXTENSION_NAME,
        "VK_KHR_maintenance1",
        "VK_KHR_storage_buffer_storage_class"
    };

    private static final String[] OPTIONAL_DEVICE_EXTENSIONS = {
        "VK_EXT_memory_budget",
        "VK_KHR_draw_indirect_count",
        "VK_EXT_extended_dynamic_state",
        "VK_EXT_pipeline_creation_cache_control",
        "VK_KHR_shader_float16_int8"
    };

    private static Set<String> enabledExtensions = new HashSet<>();

    public static void initialize(VkInstance instance) {
        pickPhysicalDevice(instance);
        createLogicalDevice();
        retrieveQueues();
    }

    private static void pickPhysicalDevice(VkInstance instance) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pDeviceCount = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, pDeviceCount, null);

            if (pDeviceCount.get(0) == 0) {
                throw new RuntimeException("No Vulkan-capable GPUs found!");
            }

            PointerBuffer ppDevices = stack.mallocPointer(pDeviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, pDeviceCount, ppDevices);

            // Score and pick best device
            VkPhysicalDevice best = null;
            int bestScore = -1;

            for (int i = 0; i < pDeviceCount.get(0); i++) {
                VkPhysicalDevice candidate = new VkPhysicalDevice(ppDevices.get(i), instance);
                int score = scoreDevice(candidate, stack);
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }

            if (best == null) {
                throw new RuntimeException("No suitable GPU found!");
            }

            physicalDevice = best;

            // Cache device properties
            deviceProperties = VkPhysicalDeviceProperties.malloc();
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);
            deviceName = deviceProperties.deviceNameString();

            deviceFeatures = VkPhysicalDeviceFeatures.malloc();
            vkGetPhysicalDeviceFeatures(physicalDevice, deviceFeatures);

            memoryProperties = VkPhysicalDeviceMemoryProperties.malloc();
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);

            Initializer.LOGGER.info("Selected GPU: {} (score: {})", deviceName, bestScore);
        }
    }

    private static int scoreDevice(VkPhysicalDevice device, MemoryStack stack) {
        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(device, props);

        // Must have required extensions
        if (!hasRequiredExtensions(device, stack)) return -1;

        // Must have required queue families
        QueueFamilyIndices indices = findQueueFamilies(device, stack);
        if (!indices.isComplete()) return -1;

        int score = 0;

        // Prefer discrete GPU
        if (props.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) score += 1000;
        if (props.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) score += 500;

        // VRAM
        VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(device, memProps);
        for (int i = 0; i < memProps.memoryHeapCount(); i++) {
            if ((memProps.memoryHeaps(i).flags() & VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) {
                score += (int)(memProps.memoryHeaps(i).size() / (1024 * 1024)); // Add MB count
            }
        }

        return score;
    }

    private static boolean hasRequiredExtensions(VkPhysicalDevice device, MemoryStack stack) {
        IntBuffer pCount = stack.mallocInt(1);
        vkEnumerateDeviceExtensionProperties(device, (String) null, pCount, null);
        VkExtensionProperties.Buffer available = VkExtensionProperties.malloc(pCount.get(0), stack);
        vkEnumerateDeviceExtensionProperties(device, (String) null, pCount, available);

        Set<String> availableNames = new HashSet<>();
        for (VkExtensionProperties ext : available) {
            availableNames.add(ext.extensionNameString());
        }

        for (String required : REQUIRED_DEVICE_EXTENSIONS) {
            if (!availableNames.contains(required)) return false;
        }
        return true;
    }

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, MemoryStack stack) {
        QueueFamilyIndices indices = new QueueFamilyIndices();
        IntBuffer pCount = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(device, pCount, null);
        VkQueueFamilyProperties.Buffer families = VkQueueFamilyProperties.malloc(pCount.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(device, pCount, families);

        int i = 0;
        for (VkQueueFamilyProperties family : families) {
            if ((family.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                indices.graphicsFamily = i;
            }
            // Transfer-only queue (for async uploads)
            if ((family.queueFlags() & VK_QUEUE_TRANSFER_BIT) != 0 &&
                (family.queueFlags() & VK_QUEUE_GRAPHICS_BIT) == 0) {
                indices.transferFamily = i;
            }
            long surface = Vulkan.getSurface();
            if (surface != VK_NULL_HANDLE) {
                IntBuffer presentSupport = stack.mallocInt(1);
                KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);
                if (presentSupport.get(0) == VK_TRUE) {
                    indices.presentFamily = i;
                }
            } else {
                indices.presentFamily = indices.graphicsFamily;
            }
            if (indices.isComplete()) break;
            i++;
        }
        // If no dedicated transfer, use graphics
        if (indices.transferFamily == null) {
            indices.transferFamily = indices.graphicsFamily;
        }
        return indices;
    }

    private static void createLogicalDevice() {
        try (MemoryStack stack = stackPush()) {
            QueueFamilyIndices indices = findQueueFamilies(physicalDevice, stack);
            graphicsQueueFamily = indices.graphicsFamily;
            presentQueueFamily  = indices.presentFamily;
            transferQueueFamily = indices.transferFamily;

            Set<Integer> uniqueQueueFamilies = new HashSet<>(Arrays.asList(
                graphicsQueueFamily, presentQueueFamily, transferQueueFamily
            ));

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos =
                VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.size(), stack);

            int idx = 0;
            for (int queueFamily : uniqueQueueFamilies) {
                VkDeviceQueueCreateInfo queueInfo = queueCreateInfos.get(idx++);
                queueInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueInfo.queueFamilyIndex(queueFamily);
                queueInfo.pQueuePriorities(stack.floats(1.0f));
            }

            // Enable features
            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(stack);
            if (deviceFeatures.samplerAnisotropy())  features.samplerAnisotropy(true);
            if (deviceFeatures.multiDrawIndirect())  features.multiDrawIndirect(true);
            if (deviceFeatures.drawIndirectFirstInstance()) features.drawIndirectFirstInstance(true);
            if (deviceFeatures.fillModeNonSolid())   features.fillModeNonSolid(true);
            if (deviceFeatures.wideLines())          features.wideLines(true);
            if (deviceFeatures.depthClamp())         features.depthClamp(true);

            // Vulkan 1.2 features
            VkPhysicalDeviceVulkan12Features features12 = VkPhysicalDeviceVulkan12Features.calloc(stack);
            features12.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
            features12.drawIndirectCount(true);
            features12.descriptorIndexing(true);
            features12.bufferDeviceAddress(true);

            // Collect extensions
            List<String> extensions = new ArrayList<>(Arrays.asList(REQUIRED_DEVICE_EXTENSIONS));
            try (MemoryStack innerStack = stackPush()) {
                IntBuffer pCount = innerStack.mallocInt(1);
                vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, pCount, null);
                VkExtensionProperties.Buffer available = VkExtensionProperties.malloc(pCount.get(0), innerStack);
                vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, pCount, available);
                Set<String> availableNames = new HashSet<>();
                for (VkExtensionProperties e : available) availableNames.add(e.extensionNameString());
                for (String opt : OPTIONAL_DEVICE_EXTENSIONS) {
                    if (availableNames.contains(opt)) extensions.add(opt);
                }
            }
            enabledExtensions.addAll(extensions);

            PointerBuffer pExtensions = stack.mallocPointer(extensions.size());
            for (String ext : extensions) pExtensions.put(stack.UTF8(ext));
            pExtensions.flip();

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pNext(features12.address());
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(features);
            createInfo.ppEnabledExtensionNames(pExtensions);

            PointerBuffer pDevice = stack.mallocPointer(1);
            int result = vkCreateDevice(physicalDevice, createInfo, null, pDevice);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device. VkResult: " + result);
            }
            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);
        }
    }

    private static void retrieveQueues() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);

            vkGetDeviceQueue(device, graphicsQueueFamily, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);

            vkGetDeviceQueue(device, presentQueueFamily, 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), device);

            vkGetDeviceQueue(device, transferQueueFamily, 0, pQueue);
            transferQueue = new VkQueue(pQueue.get(0), device);
        }
    }

    public static void cleanup() {
        if (device != null) {
            vkDestroyDevice(device, null);
            device = null;
        }
        if (deviceProperties != null) { deviceProperties.free(); deviceProperties = null; }
        if (deviceFeatures != null)    { deviceFeatures.free();    deviceFeatures = null; }
        if (memoryProperties != null)  { memoryProperties.free();  memoryProperties = null; }
    }

    public static void waitIdle() {
        if (device != null) vkDeviceWaitIdle(device);
    }

    public static boolean isExtensionEnabled(String name) { return enabledExtensions.contains(name); }

    public static VkPhysicalDevice getPhysicalDevice() { return physicalDevice; }
    public static VkDevice getDevice() { return device; }
    public static VkQueue getGraphicsQueue() { return graphicsQueue; }
    public static VkQueue getPresentQueue() { return presentQueue; }
    public static VkQueue getTransferQueue() { return transferQueue; }
    public static int getGraphicsQueueFamily() { return graphicsQueueFamily; }
    public static int getPresentQueueFamily()  { return presentQueueFamily; }
    public static int getTransferQueueFamily() { return transferQueueFamily; }
    public static String getDeviceName() { return deviceName; }
    public static VkPhysicalDeviceProperties getDeviceProperties() { return deviceProperties; }
    public static VkPhysicalDeviceMemoryProperties getMemoryProperties() { return memoryProperties; }
    public static VkPhysicalDeviceFeatures getDeviceFeatures() { return deviceFeatures; }
}
