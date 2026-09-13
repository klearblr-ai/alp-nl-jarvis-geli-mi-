package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.ProceduralAIRigClient;
import com.klearblrz.arrowrip.SocialMoodClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blends social LOVE / ANGER memory into the automatic full-body rig. */
@Mixin(PlayerEntityModel.class)
public abstract class EmotionPoseMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$emotionPose(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId() || !ProceduralAIRigClient.isEnabled()) return;

        float love = MathHelper.clamp(SocialMoodClient.activeLove() / 100.0F, 0.0F, 1.0F);
        float anger = MathHelper.clamp(SocialMoodClient.activeAnger() / 100.0F, 0.0F, 1.0F);
        if (love < 0.01F && anger < 0.01F) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        float age = state.age;
        float calmWave = MathHelper.sin(age * 0.085F);
        float tenseWave = MathHelper.sin(age * 0.22F);
        float loveWeight = Math.max(0.0F, love - anger * 0.45F);
        float angerWeight = Math.max(0.0F, anger - love * 0.30F);

        // Head/torso body language remains subtle so combat clips are not destroyed.
        model.body.pitch += angerWeight * 0.105F - loveWeight * 0.025F;
        model.body.roll += calmWave * loveWeight * 0.028F + tenseWave * angerWeight * 0.018F;
        model.head.pitch -= angerWeight * 0.045F;
        model.head.roll += calmWave * loveWeight * 0.018F;

        ModelPart root = model.getRootPart();
        if (!root.hasChild("arrowrip_ai_rig")) return;
        ModelPart rig = root.getChild("arrowrip_ai_rig");
        if (!rig.visible) return;

        ModelPart ru = rig.getChild("arrowrip_r_upper_arm");
        ModelPart lu = rig.getChild("arrowrip_l_upper_arm");
        ModelPart rf = ru.getChild("arrowrip_r_forearm");
        ModelPart lf = lu.getChild("arrowrip_l_forearm");
        ModelPart rh = rf.getChild("arrowrip_r_hand");
        ModelPart lh = lf.getChild("arrowrip_l_hand");
        ModelPart rt = rig.getChild("arrowrip_r_thigh");
        ModelPart lt = rig.getChild("arrowrip_l_thigh");

        if (angerWeight > 0.01F) {
            // Tense shoulders, guarded fists and forward fighting weight.
            ru.roll += 0.16F * angerWeight;
            lu.roll -= 0.16F * angerWeight;
            ru.pitch -= 0.18F * angerWeight;
            lu.pitch -= 0.16F * angerWeight;
            rf.pitch -= (0.28F + 0.04F * tenseWave) * angerWeight;
            lf.pitch -= (0.27F - 0.04F * tenseWave) * angerWeight;
            rh.roll += 0.12F * angerWeight;
            lh.roll -= 0.12F * angerWeight;
            rt.pitch += 0.065F * angerWeight;
            lt.pitch -= 0.045F * angerWeight;
            curlDigits(rh, "r", 0.70F * angerWeight);
            curlDigits(lh, "l", 0.70F * angerWeight);
        }

        if (loveWeight > 0.01F) {
            // Open, relaxed body language with small asymmetry and breathing motion.
            ru.roll -= (0.095F + calmWave * 0.018F) * loveWeight;
            lu.roll += (0.095F - calmWave * 0.018F) * loveWeight;
            ru.pitch += 0.055F * loveWeight;
            lu.pitch -= 0.035F * loveWeight;
            rf.pitch += 0.09F * loveWeight;
            lf.pitch += 0.07F * loveWeight;
            rh.roll -= 0.055F * loveWeight;
            lh.roll += 0.055F * loveWeight;
            rt.roll -= 0.025F * loveWeight;
            lt.roll += 0.025F * loveWeight;
            curlDigits(rh, "r", 0.14F * loveWeight);
            curlDigits(lh, "l", 0.14F * loveWeight);
        }
    }

    private static void curlDigits(ModelPart hand, String side, float curl) {
        String[] names = {"pinky", "ring", "middle", "index"};
        for (String name : names) {
            String child = "arrowrip_" + side + "_ai_" + name;
            if (hand.hasChild(child)) hand.getChild(child).pitch += curl;
        }
        String thumb = "arrowrip_" + side + "_ai_thumb";
        if (hand.hasChild(thumb)) hand.getChild(thumb).pitch += curl * 0.72F;
    }
}
