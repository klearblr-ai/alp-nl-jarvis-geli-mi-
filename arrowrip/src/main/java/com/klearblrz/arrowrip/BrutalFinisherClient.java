package com.klearblrz.arrowrip;

import javazoom.jl.player.Player;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
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
    private static volatile int activeTrack = -1;
    private static boolean autoMode = true;
    private static int reevaluateCooldown;
    private static int combatTicks;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.music_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new FinisherMenuScreen());
            }

            PlayerEntity p = client.player;
            if (p == null || client.world == null) return;

            if (client.options.attackKey.isPressed() || (client.targetedEntity instanceof PlayerEntity target && target != p && p.distanceTo(target) < 5.0f)) {
                combatTicks = 100;
            } else if (combatTicks > 0) {
                combatTicks--;
            }

            if (!autoMode) return;
            if (reevaluateCooldown > 0) {
                reevaluateCooldown--;
                return;
            }
            reevaluateCooldown = 20;

            int wanted = chooseAutoTrack(p);
            if (wanted != activeTrack || !playing) {
                playTrack(wanted);
            }
        });
    }

    private static int chooseAutoTrack(PlayerEntity p) {
        float hp = p.getHealth() / Math.max(1.0f, p.getMaxHealth());
        if (hp <= 0.25f) return 4;                  // RAGE QUIT - critical health
        if (combatTicks > 55) return 0;            // DARK KNIGHT - fresh/hard combat
        if (combatTicks > 0) return 1;             // HXME INVASION - ongoing combat
        if (p.isSprinting() || p.getVelocity().horizontalLengthSquared() > 0.08) return 2; // Level Up - movement
        if (hp <= 0.55f) return 3;                 // OUR WXRLD - hurt/recovery
        return 2;                                  // Level Up - normal roaming
    }

    public static void toggleAutoMode() {
        autoMode = !autoMode;
        if (autoMode) {
            reevaluateCooldown = 0;
        }
    }

    public static boolean isAutoMode() {
        return autoMode;
    }

    public static String getAutoModeName() {
        return autoMode ? "AÇIK" : "KAPALI";
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

    public static String getNowPlayingName() {
        return activeTrack >= 0 && activeTrack < TRACKS.length ? TRACKS[activeTrack].name : "Yok";
    }

    public static void playBackgroundSelected() {
        autoMode = false;
        playTrack(selectedTrack);
    }

    private static void playTrack(int index) {
        if (index < 0 || index >= TRACKS.length) return;
        stopBackgroundMusic();
        activeTrack = index;
        startMusic(TRACKS[index].path);
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
        }, "ArrowRip-Adaptive-Music");
        t.setDaemon(true);
        t.start();
    }

    private record Track(String name, String path) {}
}
