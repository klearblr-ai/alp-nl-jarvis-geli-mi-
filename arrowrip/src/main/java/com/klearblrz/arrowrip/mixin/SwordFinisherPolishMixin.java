package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.SwordFinisherClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Late pass that gives all 24 finishers visibly different full-body choreography. */
@Mixin(value = PlayerEntityModel.class, priority = 100)
public abstract class SwordFinisherPolishMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$polishFinishers(PlayerEntityRenderState state, CallbackInfo ci) {
        if (!SwordFinisherClient.isFinisherActive()) return;
        boolean attacker = state.id == SwordFinisherClient.getFinisherAttackerId();
        boolean target = state.id == SwordFinisherClient.getFinisherTargetId();
        if (!attacker && !target) return;

        PlayerEntityModel m = (PlayerEntityModel)(Object)this;
        if (m.getRootPart().hasChild("arrowrip_ai_rig")) {
            m.getRootPart().getChild("arrowrip_ai_rig").visible = false;
        }
        m.rightArm.visible = m.leftArm.visible = true;
        m.rightLeg.visible = m.leftLeg.visible = true;

        int style = MathHelper.clamp(SwordFinisherClient.getFinisherStyle(), 1, 24);
        float p = 1.0F - SwordFinisherClient.getFinisherTicks()
                / (float)Math.max(1, SwordFinisherClient.getFinisherDuration());
        p = MathHelper.clamp(p, 0.0F, 1.0F);
        if (attacker) poseAttacker(m, style, p);
        else poseTarget(m, style, p);
    }

    private static void poseAttacker(PlayerEntityModel m, int s, float p) {
        float side = (s & 1) == 0 ? -1.0F : 1.0F;
        float prep = smooth(MathHelper.clamp(p / 0.22F, 0.0F, 1.0F));
        float a = MathHelper.sin(MathHelper.clamp((p - 0.10F) / 0.46F, 0.0F, 1.0F) * (float)Math.PI);
        float b = MathHelper.sin(MathHelper.clamp((p - 0.43F) / 0.42F, 0.0F, 1.0F) * (float)Math.PI);
        float tier = (s - 1) / 8.0F;
        int technique = (s - 1) % 8;

        m.body.pitch = 0.06F + 0.05F * prep;
        m.body.yaw = side * 0.12F * prep;
        m.body.roll = 0.0F;
        m.head.pitch = -0.06F * prep;
        m.head.yaw = -side * 0.08F * prep;
        m.head.roll = 0.0F;
        m.rightArm.pitch = -0.78F;
        m.rightArm.yaw = -side * 0.18F;
        m.rightArm.roll = side * 0.10F;
        m.leftArm.pitch = -0.40F;
        m.leftArm.yaw = side * 0.16F;
        m.leftArm.roll = -side * 0.06F;
        m.rightLeg.pitch = 0.12F;
        m.leftLeg.pitch = -0.08F;
        m.rightLeg.yaw = m.leftLeg.yaw = 0.0F;
        m.rightLeg.roll = 0.03F;
        m.leftLeg.roll = -0.03F;

        switch (technique) {
            case 0 -> { // diagonal / iai / final diagonal
                m.body.yaw += side * (0.54F + tier * 0.10F) * a;
                m.body.roll -= side * 0.10F * a;
                m.rightArm.pitch = -2.08F + (1.24F + tier * 0.12F) * a;
                m.rightArm.yaw = -side * (0.66F - 0.30F * a);
                m.leftArm.pitch = -0.64F - 0.18F * b;
                m.rightLeg.pitch += 0.22F * a;
            }
            case 1 -> { // reverse / knee / reverse-grip
                m.body.yaw -= side * (0.64F + tier * 0.10F) * a;
                m.body.roll += side * 0.13F * a;
                m.rightArm.pitch = -0.70F - (0.76F + tier * 0.12F) * a;
                m.rightArm.yaw = side * (0.92F - 0.18F * a);
                m.rightArm.roll = -side * (0.38F + 0.08F * tier) * a;
                m.rightLeg.pitch = -0.28F * b;
            }
            case 2 -> { // rising / low stab / combo
                m.body.pitch = 0.18F * a + 0.24F * b;
                m.body.roll = -side * 0.18F * a;
                m.rightArm.pitch = -0.42F - (1.70F + tier * 0.14F) * a + 0.38F * b;
                m.rightArm.yaw = -side * (0.36F + 0.30F * b);
                m.rightLeg.pitch = 0.38F * b;
                m.leftLeg.pitch = -0.26F * b;
            }
            case 3 -> { // overhead / cross / roundhouse-thrust
                m.body.pitch = (0.34F + tier * 0.05F) * a + 0.18F * b;
                m.rightArm.pitch = -2.78F + 1.92F * a - 0.32F * b;
                m.leftArm.pitch = -2.02F + 1.18F * a;
                m.body.yaw += side * 0.30F * b;
                m.rightLeg.pitch = 0.30F * a - 0.48F * b;
                m.leftLeg.pitch = 0.16F * a + 0.22F * b;
            }
            case 4 -> { // straight thrust / backhand / execution
                m.body.pitch = (0.42F + tier * 0.06F) * a;
                m.body.yaw = -side * 0.16F * prep + side * 0.26F * b;
                m.rightArm.pitch = -1.52F - 0.16F * b;
                m.rightArm.yaw = -side * (0.08F + 0.22F * b);
                m.rightArm.roll = side * 0.04F;
                m.leftArm.pitch = -0.76F - 0.28F * tier;
                m.rightLeg.pitch = 0.52F * a;
                m.leftLeg.pitch = -0.34F * a;
            }
            case 5 -> { // two-hand thrust / disarm / final combo
                m.body.pitch = (0.46F + tier * 0.05F) * a;
                m.body.yaw += side * (0.38F * a - 0.30F * b);
                m.rightArm.pitch = -1.48F - 0.20F * b;
                m.leftArm.pitch = -1.42F + 0.18F * a - 0.30F * b;
                m.rightArm.yaw = -side * 0.12F;
                m.leftArm.yaw = side * 0.12F;
                m.rightLeg.pitch = 0.56F * a;
                m.leftLeg.pitch = -0.38F * a;
            }
            case 6 -> { // spin / elbow / jumping cut
                float spin = smooth(MathHelper.clamp((p - 0.08F) / 0.72F, 0.0F, 1.0F));
                m.body.yaw = side * (float)Math.PI * (1.15F + 0.16F * tier) * spin;
                m.body.roll = side * 0.12F * b;
                m.rightArm.pitch = -1.18F - 0.48F * b;
                m.rightArm.yaw = -side * (0.96F + 0.10F * tier);
                m.leftArm.pitch = -0.58F - 0.34F * a;
                m.rightLeg.pitch = -0.38F * a + 0.24F * b;
                m.leftLeg.pitch = 0.30F * a - 0.18F * b;
            }
            case 7 -> { // kick+slash / feint / sweep-stab
                m.body.yaw = side * (0.50F * a - 0.42F * b);
                m.body.roll = -side * 0.20F * a;
                m.rightLeg.pitch = -(1.08F + 0.12F * tier) * a + 0.36F * b;
                m.leftLeg.pitch = 0.34F * a - 0.20F * b;
                m.rightArm.pitch = -1.10F - 0.42F * a - 0.48F * b;
                m.rightArm.yaw = -side * (0.64F * a + 0.38F * b);
                m.leftArm.pitch = -0.56F - 0.28F * b;
            }
            default -> {}
        }

        // Extra signatures make styles inside the same technique family distinct.
        if (s == 13 || s == 18) { // disarm / guard break
            m.leftArm.pitch -= 0.72F * a;
            m.leftArm.yaw += side * 0.64F * a;
            m.rightArm.roll += side * 0.32F * b;
        }
        if (s == 19) { // iaido snap
            float snap = MathHelper.sin(MathHelper.clamp((p - 0.25F) / 0.22F, 0.0F, 1.0F) * (float)Math.PI);
            m.body.yaw += side * 0.82F * snap;
            m.rightArm.yaw -= side * 0.78F * snap;
            m.head.yaw -= side * 0.20F * snap;
        }
        if (s == 20) { // reverse grip
            m.rightArm.roll = -side * 0.82F;
            m.rightArm.yaw = side * 0.22F;
        }
        if (s == 22) { // roundhouse + thrust
            m.rightLeg.pitch -= 0.62F * a;
            m.body.roll -= side * 0.18F * a;
            m.body.pitch += 0.30F * b;
        }
        if (s == 23) { // execution overhead
            m.rightArm.pitch -= 0.62F * a;
            m.leftArm.pitch -= 0.50F * a;
            m.body.pitch += 0.30F * a;
        }
        if (s == 24) { // final anime combo
            m.body.yaw += side * 0.72F * b;
            m.body.pitch += 0.38F * b;
            m.rightArm.pitch -= 0.48F * b;
            m.rightLeg.pitch -= 0.42F * a;
            m.leftLeg.pitch += 0.28F * a;
        }
    }

    private static void poseTarget(PlayerEntityModel m, int s, float p) {
        float side = (s & 1) == 0 ? -1.0F : 1.0F;
        float a = MathHelper.sin(MathHelper.clamp((p - 0.13F) / 0.44F, 0.0F, 1.0F) * (float)Math.PI);
        float b = MathHelper.sin(MathHelper.clamp((p - 0.46F) / 0.39F, 0.0F, 1.0F) * (float)Math.PI);
        float fall = smooth(MathHelper.clamp((p - 0.68F) / 0.28F, 0.0F, 1.0F));
        float recoil = MathHelper.clamp(a + b * 0.75F, 0.0F, 1.2F);
        int technique = (s - 1) % 8;

        m.body.pitch = -0.16F * recoil + (s >= 17 ? 0.62F : 0.34F) * fall;
        m.body.yaw = -side * 0.42F * a + side * 0.24F * b;
        m.body.roll = side * (0.14F * a + 0.24F * fall);
        m.head.pitch = 0.27F * recoil + 0.20F * fall;
        m.head.yaw = side * 0.30F * a - side * 0.18F * b;
        m.head.roll = side * 0.16F * recoil;
        m.rightArm.pitch = 0.28F + 0.44F * recoil + 0.26F * fall;
        m.leftArm.pitch = 0.22F + 0.38F * recoil + 0.22F * fall;
        m.rightArm.roll = 0.24F * recoil;
        m.leftArm.roll = -0.24F * recoil;
        m.rightLeg.pitch = -0.14F * a + 0.40F * fall;
        m.leftLeg.pitch = 0.18F * a + 0.30F * fall;

        if (technique == 2 || technique == 3) {
            m.body.pitch += 0.24F * a;
            m.head.pitch += 0.20F * a;
        } else if (technique == 4 || technique == 5) {
            m.body.pitch -= 0.18F * a;
            m.body.yaw += side * 0.14F * a;
        } else if (technique == 7) {
            m.body.roll += side * 0.22F * a;
            m.rightLeg.pitch += 0.24F * a;
        }

        if (s == 13 || s == 18) {
            m.rightArm.pitch = -0.42F * a + 0.58F * fall;
            m.rightArm.roll = side * 0.76F * a;
        }
        if (s == 19) {
            m.body.yaw += side * 0.62F * a;
            m.head.yaw -= side * 0.42F * a;
        }
        if (s == 24) {
            m.body.roll += side * 0.38F * b;
            m.body.pitch += 0.46F * fall;
            m.head.roll += side * 0.26F * b;
        }
    }

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
