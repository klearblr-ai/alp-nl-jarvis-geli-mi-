package com.klearblrz.arrowrip.mixin;

import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Only two combat changes:
 *  - empty hand: one clean punch
 *  - sword: one clean diagonal slash
 * Other held items keep vanilla swing/use animations.
 */
@Mixin(PlayerEntityModel.class)
public abstract class SimpleCombatMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$simpleCombat(PlayerEntityRenderState state, CallbackInfo ci) {
        if (state.spectator || state.hasVehicle || state.touchingWater || state.isGliding || state.leaningPitch > 0.20F) return;
        if (state.isUsingItem) return;

        float p = MathHelper.clamp(state.handSwingProgress, 0.0F, 1.0F);
        if (p <= 0.001F) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        ItemStack main = state.getMainHandItemStack();
        boolean sword = !main.isEmpty() && main.isIn(ItemTags.SWORDS);
        boolean empty = main.isEmpty();
        if (!sword && !empty) return;

        boolean right = state.mainArm == Arm.RIGHT;
        var attackArm = right ? model.rightArm : model.leftArm;
        var offArm = right ? model.leftArm : model.rightArm;
        float side = right ? 1.0F : -1.0F;

        // One smooth impulse for the whole vanilla swing cycle.
        float hit = MathHelper.sin(p * (float)Math.PI);
        float settle = MathHelper.sin(Math.min(1.0F, p * 1.30F) * (float)Math.PI);

        if (sword) {
            // Single diagonal sword slash. No combo cycling, no alternate styles.
            model.body.yaw += side * (0.18F + 0.34F * hit);
            model.body.roll -= side * 0.055F * hit;

            attackArm.pitch = -0.72F - 1.02F * hit;
            attackArm.yaw = -side * (0.30F + 0.56F * hit);
            attackArm.roll = side * (0.16F + 0.34F * settle);

            // Off hand stabilizes the torso without becoming a second attack.
            offArm.pitch = MathHelper.lerp(hit, offArm.pitch, -0.34F);
            offArm.yaw = MathHelper.lerp(hit, offArm.yaw, side * 0.16F);
            offArm.roll = MathHelper.lerp(hit, offArm.roll, -side * 0.10F);
        } else {
            // Empty-hand punch: shoulder drives forward, elbow straightens, torso turns once.
            model.body.yaw += side * 0.42F * hit;
            model.body.pitch += 0.07F * hit;

            attackArm.pitch = -0.52F - 1.02F * hit;
            attackArm.yaw = -side * (0.10F + 0.18F * hit);
            attackArm.roll = side * 0.06F * hit;

            // Guard hand stays close to the chest.
            offArm.pitch = MathHelper.lerp(hit, offArm.pitch, -0.62F);
            offArm.yaw = MathHelper.lerp(hit, offArm.yaw, side * 0.28F);
            offArm.roll = MathHelper.lerp(hit, offArm.roll, -side * 0.12F);
        }
    }
}
