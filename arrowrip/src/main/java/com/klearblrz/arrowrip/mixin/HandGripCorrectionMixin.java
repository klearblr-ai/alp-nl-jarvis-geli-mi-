package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AnimeAnimationClient;
import com.klearblrz.arrowrip.ProceduralAIRigClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Final-pass hand correction: real grip around held items, proper fist on punches and corrected thumbs. */
@Mixin(PlayerEntityModel.class)
public abstract class HandGripCorrectionMixin {
    private static final String[] DIGITS = {"pinky", "ring", "middle", "index"};

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$finalHandGrip(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) return;

        boolean rightItem = !state.getItemStackForArm(Arm.RIGHT).isEmpty();
        boolean leftItem = !state.getItemStackForArm(Arm.LEFT).isEmpty();
        boolean bow = state.getMainHandItemStack().isOf(Items.BOW);
        boolean swing = state.handSwingProgress > 0.01F;

        int rightMode = AnimeAnimationClient.areFingersOpen() ? 0 : 1;
        int leftMode = rightMode;
        if (rightItem) rightMode = 1;
        if (leftItem) leftMode = 1;
        if (bow) { rightMode = 1; leftMode = 1; }
        if (swing) {
            if (state.mainArm == Arm.RIGHT) rightMode = 1;
            else leftMode = 1;
        }

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        applyVanilla(model.rightArm, true, rightMode, state.age);
        applyVanilla(model.leftArm, false, leftMode, state.age);

        if (ProceduralAIRigClient.isEnabled()) {
            ModelPart root = model.getRootPart();
            if (root.hasChild("arrowrip_ai_rig")) {
                ModelPart rig = root.getChild("arrowrip_ai_rig");
                if (rig.visible) {
                    ModelPart rh = rig.getChild("arrowrip_r_upper_arm").getChild("arrowrip_r_forearm").getChild("arrowrip_r_hand");
                    ModelPart lh = rig.getChild("arrowrip_l_upper_arm").getChild("arrowrip_l_forearm").getChild("arrowrip_l_hand");
                    applyRig(rh, true, rightMode, state.age);
                    applyRig(lh, false, leftMode, state.age);
                }
            }
        }
    }

    private static void applyVanilla(ModelPart arm, boolean right, int mode, float age) {
        String s = right ? "r" : "l";
        float sign = right ? 1.0F : -1.0F;
        float idle = MathHelper.sin(age * 0.08F + (right ? 0.0F : 1.2F)) * 0.025F;
        for (int i = 0; i < DIGITS.length; i++) {
            String n = "arrowrip_" + s + "_" + DIGITS[i];
            if (!arm.hasChild(n)) continue;
            ModelPart f = arm.getChild(n);
            if (mode == 0) {
                f.pitch = idle * (0.8F + i * 0.08F);
                f.yaw = sign * (i - 1.5F) * 0.035F;
                f.roll = 0.0F;
            } else {
                f.pitch = -(1.02F + i * 0.055F);
                f.yaw = sign * 0.035F;
                f.roll = sign * 0.018F;
            }
            String tipName = n + "_tip";
            if (f.hasChild(tipName)) {
                ModelPart tip = f.getChild(tipName);
                tip.pitch = mode == 0 ? 0.04F : -(1.26F + i * 0.035F);
                tip.yaw = 0.0F;
                tip.roll = 0.0F;
            }
        }
        String tn = "arrowrip_" + s + "_thumb";
        if (arm.hasChild(tn)) {
            ModelPart thumb = arm.getChild(tn);
            if (mode == 0) {
                thumb.pitch = -0.24F;
                thumb.yaw = sign * 0.48F;
                thumb.roll = -sign * 0.22F;
            } else {
                // Opposite of the old reversed direction: thumb folds across the fist/item grip.
                thumb.pitch = -0.72F;
                thumb.yaw = sign * 0.72F;
                thumb.roll = -sign * 0.48F;
            }
            if (thumb.hasChild(tn + "_tip")) {
                ModelPart tip = thumb.getChild(tn + "_tip");
                tip.pitch = mode == 0 ? -0.10F : -0.92F;
                tip.yaw = sign * (mode == 0 ? 0.12F : 0.32F);
                tip.roll = -sign * (mode == 0 ? 0.06F : 0.18F);
            }
        }
    }

    private static void applyRig(ModelPart hand, boolean right, int mode, float age) {
        String s = right ? "r" : "l";
        float sign = right ? 1.0F : -1.0F;
        float idle = MathHelper.sin(age * 0.09F + (right ? 0.2F : 1.5F)) * 0.022F;
        for (int i = 0; i < DIGITS.length; i++) {
            String n = "arrowrip_" + s + "_ai_" + DIGITS[i];
            if (!hand.hasChild(n)) continue;
            ModelPart f = hand.getChild(n);
            if (mode == 0) {
                f.pitch = idle * (0.8F + i * 0.08F);
                f.yaw = sign * (i - 1.5F) * 0.035F;
                f.roll = 0.0F;
            } else {
                f.pitch = -(1.12F + i * 0.055F);
                f.yaw = sign * 0.035F;
                f.roll = sign * 0.018F;
            }
        }
        String tn = "arrowrip_" + s + "_ai_thumb";
        if (hand.hasChild(tn)) {
            ModelPart thumb = hand.getChild(tn);
            if (mode == 0) {
                thumb.pitch = -0.22F;
                thumb.yaw = sign * 0.46F;
                thumb.roll = -sign * 0.20F;
            } else {
                thumb.pitch = -0.74F;
                thumb.yaw = sign * 0.70F;
                thumb.roll = -sign * 0.46F;
            }
        }
    }
}
