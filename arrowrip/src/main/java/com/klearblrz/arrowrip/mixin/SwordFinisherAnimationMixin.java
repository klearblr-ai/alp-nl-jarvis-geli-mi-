package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.SwordFinisherClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerEntityModel.class, priority = 500)
public abstract class SwordFinisherAnimationMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$swordAndFinisherPose(PlayerEntityRenderState state, CallbackInfo ci) {
        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Local sword draw/sheath visual on the back.
        if (state.id == client.player.getId() && model.body.hasChild("arrowrip_back_sword")) {
            ModelPart backSword = model.body.getChild("arrowrip_back_sword");
            int dt = SwordFinisherClient.getDrawTicks();
            boolean drawn = SwordFinisherClient.isSwordDrawn();
            if (dt > 0) {
                float p = 1.0F - dt / (float)Math.max(1, SwordFinisherClient.getDrawDuration());
                float smooth = p * p * (3.0F - 2.0F * p);
                boolean drawing = SwordFinisherClient.getDrawDirection() > 0;
                backSword.visible = drawing ? p < 0.62F : p > 0.38F;
                float reach = MathHelper.sin(p * (float)Math.PI);
                model.rightArm.pitch = -1.45F - 0.55F * reach;
                model.rightArm.yaw = -0.55F + 0.30F * reach;
                model.rightArm.roll = 0.22F;
                model.body.yaw += 0.20F * reach;
                model.head.yaw -= 0.12F * reach;
                backSword.roll = -0.78F + (drawing ? 0.24F : -0.18F) * smooth;
            } else {
                backSword.visible = !drawn;
                backSword.roll = -0.78F;
            }
        }

        if (!SwordFinisherClient.isFinisherActive()) return;
        boolean attacker = state.id == SwordFinisherClient.getFinisherAttackerId();
        boolean target = state.id == SwordFinisherClient.getFinisherTargetId();
        if (!attacker && !target) return;

        // During finishers use one clean full-body model, avoiding double-rendered procedural limbs.
        if (model.getRootPart().hasChild("arrowrip_ai_rig")) {
            model.getRootPart().getChild("arrowrip_ai_rig").visible = false;
        }
        model.rightArm.visible = model.leftArm.visible = true;
        model.rightLeg.visible = model.leftLeg.visible = true;

        int style = SwordFinisherClient.getFinisherStyle();
        int ticks = SwordFinisherClient.getFinisherTicks();
        int duration = Math.max(1, SwordFinisherClient.getFinisherDuration());
        float p = 1.0F - ticks / (float)duration;
        p = MathHelper.clamp(p, 0.0F, 1.0F);

