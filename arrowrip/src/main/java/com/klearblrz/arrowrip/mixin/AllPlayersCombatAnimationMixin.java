package com.klearblrz.arrowrip.mixin;

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
 * Reinterprets vanilla replicated swing state for remote players as richer client-side
 * full-body combat animation. No server support is required: unmodded remote players
 * still send ordinary movement/swing state, which this client renders as punch, kick,
 * slash or stab choreography locally.
 */
@Mixin(value = PlayerEntityModel.class, priority = 260)
public abstract class AllPlayersCombatAnimationMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$allPlayersCombat(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ProceduralAIRigClient.isEnabled() || client.player == null) return;
        if (state.id == client.player.getId()) return; // local player already has the full AI timeline.

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        ModelPart root = model.getRootPart();
        if (!root.hasChild("arrowrip_ai_rig")) return;
        ModelPart rig = root.getChild("arrowrip_ai_rig");

        // Renderer models are reused; always restore vanilla visibility before deciding
        // whether this particular remote player needs the articulated rig this frame.
        model.rightArm.visible = true;
        model.leftArm.visible = true;
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        rig.visible = false;

        float swing = state.handSwingProgress;
        if (state.spectator || swing <= 0.001F) return;

        rig.visible = true;
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

        reset(rig); reset(ru); reset(lu); reset(rf); reset(lf); reset(rh); reset(lh);
        reset(rt); reset(lt); reset(rs); reset(ls); reset(rfoot); reset(lfoot);

        boolean rightMain = state.mainArm == Arm.RIGHT;
        float side = rightMain ? 1.0F : -1.0F;
        ModelPart weaponUpper = rightMain ? ru : lu;
        ModelPart weaponFore = rightMain ? rf : lf;
        ModelPart weaponHand = rightMain ? rh : lh;
        ModelPart offUpper = rightMain ? lu : ru;
        ModelPart offFore = rightMain ? lf : rf;

        float p = MathHelper.clamp(swing, 0.0F, 1.0F);
        float hit = MathHelper.sin(p * (float)Math.PI);
        float wind = MathHelper.sin(Math.min(1.0F, p * 1.6F) * (float)Math.PI * 0.5F);
        boolean sword = state.getMainHandItemStack().isIn(ItemTags.SWORDS);

        // Stable style selector for the duration of a swing. Remote vanilla packets do
        // not tell us "kick" or "stab", so this client intentionally choreographs them.
        int beat = Math.max(0, (int)((state.age - p * 6.0F) / 7.0F));
        int style = Math.floorMod(state.id * 31 + beat, sword ? 4 : 3);

        if (sword) {
            if (style == 0) {
                // Full-body thrust / sword stab.
                rig.pitch = 0.10F * hit;
                model.body.pitch += 0.18F * hit;
                model.body.yaw += side * 0.18F * hit;
                weaponUpper.pitch = -1.18F - 0.42F * hit;
                weaponUpper.yaw = -side * (0.18F + 0.10F * hit);
                weaponFore.pitch = -0.48F + 0.38F * hit;
                weaponFore.yaw = -side * 0.08F;
                weaponHand.pitch = -0.12F * hit;
                offUpper.pitch = -0.42F;
                offFore.pitch = -0.72F;
                rt.pitch = rightMain ? 0.30F * hit : -0.08F * hit;
                lt.pitch = rightMain ? -0.08F * hit : 0.30F * hit;
                rs.pitch = 0.16F + 0.18F * hit;
                ls.pitch = 0.12F + 0.12F * hit;
            } else {
                // Three slash planes: diagonal, reverse and horizontal/spin-like.
                float plane = style == 1 ? 1.0F : (style == 2 ? -1.0F : 0.15F);
                model.body.yaw += side * hit * (0.30F + 0.22F * Math.abs(plane));
                model.body.roll -= side * plane * hit * 0.08F;
                rig.yaw -= side * hit * 0.10F;
                weaponUpper.pitch = -0.72F - hit * 1.00F;
                weaponUpper.yaw = -side * (0.24F + plane * hit * 0.64F);
                weaponUpper.roll = side * (0.10F + plane * hit * 0.24F);
                weaponFore.pitch = -0.54F - hit * 0.46F;
                weaponFore.roll = side * plane * hit * 0.30F;
                weaponHand.roll = side * plane * hit * 0.34F;
                offUpper.pitch = -0.30F + hit * 0.18F;
                offFore.pitch = -0.54F;
                rt.pitch = 0.10F * hit;
                lt.pitch = -0.07F * hit;
            }
            curlHand(weaponHand, rightMain, 0.96F);
            curlHand(rightMain ? lh : rh, !rightMain, 0.62F);
        } else if (style == 0 || style == 1) {
            // Alternating full-body punches.
            boolean rightPunch = style == 0;
            float punchSide = rightPunch ? 1.0F : -1.0F;
            ModelPart pu = rightPunch ? ru : lu;
            ModelPart pf = rightPunch ? rf : lf;
            ModelPart ph = rightPunch ? rh : lh;
            ModelPart guardU = rightPunch ? lu : ru;
            ModelPart guardF = rightPunch ? lf : rf;

            model.body.yaw += punchSide * 0.52F * hit;
            model.body.roll -= punchSide * 0.055F * hit;
            pu.pitch = -0.72F - 0.86F * hit;
            pu.yaw = -punchSide * (0.18F + 0.18F * hit);
            pf.pitch = -0.86F + 0.74F * hit;
            ph.roll = punchSide * 0.12F * hit;
            guardU.pitch = -0.58F;
            guardF.pitch = -0.82F;
            rt.pitch = rightPunch ? 0.12F * hit : -0.05F * hit;
            lt.pitch = rightPunch ? -0.05F * hit : 0.12F * hit;
            curlHand(ph, rightPunch, 1.0F);
            curlHand(rightPunch ? lh : rh, !rightPunch, 0.90F);
        } else {
            // Kick: hip -> knee -> ankle chain plus counter-balanced upper body.
            boolean rightKick = ((state.id + beat) & 1) == 0;
            float kickSide = rightKick ? 1.0F : -1.0F;
            ModelPart thigh = rightKick ? rt : lt;
            ModelPart shin = rightKick ? rs : ls;
            ModelPart foot = rightKick ? rfoot : lfoot;
            ModelPart baseThigh = rightKick ? lt : rt;
            ModelPart baseShin = rightKick ? ls : rs;

            model.body.yaw -= kickSide * 0.34F * hit;
            model.body.roll -= kickSide * 0.20F * hit;
            rig.roll += kickSide * 0.08F * hit;
            thigh.pitch = -0.52F - 0.92F * hit;
            thigh.yaw = -kickSide * 0.24F * hit;
            thigh.roll = kickSide * 0.12F * hit;
            shin.pitch = 0.96F * (1.0F - hit) - 0.16F * hit;
            foot.pitch = 0.38F * hit;
            foot.yaw = -kickSide * 0.10F * hit;
            baseThigh.pitch = 0.18F * hit;
            baseShin.pitch = 0.28F * hit;
            ru.pitch = -0.48F - 0.12F * wind;
            lu.pitch = -0.48F - 0.12F * wind;
            rf.pitch = -0.70F;
            lf.pitch = -0.70F;
            ru.roll = 0.14F;
            lu.roll = -0.14F;
            curlHand(rh, true, 0.94F);
            curlHand(lh, false, 0.94F);
        }
    }

    private static void reset(ModelPart p) {
        p.pitch = 0.0F;
        p.yaw = 0.0F;
        p.roll = 0.0F;
    }

    private static void curlHand(ModelPart hand, boolean right, float curl) {
        String side = right ? "r" : "l";
        String[] digits = {"pinky", "ring", "middle", "index"};
        for (int i = 0; i < digits.length; i++) {
            String name = "arrowrip_" + side + "_ai_" + digits[i];
            if (hand.hasChild(name)) {
                ModelPart f = hand.getChild(name);
                f.pitch = -0.10F - curl * (0.90F + i * 0.03F);
                f.yaw = 0.0F;
                f.roll = 0.0F;
            }
        }
        String thumbName = "arrowrip_" + side + "_ai_thumb";
        if (hand.hasChild(thumbName)) {
            ModelPart thumb = hand.getChild(thumbName);
            thumb.pitch = -0.20F - curl * 0.66F;
            thumb.yaw = (right ? 1.0F : -1.0F) * (0.48F - curl * 0.24F);
            thumb.roll = (right ? 1.0F : -1.0F) * (0.24F + curl * 0.18F);
        }
    }
}
