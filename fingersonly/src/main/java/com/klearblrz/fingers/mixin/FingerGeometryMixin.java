package com.klearblrz.fingers.mixin;

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
    private static void fingersonly$addFingers(Dilation dilation, boolean slim, CallbackInfoReturnable<ModelData> cir) {
        ModelPartData root = cir.getReturnValue().getRoot();
        addHand(root.getChild("right_arm"), true, slim, dilation);
        addHand(root.getChild("left_arm"), false, slim, dilation);
    }

    private static void addHand(ModelPartData arm, boolean right, boolean slim, Dilation dilation) {
        String side = right ? "r" : "l";
        float center = right ? (slim ? -0.45F : -0.92F) : (slim ? 0.45F : 0.92F);
        float spacing = slim ? 0.63F : 0.76F;
        int u = right ? 40 : 32;
        int v = right ? 16 : 48;

        String[] digits = {"pinky", "ring", "middle", "index"};
        for (int i = 0; i < digits.length; i++) {
            float x = center + (i - 1.5F) * spacing;
            arm.addChild(
                    "fingersonly_" + side + "_" + digits[i],
                    ModelPartBuilder.create().uv(u, v)
                            .cuboid(-0.30F, 0.0F, -0.42F, 0.60F, 3.05F, 0.84F, dilation),
                    ModelTransform.origin(x, 9.10F, -0.38F));
        }

        float thumbX = center + (right ? 1.58F : -1.58F) * (slim ? 0.86F : 1.0F);
        arm.addChild(
                "fingersonly_" + side + "_thumb",
                ModelPartBuilder.create().uv(u, v)
                        .cuboid(-0.36F, 0.0F, -0.45F, 0.72F, 2.55F, 0.90F, dilation),
                ModelTransform.origin(thumbX, 8.12F, -0.10F));
    }
}
