package net.vulkadroid.vulkan.device;

public class QueueFamilyIndices {
    public Integer graphicsFamily;
    public Integer presentFamily;
    public Integer transferFamily;

    public boolean isComplete() {
        return graphicsFamily != null && presentFamily != null;
    }

    public int[] uniqueFamilies() {
        if (graphicsFamily.equals(presentFamily)) {
            return new int[]{graphicsFamily};
        }
        return new int[]{graphicsFamily, presentFamily};
    }
}
