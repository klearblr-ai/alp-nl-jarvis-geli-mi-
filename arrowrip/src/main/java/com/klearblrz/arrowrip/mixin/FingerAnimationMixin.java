package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.FingerWalkClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Finger animation attached directly to vanilla arms so normal and Quick Skin textures keep working. */
@Mixin(PlayerEntityModel.class)
public abstract class FingerAnimationMixin {
    private static final String[] DIGITS = {"pinky", "ring", "middle", "index"};

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$animateFingers(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntityModel model = (PlayerEntityModel)(Object)this;

        // Fingers are children of the vanilla arms, so they inherit whatever skin texture is currently bound.
        setHandVisible(model.rightArm, true, !state.spectator);
        setHandVisible(model.leftArm, false, !state.spectator);
        if (state.spectator) return;

        boolean local = client.player != null && state.id == client.player.getId();
        int rightMode = local && !FingerWalkClient.fingersOpen() ? 1 : 0;
        int leftMode = rightMode;

        // Holding an item naturally closes the main hand around it. This is not an attack animation.
        if (!state.getMainHandItemStack().isEmpty()) {
            if (state.mainArm == Arm.RIGHT) rightMode = 1;
            else leftMode = 1;
        }

        applyHand(model.rightArm, true, rightMode, state.age, state.limbSwingAmplitude);
        applyHand(model.leftArm, false, leftMode, state.age, state.limbSwingAmplitude);
    }

    private static void setHandVisible(ModelPart arm, boolean right, boolean visible) {
        String side = right ? "r" : "l";
        try {
            for (String digit : DIGITS) {
                arm.getChild("arrowrip_" + side + "_" + digit).visible = visible;
            }
            arm.getChild("arrowrip_" + side + "_thumb").visible = visible;
        } catch (Throwable ignored) { }
    }

    // 0 = open/relaxed, 1 = fist/grip.
    private static void applyHand(ModelPart arm, boolean right, int mode, float age, float moveAmp) {
        String side = right ? "r" : "l";
        float sign = right ? 1.0F : -1.0F;
        float motion = MathHelper.clamp(moveAmp, 0.0F, 1.0F);
        float idle = MathHelper.sin(age * 0.095F) * (0.045F + motion * 0.018F);

        for (int i = 0; i < DIGITS.length; i++) {
            try {
                ModelPart finger = arm.getChild("arrowrip_" + side + "_" + DIGITS[i]);
                ModelPart tip = finger.getChild("arrowrip_" + side + "_" + DIGITS[i] + "_tip");
                if (mode == 0) {
                    finger.pitch = -0.07F + idle * (0.65F + i * 0.08F);
                    finger.yaw = sign * ((i - 1.5F) * 0.050F);
                    finger.roll = sign * ((i - 1.5F) * 0.016F);
                    tip.pitch = -0.10F + idle * 0.45F;
                    tip.yaw = 0.0F;
                    tip.roll = 0.0F;
                } else {
                    float curl = 0.92F + i * 0.055F;
                    finger.pitch = -curl;
                    finger.yaw = sign * (0.050F - i * 0.008F);
                    finger.roll = sign * 0.028F;
                    tip.pitch = -1.02F - i * 0.035F;
                    tip.yaw = 0.0F;
                    tip.roll = 0.0F;
                }
            } catch (Throwable ignored) { }
        }

        try {
            ModelPart thumb = arm.getChild("arrowrip_" + side + "_thumb");
            ModelPart thumbTip = thumb.getChild("arrowrip_" + side + "_thumb_tip");
            if (mode == 0) {
                thumb.pitch = -0.22F + idle * 0.30F;
                thumb.yaw = -sign * 0.56F;
                thumb.roll = sign * 0.32F;
                thumbTip.pitch = -0.12F;
            } else {
                thumb.pitch = -0.76F;
                thumb.yaw = -sign * 0.74F;
                thumb.roll = sign * 0.57F;
                thumbTip.pitch = -0.72F;
            }
        } catch (Throwable ignored) { }
    }
}
