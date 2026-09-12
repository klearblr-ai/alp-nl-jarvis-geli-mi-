package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.G17Client;
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
public abstract class G17PoseMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$g17Pose(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) return;
        if (!G17Client.isGunItem(state.getMainHandItemStack())) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        boolean rightMain = state.mainArm == Arm.RIGHT;
        float side = rightMain ? 1.0F : -1.0F;
        float recoil = MathHelper.clamp(G17Client.recoilTicks() / 7.0F, 0.0F, 1.0F);
        float snap = MathHelper.sin(recoil * (float)Math.PI);

        ModelPart root = model.getRootPart();
        if (root.hasChild("arrowrip_ai_rig")) {
            ModelPart rig = root.getChild("arrowrip_ai_rig");
            if (rig.visible) {
                ModelPart ru = rig.getChild("arrowrip_r_upper_arm");
                ModelPart lu = rig.getChild("arrowrip_l_upper_arm");
                ModelPart rf = ru.getChild("arrowrip_r_forearm");
                ModelPart lf = lu.getChild("arrowrip_l_forearm");
                ModelPart rh = rf.getChild("arrowrip_r_hand");
                ModelPart lh = lf.getChild("arrowrip_l_hand");

                ModelPart gunUpper = rightMain ? ru : lu;
                ModelPart gunFore = rightMain ? rf : lf;
                ModelPart gunHand = rightMain ? rh : lh;
                ModelPart supportUpper = rightMain ? lu : ru;
                ModelPart supportFore = rightMain ? lf : rf;
                ModelPart supportHand = rightMain ? lh : rh;

                gunUpper.pitch = -1.42F + snap * 0.18F;
                gunUpper.yaw = -side * 0.10F;
                gunUpper.roll = side * 0.04F;
                gunFore.pitch = -0.10F + snap * 0.20F;
                gunFore.yaw = -side * 0.03F;
                gunHand.pitch = -0.03F + snap * 0.18F;
                gunHand.roll = side * 0.02F;

                supportUpper.pitch = -1.26F + snap * 0.12F;
                supportUpper.yaw = side * 0.20F;
                supportUpper.roll = -side * 0.07F;
                supportFore.pitch = -0.36F + snap * 0.14F;
                supportFore.yaw = side * 0.12F;
                supportHand.pitch = -0.05F;
                supportHand.roll = -side * 0.08F;

                rig.pitch = 0.02F + snap * 0.035F;
                rig.yaw = -side * 0.03F;
                return;
            }
        }

        ModelPart gunArm = rightMain ? model.rightArm : model.leftArm;
        ModelPart supportArm = rightMain ? model.leftArm : model.rightArm;
        gunArm.pitch = -1.48F + snap * 0.20F;
        gunArm.yaw = -side * 0.10F;
        gunArm.roll = side * 0.04F;
        supportArm.pitch = -1.30F + snap * 0.12F;
        supportArm.yaw = side * 0.24F;
        supportArm.roll = -side * 0.08F;
        model.body.pitch = 0.03F + snap * 0.03F;
    }
}
