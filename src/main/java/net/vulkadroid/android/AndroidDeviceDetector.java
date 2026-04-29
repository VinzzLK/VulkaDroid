package net.vulkadroid.android;

import net.vulkadroid.Initializer;

public class AndroidDeviceDetector {
    private static boolean isAndroid = true;
    private static boolean isAdreno650 = true;
    private static String deviceInfo = "Unknown";
    private static String gpuRenderer = "Unknown";

    public static void detect() {
        // 1. Check if android.os.Build exists (true Android)
        try {
            Class.forName("android.os.Build");
            isAndroid = true;
        } catch (ClassNotFoundException e) {
            isAndroid = false;
        }

        if (isAndroid) {
            try {
                String model        = getAndroidProp("ro.product.model");
                String board        = getAndroidProp("ro.board.platform");
                String gpuVersion   = getAndroidProp("ro.hardware.egl");
                String hardware     = getAndroidProp("ro.hardware");

                gpuRenderer = (!gpuVersion.isEmpty()) ? gpuVersion : hardware;
                deviceInfo  = model + " / " + board;

                String combined = (board + hardware + model + gpuRenderer).toLowerCase();
                isAdreno650 = combined.contains("kona") || combined.contains("msmnile") ||
                              combined.contains("adreno650") || combined.contains("adreno 650") ||
                              combined.contains("poco f3") || combined.contains("mi 11") ||
                              combined.contains("oneplus 9") || combined.contains("sm8250");

            } catch (Exception e) {
                Initializer.LOGGER.warn("Failed to read Android device props: {}", e.getMessage());
            }
        } else {
            deviceInfo = System.getProperty("os.name") + " / " + System.getProperty("os.arch");
        }

        Initializer.LOGGER.info("Platform: {} | Android: {} | Adreno650: {} | GPU: {}",
            deviceInfo, isAndroid, isAdreno650, gpuRenderer);
    }

    private static String getAndroidProp(String key) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            return (String) systemProperties.getMethod("get", String.class)
                .invoke(null, key);
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isAndroid()    { return isAndroid; }
    public static boolean isAdreno650()  { return isAdreno650; }
    public static String getDeviceInfo() { return deviceInfo; }
    public static String getGpuRenderer(){ return gpuRenderer; }
}