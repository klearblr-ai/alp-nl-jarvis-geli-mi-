package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AutoPvPAnimationClient;
import net.minecraft.client.MinecraftClient;
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
public abstract class AutoPvPAnimationMixin {
    private static float arrowrip$deathStartAge = -1.0F;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$autoPvpTimeline(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        PlayerEntityModel model = (PlayerEntityModel)(Object)this;

        // Cinematic target reaction has priority over ordinary remote-player rendering.
        if (AutoPvPAnimationClient.getFinisherTicks() > 0
                && state.id == AutoPvPAnimationClient.getFinisherTargetId()) {
            applyFinisherTarget(model, AutoPvPAnimationClient.getFinisherProgress());
            return;
        }

        if (state.id != client.player.getId()) return;

        float hpRatio = client.player.getHealth() / Math.max(1.0F, client.player.getMaxHealth());

        // One-way anime death/collapse timeline rather than a looping pose.
        if (client.player.getHealth() <= 0.0F) {
            if (arrowrip$deathStartAge < 0.0F) arrowrip$deathStartAge = state.age;
            float d = smooth(MathHelper.clamp((state.age - arrowrip$deathStartAge) / 22.0F, 0.0F, 1.0F));
            model.body.pitch = 0.18F + 1.18F * d;
            model.body.roll = 0.42F * d;
            model.head.pitch = -0.08F + 0.56F * d;
            model.head.roll = -0.34F * d;
            model.rightArm.pitch = -0.20F + 1.18F * d;
            model.leftArm.pitch = -0.08F + 0.92F * d;
            model.rightArm.roll = 0.38F + 0.42F * d;
            model.leftArm.roll = -0.28F - 0.34F * d;
            model.rightLeg.pitch = 0.24F + 0.52F * d;
            model.leftLeg.pitch = -0.12F + 0.30F * d;
            model.rightLeg.roll = 0.08F + 0.16F * d;
            model.leftLeg.roll = -0.08F - 0.10F * d;
            return;
        } else {
            arrowrip$deathStartAge = -1.0F;
        }

        if (AutoPvPAnimationClient.getFinisherTicks() > 0) {
            applyFinisherAttacker(model, state, AutoPvPAnimationClient.getFinisherProgress());
            return;
        }

        if (AutoPvPAnimationClient.getStabTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getStabTicks() / 13.0F;
            applySwordStab(model, state, MathHelper.clamp(p, 0.0F, 1.0F));
            return;
        }

        if (AutoPvPAnimationClient.getKickTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getKickTicks() / 15.0F;
            applyKick(model, state, MathHelper.clamp(p, 0.0F, 1.0F));
            return;
        }

        if (AutoPvPAnimationClient.getPunchTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getPunchTicks() / 9.0F;
            applyPunch(model, state, MathHelper.clamp(p, 0.0F, 1.0F));
            return;
        }

        if (AutoPvPAnimationClient.getMineTicks() > 0) {
            applyMining(model, state);
            return;
        }

        if (AutoPvPAnimationClient.getPlaceTicks() > 0) {
            applyBridgePlace(model, state);
            return;
        }

        if (AutoPvPAnimationClient.getLandingTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getLandingTicks() / 10.0F;
            float impact = 1.0F - smooth(MathHelper.clamp(p / 0.72F, 0.0F, 1.0F));
            model.body.pitch = 0.42F * impact;
            model.head.pitch = -0.20F * impact;
            model.rightArm.pitch = -0.46F * impact;
            model.leftArm.pitch = -0.46F * impact;
            model.rightArm.roll = 0.34F * impact;
            model.leftArm.roll = -0.34F * impact;
            model.rightLeg.pitch = 0.62F * impact;
            model.leftLeg.pitch = 0.44F * impact;
            model.rightLeg.roll = 0.10F * impact;
            model.leftLeg.roll = -0.10F * impact;
            return;
        }

        if (AutoPvPAnimationClient.getHurtTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getHurtTicks() / 11.0F;
            float recoil = MathHelper.sin(p * (float)Math.PI);
            model.body.pitch = -0.18F * recoil;
            model.body.yaw += 0.32F * recoil;
            model.body.roll = -0.10F * recoil;
            model.head.pitch = 0.22F * recoil;
            model.head.yaw -= 0.28F * recoil;
            model.rightArm.pitch += 0.38F * recoil;
            model.leftArm.pitch += 0.22F * recoil;
            model.rightLeg.pitch -= 0.16F * recoil;
            model.leftLeg.pitch += 0.20F * recoil;
            return;
        }

        // Low-health PvP stance: bent, guarded and breathing harder.
        if (hpRatio <= 0.25F && state.limbSwingAmplitude < 0.05F && client.player.isOnGround()) {
            float breath = MathHelper.sin(state.age * 0.16F);
            model.body.pitch = 0.24F + breath * 0.025F;
            model.body.yaw = 0.12F;
            model.head.pitch = -0.08F + breath * 0.018F;
            model.rightArm.pitch = -0.72F + breath * 0.035F;
            model.leftArm.pitch = -0.56F - breath * 0.030F;
            model.rightArm.roll = 0.34F;
            model.leftArm.roll = -0.34F;
            model.rightLeg.pitch = 0.20F;
            model.leftLeg.pitch = -0.12F;
        }
    }

