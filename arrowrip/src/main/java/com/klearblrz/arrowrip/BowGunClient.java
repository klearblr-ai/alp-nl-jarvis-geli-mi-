package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side bow-as-gun controller.
 * The server still receives normal vanilla bow use/release packets and remains authoritative.
 */
public final class BowGunClient implements ClientModInitializer {
    private static KeyBinding inspectKey;
    private static KeyBinding reloadKey;
    private static KeyBinding fireModeKey;

    private static final int MAG_SIZE = 19;
    private static final int FIRE_TICKS = 3;
    private static final int RELOAD_TICKS_MAX = 20;
    private static final int INSPECT_TICKS_MAX = 52;

    private static int magazine = MAG_SIZE;
    private static int reloadTicks;
    private static int inspectTicks;
    private static int recoilTicks;
    private static int restartDelay;
    private static boolean releasedThisUse;
    private static boolean reloadStopSent;
    private static boolean autoMode = true;
    private static boolean semiLatched;

    @Override
    public void onInitializeClient() {
        inspectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.bowgun_inspect",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                ArrowRipClient.CATEGORY));
        reloadKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.bowgun_reload",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                ArrowRipClient.CATEGORY));
        fireModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.bowgun_firemode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(BowGunClient::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> renderHud(ctx));
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            resetAll();
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
                client.player.sendMessage(Text.literal("SİLAH INSPECT"), true);
            }
        }

        while (reloadKey.wasPressed()) {
            if (reloadTicks <= 0 && magazine < MAG_SIZE) startReload(client);
        }

        while (fireModeKey.wasPressed()) {
            autoMode = !autoMode;
            semiLatched = false;
            client.player.sendMessage(Text.literal(autoMode ? "ATEŞ MODU: TARAMALI" : "ATEŞ MODU: TEK TEK"), true);
        }

        if (inspectTicks > 0) inspectTicks--;
        if (recoilTicks > 0) recoilTicks--;
        if (restartDelay > 0) restartDelay--;

        boolean usePressed = client.options.useKey.isPressed();
        if (!usePressed) semiLatched = false;

        if (reloadTicks > 0) {
            if (!reloadStopSent && client.player.isUsingItem() && client.interactionManager != null) {
                client.interactionManager.stopUsingItem(client.player);
                reloadStopSent = true;
            }
            reloadTicks--;
            if (reloadTicks == 0) {
                magazine = MAG_SIZE;
                releasedThisUse = false;
                reloadStopSent = false;
                restartDelay = 2;
                client.player.sendMessage(Text.literal("SANAL ŞARJÖR 19/19"), true);
            }
            return;
        }

        if (client.player.isUsingItem() && client.player.getActiveItem().isOf(Items.BOW)) {
            int used = client.player.getActiveItem().getMaxUseTime(client.player) - client.player.getItemUseTimeLeft();
            boolean canFire = autoMode || !semiLatched;
            if (used >= FIRE_TICKS && !releasedThisUse && canFire && client.interactionManager != null) {
                client.interactionManager.stopUsingItem(client.player);
                releasedThisUse = true;
                semiLatched = true;
                recoilTicks = 6;
                magazine = Math.max(0, magazine - 1);
                restartDelay = 2;
                if (magazine <= 0) startReload(client);
            }
        } else {
            releasedThisUse = false;
        }

        // AUTO: right click can stay held and the client repeatedly starts/release-uses the real bow.
        // SEMI: the player must release right click before the next shot.
        if (client.currentScreen == null
                && usePressed
                && restartDelay <= 0
                && reloadTicks <= 0
                && magazine > 0
                && (autoMode || !semiLatched)
                && !client.player.isUsingItem()
                && client.interactionManager != null) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        }
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.getMainHandStack().isOf(Items.BOW)) return;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        // Compact gun crosshair. It shrinks while aiming/using the bow.
        int gap = mc.player.isUsingItem() ? 2 : 4;
        int arm = 4;
        int cross = 0xE8FFFFFF;
        ctx.fill(cx - gap - arm, cy, cx - gap, cy + 1, cross);
        ctx.fill(cx + gap + 1, cy, cx + gap + arm + 1, cy + 1, cross);
        ctx.fill(cx, cy - gap - arm, cx + 1, cy - gap, cross);
        ctx.fill(cx, cy + gap + 1, cx + 1, cy + gap + arm + 1, cross);

        String mode = autoMode ? "AUTO" : "SEMI";
        String ammo = mode + "  " + magazine + "/" + MAG_SIZE;
        int tx = w - mc.textRenderer.getWidth(ammo) - 12;
        int ty = h - 39;
        ctx.fill(tx - 5, ty - 4, w - 7, ty + 11, 0x88000000);
        ctx.drawTextWithShadow(mc.textRenderer, ammo, tx, ty, 0xFFFFFFFF);

        if (reloadTicks > 0) {
            int barW = 58;
            int bx = w - barW - 12;
            int by = h - 22;
            float p = reloadProgress(0.0F);
            ctx.fill(bx, by, bx + barW, by + 3, 0x66000000);
            ctx.fill(bx, by, bx + Math.max(1, (int)(barW * p)), by + 3, 0xFFFFFFFF);
            ctx.drawTextWithShadow(mc.textRenderer, "RELOAD", bx, by - 10, 0xFFFFFFFF);
        }
    }

    private static void startReload(MinecraftClient client) {
        reloadTicks = RELOAD_TICKS_MAX;
        inspectTicks = 0;
        recoilTicks = 0;
        releasedThisUse = true;
        reloadStopSent = false;
        semiLatched = true;
        if (client.player != null) client.player.sendMessage(Text.literal("ŞARJÖR DEĞİŞTİRİLİYOR…"), true);
    }

    private static void resetTransient() {
        reloadTicks = 0;
        inspectTicks = 0;
        recoilTicks = 0;
        restartDelay = 0;
        releasedThisUse = false;
        reloadStopSent = false;
        semiLatched = false;
    }

    private static void resetAll() {
        magazine = MAG_SIZE;
        resetTransient();
    }

    public static float recoilProgress(float tickDelta) {
        if (recoilTicks <= 0) return 0.0F;
        float p = (recoilTicks - tickDelta) / 6.0F;
        return Math.max(0.0F, Math.min(1.0F, p));
    }

    public static float reloadProgress(float tickDelta) {
        if (reloadTicks <= 0) return 0.0F;
        return 1.0F - Math.max(0.0F, Math.min(1.0F, (reloadTicks - tickDelta) / (float) RELOAD_TICKS_MAX));
    }

    public static float inspectProgress(float tickDelta) {
        if (inspectTicks <= 0) return 0.0F;
        return 1.0F - Math.max(0.0F, Math.min(1.0F, (inspectTicks - tickDelta) / (float) INSPECT_TICKS_MAX));
    }

    public static boolean isReloading() { return reloadTicks > 0; }
    public static boolean isInspecting() { return inspectTicks > 0; }
    public static boolean isAutoMode() { return autoMode; }
    public static int magazine() { return magazine; }
    public static int magazineSize() { return MAG_SIZE; }
}
