package net.vulkadroid.android;

import net.vulkadroid.Initializer;
import net.vulkadroid.config.Config;
import net.vulkadroid.vulkan.Vulkan;
import net.vulkadroid.vulkan.Renderer;
import net.vulkadroid.vulkan.framebuffer.SwapChain;

public class SurfaceManager {
    private static long nativeWindow = 0;
    private static boolean surfaceValid = false;
    private static int surfaceWidth  = 0;
    private static int surfaceHeight = 0;

    /**
     * Called when Android provides an ANativeWindow.
     * This is the entry point for Vulkan surface creation on Android.
     */
    public static void onSurfaceCreated(long aNativeWindowPtr, int width, int height) {
        nativeWindow  = aNativeWindowPtr;
        surfaceWidth  = width;
        surfaceHeight = height;
        Config.setWindowSize(width, height);

        Initializer.LOGGER.info("Android surface created: {}x{} ptr={}", width, height, aNativeWindowPtr);

        if (Vulkan.isInitialized()) {
            Vulkan.createAndroidSurface(aNativeWindowPtr);
            SwapChain.create();
            surfaceValid = true;
        }
    }

    /**
     * Called on orientation change or surface resize.
     */
    public static void onSurfaceChanged(int newWidth, int newHeight) {
        if (newWidth == surfaceWidth && newHeight == surfaceHeight) return;
        surfaceWidth  = newWidth;
        surfaceHeight = newHeight;
        Config.setWindowSize(newWidth, newHeight);
        Initializer.LOGGER.info("Android surface changed: {}x{}", newWidth, newHeight);
        if (surfaceValid) {
            Renderer.handleResize();
        }
    }

    /**
     * Called when the Android surface is destroyed (app backgrounded, etc.)
     */
    public static void onSurfaceDestroyed() {
        surfaceValid = false;
        nativeWindow = 0;
        Initializer.LOGGER.info("Android surface destroyed");
        SwapChain.cleanup();
    }

    /**
     * Called when app resumes — recreate surface.
     */
    public static void onResume(long aNativeWindowPtr) {
        if (aNativeWindowPtr != 0) {
            nativeWindow = aNativeWindowPtr;
            if (Vulkan.isInitialized()) {
                Vulkan.createAndroidSurface(aNativeWindowPtr);
                SwapChain.create();
                surfaceValid = true;
                Initializer.LOGGER.info("Android surface resumed");
            }
        }
    }

    public static boolean isSurfaceValid() { return surfaceValid; }
    public static int getSurfaceWidth()    { return surfaceWidth; }
    public static int getSurfaceHeight()   { return surfaceHeight; }
    public static long getNativeWindow()   { return nativeWindow; }
}
