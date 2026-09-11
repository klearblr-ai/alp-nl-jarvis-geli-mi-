package com.klearblrz.arrowrip.mixin;

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
public abstract class SwordComboMixin {
    private static int arrowrip$swordStyle = 0;
    private static float arrowrip$lastSwing = 0.0F;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$eightSwordStyles(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) return;
        if (!state.getMainHandItemStack().isIn(ItemTags.SWORDS)) {
            arrowrip$lastSwing = state.handSwingProgress;
            return;
        }

        float p = state.handSwingProgress;
        if (p <= 0.001F) {
            arrowrip$lastSwing = 0.0F;
            return;
        }

        if (arrowrip$lastSwing <= 0.001F || p < arrowrip$lastSwing - 0.20F) {
            arrowrip$swordStyle++;
            if (arrowrip$swordStyle > 8) arrowrip$swordStyle = 1;
        }
        arrowrip$lastSwing = p;

        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        boolean right = state.mainArm == Arm.RIGHT;
        float sign = right ? 1.0F : -1.0F;
        var swordArm = right ? model.rightArm : model.leftArm;
        var offArm = right ? model.leftArm : model.rightArm;

        float slash = MathHelper.sin(p * (float) Math.PI);
        float fast = MathHelper.sin(Math.min(1.0F, p * 1.7F) * (float) Math.PI);
        float late = MathHelper.sin(Math.max(0.0F, p - 0.36F) * 1.56F * (float) Math.PI);

        switch (arrowrip$swordStyle) {
            case 1 -> { // sağ/sol çapraz aşağı kesiş
                model.body.yaw = sign * (0.28F + 0.58F * slash);
                model.body.pitch = 0.10F + 0.16F * slash;
                model.head.yaw = -sign * 0.22F * slash;
                swordArm.pitch = -0.92F - 1.42F * slash;
                swordArm.yaw = -sign * (0.30F + 0.62F * slash);
                swordArm.roll = sign * (0.16F + 0.34F * fast);
                offArm.pitch = 0.32F + 0.30F * slash;
                offArm.yaw = sign * 0.20F;
            }
            case 2 -> { // ters çapraz yukarı kesiş
                model.body.yaw = -sign * (0.22F + 0.52F * slash);
                model.body.pitch = -0.06F + 0.18F * slash;
                model.head.roll = sign * 0.08F * fast;
                swordArm.pitch = 0.38F - 1.62F * slash;
                swordArm.yaw = sign * (0.22F + 0.74F * slash);
                swordArm.roll = -sign * (0.18F + 0.44F * slash);
                offArm.pitch = -0.18F + 0.42F * slash;
                offArm.roll = sign * 0.20F;
            }
            case 3 -> { // yatay bel hizası kesiş
                model.body.yaw = sign * (0.18F + 0.88F * slash);
                model.body.roll = -sign * 0.08F * slash;
                model.head.yaw = -sign * 0.34F * slash;
                swordArm.pitch = -0.58F - 0.46F * slash;
                swordArm.yaw = -sign * (0.52F + 1.02F * slash);
                swordArm.roll = sign * 0.42F * fast;
                offArm.pitch = 0.44F;
                offArm.yaw = sign * 0.32F;
            }
            case 4 -> { // anime uppercut slash
                model.body.pitch = 0.24F - 0.34F * slash;
                model.body.yaw = -sign * 0.36F * slash;
                model.head.pitch = -0.14F * slash;
                swordArm.pitch = 0.82F - 2.18F * slash;
                swordArm.yaw = sign * 0.38F * slash;
                swordArm.roll = -sign * 0.22F * fast;
                offArm.pitch = -0.44F + 0.30F * slash;
                model.rightLeg.pitch += 0.18F * slash;
                model.leftLeg.pitch -= 0.14F * slash;
            }
            case 5 -> { // overhead ağır vuruş
                model.body.pitch = -0.18F + 0.48F * slash;
                model.head.pitch = -0.22F + 0.28F * slash;
                swordArm.pitch = -2.72F + 2.18F * p;
                swordArm.yaw = -sign * 0.16F;
                swordArm.roll = sign * 0.10F;
                offArm.pitch = -1.52F + 1.12F * p;
                offArm.yaw = sign * 0.18F;
                model.rightLeg.pitch += 0.12F * fast;
                model.leftLeg.pitch -= 0.10F * fast;
            }
            case 6 -> { // reverse-grip / backhand slash
                model.body.yaw = -sign * (0.34F + 0.66F * slash);
                model.body.roll = sign * 0.10F * slash;
                model.head.yaw = sign * 0.24F * slash;
                swordArm.pitch = -0.44F - 1.06F * slash;
                swordArm.yaw = sign * (0.62F + 0.82F * slash);
                swordArm.roll = -sign * (0.74F + 0.36F * fast);
                offArm.pitch = -0.24F + 0.46F * slash;
                offArm.roll = sign * 0.26F;
            }
            case 7 -> { // iki aşamalı cross-cut
                float second = MathHelper.sin(Math.max(0.0F, p - 0.42F) * 1.72F * (float) Math.PI);
                model.body.yaw = sign * (0.62F * slash - 0.78F * second);
                model.head.yaw = -sign * (0.16F * slash - 0.20F * second);
                swordArm.pitch = -0.86F - 1.18F * slash + 0.52F * second;
                swordArm.yaw = -sign * (0.54F * slash - 0.88F * second);
                swordArm.roll = sign * (0.20F + 0.32F * fast - 0.48F * second);
                offArm.pitch = 0.26F + 0.34F * slash;
                offArm.yaw = sign * 0.22F;
            }
            case 8 -> { // anime spin-cut finisher
                float spin = MathHelper.sin(p * (float) Math.PI * 0.94F);
                model.body.yaw = -sign * (0.42F + 1.18F * spin) + sign * 0.24F * late;
                model.body.roll = -sign * 0.16F * slash;
                model.body.pitch = 0.10F + 0.18F * slash;
                model.head.yaw = sign * 0.64F * spin;
                model.head.roll = sign * 0.12F * fast;
                swordArm.pitch = -0.92F - 1.08F * slash;
                swordArm.yaw = sign * (0.22F + 1.12F * slash);
                swordArm.roll = -sign * (0.20F + 0.64F * fast);
                offArm.pitch = -0.28F + 0.48F * slash;
                offArm.yaw = -sign * 0.36F;
                model.rightLeg.pitch += 0.24F * slash;
                model.leftLeg.pitch -= 0.20F * slash;
            }
            default -> { }
        }
    }
}
