package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.SwordDisarmClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Full-body recoil for the client-side sword disarm illusion. */
@Mixin(value = PlayerEntityModel.class, priority = 120)
public abstract class SwordDisarmPoseMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$disarmRecoil(PlayerEntityRenderState state, CallbackInfo ci) {
        if (!SwordDisarmClient.isActive()) return;
        boolean target = state.id == SwordDisarmClient.getTargetId();
        boolean attacker = state.id == SwordDisarmClient.getAttackerId();
        if (!target && !attacker) return;

        PlayerEntityModel m = (PlayerEntityModel)(Object)this;
        float p = SwordDisarmClient.getProgress();
        float clash = MathHelper.sin(MathHelper.clamp(p / 0.38F, 0.0F, 1.0F) * (float)Math.PI);
        float recover = smooth(MathHelper.clamp((p - 0.48F) / 0.52F, 0.0F, 1.0F));

        if (target) {
            // Hand gets knocked outward, shoulders recoil, stance opens up.
            m.body.yaw += 0.58F * clash;
            m.body.roll -= 0.18F * clash;
            m.head.yaw -= 0.36F * clash;
            m.head.roll += 0.12F * clash;
            m.rightArm.pitch = -0.46F + 0.82F * clash - 0.22F * recover;
            m.rightArm.yaw = -1.02F * clash;
            m.rightArm.roll = 0.92F * clash;
            m.leftArm.pitch = -0.34F - 0.38F * clash;
            m.leftArm.yaw = 0.24F * clash;
            m.rightLeg.pitch = -0.16F * clash;
            m.leftLeg.pitch = 0.22F * clash;
        } else {
            // Attacker follows through as if striking the opponent's blade rather than the body.
            m.body.yaw -= 0.46F * clash;
            m.body.roll += 0.08F * clash;
            m.head.yaw += 0.20F * clash;
            m.rightArm.pitch = -1.38F - 0.44F * clash;
            m.rightArm.yaw = -0.72F * clash;
            m.rightArm.roll = 0.30F * clash;
            m.leftArm.pitch = -0.62F;
            m.leftArm.yaw = 0.22F;
            m.rightLeg.pitch = 0.24F * clash;
            m.leftLeg.pitch = -0.16F * clash;
        }
    }

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
