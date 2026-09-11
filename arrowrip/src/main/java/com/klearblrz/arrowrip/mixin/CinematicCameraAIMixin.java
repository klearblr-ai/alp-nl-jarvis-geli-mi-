package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AutoPvPAnimationClient;
import com.klearblrz.arrowrip.ProceduralAIRigClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frame-by-frame procedural movie camera. The shot is generated from live player
 * state and combat impulses, so it follows the full-body AI instead of replaying a
 * fixed camera clip.
 */
@Mixin(Camera.class)
public abstract class CinematicCameraAIMixin {
    @Shadow protected abstract void moveBy(float surge, float heave, float sway);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    private float arrowrip$surge;
    private float arrowrip$heave;
    private float arrowrip$sway;
    private float arrowrip$yaw;
    private float arrowrip$pitch;

    @Inject(method = "update", at = @At("TAIL"))
    private void arrowrip$movieCamera(World world, Entity focusedEntity, boolean thirdPerson,
                                      boolean inverseView, float tickProgress, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ProceduralAIRigClient.isEnabled() || client.player == null || focusedEntity != client.player || !thirdPerson) {
            arrowrip$surge = arrowrip$heave = arrowrip$sway = arrowrip$yaw = arrowrip$pitch = 0.0F;
            return;
        }

        Camera self = (Camera)(Object)this;
        float age = client.player.age + tickProgress;
        float energy = ProceduralAIRigClient.energy();
        float combat = ProceduralAIRigClient.combat();
        float focus = ProceduralAIRigClient.focus();
        float turn = ProceduralAIRigClient.turnVelocity();
        float strain = ProceduralAIRigClient.strain();

        // Base handheld/director drift. Very small at idle, stronger during movement.
        float wantedSurge = 0.05F + MathHelper.sin(age * 0.055F) * 0.035F;
        float wantedHeave = MathHelper.sin(age * 0.041F + 1.2F) * (0.025F + energy * 0.030F);
        float wantedSway = MathHelper.sin(age * 0.033F) * (0.055F + energy * 0.055F);
        float wantedYaw = -turn * (2.0F + 2.8F * energy) + MathHelper.sin(age * 0.024F) * 0.7F;
        float wantedPitch = -1.0F * strain + MathHelper.sin(age * 0.037F + 0.7F) * 0.35F;

        if (client.player.isSprinting()) {
            // Chase shot: pull slightly back and lower the lens.
            wantedSurge -= 0.28F + 0.12F * energy;
            wantedHeave -= 0.08F;
            wantedPitch += 2.2F;
            wantedSway += turn * 0.18F;
        }

        if (combat > 0.08F) {
            // Shoulder/orbit combat framing.
            float orbit = MathHelper.sin(age * 0.055F + focus * 1.7F);
            wantedSway += orbit * (0.16F + 0.18F * combat);
            wantedSurge += 0.12F * combat;
            wantedYaw += orbit * (2.3F + 2.8F * focus) * combat;
            wantedPitch -= 0.9F * combat;
        }

        if (client.player.handSwinging) {
            float hit = MathHelper.sin(MathHelper.clamp(client.player.handSwingProgress, 0.0F, 1.0F) * (float)Math.PI);
            wantedSurge += 0.22F * hit;
            wantedSway += MathHelper.sin(age * 0.42F) * 0.12F * hit;
            wantedYaw += MathHelper.sin(age * 0.53F) * 2.6F * hit;
        }

        if (AutoPvPAnimationClient.getStabTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getStabTicks() / 13.0F;
            float impulse = MathHelper.sin(MathHelper.clamp(p, 0.0F, 1.0F) * (float)Math.PI);
            wantedSurge += 0.72F * impulse;
            wantedHeave -= 0.10F * impulse;
            wantedSway += 0.28F * impulse;
            wantedYaw -= 5.0F * impulse;
            wantedPitch += 2.0F * impulse;
        }

        if (AutoPvPAnimationClient.getPunchTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getPunchTicks() / 9.0F;
            float hit = MathHelper.sin(MathHelper.clamp(p, 0.0F, 1.0F) * (float)Math.PI);
            wantedSurge += 0.40F * hit;
            wantedSway -= 0.22F * hit;
            wantedYaw += 4.0F * hit;
        }

        if (AutoPvPAnimationClient.getKickTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getKickTicks() / 15.0F;
            float kick = MathHelper.sin(MathHelper.clamp(p, 0.0F, 1.0F) * (float)Math.PI);
            wantedHeave -= 0.28F * kick;
            wantedSway += 0.34F * kick;
            wantedPitch += 4.8F * kick;
            wantedYaw -= 3.4F * kick;
        }

        if (AutoPvPAnimationClient.getMineTicks() > 0) {
            wantedSway -= 0.18F;
            wantedHeave += 0.05F;
            wantedYaw += MathHelper.sin(age * 0.55F) * 1.3F;
        }

        if (AutoPvPAnimationClient.getPlaceTicks() > 0) {
            wantedSway += 0.24F;
            wantedSurge += 0.14F;
            wantedPitch += 1.6F;
        }

        if (AutoPvPAnimationClient.getLandingTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getLandingTicks() / 10.0F;
            float land = 1.0F - smooth(MathHelper.clamp(p, 0.0F, 1.0F));
            wantedHeave -= 0.20F * land;
            wantedPitch += 2.8F * land;
        }

        if (AutoPvPAnimationClient.getHurtTicks() > 0) {
            float p = 1.0F - AutoPvPAnimationClient.getHurtTicks() / 11.0F;
            float recoil = MathHelper.sin(MathHelper.clamp(p, 0.0F, 1.0F) * (float)Math.PI);
            wantedSurge -= 0.26F * recoil;
            wantedSway += MathHelper.sin(age * 0.9F) * 0.20F * recoil;
            wantedYaw += MathHelper.sin(age * 1.2F) * 4.5F * recoil;
            wantedPitch -= 2.0F * recoil;
        }

        if (AutoPvPAnimationClient.getFinisherTicks() > 0) {
            // Most cinematic shot: close orbit around the local player during the finish.
            float t = AutoPvPAnimationClient.getFinisherTicks();
            float orbit = age * 0.095F + t * 0.028F;
            wantedSurge += 0.82F;
            wantedHeave += 0.06F + MathHelper.sin(orbit * 0.65F) * 0.10F;
            wantedSway += MathHelper.sin(orbit) * 0.52F;
            wantedYaw += MathHelper.sin(orbit) * 8.5F;
            wantedPitch -= 2.8F + MathHelper.cos(orbit) * 1.6F;
        }

        // Per-frame smoothing makes the camera feel keyframed without authored keyframes.
        arrowrip$surge = lerp(arrowrip$surge, wantedSurge, 0.16F);
        arrowrip$heave = lerp(arrowrip$heave, wantedHeave, 0.14F);
        arrowrip$sway = lerp(arrowrip$sway, wantedSway, 0.15F);
        arrowrip$yaw = lerp(arrowrip$yaw, wantedYaw, 0.13F);
        arrowrip$pitch = lerp(arrowrip$pitch, wantedPitch, 0.13F);

        moveBy(arrowrip$surge, arrowrip$heave, arrowrip$sway);
        setRotation(self.getYaw() + arrowrip$yaw, self.getPitch() + arrowrip$pitch);
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * MathHelper.clamp(amount, 0.0F, 1.0F);
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }
}
