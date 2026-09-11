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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class BrutalFinisherClient implements ClientModInitializer {
    private static KeyBinding menuKey;

    private static final Track[] TRACKS = {
            new Track("DARK KNIGHT", "/assets/arrowrip/dark_knight.mp3", 129.20),
            new Track("HXME INVASION", "/assets/arrowrip/hxme_invasion.mp3", 123.05),
            new Track("Level Up", "/assets/arrowrip/level_up.mp3", 123.05),
            new Track("OUR WXRLD", "/assets/arrowrip/our_wxrld.mp3", 117.45),
            new Track("RAGE QUIT", "/assets/arrowrip/rage_quit.mp3", 117.45)
    };

    // Fallback values. The final JAR contains the user's .viz and these are replaced
    // at runtime by values read directly from visualizer_88.json inside that .viz ZIP.
    private static int vizSamples = 173;
    private static double vizLowHz = 45.0;
    private static double vizHighHz = 300.0;
    private static double vizRedMult = 2.0;
    private static double vizBlackMult = 1.9;
    private static double vizBarMult = 1.5;
    private static boolean vizMirror = true;
    private static boolean vizPresetLoaded;

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
        loadVizPresetResource();

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

    private static void loadVizPresetResource() {
        try (InputStream raw = BrutalFinisherClient.class.getResourceAsStream("/assets/arrowrip/next_level_phonk.viz")) {
            if (raw == null) return;
            try (ZipInputStream zip = new ZipInputStream(raw)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (!"visualizer_88.json".equals(entry.getName())) continue;
                    String json = new String(zip.readAllBytes(), StandardCharsets.UTF_8);

                    vizSamples = Math.max(24, Math.min(512, (int)Math.round(readNumber(json, "sampleOutCount", vizSamples))));
                    vizLowHz = readNumber(json, "lowerHz", vizLowHz);
                    vizHighHz = readNumber(json, "higherHz", vizHighHz);
                    vizMirror = readNumber(json, "mirrorSamples", vizMirror ? 1 : 0) >= 0.5;

                    List<Double> multipliers = readNumbers(json, "barHeightMultiplier");
                    if (multipliers.size() > 0) vizRedMult = multipliers.get(0);
                    if (multipliers.size() > 1) vizBlackMult = multipliers.get(1);
                    if (multipliers.size() > 2) vizBarMult = multipliers.get(2);

                    vizPresetLoaded = true;
                    break;
                }
            }
        } catch (Exception ignored) {
            vizPresetLoaded = false;
        }
    }

    private static double readNumber(String json, String key, double fallback) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\{.*?\\\"v\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (!m.find()) return fallback;
        try { return Double.parseDouble(m.group(1)); }
        catch (Exception ignored) { return fallback; }
    }

    private static List<Double> readNumbers(String json, String key) {
        List<Double> out = new ArrayList<>();
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\{.*?\\\"v\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        while (m.find() && out.size() < 8) {
            try { out.add(Double.parseDouble(m.group(1))); }
            catch (Exception ignored) {}
        }
        return out;
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
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal((vizPresetLoaded ? "VIZ • " : "VIZ* • ") + track.name), panelX + 4, panelY + 5, 0xFFFF7777);

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

        for (int i = 0; i < vizSamples; i++) {
            double samplePosition = i / Math.max(1.0, vizSamples - 1.0);
            double mirrored = vizMirror
                    ? (samplePosition <= 0.5 ? samplePosition * 2.0 : (1.0 - samplePosition) * 2.0)
                    : samplePosition;

            double hz = vizLowHz + (vizHighHz - vizLowHz) * Math.max(0.0, mirrored);
            double band = 0.50
                    + 0.23 * Math.sin(playbackTicks * 0.19 + i * 0.31 + hz * 0.014)
                    + 0.17 * Math.sin(playbackTicks * 0.11 + i * 0.73)
                    + 0.30 * beat;
            band = clamp01(band);

            double angle = (Math.PI * 2.0 * i / vizSamples) - Math.PI / 2.0;

            double redR = baseRadius + band * 8.0 * vizRedMult;
            int rx = (int)Math.round(cx + Math.cos(angle) * redR);
            int ry = (int)Math.round(cy + Math.sin(angle) * redR);
            putPixel(ctx, rx, ry, 0xF0FF1018);

            if (beat > 0.42 && (i & 1) == 0) {
                putPixel(ctx, rx + 1, ry, 0x99FF0000);
                putPixel(ctx, rx - 1, ry + 1, 0x9960AAFF);
            }

            double blackR = baseRadius + band * 7.2 * vizBlackMult;
            int bx = (int)Math.round(cx + Math.cos(angle) * blackR);
            int by = (int)Math.round(cy + Math.sin(angle) * blackR);
            putPixel(ctx, bx, by, 0xE8000000);

            if (i % 3 == 0) {
                double inner = baseRadius - 1.0;
                double outer = baseRadius + 4.0 + band * 8.0 * vizBarMult;
                drawRadialLine(ctx, cx, cy, angle, inner, outer, 0xEEFFFFFF);
            }

            if (beat > 0.55 && i % 19 == (playbackTicks % 19)) {
                double sparkR = baseRadius + 18.0 + band * 7.0;
                int sx = (int)Math.round(cx + Math.cos(angle + playbackTicks * 0.015) * sparkR);
                int sy = (int)Math.round(cy + Math.sin(angle + playbackTicks * 0.015) * sparkR);
                ctx.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xCCFF3636);
            }
        }

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
