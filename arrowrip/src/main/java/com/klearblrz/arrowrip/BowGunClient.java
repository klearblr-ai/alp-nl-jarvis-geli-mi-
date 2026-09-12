package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/** Client-only bow-as-gun controller: fast release, virtual 19-round magazine, reload and inspect timelines. */
public final class BowGunClient implements ClientModInitializer {
    private static KeyBinding inspectKey;
    private static final int MAG_SIZE = 19;
    private static final int FIRE_TICKS = 4;
    private static final int RELOAD_TICKS_MAX = 16;
    private static final int INSPECT_TICKS_MAX = 52;

    private static int magazine = MAG_SIZE;
    private static int reloadTicks;
    private static int inspectTicks;
    private static int recoilTicks;
    private static int restartDelay;
    private static boolean releasedThisUse;

    @Override
    public void onInitializeClient() {
        inspectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.bowgun_inspect",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                ArrowRipClient.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(BowGunClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            resetTransient();
            return;
        }

        boolean bow = client.player.getMainHandStack().isOf(Items.BOW);
        if (!bow) {
            resetTransient();
            return;
        }

        while (inspectKey.wasPressed()) {
            if (reloadTicks <= 0) {
                inspectTicks = INSPECT_TICKS_MAX;
                client.player.sendMessage(Text.literal("Glock Inspect"), true);
            }
        }

        if (inspectTicks > 0) inspectTicks--;
        if (recoilTicks > 0) recoilTicks--;
        if (restartDelay > 0) restartDelay--;

        if (reloadTicks > 0) {
            reloadTicks--;
            if (client.player.isUsingItem() && client.interactionManager != null) {
                client.interactionManager.stopUsingItem(client.player);
            }
            if (reloadTicks == 0) {
                magazine = MAG_SIZE;
                releasedThisUse = false;
            }
            return;
        }

        // Release a vanilla bow after a very short charge. Public servers still decide whether they accept the shot.
        if (client.player.isUsingItem() && client.player.getActiveItem().isOf(Items.BOW)) {
            int used = client.player.getActiveItem().getMaxUseTime(client.player) - client.player.getItemUseTimeLeft();
            if (used >= FIRE_TICKS && !releasedThisUse && client.interactionManager != null) {
                client.interactionManager.stopUsingItem(client.player);
                releasedThisUse = true;
                recoilTicks = 5;
                magazine--;
                restartDelay = 1;
                if (magazine <= 0) {
                    magazine = 0;
                    reloadTicks = RELOAD_TICKS_MAX;
                    inspectTicks = 0;
                }
            }
        } else {
            releasedThisUse = false;
        }

        // Holding use restarts the bow after each fast release, giving a semi/rapid gun feel.
        if (client.options.useKey.isPressed() && restartDelay <= 0 && reloadTicks <= 0
                && !client.player.isUsingItem() && client.interactionManager != null) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        }
    }

    private static void resetTransient() {
        reloadTicks = 0;
        inspectTicks = 0;
        recoilTicks = 0;
        restartDelay = 0;
        releasedThisUse = false;
    }

    public static float recoilProgress(float tickDelta) {
        if (recoilTicks <= 0) return 0.0F;
        float p = (recoilTicks - tickDelta) / 5.0F;
        return Math.max(0.0F, Math.min(1.0F, p));
    }

    public static float reloadProgress(float tickDelta) {
        if (reloadTicks <= 0) return 0.0F;
        return 1.0F - Math.max(0.0F, Math.min(1.0F, (reloadTicks - tickDelta) / (float)RELOAD_TICKS_MAX));
    }

    public static float inspectProgress(float tickDelta) {
        if (inspectTicks <= 0) return 0.0F;
        return 1.0F - Math.max(0.0F, Math.min(1.0F, (inspectTicks - tickDelta) / (float)INSPECT_TICKS_MAX));
    }

    public static boolean isReloading() { return reloadTicks > 0; }
    public static boolean isInspecting() { return inspectTicks > 0; }
    public static int magazine() { return magazine; }
    public static int magazineSize() { return MAG_SIZE; }
}
