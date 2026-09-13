package com.klearblrz.fingers.mixin;

import com.klearblrz.fingers.FingersClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class FingerAnimationMixin {
    private static final String[] DIGITS = {"pinky", "ring", "middle", "index"};

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void fingersonly$animate(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        boolean local = state.id == client.player.getId();
        boolean thirdPerson = !client.options.getPerspective().isFirstPerson();
        boolean visible = local && thirdPerson && !state.spectator;

        setHandVisible(model.rightArm, true, visible);
        setHandVisible(model.leftArm, false, visible);
        if (!visible) return;

        boolean holding = !state.getMainHandItemStack().isEmpty();
        float targetCurl = FingersClient.curl;
        if (FingersClient.autoGrip && holding) targetCurl = Math.max(targetCurl, 0.72F);
        if (FingersClient.closed) targetCurl = 1.0F;
        if (state.handSwingProgress > 0.001F) {
            targetCurl = Math.max(targetCurl, 0.88F + 0.10F * MathHelper.sin(state.handSwingProgress * (float)Math.PI));
        }
        targetCurl = MathHelper.clamp(targetCurl, 0.0F, 1.0F);

        animateHand(model.rightArm, true, targetCurl, state.age);
        animateHand(model.leftArm, false, targetCurl, state.age + 4.7F);
    }

    private static void setHandVisible(ModelPart arm, boolean right, boolean visible) {
        String side = right ? "r" : "l";
        for (String digit : DIGITS) {
            if (arm.hasChild("fingersonly_" + side + "_" + digit)) {
                arm.getChild("fingersonly_" + side + "_" + digit).visible = visible;
            }
        }
        if (arm.hasChild("fingersonly_" + side + "_thumb")) {
            arm.getChild("fingersonly_" + side + "_thumb").visible = visible;
        }
    }

    private static void animateHand(ModelPart arm, boolean right, float curl, float age) {
        String side = right ? "r" : "l";
        float sign = right ? 1.0F : -1.0F;
        float idle = MathHelper.sin(age * 0.075F) * 0.035F * (1.0F - curl);

        for (int i = 0; i < DIGITS.length; i++) {
            String name = "fingersonly_" + side + "_" + DIGITS[i];
            if (!arm.hasChild(name)) continue;
            ModelPart finger = arm.getChild(name);

            finger.xScale = FingersClient.width;
            finger.yScale = FingersClient.length;
            finger.zScale = FingersClient.thickness;

            float individual = (i - 1.5F) * 0.055F;
            finger.pitch = 1.43F * curl + idle + individual * 0.15F * curl;
            finger.yaw = sign * individual * FingersClient.spread * (1.0F - curl * 0.78F);
            finger.roll = sign * individual * 0.36F * (1.0F - curl);
        }

        String thumbName = "fingersonly_" + side + "_thumb";
        if (arm.hasChild(thumbName)) {
            ModelPart thumb = arm.getChild(thumbName);
            thumb.xScale = FingersClient.width * 1.04F;
            thumb.yScale = FingersClient.length * 0.92F;
            thumb.zScale = FingersClient.thickness * 1.04F;
            thumb.pitch = 0.52F + curl * 0.70F + idle * 0.45F;
            thumb.yaw = sign * (-0.70F + curl * 0.46F);
            thumb.roll = sign * (0.30F + curl * 0.50F);
        }
    }
}
