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

        // Sağ kenarda gerçek HUD paneli.
        int panelW = 128;
        int panelX = w - panelW - 8;
        int panelY = Math.max(18, h / 2 - 74);
        int panelH = 148;
        ctx.fill(panelX - 4, panelY - 4, w - 4, panelY + panelH, 0x76090000);
        ctx.fill(panelX - 2, panelY - 2, w - 6, panelY, 0xCC7A0007);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal("🩸 " + track.name), panelX + 4, panelY + 5, 0xFFFF7777);

        if (visualizerEnabled) {
            int bars = 16;
            int barW = 4;
            int gap = 2;
            int baseY = panelY + 88;
            int startX = panelX + 4;
            double beatTicks = Math.max(1.0, 1200.0 / track.bpm);
            double beatPhase = (playbackTicks % beatTicks) / beatTicks;
            double pulse = Math.pow(Math.max(0.0, Math.sin(beatPhase * Math.PI)), 1.55);

            for (int i = 0; i < bars; i++) {
                double wave = 0.35 + 0.65 * Math.abs(Math.sin(playbackTicks * 0.23 + i * 0.61));
                int height = 4 + (int)(45.0 * (0.38 * wave + 0.62 * pulse));
                int x = startX + i * (barW + gap);
                int top = baseY - height;
                int color = (i % 4 == 0) ? 0xEE3D0000 : 0xF09B0010;
                ctx.fill(x, top, x + barW, baseY, color);
                if ((i + playbackTicks) % 5 == 0) {
                    int drip = 3 + (i % 5) * 2;
                    ctx.fill(x + 1, baseY, x + 3, baseY + drip, 0xDD620007);
                }
            }
        }

        if (lyricsEnabled) {
            String lyric = getLyricDisplay(track);
            int textY = panelY + 104;
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal("SÖZ"), panelX + 4, textY, 0xFFFF4444);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(lyric), panelX + 4, textY + 13, 0xFFFFB0B0);
        }
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
