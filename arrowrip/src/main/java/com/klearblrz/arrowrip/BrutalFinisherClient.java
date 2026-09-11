package com.klearblrz.arrowrip;

import javazoom.jl.player.Player;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;

public final class BrutalFinisherClient implements ClientModInitializer {
    private static KeyBinding menuKey;

    private static final Track[] TRACKS = {
            new Track("DARK KNIGHT", "/assets/arrowrip/dark_knight.mp3", 129.20),
            new Track("HXME INVASION", "/assets/arrowrip/hxme_invasion.mp3", 123.05),
            new Track("Level Up", "/assets/arrowrip/level_up.mp3", 123.05),
            new Track("OUR WXRLD", "/assets/arrowrip/our_wxrld.mp3", 117.45),
            new Track("RAGE QUIT", "/assets/arrowrip/rage_quit.mp3", 117.45)
    };

    private static int selectedTrack = 0;
    private static volatile Player musicPlayer;
    private static volatile boolean playing;
    private static volatile int activeTrack = -1;
    private static boolean autoMode = true;
    private static int reevaluateCooldown;
    private static int combatTicks;
    private static int playbackTicks;
    private static boolean visualizerEnabled = true;
    private static boolean lyricsEnabled = true;

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

            if (playing) playbackTicks++;

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

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderMusicHud(drawContext));
    }

    private static int chooseAutoTrack(PlayerEntity p) {
        float hp = p.getHealth() / Math.max(1.0f, p.getMaxHealth());
        if (hp <= 0.25f) return 4;
        if (combatTicks > 55) return 0;
        if (combatTicks > 0) return 1;
        if (p.isSprinting() || p.getVelocity().horizontalLengthSquared() > 0.08) return 2;
        if (hp <= 0.55f) return 3;
        return 2;
    }

    private static void renderMusicHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !playing || activeTrack < 0 || activeTrack >= TRACKS.length) return;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        Track track = TRACKS[activeTrack];

        if (visualizerEnabled) {
            int bars = 24;
            int barW = 3;
            int gap = 2;
            int totalW = bars * barW + (bars - 1) * gap;
            int startX = (w - totalW) / 2;
            int baseY = h - 34;
            double beatTicks = Math.max(1.0, 1200.0 / track.bpm);
            double beatPhase = (playbackTicks % beatTicks) / beatTicks;
            double pulse = Math.pow(Math.max(0.0, Math.sin(beatPhase * Math.PI)), 1.6);

            for (int i = 0; i < bars; i++) {
                double wave = 0.35 + 0.65 * Math.abs(Math.sin(playbackTicks * 0.23 + i * 0.61));
                double center = 1.0 - Math.abs((i - (bars - 1) / 2.0) / (bars / 2.0));
                int height = 3 + (int)(24.0 * (0.35 * wave + 0.65 * pulse) * (0.55 + center * 0.45));
                int x = startX + i * (barW + gap);
                int top = baseY - height;
                int color = (i % 5 == 0) ? 0xDD3A0000 : 0xEE8F0008;
                ctx.fill(x, top, x + barW, baseY, color);
                if ((i + playbackTicks) % 7 == 0) {
                    int drip = 2 + (i % 4) * 2;
                    ctx.fill(x + 1, baseY, x + 2, baseY + drip, 0xCC650006);
                }
            }
            ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(track.name), w / 2, h - 55, 0xFFD8D8D8);
        }

        if (lyricsEnabled) {
            String lyric = getLyricDisplay(track);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(lyric), w / 2, h - 72, 0xFFFF5555);
        }
    }

    private static String getLyricDisplay(Track track) {
        // Gerçek söz dosyası yoksa metin uydurmayız. Karışık/çözülemeyen bölüm ekranda üç nokta olarak kalır.
        // Böylece daha sonra zaman kodlu gerçek sözler eklendiğinde aynı HUD doğrudan onları gösterebilir.
        double beatTicks = Math.max(1.0, 1200.0 / track.bpm);
        int beat = (int)(playbackTicks / beatTicks);
        return (beat % 4 == 3) ? "♪ …" : "♪ " + track.name + "  ·  söz çözümleme";
    }

    public static void toggleVisualizer() { visualizerEnabled = !visualizerEnabled; }
    public static boolean isVisualizerEnabled() { return visualizerEnabled; }
    public static String getVisualizerName() { return visualizerEnabled ? "AÇIK" : "KAPALI"; }

    public static void toggleLyrics() { lyricsEnabled = !lyricsEnabled; }
    public static boolean isLyricsEnabled() { return lyricsEnabled; }
    public static String getLyricsName() { return lyricsEnabled ? "AÇIK" : "KAPALI"; }

    public static void toggleAutoMode() {
        autoMode = !autoMode;
        if (autoMode) reevaluateCooldown = 0;
    }

    public static boolean isAutoMode() { return autoMode; }
    public static String getAutoModeName() { return autoMode ? "AÇIK" : "KAPALI"; }

    public static void cycleBackgroundTrack() {
        selectedTrack = (selectedTrack + 1) % TRACKS.length;
    }

    public static String getBackgroundTrackName() { return TRACKS[selectedTrack].name; }
    public static boolean isBackgroundPlaying() { return playing; }
    public static String getNowPlayingName() { return activeTrack >= 0 && activeTrack < TRACKS.length ? TRACKS[activeTrack].name : "Yok"; }

    public static void playBackgroundSelected() {
        autoMode = false;
        playTrack(selectedTrack);
    }

    private static void playTrack(int index) {
        if (index < 0 || index >= TRACKS.length) return;
        stopBackgroundMusic();
        activeTrack = index;
        playbackTicks = 0;
        startMusic(TRACKS[index].path);
    }

    public static void stopBackgroundMusic() {
        try {
            Player old = musicPlayer;
            if (old != null) old.close();
        } catch (Exception ignored) {}
        musicPlayer = null;
        playing = false;
        playbackTicks = 0;
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

    private record Track(String name, String path, double bpm) {}
}
