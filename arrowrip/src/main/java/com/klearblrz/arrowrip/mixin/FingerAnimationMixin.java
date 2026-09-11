package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AnimeAnimationClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class FingerAnimationMixin {
    private static final String[] DIGITS = {"pinky", "ring", "middle", "index"};

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$animateFingers(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        boolean local = state.id == client.player.getId();
        boolean speech = AnimeAnimationClient.getSpeechEmoteTicks() > 0
                && state.id == AnimeAnimationClient.getSpeechSpeakerId();
        boolean neckTarget = AnimeAnimationClient.getNeckSnapTicks() > 0
                && state.id == AnimeAnimationClient.getNeckSnapTargetId();
        boolean relevant = local || speech || neckTarget;

        try {
            setHandVisible(model.rightArm, true, relevant);
            setHandVisible(model.leftArm, false, relevant);
        } catch (Throwable ignored) {
            return;
        }
        if (!relevant) return;

        int rightMode = AnimeAnimationClient.areFingersOpen() ? 0 : 1;
        int leftMode = rightMode;

        if (state.getMainHandItemStack().isIn(ItemTags.SWORDS)) {
            if (state.mainArm == Arm.RIGHT) rightMode = 1;
            else leftMode = 1;
        }
        if (state.handSwingProgress > 0.01F) {
            if (state.mainArm == Arm.RIGHT) rightMode = 1;
            else leftMode = 1;
        }

        if (local && AnimeAnimationClient.getEmoteType() == 10) {
            rightMode = 1;
            leftMode = 1;
        }

        if (speech) {
            int style = AnimeAnimationClient.getSpeechEmoteType();
            switch (style) {
                case 4 -> { rightMode = 1; leftMode = 1; }      // power fists
                case 5 -> rightMode = 0;                        // stop palm
                case 8, 9, 10 -> rightMode = 2;                 // pointing / threat / beckon
                case 11 -> rightMode = 1;                       // determined fist
                case 12 -> { rightMode = 0; leftMode = 0; }     // dramatic spread
                default -> { }
            }
        }

        if (AnimeAnimationClient.getNeckSnapTicks() > 0 && local) {
            rightMode = 1;
            leftMode = 1;
        }
        if (neckTarget) {
            rightMode = 0;
            leftMode = 0;
        }

        applyHand(model.rightArm, true, rightMode, state.age);
        applyHand(model.leftArm, false, leftMode, state.age);
    }

    private static void setHandVisible(ModelPart arm, boolean right, boolean visible) {
        String side = right ? "r" : "l";
        for (String digit : DIGITS) arm.getChild("arrowrip_" + side + "_" + digit).visible = visible;
        arm.getChild("arrowrip_" + side + "_thumb").visible = visible;
    }

    // 0 = open, 1 = fist/grip, 2 = point with index finger.
    private static void applyHand(ModelPart arm, boolean right, int mode, float age) {
        String side = right ? "r" : "l";
        float sign = right ? 1.0F : -1.0F;
        float idle = MathHelper.sin(age * 0.09F) * 0.055F;

        for (int i = 0; i < DIGITS.length; i++) {
            ModelPart finger = arm.getChild("arrowrip_" + side + "_" + DIGITS[i]);
            if (mode == 0) {
                finger.pitch = idle * (0.65F + i * 0.10F);
                finger.yaw = sign * ((i - 1.5F) * 0.055F);
                finger.roll = sign * ((i - 1.5F) * 0.018F);
            } else if (mode == 2 && i == 3) {
                finger.pitch = -0.05F + idle * 0.30F;
                finger.yaw = 0.0F;
                finger.roll = 0.0F;
            } else {
                float curl = 1.16F + i * 0.085F;
                finger.pitch = -curl;
                finger.yaw = sign * (0.055F - i * 0.010F);
                finger.roll = sign * 0.035F;
            }
        }

        ModelPart thumb = arm.getChild("arrowrip_" + side + "_thumb");
        if (mode == 0) {
            thumb.pitch = -0.18F + idle * 0.35F;
            thumb.yaw = -sign * 0.55F;
            thumb.roll = sign * 0.34F;
        } else {
            thumb.pitch = -0.84F;
            thumb.yaw = -sign * 0.78F;
            thumb.roll = sign * 0.62F;
        }
    }
}
