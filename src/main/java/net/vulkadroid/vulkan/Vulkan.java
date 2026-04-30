package net.vulkadroid.vulkan;

import net.vulkadroid.Initializer;
import net.vulkadroid.android.AndroidDeviceDetector;
import net.vulkadroid.vulkan.device.DeviceManager;
import net.vulkadroid.vulkan.memory.MemoryManager;
import net.vulkadroid.vulkan.framebuffer.SwapChain;
import net.vulkadroid.vulkan.framebuffer.RenderPass;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRAndroidSurface.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;

public class Vulkan {

    public static final boolean ENABLE_VALIDATION = Boolean.getBoolean("vulkadroid.validation");

    private static VkInstance instance;
    private static long debugMessenger = VK_NULL_HANDLE;
    private static long surface = VK_NULL_HANDLE;
    private static boolean initialized = false;

    // Simpan reference callback agar tidak di-GC
    private static VkDebugUtilsMessengerCallbackEXT debugCallback;

    private static final String[] VALIDATION_LAYERS = {
        "VK_LAYER_KHRONOS_validation"
    };

    public static void initialize() {
        if (initialized) return;

        createInstance();
        if (ENABLE_VALIDATION) setupDebugMessenger();
        DeviceManager.initialize(instance);
        MemoryManager.initialize(
            DeviceManager.getPhysicalDevice(),
            DeviceManager.getDevice(),
            instance
        );
        SwapChain.initialize();

        if (SwapChain.getHandle() != VK_NULL_HANDLE && SwapChain.getImageViews() != null) {
            RenderPass.initialize();
        } else {
            Initializer.LOGGER.warn("SwapChain not ready, skipping RenderPass initialization");
        }

        Synchronization.initialize();
        initialized = true;
        Initializer.LOGGER.info("Vulkan {} initialized on {}",
            getVulkanVersionString(), DeviceManager.getDeviceName());
    }

    private static void createInstance() {
        try (MemoryStack stack = stackPush()) {

            // ── VkApplicationInfo ─────────────────────────────────────────────
            // Pakai calloc (tidak pakai UNSAFE), tapi setter-nya pakai UNSAFE
            // → bypass semua setter dengan memPutInt/memPutLong langsung
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            long appInfoAddr = appInfo.address();

            ByteBuffer appNameBuf    = stack.UTF8("Minecraft");
            ByteBuffer engineNameBuf = stack.UTF8("VulkaDroid");

            memPutInt (appInfoAddr + VkApplicationInfo.STYPE,              VK_STRUCTURE_TYPE_APPLICATION_INFO);
            memPutLong(appInfoAddr + VkApplicationInfo.PNEXT,              NULL);
            memPutLong(appInfoAddr + VkApplicationInfo.PAPPLICATIONNAME,   memAddress(appNameBuf));
            memPutInt (appInfoAddr + VkApplicationInfo.APPLICATIONVERSION, VK_MAKE_VERSION(1, 21, 1));
            memPutLong(appInfoAddr + VkApplicationInfo.PENGINENAME,        memAddress(engineNameBuf));
            memPutInt (appInfoAddr + VkApplicationInfo.ENGINEVERSION,      VK_MAKE_VERSION(1, 0, 0));
            memPutInt (appInfoAddr + VkApplicationInfo.APIVERSION,         VK12.VK_API_VERSION_1_2);

            // ── Extensions ────────────────────────────────────────────────────
            List<String> extensions = getRequiredInstanceExtensions();
            PointerBuffer pExtensions = stack.mallocPointer(extensions.size());
            for (String ext : extensions) pExtensions.put(stack.UTF8(ext));
            pExtensions.flip();

            // ── Validation layers (optional) ──────────────────────────────────
            PointerBuffer pLayers = null;
            if (ENABLE_VALIDATION && checkValidationLayerSupport()) {
                pLayers = stack.mallocPointer(VALIDATION_LAYERS.length);
                for (String layer : VALIDATION_LAYERS) pLayers.put(stack.UTF8(layer));
                pLayers.flip();
            }

            // ── VkInstanceCreateInfo ──────────────────────────────────────────
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
            long ciAddr = createInfo.address();

            memPutInt (ciAddr + VkInstanceCreateInfo.STYPE,                   VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            memPutLong(ciAddr + VkInstanceCreateInfo.PNEXT,                   NULL);
            memPutInt (ciAddr + VkInstanceCreateInfo.FLAGS,                    0);
            memPutLong(ciAddr + VkInstanceCreateInfo.PAPPLICATIONINFO,         appInfoAddr);
            memPutInt (ciAddr + VkInstanceCreateInfo.ENABLEDEXTENSIONCOUNT,    pExtensions.remaining());
            memPutLong(ciAddr + VkInstanceCreateInfo.PPENABLEDEXTENSIONNAMES,  memAddress(pExtensions));

            if (pLayers != null) {
                memPutInt (ciAddr + VkInstanceCreateInfo.ENABLEDLAYERCOUNT,    pLayers.remaining());
                memPutLong(ciAddr + VkInstanceCreateInfo.PPENABLEDLAYERNAMES,  memAddress(pLayers));
            } else {
                memPutInt (ciAddr + VkInstanceCreateInfo.ENABLEDLAYERCOUNT,    0);
                memPutLong(ciAddr + VkInstanceCreateInfo.PPENABLEDLAYERNAMES,  NULL);
            }

            // ── Debug pNext (opsional) ────────────────────────────────────────
            if (ENABLE_VALIDATION) {
                VkDebugUtilsMessengerCreateInfoEXT dbgInfo =
                    VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                populateDebugMessengerCreateInfo(dbgInfo, stack);
                memPutLong(ciAddr + VkInstanceCreateInfo.PNEXT, dbgInfo.address());
            }

            // ── vkCreateInstance ──────────────────────────────────────────────
            PointerBuffer pInstance = stack.mallocPointer(1);
            int result = vkCreateInstance(createInfo, null, pInstance);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan instance. VkResult: " + result);
            }

            instance = new VkInstance(pInstance.get(0), createInfo);
            Initializer.LOGGER.info("Vulkan instance created with {} extensions", extensions.size());
        }
    }

