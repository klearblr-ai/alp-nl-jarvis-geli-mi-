package com.klearblrz.arrowrip.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skin-safe rectangular fingers. Fingers inherit the player's currently bound skin texture
 * because they are children of the vanilla arm model parts.
 */
@Mixin(PlayerEntityModel.class)
public abstract class FingerAnimationMixin {
    private static final String[] DIGITS = {"pinky", "ring", "middle", "index"};

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$animateFingers(PlayerEntityRenderState state, CallbackInfo ci) {
        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        boolean visible = !state.spectator;
        setHandVisible(model.rightArm, true, visible);
        setHandVisible(model.leftArm, false, visible);
        if (!visible) return;

        int rightMode = 0;
        int leftMode = 0;

        // Any held main-hand item is physically gripped by the fingers.
        if (!state.getMainHandItemStack().isEmpty()) {
            if (state.mainArm == Arm.RIGHT) rightMode = 1;
            else leftMode = 1;
        }

        // Empty-hand attacks automatically become a fist. Sword attacks keep the grip closed.
        if (state.handSwingProgress > 0.001F) {
            if (state.mainArm == Arm.RIGHT) rightMode = 1;
            else leftMode = 1;
        }

        applyHand(model.rightArm, true, rightMode, state.age, state.limbSwingAmplitude, state.handSwingProgress);
        applyHand(model.leftArm, false, leftMode, state.age, state.limbSwingAmplitude, state.handSwingProgress);
    }

    private static void setHandVisible(ModelPart arm, boolean right, boolean visible) {
        String side = right ? "r" : "l";
        try {
            for (String digit : DIGITS) {
                ModelPart finger = arm.getChild("arrowrip_" + side + "_" + digit);
                finger.visible = visible;
                finger.getChild("arrowrip_" + side + "_" + digit + "_tip").visible = visible;
            }
            ModelPart thumb = arm.getChild("arrowrip_" + side + "_thumb");
            thumb.visible = visible;
            thumb.getChild("arrowrip_" + side + "_thumb_tip").visible = visible;
        } catch (Throwable ignored) { }
    }

    // 0 = relaxed/open, 1 = fist/item grip.
    private static void applyHand(ModelPart arm, boolean right, int mode, float age, float moveAmp, float swingProgress) {
        String side = right ? "r" : "l";
        float sign = right ? 1.0F : -1.0F;
        float motion = MathHelper.clamp(moveAmp, 0.0F, 1.0F);
        float attack = MathHelper.sin(MathHelper.clamp(swingProgress, 0.0F, 1.0F) * (float)Math.PI);
        float idle = MathHelper.sin(age * 0.095F) * (0.035F + motion * 0.022F);

        for (int i = 0; i < DIGITS.length; i++) {
            try {
                ModelPart finger = arm.getChild("arrowrip_" + side + "_" + DIGITS[i]);
                ModelPart tip = finger.getChild("arrowrip_" + side + "_" + DIGITS[i] + "_tip");
                if (mode == 0) {
                    finger.pitch = -0.06F + idle * (0.60F + i * 0.08F);
                    finger.yaw = sign * ((i - 1.5F) * 0.045F);
                    finger.roll = sign * ((i - 1.5F) * 0.014F);
                    tip.pitch = -0.09F + idle * 0.40F;
                    tip.yaw = 0.0F;
                    tip.roll = 0.0F;
                } else {
                    float curl = 0.86F + i * 0.050F + attack * 0.10F;
                    finger.pitch = -curl;
                    finger.yaw = sign * (0.045F - i * 0.007F);
                    finger.roll = sign * 0.025F;
                    tip.pitch = -0.96F - i * 0.032F - attack * 0.08F;
                    tip.yaw = 0.0F;
                    tip.roll = 0.0F;
                }
            } catch (Throwable ignored) { }
        }

        try {
            ModelPart thumb = arm.getChild("arrowrip_" + side + "_thumb");
            ModelPart thumbTip = thumb.getChild("arrowrip_" + side + "_thumb_tip");
            if (mode == 0) {
                thumb.pitch = -0.20F + idle * 0.28F;
                thumb.yaw = -sign * 0.54F;
                thumb.roll = sign * 0.30F;
                thumbTip.pitch = -0.10F;
            } else {
                thumb.pitch = -0.70F - attack * 0.07F;
                thumb.yaw = -sign * 0.72F;
                thumb.roll = sign * 0.54F;
                thumbTip.pitch = -0.68F - attack * 0.05F;
            }
        } catch (Throwable ignored) { }
    }
}
