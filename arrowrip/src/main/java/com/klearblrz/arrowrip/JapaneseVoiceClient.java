package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public final class JapaneseVoiceClient implements ClientModInitializer {
    private static final String[] NAMES = {
            "Nani?!", "Yamero!", "Ikuzo!", "Yare yare...", "Mada mada!", "Sugoi!", "Nanda?!", "Kamuda!"
    };
    private static final String[] SOUNDS = {
            "voice_nani", "voice_yamero", "voice_ikuzo", "voice_yareyare",
            "voice_madamada", "voice_sugoi", "voice_nanda", "voice_kamuda"
    };

    private static KeyBinding voiceKey;
    private static int lastIndex = -1;

    @Override
    public void onInitializeClient() {
        voiceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.random_japanese_voice", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (voiceKey.wasPressed()) playRandomVoice(client);
        });
    }

    public static void playRandomVoice(MinecraftClient client) {
        if (client == null) return;
        int idx;
        if (SOUNDS.length <= 1) idx = 0;
        else {
            do idx = ThreadLocalRandom.current().nextInt(SOUNDS.length);
            while (idx == lastIndex);
        }
        lastIndex = idx;
        if (client.player != null) client.player.sendMessage(Text.literal(NAMES[idx]), true);
        try {
            Identifier id = Identifier.of("arrowrip", SOUNDS[idx]);
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(id), 1.0f, 1.0f));
        } catch (Throwable t) {
            if (client.player != null) client.player.sendMessage(Text.literal("Ses motoru yüklenemedi"), true);
        }
    }
}
