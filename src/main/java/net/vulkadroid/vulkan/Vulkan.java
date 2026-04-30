package net.vulkadroid.vulkan;

import net.vulkadroid.Initializer;
import net.vulkadroid.android.AndroidDeviceDetector;
import net.vulkadroid.vulkan.device.DeviceManager;
import net.vulkadroid.vulkan.memory.MemoryManager;
import net.vulkadroid.vulkan.framebuffer.SwapChain;
import net.vulkadroid.vulkan.framebuffer.RenderPass;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Configuration;
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

        // AppInfo via memPut (sama seperti sebelumnya)
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

        // Extensions
        List<String> extensions = getRequiredInstanceExtensions();
        PointerBuffer pExtensions = stack.mallocPointer(extensions.size());
        for (String ext : extensions) pExtensions.put(stack.UTF8(ext));
        pExtensions.flip();

        // Layers
        PointerBuffer pLayers = null;
        if (ENABLE_VALIDATION && checkValidationLayerSupport()) {
            pLayers = stack.mallocPointer(VALIDATION_LAYERS.length);
            for (String layer : VALIDATION_LAYERS) pLayers.put(stack.UTF8(layer));
            pLayers.flip();
        }

        // InstanceCreateInfo via memPut
        VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
        long ciAddr = createInfo.address();
        memPutInt (ciAddr + VkInstanceCreateInfo.STYPE,                  VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
        memPutLong(ciAddr + VkInstanceCreateInfo.PNEXT,                  NULL);
        memPutInt (ciAddr + VkInstanceCreateInfo.FLAGS,                   0);
        memPutLong(ciAddr + VkInstanceCreateInfo.PAPPLICATIONINFO,        appInfoAddr);
        memPutInt (ciAddr + VkInstanceCreateInfo.ENABLEDEXTENSIONCOUNT,   pExtensions.remaining());
        memPutLong(ciAddr + VkInstanceCreateInfo.PPENABLEDEXTENSIONNAMES, memAddress(pExtensions));
        if (pLayers != null) {
            memPutInt (ciAddr + VkInstanceCreateInfo.ENABLEDLAYERCOUNT,   pLayers.remaining());
            memPutLong(ciAddr + VkInstanceCreateInfo.PPENABLEDLAYERNAMES, memAddress(pLayers));
        } else {
            memPutInt (ciAddr + VkInstanceCreateInfo.ENABLEDLAYERCOUNT,   0);
            memPutLong(ciAddr + VkInstanceCreateInfo.PPENABLEDLAYERNAMES, NULL);
        }

        // ── Bypass vkCreateInstance wrapper yang panggil validate() ──────────
        // Masalah: nvkCreateInstance() → validate() → nenabledLayerCount() → UNSAFE field crash
        // UNSAFE field tidak ada di custom LWJGL JAR dari vulkanmod-an-libs
        //
        // Fix: Panggil JNI.callPPPI langsung pakai function pointer dari VK global commands.
        // Ini sepenuhnya skip semua Java wrapper + validate() milik LWJGL.
        PointerBuffer pInstance = stack.mallocPointer(1);
        int result;
        long fp = resolveFunctionPointerVkCreateInstance();
        result = org.lwjgl.system.JNI.callPPPI(ciAddr, NULL, memAddress(pInstance), fp);

        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create Vulkan instance. VkResult: " + result);
        }

        // new VkInstance(handle, createInfo) juga crash: VkApplicationInfo.apiVersion() pakai UNSAFE
        // Solusi: buat VkInstance via Unsafe.allocateInstance + set fields manual
        instance = buildVkInstanceReflective(pInstance.get(0), extensions);
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

    /**
     * Buat VkInstance TANPA memanggil konstruktor LWJGL standar.
     * new VkInstance(handle, createInfo) → getInstanceCapabilities → VkApplicationInfo.apiVersion()
     * → napiVersion() → UNSAFE field crash karena custom LWJGL JAR stripped.
     *
     * Fix: allocateInstance via JVM Unsafe, set fields 'address' dan 'capabilities' reflektif.
     */
    private static VkInstance buildVkInstanceReflective(long handle, List<String> exts) throws Exception {
        sun.misc.Unsafe jvmUnsafe = getJvmUnsafe();

        // Allocate tanpa constructor
        @SuppressWarnings("unchecked")
        VkInstance inst = (VkInstance) jvmUnsafe.allocateInstance(VkInstance.class);

        // Set field 'address' dari Pointer.Default (superclass)
        setFieldInHierarchy(inst, "address", handle);

        // Bangun VKCapabilitiesInstance menggunakan vkGetInstanceProcAddr
        long getProcFP = resolveVkGetInstanceProcAddr();
        org.lwjgl.system.FunctionProvider fp = funcName ->
            org.lwjgl.system.JNI.callPPP(handle,
                org.lwjgl.system.MemoryUtil.memAddress(funcName), getProcFP);

        java.util.Set<String> extSet = new java.util.HashSet<>(exts);
        // Constructor VKCapabilitiesInstance bersifat package-private, harus pakai refleksi
        VKCapabilitiesInstance caps;
        try {
            java.lang.reflect.Constructor<VKCapabilitiesInstance> ctor =
                VKCapabilitiesInstance.class.getDeclaredConstructor(
                    org.lwjgl.system.FunctionProvider.class, int.class,
                    java.util.Set.class, java.util.Set.class);
            ctor.setAccessible(true);
            caps = ctor.newInstance(fp, VK12.VK_API_VERSION_1_2, extSet, new java.util.HashSet<>());
        } catch (Exception e) {
            throw new RuntimeException("Gagal buat VKCapabilitiesInstance via refleksi", e);
        }

        // Set field 'capabilities' dari DispatchableHandleInstance
        setFieldInHierarchy(inst, "capabilities", caps);

        Initializer.LOGGER.info("[VulkaDroid] VkInstance built reflectively. Handle: 0x{}", Long.toHexString(handle));
        return inst;
    }

    private static sun.misc.Unsafe getJvmUnsafe() throws Exception {
        java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

    private static void setFieldInHierarchy(Object obj, String name, Object value) throws Exception {
        Class<?> c = obj.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                if (value instanceof Long) f.setLong(obj, (Long) value);
                else f.set(obj, value);
                return;
            } catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException("Field '" + name + "' tidak ditemukan di " + obj.getClass().getName());
    }

    private static long resolveVkGetInstanceProcAddr() {
        try {
            Class<?> vkClass = Class.forName("org.lwjgl.vulkan.VK");
            java.lang.reflect.Method m = vkClass.getDeclaredMethod("getGlobalCommands");
            m.setAccessible(true);
            Object globalCaps = m.invoke(null);
            java.lang.reflect.Field f = globalCaps.getClass().getDeclaredField("vkGetInstanceProcAddr");
            f.setAccessible(true);
            long addr = f.getLong(globalCaps);
            if (addr != 0L) return addr;
        } catch (Throwable e) {
            Initializer.LOGGER.warn("[VulkaDroid] resolveVkGetInstanceProcAddr gagal: {}", e.getMessage());
        }
        throw new RuntimeException("Tidak bisa resolve vkGetInstanceProcAddr");
    }

    /**
     * Mendapatkan function pointer vkCreateInstance tanpa melalui LWJGL validate().
     *
     * Strategy:
     *  1. Refleksi ke VK.getGlobalCommands() (LWJGL internal) untuk baca field long.
     *     Ini aman karena kita TIDAK memanggil metode yang ada validate()-nya.
     *  2. Fallback: Library.loadNative("vulkan") dan cari simbol manual.
     */
    private static long resolveFunctionPointerVkCreateInstance() {
        // --- Strategy 1: LWJGL VK.getGlobalCommands().vkCreateInstance ---
        try {
            Class<?> vkClass = Class.forName("org.lwjgl.vulkan.VK");
            java.lang.reflect.Method getGlobal = vkClass.getDeclaredMethod("getGlobalCommands");
            getGlobal.setAccessible(true);
            Object caps = getGlobal.invoke(null);
            java.lang.reflect.Field field = caps.getClass().getDeclaredField("vkCreateInstance");
            field.setAccessible(true);
            long fp = field.getLong(caps);
            if (fp != 0L) {
                Initializer.LOGGER.debug("[VulkaDroid] vkCreateInstance FP via VK.getGlobalCommands: 0x{}", Long.toHexString(fp));
                return fp;
            }
        } catch (Throwable e) {
            Initializer.LOGGER.warn("[VulkaDroid] Strategy 1 (VK.getGlobalCommands) gagal: {}", e.getMessage());
        }

        // --- Strategy 2: Library.loadNative("vulkan") ---
        try {
            org.lwjgl.system.SharedLibrary lib =
                org.lwjgl.system.Library.loadNative(
                    Vulkan.class,
                    "org.lwjgl.vulkan",
                    org.lwjgl.system.Configuration.VULKAN_LIBRARY_NAME,
                    "vulkan");
            long fp = lib.getFunctionAddress("vkCreateInstance");
            if (fp != 0L) {
                Initializer.LOGGER.debug("[VulkaDroid] vkCreateInstance FP via Library.loadNative: 0x{}", Long.toHexString(fp));
                return fp;
            }
        } catch (Throwable e) {
            Initializer.LOGGER.warn("[VulkaDroid] Strategy 2 (Library.loadNative) gagal: {}", e.getMessage());
        }

        throw new RuntimeException(
            "Tidak bisa mendapatkan vkCreateInstance function pointer. " +
            "Pastikan libvulkan.so tersedia di LD_LIBRARY_PATH.");
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