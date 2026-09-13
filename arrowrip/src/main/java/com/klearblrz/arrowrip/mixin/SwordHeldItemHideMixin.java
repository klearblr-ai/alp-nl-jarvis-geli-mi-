package com.klearblrz.arrowrip.mixin;

import com.klearblrz.arrowrip.SwordDisarmClient;
import com.klearblrz.arrowrip.SwordFinisherClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerHeldItemFeatureRenderer.class)
public abstract class SwordHeldItemHideMixin {
    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void arrowrip$hideSwordWhenNeeded(
            PlayerEntityRenderState state,
            ItemRenderState itemRenderState,
            ItemStack stack,
            Arm arm,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Local draw/sheath illusion.
        if (state.id == client.player.getId()
                && !SwordFinisherClient.isSwordDrawn()
                && stack.isIn(ItemTags.SWORDS)) {
            ci.cancel();
            return;
        }

        // Remote visual disarm. Server inventory is untouched; only rendering is suppressed.
        if (SwordDisarmClient.shouldHideSword(state.id, stack)) ci.cancel();
    }
}