    private static void applySwordStab(PlayerEntityModel model, PlayerEntityRenderState state, float p) {
        float wind = smooth(MathHelper.clamp(p / 0.28F, 0.0F, 1.0F));
        float thrust = smooth(MathHelper.clamp((p - 0.24F) / 0.34F, 0.0F, 1.0F));
        float recover = smooth(MathHelper.clamp((p - 0.66F) / 0.34F, 0.0F, 1.0F));
        float hit = thrust * (1.0F - recover);
        boolean right = state.mainArm == Arm.RIGHT;
        float sign = right ? 1.0F : -1.0F;
        var arm = right ? model.rightArm : model.leftArm;
        var off = right ? model.leftArm : model.rightArm;

        model.body.pitch = 0.08F + 0.30F * hit;
        model.body.yaw = -sign * (0.28F * wind - 0.50F * hit);
        model.head.yaw = sign * 0.15F * hit;
        arm.pitch = -0.72F - 0.70F * hit;
        arm.yaw = -sign * (0.55F * wind + 0.10F * hit);
        arm.roll = sign * (0.18F + 0.22F * wind);
        off.pitch = 0.26F + 0.38F * hit;
        off.yaw = sign * 0.26F;
        model.rightLeg.pitch = 0.30F * hit;
        model.leftLeg.pitch = -0.22F * hit;
    }

    private static void applyPunch(PlayerEntityModel model, PlayerEntityRenderState state, float p) {
        float wind = smooth(MathHelper.clamp(p / 0.30F, 0.0F, 1.0F));
        float hit = MathHelper.sin(MathHelper.clamp((p - 0.18F) / 0.66F, 0.0F, 1.0F) * (float)Math.PI);
        boolean right = state.mainArm == Arm.RIGHT;
        float sign = right ? 1.0F : -1.0F;
        var arm = right ? model.rightArm : model.leftArm;
        var off = right ? model.leftArm : model.rightArm;
        model.body.yaw = -sign * 0.34F * wind + sign * 0.72F * hit;
        model.body.pitch = 0.18F * hit;
        model.head.yaw = -sign * 0.22F * hit;
        arm.pitch = -0.78F - 0.94F * hit;
        arm.yaw = -sign * (0.48F * wind + 0.24F * hit);
        arm.roll = sign * 0.30F;
        off.pitch = -0.68F;
        off.yaw = sign * 0.32F;
        model.rightLeg.pitch = right ? 0.22F * hit : -0.12F * hit;
        model.leftLeg.pitch = right ? -0.12F * hit : 0.22F * hit;
    }

    private static void applyKick(PlayerEntityModel model, PlayerEntityRenderState state, float p) {
        float chamber = smooth(MathHelper.clamp(p / 0.32F, 0.0F, 1.0F));
        float extend = smooth(MathHelper.clamp((p - 0.25F) / 0.30F, 0.0F, 1.0F));
        float recover = smooth(MathHelper.clamp((p - 0.62F) / 0.38F, 0.0F, 1.0F));
        float kick = extend * (1.0F - recover);
        float side = (state.age % 2.0F < 1.0F) ? 1.0F : -1.0F;
        model.body.pitch = -0.08F + 0.16F * chamber;
        model.body.yaw = side * (0.18F + 0.50F * kick);
        model.body.roll = -side * 0.18F * kick;
        model.head.yaw = -side * 0.20F * kick;
        model.rightArm.pitch = -0.62F;
        model.leftArm.pitch = -0.62F;
        model.rightArm.roll = 0.42F;
        model.leftArm.roll = -0.42F;
        if (side > 0.0F) {
            model.rightLeg.pitch = -0.32F * chamber - 1.15F * kick;
            model.rightLeg.yaw = -0.20F * kick;
            model.rightLeg.roll = 0.18F * kick;
            model.leftLeg.pitch = 0.20F;
        } else {
            model.leftLeg.pitch = -0.32F * chamber - 1.15F * kick;
            model.leftLeg.yaw = 0.20F * kick;
            model.leftLeg.roll = -0.18F * kick;
            model.rightLeg.pitch = 0.20F;
        }
    }

