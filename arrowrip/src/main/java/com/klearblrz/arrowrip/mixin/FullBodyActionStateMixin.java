package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.AutoPvPAnimationClient;
import com.klearblrz.arrowrip.ProceduralAIRigClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Strong action layer for obvious, readable full-body auto-play states. */
@Mixin(value = PlayerEntityModel.class, priority = 350)
public abstract class FullBodyActionStateMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$fullBodyActions(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!ProceduralAIRigClient.isEnabled() || mc.player == null || state.id != mc.player.getId()) return;
        if (mc.options.getPerspective().isFirstPerson() || state.spectator) return;
        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        ModelPart root = model.getRootPart();
        if (!root.hasChild("arrowrip_ai_rig")) return;
        ModelPart rig = root.getChild("arrowrip_ai_rig");
        if (!rig.visible) return;

        ModelPart ru=rig.getChild("arrowrip_r_upper_arm"), lu=rig.getChild("arrowrip_l_upper_arm");
        ModelPart rf=ru.getChild("arrowrip_r_forearm"), lf=lu.getChild("arrowrip_l_forearm");
        ModelPart rh=rf.getChild("arrowrip_r_hand"), lh=lf.getChild("arrowrip_l_hand");
        ModelPart rt=rig.getChild("arrowrip_r_thigh"), lt=rig.getChild("arrowrip_l_thigh");
        ModelPart rs=rt.getChild("arrowrip_r_shin"), ls=lt.getChild("arrowrip_l_shin");
        ModelPart rfoot=rs.getChild("arrowrip_r_foot"), lfoot=ls.getChild("arrowrip_l_foot");

        float age=state.age;
        float stride=MathHelper.clamp(state.limbSwingAmplitude*1.65F,0F,1F);
        float step=MathHelper.sin(state.limbSwingAnimationProgress*0.82F);
        boolean sprint=mc.player.isSprinting();
        boolean sword=state.getMainHandItemStack().isIn(ItemTags.SWORDS);
        boolean rightMain=state.mainArm==Arm.RIGHT;
        ModelPart wu=rightMain?ru:lu, wf=rightMain?rf:lf, wh=rightMain?rh:lh;
        ModelPart ou=rightMain?lu:ru, of=rightMain?lf:rf;
        float side=rightMain?1F:-1F;

        // WALK / RUN: exaggerated hips, knees, ankles, shoulders and torso counter-rotation.
        if (stride>0.035F && mc.player.isOnGround()) {
            float power=sprint?1.25F:0.78F;
            float rswing=MathHelper.cos(state.limbSwingAnimationProgress*0.82F)*power*stride;
            float lswing=-rswing;
            rt.pitch=rswing; lt.pitch=lswing;
            rs.pitch=0.12F+Math.max(0F,-rswing)*0.95F;
            ls.pitch=0.12F+Math.max(0F,-lswing)*0.95F;
            rfoot.pitch=-rs.pitch*0.46F+Math.max(0F,rswing)*0.18F;
            lfoot.pitch=-ls.pitch*0.46F+Math.max(0F,lswing)*0.18F;
            ru.pitch=lswing*0.86F; lu.pitch=rswing*0.86F;
            rf.pitch=-0.18F-Math.max(0F,-lswing)*0.48F;
            lf.pitch=-0.18F-Math.max(0F,-rswing)*0.48F;
            rig.yaw+=step*0.13F*stride;
            model.body.yaw-=step*0.16F*stride;
            model.body.roll+=step*0.045F*stride;
            model.body.pitch=sprint?0.34F:0.07F;
            model.head.pitch-=sprint?0.12F:0.02F;
        }

        // BREAK BLOCK: two-hand full-body mining swing.
        if (AutoPvPAnimationClient.getMineTicks()>0) {
            float h=(MathHelper.sin(age*1.05F)+1F)*0.5F;
            model.body.pitch=0.30F; model.body.yaw=side*(0.20F-h*0.38F);
            wu.pitch=-2.45F+h*1.55F; wu.yaw=-side*0.28F; wu.roll=side*0.16F;
            wf.pitch=-0.70F-h*0.40F; wh.pitch=-0.18F;
            ou.pitch=-1.70F+h*0.95F; ou.yaw=side*0.18F; of.pitch=-0.68F;
            rt.pitch=0.18F+h*0.10F; lt.pitch=-0.14F-h*0.06F;
            rs.pitch=0.24F; ls.pitch=0.18F;
        }

        // PLACE / BRIDGE: lean and reach toward edge, with crouched supporting legs.
        if (AutoPvPAnimationClient.getPlaceTicks()>0) {
            boolean bridge=mc.player.isSneaking() || mc.player.getPitch()>45F;
            float pulse=0.5F+0.5F*MathHelper.sin(age*0.95F);
            model.body.pitch=bridge?0.58F:0.32F;
            model.body.yaw=-side*(0.10F+0.08F*pulse);
            wu.pitch=bridge?-1.58F:-1.28F; wu.yaw=-side*0.24F; wf.pitch=-0.22F; wh.pitch=0.18F;
            ou.pitch=-0.38F; of.pitch=-0.66F;
            rt.pitch=bridge?0.42F:0.18F; lt.pitch=bridge?0.18F:-0.10F;
            rs.pitch=bridge?0.78F:0.22F; ls.pitch=bridge?0.50F:0.16F;
            rfoot.pitch=-0.20F; lfoot.pitch=-0.16F;
        }

        // SWORD / MELEE: readable weight transfer on every hit, not just arm swing.
        if (state.handSwingProgress>0.001F) {
            float p=state.handSwingProgress;
            float hit=MathHelper.sin(p*(float)Math.PI);
            model.body.yaw+=side*hit*0.62F;
            model.body.roll-=side*hit*0.10F;
            model.body.pitch+=hit*0.12F;
            if (sword) {
                wu.pitch=-0.82F-hit*1.05F; wu.yaw=-side*(0.30F+hit*0.72F); wu.roll=side*(0.12F+hit*0.28F);
                wf.pitch=-0.55F-hit*0.64F; wf.roll=side*hit*0.34F; wh.roll=side*hit*0.42F;
                ou.pitch=-0.34F+hit*0.18F; of.pitch=-0.64F;
            } else {
                wu.pitch=-0.70F-hit*0.90F; wf.pitch=-0.90F+hit*0.82F;
                ou.pitch=-0.56F; of.pitch=-0.82F;
            }
            rt.pitch+=rightMain?hit*0.28F:-hit*0.08F;
            lt.pitch+=rightMain?-hit*0.08F:hit*0.28F;
            rs.pitch+=hit*0.18F; ls.pitch+=hit*0.10F;
        }

        // JUMP / FALL / LAND.
        if (!mc.player.isOnGround()) {
            float vy=MathHelper.clamp((float)mc.player.getVelocity().y*3F,-1F,1F);
            if (vy>0F) {
                model.body.pitch=-0.08F; rt.pitch=0.42F; lt.pitch=-0.18F; rs.pitch=0.74F; ls.pitch=0.34F;
                ru.pitch=-0.42F; lu.pitch=-0.28F; rf.pitch=-0.42F; lf.pitch=-0.34F;
            } else {
                model.body.pitch=0.16F; rt.pitch=0.20F; lt.pitch=0.12F; rs.pitch=0.92F; ls.pitch=0.78F;
                ru.roll=0.24F; lu.roll=-0.24F; rf.pitch=-0.30F; lf.pitch=-0.30F;
            }
        }
        if (AutoPvPAnimationClient.getLandingTicks()>0) {
            float q=AutoPvPAnimationClient.getLandingTicks()/10F;
            model.body.pitch=0.38F*q; rt.pitch=0.56F*q; lt.pitch=0.56F*q;
            rs.pitch=1.05F*q; ls.pitch=1.05F*q; rfoot.pitch=-0.42F*q; lfoot.pitch=-0.42F*q;
            ru.pitch=-0.34F*q; lu.pitch=-0.34F*q;
        }

        // Hurt recoil is whole-body.
        if (AutoPvPAnimationClient.getHurtTicks()>0) {
            float q=AutoPvPAnimationClient.getHurtTicks()/11F;
            model.body.pitch=-0.24F*q; model.body.yaw+=0.34F*q; rig.roll-=0.16F*q;
            ru.pitch+=0.42F*q; lu.pitch+=0.26F*q; rt.pitch-=0.18F*q; lt.pitch+=0.14F*q;
        }
    }
}
