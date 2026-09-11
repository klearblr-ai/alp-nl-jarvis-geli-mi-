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

    // Values translated from the uploaded .viz preset:
    // Spectrum, 173 samples, 45-300 Hz, mirrored, smooth=1,
    // circular path, red/black line layers + white bars and beat RGB split.
    private static final int VIZ_SAMPLES = 173;
    private static final double VIZ_LOW_HZ = 45.0;
    private static final double VIZ_HIGH_HZ = 300.0;
    private static final double VIZ_RED_MULT = 2.0;
    private static final double VIZ_BLACK_MULT = 1.9;
    private static final double VIZ_BAR_MULT = 1.5;

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

            if (client.options.attackKey.isPressed() ||
                    (client.targetedEntity instanceof PlayerEntity target && target != p && p.distanceTo(target) < 5.0f)) {
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
            if (wanted != activeTrack || !playing) playTrack(wanted);
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

        int panelW = 136;
        int panelX = w - panelW - 8;
        int panelY = Math.max(16, h / 2 - 82);
        int panelH = 164;

        ctx.fill(panelX - 4, panelY - 4, w - 4, panelY + panelH, 0x8A050000);
        ctx.fill(panelX - 2, panelY - 2, w - 6, panelY, 0xDD8A0008);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal("VIZ • " + track.name), panelX + 4, panelY + 5, 0xFFFF7777);

        if (visualizerEnabled) renderUploadedVizPreset(ctx, panelX, panelY, track);

        if (lyricsEnabled) {
            String lyric = getLyricDisplay(track);
            int textY = panelY + 132;
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal("SÖZ"), panelX + 4, textY, 0xFFFF4444);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(lyric), panelX + 4, textY + 13, 0xFFFFB0B0);
        }
    }

    private static void renderUploadedVizPreset(DrawContext ctx, int panelX, int panelY, Track track) {
        int cx = panelX + 68;
        int cy = panelY + 72;
        double baseRadius = 28.0;
        double beatTicks = Math.max(1.0, 1200.0 / track.bpm);
        double beatPhase = (playbackTicks % beatTicks) / beatTicks;
        double beat = Math.pow(Math.max(0.0, Math.sin(beatPhase * Math.PI)), 1.6);

        // The preset uses 173 mirrored spectrum samples. We preserve that sampling count,
        // but synthesize the per-band amplitudes from time/beat because JLayer playback
        // does not expose PCM FFT frames directly.
        for (int i = 0; i < VIZ_SAMPLES; i++) {
            double mirrored = i <= VIZ_SAMPLES / 2
                    ? i / (VIZ_SAMPLES / 2.0)
                    : (VIZ_SAMPLES - 1 - i) / (VIZ_SAMPLES / 2.0);

            double hz = VIZ_LOW_HZ + (VIZ_HIGH_HZ - VIZ_LOW_HZ) * Math.max(0.0, mirrored);
            double band = 0.50
                    + 0.23 * Math.sin(playbackTicks * 0.19 + i * 0.31 + hz * 0.014)
                    + 0.17 * Math.sin(playbackTicks * 0.11 + i * 0.73)
                    + 0.30 * beat;
            band = clamp01(band);

            double angle = (Math.PI * 2.0 * i / VIZ_SAMPLES) - Math.PI / 2.0;

            // Layer 1 from preset: red circular line, multiplier 2.0.
            double redR = baseRadius + band * 8.0 * VIZ_RED_MULT;
            int rx = (int)Math.round(cx + Math.cos(angle) * redR);
            int ry = (int)Math.round(cy + Math.sin(angle) * redR);
            putPixel(ctx, rx, ry, 0xF0FF1018);

            // Beat RGB split approximation from the preset.
            if (beat > 0.42 && (i & 1) == 0) {
                putPixel(ctx, rx + 1, ry, 0x99FF0000);
                putPixel(ctx, rx - 1, ry + 1, 0x9960AAFF);
            }

            // Layer 2 from preset: dark/black line, multiplier 1.9.
            double blackR = baseRadius + band * 7.2 * VIZ_BLACK_MULT;
            int bx = (int)Math.round(cx + Math.cos(angle) * blackR);
            int by = (int)Math.round(cy + Math.sin(angle) * blackR);
            putPixel(ctx, bx, by, 0xE8000000);

            // Layer 3 from preset: white circular bars, multiplier 1.5.
            if (i % 3 == 0) {
                double inner = baseRadius - 1.0;
                double outer = baseRadius + 4.0 + band * 8.0 * VIZ_BAR_MULT;
                drawRadialLine(ctx, cx, cy, angle, inner, outer, 0xEEFFFFFF);
            }

            // Preset particle/vortex feel: small beat-driven sparks around the circle.
            if (beat > 0.55 && i % 19 == (playbackTicks % 19)) {
                double sparkR = baseRadius + 18.0 + band * 7.0;
                int sx = (int)Math.round(cx + Math.cos(angle + playbackTicks * 0.015) * sparkR);
                int sy = (int)Math.round(cy + Math.sin(angle + playbackTicks * 0.015) * sparkR);
                ctx.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xCCFF3636);
            }
        }

        // Thin inner circle and a subtle beat pulse, matching the preset's central ring feel.
        drawCircle(ctx, cx, cy, (int)Math.round(baseRadius - 2 + beat * 2), 0xD0FFFFFF);
        if (beat > 0.62) drawCircle(ctx, cx, cy, (int)Math.round(baseRadius + 3 + beat * 4), 0x70FF2020);
    }

    private static void drawRadialLine(DrawContext ctx, int cx, int cy, double angle, double r1, double r2, int color) {
        int steps = Math.max(1, (int)Math.ceil(r2 - r1));
        for (int s = 0; s <= steps; s++) {
            double r = r1 + (r2 - r1) * (s / (double)steps);
            int x = (int)Math.round(cx + Math.cos(angle) * r);
            int y = (int)Math.round(cy + Math.sin(angle) * r);
            putPixel(ctx, x, y, color);
        }
    }

    private static void drawCircle(DrawContext ctx, int cx, int cy, int radius, int color) {
        int points = 144;
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2.0 * i / points;
            int x = (int)Math.round(cx + Math.cos(a) * radius);
            int y = (int)Math.round(cy + Math.sin(a) * radius);
            putPixel(ctx, x, y, color);
        }
    }

    private static void putPixel(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x, y, x + 1, y + 1, color);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static String getLyricDisplay(Track track) {
        double beatTicks = Math.max(1.0, 1200.0 / track.bpm);
        int beat = (int)(playbackTicks / beatTicks);
        return (beat % 4 == 3) ? "♪ …" : "♪ çözülüyor…";
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

    public static void cycleBackgroundTrack() { selectedTrack = (selectedTrack + 1) % TRACKS.length; }
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
