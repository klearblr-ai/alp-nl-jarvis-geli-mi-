package com.klearblrz.arrowrip.mixin;

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

/**
 * Final polish layer for the procedural rig.  This is intentionally not an authored
 * animation clip: it continuously adds spine/hip counter-rotation, weight transfer,
 * independent elbow/wrist/finger motion, ankle compensation and small cinematic
 * gestures while the player is idle.  Combat/movement impulses remain driven by the
 * live game state and blend on top of these motions.
 */
@Mixin(value = PlayerEntityModel.class, priority = 200)
public abstract class AutoPlayFullBodyMixin {
    private static final String[] DIGITS = {"pinky", "ring", "middle", "index"};

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$autoPlayFullBody(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ProceduralAIRigClient.isEnabled() || client.player == null || state.id != client.player.getId()) return;
        if (client.options.getPerspective().isFirstPerson() || state.spectator) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
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
        ModelPart rs = rt.getChild("arrowrip_r_shin");
        ModelPart ls = lt.getChild("arrowrip_l_shin");
        ModelPart rfoot = rs.getChild("arrowrip_r_foot");
        ModelPart lfoot = ls.getChild("arrowrip_l_foot");

        float age = state.age;
        float stride = MathHelper.clamp(state.limbSwingAmplitude * 1.45F, 0.0F, 1.0F);
        float energy = ProceduralAIRigClient.energy();
        float combat = ProceduralAIRigClient.combat();
        float focus = ProceduralAIRigClient.focus();
        float strain = ProceduralAIRigClient.strain();
        float turn = ProceduralAIRigClient.turnVelocity();
        float slow = MathHelper.sin(age * 0.037F);
        float slow2 = MathHelper.sin(age * 0.051F + 1.3F);
        float mid = MathHelper.sin(age * 0.091F + 0.6F);
        float micro = MathHelper.sin(age * 0.173F + MathHelper.sin(age * 0.029F));

        boolean sword = state.getMainHandItemStack().isIn(ItemTags.SWORDS);
        boolean rightMain = state.mainArm == Arm.RIGHT;
        float side = rightMain ? 1.0F : -1.0F;
        ModelPart weaponUpper = rightMain ? ru : lu;
        ModelPart weaponFore = rightMain ? rf : lf;
        ModelPart weaponHand = rightMain ? rh : lh;
        ModelPart offUpper = rightMain ? lu : ru;
        ModelPart offFore = rightMain ? lf : rf;
        ModelPart offHand = rightMain ? lh : rh;

        boolean action = state.handSwingProgress > 0.001F
                || AutoPvPAnimationClient.getStabTicks() > 0
                || AutoPvPAnimationClient.getPunchTicks() > 0
                || AutoPvPAnimationClient.getKickTicks() > 0
                || AutoPvPAnimationClient.getMineTicks() > 0
                || AutoPvPAnimationClient.getPlaceTicks() > 0
                || AutoPvPAnimationClient.getLandingTicks() > 0
                || AutoPvPAnimationClient.getHurtTicks() > 0
                || AutoPvPAnimationClient.getFinisherTicks() > 0;

        float grounded = client.player.isOnGround() ? 1.0F : 0.0F;
        float idle = MathHelper.clamp(1.0F - stride * 4.2F, 0.0F, 1.0F)
                * (1.0F - combat * 0.72F) * grounded * (action ? 0.10F : 1.0F);
        float locomotion = MathHelper.clamp(stride * 1.8F, 0.0F, 1.0F);

        // Blender-like torso chain illusion: chest/head and pelvis/rig rotate against each other.
        float chestTwist = (0.060F * slow + 0.026F * slow2) * idle + turn * (0.13F + 0.06F * locomotion);
        float chestRoll = (0.018F * slow2 + 0.010F * mid) * idle - turn * 0.055F;
        model.body.yaw += chestTwist;
        model.body.roll += chestRoll;
        model.body.pitch += idle * (0.018F + 0.012F * slow) + strain * 0.028F;
        model.head.yaw -= chestTwist * 1.35F;
        model.head.roll -= chestRoll * 0.72F;
        model.head.pitch += idle * (-0.018F + 0.018F * slow2) - focus * 0.025F;
        rig.yaw -= chestTwist * 0.82F;
        rig.roll -= chestRoll * 0.55F;
        rig.pitch += idle * 0.010F * slow;

