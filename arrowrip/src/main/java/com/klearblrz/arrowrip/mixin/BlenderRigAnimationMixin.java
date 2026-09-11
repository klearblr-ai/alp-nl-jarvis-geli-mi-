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
public abstract class BlenderRigAnimationMixin {
    private static final String[] DIGITS = {"pinky", "ring", "middle", "index"};

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$proceduralBlenderRig(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        ModelPart root = model.getRootPart();
        if (!root.hasChild("arrowrip_ai_rig")) return;

        ModelPart rig = root.getChild("arrowrip_ai_rig");
        boolean thirdPerson = !client.options.getPerspective().isFirstPerson();
        rig.visible = thirdPerson && !state.spectator;
        if (!rig.visible) return;

        // Replace only the local skin limbs. Armor/equipment layers keep their own vanilla model.
        model.rightArm.visible = false;
        model.leftArm.visible = false;
        model.rightLeg.visible = false;
        model.leftLeg.visible = false;

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

        resetBone(rig); resetBone(ru); resetBone(lu); resetBone(rf); resetBone(lf);
        resetBone(rh); resetBone(lh); resetBone(rt); resetBone(lt); resetBone(rs); resetBone(ls);
        resetBone(rfoot); resetBone(lfoot);

        float age = state.age;
        float stride = MathHelper.clamp(state.limbSwingAmplitude * 1.35F, 0.0F, 1.0F);
        float phase = state.limbSwingAnimationProgress * 0.72F;
        float energy = ProceduralAIRigClient.energy();
        float combat = ProceduralAIRigClient.combat();
        float focus = ProceduralAIRigClient.focus();
        float strain = ProceduralAIRigClient.strain();
        float turn = ProceduralAIRigClient.turnVelocity();
        float breathe = MathHelper.sin(age * (0.065F + strain * 0.045F));
        float micro = MathHelper.sin(age * 0.113F + MathHelper.sin(age * 0.031F) * 1.7F);

        // The rig inherits only part of the torso rotation so hips and shoulders can counter-rotate.
        rig.pitch = model.body.pitch * 0.38F + 0.025F * breathe;
        rig.yaw = model.body.yaw * 0.46F + turn * 0.16F;
        rig.roll = model.body.roll * 0.32F - turn * 0.055F;

        // Procedural locomotion: no authored clip, each joint is solved continuously.
        float rStep = MathHelper.cos(phase) * stride;
        float lStep = MathHelper.cos(phase + (float)Math.PI) * stride;
        float armScale = 0.72F + 0.28F * energy;
        ru.pitch = lStep * armScale + 0.035F * breathe;
        lu.pitch = rStep * armScale - 0.035F * breathe;
        ru.roll = 0.08F + turn * 0.07F;
        lu.roll = -0.08F + turn * 0.07F;
        rf.pitch = -0.12F - Math.max(0.0F, -lStep) * 0.52F - combat * 0.20F;
        lf.pitch = -0.12F - Math.max(0.0F, -rStep) * 0.52F - combat * 0.20F;
        rh.roll = 0.035F * micro;
        lh.roll = -0.035F * micro;

        rt.pitch = rStep * (0.92F + 0.26F * energy);
        lt.pitch = lStep * (0.92F + 0.26F * energy);
        rt.roll = 0.025F + turn * 0.035F;
        lt.roll = -0.025F + turn * 0.035F;
        rs.pitch = 0.08F + Math.max(0.0F, -rStep) * 0.74F;
        ls.pitch = 0.08F + Math.max(0.0F, -lStep) * 0.74F;
        rfoot.pitch = -rs.pitch * 0.36F + Math.max(0.0F, rStep) * 0.16F;
        lfoot.pitch = -ls.pitch * 0.36F + Math.max(0.0F, lStep) * 0.16F;

        // Idle weight shift and breathing remain asymmetric to avoid a mannequin feel.
        if (stride < 0.045F && client.player.isOnGround()) {
            rt.pitch = 0.06F + 0.025F * breathe;
            lt.pitch = -0.035F - 0.018F * breathe;
            rs.pitch = 0.08F;
            ls.pitch = 0.13F + 0.025F * micro;
            ru.pitch = 0.08F + 0.025F * breathe;
            lu.pitch = -0.03F - 0.018F * breathe;
            rf.pitch = -0.16F - 0.035F * micro;
            lf.pitch = -0.10F + 0.025F * micro;
        }

        // Sprint posture is generated by energy rather than a fixed sprint clip.
        if (client.player.isSprinting() && stride > 0.05F) {
            model.body.pitch = Math.max(model.body.pitch, 0.30F + energy * 0.14F);
            model.head.pitch -= 0.10F + 0.05F * energy;
            ru.pitch *= 1.34F;
            lu.pitch *= 1.34F;
            rt.pitch *= 1.20F;
            lt.pitch *= 1.20F;
            rs.pitch += 0.12F * Math.max(0.0F, -rStep);
            ls.pitch += 0.12F * Math.max(0.0F, -lStep);
        }

        boolean sword = state.getMainHandItemStack().isIn(ItemTags.SWORDS);
        boolean rightMain = state.mainArm == Arm.RIGHT;
        ModelPart weaponUpper = rightMain ? ru : lu;
        ModelPart weaponFore = rightMain ? rf : lf;
        ModelPart weaponHand = rightMain ? rh : lh;
        ModelPart offUpper = rightMain ? lu : ru;
        ModelPart offFore = rightMain ? lf : rf;
        float side = rightMain ? 1.0F : -1.0F;

        // AI combat intent continuously blends into the gait instead of snapping to a stored stance.
        if (combat > 0.02F) {
            if (sword) {
                weaponUpper.pitch = lerp(weaponUpper.pitch, -0.78F - focus * 0.18F, combat);
                weaponUpper.yaw = lerp(weaponUpper.yaw, -side * (0.30F + focus * 0.22F), combat);
                weaponUpper.roll = lerp(weaponUpper.roll, side * 0.12F, combat);
                weaponFore.pitch = lerp(weaponFore.pitch, -0.64F - focus * 0.22F, combat);
                weaponFore.yaw = lerp(weaponFore.yaw, -side * 0.12F, combat);
                weaponHand.roll = lerp(weaponHand.roll, side * 0.18F, combat);
                offUpper.pitch = lerp(offUpper.pitch, -0.34F, combat * 0.78F);
                offFore.pitch = lerp(offFore.pitch, -0.56F, combat * 0.72F);
            } else {
                ru.pitch = lerp(ru.pitch, -0.58F, combat);
                lu.pitch = lerp(lu.pitch, -0.54F, combat);
                rf.pitch = lerp(rf.pitch, -0.82F, combat);
                lf.pitch = lerp(lf.pitch, -0.78F, combat);
                ru.yaw = lerp(ru.yaw, -0.24F, combat);
                lu.yaw = lerp(lu.yaw, 0.24F, combat);
            }
            rt.pitch += 0.10F * combat;
            lt.pitch -= 0.07F * combat;
        }

        // Vanilla swing progress acts only as an impulse; slash plane comes from live turn/focus/micro-motion.
        if (state.handSwingProgress > 0.001F) {
            float p = state.handSwingProgress;
            float impulse = MathHelper.sin(p * (float)Math.PI);
            float plane = MathHelper.clamp(turn * 0.65F + MathHelper.sin(age * 0.17F) * 0.35F, -1.0F, 1.0F);
            model.body.yaw += side * impulse * (0.30F + 0.22F * Math.abs(plane));
            model.body.roll -= side * impulse * 0.06F * plane;
            weaponUpper.pitch -= impulse * (0.92F + 0.30F * focus);
            weaponUpper.yaw -= side * impulse * (0.48F + 0.42F * plane);
            weaponFore.pitch -= impulse * (0.58F + 0.28F * energy);
            weaponFore.roll += side * impulse * (0.22F + 0.26F * plane);
            weaponHand.roll += side * impulse * 0.28F;
            offUpper.pitch += impulse * 0.24F;
        }

        // Existing event detector feeds impulses, while the articulated rig decides the joints.
        if (AutoPvPAnimationClient.getStabTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getStabTicks() / 13.0F;
            float thrust = MathHelper.sin(MathHelper.clamp(p, 0.0F, 1.0F) * (float)Math.PI);
            model.body.pitch += 0.24F * thrust;
            weaponUpper.pitch = lerp(weaponUpper.pitch, -1.34F, thrust);
            weaponFore.pitch = lerp(weaponFore.pitch, -0.18F, thrust);
            weaponFore.yaw = lerp(weaponFore.yaw, 0.0F, thrust);
            weaponHand.pitch = -0.12F * thrust;
            rt.pitch += (rightMain ? 0.28F : -0.08F) * thrust;
            lt.pitch += (rightMain ? -0.08F : 0.28F) * thrust;
        }

        if (AutoPvPAnimationClient.getPunchTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getPunchTicks() / 9.0F;
            float hit = MathHelper.sin(MathHelper.clamp(p, 0.0F, 1.0F) * (float)Math.PI);
            weaponUpper.pitch = -1.18F - 0.38F * hit;
            weaponUpper.yaw = -side * 0.18F;
            weaponFore.pitch = -0.08F - 0.16F * hit;
            model.body.yaw += side * 0.58F * hit;
            offUpper.pitch = -0.54F;
            offFore.pitch = -0.78F;
        }

        if (AutoPvPAnimationClient.getKickTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getKickTicks() / 15.0F;
            float kick = MathHelper.sin(MathHelper.clamp(p, 0.0F, 1.0F) * (float)Math.PI);
            boolean rightKick = MathHelper.sin(age * 0.37F) >= 0.0F;
            ModelPart thigh = rightKick ? rt : lt;
            ModelPart shin = rightKick ? rs : ls;
            ModelPart foot = rightKick ? rfoot : lfoot;
            thigh.pitch = -0.72F - 0.64F * kick;
            thigh.yaw = (rightKick ? -1.0F : 1.0F) * 0.18F * kick;
            shin.pitch = 0.92F * (1.0F - kick) - 0.18F * kick;
            foot.pitch = 0.34F * kick;
            model.body.roll += (rightKick ? -1.0F : 1.0F) * 0.18F * kick;
            ru.pitch = -0.52F; lu.pitch = -0.52F;
            rf.pitch = -0.72F; lf.pitch = -0.72F;
        }

        if (AutoPvPAnimationClient.getMineTicks() > 0) {
            float hit = (MathHelper.sin(age * 0.76F) + 1.0F) * 0.5F;
            model.body.pitch = 0.22F + hit * 0.10F;
            weaponUpper.pitch = -2.10F + hit * 1.24F;
            weaponFore.pitch = -0.44F - hit * 0.52F;
            weaponHand.pitch = -0.18F * hit;
            offUpper.pitch = -1.12F + hit * 0.44F;
            offFore.pitch = -0.58F;
        }

        if (AutoPvPAnimationClient.getPlaceTicks() > 0) {
            float reach = 0.78F + 0.10F * MathHelper.sin(age * 0.48F);
            model.body.pitch = 0.38F;
            weaponUpper.pitch = -1.24F * reach;
            weaponFore.pitch = -0.30F;
            weaponHand.pitch = 0.12F;
            rt.pitch = 0.24F; lt.pitch = -0.18F;
        }

        if (AutoPvPAnimationClient.getLandingTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getLandingTicks() / 10.0F;
            float bend = 1.0F - smooth(MathHelper.clamp(p, 0.0F, 1.0F));
            rt.pitch += 0.48F * bend; lt.pitch += 0.48F * bend;
            rs.pitch += 0.82F * bend; ls.pitch += 0.82F * bend;
            rfoot.pitch -= 0.34F * bend; lfoot.pitch -= 0.34F * bend;
            model.body.pitch += 0.28F * bend;
        }

        if (AutoPvPAnimationClient.getHurtTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getHurtTicks() / 11.0F;
            float recoil = MathHelper.sin(MathHelper.clamp(p, 0.0F, 1.0F) * (float)Math.PI);
            rig.yaw += 0.26F * recoil;
            rig.roll -= 0.11F * recoil;
            ru.pitch += 0.32F * recoil; lu.pitch += 0.20F * recoil;
            rt.pitch -= 0.12F * recoil; lt.pitch += 0.18F * recoil;
        }

        if (AutoPvPAnimationClient.getFinisherTicks() > 0) {
            float p = AutoPvPAnimationClient.getFinisherProgress();
            float wind = smooth(MathHelper.clamp(p / 0.32F, 0.0F, 1.0F));
            float impact = MathHelper.sin(MathHelper.clamp((p - 0.25F) / 0.55F, 0.0F, 1.0F) * (float)Math.PI);
            model.body.pitch = 0.18F * wind + 0.20F * impact;
            model.body.yaw = -side * 0.36F * wind + side * 0.72F * impact;
            weaponUpper.pitch = -1.22F - 0.64F * impact;
            weaponFore.pitch = -0.86F + 0.72F * impact;
            weaponUpper.yaw = -side * (0.52F * wind + 0.30F * impact);
            weaponHand.roll = side * 0.34F * impact;
            offUpper.pitch = -0.58F; offFore.pitch = -0.86F;
            rt.pitch = 0.24F * impact; lt.pitch = -0.16F * impact;
        }

        // Strain changes posture continuously instead of triggering a separate low-health clip.
        model.body.pitch += strain * (0.08F + 0.025F * breathe);
        model.head.pitch += strain * (-0.055F + 0.018F * breathe);
        ru.roll += strain * 0.08F; lu.roll -= strain * 0.08F;
        rs.pitch += strain * 0.10F; ls.pitch += strain * 0.10F;

        // Procedural fingers on the articulated hands.
        int rightHandMode = AnimeAnimationClient.areFingersOpen() ? 0 : 1;
        int leftHandMode = rightHandMode;
        if (sword) {
            if (rightMain) rightHandMode = 1; else leftHandMode = 1;
        }
        if (state.handSwingProgress > 0.01F || combat > 0.70F) {
            if (rightMain) rightHandMode = 1; else leftHandMode = 1;
        }
        if (AnimeAnimationClient.getSpeechEmoteTicks() > 0
                && AnimeAnimationClient.getSpeechSpeakerId() == state.id) {
            int style = AnimeAnimationClient.getSpeechEmoteType();
            if (style == 5 || style == 12) { rightHandMode = 0; leftHandMode = 0; }
            if (style == 8 || style == 9 || style == 10) rightHandMode = 2;
            if (style == 4 || style == 11) { rightHandMode = 1; leftHandMode = 1; }
        }
        animateHand(rh, true, rightHandMode, age, combat);
        animateHand(lh, false, leftHandMode, age, combat);
    }

