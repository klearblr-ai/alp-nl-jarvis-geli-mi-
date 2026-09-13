package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AutoPvPAnimationClient;
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

/** Late pass that turns the automatic sword stab into three proper full-body lunges. */
@Mixin(value = PlayerEntityModel.class, priority = 110)
public abstract class SwordStabPolishMixin {
    private static int arrowrip$lastStabTicks;
    private static int arrowrip$stabVariant;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$betterStab(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        int ticks = AutoPvPAnimationClient.getStabTicks();
        if (ticks <= 0) {
            if (state.id == client.player.getId()) arrowrip$lastStabTicks = 0;
            return;
        }

        PlayerEntityModel m = (PlayerEntityModel)(Object)this;
        if (state.id == client.player.getId()) {
            if (ticks > arrowrip$lastStabTicks) arrowrip$stabVariant = (arrowrip$stabVariant + 1) % 3;
            arrowrip$lastStabTicks = ticks;
            applyAttacker(m, state, ticks, arrowrip$stabVariant);
            return;
        }

        if (client.targetedEntity != null && state.id == client.targetedEntity.getId()) {
            applyTarget(m, ticks, arrowrip$stabVariant);
        }
    }

    private static void applyAttacker(PlayerEntityModel m, PlayerEntityRenderState state, int ticks, int variant) {
        float p = 1.0F - ticks / 13.0F;
        p = MathHelper.clamp(p, 0.0F, 1.0F);
        float wind = smooth(MathHelper.clamp(p / 0.30F, 0.0F, 1.0F));
        float drive = smooth(MathHelper.clamp((p - 0.22F) / 0.34F, 0.0F, 1.0F));
        float recover = smooth(MathHelper.clamp((p - 0.66F) / 0.34F, 0.0F, 1.0F));
        float thrust = drive * (1.0F - recover);
        boolean right = state.mainArm == Arm.RIGHT;
        float side = right ? 1.0F : -1.0F;

        // Vanilla/full body fallback.
        m.body.pitch = 0.10F + 0.42F * thrust;
        m.body.yaw = -side * (0.30F * wind - 0.22F * thrust);
        m.body.roll = side * 0.05F * thrust;
        m.head.pitch = -0.08F + 0.10F * thrust;
        m.head.yaw = side * 0.14F * thrust;
        ModelPart arm = right ? m.rightArm : m.leftArm;
        ModelPart off = right ? m.leftArm : m.rightArm;

        if (variant == 0) { // straight chest lunge
            arm.pitch = -1.48F - 0.18F * thrust;
            arm.yaw = -side * (0.44F * wind - 0.36F * thrust);
            arm.roll = side * 0.10F;
            off.pitch = -0.82F + 0.20F * thrust;
            off.yaw = side * 0.22F;
            m.rightLeg.pitch = right ? 0.52F * thrust : -0.34F * thrust;
            m.leftLeg.pitch = right ? -0.34F * thrust : 0.52F * thrust;
        } else if (variant == 1) { // two-hand power thrust
            arm.pitch = -1.54F;
            arm.yaw = -side * 0.08F;
            arm.roll = side * 0.03F;
            off.pitch = -1.46F + 0.12F * wind;
            off.yaw = side * 0.12F;
            off.roll = -side * 0.04F;
            m.body.pitch += 0.14F * thrust;
            m.rightLeg.pitch = right ? 0.58F * thrust : -0.38F * thrust;
            m.leftLeg.pitch = right ? -0.38F * thrust : 0.58F * thrust;
        } else { // low reverse/upward stab
            m.body.pitch = 0.28F + 0.44F * thrust;
            m.body.roll = -side * 0.12F * thrust;
            arm.pitch = -0.58F - 1.04F * thrust;
            arm.yaw = side * (0.30F * wind - 0.18F * thrust);
            arm.roll = -side * (0.56F - 0.20F * thrust);
            off.pitch = -0.58F;
            off.yaw = -side * 0.22F;
            m.rightLeg.pitch = right ? 0.46F * thrust : -0.26F * thrust;
            m.leftLeg.pitch = right ? -0.26F * thrust : 0.46F * thrust;
        }

        // Articulated rig override when AUTO PLAY FULL BODY is showing the split limbs.
        ModelPart root = m.getRootPart();
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

        ModelPart wu = right ? ru : lu;
        ModelPart wf = right ? rf : lf;
        ModelPart wh = right ? rh : lh;
        ModelPart ou = right ? lu : ru;
        ModelPart of = right ? lf : rf;

        rig.yaw = m.body.yaw * 0.42F;
        rig.roll = m.body.roll * 0.55F;
        if (variant == 0) {
            wu.pitch = -1.18F - 0.36F * thrust;
            wu.yaw = -side * (0.42F * wind - 0.28F * thrust);
            wf.pitch = -0.54F + 0.44F * thrust;
            wf.yaw = -side * 0.08F;
            wh.pitch = -0.10F * thrust;
            wh.roll = side * 0.12F;
            ou.pitch = -0.52F; of.pitch = -0.72F;
        } else if (variant == 1) {
            wu.pitch = -1.18F - 0.30F * thrust;
            wf.pitch = -0.24F + 0.18F * thrust;
            ou.pitch = -1.08F - 0.22F * thrust;
            of.pitch = -0.38F + 0.12F * thrust;
            wu.yaw = -side * 0.10F; ou.yaw = side * 0.10F;
            wh.pitch = -0.08F; wh.roll = side * 0.04F;
        } else {
            wu.pitch = -0.38F - 1.02F * thrust;
            wu.yaw = side * 0.22F;
            wu.roll = -side * 0.36F;
            wf.pitch = -0.72F + 0.48F * thrust;
            wf.roll = -side * 0.26F;
            wh.roll = -side * 0.34F;
            ou.pitch = -0.48F; of.pitch = -0.64F;
        }

        rt.pitch = right ? 0.54F * thrust : -0.34F * thrust;
        lt.pitch = right ? -0.34F * thrust : 0.54F * thrust;
        rs.pitch = right ? 0.36F * thrust : 0.14F * thrust;
        ls.pitch = right ? 0.14F * thrust : 0.36F * thrust;
        rfoot.pitch = -rs.pitch * 0.28F;
        lfoot.pitch = -ls.pitch * 0.28F;
    }

    private static void applyTarget(PlayerEntityModel m, int ticks, int variant) {
        float p = 1.0F - ticks / 13.0F;
        float hit = MathHelper.sin(MathHelper.clamp((p - 0.27F) / 0.52F, 0.0F, 1.0F) * (float)Math.PI);
        float dir = variant == 2 ? -1.0F : 1.0F;
        m.body.pitch = -0.18F * hit + (variant == 2 ? 0.16F : 0.0F);
        m.body.yaw += dir * 0.24F * hit;
        m.body.roll += dir * 0.10F * hit;
        m.head.pitch = 0.22F * hit;
        m.head.yaw -= dir * 0.28F * hit;
        m.rightArm.pitch += 0.34F * hit;
        m.leftArm.pitch += 0.28F * hit;
        m.rightLeg.pitch -= 0.12F * hit;
        m.leftLeg.pitch += 0.16F * hit;
    }

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