        // Weight transfer through hips -> knees -> ankles, even while standing still.
        float weight = slow * idle;
        rt.roll += 0.030F * weight;
        lt.roll += 0.030F * weight;
        rt.pitch += 0.032F * Math.max(0.0F, weight);
        lt.pitch += 0.032F * Math.max(0.0F, -weight);
        rs.pitch += 0.055F * Math.max(0.0F, weight);
        ls.pitch += 0.055F * Math.max(0.0F, -weight);
        rfoot.pitch -= 0.030F * Math.max(0.0F, weight);
        lfoot.pitch -= 0.030F * Math.max(0.0F, -weight);
        rfoot.roll -= 0.018F * weight;
        lfoot.roll -= 0.018F * weight;

        // Shoulder/elbow/wrist are deliberately out of phase so arms never look like rigid sticks.
        ru.roll += idle * (0.025F + 0.018F * slow2);
        lu.roll -= idle * (0.025F - 0.018F * slow2);
        ru.yaw += idle * 0.020F * mid;
        lu.yaw -= idle * 0.020F * mid;
        rf.pitch += idle * (-0.045F - 0.025F * slow);
        lf.pitch += idle * (-0.045F + 0.025F * slow);
        rf.yaw += idle * 0.022F * slow2;
        lf.yaw -= idle * 0.022F * slow2;
        rh.roll += idle * (0.050F * micro + 0.020F * slow);
        lh.roll -= idle * (0.050F * micro - 0.020F * slow);
        rh.yaw += idle * 0.035F * slow2;
        lh.yaw -= idle * 0.035F * slow2;

        // Better moving body mechanics: pelvis follows feet while chest counter-rotates.
        if (locomotion > 0.01F) {
            float step = MathHelper.sin(state.limbSwingAnimationProgress * 0.72F);
            rig.yaw += step * 0.065F * locomotion;
            model.body.yaw -= step * 0.050F * locomotion;
            model.body.roll += step * 0.018F * locomotion;
            model.head.yaw += step * 0.028F * locomotion;
            rfoot.pitch += Math.max(0.0F, step) * 0.075F * locomotion;
            lfoot.pitch += Math.max(0.0F, -step) * 0.075F * locomotion;
        }

        // Air pose is solved from vertical velocity, not a fixed jump animation.
        if (!client.player.isOnGround()) {
            float vy = MathHelper.clamp((float)client.player.getVelocity().y * 2.8F, -1.0F, 1.0F);
            float fall = Math.max(0.0F, -vy);
            float rise = Math.max(0.0F, vy);
            rt.pitch += 0.22F + 0.28F * rise;
            lt.pitch -= 0.10F + 0.16F * rise;
            rs.pitch += 0.38F + 0.34F * fall;
            ls.pitch += 0.24F + 0.24F * fall;
            ru.roll += 0.10F * fall;
            lu.roll -= 0.10F * fall;
            rf.pitch -= 0.14F * fall;
            lf.pitch -= 0.14F * fall;
            model.body.pitch += 0.08F * fall - 0.05F * rise;
        }

        // Cinematic auto-play gesture cycle while idle. It softly fades in/out and never blocks gameplay.
        float cycle = (age % 240.0F) / 240.0F;
        float shoulderRoll = idle * bell(cycle, 0.20F, 0.12F);
        float gesture = idle * bell(cycle, 0.43F, 0.13F);
        float flourish = idle * bell(cycle, 0.67F, 0.15F);
        float settle = idle * bell(cycle, 0.88F, 0.11F);

        // Small shoulder roll / neck glance.
        ru.pitch -= shoulderRoll * 0.12F;
        lu.pitch += shoulderRoll * 0.08F;
        ru.roll += shoulderRoll * 0.11F;
        lu.roll -= shoulderRoll * 0.07F;
        rf.pitch -= shoulderRoll * 0.18F;
        model.head.yaw += shoulderRoll * 0.15F;
        model.head.roll -= shoulderRoll * 0.035F;

