package net.vulkadroid.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.vulkadroid.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "setup", at = @At("TAIL"))
    private void onSetup(BlockGetter level, Entity entity,
            boolean thirdPerson, boolean inverseView, float partialTick, CallbackInfo ci) {
        Camera cam = (Camera)(Object)this;
        WorldRenderer.updateCameraPosition(cam.getPosition().x, cam.getPosition().y, cam.getPosition().z);
    }
}
