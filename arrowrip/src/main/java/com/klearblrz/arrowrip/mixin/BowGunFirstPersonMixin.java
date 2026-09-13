package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.BowGunClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces vanilla bow draw animation with a compact pistol-like first-person animation. */
@Mixin(HeldItemRenderer.class)
public abstract class BowGunFirstPersonMixin {
    @Shadow
    public abstract void renderItem(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext renderMode,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int light);

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void arrowrip$gunBow(
            AbstractClientPlayerEntity player,
            float tickProgress,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            CallbackInfo ci) {
        if (!item.isOf(Items.BOW)) return;

        boolean main = hand == Hand.MAIN_HAND;
        Arm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean right = arm == Arm.RIGHT;
        float side = right ? 1.0F : -1.0F;

        matrices.push();

        // Stable grip: do not feed rapidly changing equipProgress into the base pose.
        matrices.translate(side * 0.54F, -0.43F, -0.82F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-8.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -5.0F));
        matrices.scale(0.90F, 0.90F, 0.90F);

        float recoil = BowGunClient.recoilProgress(tickProgress);
        if (recoil > 0.0F) {
            float kick = MathHelper.sin(recoil * (float)Math.PI);
            matrices.translate(0.0F, 0.025F * kick, 0.10F * kick);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-16.0F * kick));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 2.8F * kick));
        }

        float inspect = BowGunClient.inspectProgress(tickProgress);
        if (inspect > 0.0F) {
            float in = smooth(MathHelper.clamp(inspect / 0.22F, 0.0F, 1.0F));
            float out = smooth(MathHelper.clamp((1.0F - inspect) / 0.22F, 0.0F, 1.0F));
            float hold = Math.min(in, out);
            float orbit = MathHelper.sin(inspect * (float)Math.PI);
            matrices.translate(side * -0.18F * hold, 0.18F * hold, -0.30F * hold);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (72.0F * hold + 22.0F * orbit)));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(24.0F * orbit));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * (-18.0F * hold + 9.0F * MathHelper.sin(inspect * (float)Math.PI * 2.0F))));
        }

        // Three clean reload phases: lower/eject -> return/seat -> short lock/slide motion.
        float reload = BowGunClient.reloadProgress(tickProgress);
        if (reload > 0.0F) {
            float lower = smooth(MathHelper.clamp(reload / 0.34F, 0.0F, 1.0F));
            float back = smooth(MathHelper.clamp((reload - 0.34F) / 0.46F, 0.0F, 1.0F));
            float lock = smooth(MathHelper.clamp((reload - 0.80F) / 0.20F, 0.0F, 1.0F));
            float down = lower * (1.0F - back);

            matrices.translate(side * 0.08F * down, 0.36F * down - 0.08F * back, 0.10F * down + 0.05F * lock);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * (32.0F * down - 7.0F * back)));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(22.0F * down - 7.0F * lock));
        }

        this.renderItem(
                player,
                item,
                right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                matrices,
                queue,
                light);

        // Fake magazine: rendered only during reload. It never exists in inventory or on the server.
        if (reload > 0.02F) {
            float appear = smooth(MathHelper.clamp(reload / 0.16F, 0.0F, 1.0F));
            float seat = smooth(MathHelper.clamp((reload - 0.52F) / 0.42F, 0.0F, 1.0F));
            float lift = MathHelper.sin(MathHelper.clamp(reload / 0.76F, 0.0F, 1.0F) * (float)Math.PI);
            matrices.push();
            matrices.translate(-side * (0.58F - 0.16F * seat), 0.22F - 0.24F * lift + 0.12F * seat, -0.26F + 0.18F * seat);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(76.0F - 42.0F * seat));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-side * (24.0F - 14.0F * seat)));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * (12.0F + 18.0F * lift)));
            matrices.scale(0.62F * appear, 1.12F * appear, 0.72F * appear);
            this.renderItem(
                    player,
                    new ItemStack(Items.NETHER_BRICK),
                    right ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                    matrices,
                    queue,
                    light);
            matrices.pop();
        }

        matrices.pop();
        ci.cancel();
    }

    private static float smooth(float x) {
        return x * x * (3.0F - 2.0F * x);
    }
}
