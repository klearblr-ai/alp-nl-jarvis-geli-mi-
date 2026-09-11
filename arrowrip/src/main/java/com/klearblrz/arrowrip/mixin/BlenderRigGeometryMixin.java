package com.klearblrz.arrowrip.mixin;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityModel.class)
public abstract class BlenderRigGeometryMixin {
    @Inject(method = "getTexturedModelData", at = @At("RETURN"))
    private static void arrowrip$addArticulatedRig(Dilation dilation, boolean slim, CallbackInfoReturnable<ModelData> cir) {
        ModelPartData root = cir.getReturnValue().getRoot();
        ModelPartData rig = root.addChild("arrowrip_ai_rig", ModelPartBuilder.create(), ModelTransform.NONE);

        addArm(rig, true, slim, dilation);
        addArm(rig, false, slim, dilation);
        addLeg(rig, true, dilation);
        addLeg(rig, false, dilation);
    }

    private static void addArm(ModelPartData rig, boolean right, boolean slim, Dilation dilation) {
        String s = right ? "r" : "l";
        float shoulderX = right ? -5.0F : 5.0F;
        float width = slim ? 3.0F : 4.0F;
        float x0 = slim ? -1.5F : -2.0F;
        int u = right ? 40 : 32;
        int v = right ? 16 : 48;

        ModelPartData upper = rig.addChild(
                "arrowrip_" + s + "_upper_arm",
                ModelPartBuilder.create().uv(u, v).cuboid(x0, -2.0F, -2.0F, width, 6.0F, 4.0F, dilation),
                ModelTransform.origin(shoulderX, 2.0F, 0.0F));

        ModelPartData forearm = upper.addChild(
                "arrowrip_" + s + "_forearm",
                ModelPartBuilder.create().uv(u, v + 5).cuboid(x0, 0.0F, -2.0F, width, 6.0F, 4.0F, dilation),
                ModelTransform.origin(0.0F, 4.0F, 0.0F));

        ModelPartData hand = forearm.addChild(
                "arrowrip_" + s + "_hand",
                ModelPartBuilder.create().uv(u, v).cuboid(x0, 0.0F, -2.0F, width, 1.65F, 4.0F, dilation),
                ModelTransform.origin(0.0F, 5.25F, 0.0F));

        float spacing = slim ? 0.62F : 0.78F;
        float center = 0.0F;
        String[] digits = {"pinky", "ring", "middle", "index"};
        for (int i = 0; i < 4; i++) {
            float x = center + (i - 1.5F) * spacing;
            hand.addChild(
                    "arrowrip_" + s + "_ai_" + digits[i],
                    ModelPartBuilder.create().uv(u, v)
                            .cuboid(-0.29F, 0.0F, -0.40F, 0.58F, 2.25F, 0.80F, dilation),
                    ModelTransform.origin(x, 1.15F, -0.45F));
        }

        float thumbX = right ? 1.45F : -1.45F;
        hand.addChild(
                "arrowrip_" + s + "_ai_thumb",
                ModelPartBuilder.create().uv(u, v)
                        .cuboid(-0.34F, 0.0F, -0.42F, 0.68F, 1.95F, 0.84F, dilation),
                ModelTransform.origin(thumbX, 0.72F, -0.10F));
    }

    private static void addLeg(ModelPartData rig, boolean right, Dilation dilation) {
        String s = right ? "r" : "l";
        float hipX = right ? -1.9F : 1.9F;
        int u = right ? 0 : 16;
        int v = right ? 16 : 48;

        ModelPartData thigh = rig.addChild(
                "arrowrip_" + s + "_thigh",
                ModelPartBuilder.create().uv(u, v).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, dilation),
                ModelTransform.origin(hipX, 12.0F, 0.0F));

        ModelPartData shin = thigh.addChild(
                "arrowrip_" + s + "_shin",
                ModelPartBuilder.create().uv(u, v + 6).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, dilation),
                ModelTransform.origin(0.0F, 6.0F, 0.0F));

        shin.addChild(
                "arrowrip_" + s + "_foot",
                ModelPartBuilder.create().uv(u, v + 10).cuboid(-2.0F, 0.0F, -3.2F, 4.0F, 2.0F, 5.2F, dilation),
                ModelTransform.origin(0.0F, 5.5F, 0.0F));
    }
}
