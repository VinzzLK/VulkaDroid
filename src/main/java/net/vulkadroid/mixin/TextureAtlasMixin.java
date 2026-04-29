package net.vulkadroid.mixin;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.vulkadroid.Initializer;
import net.vulkadroid.vulkan.texture.VTextureSelector;
import net.vulkadroid.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_SRGB;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {

    @Shadow
    private int width;
    
    @Shadow
    private int height;

    @Inject(method = "upload", at = @At("TAIL"), remap = false)
    private void onUpload(CallbackInfo ci) {
        if (!Initializer.isInitialized()) return;
        TextureAtlas self = (TextureAtlas)(Object)this;
        int glId = self.getId();
        
        // Pake shadow fields
        int w = this.width;
        int h = this.height;
        
        VulkanImage existing = VTextureSelector.getTextureById(glId);
        if (existing != null) existing.destroy();

        VulkanImage atlasImage = new VulkanImage(w, h, VK_FORMAT_R8G8B8A8_SRGB);
        VTextureSelector.registerTexture(glId, atlasImage);
        VTextureSelector.registerLocation(self.location(), glId);

        Initializer.LOGGER.info("TextureAtlas '{}' → Vulkan {}x{}", self.location(), w, h);
    }
}