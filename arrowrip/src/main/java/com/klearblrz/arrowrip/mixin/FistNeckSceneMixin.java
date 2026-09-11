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
public abstract class FistNeckSceneMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$specialScenes(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        PlayerEntityModel model = (PlayerEntityModel) (Object) this;

        // Fist clash emote: both fists pull back, strike together, then recoil.
        if (state.id == client.player.getId() && AnimeAnimationClient.getEmoteType() == 10) {
            int ticks = AnimeAnimationClient.getEmoteTicks();
            float p = MathHelper.clamp(1.0F - ticks / 140.0F, 0.0F, 1.0F);
            float cycle = (p * 3.0F) % 1.0F;
            float hit = MathHelper.sin(MathHelper.clamp(cycle * 1.45F, 0.0F, 1.0F) * (float)Math.PI);
            float recoil = MathHelper.sin(MathHelper.clamp((cycle - 0.38F) * 1.65F, 0.0F, 1.0F) * (float)Math.PI);
            model.body.pitch = 0.10F + 0.08F * hit;
            model.head.pitch = -0.09F + 0.05F * recoil;
            model.rightArm.pitch = -1.20F - 0.42F * hit + 0.18F * recoil;
            model.leftArm.pitch = -1.20F - 0.42F * hit + 0.18F * recoil;
            model.rightArm.yaw = -0.92F + 0.74F * hit;
            model.leftArm.yaw = 0.92F - 0.74F * hit;
            model.rightArm.roll = 0.38F - 0.26F * hit;
            model.leftArm.roll = -0.38F + 0.26F * hit;
            model.rightLeg.pitch = 0.12F;
            model.leftLeg.pitch = -0.10F;
            return;
        }

        int neckTicks = AnimeAnimationClient.getNeckSnapTicks();
        if (neckTicks <= 0) return;
        int duration = Math.max(1, AnimeAnimationClient.getNeckSnapDuration());
        float p = MathHelper.clamp(1.0F - (float)neckTicks / duration, 0.0F, 1.0F);
        float reach = smooth(MathHelper.clamp(p / 0.32F, 0.0F, 1.0F));
        float snap = smooth(MathHelper.clamp((p - 0.42F) / 0.18F, 0.0F, 1.0F));
        float release = smooth(MathHelper.clamp((p - 0.76F) / 0.20F, 0.0F, 1.0F));
        float hold = 1.0F - release;

        if (state.id == client.player.getId()) {
            // Attacker: grab around shoulder/neck height, twist, then release.
            model.body.pitch = 0.18F * reach * hold;
            model.body.yaw = -0.32F * reach * hold + 0.58F * snap * hold;
            model.head.yaw = 0.18F * reach * hold;
            model.rightArm.pitch = (-1.32F - 0.20F * snap) * reach * hold;
            model.leftArm.pitch = (-1.28F - 0.14F * snap) * reach * hold;
            model.rightArm.yaw = (-0.62F + 0.36F * snap) * reach * hold;
            model.leftArm.yaw = (0.62F - 0.36F * snap) * reach * hold;
            model.rightArm.roll = 0.22F * reach * hold;
            model.leftArm.roll = -0.22F * reach * hold;
            model.rightLeg.pitch = 0.18F * reach;
            model.leftLeg.pitch = -0.14F * reach;
        } else if (state.id == AnimeAnimationClient.getNeckSnapTargetId()) {
            // Target: visual-only head/upper-body reaction on this client.
            model.body.pitch = 0.12F * reach * hold;
            model.body.yaw = 0.18F * reach * hold + 0.36F * snap * hold;
            model.head.yaw = 0.20F * reach * hold + 1.18F * snap * hold;
            model.head.roll = 0.08F * reach * hold + 0.62F * snap * hold;
            model.head.pitch = -0.08F * reach * hold + 0.26F * snap * hold;
            model.rightArm.pitch = -0.22F * reach * hold;
            model.leftArm.pitch = -0.18F * reach * hold;
            model.rightArm.roll = 0.32F * snap * hold;
            model.leftArm.roll = -0.28F * snap * hold;
        }
    }

    private static float smooth(float x) {
        return x * x * (3.0F - 2.0F * x);
    }
}
