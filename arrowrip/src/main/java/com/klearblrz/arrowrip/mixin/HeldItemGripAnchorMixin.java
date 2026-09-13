package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.ProceduralAIRigClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * True articulated held-item anchor.
 *
 * Vanilla held items are normally attached to the shoulder/arm transform. ArrowRip replaces
 * that arm with upper-arm -> forearm -> hand bones, so using the vanilla anchor makes items
 * look like they slide out of the palm. This mixin makes the final hand bone the real item
 * anchor and keeps the visible articulated arm enabled while an item is held.
 */
@Mixin(PlayerEntityModel.class)
public abstract class HeldItemGripAnchorMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$keepRigHandsVisible(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId() || !ProceduralAIRigClient.isEnabled()) return;
        if (client.options.getPerspective().isFirstPerson()) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        ModelPart root = model.getRootPart();
        if (!root.hasChild("arrowrip_ai_rig")) return;
        ModelPart rig = root.getChild("arrowrip_ai_rig");
        if (!rig.visible) return;

        // The articulated limbs stay visible even while holding an item. The item itself is
        // re-anchored to the articulated palm by arrowrip$anchorHeldItem below.
        model.rightArm.visible = false;
        model.leftArm.visible = false;
        rig.getChild("arrowrip_r_upper_arm").visible = true;
        rig.getChild("arrowrip_l_upper_arm").visible = true;
    }

    @Inject(
            method = "setArmAngle(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void arrowrip$anchorHeldItem(PlayerEntityRenderState state, Arm arm, MatrixStack matrices, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId() || !ProceduralAIRigClient.isEnabled()) return;
        if (client.options.getPerspective().isFirstPerson() || state.spectator) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        ModelPart root = model.getRootPart();
        if (!root.hasChild("arrowrip_ai_rig")) return;

        ModelPart rig = root.getChild("arrowrip_ai_rig");
        if (!rig.visible) return;

        boolean right = arm == Arm.RIGHT;
        String side = right ? "r" : "l";
        ModelPart upper = rig.getChild("arrowrip_" + side + "_upper_arm");
        ModelPart forearm = upper.getChild("arrowrip_" + side + "_forearm");
        ModelPart hand = forearm.getChild("arrowrip_" + side + "_hand");

        // Follow the exact visible arm chain all the way to the palm.
        root.applyTransform(matrices);
        rig.applyTransform(matrices);
        upper.applyTransform(matrices);
        forearm.applyTransform(matrices);
        hand.applyTransform(matrices);

        // HeldItemFeatureRenderer will still append its vanilla -90X / 180Y / shoulder-to-hand
        // offset after setArmAngle(). Pre-compensate that large offset, then replace it with a
        // tiny palm-center offset so the grip sits INSIDE the hand instead of floating away.
        float vanillaX = (right ? 1.0F : -1.0F) / 16.0F;
        float vanillaY = 0.125F;
        float vanillaZ = -0.625F;

        float palmX = (right ? 1.0F : -1.0F) * 0.010F;
        float palmY = 0.035F;
        float palmZ = -0.090F;

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        matrices.translate(palmX - vanillaX, palmY - vanillaY, palmZ - vanillaZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));

        ci.cancel();
    }
}
