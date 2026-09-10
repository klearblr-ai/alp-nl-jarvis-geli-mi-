package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class ArrowRipClient implements ClientModInitializer {
    private static KeyBinding pullKey;
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("arrowrip", "main"));
    private static final DustParticleEffect BLOOD = new DustParticleEffect(0x7A0202, 1.15f);
    private static final DustParticleEffect DARK_BLOOD = new DustParticleEffect(0x380000, 1.35f);
    private static int holdTicks = 0;
    private static int animationTicks = 0;
    private static int visualArrowCount = 0;
    private static int bleedTicks = 0;
    private static int dripCooldown = 0;
    private static int groundBloodTicks = 0;
    private static double groundBloodX, groundBloodY, groundBloodZ;

    @Override
    public void onInitializeClient() {
        pullKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.pull",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(ArrowRipClient::tick);
    }

    private static void tick(MinecraftClient client) {
        PlayerEntity p = client.player;
        if (p == null || client.world == null) {
            holdTicks = 0;
            animationTicks = 0;
            visualArrowCount = 0;
            bleedTicks = 0;
            dripCooldown = 0;
            groundBloodTicks = 0;
            return;
        }

        int trackedArrows = p.getStuckArrowCount();
        if (trackedArrows > 0) {
            visualArrowCount = Math.max(visualArrowCount, trackedArrows);
            p.setStuckArrowCount(0);
        }

        if (animationTicks > 0) {
            animationTicks--;
            animatePull(client, p, animationTicks);
        }

        if (bleedTicks > 0) bleedTicks--;
        if (dripCooldown > 0) dripCooldown--;

        if ((visualArrowCount > 0 || bleedTicks > 0) && dripCooldown <= 0) {
            spawnBloodDrip(client, p, visualArrowCount > 0 ? 3 : 2);
            dripCooldown = visualArrowCount > 0 ? 4 + client.world.random.nextInt(5) : 7 + client.world.random.nextInt(7);
        }

        if (groundBloodTicks > 0) {
            groundBloodTicks--;
            if (groundBloodTicks % 3 == 0) spawnGroundBlood(client);
        }

        if (pullKey.isPressed() && visualArrowCount > 0) {
            holdTicks++;
            if (holdTicks == 1) {
                p.swingHand(Hand.MAIN_HAND);
                p.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), 0.18f, 1.35f);
            }
            if (holdTicks == 6) p.swingHand(Hand.MAIN_HAND);
            if (holdTicks == 12) {
                p.swingHand(Hand.MAIN_HAND);
                p.playSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 0.16f, 1.25f);
            }
            if (holdTicks >= 20) {
                visualArrowCount--;
                animationTicks = 16;
                bleedTicks = Math.max(bleedTicks, 90);
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
        for (int i = 0; i < 28; i++) {
            double ox = (client.world.random.nextDouble() - 0.5) * 0.42;
            double oy = (client.world.random.nextDouble() - 0.5) * 0.38;
            double oz = (client.world.random.nextDouble() - 0.5) * 0.42;
            double vx = (client.world.random.nextDouble() - 0.5) * 0.09;
            double vy = -0.025 - client.world.random.nextDouble() * 0.05;
            double vz = (client.world.random.nextDouble() - 0.5) * 0.09;
            client.world.addParticleClient(i % 3 == 0 ? DARK_BLOOD : BLOOD,
                    p.getX() + ox, chestY + oy, p.getZ() + oz, vx, vy, vz);
        }
        spawnBloodDrip(client, p, 14);
        dropBloodyArrow(client, p);
    }

    private static void dropBloodyArrow(MinecraftClient client, PlayerEntity p) {
        Vec3d look = p.getRotationVec(1.0f);
        double x = p.getX() + look.x * 0.28;
        double y = p.getY() + 0.95;
        double z = p.getZ() + look.z * 0.28;

        ItemEntity arrow = new ItemEntity(client.world, x, y, z, new ItemStack(Items.ARROW));
        arrow.setPickupDelayInfinite();
        arrow.setVelocity(look.x * 0.08, 0.12, look.z * 0.08);
        client.world.addEntity(arrow);

        groundBloodX = x + look.x * 0.18;
        groundBloodY = p.getY() + 0.035;
        groundBloodZ = z + look.z * 0.18;
        groundBloodTicks = 240;

        for (int i = 0; i < 18; i++) {
            client.world.addParticleClient(i % 2 == 0 ? BLOOD : DARK_BLOOD,
                    x + (client.world.random.nextDouble() - 0.5) * 0.18,
                    y + (client.world.random.nextDouble() - 0.5) * 0.12,
                    z + (client.world.random.nextDouble() - 0.5) * 0.18,
                    (client.world.random.nextDouble() - 0.5) * 0.025,
                    -0.03 - client.world.random.nextDouble() * 0.025,
                    (client.world.random.nextDouble() - 0.5) * 0.025);
        }
        spawnGroundBlood(client);
    }

    private static void spawnGroundBlood(MinecraftClient client) {
        int amount = groundBloodTicks > 180 ? 5 : 2;
        for (int i = 0; i < amount; i++) {
            double angle = client.world.random.nextDouble() * Math.PI * 2.0;
            double radius = client.world.random.nextDouble() * 0.38;
            client.world.addParticleClient(i % 3 == 0 ? DARK_BLOOD : BLOOD,
                    groundBloodX + Math.cos(angle) * radius,
                    groundBloodY,
                    groundBloodZ + Math.sin(angle) * radius,
                    0.0, 0.001, 0.0);
        }
    }

    private static void spawnBloodDrip(MinecraftClient client, PlayerEntity p, int amount) {
        double sourceY = p.getY() + p.getHeight() * (0.48 + client.world.random.nextDouble() * 0.22);
        for (int i = 0; i < amount; i++) {
            double ox = (client.world.random.nextDouble() - 0.5) * 0.34;
            double oz = (client.world.random.nextDouble() - 0.5) * 0.34;
            double vx = (client.world.random.nextDouble() - 0.5) * 0.025;
            double vy = -0.045 - client.world.random.nextDouble() * 0.035;
            double vz = (client.world.random.nextDouble() - 0.5) * 0.025;
            client.world.addParticleClient(i % 3 == 0 ? DARK_BLOOD : BLOOD,
                    p.getX() + ox, sourceY, p.getZ() + oz, vx, vy, vz);
        }
        if (client.world.random.nextInt(3) == 0) {
            client.world.addParticleClient(DARK_BLOOD,
                    p.getX() + (client.world.random.nextDouble() - 0.5) * 0.42,
                    p.getY() + 0.05,
                    p.getZ() + (client.world.random.nextDouble() - 0.5) * 0.42,
                    0.0, 0.005, 0.0);
        }
    }

    private static void animatePull(MinecraftClient client, PlayerEntity p, int left) {
        if (left == 14 || left == 9 || left == 4) p.swingHand(Hand.MAIN_HAND);
        if (left <= 12 && left >= 2 && client.world.random.nextBoolean()) {
            client.world.addParticleClient(BLOOD,
                    p.getX() + (client.world.random.nextDouble() - 0.5) * 0.28,
                    p.getY() + 0.15 + client.world.random.nextDouble() * 0.35,
                    p.getZ() + (client.world.random.nextDouble() - 0.5) * 0.28,
                    0.0, -0.035, 0.0);
        }
    }
}
