package com.klearblrz.arrowrip.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skin-safe locomotion polish only. It deliberately does not replace attack/swing animations.
 */
@Mixin(PlayerEntityModel.class)
public abstract class SimpleWalkRunMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$walkRunOnly(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) return;
        if (state.spectator || state.hasVehicle || state.touchingWater || state.isGliding || state.leaningPitch > 0.20F) return;

        // Let vanilla own attacks and item use completely.
        if (state.handSwingProgress > 0.001F || state.isUsingItem) return;

        float amp = MathHelper.clamp(state.limbSwingAmplitude, 0.0F, 1.0F);
        if (amp < 0.035F) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        float phase = state.limbSwingAnimationProgress * 0.72F;
        float wave = MathHelper.sin(phase);
        float bounce = Math.abs(MathHelper.cos(phase));

        if (client.player.isSprinting()) {
            // Anime sprint: forward lean, stronger stride and counter-rotation.
            model.body.pitch = Math.max(model.body.pitch, 0.34F + amp * 0.10F);
            model.body.yaw += wave * 0.10F;
            model.head.pitch -= 0.11F;
            model.head.roll += wave * 0.028F;

            model.rightArm.pitch *= 1.38F;
            model.leftArm.pitch *= 1.38F;
            model.rightArm.roll += 0.10F;
            model.leftArm.roll -= 0.10F;

            model.rightLeg.pitch *= 1.24F;
            model.leftLeg.pitch *= 1.24F;
            model.rightLeg.roll += 0.040F;
            model.leftLeg.roll -= 0.040F;
        } else if (amp > 0.34F) {
            // Brisk walk: more energetic but still natural.
            model.body.pitch += 0.08F + amp * 0.04F;
            model.body.yaw += wave * 0.065F;
            model.head.roll += wave * 0.020F;
            model.rightArm.pitch *= 1.17F;
            model.leftArm.pitch *= 1.17F;
            model.rightArm.roll += 0.055F;
            model.leftArm.roll -= 0.055F;
            model.rightLeg.pitch *= 1.10F;
            model.leftLeg.pitch *= 1.10F;
        } else {
            // Relaxed walk: small shoulder sway and soft weight transfer.
            model.body.pitch += 0.035F;
            model.body.yaw += wave * 0.040F;
            model.head.roll += wave * 0.012F;
            model.rightArm.pitch *= 1.07F;
            model.leftArm.pitch *= 1.07F;
            model.rightArm.roll += 0.028F + bounce * 0.010F;
            model.leftArm.roll -= 0.028F + bounce * 0.010F;
            model.rightLeg.pitch *= 1.04F;
            model.leftLeg.pitch *= 1.04F;
        }
    }
}