    private static void applyMining(PlayerEntityModel model, PlayerEntityRenderState state) {
        float cycle = (MathHelper.sin(state.age * 0.72F) + 1.0F) * 0.5F;
        boolean right = state.mainArm == Arm.RIGHT;
        var arm = right ? model.rightArm : model.leftArm;
        var off = right ? model.leftArm : model.rightArm;
        model.body.pitch = 0.28F;
        model.body.yaw = (right ? 1.0F : -1.0F) * (0.16F - 0.22F * cycle);
        model.head.pitch = -0.16F;
        arm.pitch = -2.35F + 1.70F * cycle;
        arm.yaw = (right ? -1.0F : 1.0F) * 0.28F;
        arm.roll = (right ? 1.0F : -1.0F) * 0.16F;
        off.pitch = -1.42F + 0.82F * cycle;
        off.yaw = (right ? 1.0F : -1.0F) * 0.20F;
        model.rightLeg.pitch = 0.16F;
        model.leftLeg.pitch = -0.10F;
    }

    private static void applyBridgePlace(PlayerEntityModel model, PlayerEntityRenderState state) {
        float pulse = MathHelper.sin(state.age * 0.55F) * 0.08F;
        boolean right = state.mainArm == Arm.RIGHT;
        var arm = right ? model.rightArm : model.leftArm;
        var off = right ? model.leftArm : model.rightArm;
        model.body.pitch = 0.46F;
        model.body.yaw = (right ? -1.0F : 1.0F) * 0.16F;
        model.head.pitch = 0.34F;
        arm.pitch = -1.36F + pulse;
        arm.yaw = (right ? -1.0F : 1.0F) * 0.32F;
        arm.roll = (right ? 1.0F : -1.0F) * 0.16F;
        off.pitch = -0.28F;
        off.yaw = (right ? 1.0F : -1.0F) * 0.14F;
        model.rightLeg.pitch = 0.30F;
        model.leftLeg.pitch = -0.24F;
        model.rightLeg.roll = 0.06F;
        model.leftLeg.roll = -0.06F;
    }

    private static void applyFinisherAttacker(PlayerEntityModel model, PlayerEntityRenderState state, float p) {
        float ready = smooth(MathHelper.clamp(p / 0.20F, 0.0F, 1.0F));
        float dash = smooth(MathHelper.clamp((p - 0.18F) / 0.24F, 0.0F, 1.0F));
        float impact = smooth(MathHelper.clamp((p - 0.40F) / 0.16F, 0.0F, 1.0F));
        float settle = smooth(MathHelper.clamp((p - 0.62F) / 0.38F, 0.0F, 1.0F));
        float power = impact * (1.0F - settle);
        boolean right = state.mainArm == Arm.RIGHT;
        float sign = right ? 1.0F : -1.0F;
        var arm = right ? model.rightArm : model.leftArm;
        var off = right ? model.leftArm : model.rightArm;

        model.body.pitch = 0.12F * ready + 0.34F * dash - 0.10F * settle;
        model.body.yaw = -sign * 0.42F * ready + sign * 0.62F * power;
        model.head.pitch = -0.10F * ready;
        model.head.yaw = -sign * 0.24F * power;
        arm.pitch = -0.88F - 0.62F * ready - 0.68F * power;
        arm.yaw = -sign * (0.62F * ready + 0.16F * power);
        arm.roll = sign * (0.16F + 0.30F * power);
        off.pitch = -0.46F - 0.28F * ready + 0.70F * power;
        off.yaw = sign * 0.28F;
        model.rightLeg.pitch = 0.42F * dash;
        model.leftLeg.pitch = -0.30F * dash;
        model.rightLeg.roll = 0.08F * dash;
        model.leftLeg.roll = -0.08F * dash;
    }

    private static void applyFinisherTarget(PlayerEntityModel model, float p) {
        float notice = smooth(MathHelper.clamp(p / 0.28F, 0.0F, 1.0F));
        float hit = smooth(MathHelper.clamp((p - 0.43F) / 0.16F, 0.0F, 1.0F));
        float fall = smooth(MathHelper.clamp((p - 0.56F) / 0.36F, 0.0F, 1.0F));
        model.body.pitch = -0.08F * notice + 0.92F * fall;
        model.body.yaw = -0.18F * notice + 0.34F * hit;
        model.body.roll = 0.18F * hit + 0.46F * fall;
        model.head.pitch = -0.12F * notice + 0.44F * hit;
        model.head.yaw = -0.44F * hit;
        model.head.roll = 0.32F * hit + 0.20F * fall;
        model.rightArm.pitch = -0.42F * notice + 0.88F * fall;
        model.leftArm.pitch = -0.34F * notice + 0.72F * fall;
        model.rightArm.roll = 0.44F * hit;
        model.leftArm.roll = -0.36F * hit;
        model.rightLeg.pitch = 0.24F * fall;
        model.leftLeg.pitch = -0.16F * fall;
    }

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
