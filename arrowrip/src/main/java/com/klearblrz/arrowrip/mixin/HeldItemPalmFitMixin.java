package com.klearblrz.arrowrip.mixin;

import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tiny palm-center correction for vanilla held-item rendering.
 * It does not create a new item animation; it only seats the item deeper in the hand
 * so the rectangular fingers visually wrap around it instead of floating beside it.
 */
@Mixin(PlayerEntityModel.class)
public abstract class HeldItemPalmFitMixin {
    @Inject(
            method = "setArmAngle(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;)V",
            at = @At("TAIL")
    )
    private void arrowrip$fitItemIntoPalm(PlayerEntityRenderState state, Arm arm, MatrixStack matrices, CallbackInfo ci) {
        if (state.spectator) return;

        // Less than one model pixel: enough to sit inside the finger cage without breaking packs.
        double side = arm == Arm.RIGHT ? -0.008 : 0.008;
        matrices.translate(side, -0.012, 0.040);
    }
}
