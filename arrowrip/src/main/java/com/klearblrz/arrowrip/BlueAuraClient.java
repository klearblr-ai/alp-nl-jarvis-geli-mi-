package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;

public final class BlueAuraClient implements ClientModInitializer {
    private static final String AURA_TEAM = "arrowrip_blue_aura";
    private static final String AURA_SOUND = "/assets/arrowrip/anime_aura_leaking_power.wav";

    private static KeyBinding auraKey;
    private static boolean auraActive;
    private static Clip auraClip;

    private static BlockPos lightPos;
    private static BlockState replacedState;
    private static String previousTeamName;
    private static boolean previousGlowing;

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
                keepBlueGlow(client);
                keepBodyLight(client);
            }
        });
    }

    private static void enableAura(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        previousGlowing = client.player.isGlowing();
        Team old = client.player.getScoreboardTeam();
        previousTeamName = old == null ? null : old.getName();
        keepBlueGlow(client);
        keepBodyLight(client);
        playAuraSound();
    }

    private static void disableAura(MinecraftClient client) {
        stopAuraSound();
        restoreLight(client);

        if (client.player != null && client.world != null) {
            client.player.setGlowing(previousGlowing);
            Scoreboard board = client.world.getScoreboard();
            board.clearTeam(client.player.getNameForScoreboard());
            if (previousTeamName != null && !AURA_TEAM.equals(previousTeamName)) {
                Team old = board.getTeam(previousTeamName);
                if (old != null) board.addScoreHolderToTeam(client.player.getNameForScoreboard(), old);
            }
        }
        previousTeamName = null;
    }

    private static void keepBlueGlow(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        client.player.setGlowing(true);

        Scoreboard board = client.world.getScoreboard();
        Team auraTeam = board.getTeam(AURA_TEAM);
        if (auraTeam == null) auraTeam = board.addTeam(AURA_TEAM);
        auraTeam.setColor(Formatting.AQUA);

        if (client.player.getScoreboardTeam() != auraTeam) {
            board.addScoreHolderToTeam(client.player.getNameForScoreboard(), auraTeam);
        }
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
        client.world.setBlockState(lightPos, light, Block.NOTIFY_ALL);
    }

    private static void restoreLight(MinecraftClient client) {
        if (lightPos == null || replacedState == null || client.world == null) {
            lightPos = null;
            replacedState = null;
            return;
        }

        if (client.world.getBlockState(lightPos).isOf(Blocks.LIGHT)) {
            client.world.setBlockState(lightPos, replacedState, Block.NOTIFY_ALL);
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