        if (style == 19) {
            poseLongChoreography(model, state.age, p, attacker);
        } else {
            poseShortFinisher(model, style, state.age, p, attacker);
        }
    }

    private static void poseShortFinisher(PlayerEntityModel m, int style, float age, float p, boolean attacker) {
        float in = smooth(MathHelper.clamp(p * 4.0F, 0.0F, 1.0F));
        float out = smooth(MathHelper.clamp((1.0F - p) * 5.0F, 0.0F, 1.0F));
        float w = in * out;
        float hit = MathHelper.sin(MathHelper.clamp((p - 0.18F) / 0.62F, 0.0F, 1.0F) * (float)Math.PI);
        float side = (style % 2 == 0) ? -1.0F : 1.0F;
        float tier = 0.75F + (style / 6) * 0.12F;

        if (attacker) {
            m.body.yaw = side * (0.28F + 0.18F * hit);
            m.body.pitch = 0.10F + 0.20F * hit;
            m.head.yaw = -side * 0.24F * w;
            m.rightArm.pitch = -1.15F - tier * hit;
            m.rightArm.yaw = -side * (0.30F + 0.55F * hit);
            m.rightArm.roll = side * (0.18F + 0.34F * hit);
            m.leftArm.pitch = -0.42F - 0.28F * w;
            m.leftArm.yaw = side * 0.26F;
            m.rightLeg.pitch = 0.20F + 0.18F * hit;
            m.leftLeg.pitch = -0.14F - 0.12F * hit;

            switch (style) {
                case 3, 8, 13 -> { // rising cut
                    m.rightArm.pitch = -2.35F + 1.50F * p;
                    m.body.roll = -side * 0.22F * hit;
                }
                case 4, 9, 14 -> { // overhead
                    m.rightArm.pitch = -2.85F + 2.05F * hit;
                    m.leftArm.pitch = -1.30F + 0.45F * hit;
                    m.body.pitch = 0.34F * hit;
                }
                case 5, 10, 15 -> { // thrust
                    m.rightArm.pitch = -1.52F;
                    m.rightArm.yaw = -side * 0.08F;
                    m.body.pitch = 0.42F * hit;
                    m.rightLeg.pitch = 0.42F * hit;
                    m.leftLeg.pitch = -0.28F * hit;
                }
                case 6, 11, 16 -> { // spin cut
                    m.body.yaw = side * (float)Math.PI * 1.25F * p;
                    m.rightArm.pitch = -1.26F;
                    m.rightArm.yaw = -side * 1.05F;
                    m.leftArm.pitch = -0.74F;
                }
                case 7, 12, 17 -> { // kick + slash
                    m.rightLeg.pitch = -1.18F * hit;
                    m.leftLeg.pitch = 0.36F * hit;
                    m.body.roll = side * 0.20F * hit;
                }
                case 18 -> { // execution stance
                    float slam = MathHelper.sin(MathHelper.clamp((p - 0.35F) * 2.2F, 0.0F, 1.0F) * (float)Math.PI);
                    m.rightArm.pitch = -2.65F + 2.15F * slam;
                    m.leftArm.pitch = -1.05F;
                    m.body.pitch = 0.48F * slam;
                    m.head.pitch = -0.24F + 0.30F * slam;
                }
                default -> {}
            }
        } else {
            float recoil = hit * (0.70F + style * 0.012F);
            m.body.pitch = -0.18F * recoil;
            m.body.yaw = -side * 0.36F * recoil;
            m.body.roll = side * 0.18F * recoil;
            m.head.pitch = 0.24F * recoil;
            m.head.yaw = side * 0.28F * recoil;
            m.rightArm.pitch = 0.42F + 0.36F * recoil;
            m.leftArm.pitch = 0.36F + 0.28F * recoil;
            m.rightArm.roll = 0.26F * recoil;
            m.leftArm.roll = -0.26F * recoil;
            m.rightLeg.pitch = -0.18F * recoil;
            m.leftLeg.pitch = 0.22F * recoil;
            if (style >= 13) {
                float collapse = smooth(MathHelper.clamp((p - 0.62F) / 0.35F, 0.0F, 1.0F));
                m.body.pitch += 0.92F * collapse;
                m.head.pitch += 0.46F * collapse;
                m.rightLeg.pitch += 0.55F * collapse;
                m.leftLeg.pitch += 0.38F * collapse;
            }
        }
    }

    private static void poseLongChoreography(PlayerEntityModel m, float age, float p, boolean attacker) {
        // 20 cinematic chapters across 2 minutes; motion is continuous rather than a frozen pose.
        float chapterF = p * 20.0F;
        int chapter = Math.min(19, (int)chapterF);
        float q = chapterF - chapter;
        float e = smooth(q);
        float wave = MathHelper.sin((age + chapter * 13.0F) * 0.12F);
        float side = (chapter % 2 == 0) ? 1.0F : -1.0F;

        if (attacker) {
            m.body.yaw = side * (0.18F + 0.46F * e);
            m.body.pitch = 0.08F + 0.24F * MathHelper.sin(q * (float)Math.PI);
            m.body.roll = side * 0.08F * wave;
            m.head.yaw = -m.body.yaw * 0.58F;
            m.head.pitch = -0.08F + 0.10F * wave;
            m.rightArm.pitch = -0.72F - 1.18F * MathHelper.sin(q * (float)Math.PI);
            m.rightArm.yaw = -side * (0.28F + 0.72F * e);
            m.rightArm.roll = side * (0.16F + 0.34F * wave);
            m.leftArm.pitch = -0.34F - 0.62F * (1.0F - e);
            m.leftArm.yaw = side * 0.24F;
            m.rightLeg.pitch = 0.28F * MathHelper.sin((q + 0.20F) * (float)Math.PI * 2.0F);
            m.leftLeg.pitch = -0.28F * MathHelper.sin((q + 0.20F) * (float)Math.PI * 2.0F);

            // Every chapter changes technique: slash, thrust, kick, spin, guard, recoil-reset.
            switch (chapter % 6) {
                case 0 -> { m.rightArm.pitch -= 0.55F * e; m.body.yaw += side * 0.38F * e; }
                case 1 -> { m.rightArm.pitch = -1.50F; m.body.pitch += 0.38F * e; m.rightLeg.pitch += 0.30F * e; }
                case 2 -> { m.rightLeg.pitch = -1.20F * MathHelper.sin(q * (float)Math.PI); m.body.roll += side * 0.22F * e; }
                case 3 -> { m.body.yaw += side * (float)Math.PI * 1.35F * e; m.rightArm.yaw = -side * 1.10F; }
                case 4 -> { m.rightArm.pitch = -2.52F + 1.36F * e; m.leftArm.pitch = -1.25F + 0.52F * e; }
                case 5 -> { m.rightArm.pitch = -0.92F; m.leftArm.pitch = -0.88F; m.body.pitch = -0.08F + 0.16F * wave; }
            }
        } else {
            float reaction = MathHelper.sin(q * (float)Math.PI);
            m.body.yaw = -side * 0.42F * reaction;
            m.body.pitch = -0.20F * reaction;
            m.body.roll = side * 0.16F * reaction;
            m.head.yaw = side * 0.30F * reaction;
            m.head.pitch = 0.22F * reaction;
            m.rightArm.pitch = 0.25F + 0.58F * reaction;
            m.leftArm.pitch = 0.18F + 0.48F * reaction;
            m.rightLeg.pitch = -0.18F * reaction;
            m.leftLeg.pitch = 0.22F * reaction;

            if (chapter >= 15) {
                float collapse = smooth(MathHelper.clamp((chapterF - 15.0F) / 5.0F, 0.0F, 1.0F));
                m.body.pitch += 1.02F * collapse;
                m.head.pitch += 0.55F * collapse;
                m.rightArm.pitch += 0.42F * collapse;
                m.leftArm.pitch += 0.48F * collapse;
                m.rightLeg.pitch += 0.52F * collapse;
                m.leftLeg.pitch += 0.38F * collapse;
            }
        }
    }

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
