package net.vulkadroid.android;

import net.vulkadroid.Initializer;

public class AndroidDeviceDetector {
    private static boolean isAndroid = false;
    private static boolean isAdreno650 = false;
    private static String deviceInfo = "Unknown";
    private static String gpuRenderer = "Unknown";

    public static void detect() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String javaVm = System.getProperty("java.vm.name", "").toLowerCase();

        isAndroid = osName.contains("android") || javaVm.contains("dalvik") || javaVm.contains("art");

        if (isAndroid) {
            try {
                String model        = getAndroidProp("ro.product.model");
                String board        = getAndroidProp("ro.board.platform");
                String gpuVersion   = getAndroidProp("ro.hardware.egl");
                String hardware     = getAndroidProp("ro.hardware");

                gpuRenderer = gpuVersion.isEmpty() ? hardware : gpuVersion;
                deviceInfo  = model + " / " + board;

                // Detect Adreno 650 (Snapdragon 870 / SM8250-AC or SM8250)
                isAdreno650 = board.contains("kona") || board.contains("msmnile") ||
                              hardware.toLowerCase().contains("adreno650") ||
                              model.toLowerCase().contains("poco f3") ||
                              model.toLowerCase().contains("mi 11") ||
                              model.toLowerCase().contains("oneplus 9") ||
                              gpuRenderer.toLowerCase().contains("adreno650") ||
                              gpuRenderer.toLowerCase().contains("adreno 650");

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
