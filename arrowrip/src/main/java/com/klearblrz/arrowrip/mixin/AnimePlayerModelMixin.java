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
        boolean moving = amp > 0.045F;
        boolean running = amp > 0.55F;

        if (AnimeAnimationClient.isSitting()) {
            model.body.pitch = 0.12F;
            model.head.pitch -= 0.08F;
            model.rightLeg.pitch = -1.42F;
            model.leftLeg.pitch = -1.42F;
            model.rightLeg.yaw = 0.28F;
            model.leftLeg.yaw = -0.28F;
            model.rightLeg.roll = 0.08F;
            model.leftLeg.roll = -0.08F;
            model.rightArm.pitch = -0.25F;
            model.leftArm.pitch = -0.25F;
            model.rightArm.roll = 0.12F;
            model.leftArm.roll = -0.12F;
        } else if (state.isInSneakingPose) {
            model.body.pitch += 0.18F;
            model.head.pitch -= 0.10F;
            model.rightArm.pitch += 0.20F;
            model.leftArm.pitch += 0.20F;
            model.rightArm.roll += 0.09F;
            model.leftArm.roll -= 0.09F;
        } else if (moving) {
            float wave = MathHelper.sin(state.limbSwingAnimationProgress * 0.6662F);
            model.body.pitch += running ? 0.22F : 0.09F;
            model.body.yaw += wave * (running ? 0.12F : 0.06F) * amp;
            model.head.roll += wave * 0.035F * amp;
            model.rightArm.roll += 0.05F * amp;
            model.leftArm.roll -= 0.05F * amp;
            if (running) {
                model.rightArm.pitch *= 1.18F;
                model.leftArm.pitch *= 1.18F;
                model.rightLeg.pitch *= 1.12F;
                model.leftLeg.pitch *= 1.12F;
            }
        } else {
            float breathe = MathHelper.sin(age * 0.09F);
            model.body.pitch += breathe * 0.018F;
            model.head.roll += MathHelper.sin(age * 0.045F) * 0.018F;
            model.rightArm.roll += 0.045F + breathe * 0.012F;
            model.leftArm.roll -= 0.045F + breathe * 0.012F;
        }

        // Anime-style attack slash. Works client-side on every normal hand swing.
        if (state.handSwingProgress > 0.001F) {
            float slash = MathHelper.sin(state.handSwingProgress * (float) Math.PI);
            boolean right = state.mainArm == Arm.RIGHT;
            float sign = right ? 1.0F : -1.0F;
            if (right) {
                model.rightArm.pitch -= 1.15F * slash;
                model.rightArm.yaw -= 0.45F * slash;
                model.leftArm.pitch += 0.22F * slash;
            } else {
                model.leftArm.pitch -= 1.15F * slash;
                model.leftArm.yaw += 0.45F * slash;
                model.rightArm.pitch += 0.22F * slash;
            }
            model.body.yaw += sign * 0.34F * slash;
            model.head.yaw -= sign * 0.12F * slash;
            model.body.pitch += 0.10F * slash;
        }

        int emote = AnimeAnimationClient.getEmoteType();
        if (emote == 0) return;
        float pulse = MathHelper.sin(age * 0.16F) * 0.06F;

        switch (emote) {
            case 1 -> { // power up
                model.body.pitch = -0.05F;
                model.head.pitch = -0.12F;
                model.rightArm.pitch = -0.55F + pulse;
                model.leftArm.pitch = -0.55F - pulse;
                model.rightArm.roll = 0.75F;
                model.leftArm.roll = -0.75F;
                model.rightLeg.roll = 0.08F;
                model.leftLeg.roll = -0.08F;
            }
            case 2 -> { // thinking
                model.head.pitch = 0.18F;
                model.head.yaw = 0.18F;
                model.rightArm.pitch = -1.45F;
                model.rightArm.yaw = -0.32F;
                model.rightArm.roll = 0.15F;
                model.leftArm.pitch = 0.18F;
            }
            case 3 -> { // victory
                model.rightArm.pitch = -2.75F + pulse;
                model.leftArm.pitch = -2.75F - pulse;
                model.rightArm.roll = 0.20F;
                model.leftArm.roll = -0.20F;
                model.head.pitch = -0.10F;
            }
            case 4 -> { // sword stance
                model.body.yaw = 0.28F;
                model.body.pitch = 0.10F;
                model.rightArm.pitch = -1.15F;
                model.rightArm.yaw = -0.45F;
                model.leftArm.pitch = 0.55F;
                model.leftArm.yaw = 0.35F;
                model.rightLeg.pitch = 0.22F;
                model.leftLeg.pitch = -0.18F;
            }
            case 5 -> { // anime bow
                model.body.pitch = 0.72F;
                model.head.pitch = -0.45F;
                model.rightArm.pitch = 0.18F;
                model.leftArm.pitch = 0.18F;
                model.rightArm.roll = 0.10F;
                model.leftArm.roll = -0.10F;
            }
            default -> { }
        }
    }
}
