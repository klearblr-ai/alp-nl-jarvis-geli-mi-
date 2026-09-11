package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

public final class JapaneseVoiceClient implements ClientModInitializer {
    private static final String[] NAMES = {
            "Nani?!", "Yamero!", "Ikuzo!", "Yare yare...", "Mada mada!", "Sugoi!", "Nanda?!", "Kamuda!"
    };
    private static final String[] FILES = {
            "/assets/arrowrip/voice_nani.wav",
            "/assets/arrowrip/voice_yamero.wav",
            "/assets/arrowrip/voice_ikuzo.wav",
            "/assets/arrowrip/voice_yareyare.wav",
            "/assets/arrowrip/voice_madamada.wav",
            "/assets/arrowrip/voice_sugoi.wav",
            "/assets/arrowrip/voice_nanda.wav",
            "/assets/arrowrip/voice_kamuda.wav"
    };

    private static KeyBinding voiceKey;
    private static int lastIndex = -1;
    private static volatile Clip activeClip;

    @Override
    public void onInitializeClient() {
        voiceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.random_japanese_voice", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (voiceKey.wasPressed()) playRandom(client);
        });
    }

    private static void playRandom(MinecraftClient client) {
        int idx;
        if (FILES.length <= 1) idx = 0;
        else {
            do idx = ThreadLocalRandom.current().nextInt(FILES.length);
            while (idx == lastIndex);
        }
        lastIndex = idx;
        if (client.player != null) client.player.sendMessage(Text.literal(NAMES[idx]), true);
        playOneShot(FILES[idx]);
    }

    private static void playOneShot(String resource) {
        try {
            Clip old = activeClip;
            if (old != null) { old.stop(); old.close(); }
        } catch (Throwable ignored) {}

        Thread t = new Thread(() -> {
            Clip clip = null;
            try (InputStream raw = JapaneseVoiceClient.class.getResourceAsStream(resource)) {
                if (raw == null) return;
                try (BufferedInputStream buffered = new BufferedInputStream(raw);
                     AudioInputStream audio = AudioSystem.getAudioInputStream(buffered)) {
                    clip = AudioSystem.getClip();
                    clip.open(audio);
                    activeClip = clip;
                    clip.start();
                    while (clip.isRunning()) Thread.sleep(8L);
                }
            } catch (Throwable ignored) {
            } finally {
                try { if (clip != null) clip.close(); } catch (Throwable ignored) {}
                if (activeClip == clip) activeClip = null;
            }
        }, "ArrowRip-Random-Japanese-Voice");
        t.setDaemon(true);
        t.start();
    }
}
