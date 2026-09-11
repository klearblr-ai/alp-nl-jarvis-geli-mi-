package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.lwjgl.glfw.GLFW;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;

public final class BlueAuraClient implements ClientModInitializer {
    private static final String AURA_SOUND = "/assets/arrowrip/anime_aura_leaking_power.wav";

    private static KeyBinding auraKey;
    private static boolean auraActive;
    private static Clip auraClip;
    private static int auraTicks;

    private static BlockPos lightPos;
    private static BlockState replacedState;

    @Override
    public void onInitializeClient() {
        auraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.blue_aura", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (auraKey.wasPressed()) {
                auraActive = !auraActive;
                if (auraActive) enableAura(client);
                else disableAura(client);
            }

            if (client.player == null || client.world == null) {
                if (auraActive) {
                    auraActive = false;
                    stopAuraSound();
                    lightPos = null;
                    replacedState = null;
                }
                return;
            }

            if (auraActive) {
                auraTicks++;
                keepBodyLight(client);
            }
        });

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context ->
                renderAura(context.matrices(), context.consumers().getBuffer(RenderLayers.linesTranslucent())));
    }

    private static void enableAura(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        auraTicks = 0;
        keepBodyLight(client);
        playAuraSound();
    }

    private static void disableAura(MinecraftClient client) {
        stopAuraSound();
        restoreLight(client);
        auraTicks = 0;
    }

    private static void renderAura(MatrixStack matrices, VertexConsumer lines) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!auraActive || client.player == null || client.world == null) return;

        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        double px = client.player.getX() - camera.x;
        double py = client.player.getY() - camera.y;
        double pz = client.player.getZ() - camera.z;
        double pulse = 0.015 + 0.020 * (0.5 + 0.5 * Math.sin(auraTicks * 0.32));

        for (int layer = 0; layer < 7; layer++) {
            double e = pulse + layer * 0.018;
            float fade = 0.92f - layer * 0.105f;
            int color = argb(fade, 0.08f + layer * 0.018f, 0.60f + layer * 0.045f, 1.0f);
            float width = 1.4f + layer * 0.33f;

            drawBodyPart(matrices, lines, new Box(-0.31-e, 1.43-e, -0.31-e, 0.31+e, 2.05+e, 0.31+e), px, py, pz, color, width);
            drawBodyPart(matrices, lines, new Box(-0.37-e, 0.68-e, -0.22-e, 0.37+e, 1.50+e, 0.22+e), px, py, pz, color, width);
            drawBodyPart(matrices, lines, new Box(-0.61-e, 0.68-e, -0.18-e, -0.37+e, 1.48+e, 0.18+e), px, py, pz, color, width);
            drawBodyPart(matrices, lines, new Box(0.37-e, 0.68-e, -0.18-e, 0.61+e, 1.48+e, 0.18+e), px, py, pz, color, width);
            drawBodyPart(matrices, lines, new Box(-0.31-e, -0.03-e, -0.18-e, -0.02+e, 0.72+e, 0.18+e), px, py, pz, color, width);
            drawBodyPart(matrices, lines, new Box(0.02-e, -0.03-e, -0.18-e, 0.31+e, 0.72+e, 0.18+e), px, py, pz, color, width);
        }

        double halo = 0.10 + 0.04 * Math.sin(auraTicks * 0.23);
        drawBodyPart(matrices, lines, new Box(-0.72-halo, -0.10, -0.42-halo, 0.72+halo, 2.12, 0.42+halo),
                px, py, pz, argb(0.36f, 0.03f, 0.45f, 1.0f), 2.2f);
        drawBodyPart(matrices, lines, new Box(-0.82-halo, -0.18, -0.50-halo, 0.82+halo, 2.20, 0.50+halo),
                px, py, pz, argb(0.18f, 0.02f, 0.28f, 1.0f), 2.8f);
    }

    private static void drawBodyPart(MatrixStack matrices, VertexConsumer lines, Box localBox,
                                     double x, double y, double z, int color, float width) {
        VertexRendering.drawOutline(matrices, lines, VoxelShapes.cuboid(localBox), x, y, z, color, width);
    }

    private static int argb(float a, float r, float g, float b) {
        int ai = Math.max(0, Math.min(255, Math.round(a * 255f)));
        int ri = Math.max(0, Math.min(255, Math.round(r * 255f)));
        int gi = Math.max(0, Math.min(255, Math.round(g * 255f)));
        int bi = Math.max(0, Math.min(255, Math.round(b * 255f)));
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static void keepBodyLight(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        BlockPos wanted = client.player.getBlockPos().up();
        if (wanted.equals(lightPos)) return;
        restoreLight(client);
        BlockState current = client.world.getBlockState(wanted);
        if (!current.isAir() && !current.isOf(Blocks.LIGHT)) return;
        lightPos = wanted.toImmutable();
        replacedState = current;
        BlockState light = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15);
        client.world.setBlockState(lightPos, light, Block.NOTIFY_LISTENERS);
    }

    private static void restoreLight(MinecraftClient client) {
        if (lightPos == null || replacedState == null || client.world == null) {
            lightPos = null;
            replacedState = null;
            return;
        }
        if (client.world.getBlockState(lightPos).isOf(Blocks.LIGHT)) {
            client.world.setBlockState(lightPos, replacedState, Block.NOTIFY_LISTENERS);
        }
        lightPos = null;
        replacedState = null;
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
            } catch (Exception ignored) {
                auraClip = null;
            }
        }, "ArrowRip-Blue-Aura-Sound");
        thread.setDaemon(true);
        thread.start();
    }

    private static void stopAuraSound() {
        try {
            Clip clip = auraClip;
            if (clip != null) {
                clip.stop();
                clip.close();
            }
        } catch (Exception ignored) {}
        auraClip = null;
    }
}
