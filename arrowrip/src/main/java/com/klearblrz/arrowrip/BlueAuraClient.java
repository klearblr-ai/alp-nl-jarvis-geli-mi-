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
import org.lwjgl.glfw.GLFW;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;

public final class BlueAuraClient implements ClientModInitializer {
    private static final String AURA_SOUND = "/assets/arrowrip/anime_aura_leaking_power.wav";
    private static KeyBinding auraKey, bloodKey;
    private static boolean auraActive, bloodMode;
    private static Clip auraClip;
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
                    if (auraActive) playAuraSound(); else stopAuraSound();
                    if (client.player != null) client.player.sendMessage(Text.literal(auraActive ? "Mavi Aura: AÇIK" : "Mavi Aura: KAPALI"), true);
                }
                while (bloodKey.wasPressed()) {
                    bloodMode = !bloodMode;
                    auraActive = bloodMode;
                    burstTicks = bloodMode ? 18 : 0;
                    auraTicks = 0;
                    if (bloodMode) playAuraSound(); else stopAuraSound();
                    if (client.player != null) client.player.sendMessage(Text.literal(bloodMode ? "Kan Aura: AÇIK" : "Kan Aura: KAPALI"), true);
                }

                if (client.player == null || client.world == null) {
                    auraActive = false;
                    bloodMode = false;
                    stopAuraSound();
                    return;
                }
                if (auraActive) auraTicks++;
                if (burstTicks > 0) burstTicks--;
            } catch (Throwable ignored) {
                auraActive = false;
                bloodMode = false;
                stopAuraSound();
            }
        });

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!auraActive) return;
            try {
                VertexConsumer fill = context.consumers().getBuffer(RenderLayers.debugFilledBox());
                VertexConsumer lines = context.consumers().getBuffer(RenderLayers.linesTranslucent());
                renderAura(context.matrices(), fill, lines);
            } catch (Throwable ignored) {
                // Visual-only effect: never take the game down with it.
            }
        });
    }

    private static void renderAura(MatrixStack matrices, VertexConsumer fill, VertexConsumer lines) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.options.getPerspective().isFirstPerson()) return;

        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        double px = mc.player.getX() - cam.x;
        double py = mc.player.getY() - cam.y;
        double pz = mc.player.getZ() - cam.z;

        float r = bloodMode ? 0.78f : 0.03f;
        float g = bloodMode ? 0.015f : 0.48f;
        float b = bloodMode ? 0.02f : 1.00f;
        float pulse = (float)(0.5 + 0.5 * Math.sin(auraTicks * 0.34));

        // Dense body-hugging emissive-looking shells. No particles.
        for (int layer = 0; layer < 5; layer++) {
            double e = 0.018 + layer * 0.025 + pulse * 0.012;
            float alpha = Math.max(0.035f, 0.18f - layer * 0.028f);
            drawBody(fill, matrices, px, py, pz, e, r, g, b, alpha);
        }

        // Bright inner flash so the light feels like it comes FROM the body.
        drawBody(fill, matrices, px, py, pz, 0.006 + pulse * 0.008,
                bloodMode ? 1.0f : 0.40f,
                bloodMode ? 0.08f : 0.82f,
                bloodMode ? 0.08f : 1.0f,
                0.22f + pulse * 0.08f);

        // Opening burst: an expanding full-body light explosion, still geometry not particles.
        if (burstTicks > 0) {
            double t = (18 - burstTicks) / 18.0;
            double e = 0.10 + t * 0.70;
            float a = (float)((1.0 - t) * 0.24);
            VertexRendering.drawFilledBox(matrices, fill,
                    px - 0.62 - e, py - 0.10 - e, pz - 0.38 - e,
                    px + 0.62 + e, py + 2.06 + e, pz + 0.38 + e,
                    r, g, b, a);
        }

        // Thin crisp rim, so the aura still reads clearly on bright maps.
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

    private static void box(MatrixStack matrices, VertexConsumer fill,
                            double x1,double y1,double z1,double x2,double y2,double z2,
                            float r,float g,float b,float a) {
        VertexRendering.drawFilledBox(matrices, fill, x1,y1,z1,x2,y2,z2,r,g,b,a);
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

    private static void stopAuraSound() {
        try {
            Clip clip = auraClip;
            if (clip != null) { clip.stop(); clip.close(); }
        } catch (Throwable ignored) {}
        auraClip = null;
    }
}
