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
public abstract class FingerGeometryMixin {
    @Inject(method = "getTexturedModelData", at = @At("RETURN"))
    private static void arrowrip$addBlockyFingers(Dilation dilation, boolean slim, CallbackInfoReturnable<ModelData> cir) {
        ModelData data = cir.getReturnValue();
        ModelPartData root = data.getRoot();
        addHand(root.getChild("right_arm"), true, slim, dilation);
        addHand(root.getChild("left_arm"), false, slim, dilation);
    }

    private static void addHand(ModelPartData arm, boolean right, boolean slim, Dilation dilation) {
        float center = right ? (slim ? -0.45F : -0.82F) : (slim ? 0.45F : 0.82F);
        float spacing = slim ? 0.62F : 0.74F;
        String side = right ? "r" : "l";
        int u = right ? 40 : 32;
        int v = right ? 16 : 48;

        String[] names = {"pinky", "ring", "middle", "index"};
        for (int i = 0; i < 4; i++) {
            float x = center + (i - 1.5F) * spacing;
            float proximalLen = (i == 0 ? 1.45F : (i == 2 ? 1.72F : 1.62F));
            float tipLen = (i == 0 ? 1.20F : (i == 2 ? 1.38F : 1.30F));

            ModelPartData finger = arm.addChild(
                    "arrowrip_" + side + "_" + names[i],
                    ModelPartBuilder.create().uv(u, v)
                            .cuboid(-0.31F, 0.0F, -0.40F, 0.62F, proximalLen, 0.80F, dilation),
                    ModelTransform.origin(x, 9.05F, -0.42F));

            finger.addChild(
                    "arrowrip_" + side + "_" + names[i] + "_tip",
                    ModelPartBuilder.create().uv(u, v)
                            .cuboid(-0.29F, 0.0F, -0.38F, 0.58F, tipLen, 0.76F, dilation),
                    ModelTransform.origin(0.0F, proximalLen - 0.08F, 0.0F));
        }

        // 5th finger: thumb is deliberately offset outward so it is clearly visible in third person.
        float thumbX = center + (right ? 2.05F : -2.05F) * (slim ? 0.80F : 0.92F);
        ModelPartData thumb = arm.addChild(
                "arrowrip_" + side + "_thumb",
                ModelPartBuilder.create().uv(u, v)
                        .cuboid(-0.37F, 0.0F, -0.44F, 0.74F, 1.38F, 0.88F, dilation),
                ModelTransform.origin(thumbX, 8.05F, -0.05F));

        thumb.addChild(
                "arrowrip_" + side + "_thumb_tip",
                ModelPartBuilder.create().uv(u, v)
                        .cuboid(-0.34F, 0.0F, -0.41F, 0.68F, 1.18F, 0.82F, dilation),
                ModelTransform.origin(0.0F, 1.28F, 0.0F));
    }
}
