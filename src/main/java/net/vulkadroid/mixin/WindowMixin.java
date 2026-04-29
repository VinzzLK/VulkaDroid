package net.vulkadroid.mixin;

import com.mojang.blaze3d.platform.Window;
import net.vulkadroid.Initializer;
import net.vulkadroid.android.AndroidDeviceDetector;
import net.vulkadroid.config.Config;
import net.vulkadroid.vulkan.Vulkan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onWindowInit(CallbackInfo ci) {
        Window self = (Window)(Object)this;
        Config.setWindowSize(self.getWidth(), self.getHeight());
        if (!AndroidDeviceDetector.isAndroid()) {
            long glfwHandle = self.getWindow();
            if (glfwHandle != 0 && Vulkan.isInitialized()) createDesktopSurface(glfwHandle);
        }
    }

    @Inject(method = "setWindowed", at = @At("TAIL"))
    private void onSetWindowed(int w, int h, CallbackInfo ci) {
        Config.setWindowSize(w, h);
    }

    @Inject(method = "toggleFullScreen", at = @At("TAIL"))
    private void onToggleFullScreen(CallbackInfo ci) {
        Window self = (Window)(Object)this;
        Config.setWindowSize(self.getWidth(), self.getHeight());
    }

    private void createDesktopSurface(long glfwWindowHandle) {
        try {
            org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush();
            java.nio.LongBuffer pSurface = stack.mallocLong(1);
            long result = org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface(
                Vulkan.getInstance(), glfwWindowHandle, null, pSurface);
            if (result == org.lwjgl.vulkan.VK10.VK_SUCCESS) {
                Vulkan.setSurface(pSurface.get(0));
                Initializer.LOGGER.info("Desktop Vulkan surface created");
            }
            stack.close();
        } catch (Exception e) {
            Initializer.LOGGER.error("Failed to create desktop Vulkan surface", e);
        }
    }
}
