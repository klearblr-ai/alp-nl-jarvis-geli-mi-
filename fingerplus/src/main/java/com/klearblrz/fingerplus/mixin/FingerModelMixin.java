package com.klearblrz.fingerplus.mixin;

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
public abstract class FingerModelMixin {
    @Inject(method = "getTexturedModelData", at = @At("RETURN"))
    private static void fingerplus$addFingers(Dilation dilation, boolean slim, CallbackInfoReturnable<ModelData> cir) {
        ModelPartData root = cir.getReturnValue().getRoot();
        addHand(root.getChild("right_arm"), true, slim, dilation);
        addHand(root.getChild("left_arm"), false, slim, dilation);
    }

    private static void addHand(ModelPartData arm, boolean right, boolean slim, Dilation dilation) {
        String side = right ? "r" : "l";
        int u = right ? 40 : 32;
        int v = right ? 16 : 48;
        float handCenter = right ? (slim ? -0.45F : -0.70F) : (slim ? 0.45F : 0.70F);
        float step = slim ? 0.62F : 0.76F;
        String[] digits = {"pinky", "ring", "middle", "index"};

        for (int i = 0; i < digits.length; i++) {
            float x = handCenter + (i - 1.5F) * step;
            arm.addChild(
                    "fingerplus_" + side + "_" + digits[i],
                    ModelPartBuilder.create().uv(u, v + 4)
                            .cuboid(-0.31F, 0.0F, -0.43F, 0.62F, 3.05F, 0.86F, dilation),
                    ModelTransform.origin(x, 9.30F, -0.22F));
        }

        float thumbX = handCenter + (right ? 1.68F : -1.68F) * (slim ? 0.82F : 1.0F);
        arm.addChild(
                "fingerplus_" + side + "_thumb",
                ModelPartBuilder.create().uv(u, v + 3)
                        .cuboid(-0.37F, 0.0F, -0.45F, 0.74F, 2.42F, 0.90F, dilation),
                ModelTransform.origin(thumbX, 8.08F, -0.05F));
    }
}
