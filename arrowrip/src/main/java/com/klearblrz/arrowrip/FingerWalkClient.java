package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Lightweight controls for the skin-safe finger + locomotion build. */
public final class FingerWalkClient implements ClientModInitializer {
    private static KeyBinding fingersKey;
    private static boolean fingersOpen = true;

    @Override
    public void onInitializeClient() {
        fingersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.fingers_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (fingersKey.wasPressed()) {
                fingersOpen = !fingersOpen;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(
                            fingersOpen ? "PARMAKLAR: AÇIK" : "PARMAKLAR: YUMRUK"), true);
                }
            }
        });
    }

    public static boolean fingersOpen() {
        return fingersOpen;
    }
}
