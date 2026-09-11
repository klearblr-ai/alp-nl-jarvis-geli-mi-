package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.ProceduralAIRigClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extra procedural locomotion layer for walk/run/speed-bridging. */
@Mixin(PlayerEntityModel.class)
public abstract class LocomotionBridgePolishMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$locomotionBridge(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ProceduralAIRigClient.isEnabled() || client.player == null || state.id != client.player.getId()) return;
        if (client.options.getPerspective().isFirstPerson() || state.spectator) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        ModelPart root = model.getRootPart();
        if (!root.hasChild("arrowrip_ai_rig")) return;
        ModelPart rig = root.getChild("arrowrip_ai_rig");
        if (!rig.visible) return;

        ModelPart ru = rig.getChild("arrowrip_r_upper_arm");
        ModelPart lu = rig.getChild("arrowrip_l_upper_arm");
        ModelPart rf = ru.getChild("arrowrip_r_forearm");
        ModelPart lf = lu.getChild("arrowrip_l_forearm");
        ModelPart rt = rig.getChild("arrowrip_r_thigh");
        ModelPart lt = rig.getChild("arrowrip_l_thigh");
        ModelPart rs = rt.getChild("arrowrip_r_shin");
        ModelPart ls = lt.getChild("arrowrip_l_shin");
        ModelPart rfoot = rs.getChild("arrowrip_r_foot");
        ModelPart lfoot = ls.getChild("arrowrip_l_foot");

        float age = state.age;
        float walk = ProceduralAIRigClient.walk();
        float run = ProceduralAIRigClient.run();
        float bridge = ProceduralAIRigClient.bridge();
        float phase = state.limbSwingAnimationProgress * (0.72F + run * 0.36F);

        if (walk > 0.03F && bridge < 0.15F) {
            float step = MathHelper.sin(phase);
            model.body.roll += step * 0.035F * walk;
            model.body.yaw += step * 0.045F * walk;
            model.head.roll -= step * 0.020F * walk;
            ru.roll += 0.05F * step * walk;
            lu.roll += 0.05F * step * walk;
            rs.pitch += Math.max(0.0F, step) * 0.15F * walk;
            ls.pitch += Math.max(0.0F, -step) * 0.15F * walk;
            rfoot.pitch -= 0.10F * step * walk;
            lfoot.pitch += 0.10F * step * walk;
        }

        if (run > 0.03F && bridge < 0.15F) {
            float step = MathHelper.sin(phase * 1.08F);
            float kick = Math.abs(MathHelper.cos(phase * 1.08F));
            model.body.pitch = Math.max(model.body.pitch, 0.34F + 0.16F * run);
            model.head.pitch -= 0.08F * run;
            model.body.yaw += step * 0.055F * run;
            ru.pitch -= 0.18F * run;
            lu.pitch -= 0.18F * run;
            rf.pitch -= 0.25F * run;
            lf.pitch -= 0.25F * run;
            rs.pitch += kick * 0.23F * run;
            ls.pitch += (1.0F - kick) * 0.19F * run;
            rfoot.pitch -= step * 0.12F * run;
            lfoot.pitch += step * 0.12F * run;
        }

        if (bridge > 0.03F) {
            float pulse = MathHelper.sin(age * 1.35F);
            float place = (MathHelper.sin(age * 2.65F) + 1.0F) * 0.5F;
            float side = MathHelper.sin(age * 0.72F);

            model.body.pitch = 0.46F + 0.08F * bridge;
            model.body.yaw += side * 0.10F * bridge;
            model.body.roll += side * 0.055F * bridge;
            model.head.pitch -= 0.18F * bridge;
            model.head.yaw -= side * 0.07F * bridge;

            ru.pitch = MathHelper.lerp(bridge, ru.pitch, -1.08F - place * 0.30F);
            rf.pitch = MathHelper.lerp(bridge, rf.pitch, -0.28F - place * 0.20F);
            ru.yaw = MathHelper.lerp(bridge, ru.yaw, -0.22F + side * 0.10F);
            lu.pitch = MathHelper.lerp(bridge, lu.pitch, -0.42F + pulse * 0.08F);
            lf.pitch = MathHelper.lerp(bridge, lf.pitch, -0.74F);

            rt.pitch = MathHelper.lerp(bridge, rt.pitch, 0.22F + side * 0.12F);
            lt.pitch = MathHelper.lerp(bridge, lt.pitch, -0.16F - side * 0.10F);
            rs.pitch = MathHelper.lerp(bridge, rs.pitch, 0.52F + place * 0.12F);
            ls.pitch = MathHelper.lerp(bridge, ls.pitch, 0.36F + (1.0F - place) * 0.12F);
            rfoot.pitch = MathHelper.lerp(bridge, rfoot.pitch, -0.20F);
            lfoot.pitch = MathHelper.lerp(bridge, lfoot.pitch, -0.12F);
            rig.yaw += side * 0.06F * bridge;
        }
    }
}