    private static void populateDebugMessengerCreateInfo(
            VkDebugUtilsMessengerCreateInfoEXT info, MemoryStack stack) {

        // Buat callback terpisah dan simpan reference-nya agar tidak di-GC
        debugCallback = VkDebugUtilsMessengerCallbackEXT.create(
            (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                VkDebugUtilsMessengerCallbackDataEXT data =
                    VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                String msg = data.pMessageString();
                if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0)
                    Initializer.LOGGER.error("[Vulkan Validation] {}", msg);
                else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0)
                    Initializer.LOGGER.warn("[Vulkan Validation] {}", msg);
                else
                    Initializer.LOGGER.debug("[Vulkan Validation] {}", msg);
                return VK_FALSE;
            }
        );

        long addr = info.address();
        memPutInt (addr + VkDebugUtilsMessengerCreateInfoEXT.STYPE,
                   VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        memPutLong(addr + VkDebugUtilsMessengerCreateInfoEXT.PNEXT,   NULL);
        memPutInt (addr + VkDebugUtilsMessengerCreateInfoEXT.FLAGS,    0);
        memPutInt (addr + VkDebugUtilsMessengerCreateInfoEXT.MESSAGESEVERITY,
                   VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                   VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                   VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        memPutInt (addr + VkDebugUtilsMessengerCreateInfoEXT.MESSAGETYPE,
                   VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                   VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                   VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        memPutLong(addr + VkDebugUtilsMessengerCreateInfoEXT.PFNUSERCALLBACK,
                   debugCallback.address());
        memPutLong(addr + VkDebugUtilsMessengerCreateInfoEXT.PUSERDATA, NULL);
    }

    private static void setupDebugMessenger() {
        try (MemoryStack stack = stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT createInfo =
                VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
            populateDebugMessengerCreateInfo(createInfo, stack);

            LongBuffer pDebugMessenger = stack.mallocLong(1);
            if (vkCreateDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger)
                    != VK_SUCCESS) {
                Initializer.LOGGER.warn("Failed to set up debug messenger");
                return;
            }
            debugMessenger = pDebugMessenger.get(0);
        }
    }

    public static void createAndroidSurface(long nativeWindow) {
        try (MemoryStack stack = stackPush()) {
            VkAndroidSurfaceCreateInfoKHR createInfo =
                VkAndroidSurfaceCreateInfoKHR.calloc(stack);
            long addr = createInfo.address();

            memPutInt (addr + VkAndroidSurfaceCreateInfoKHR.STYPE,
                       VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR);
            memPutLong(addr + VkAndroidSurfaceCreateInfoKHR.PNEXT,  NULL);
            memPutInt (addr + VkAndroidSurfaceCreateInfoKHR.FLAGS,  0);
            memPutLong(addr + VkAndroidSurfaceCreateInfoKHR.WINDOW, nativeWindow);

            LongBuffer pSurface = stack.mallocLong(1);
            int result = vkCreateAndroidSurfaceKHR(instance, createInfo, null, pSurface);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Android surface. VkResult: " + result);
            }
            surface = pSurface.get(0);
        }
    }

    private static List<String> getRequiredInstanceExtensions() {
        List<String> extensions = new ArrayList<>();
        if (AndroidDeviceDetector.isAndroid()) {
            extensions.add(KHRSurface.VK_KHR_SURFACE_EXTENSION_NAME);
            extensions.add(VK_KHR_ANDROID_SURFACE_EXTENSION_NAME);
        } else {
            extensions.add(KHRSurface.VK_KHR_SURFACE_EXTENSION_NAME);
        }
        extensions.add("VK_KHR_get_physical_device_properties2");
        if (ENABLE_VALIDATION) extensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        return filterAvailableExtensions(extensions);
    }

    private static List<String> filterAvailableExtensions(List<String> requested) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pCount = stack.mallocInt(1);
            vkEnumerateInstanceExtensionProperties((String) null, pCount, null);
            VkExtensionProperties.Buffer available =
                VkExtensionProperties.malloc(pCount.get(0), stack);
            vkEnumerateInstanceExtensionProperties((String) null, pCount, available);

            List<String> filtered = new ArrayList<>();
            for (String req : requested) {
                for (VkExtensionProperties ext : available) {
                    if (ext.extensionNameString().equals(req)) {
                        filtered.add(req);
                        break;
                    }
                }
            }
            return filtered;
        }
    }

    private static boolean checkValidationLayerSupport() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pCount = stack.mallocInt(1);
            vkEnumerateInstanceLayerProperties(pCount, null);
            VkLayerProperties.Buffer available =
                VkLayerProperties.malloc(pCount.get(0), stack);
            vkEnumerateInstanceLayerProperties(pCount, available);

            for (String layer : VALIDATION_LAYERS) {
                boolean found = false;
                for (VkLayerProperties l : available) {
                    if (l.layerNameString().equals(layer)) { found = true; break; }
                }
                if (!found) return false;
            }
            return true;
        }
    }

    private static String getVulkanVersionString() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pVersion = stack.mallocInt(1);
            vkEnumerateInstanceVersion(pVersion);
            int ver = pVersion.get(0);
            return VK_VERSION_MAJOR(ver) + "." + VK_VERSION_MINOR(ver) + "." + VK_VERSION_PATCH(ver);
        }
    }

    public static void cleanup() {
        if (!initialized) return;
        Synchronization.cleanup();
        SwapChain.cleanup();
        RenderPass.cleanup();
        MemoryManager.cleanup();
        DeviceManager.cleanup();
        if (surface != VK_NULL_HANDLE) {
            KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
            surface = VK_NULL_HANDLE;
        }
        if (debugMessenger != VK_NULL_HANDLE) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
            debugMessenger = VK_NULL_HANDLE;
        }
        if (debugCallback != null) {
            debugCallback.free();
            debugCallback = null;
        }
        if (instance != null) {
            vkDestroyInstance(instance, null);
            instance = null;
        }
        initialized = false;
    }

    public static VkInstance getInstance()  { return instance; }
    public static long getSurface()         { return surface; }
    public static boolean isInitialized()   { return initialized; }
    public static void setSurface(long s)   { surface = s; }
}