    private static void animateHand(ModelPart hand, boolean right, int mode, float age, float combat) {
        String s = right ? "r" : "l";
        float sign = right ? 1.0F : -1.0F;
        float tremor = MathHelper.sin(age * 0.13F + (right ? 0.0F : 1.7F)) * (0.025F + combat * 0.018F);
        for (int i = 0; i < DIGITS.length; i++) {
            ModelPart f = hand.getChild("arrowrip_" + s + "_ai_" + DIGITS[i]);
            if (mode == 0) {
                f.pitch = tremor * (0.7F + i * 0.09F);
                f.yaw = sign * (i - 1.5F) * 0.045F;
                f.roll = sign * (i - 1.5F) * 0.014F;
            } else if (mode == 2 && i == 3) {
                f.pitch = -0.035F + tremor * 0.30F;
                f.yaw = 0.0F;
                f.roll = 0.0F;
            } else {
                f.pitch = -(1.05F + i * 0.08F) - combat * 0.08F;
                f.yaw = sign * 0.04F;
                f.roll = sign * 0.025F;
            }
        }
        ModelPart thumb = hand.getChild("arrowrip_" + s + "_ai_thumb");
        if (mode == 0) {
            thumb.pitch = -0.18F + tremor;
            thumb.yaw = -sign * 0.52F;
            thumb.roll = sign * 0.30F;
        } else {
            thumb.pitch = -0.82F;
            thumb.yaw = -sign * 0.74F;
            thumb.roll = sign * 0.58F;
        }
    }

    private static void resetBone(ModelPart part) {
        part.pitch = part.yaw = part.roll = 0.0F;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * MathHelper.clamp(t, 0.0F, 1.0F);
    }

    private static float smooth(float x) {
        return x * x * (3.0F - 2.0F * x);
    }
}
