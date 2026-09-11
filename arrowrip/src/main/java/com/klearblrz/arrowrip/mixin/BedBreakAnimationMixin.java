package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AutoPvPAnimationClient;
import com.klearblrz.arrowrip.ProceduralAIRigClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class BedBreakAnimationMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$bedWarsBreak(PlayerEntityRenderState state, CallbackInfo ci) {
        if (!ProceduralAIRigClient.isEnabled() || AutoPvPAnimationClient.getBedBreakTicks() <= 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        ModelPart root = model.getRootPart();
        if (!root.hasChild("arrowrip_ai_rig")) return;
        ModelPart rig = root.getChild("arrowrip_ai_rig");
        if (!rig.visible) return;

        ModelPart ru = rig.getChild("arrowrip_r_upper_arm");
        ModelPart lu = rig.getChild("arrowrip_l_upper_arm");
        ModelPart rf = ru.getChild("arrowrip_r_forearm");
        ModelPart lf = lu.getChild("arrowrip_l_forearm");
        ModelPart rh = rf.getChild("arrowrip_r_hand");
        ModelPart lh = lf.getChild("arrowrip_l_hand");
        ModelPart rt = rig.getChild("arrowrip_r_thigh");
        ModelPart lt = rig.getChild("arrowrip_l_thigh");
        ModelPart rs = rt.getChild("arrowrip_r_shin");
        ModelPart ls = lt.getChild("arrowrip_l_shin");

        boolean rightMain = state.mainArm == Arm.RIGHT;
        ModelPart mainUpper = rightMain ? ru : lu;
        ModelPart mainFore = rightMain ? rf : lf;
        ModelPart mainHand = rightMain ? rh : lh;
        ModelPart offUpper = rightMain ? lu : ru;
        ModelPart offFore = rightMain ? lf : rf;
        ModelPart offHand = rightMain ? lh : rh;
        float side = rightMain ? 1.0F : -1.0F;

        float age = state.age;
        float cycle = (MathHelper.sin(age * 1.14F) + 1.0F) * 0.5F;
        float strike = MathHelper.sin(cycle * (float)Math.PI);

        // BedWars-specific low, aggressive break posture instead of generic mining.
        model.body.pitch = 0.52F + strike * 0.08F;
        model.body.yaw = -side * (0.12F + strike * 0.16F);
        model.head.pitch = 0.26F;

        mainUpper.pitch = -2.28F + cycle * 1.42F;
        mainUpper.yaw = -side * (0.24F + 0.18F * strike);
        mainUpper.roll = side * 0.10F;
        mainFore.pitch = -0.86F - cycle * 0.42F;
        mainFore.yaw = -side * 0.10F;
        mainHand.pitch = -0.16F + cycle * 0.22F;
        mainHand.roll = side * 0.20F;

        offUpper.pitch = -1.36F + cycle * 0.58F;
        offUpper.yaw = side * 0.18F;
        offFore.pitch = -0.72F - cycle * 0.30F;
        offHand.pitch = -0.10F;

        // Crouched weight transfer so it reads like breaking a defended bed in PvP.
        rt.pitch = 0.30F + (rightMain ? 0.10F : 0.0F);
        lt.pitch = 0.22F + (rightMain ? 0.0F : 0.10F);
        rs.pitch = 0.48F;
        ls.pitch = 0.42F;
        rig.roll = side * (cycle - 0.5F) * 0.045F;
    }
}
