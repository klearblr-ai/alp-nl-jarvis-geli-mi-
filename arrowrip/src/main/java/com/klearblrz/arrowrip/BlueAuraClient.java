package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;

public final class BlueAuraClient implements ClientModInitializer {
    private static final String AURA_SOUND = "/assets/arrowrip/anime_aura_leaking_power.wav";
    private static final String ANIME_VOICE = "/assets/arrowrip/anime_japanese_voice.wav";
    private static KeyBinding auraKey, bloodKey;
    private static boolean auraActive, bloodMode;
    private static Clip auraClip;
    private static Clip voiceClip;
    private static int auraTicks, burstTicks;

    @Override
    public void onInitializeClient() {
        auraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.blue_aura", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, ArrowRipClient.CATEGORY));
        bloodKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.blood_aura", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                while (auraKey.wasPressed()) {
                    auraActive = !auraActive;
                    bloodMode = false;
                    burstTicks = auraActive ? 18 : 0;
                    auraTicks = 0;
                    if (auraActive) {
                        playAuraSound();
                        playAnimeVoice();
                    } else stopAuraSound();
                    if (client.player != null) client.player.sendMessage(Text.literal(auraActive ? "Mavi Aura: AÇIK" : "Mavi Aura: KAPALI"), true);
                }
                while (bloodKey.wasPressed()) {
                    bloodMode = !bloodMode;
                    auraActive = bloodMode;
                    burstTicks = bloodMode ? 18 : 0;
                    auraTicks = 0;
                    if (bloodMode) {
                        playAuraSound();
                        playAnimeVoice();
                    } else stopAuraSound();
                    if (client.player != null) client.player.sendMessage(Text.literal(bloodMode ? "Kan Aura: AÇIK" : "Kan Aura: KAPALI"), true);
                }
                if (client.player == null || client.world == null) {
                    auraActive = false;
                    bloodMode = false;
                    stopAuraSound();
                    stopAnimeVoice();
                    return;
                }
                if (auraActive) auraTicks++;
                if (burstTicks > 0) burstTicks--;
            } catch (Throwable ignored) {
                auraActive = false;
                bloodMode = false;
                stopAuraSound();
                stopAnimeVoice();
            }
        });

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!auraActive) return;
            try {
                VertexConsumer fill = context.consumers().getBuffer(RenderLayers.debugFilledBox());
                VertexConsumer lines = context.consumers().getBuffer(RenderLayers.linesTranslucent());
                renderAura(context.matrices(), fill, lines);
            } catch (Throwable ignored) {}
        });
    }

    private static void renderAura(MatrixStack matrices, VertexConsumer fill, VertexConsumer lines) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.getPerspective().isFirstPerson()) return;

        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        double px = mc.player.getX() - cam.x;
        double py = mc.player.getY() - cam.y;
        double pz = mc.player.getZ() - cam.z;

        float r = bloodMode ? 0.78f : 0.03f;
        float g = bloodMode ? 0.015f : 0.48f;
        float b = bloodMode ? 0.02f : 1.00f;
        float pulse = (float)(0.5 + 0.5 * Math.sin(auraTicks * 0.34));

        for (int layer = 0; layer < 5; layer++) {
            double e = 0.018 + layer * 0.025 + pulse * 0.012;
            float alpha = Math.max(0.035f, 0.18f - layer * 0.028f);
            drawBody(fill, matrices, px, py, pz, e, r, g, b, alpha);
        }

        drawBody(fill, matrices, px, py, pz, 0.006 + pulse * 0.008,
                bloodMode ? 1.0f : 0.40f,
                bloodMode ? 0.08f : 0.82f,
                bloodMode ? 0.08f : 1.0f,
                0.22f + pulse * 0.08f);

        if (burstTicks > 0) {
            double t = (18 - burstTicks) / 18.0;
            double e = 0.10 + t * 0.70;
            float a = (float)((1.0 - t) * 0.24);
            box(matrices, fill,
                    px - 0.62 - e, py - 0.10 - e, pz - 0.38 - e,
                    px + 0.62 + e, py + 2.06 + e, pz + 0.38 + e,
                    r, g, b, a);
        }

        int color = argb(0.78f, Math.min(1f,r+0.18f), Math.min(1f,g+0.18f), Math.min(1f,b+0.18f));
        VertexRendering.drawOutline(matrices, lines,
                net.minecraft.util.shape.VoxelShapes.cuboid(-0.68, -0.08, -0.42, 0.68, 2.10, 0.42),
                px, py, pz, color, 1.7f);
    }

    private static void drawBody(VertexConsumer fill, MatrixStack matrices,
                                 double px, double py, double pz, double e,
                                 float r, float g, float b, float a) {
        box(matrices, fill, px-0.31-e, py+1.43-e, pz-0.31-e, px+0.31+e, py+2.05+e, pz+0.31+e, r,g,b,a);
        box(matrices, fill, px-0.37-e, py+0.68-e, pz-0.22-e, px+0.37+e, py+1.50+e, pz+0.22+e, r,g,b,a);
        box(matrices, fill, px-0.61-e, py+0.68-e, pz-0.18-e, px-0.37+e, py+1.48+e, pz+0.18+e, r,g,b,a);
        box(matrices, fill, px+0.37-e, py+0.68-e, pz-0.18-e, px+0.61+e, py+1.48+e, pz+0.18+e, r,g,b,a);
        box(matrices, fill, px-0.31-e, py-0.03-e, pz-0.18-e, px-0.02+e, py+0.72+e, pz+0.18+e, r,g,b,a);
        box(matrices, fill, px+0.02-e, py-0.03-e, pz-0.18-e, px+0.31+e, py+0.72+e, pz+0.18+e, r,g,b,a);
    }

    private static void box(MatrixStack matrices, VertexConsumer v,
                            double x1,double y1,double z1,double x2,double y2,double z2,
                            float r,float g,float b,float a) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        int c = argb(a,r,g,b);
        float ax=(float)x1, ay=(float)y1, az=(float)z1, bx=(float)x2, by=(float)y2, bz=(float)z2;
        quad(v,m, ax,ay,az, bx,ay,az, bx,by,az, ax,by,az,c);
        quad(v,m, bx,ay,bz, ax,ay,bz, ax,by,bz, bx,by,bz,c);
        quad(v,m, ax,ay,bz, ax,ay,az, ax,by,az, ax,by,bz,c);
        quad(v,m, bx,ay,az, bx,ay,bz, bx,by,bz, bx,by,az,c);
        quad(v,m, ax,by,az, bx,by,az, bx,by,bz, ax,by,bz,c);
        quad(v,m, ax,ay,bz, bx,ay,bz, bx,ay,az, ax,ay,az,c);
    }

    private static void quad(VertexConsumer v, Matrix4f m,
                             float x1,float y1,float z1,float x2,float y2,float z2,
                             float x3,float y3,float z3,float x4,float y4,float z4,int c) {
        v.vertex(m,x1,y1,z1).color(c);
        v.vertex(m,x2,y2,z2).color(c);
        v.vertex(m,x3,y3,z3).color(c);
        v.vertex(m,x4,y4,z4).color(c);
    }

    private static int argb(float a, float r, float g, float b) {
        int ai=Math.max(0,Math.min(255,Math.round(a*255f)));
        int ri=Math.max(0,Math.min(255,Math.round(r*255f)));
        int gi=Math.max(0,Math.min(255,Math.round(g*255f)));
        int bi=Math.max(0,Math.min(255,Math.round(b*255f)));
        return (ai<<24)|(ri<<16)|(gi<<8)|bi;
    }

    private static void playAuraSound() {
        stopAuraSound();
        Thread thread = new Thread(() -> {
            try (InputStream raw = BlueAuraClient.class.getResourceAsStream(AURA_SOUND)) {
                if (raw == null) return;
                try (BufferedInputStream buffered = new BufferedInputStream(raw);
                     AudioInputStream audio = AudioSystem.getAudioInputStream(buffered)) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(audio);
                    auraClip = clip;
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    clip.start();
                }
            } catch (Throwable ignored) { auraClip = null; }
        }, "ArrowRip-Aura-Sound");
        thread.setDaemon(true);
        thread.start();
    }

    private static void playAnimeVoice() {
        stopAnimeVoice();
        Thread thread = new Thread(() -> {
            try (InputStream raw = BlueAuraClient.class.getResourceAsStream(ANIME_VOICE)) {
                if (raw == null) return;
                try (BufferedInputStream buffered = new BufferedInputStream(raw);
                     AudioInputStream audio = AudioSystem.getAudioInputStream(buffered)) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(audio);
                    voiceClip = clip;
                    clip.start();
                }
            } catch (Throwable ignored) { voiceClip = null; }
        }, "ArrowRip-Anime-Voice");
        thread.setDaemon(true);
        thread.start();
    }

    private static void stopAuraSound() {
        try {
            Clip clip = auraClip;
            if (clip != null) { clip.stop(); clip.close(); }
        } catch (Throwable ignored) {}
        auraClip = null;
    }

    private static void stopAnimeVoice() {
        try {
            Clip clip = voiceClip;
            if (clip != null) { clip.stop(); clip.close(); }
        } catch (Throwable ignored) {}
        voiceClip = null;
    }
}
