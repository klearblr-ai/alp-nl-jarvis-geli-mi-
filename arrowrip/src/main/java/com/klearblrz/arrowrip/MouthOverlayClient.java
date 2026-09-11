package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class MouthOverlayClient implements ClientModInitializer {
    private static KeyBinding mouthKey, upKey, downKey, leftKey, rightKey, forwardKey, backKey, biggerKey, smallerKey, resetKey;
    private static boolean enabled = false;
    private static double offsetX, offsetY, offsetZ;
    private static float scale = 1.0f;

    @Override
    public void onInitializeClient() {
        mouthKey = key("key.arrowrip.mouth", GLFW.GLFW_KEY_M);
        upKey = key("key.arrowrip.mouth_up", GLFW.GLFW_KEY_UP);
        downKey = key("key.arrowrip.mouth_down", GLFW.GLFW_KEY_DOWN);
        leftKey = key("key.arrowrip.mouth_left", GLFW.GLFW_KEY_LEFT);
        rightKey = key("key.arrowrip.mouth_right", GLFW.GLFW_KEY_RIGHT);
        forwardKey = key("key.arrowrip.mouth_forward", GLFW.GLFW_KEY_PAGE_UP);
        backKey = key("key.arrowrip.mouth_back", GLFW.GLFW_KEY_PAGE_DOWN);
        biggerKey = key("key.arrowrip.mouth_bigger", GLFW.GLFW_KEY_KP_ADD);
        smallerKey = key("key.arrowrip.mouth_smaller", GLFW.GLFW_KEY_KP_SUBTRACT);
        resetKey = key("key.arrowrip.mouth_reset", GLFW.GLFW_KEY_HOME);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (mouthKey.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) client.player.sendMessage(Text.literal(enabled ? "Ağız: AÇIK" : "Ağız: KAPALI"), true);
            }
            if (!enabled) return;
            boolean changed = false;
            while (upKey.wasPressed()) { offsetY += 0.015; changed = true; }
            while (downKey.wasPressed()) { offsetY -= 0.015; changed = true; }
            while (leftKey.wasPressed()) { offsetX -= 0.015; changed = true; }
            while (rightKey.wasPressed()) { offsetX += 0.015; changed = true; }
            while (forwardKey.wasPressed()) { offsetZ -= 0.008; changed = true; }
            while (backKey.wasPressed()) { offsetZ += 0.008; changed = true; }
            while (biggerKey.wasPressed()) { scale = Math.min(2.0f, scale + 0.05f); changed = true; }
            while (smallerKey.wasPressed()) { scale = Math.max(0.35f, scale - 0.05f); changed = true; }
            while (resetKey.wasPressed()) { offsetX = offsetY = offsetZ = 0; scale = 1.0f; changed = true; }
            if (changed && client.player != null) client.player.sendMessage(Text.literal(String.format("Ağız ayarı X %.2f Y %.2f Z %.2f %.2fx", offsetX, offsetY, offsetZ, scale)), true);
        });
    }

    private static KeyBinding key(String name, int glfw) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, glfw, ArrowRipClient.CATEGORY));
    }
}
