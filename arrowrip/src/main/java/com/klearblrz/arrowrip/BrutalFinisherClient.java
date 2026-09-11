package com.klearblrz.arrowrip;

import javazoom.jl.player.Player;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;

public final class BrutalFinisherClient implements ClientModInitializer {
    private static KeyBinding menuKey;

    private static final Track[] TRACKS = {
            new Track("DARK KNIGHT", "/assets/arrowrip/dark_knight.mp3"),
            new Track("HXME INVASION", "/assets/arrowrip/hxme_invasion.mp3"),
            new Track("Level Up", "/assets/arrowrip/level_up.mp3"),
            new Track("OUR WXRLD", "/assets/arrowrip/our_wxrld.mp3"),
            new Track("RAGE QUIT", "/assets/arrowrip/rage_quit.mp3")
    };

    private static int selectedTrack = 0;
    private static volatile Player musicPlayer;
    private static volatile boolean playing;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.music_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new FinisherMenuScreen());
            }
        });
    }

    public static void cycleBackgroundTrack() {
        selectedTrack = (selectedTrack + 1) % TRACKS.length;
    }

    public static String getBackgroundTrackName() {
        return TRACKS[selectedTrack].name;
    }

    public static boolean isBackgroundPlaying() {
        return playing;
    }

    public static void playBackgroundSelected() {
        stopBackgroundMusic();
        startMusic(TRACKS[selectedTrack].path);
    }

    public static void stopBackgroundMusic() {
        try {
            Player old = musicPlayer;
            if (old != null) old.close();
        } catch (Exception ignored) {}
        musicPlayer = null;
        playing = false;
    }

    private static void startMusic(String resourcePath) {
        Thread t = new Thread(() -> {
            try (InputStream in = BrutalFinisherClient.class.getResourceAsStream(resourcePath)) {
                if (in == null) return;
                Player p = new Player(in);
                musicPlayer = p;
                playing = true;
                p.play();
            } catch (Exception ignored) {
            } finally {
                musicPlayer = null;
                playing = false;
            }
        }, "ArrowRip-Music");
        t.setDaemon(true);
        t.start();
    }

    private record Track(String name, String path) {}
}
