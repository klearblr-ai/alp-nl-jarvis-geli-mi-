package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.SwordFinisherClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Late finisher pass: gives every finisher its own full-body choreography. */
@Mixin(value = PlayerEntityModel.class, priority = 100)
public abstract class SwordFinisherPolishMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$polish24Finishers(PlayerEntityRenderState state, CallbackInfo ci) {
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

        int style = SwordFinisherClient.getFinisherStyle();
        float p = 1.0F - SwordFinisherClient.getFinisherTicks()
                / (float)Math.max(1, SwordFinisherClient.getFinisherDuration());
        p = MathHelper.clamp(p, 0.0F, 1.0F);

        if (attacker) poseAttacker(m, style, p);
        else poseTarget(m, style, p);
    }

    private static void poseAttacker(PlayerEntityModel m, int s, float p) {
        float wind = smooth(MathHelper.clamp(p / 0.24F, 0.0F, 1.0F));
        float h1 = MathHelper.sin(MathHelper.clamp((p - 0.12F) / 0.46F, 0.0F, 1.0F) * (float)Math.PI);
        float h2 = MathHelper.sin(MathHelper.clamp((p - 0.43F) / 0.42F, 0.0F, 1.0F) * (float)Math.PI);
        float settle = smooth(MathHelper.clamp((p - 0.70F) / 0.30F, 0.0F, 1.0F));
        float side = (s & 1) == 0 ? -1.0F : 1.0F;

        // clean cinematic base stance
        m.body.pitch = 0.08F + 0.08F * wind - 0.04F * settle;
        m.body.yaw = side * 0.16F * wind;
        m.body.roll = 0.0F;
        m.head.pitch = -0.07F * wind;
        m.head.yaw = -side * 0.10F * wind;
        m.head.roll = 0.0F;
        m.rightArm.pitch = -0.82F;
        m.rightArm.yaw = -side * 0.20F;
        m.rightArm.roll = side * 0.12F;
        m.leftArm.pitch = -0.42F;
        m.leftArm.yaw = side * 0.18F;
        m.leftArm.roll = -side * 0.08F;
        m.rightLeg.pitch = 0.14F;
        m.leftLeg.pitch = -0.10F;
        m.rightLeg.yaw = m.leftLeg.yaw = 0.0F;
        m.rightLeg.roll = 0.03F;
        m.leftLeg.roll = -0.03F;

        switch (s) {
            case 1 -> { // diagonal break
                m.body.yaw += side * 0.68F * h1; m.body.roll -= side * 0.12F * h1;
                m.rightArm.pitch = -2.05F + 1.36F * h1; m.rightArm.yaw = -side * (0.70F - 0.42F * h1);
                m.leftArm.pitch = -0.72F + 0.24F * h1;
            }
            case 2 -> { // reverse cut
                m.body.yaw -= side * 0.72F * h1; m.body.roll += side * 0.14F * h1;
                m.rightArm.pitch = -0.74F - 0.86F * h1; m.rightArm.yaw = side * (0.90F - 0.25F * h1);
                m.rightArm.roll = -side * 0.42F * h1;
            }
            case 3 -> { // rising fang
                m.body.pitch = 0.22F * h1; m.body.roll = -side * 0.22F * h1;
                m.rightArm.pitch = -0.42F - 1.92F * h1; m.rightArm.yaw = -side * 0.40F;
                m.rightLeg.pitch = 0.34F * h1; m.leftLeg.pitch = -0.26F * h1;
            }
            case 4 -> { // overhead crush
                m.body.pitch = 0.42F * h1; m.rightArm.pitch = -2.88F + 2.18F * h1;
                m.leftArm.pitch = -2.32F + 1.64F * h1; m.rightArm.yaw = -0.08F * side;
                m.rightLeg.pitch = 0.30F * h1; m.leftLeg.pitch = 0.18F * h1;
            }
            case 5 -> { // straight impale
                m.body.pitch = 0.48F * h1; m.body.yaw = -side * 0.16F * wind;
                m.rightArm.pitch = -1.56F; m.rightArm.yaw = -side * 0.05F; m.rightArm.roll = side * 0.04F;
                m.leftArm.pitch = -0.82F; m.rightLeg.pitch = 0.52F * h1; m.leftLeg.pitch = -0.34F * h1;
            }
            case 6 -> { // two hand thrust
                m.body.pitch = 0.52F * h1;
                m.rightArm.pitch = -1.54F; m.leftArm.pitch = -1.47F;
                m.rightArm.yaw = -0.10F; m.leftArm.yaw = 0.10F;
                m.rightLeg.pitch = 0.56F * h1; m.leftLeg.pitch = -0.38F * h1;
            }
            case 7 -> { // spin cutter
                m.body.yaw = side * ((float)Math.PI * 1.45F * smooth(MathHelper.clamp(p / 0.72F,0,1)));
                m.rightArm.pitch = -1.22F; m.rightArm.yaw = -side * 1.08F;
                m.leftArm.pitch = -0.62F; m.rightLeg.pitch = 0.18F * h1; m.leftLeg.pitch = -0.18F * h1;
            }
            case 8 -> { // kick + slash
                m.body.yaw = side * 0.52F * h1; m.body.roll = -side * 0.22F * h1;
                m.rightLeg.pitch = -1.34F * h1; m.rightLeg.yaw = -side * 0.18F * h1;
                m.leftLeg.pitch = 0.34F * h1; m.rightArm.pitch = -1.28F - 0.48F * h2;
                m.rightArm.yaw = -side * (0.72F + 0.32F * h2);
            }
            case 9 -> { // knee + pommel
                m.body.pitch = 0.30F * h1; m.rightLeg.pitch = -1.10F * h1; m.leftLeg.pitch = 0.34F * h1;
                m.rightArm.pitch = -0.64F - 0.90F * h2; m.rightArm.yaw = -side * 0.26F;
                m.leftArm.pitch = -1.04F * h2;
            }
            case 10 -> { // low stab
                m.body.pitch = 0.64F * h1; m.body.yaw = side * 0.18F;
                m.rightArm.pitch = -1.38F; m.rightArm.yaw = -side * 0.10F;
                m.rightLeg.pitch = 0.62F * h1; m.leftLeg.pitch = -0.46F * h1;
                m.head.pitch = 0.14F * h1;
            }
            case 11 -> { // cross slash
                m.body.yaw = side * (0.54F * h1 - 0.78F * h2);
                m.rightArm.pitch = -1.88F + 0.92F * h1 - 0.74F * h2;
                m.rightArm.yaw = -side * (0.72F * h1 - 0.82F * h2);
                m.leftArm.pitch = -0.68F - 0.34F * h2;
            }
            case 12 -> { // backhand
                m.body.yaw = -side * 0.82F * h1;
                m.rightArm.pitch = -0.78F - 0.82F * h1; m.rightArm.yaw = side * 1.12F * h1;
                m.rightArm.roll = -side * 0.48F * h1; m.head.yaw = side * 0.34F * h1;
            }
            case 13 -> { // disarm + pommel
                m.body.yaw = side * (0.50F * h1 - 0.28F * h2);
                m.rightArm.pitch = -1.20F - 0.62F * h1; m.rightArm.yaw = -side * 0.82F * h1;
                m.leftArm.pitch = -0.66F - 0.88F * h2; m.leftArm.yaw = side * 0.26F;
                m.rightLeg.pitch = 0.24F * h2;
            }
            case 14 -> { // elbow + slash
                m.body.yaw = side * (0.70F * h1 - 0.64F * h2);
                m.leftArm.pitch = -1.28F * h1; m.leftArm.yaw = side * 0.82F * h1;
                m.rightArm.pitch = -1.04F - 0.92F * h2; m.rightArm.yaw = -side * 0.92F * h2;
            }
            case 15 -> { // feint + thrust
                float fake = MathHelper.sin(MathHelper.clamp(p / 0.38F,0,1)*(float)Math.PI);
                m.body.yaw = side * (0.48F * fake - 0.24F * h2); m.body.pitch = 0.46F * h2;
                m.rightArm.pitch = -1.02F - 0.48F * fake - 0.34F * h2;
                m.rightArm.yaw = -side * (0.78F * fake - 0.10F * h2);
                m.rightLeg.pitch = 0.50F * h2; m.leftLeg.pitch = -0.30F * h2;
            }
            case 16 -> { // jumping cut
                float jump = MathHelper.sin(MathHelper.clamp(p / 0.72F,0,1)*(float)Math.PI);
                m.body.pitch = -0.18F * jump + 0.38F * h2; m.rightArm.pitch = -2.64F + 1.96F * h2;
                m.leftArm.pitch = -1.42F + 0.76F * h2; m.rightLeg.pitch = -0.82F * jump;
                m.leftLeg.pitch = 0.64F * jump;
            }
            case 17 -> { // sweep + stab
                m.body.yaw = side * 0.84F * h1; m.body.pitch = 0.36F * h2;
                m.rightLeg.pitch = -0.92F * h1; m.rightLeg.roll = side * 0.34F * h1;
                m.rightArm.pitch = -1.48F - 0.20F * h2; m.rightArm.yaw = -side * 0.08F;
                m.leftLeg.pitch = 0.42F * h1;
            }
            case 18 -> { // guard break
                m.body.pitch = 0.22F * h1 + 0.34F * h2; m.body.yaw = side * 0.52F * h2;
                m.leftArm.pitch = -1.34F * h1; m.rightArm.pitch = -2.42F + 1.52F * h2;
                m.rightArm.yaw = -side * 0.58F * h2;
            }
            case 19 -> { // iaido draw cut
                float snap = MathHelper.sin(MathHelper.clamp((p - 0.26F) / 0.22F,0,1)*(float)Math.PI);
                m.body.yaw = -side * 0.34F * wind + side * 0.96F * snap;
                m.rightArm.pitch = -0.32F - 1.18F * snap; m.rightArm.yaw = -side * 1.06F * snap;
                m.leftArm.pitch = -0.48F; m.head.yaw = -side * 0.24F * snap;
            }
            case 20 -> { // reverse grip stab
                m.body.pitch = 0.42F * h1; m.body.roll = side * 0.12F * h1;
                m.rightArm.pitch = -0.52F - 1.34F * h1; m.rightArm.roll = -side * 0.82F;
                m.rightArm.yaw = side * 0.22F; m.rightLeg.pitch = 0.40F * h1;
            }
            case 21 -> { // two step combo
                m.body.yaw = side * (0.64F * h1 - 0.76F * h2);
                m.rightArm.pitch = -1.92F + 1.04F * h1 - 0.58F * h2;
                m.rightArm.yaw = -side * (0.72F * h1 - 0.92F * h2);
                m.rightLeg.pitch = 0.34F * h1; m.leftLeg.pitch = 0.30F * h2;
            }
            case 22 -> { // roundhouse + thrust
                m.body.yaw = side * (1.08F * h1 - 0.22F * h2); m.body.roll = -side * 0.24F * h1;
                m.rightLeg.pitch = -1.42F * h1; m.leftLeg.pitch = 0.44F * h1;
                m.rightArm.pitch = -1.54F; m.rightArm.yaw = -side * 0.08F; m.body.pitch += 0.42F * h2;
            }
            case 23 -> { // execution overhead
                float slam = MathHelper.sin(MathHelper.clamp((p - 0.28F) / 0.46F,0,1)*(float)Math.PI);
                m.body.pitch = 0.54F * slam; m.rightArm.pitch = -2.92F + 2.22F * slam;
                m.leftArm.pitch = -2.20F + 1.54F * slam; m.rightLeg.pitch = 0.36F * slam;
                m.leftLeg.pitch = 0.22F * slam;
            }
            case 24 -> { // final anime combo
                float spin = smooth(MathHelper.clamp((p - 0.12F) / 0.58F,0,1));
                float fin = MathHelper.sin(MathHelper.clamp((p - 0.62F) / 0.30F,0,1)*(float)Math.PI);
                m.body.yaw = side * ((float)Math.PI * 1.65F * spin) - side * 0.54F * fin;
                m.body.pitch = 0.22F * h1 + 0.52F * fin;
                m.rightArm.pitch = -1.18F - 0.72F * h1 - 0.72F * fin;
                m.rightArm.yaw = -side * (1.04F * h1 + 0.16F * fin);
                m.leftArm.pitch = -0.82F - 0.38F * h2;
                m.rightLeg.pitch = -0.92F * h2 + 0.44F * fin; m.leftLeg.pitch = 0.34F * h2 - 0.30F * fin;
            }
            default -> {}
        }
    }

    private static void poseTarget(PlayerEntityModel m, int s, float p) {
        float h1 = MathHelper.sin(MathHelper.clamp((p - 0.14F) / 0.42F, 0.0F, 1.0F) * (float)Math.PI);
        float h2 = MathHelper.sin(MathHelper.clamp((p - 0.47F) / 0.38F, 0.0F, 1.0F) * (float)Math.PI);
        float fall = smooth(MathHelper.clamp((p - 0.66F) / 0.28F, 0.0F, 1.0F));
        float side = (s & 1) == 0 ? -1.0F : 1.0F;
        float recoil = MathHelper.clamp(h1 + h2 * 0.82F, 0.0F, 1.25F);

        m.body.pitch = -0.18F * recoil + (s >= 17 ? 0.66F : 0.38F) * fall;
        m.body.yaw = -side * 0.46F * h1 + side * 0.28F * h2;
        m.body.roll = side * (0.16F * h1 + 0.26F * fall);
        m.head.pitch = 0.30F * recoil + 0.22F * fall;
        m.head.yaw = side * 0.34F * h1 - side * 0.20F * h2;
        m.head.roll = side * 0.18F * recoil;
        m.rightArm.pitch = 0.30F + 0.48F * recoil + 0.28F * fall;
        m.leftArm.pitch = 0.24F + 0.42F * recoil + 0.24F * fall;
        m.rightArm.roll = 0.28F * recoil;
        m.leftArm.roll = -0.28F * recoil;
        m.rightLeg.pitch = -0.16F * h1 + 0.44F * fall;
        m.leftLeg.pitch = 0.20F * h1 + 0.32F * fall;

        switch (s) {
            case 3, 4, 16, 23 -> { m.body.pitch += 0.30F * h1; m.head.pitch += 0.24F * h1; }
            case 5, 6, 10, 15, 20, 22 -> { m.body.pitch -= 0.22F * h1; m.body.yaw += side * 0.18F * h1; }
            case 8, 9, 17, 22 -> { m.body.roll += side * 0.26F * h1; m.rightLeg.pitch += 0.30F * h1; }
            case 13, 18 -> { m.rightArm.pitch = -0.44F * h1 + 0.62F * fall; m.rightArm.roll = side * 0.78F * h1; }
            case 19 -> { m.body.yaw += side * 0.72F * h1; m.head.yaw -= side * 0.48F * h1; }
            case 24 -> { m.body.roll += side * 0.42F * h2; m.body.pitch += 0.54F * fall; m.head.roll += side * 0.30F * h2; }
            default -> {}
        }
    }

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