        if (sword) {
            // Procedural sword flourish: shoulder, elbow, wrist and spine all participate.
            weaponUpper.pitch -= flourish * 0.52F;
            weaponUpper.yaw += side * flourish * 0.58F;
            weaponUpper.roll += side * flourish * 0.33F;
            weaponFore.pitch -= flourish * 0.46F;
            weaponFore.yaw -= side * flourish * 0.24F;
            weaponHand.roll += side * flourish * 0.62F;
            weaponHand.yaw += side * flourish * 0.18F;
            offUpper.pitch -= flourish * 0.18F;
            offFore.pitch -= flourish * 0.22F;
            model.body.yaw -= side * flourish * 0.20F;
            model.body.roll += side * flourish * 0.045F;
            rt.pitch += flourish * 0.09F;
            lt.pitch -= flourish * 0.055F;
        } else {
            // Natural hand gesture / fist flex for empty-hand movie-idle.
            ru.pitch -= gesture * 0.24F;
            rf.pitch -= gesture * 0.44F;
            rh.yaw -= gesture * 0.12F;
            lu.pitch += gesture * 0.08F;
            lf.pitch -= gesture * 0.10F;
            model.body.yaw += gesture * 0.08F;
            model.head.yaw -= gesture * 0.10F;
        }

        // Settle back into a confident asymmetric stance instead of snapping to neutral.
        rt.pitch += settle * 0.055F;
        lt.pitch -= settle * 0.035F;
        ru.pitch -= settle * 0.055F;
        lu.pitch += settle * 0.025F;
        model.body.roll += settle * 0.018F;

        // Fingers are part of the articulated AI rig: independent curl + tiny breathing motion.
        float baseCurl = sword ? 0.92F : MathHelper.clamp(0.34F + combat * 0.48F + gesture * 0.42F, 0.0F, 1.0F);
        float offCurl = MathHelper.clamp(0.28F + combat * 0.52F + flourish * 0.28F, 0.0F, 1.0F);
        if (rightMain) {
            poseHand(rh, true, baseCurl, sword ? 0.02F : 0.16F * (1.0F - baseCurl), age);
            poseHand(lh, false, offCurl, 0.18F * (1.0F - offCurl), age + 3.0F);
        } else {
            poseHand(lh, false, baseCurl, sword ? 0.02F : 0.16F * (1.0F - baseCurl), age);
            poseHand(rh, true, offCurl, 0.18F * (1.0F - offCurl), age + 3.0F);
        }
    }

    private static void poseHand(ModelPart hand, boolean right, float curl, float splay, float age) {
        String side = right ? "r" : "l";
        float sign = right ? 1.0F : -1.0F;
        for (int i = 0; i < DIGITS.length; i++) {
            String name = "arrowrip_" + side + "_ai_" + DIGITS[i];
            if (!hand.hasChild(name)) continue;
            ModelPart finger = hand.getChild(name);
            float individual = MathHelper.sin(age * 0.095F + i * 0.72F) * 0.045F;
            finger.pitch = -0.10F - curl * (0.92F + i * 0.035F) + individual * (1.0F - curl);
            finger.yaw = sign * splay * (i - 1.5F) * 0.28F;
            finger.roll = sign * 0.018F * MathHelper.sin(age * 0.071F + i);
        }
        String thumbName = "arrowrip_" + side + "_ai_thumb";
        if (hand.hasChild(thumbName)) {
            ModelPart thumb = hand.getChild(thumbName);
            thumb.pitch = -0.20F - curl * 0.68F;
            thumb.yaw = sign * (0.48F - curl * 0.24F);
            thumb.roll = sign * (0.24F + curl * 0.18F);
        }
    }

    private static float bell(float x, float center, float width) {
        float d = Math.abs(x - center);
        float t = MathHelper.clamp(1.0F - d / Math.max(0.001F, width), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
