package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AnimeAnimationClient;
import com.klearblrz.arrowrip.AutoPvPAnimationClient;
import com.klearblrz.arrowrip.ProceduralAIRigClient;
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
public abstract class FingerKnuckleMixin {
    private static final String[] DIGITS = {"pinky", "ring", "middle", "index"};

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$curlFingerKnuckles(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        boolean local = state.id == client.player.getId();
        boolean speech = AnimeAnimationClient.getSpeechEmoteTicks() > 0
                && state.id == AnimeAnimationClient.getSpeechSpeakerId();
        boolean neckTarget = AnimeAnimationClient.getNeckSnapTicks() > 0
                && state.id == AnimeAnimationClient.getNeckSnapTargetId();

        int rightMode = AnimeAnimationClient.areFingersOpen() ? 0 : 1;
        int leftMode = rightMode;
        if (state.getMainHandItemStack().isIn(ItemTags.SWORDS)) {
            if (state.mainArm == Arm.RIGHT) rightMode = 1; else leftMode = 1;
        }
        if (state.handSwingProgress > 0.01F || (local && AutoPvPAnimationClient.getPunchTicks() > 0)) {
            if (state.mainArm == Arm.RIGHT) rightMode = 1; else leftMode = 1;
        }
        if (speech) {
            int style = AnimeAnimationClient.getSpeechEmoteType();
            if (style == 5 || style == 12) { rightMode = 0; leftMode = 0; }
            if (style == 8 || style == 9 || style == 10) rightMode = 2;
            if (style == 4 || style == 11) { rightMode = 1; leftMode = 1; }
        }
        if (neckTarget) { rightMode = 0; leftMode = 0; }

        if (local || speech || neckTarget) {
            curlVanillaHand(model.rightArm, true, rightMode, state.age);
            curlVanillaHand(model.leftArm, false, leftMode, state.age);
        }

        if (local && ProceduralAIRigClient.isEnabled()) {
            ModelPart root = model.getRootPart();
            if (root.hasChild("arrowrip_ai_rig")) {
                ModelPart rig = root.getChild("arrowrip_ai_rig");
                ModelPart ru = rig.getChild("arrowrip_r_upper_arm");
                ModelPart lu = rig.getChild("arrowrip_l_upper_arm");
                ModelPart rh = ru.getChild("arrowrip_r_forearm").getChild("arrowrip_r_hand");
                ModelPart lh = lu.getChild("arrowrip_l_forearm").getChild("arrowrip_l_hand");
                curlRigHand(rh, true, rightMode, state.age);
                curlRigHand(lh, false, leftMode, state.age);
            }
        }
    }

    private static void curlVanillaHand(ModelPart arm, boolean right, int mode, float age) {
        String s = right ? "r" : "l";
        float wiggle = MathHelper.sin(age * 0.10F + (right ? 0.0F : 1.4F)) * 0.03F;
        for (int i = 0; i < DIGITS.length; i++) {
            String baseName = "arrowrip_" + s + "_" + DIGITS[i];
            if (!arm.hasChild(baseName)) continue;
            ModelPart base = arm.getChild(baseName);
            String tipName = baseName + "_tip";
            if (!base.hasChild(tipName)) continue;
            ModelPart tip = base.getChild(tipName);
            applyTip(tip, mode, i, wiggle);
        }
        String thumbName = "arrowrip_" + s + "_thumb";
        if (arm.hasChild(thumbName)) {
            ModelPart thumb = arm.getChild(thumbName);
            if (thumb.hasChild(thumbName + "_tip")) applyThumbTip(thumb.getChild(thumbName + "_tip"), mode, right);
        }
    }

    private static void curlRigHand(ModelPart hand, boolean right, int mode, float age) {
        String s = right ? "r" : "l";
        float wiggle = MathHelper.sin(age * 0.12F + (right ? 0.3F : 1.9F)) * 0.025F;
        for (int i = 0; i < DIGITS.length; i++) {
            String baseName = "arrowrip_" + s + "_ai_" + DIGITS[i];
            if (!hand.hasChild(baseName)) continue;
            ModelPart base = hand.getChild(baseName);
            String tipName = baseName + "_tip";
            if (!base.hasChild(tipName)) continue;
            applyTip(base.getChild(tipName), mode, i, wiggle);
        }
        String thumbName = "arrowrip_" + s + "_ai_thumb";
        if (hand.hasChild(thumbName)) {
            ModelPart thumb = hand.getChild(thumbName);
            if (thumb.hasChild(thumbName + "_tip")) applyThumbTip(thumb.getChild(thumbName + "_tip"), mode, right);
        }
    }

    private static void applyTip(ModelPart tip, int mode, int index, float wiggle) {
        if (mode == 0) {
            tip.pitch = 0.04F + wiggle * (0.7F + index * 0.08F);
            tip.yaw = 0.0F;
            tip.roll = 0.0F;
        } else if (mode == 2 && index == 3) {
            tip.pitch = 0.02F;
            tip.yaw = 0.0F;
            tip.roll = 0.0F;
        } else {
            // Strong second-knuckle curl: makes an actual blocky fist instead of four straight rods.
            tip.pitch = -1.28F - index * 0.045F;
            tip.yaw = 0.0F;
            tip.roll = 0.0F;
        }
    }

    private static void applyThumbTip(ModelPart tip, int mode, boolean right) {
        float sign = right ? 1.0F : -1.0F;
        if (mode == 0) {
            tip.pitch = -0.12F;
            tip.yaw = -sign * 0.16F;
            tip.roll = sign * 0.08F;
        } else {
            // Thumb folds across the curled fingers to finish the fist silhouette.
            tip.pitch = -1.02F;
            tip.yaw = -sign * 0.42F;
            tip.roll = sign * 0.26F;
        }
    }
}
