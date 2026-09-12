package com.klearblrz.arrowrip.mixin;

import net.minecraft.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes the client-side bow look fully charged immediately. Server shot rules remain authoritative. */
@Mixin(BowItem.class)
public abstract class BowFastPullMixin {
    @Inject(method = "getPullProgress", at = @At("HEAD"), cancellable = true)
    private static void arrowrip$instantVisualPull(int useTicks, CallbackInfoReturnable<Float> cir) {
        if (useTicks > 0) cir.setReturnValue(1.0F);
    }
}
