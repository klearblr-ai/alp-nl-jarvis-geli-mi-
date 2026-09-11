package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AnimeAnimationClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class AnimePlayerModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$animePose(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) return;

        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        float age = state.age;
        float amp = MathHelper.clamp(state.limbSwingAmplitude, 0.0F, 1.0F);
        boolean moving = amp > 0.035F;
        boolean sprinting = client.player.isSprinting() && moving;
        boolean airborne = !client.player.isOnGround() && !state.touchingWater && !state.isGliding && !state.hasVehicle;
        double vy = client.player.getVelocity().y;
        boolean rising = airborne && vy > 0.03;
        boolean falling = airborne && vy < -0.04;
        float walk = state.limbSwingAnimationProgress;
        float wave = MathHelper.sin(walk * 0.72F);
        float breathe = MathHelper.sin(age * 0.085F);

        // Full third-person anime locomotion rewrite.
        if (AnimeAnimationClient.isSitting()) {
            model.body.pitch = 0.18F;
            model.body.yaw = 0.0F;
            model.head.pitch -= 0.10F;
            model.rightLeg.pitch = -1.48F;
            model.leftLeg.pitch = -1.48F;
            model.rightLeg.yaw = 0.34F;
            model.leftLeg.yaw = -0.34F;
            model.rightLeg.roll = 0.10F;
            model.leftLeg.roll = -0.10F;
            model.rightArm.pitch = -0.42F;
            model.leftArm.pitch = -0.42F;
            model.rightArm.yaw = -0.20F;
            model.leftArm.yaw = 0.20F;
            model.rightArm.roll = 0.16F;
            model.leftArm.roll = -0.16F;
        } else if (state.isGliding) {
            model.body.pitch = 0.40F;
            model.head.pitch = -0.52F;
            model.rightArm.pitch = 0.10F;
            model.leftArm.pitch = 0.10F;
            model.rightArm.roll = 1.28F;
            model.leftArm.roll = -1.28F;
            model.rightLeg.pitch = 0.18F;
            model.leftLeg.pitch = -0.12F;
            model.rightLeg.roll = 0.06F;
            model.leftLeg.roll = -0.06F;
        } else if (state.leaningPitch > 0.25F || state.touchingWater) {
            float swim = MathHelper.sin(age * 0.20F);
            model.body.pitch = 0.22F;
            model.head.pitch -= 0.18F;
            model.rightArm.pitch = -1.32F + swim * 0.32F;
            model.leftArm.pitch = -1.32F - swim * 0.32F;
            model.rightArm.roll = 0.18F;
            model.leftArm.roll = -0.18F;
            model.rightLeg.pitch = swim * 0.38F;
            model.leftLeg.pitch = -swim * 0.38F;
        } else if (rising) {
            model.body.pitch = -0.10F;
            model.head.pitch -= 0.08F;
            model.rightArm.pitch = -1.05F;
            model.leftArm.pitch = -1.05F;
            model.rightArm.roll = 0.44F;
            model.leftArm.roll = -0.44F;
            model.rightLeg.pitch = 0.54F;
            model.leftLeg.pitch = -0.32F;
            model.rightLeg.roll = 0.08F;
            model.leftLeg.roll = -0.08F;
        } else if (falling) {
            model.body.pitch = 0.16F;
            model.head.pitch += 0.10F;
            model.rightArm.pitch = -0.20F;
            model.leftArm.pitch = -0.20F;
            model.rightArm.roll = 0.95F;
            model.leftArm.roll = -0.95F;
            model.rightLeg.pitch = 0.24F;
            model.leftLeg.pitch = 0.24F;
            model.rightLeg.roll = 0.15F;
            model.leftLeg.roll = -0.15F;
        } else if (state.isInSneakingPose) {
            model.body.pitch = 0.66F;
            model.head.pitch -= 0.30F;
            model.body.yaw += wave * 0.08F;
            model.rightArm.pitch = -0.32F + wave * 0.18F;
            model.leftArm.pitch = -0.32F - wave * 0.18F;
            model.rightArm.roll = 0.18F;
            model.leftArm.roll = -0.18F;
            model.rightLeg.pitch *= 0.74F;
            model.leftLeg.pitch *= 0.74F;
        } else if (sprinting) {
            model.body.pitch = 0.48F;
            model.body.yaw += wave * 0.14F;
            model.head.pitch -= 0.17F;
            model.head.roll += wave * 0.045F;
            model.rightArm.pitch *= 1.48F;
            model.leftArm.pitch *= 1.48F;
            model.rightArm.roll += 0.12F;
            model.leftArm.roll -= 0.12F;
            model.rightLeg.pitch *= 1.28F;
            model.leftLeg.pitch *= 1.28F;
            model.rightLeg.roll = 0.045F;
            model.leftLeg.roll = -0.045F;
        } else if (moving) {
            model.body.pitch = 0.11F;
            model.body.yaw += wave * 0.075F;
            model.head.roll += wave * 0.026F;
            model.rightArm.pitch *= 1.16F;
            model.leftArm.pitch *= 1.16F;
            model.rightArm.roll += 0.075F;
            model.leftArm.roll -= 0.075F;
            model.rightLeg.pitch *= 1.08F;
            model.leftLeg.pitch *= 1.08F;
        } else {
            model.body.pitch = -0.025F + breathe * 0.020F;
            model.body.yaw = MathHelper.sin(age * 0.036F) * 0.025F;
            model.head.roll += MathHelper.sin(age * 0.050F) * 0.024F;
            model.head.pitch += MathHelper.sin(age * 0.027F) * 0.015F;
            model.rightArm.pitch = 0.055F + breathe * 0.026F;
            model.leftArm.pitch = -0.025F - breathe * 0.022F;
            model.rightArm.roll = 0.095F;
            model.leftArm.roll = -0.095F;
            model.rightLeg.roll = 0.012F;
            model.leftLeg.roll = -0.012F;
        }

        // Every normal attack becomes a more dramatic anime slash/punch.
        if (state.handSwingProgress > 0.001F) {
            float p = state.handSwingProgress;
            float slash = MathHelper.sin(p * (float) Math.PI);
            float snap = MathHelper.sin(Math.min(1.0F, p * 1.55F) * (float) Math.PI);
            boolean right = state.mainArm == Arm.RIGHT;
            float sign = right ? 1.0F : -1.0F;

            model.body.yaw += sign * (0.22F + 0.42F * slash);
            model.body.pitch += 0.12F * slash;
            model.head.yaw -= sign * 0.18F * slash;
            model.head.roll -= sign * 0.08F * snap;

            if (right) {
                model.rightArm.pitch = -0.75F - 1.55F * slash;
                model.rightArm.yaw = -0.28F - 0.58F * slash;
                model.rightArm.roll = 0.14F + 0.26F * snap;
                model.leftArm.pitch = 0.28F + 0.35F * slash;
                model.leftArm.yaw = 0.20F;
            } else {
                model.leftArm.pitch = -0.75F - 1.55F * slash;
                model.leftArm.yaw = 0.28F + 0.58F * slash;
                model.leftArm.roll = -0.14F - 0.26F * snap;
                model.rightArm.pitch = 0.28F + 0.35F * slash;
                model.rightArm.yaw = -0.20F;
            }
        }

        // Item-use poses get extra anime exaggeration without replacing vanilla logic.
        if (state.isUsingItem) {
            switch (state.rightArmPose) {
                case BLOCK -> {
                    model.body.yaw -= 0.12F;
                    model.rightArm.pitch -= 0.18F;
                    model.rightArm.yaw -= 0.18F;
                    model.leftArm.pitch += 0.12F;
                }
                case BOW_AND_ARROW -> {
                    model.body.yaw -= 0.18F;
                    model.head.pitch -= 0.06F;
                    model.rightLeg.pitch = 0.18F;
                    model.leftLeg.pitch = -0.14F;
                }
                case CROSSBOW_CHARGE, CROSSBOW_HOLD -> {
                    model.body.pitch += 0.09F;
                    model.head.pitch -= 0.04F;
                }
                case SPEAR -> {
                    model.body.pitch += 0.24F;
                    model.body.yaw -= 0.18F;
                    model.rightLeg.pitch = 0.26F;
                    model.leftLeg.pitch = -0.16F;
                }
                default -> { }
            }
        }

        int emote = AnimeAnimationClient.getEmoteType();
        if (emote == 0) return;
        float pulse = MathHelper.sin(age * 0.16F) * 0.07F;
        float slow = MathHelper.sin(age * 0.07F);

        switch (emote) {
            case 1 -> { // power up
                model.body.pitch = -0.08F;
                model.head.pitch = -0.18F;
                model.head.roll = pulse * 0.22F;
                model.rightArm.pitch = -0.72F + pulse;
                model.leftArm.pitch = -0.72F - pulse;
                model.rightArm.roll = 0.92F;
                model.leftArm.roll = -0.92F;
                model.rightLeg.roll = 0.13F;
                model.leftLeg.roll = -0.13F;
            }
            case 2 -> { // thinking
                model.body.yaw = -0.08F;
                model.head.pitch = 0.16F;
                model.head.yaw = 0.27F;
                model.rightArm.pitch = -1.56F;
                model.rightArm.yaw = -0.36F;
                model.rightArm.roll = 0.18F;
                model.leftArm.pitch = 0.22F;
                model.leftArm.yaw = 0.14F;
            }
            case 3 -> { // victory
                model.body.pitch = -0.06F;
                model.rightArm.pitch = -2.88F + pulse;
                model.leftArm.pitch = -2.48F - pulse;
                model.rightArm.roll = 0.22F;
                model.leftArm.roll = -0.28F;
                model.head.pitch = -0.14F;
                model.head.yaw = slow * 0.08F;
            }
            case 4 -> { // sword stance
                model.body.yaw = 0.38F;
                model.body.pitch = 0.16F;
                model.head.yaw = -0.16F;
                model.rightArm.pitch = -1.28F;
                model.rightArm.yaw = -0.62F;
                model.rightArm.roll = 0.16F;
                model.leftArm.pitch = 0.68F;
                model.leftArm.yaw = 0.42F;
                model.rightLeg.pitch = 0.34F;
                model.leftLeg.pitch = -0.24F;
                model.rightLeg.roll = 0.06F;
                model.leftLeg.roll = -0.06F;
            }
            case 5 -> { // anime bow
                model.body.pitch = 0.84F;
                model.head.pitch = -0.58F;
                model.rightArm.pitch = 0.24F;
                model.leftArm.pitch = 0.24F;
                model.rightArm.roll = 0.12F;
                model.leftArm.roll = -0.12F;
            }
            case 6 -> { // villain laugh
                model.body.pitch = -0.14F;
                model.head.pitch = -0.38F + pulse * 0.7F;
                model.head.yaw = slow * 0.13F;
                model.rightArm.pitch = -1.05F;
                model.leftArm.pitch = -1.05F;
                model.rightArm.roll = 0.58F;
                model.leftArm.roll = -0.58F;
            }
            case 7 -> { // cool pose
                model.body.yaw = -0.30F;
                model.body.pitch = 0.04F;
                model.head.yaw = 0.30F;
                model.head.roll = -0.09F;
                model.rightArm.pitch = -0.22F;
                model.rightArm.yaw = -0.24F;
                model.rightArm.roll = 0.46F;
                model.leftArm.pitch = 0.16F;
                model.leftArm.yaw = 0.18F;
                model.leftArm.roll = -0.12F;
                model.rightLeg.pitch = 0.20F;
                model.leftLeg.pitch = -0.12F;
            }
            case 8 -> { // rage
                model.body.pitch = 0.14F + pulse * 0.4F;
                model.head.pitch = -0.24F;
                model.rightArm.pitch = -0.62F + pulse;
                model.leftArm.pitch = -0.62F - pulse;
                model.rightArm.roll = 0.80F;
                model.leftArm.roll = -0.80F;
                model.rightLeg.roll = 0.12F;
                model.leftLeg.roll = -0.12F;
            }
            case 9 -> { // salute
                model.body.pitch = -0.02F;
                model.head.yaw = -0.12F;
                model.rightArm.pitch = -1.72F;
                model.rightArm.yaw = -0.22F;
                model.rightArm.roll = 0.34F;
                model.leftArm.pitch = 0.04F;
                model.leftArm.roll = -0.08F;
            }
            default -> { }
        }
    }
}
