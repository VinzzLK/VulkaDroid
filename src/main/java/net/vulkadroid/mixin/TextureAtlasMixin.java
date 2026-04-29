package net.vulkadroid.mixin;

import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {
    // Tidak digunakan lagi — semua tracking lewat TextureManagerMixin
}