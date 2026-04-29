package net.vulkadroid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.vulkadroid.android.AndroidDeviceDetector;
import net.vulkadroid.android.Adreno650Optimizer;
import net.vulkadroid.config.Config;
import net.vulkadroid.vulkan.Vulkan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class Initializer implements ClientModInitializer {

    public static final String MOD_ID = "vulkadroid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static boolean initialized = false;

    @Override
public void onInitializeClient() {
    LOGGER.info("========================================");
    LOGGER.info("  VulkaDroid 1.0.0-alpha initializing  ");
    LOGGER.info("========================================");

    try {
        Config.load();
        LOGGER.info("Configuration loaded");

        AndroidDeviceDetector.detect();
        LOGGER.info("Device: {}", AndroidDeviceDetector.getDeviceInfo());

        if (AndroidDeviceDetector.isAdreno650()) {
            Adreno650Optimizer.apply();
            LOGGER.info("Adreno 650 optimizations applied");
        } else if (AndroidDeviceDetector.isAndroid()) {
            LOGGER.info("Generic Android optimizations applied");
            Adreno650Optimizer.applyGeneric();
        }

        Vulkan.initialize();
        initialized = true;
        LOGGER.info("VulkaDroid initialized successfully - Vulkan backend active");

    } catch (Throwable e) {  // <-- bukan Exception, tapi Throwable
        LOGGER.error("VulkaDroid failed to initialize! Falling back to OpenGL.", e);
        initialized = false;
    }
}

public static boolean isInitialized() {
        return initialized;
    }
