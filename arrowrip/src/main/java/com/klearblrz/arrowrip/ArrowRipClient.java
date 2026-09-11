package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

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

    // Client-only sword lodged in another player's abdomen.
    private static UUID stabbedTargetUuid = null;
    private static ItemEntity stabbedSwordEntity = null;
    private static int stabbedSwordTicks = 0;
    private static int stabbedBloodCooldown = 0;

    @Override
    public void onInitializeClient() {
        pullKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.pull",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(ArrowRipClient::tick);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() || hand != Hand.MAIN_HAND || !(entity instanceof PlayerEntity target)) {
                return ActionResult.PASS;
            }
            ItemStack held = player.getMainHandStack();
            if (!(held.getItem() instanceof SwordItem)) {
                return ActionResult.PASS;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            lodgeSwordInTarget(client, player, target, held);
            return ActionResult.PASS;
        });
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
            clearStabVisual();
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

        tickStabbedSword(client);

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

    private static void lodgeSwordInTarget(MinecraftClient client, PlayerEntity attacker, PlayerEntity target, ItemStack swordStack) {
        if (client.world == null) return;
        clearStabVisual();

        stabbedTargetUuid = target.getUuid();
        stabbedSwordTicks = 65;
        stabbedBloodCooldown = 0;

        Vec3d towardAttacker = attacker.getPos().subtract(target.getPos());
        if (towardAttacker.lengthSquared() < 0.0001) towardAttacker = new Vec3d(0, 0, 1);
        towardAttacker = towardAttacker.normalize();

        double x = target.getX() + towardAttacker.x * 0.20;
        double y = target.getY() + target.getHeight() * 0.52;
        double z = target.getZ() + towardAttacker.z * 0.20;

        stabbedSwordEntity = new ItemEntity(client.world, x, y, z, swordStack.copyWithCount(1));
        stabbedSwordEntity.setPickupDelayInfinite();
        stabbedSwordEntity.setNoGravity(true);
        stabbedSwordEntity.setVelocity(Vec3d.ZERO);
        stabbedSwordEntity.setYaw(target.getYaw() + 90.0f);
        client.world.addEntity(stabbedSwordEntity);

        target.playSound(SoundEvents.ENTITY_PLAYER_HURT, 0.55f, 0.78f);
        target.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL, 0.30f, 0.62f);
        spawnTargetBlood(client, target, 26);
    }

    private static void tickStabbedSword(MinecraftClient client) {
        if (stabbedSwordTicks <= 0 || stabbedTargetUuid == null || stabbedSwordEntity == null || client.world == null) {
            if (stabbedSwordTicks <= 0) clearStabVisual();
            return;
        }

        Entity entity = client.world.getPlayerByUuid(stabbedTargetUuid);
        if (!(entity instanceof PlayerEntity target) || !target.isAlive()) {
            clearStabVisual();
            return;
        }

        stabbedSwordTicks--;
        if (stabbedBloodCooldown > 0) stabbedBloodCooldown--;

        Vec3d towardViewer = client.player != null ? client.player.getPos().subtract(target.getPos()) : new Vec3d(0, 0, 1);
        if (towardViewer.lengthSquared() < 0.0001) towardViewer = new Vec3d(0, 0, 1);
        towardViewer = towardViewer.normalize();

        double x = target.getX() + towardViewer.x * 0.20;
        double y = target.getY() + target.getHeight() * 0.52;
        double z = target.getZ() + towardViewer.z * 0.20;
        stabbedSwordEntity.setPosition(x, y, z);
        stabbedSwordEntity.setVelocity(Vec3d.ZERO);
        stabbedSwordEntity.setYaw(target.getYaw() + 90.0f);

        if (stabbedBloodCooldown <= 0) {
            spawnTargetBlood(client, target, 3);
            stabbedBloodCooldown = 5 + client.world.random.nextInt(5);
        }

        if (stabbedSwordTicks == 1) {
            spawnTargetBlood(client, target, 10);
        }
    }

    private static void clearStabVisual() {
        if (stabbedSwordEntity != null) {
            stabbedSwordEntity.discard();
        }
        stabbedSwordEntity = null;
        stabbedTargetUuid = null;
        stabbedSwordTicks = 0;
        stabbedBloodCooldown = 0;
    }

    private static void spawnTargetBlood(MinecraftClient client, PlayerEntity target, int amount) {
        if (client.world == null) return;
        double y = target.getY() + target.getHeight() * 0.52;
        for (int i = 0; i < amount; i++) {
            double ox = (client.world.random.nextDouble() - 0.5) * 0.32;
            double oz = (client.world.random.nextDouble() - 0.5) * 0.32;
            client.world.addParticleClient(i % 3 == 0 ? DARK_BLOOD : BLOOD,
                    target.getX() + ox,
                    y + (client.world.random.nextDouble() - 0.5) * 0.20,
                    target.getZ() + oz,
                    (client.world.random.nextDouble() - 0.5) * 0.035,
                    -0.035 - client.world.random.nextDouble() * 0.03,
                    (client.world.random.nextDouble() - 0.5) * 0.035);
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
