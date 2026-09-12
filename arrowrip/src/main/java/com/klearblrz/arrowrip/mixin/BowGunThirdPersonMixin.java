package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.BowGunClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third-person pistol-like bow hold plus a visible virtual magazine change. */
@Mixin(PlayerEntityModel.class)
public abstract class BowGunThirdPersonMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$bowGunPose(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) return;
        if (!state.getMainHandItemStack().isOf(Items.BOW)) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        boolean right = state.mainArm == Arm.RIGHT;
        float side = right ? 1.0F : -1.0F;
        float reload = BowGunClient.reloadProgress(0.0F);
        float recoil = BowGunClient.recoilProgress(0.0F);
        float kick = recoil > 0.0F ? MathHelper.sin(recoil * (float)Math.PI) : 0.0F;

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
                ModelPart gunU = right ? ru : lu;
                ModelPart gunF = right ? rf : lf;
                ModelPart gunH = right ? rh : lh;
                ModelPart supU = right ? lu : ru;
                ModelPart supF = right ? lf : rf;
                ModelPart supH = right ? lh : rh;

                if (reload > 0.0F) {
                    float drop = MathHelper.sin(MathHelper.clamp(reload / 0.62F, 0.0F, 1.0F) * (float)Math.PI);
                    float seat = MathHelper.clamp((reload - 0.58F) / 0.42F, 0.0F, 1.0F);
                    gunU.pitch = -0.72F + 0.34F * drop;
                    gunU.yaw = -side * (0.20F + 0.24F * drop);
                    gunF.pitch = -0.58F + 0.82F * drop - 0.22F * seat;
                    gunH.roll = side * (0.18F + 0.42F * drop);
                    supU.pitch = -1.15F + 0.48F * drop;
                    supU.yaw = side * (0.34F + 0.26F * drop);
                    supF.pitch = -0.82F - 0.34F * drop + 0.26F * seat;
                    supH.pitch = -0.16F + 0.28F * drop;
                    rig.yaw = -side * 0.08F * drop;
                    model.body.pitch = 0.08F + 0.08F * drop;
                } else {
                    gunU.pitch = -1.42F + kick * 0.18F;
                    gunU.yaw = -side * 0.10F;
                    gunU.roll = side * 0.04F;
                    gunF.pitch = -0.10F + kick * 0.20F;
                    gunH.pitch = -0.03F + kick * 0.18F;
                    supU.pitch = -1.26F + kick * 0.12F;
                    supU.yaw = side * 0.20F;
                    supF.pitch = -0.36F + kick * 0.14F;
                    supH.roll = -side * 0.08F;
                }
                return;
            }
        }

        ModelPart gun = right ? model.rightArm : model.leftArm;
        ModelPart support = right ? model.leftArm : model.rightArm;
        if (reload > 0.0F) {
            float drop = MathHelper.sin(MathHelper.clamp(reload / 0.62F, 0.0F, 1.0F) * (float)Math.PI);
            gun.pitch = -0.74F + 0.42F * drop;
            gun.yaw = -side * (0.18F + 0.22F * drop);
            gun.roll = side * 0.22F * drop;
            support.pitch = -1.12F + 0.56F * drop;
            support.yaw = side * (0.28F + 0.28F * drop);
            support.roll = -side * 0.20F * drop;
        } else {
            gun.pitch = -1.46F + kick * 0.20F;
            gun.yaw = -side * 0.10F;
            gun.roll = side * 0.04F;
            support.pitch = -1.28F + kick * 0.12F;
            support.yaw = side * 0.22F;
            support.roll = -side * 0.08F;
        }
    }
}
