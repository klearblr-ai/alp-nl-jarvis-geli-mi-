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
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> renderAura(context.matrices(), context.consumers().getBuffer(RenderLayers.linesTranslucent())));
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

        Vec3d camera = client.gameRenderer.getCamera().getPos();
        double px = client.player.getX() - camera.x;
        double py = client.player.getY() - camera.y;
        double pz = client.player.getZ() - camera.z;

        // Small breathing/power pulse. This is geometry, not particles.
        double pulse = 0.015 + 0.020 * (0.5 + 0.5 * Math.sin(auraTicks * 0.32));

        // Several translucent full-body shells create a real bright aura around the model.
        for (int layer = 0; layer < 7; layer++) {
            double e = pulse + layer * 0.018;
            float fade = 0.92f - layer * 0.105f;
            float r = 0.10f + layer * 0.015f;
            float g = 0.62f + layer * 0.035f;
            float b = 1.00f;

            drawBodyPart(matrices, lines, new Box(px - 0.31 - e, py + 1.43 - e, pz - 0.31 - e,
                    px + 0.31 + e, py + 2.05 + e, pz + 0.31 + e), r, g, b, fade); // head
            drawBodyPart(matrices, lines, new Box(px - 0.37 - e, py + 0.68 - e, pz - 0.22 - e,
                    px + 0.37 + e, py + 1.50 + e, pz + 0.22 + e), r, g, b, fade); // torso
            drawBodyPart(matrices, lines, new Box(px - 0.61 - e, py + 0.68 - e, pz - 0.18 - e,
                    px - 0.37 + e, py + 1.48 + e, pz + 0.18 + e), r, g, b, fade * 0.86f); // left arm
            drawBodyPart(matrices, lines, new Box(px + 0.37 - e, py + 0.68 - e, pz - 0.18 - e,
                    px + 0.61 + e, py + 1.48 + e, pz + 0.18 + e), r, g, b, fade * 0.86f); // right arm
            drawBodyPart(matrices, lines, new Box(px - 0.31 - e, py - 0.03 - e, pz - 0.18 - e,
                    px - 0.02 + e, py + 0.72 + e, pz + 0.18 + e), r, g, b, fade * 0.82f); // left leg
            drawBodyPart(matrices, lines, new Box(px + 0.02 - e, py - 0.03 - e, pz - 0.18 - e,
                    px + 0.31 + e, py + 0.72 + e, pz + 0.18 + e), r, g, b, fade * 0.82f); // right leg
        }

        // Two wider halos make the blue light visibly leak away from the body.
        double halo = 0.10 + 0.04 * Math.sin(auraTicks * 0.23);
        WorldRenderer.drawBox(matrices, lines,
                new Box(px - 0.72 - halo, py - 0.10, pz - 0.42 - halo,
                        px + 0.72 + halo, py + 2.12, pz + 0.42 + halo),
                0.05f, 0.46f, 1.0f, 0.32f);
        WorldRenderer.drawBox(matrices, lines,
                new Box(px - 0.82 - halo, py - 0.18, pz - 0.50 - halo,
                        px + 0.82 + halo, py + 2.20, pz + 0.50 + halo),
                0.02f, 0.30f, 1.0f, 0.16f);
    }

    private static void drawBodyPart(MatrixStack matrices, VertexConsumer lines, Box box,
                                     float r, float g, float b, float alpha) {
        WorldRenderer.drawBox(matrices, lines, box, r, g, b, alpha);
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
