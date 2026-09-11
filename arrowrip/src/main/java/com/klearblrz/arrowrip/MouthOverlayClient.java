package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.consume.UseAction;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class MouthOverlayClient implements ClientModInitializer {
    private static final Identifier MOUTH = Identifier.of("arrowrip", "textures/mouth_grin.png");
    private static final Identifier BLOODY_MOUTH = Identifier.of("arrowrip", "textures/mouth_grin_bloody.png");
    private static KeyBinding mouthKey, upKey, downKey, leftKey, rightKey, biggerKey, smallerKey, resetKey;
    private static boolean enabled = true;
    private static int offsetX = 0;
    private static int offsetY = 0;
    private static float scale = 1.0f;
    private static int bloodyTicks = 0;

    @Override
    public void onInitializeClient() {
        mouthKey = key("key.arrowrip.mouth", GLFW.GLFW_KEY_M);
        upKey = key("key.arrowrip.mouth_up", GLFW.GLFW_KEY_UP);
        downKey = key("key.arrowrip.mouth_down", GLFW.GLFW_KEY_DOWN);
        leftKey = key("key.arrowrip.mouth_left", GLFW.GLFW_KEY_LEFT);
        rightKey = key("key.arrowrip.mouth_right", GLFW.GLFW_KEY_RIGHT);
        biggerKey = key("key.arrowrip.mouth_bigger", GLFW.GLFW_KEY_KP_ADD);
        smallerKey = key("key.arrowrip.mouth_smaller", GLFW.GLFW_KEY_KP_SUBTRACT);
        resetKey = key("key.arrowrip.mouth_reset", GLFW.GLFW_KEY_HOME);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                try {
                    if (client.player.isUsingItem()
                            && client.player.getActiveItem().getUseAction() == UseAction.EAT) {
                        bloodyTicks = 100;
                    } else if (bloodyTicks > 0) {
                        bloodyTicks--;
                    }
                } catch (Throwable ignored) {
                    if (bloodyTicks > 0) bloodyTicks--;
                }
            } else {
                bloodyTicks = 0;
            }

            while (mouthKey.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) client.player.sendMessage(Text.literal(enabled ? "Ağız: AÇIK" : "Ağız: KAPALI"), true);
            }
            if (!enabled) return;

            boolean changed = false;
            while (upKey.wasPressed()) { offsetY -= 2; changed = true; }
            while (downKey.wasPressed()) { offsetY += 2; changed = true; }
            while (leftKey.wasPressed()) { offsetX -= 2; changed = true; }
            while (rightKey.wasPressed()) { offsetX += 2; changed = true; }
            while (biggerKey.wasPressed()) { scale = Math.min(2.2f, scale + 0.05f); changed = true; }
            while (smallerKey.wasPressed()) { scale = Math.max(0.35f, scale - 0.05f); changed = true; }
            while (resetKey.wasPressed()) { offsetX = 0; offsetY = 0; scale = 1.0f; changed = true; }

            if (changed && client.player != null) {
                client.player.sendMessage(Text.literal("Ağız X " + offsetX + " Y " + offsetY + " Boyut " + String.format("%.2fx", scale)), true);
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> renderMouthHud(ctx));
    }

    private static void renderMouthHud(DrawContext ctx) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.options.getPerspective().isFirstPerson()) return;

        try {
            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();
            int w = Math.max(28, Math.round(86 * scale));
            int h = Math.max(12, Math.round(34 * scale));
            int x = sw / 2 - w / 2 + offsetX;
            int y = sh / 2 - 67 + offsetY;
            Identifier texture = bloodyTicks > 0 ? BLOODY_MOUTH : MOUTH;
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, w, h, 512, 256);
        } catch (Throwable ignored) {
            enabled = false;
        }
    }

    private static KeyBinding key(String name, int glfw) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, glfw, ArrowRipClient.CATEGORY));
    }
}
