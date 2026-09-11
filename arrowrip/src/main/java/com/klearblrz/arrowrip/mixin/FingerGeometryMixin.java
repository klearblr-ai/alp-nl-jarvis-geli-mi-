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
        float center = right ? (slim ? -0.5F : -1.0F) : (slim ? 0.5F : 1.0F);
        float spacing = slim ? 0.68F : 0.82F;
        String side = right ? "r" : "l";
        int u = right ? 40 : 32;
        int v = right ? 16 : 48;

        String[] names = {"pinky", "ring", "middle", "index"};
        for (int i = 0; i < 4; i++) {
            float x = center + (i - 1.5F) * spacing;
            arm.addChild(
                    "arrowrip_" + side + "_" + names[i],
                    ModelPartBuilder.create().uv(u, v)
                            .cuboid(-0.32F, 0.0F, -0.42F, 0.64F, 3.15F, 0.84F, dilation),
                    ModelTransform.origin(x, 9.15F, -0.35F));
        }

        float thumbX = center + (right ? 1.72F : -1.72F) * (slim ? 0.82F : 1.0F);
        arm.addChild(
                "arrowrip_" + side + "_thumb",
                ModelPartBuilder.create().uv(u, v)
                        .cuboid(-0.38F, 0.0F, -0.45F, 0.76F, 2.55F, 0.90F, dilation),
                ModelTransform.origin(thumbX, 8.05F, -0.15F));
    }
}
