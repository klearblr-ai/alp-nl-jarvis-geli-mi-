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
public abstract class SwordBackGeometryMixin {
    @Inject(method = "getTexturedModelData", at = @At("RETURN"))
    private static void arrowrip$addBackSword(Dilation dilation, boolean slim, CallbackInfoReturnable<ModelData> cir) {
        ModelPartData body = cir.getReturnValue().getRoot().getChild("body");
        body.addChild(
                "arrowrip_back_sword",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-0.55F, -11.5F, -0.45F, 1.10F, 15.0F, 0.90F, dilation)
                        .uv(0, 0).cuboid(-2.30F, 2.4F, -0.55F, 4.60F, 0.80F, 1.10F, dilation)
                        .uv(0, 0).cuboid(-0.45F, 3.0F, -0.45F, 0.90F, 4.2F, 0.90F, dilation),
                ModelTransform.of(3.15F, 6.0F, 2.65F, 0.14F, 0.0F, -0.78F));
    }
}
