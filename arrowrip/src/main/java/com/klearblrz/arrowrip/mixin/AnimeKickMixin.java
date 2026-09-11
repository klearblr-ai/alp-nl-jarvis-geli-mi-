package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AnimeAnimationClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class AnimeKickMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$animeKick(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) return;

        int ticks = AnimeAnimationClient.getKickTicks();
        int type = AnimeAnimationClient.getKickType();
        if (ticks <= 0 || type <= 0) return;

        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        float progress = MathHelper.clamp(1.0F - ticks / 18.0F, 0.0F, 1.0F);
        float strike = MathHelper.sin(progress * (float) Math.PI);
        float snap = MathHelper.sin(Math.min(1.0F, progress * 1.55F) * (float) Math.PI);
        float recoil = MathHelper.sin(Math.max(0.0F, progress - 0.48F) * 1.92F * (float) Math.PI);

        // Guard arms shared by all four kicks.
        model.rightArm.pitch = -0.78F + 0.16F * snap;
        model.leftArm.pitch = -0.88F - 0.12F * snap;
        model.rightArm.yaw = -0.30F;
        model.leftArm.yaw = 0.30F;
        model.rightArm.roll = 0.42F;
        model.leftArm.roll = -0.42F;
        model.head.pitch = -0.08F + 0.10F * recoil;

        switch (type) {
            case 1 -> { // Front kick
                model.body.pitch = 0.20F * strike;
                model.body.yaw = -0.10F * strike;
                model.head.yaw = 0.08F * strike;
                model.rightLeg.pitch = -0.38F - 1.48F * strike;
                model.rightLeg.yaw = -0.06F;
                model.rightLeg.roll = -0.04F;
                model.leftLeg.pitch = 0.20F + 0.24F * strike;
                model.leftLeg.yaw = 0.05F;
                model.leftLeg.roll = 0.05F;
            }
            case 2 -> { // Roundhouse
                model.body.pitch = 0.13F * strike;
                model.body.yaw = -0.82F * strike;
                model.body.roll = -0.12F * strike;
                model.head.yaw = 0.42F * strike;
                model.head.roll = 0.09F * strike;
                model.rightLeg.pitch = -0.64F - 0.54F * strike;
                model.rightLeg.yaw = -1.02F * strike;
                model.rightLeg.roll = -0.82F * strike;
                model.leftLeg.pitch = 0.22F + 0.18F * strike;
                model.leftLeg.yaw = 0.18F * strike;
                model.leftLeg.roll = 0.10F * strike;
            }
            case 3 -> { // Side kick
                model.body.pitch = 0.10F * strike;
                model.body.yaw = 0.62F * strike;
                model.body.roll = 0.18F * strike;
                model.head.yaw = -0.34F * strike;
                model.head.roll = -0.08F * strike;
                model.leftLeg.pitch = -0.54F - 0.48F * strike;
                model.leftLeg.yaw = 0.84F * strike;
                model.leftLeg.roll = 1.12F * strike;
                model.rightLeg.pitch = 0.24F + 0.20F * strike;
                model.rightLeg.yaw = -0.14F * strike;
                model.rightLeg.roll = -0.08F * strike;
            }
            case 4 -> { // Spinning heel kick
                float spin = MathHelper.sin(progress * (float) Math.PI * 0.92F);
                model.body.pitch = 0.16F * strike;
                model.body.yaw = -1.15F * spin + 0.30F * recoil;
                model.body.roll = -0.20F * strike;
                model.head.yaw = 0.72F * spin;
                model.head.roll = 0.12F * strike;
                model.rightLeg.pitch = -0.50F - 0.58F * strike;
                model.rightLeg.yaw = -1.28F * strike;
                model.rightLeg.roll = -1.02F * strike;
                model.leftLeg.pitch = 0.18F + 0.32F * strike;
                model.leftLeg.yaw = 0.30F * strike;
                model.leftLeg.roll = 0.16F * strike;
                model.rightArm.pitch = -1.05F + 0.22F * snap;
                model.leftArm.pitch = -0.46F - 0.18F * snap;
            }
            default -> { }
        }
    }
}
