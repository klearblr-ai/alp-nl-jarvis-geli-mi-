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

/**
 * Adds elbow/wrist-like articulation on top of the vanilla one-piece arm model.
 * Minecraft does not expose separate forearm/hand bones, so this layers staged
 * pitch/yaw/roll motion to create a much more jointed third-person silhouette.
 */
@Mixin(value = PlayerEntityModel.class, priority = 900)
public abstract class ArmJointPolishMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$jointPolish(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        boolean speech = AnimeAnimationClient.getSpeechEmoteTicks() > 0
                && state.id == AnimeAnimationClient.getSpeechSpeakerId();
        boolean local = state.id == client.player.getId();
        if (!speech && !local) return;

        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        float age = state.age;
        float breath = MathHelper.sin(age * 0.11F);
        float talk = MathHelper.sin(age * 0.34F);

        // Speech scenes: fingers are not separately modeled, so use short wrist-like
        // twists and elbow-like angle changes while the character is talking.
        if (speech) {
            int type = AnimeAnimationClient.getSpeechEmoteType();
            float strength = switch (type) {
                case 4, 9, 12 -> 1.0F;
                case 5, 8, 10, 11 -> 0.82F;
                default -> 0.58F;
            };
            model.rightArm.yaw += talk * 0.08F * strength;
            model.rightArm.roll += breath * 0.055F * strength;
            model.leftArm.yaw -= talk * 0.055F * strength;
            model.leftArm.roll -= breath * 0.040F * strength;

            if (type == 5 || type == 8 || type == 9) {
                // Stop / point / threat: straight shoulder, bent-looking elbow, twisted wrist.
                model.rightArm.pitch -= 0.12F;
                model.rightArm.yaw -= 0.10F;
                model.rightArm.roll += 0.16F + talk * 0.04F;
            } else if (type == 2 || type == 7) {
                // Question / yare-yare: loose bent elbow and palm-up illusion.
                model.rightArm.pitch += 0.10F;
                model.rightArm.yaw += 0.13F;
                model.rightArm.roll += 0.18F;
            }
            return;
        }

        if (!local) return;

        boolean sword = state.getMainHandItemStack().isIn(ItemTags.SWORDS);
        boolean right = state.mainArm == Arm.RIGHT;
        float sign = right ? 1.0F : -1.0F;
        var mainArm = right ? model.rightArm : model.leftArm;
        var offArm = right ? model.leftArm : model.rightArm;

        // Sword swing: add a second-stage hinge/twist on top of the 8 slash styles.
        if (sword && state.handSwingProgress > 0.001F) {
            float p = state.handSwingProgress;
            float elbow = MathHelper.sin(p * (float)Math.PI);
            float wrist = MathHelper.sin(Math.min(1.0F, p * 1.8F) * (float)Math.PI);
            float recover = MathHelper.sin(Math.max(0.0F, p - 0.48F) * 1.92F * (float)Math.PI);

            mainArm.pitch -= 0.18F * elbow;
            mainArm.yaw -= sign * (0.08F + 0.12F * elbow);
            mainArm.roll += sign * (0.14F * wrist - 0.08F * recover);
            offArm.pitch += 0.09F * elbow;
            offArm.yaw += sign * 0.055F * elbow;
            offArm.roll -= sign * 0.045F * wrist;
            return;
        }

        // Item-use joint polish: makes block/bow/spear poses less rigid.
        if (state.isUsingItem) {
            switch (state.rightArmPose) {
                case BLOCK -> {
                    model.rightArm.pitch -= 0.10F;
                    model.rightArm.yaw -= 0.12F;
                    model.rightArm.roll += 0.16F;
                    model.leftArm.roll -= 0.08F;
                }
                case BOW_AND_ARROW -> {
                    model.rightArm.roll += 0.08F;
                    model.leftArm.roll -= 0.10F;
                    model.leftArm.yaw += 0.07F;
                }
                case CROSSBOW_CHARGE, CROSSBOW_HOLD -> {
                    model.rightArm.roll += 0.07F;
                    model.leftArm.roll -= 0.07F;
                }
                case SPEAR -> {
                    model.rightArm.pitch -= 0.12F;
                    model.rightArm.yaw -= 0.10F;
                    model.rightArm.roll += 0.11F;
                }
                default -> { }
            }
            return;
        }

        // Manual emotes: add small joint motion instead of perfectly rigid arms.
        if (AnimeAnimationClient.getEmoteType() != 0) {
            int emote = AnimeAnimationClient.getEmoteType();
            float pulse = MathHelper.sin(age * 0.19F);
            float amount = (emote == 1 || emote == 6 || emote == 8) ? 0.10F : 0.055F;
            model.rightArm.yaw += pulse * amount;
            model.leftArm.yaw -= pulse * amount;
            model.rightArm.roll += breath * amount * 0.75F;
            model.leftArm.roll -= breath * amount * 0.75F;
            return;
        }

        // Locomotion/idle: subtle elbow bend and wrist swing so arms do not feel like rods.
        float amp = MathHelper.clamp(state.limbSwingAmplitude, 0.0F, 1.0F);
        if (amp > 0.035F) {
            float step = MathHelper.sin(state.limbSwingAnimationProgress * 0.72F);
            model.rightArm.yaw += step * 0.065F * amp;
            model.leftArm.yaw -= step * 0.065F * amp;
            model.rightArm.roll += (0.035F + step * 0.025F) * amp;
            model.leftArm.roll -= (0.035F + step * 0.025F) * amp;
        } else if (sword) {
            // Ready stance: main elbow tucked, blade wrist angled; off-hand relaxed guard.
            mainArm.pitch -= 0.08F + breath * 0.012F;
            mainArm.yaw -= sign * 0.08F;
            mainArm.roll += sign * (0.14F + breath * 0.018F);
            offArm.pitch += 0.06F;
            offArm.roll -= sign * 0.055F;
        } else {
            model.rightArm.yaw -= 0.035F + breath * 0.012F;
            model.rightArm.roll += 0.045F + breath * 0.014F;
            model.leftArm.yaw += 0.025F - breath * 0.010F;
            model.leftArm.roll -= 0.030F - breath * 0.010F;
        }
    }
}
