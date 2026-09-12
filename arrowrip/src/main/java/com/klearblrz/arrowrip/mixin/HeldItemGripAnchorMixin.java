package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.ProceduralAIRigClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps held items glued to the visible hand. Minecraft's held-item feature is anchored to
 * the vanilla arm, while ArrowRip can replace that arm with an articulated AI rig. When an
 * item is present we temporarily render that side with the vanilla anchor (including the
 * custom fingers added to it), so the item and hand can never drift apart.
 */
@Mixin(PlayerEntityModel.class)
public abstract class HeldItemGripAnchorMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void arrowrip$anchorHeldItems(PlayerEntityRenderState state, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId() || !ProceduralAIRigClient.isEnabled()) return;

        PlayerEntityModel model = (PlayerEntityModel)(Object)this;
        ModelPart root = model.getRootPart();
        if (!root.hasChild("arrowrip_ai_rig")) return;

        ModelPart rig = root.getChild("arrowrip_ai_rig");
        if (!rig.visible) return;

        ModelPart rightRigArm = rig.getChild("arrowrip_r_upper_arm");
        ModelPart leftRigArm = rig.getChild("arrowrip_l_upper_arm");

        // Always restore first; otherwise a hidden rig arm could stay hidden after switching items.
        rightRigArm.visible = true;
        leftRigArm.visible = true;

        boolean rightItem = !state.getItemStackForArm(Arm.RIGHT).isEmpty();
        boolean leftItem = !state.getItemStackForArm(Arm.LEFT).isEmpty();
        boolean twoHanded = state.getMainHandItemStack().isOf(Items.BOW)
                || state.getMainHandItemStack().isOf(Items.CROSSBOW);

        if (twoHanded) {
            rightItem = true;
            leftItem = true;
        }

        if (rightItem) {
            model.rightArm.visible = true;
            rightRigArm.visible = false;
        }
        if (leftItem) {
            model.leftArm.visible = true;
            leftRigArm.visible = false;
        }
    }
}
