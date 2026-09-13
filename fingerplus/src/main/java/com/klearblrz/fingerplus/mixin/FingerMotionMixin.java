package com.klearblrz.fingerplus.mixin;

import com.klearblrz.fingerplus.FingerPlusClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class FingerMotionMixin {
    private static final String[] DIGITS = {"pinky", "ring", "middle", "index"};

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void fingerplus$animate(PlayerEntityRenderState state, CallbackInfo ci) {
        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        applyHand(model.rightArm, true, state);
        applyHand(model.leftArm, false, state);
    }

    private static void applyHand(ModelPart arm, boolean right, PlayerEntityRenderState state) {
        String side = right ? "r" : "l";
        if (!arm.hasChild("fingerplus_" + side + "_index")) return;

        float sign = right ? 1.0F : -1.0F;
        float stride = MathHelper.clamp(state.limbSwingAmplitude * 1.45F, 0.0F, 1.0F);
        float phase = state.limbSwingAnimationProgress * 0.72F + (right ? 0.0F : (float)Math.PI);
        float runBoost = MathHelper.clamp((stride - 0.48F) * 1.8F, 0.0F, 0.65F);
        float motion = FingerPlusClient.movement();
        float baseCurl = FingerPlusClient.curl();
        float length = FingerPlusClient.length();
        float thick = FingerPlusClient.thickness();
        float spacing = FingerPlusClient.spacing();
        boolean visible = FingerPlusClient.enabled();

        for (int i = 0; i < DIGITS.length; i++) {
            ModelPart finger = arm.getChild("fingerplus_" + side + "_" + DIGITS[i]);
            finger.visible = visible;
            if (!visible) continue;

            float idle = MathHelper.sin(state.age * 0.085F + i * 0.62F) * 0.018F;
            float gait = MathHelper.sin(phase + i * 0.18F) * stride * (0.035F + runBoost * 0.035F) * motion;
            float fingerBias = i * 0.022F;
            finger.pitch = -(baseCurl * (0.70F + i * 0.08F)) + idle + gait - fingerBias;
            finger.yaw = sign * (i - 1.5F) * 0.025F;
            finger.roll = sign * (i - 1.5F) * 0.010F;
            finger.xScale = thick;
            finger.yScale = length;
            finger.zScale = thick;

            float defaultX = finger.getDefaultTransform().x();
            finger.originX = defaultX * spacing;
            finger.originY = finger.getDefaultTransform().y();
            finger.originZ = finger.getDefaultTransform().z();
        }

        ModelPart thumb = arm.getChild("fingerplus_" + side + "_thumb");
        thumb.visible = visible;
        if (!visible) return;

        float thumbIdle = MathHelper.sin(state.age * 0.075F + (right ? 0.4F : 1.2F)) * 0.018F;
        float thumbGait = MathHelper.sin(phase + 0.55F) * stride * 0.028F * motion;
        thumb.pitch = -0.22F - baseCurl * 0.62F + thumbIdle + thumbGait;
        thumb.yaw = -sign * (0.48F + baseCurl * 0.18F);
        thumb.roll = sign * 0.30F;
        thumb.xScale = thick;
        thumb.yScale = length * 0.92F;
        thumb.zScale = thick;
        thumb.originX = thumb.getDefaultTransform().x() * spacing;
        thumb.originY = thumb.getDefaultTransform().y();
        thumb.originZ = thumb.getDefaultTransform().z();
    }
}
