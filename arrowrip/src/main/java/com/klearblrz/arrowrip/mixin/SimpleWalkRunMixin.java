package com.klearblrz.arrowrip.mixin;

import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skin-safe locomotion polish only: relaxed walk + stronger run.
 * No idle/emote/sit/finisher/cinematic changes live here.
 */
@Mixin(PlayerEntityModel.class)
public abstract class SimpleWalkRunMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$walkRunOnly(PlayerEntityRenderState state, CallbackInfo ci) {
        if (state.spectator || state.hasVehicle || state.touchingWater || state.isGliding || state.leaningPitch > 0.20F) return;

        // Combat and item use are handled elsewhere or left vanilla.
        if (state.handSwingProgress > 0.001F || state.isUsingItem) return;

        float amp = MathHelper.clamp(state.limbSwingAmplitude, 0.0F, 1.0F);
        if (amp < 0.035F) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        float phase = state.limbSwingAnimationProgress * 0.72F;
        float wave = MathHelper.sin(phase);
        float bounce = Math.abs(MathHelper.cos(phase));

        // High movement amplitude is treated as running so this works for every rendered skin/player.
        if (amp > 0.62F) {
            model.body.pitch = Math.max(model.body.pitch, 0.30F + amp * 0.08F);
            model.body.yaw += wave * 0.085F;
            model.head.pitch -= 0.085F;
            model.head.roll += wave * 0.024F;

            model.rightArm.pitch *= 1.30F;
            model.leftArm.pitch *= 1.30F;
            model.rightArm.roll += 0.075F;
            model.leftArm.roll -= 0.075F;

            model.rightLeg.pitch *= 1.18F;
            model.leftLeg.pitch *= 1.18F;
            model.rightLeg.roll += 0.032F;
            model.leftLeg.roll -= 0.032F;
        } else {
            model.body.pitch += 0.035F + amp * 0.025F;
            model.body.yaw += wave * 0.040F;
            model.head.roll += wave * 0.012F;

            model.rightArm.pitch *= 1.08F;
            model.leftArm.pitch *= 1.08F;
            model.rightArm.roll += 0.025F + bounce * 0.008F;
            model.leftArm.roll -= 0.025F + bounce * 0.008F;

            model.rightLeg.pitch *= 1.05F;
            model.leftLeg.pitch *= 1.05F;
        }
    }
}
