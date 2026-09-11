package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class AnimeAnimationClient implements ClientModInitializer {
    private static KeyBinding emoteKey;
    private static KeyBinding sitKey;
    private static int emoteType = 0;
    private static int emoteTicks = 0;
    private static boolean sitting = false;

    @Override
    public void onInitializeClient() {
        emoteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.anime_emote", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, ArrowRipClient.CATEGORY));
        sitKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.anime_sit", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (emoteKey.wasPressed()) {
                emoteType++;
                if (emoteType > 5) emoteType = 1;
                emoteTicks = 120;
                sitting = false;
                if (client.player != null) {
                    String name = switch (emoteType) {
                        case 1 -> "POWER UP";
                        case 2 -> "DÜŞÜNME";
                        case 3 -> "VICTORY";
                        case 4 -> "KILIÇ DURUŞU";
                        default -> "ANIME BOW";
                    };
                    client.player.sendMessage(Text.literal("Anime emote: " + name), true);
                }
            }

            while (sitKey.wasPressed()) {
                sitting = !sitting;
                emoteTicks = 0;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(sitting ? "Anime oturuş: AÇIK" : "Anime oturuş: KAPALI"), true);
                }
            }

            if (emoteTicks > 0) emoteTicks--;
            if (emoteTicks == 0) emoteType = 0;
        });
    }

    public static int getEmoteType() {
        return emoteType;
    }

    public static int getEmoteTicks() {
        return emoteTicks;
    }

    public static boolean isSitting() {
        return sitting;
    }
}
