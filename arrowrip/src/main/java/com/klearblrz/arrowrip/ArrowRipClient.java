package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public final class ArrowRipClient implements ClientModInitializer {
    private static KeyBinding pullKey;
    private static int holdTicks = 0;
    private static int animationTicks = 0;
    private static int hiddenArrows = 0;
    private static int lastServerArrowCount = 0;

    @Override
    public void onInitializeClient() {
        pullKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.pull",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.arrowrip"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(ArrowRipClient::tick);
    }

    private static void tick(MinecraftClient client) {
        PlayerEntity p = client.player;
        if (p == null || client.world == null) {
            holdTicks = 0;
            animationTicks = 0;
            return;
        }
        int serverArrows = p.getStuckArrowCount();
        if (serverArrows < lastServerArrowCount) hiddenArrows = Math.min(hiddenArrows, serverArrows);
        lastServerArrowCount = serverArrows;
        if (animationTicks > 0) {
            animationTicks--;
            animatePull(client, p, animationTicks);
        }
        if (pullKey.isPressed() && serverArrows - hiddenArrows > 0) {
            holdTicks++;
            if (holdTicks == 1 || holdTicks == 7 || holdTicks == 13) p.swingHand(Hand.MAIN_HAND);
            if (holdTicks >= 18) {
                hiddenArrows++;
                animationTicks = 14;
                holdTicks = 0;
                ripArrow(client, p);
            }
        } else {
            holdTicks = 0;
        }
    }

    private static void ripArrow(MinecraftClient client, PlayerEntity p) {
        p.swingHand(Hand.MAIN_HAND);
        p.playSound(SoundEvents.ENTITY_PLAYER_HURT, 0.55f, 0.72f);
        p.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL, 0.38f, 0.58f);
        p.playSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 0.32f, 0.82f);
        double chestY = p.getY() + p.getHeight() * 0.64;
        for (int i = 0; i < 22; i++) {
            double ox = (client.world.random.nextDouble() - 0.5) * 0.42;
            double oy = (client.world.random.nextDouble() - 0.5) * 0.38;
            double oz = (client.world.random.nextDouble() - 0.5) * 0.42;
            double vx = (client.world.random.nextDouble() - 0.5) * 0.08;
            double vy = -0.025 - client.world.random.nextDouble() * 0.045;
            double vz = (client.world.random.nextDouble() - 0.5) * 0.08;
            client.world.addParticleClient(ParticleTypes.DAMAGE_INDICATOR,
                    p.getX() + ox, chestY + oy, p.getZ() + oz, vx, vy, vz);
        }
        for (int i = 0; i < 12; i++) {
            double t = i / 11.0;
            client.world.addParticleClient(ParticleTypes.DAMAGE_INDICATOR,
                    p.getX() + 0.26 - t * 0.10,
                    chestY - t * 1.05,
                    p.getZ() + 0.18 + t * 0.08,
                    0.0, -0.02, 0.0);
        }
    }

    private static void animatePull(MinecraftClient client, PlayerEntity p, int left) {
        if (left == 10 || left == 6 || left == 2) p.swingHand(Hand.MAIN_HAND);
        if (left <= 9 && left >= 2 && client.world.random.nextBoolean()) {
            client.world.addParticleClient(ParticleTypes.DAMAGE_INDICATOR,
                    p.getX() + (client.world.random.nextDouble() - 0.5) * 0.28,
                    p.getY() + 0.15 + client.world.random.nextDouble() * 0.35,
                    p.getZ() + (client.world.random.nextDouble() - 0.5) * 0.28,
                    0.0, -0.035, 0.0);
        }
    }
}
