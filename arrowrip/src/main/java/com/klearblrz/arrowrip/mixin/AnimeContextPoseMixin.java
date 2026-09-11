package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AnimeAnimationClient;
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
public abstract class AnimeContextPoseMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$contextPose(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        PlayerEntityModel model = (PlayerEntityModel) (Object) this;

        // Whoever is currently speaking gets the moving scene, even when the voice was
        // assigned to the looked-at player by the local 50/50 illusion.
        if (AnimeAnimationClient.getSpeechEmoteTicks() > 0
                && state.id == AnimeAnimationClient.getSpeechSpeakerId()) {
            applySpeechScene(model, state);
            return;
        }

        // Automatic combat/relaxed stances only affect the local player's third-person model.
        if (state.id != client.player.getId()) return;
        if (AnimeAnimationClient.getEmoteType() != 0 || AnimeAnimationClient.isSitting()) return;
        if (state.handSwingProgress > 0.01F || state.isUsingItem) return;
        if (state.limbSwingAmplitude > 0.055F || !client.player.isOnGround() || state.touchingWater || state.isGliding) return;

        float breathe = MathHelper.sin(state.age * 0.075F);
        boolean sword = client.player.getMainHandStack().isIn(ItemTags.SWORDS);

        if (sword) {
            boolean alert = client.targetedEntity != null;
            float side = state.mainArm == Arm.RIGHT ? 1.0F : -1.0F;
            model.body.pitch = alert ? 0.13F : 0.045F;
            model.body.yaw = side * (alert ? 0.28F : 0.14F);
            model.head.yaw -= side * (alert ? 0.14F : 0.06F);
            model.head.pitch = alert ? -0.08F : -0.025F;

            if (state.mainArm == Arm.RIGHT) {
                model.rightArm.pitch = alert ? -1.18F : -0.72F;
                model.rightArm.yaw = alert ? -0.48F : -0.24F;
                model.rightArm.roll = 0.12F + breathe * 0.018F;
                model.leftArm.pitch = alert ? 0.42F : 0.16F;
                model.leftArm.yaw = 0.20F;
            } else {
                model.leftArm.pitch = alert ? -1.18F : -0.72F;
                model.leftArm.yaw = alert ? 0.48F : 0.24F;
                model.leftArm.roll = -0.12F - breathe * 0.018F;
                model.rightArm.pitch = alert ? 0.42F : 0.16F;
                model.rightArm.yaw = -0.20F;
            }
            model.rightLeg.pitch = alert ? 0.18F : 0.08F;
            model.leftLeg.pitch = alert ? -0.14F : -0.05F;
            model.rightLeg.roll = 0.035F;
            model.leftLeg.roll = -0.035F;
        } else {
            // Relaxed asymmetric anime idle rather than a rigid vanilla T-ish stance.
            model.body.pitch = -0.035F + breathe * 0.012F;
            model.body.yaw = -0.075F + MathHelper.sin(state.age * 0.035F) * 0.018F;
            model.head.yaw += 0.055F;
            model.head.roll -= 0.025F;
            model.rightArm.pitch = 0.10F + breathe * 0.020F;
            model.rightArm.yaw = -0.08F;
            model.rightArm.roll = 0.16F;
            model.leftArm.pitch = -0.04F - breathe * 0.014F;
            model.leftArm.yaw = 0.05F;
            model.leftArm.roll = -0.075F;
            model.rightLeg.pitch = 0.07F;
            model.leftLeg.pitch = -0.04F;
            model.rightLeg.roll = 0.018F;
            model.leftLeg.roll = -0.018F;
        }
    }

    private static void applySpeechScene(PlayerEntityModel model, PlayerEntityRenderState state) {
        int type = AnimeAnimationClient.getSpeechEmoteType();
        int ticks = AnimeAnimationClient.getSpeechEmoteTicks();
        int duration = Math.max(1, AnimeAnimationClient.getSpeechEmoteDuration());
        float p = MathHelper.clamp(1.0F - (float) ticks / duration, 0.0F, 1.0F);
        float intro = smooth(MathHelper.clamp(p / 0.22F, 0.0F, 1.0F));
        float outro = smooth(MathHelper.clamp((1.0F - p) / 0.18F, 0.0F, 1.0F));
        float hold = Math.min(intro, outro);
        float talk = MathHelper.sin(p * 11.0F * (float) Math.PI) * 0.055F * hold;
        float slow = MathHelper.sin(p * 3.0F * (float) Math.PI) * hold;

        switch (type) {
            case 1 -> { // animated normal talking
                model.body.yaw = -0.08F * hold;
                model.head.yaw += 0.07F * slow;
                model.rightArm.pitch = (-0.48F + talk) * hold;
                model.rightArm.yaw = -0.22F * hold;
                model.rightArm.roll = 0.24F * hold;
                model.leftArm.pitch = (0.08F - talk * 0.5F) * hold;
            }
            case 2 -> { // Nani/Nanda question: shrug + head tilt
                model.head.yaw += 0.26F * hold;
                model.head.roll += 0.12F * hold + talk;
                model.body.yaw = -0.12F * hold;
                model.rightArm.pitch = -0.55F * hold;
                model.leftArm.pitch = -0.42F * hold;
                model.rightArm.roll = 0.58F * hold;
                model.leftArm.roll = -0.52F * hold;
            }
            case 3 -> { // impressed / sugoi
                model.head.pitch = -0.09F * hold;
                model.rightArm.pitch = -0.72F * hold + talk;
                model.leftArm.pitch = -0.72F * hold - talk;
                model.rightArm.roll = 0.72F * hold;
                model.leftArm.roll = -0.72F * hold;
                model.body.pitch = -0.06F * hold;
            }
            case 4 -> { // power declaration
                model.body.pitch = 0.10F * hold;
                model.head.pitch = -0.18F * hold;
                model.rightArm.pitch = (-0.64F + talk) * hold;
                model.leftArm.pitch = (-0.64F - talk) * hold;
                model.rightArm.roll = 0.88F * hold;
                model.leftArm.roll = -0.88F * hold;
                model.rightLeg.roll = 0.10F * hold;
                model.leftLeg.roll = -0.10F * hold;
            }
            case 5 -> { // yamero stop palm
                model.body.yaw = -0.18F * hold;
                model.head.yaw = 0.10F * hold;
                model.rightArm.pitch = -1.42F * hold;
                model.rightArm.yaw = -0.15F * hold;
                model.rightArm.roll = 0.08F * hold;
                model.leftArm.pitch = 0.26F * hold;
            }
            case 6 -> { // ikuzo / ready
                model.body.pitch = 0.18F * hold;
                model.body.yaw = 0.28F * hold;
                model.head.yaw = -0.12F * hold;
                model.rightArm.pitch = -1.08F * hold + talk;
                model.rightArm.yaw = -0.46F * hold;
                model.leftArm.pitch = 0.52F * hold;
                model.leftArm.yaw = 0.30F * hold;
                model.rightLeg.pitch = 0.25F * hold;
                model.leftLeg.pitch = -0.18F * hold;
            }
            case 7 -> { // yare yare, relaxed dismissive gesture
                model.body.yaw = -0.16F * hold;
                model.head.pitch = 0.13F * hold;
                model.head.yaw = 0.16F * hold + talk;
                model.rightArm.pitch = -1.34F * hold;
                model.rightArm.yaw = -0.26F * hold;
                model.rightArm.roll = 0.22F * hold;
                model.leftArm.pitch = 0.10F * hold;
            }
            case 8 -> { // point at opponent
                model.body.yaw = 0.20F * hold;
                model.head.yaw = -0.12F * hold;
                model.rightArm.pitch = -1.52F * hold;
                model.rightArm.yaw = -0.38F * hold + talk;
                model.leftArm.pitch = 0.24F * hold;
                model.leftArm.yaw = 0.14F * hold;
            }
            case 9 -> { // "koroshite yaru" style cinematic: head down -> raise -> threaten
                float headRaise = smooth(MathHelper.clamp((p - 0.10F) / 0.28F, 0.0F, 1.0F));
                float point = smooth(MathHelper.clamp((p - 0.28F) / 0.24F, 0.0F, 1.0F));
                model.body.pitch = (0.16F - 0.20F * point) * hold;
                model.body.yaw = 0.26F * point * hold;
                model.head.pitch = (0.34F * (1.0F - headRaise) - 0.20F * headRaise) * hold;
                model.head.yaw = -0.18F * point * hold;
                model.rightArm.pitch = (-0.30F - 1.18F * point + talk) * hold;
                model.rightArm.yaw = (-0.10F - 0.42F * point) * hold;
                model.rightArm.roll = 0.22F * point * hold;
                model.leftArm.pitch = (0.10F + 0.40F * point) * hold;
                model.leftArm.yaw = 0.24F * point * hold;
                model.rightLeg.pitch = 0.18F * point * hold;
                model.leftLeg.pitch = -0.14F * point * hold;
            }
            case 10 -> { // challenge / beckon
                model.body.yaw = -0.22F * hold;
                model.head.yaw = 0.16F * hold;
                model.rightArm.pitch = -1.20F * hold;
                model.rightArm.yaw = -0.38F * hold;
                model.rightArm.roll = (0.28F + 0.15F * MathHelper.sin(p * 8.0F * (float)Math.PI)) * hold;
                model.leftArm.pitch = 0.16F * hold;
            }
            case 11 -> { // determined: fist to chest then forward
                float thrust = smooth(MathHelper.clamp((p - 0.42F) / 0.22F, 0.0F, 1.0F));
                model.body.pitch = 0.08F * hold;
                model.head.pitch = -0.10F * hold;
                model.rightArm.pitch = (-1.30F - 0.25F * thrust) * hold;
                model.rightArm.yaw = (-0.18F - 0.34F * thrust) * hold;
                model.rightArm.roll = 0.20F * hold;
                model.leftArm.pitch = 0.18F * hold;
            }
            case 12 -> { // final declaration / dramatic spread
                model.body.pitch = -0.09F * hold;
                model.head.pitch = -0.18F * hold;
                model.rightArm.pitch = (-0.88F + talk) * hold;
                model.leftArm.pitch = (-0.88F - talk) * hold;
                model.rightArm.roll = 1.05F * hold;
                model.leftArm.roll = -1.05F * hold;
                model.body.yaw = 0.08F * slow;
            }
            default -> { }
        }
    }

    private static float smooth(float x) {
        return x * x * (3.0F - 2.0F * x);
    }
}
