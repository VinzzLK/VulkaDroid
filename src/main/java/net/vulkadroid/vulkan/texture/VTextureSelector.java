package net.vulkadroid.vulkan.texture;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

public class VTextureSelector {

    private static final int MAX_UNITS = 8;
    private static final int[] boundTextureIds = new int[MAX_UNITS];
    private static final Map<Integer, VulkanImage> glIdToVulkanImage = new HashMap<>();
    private static final Map<ResourceLocation, Integer> locationToGlId = new HashMap<>();

    public static void bindTexture(int unit, int glId) {
        if (unit >= 0 && unit < MAX_UNITS) boundTextureIds[unit] = glId;
    }

    public static void bindTexture(int unit, ResourceLocation location) {
        Integer glId = locationToGlId.get(location);
        if (glId != null) bindTexture(unit, glId);
    }

    public static void registerTexture(int glId, VulkanImage image) {
        glIdToVulkanImage.put(glId, image);
    }

    public static void registerLocation(ResourceLocation location, int glId) {
        locationToGlId.put(location, glId);
    }

    public static void unregisterTexture(int glId) {
        VulkanImage img = glIdToVulkanImage.remove(glId);
        if (img != null) img.destroy();
    }

    public static VulkanImage getTexture(int unit) {
        int glId = (unit >= 0 && unit < MAX_UNITS) ? boundTextureIds[unit] : 0;
        return glIdToVulkanImage.get(glId);
    }

    public static VulkanImage getTextureById(int glId) {
        return glIdToVulkanImage.get(glId);
    }

    public static int getBoundId(int unit) {
        return (unit >= 0 && unit < MAX_UNITS) ? boundTextureIds[unit] : 0;
    }

    public static void cleanup() {
        for (VulkanImage img : glIdToVulkanImage.values()) {
            if (img != null) img.destroy();
        }
        glIdToVulkanImage.clear();
        locationToGlId.clear();
    }
}
