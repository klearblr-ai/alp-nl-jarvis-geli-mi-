package com.klearblrz.arrowrip;

import javazoom.jl.player.Player;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class BrutalFinisherClient implements ClientModInitializer {
    private static final DustParticleEffect BLOOD = new DustParticleEffect(0x8B0000, 1.35f);
    private static final DustParticleEffect DARK = new DustParticleEffect(0x300000, 1.55f);
    private static KeyBinding finisherKey;

    private static final Track[] TRACKS = {
            new Track("/assets/arrowrip/dark_knight.mp3", 129.19921875),
            new Track("/assets/arrowrip/hxme_invasion.mp3", 123.046875),
            new Track("/assets/arrowrip/level_up.mp3", 123.046875),
            new Track("/assets/arrowrip/our_wxrld.mp3", 117.45383522727273),
            new Track("/assets/arrowrip/rage_quit.mp3", 117.45383522727273)
    };

    private static UUID lastCombatTarget;
    private static Vec3d lastCombatPosition = Vec3d.ZERO;
    private static int combatMemoryTicks;
    private static boolean finisherActive;
    private static int finisherTick;
    private static int beatIndex;
    private static int nextBeatTick;
    private static int beatIntervalTicks = 10;
    private static int finisherEndTick = 320;
    private static Track activeTrack = TRACKS[0];
    private static Vec3d finisherOrigin = Vec3d.ZERO;
    private static final List<SwordVisual> swords = new ArrayList<>();
    private static ArmorStandEntity headVisual;
    private static int headTick;
    private static volatile Player musicPlayer;

    private static boolean savedForward, savedBack, savedLeft, savedRight, savedJump, savedSneak;

    @Override
    public void onInitializeClient() {
        finisherKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.brutal_finisher", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, ArrowRipClient.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(BrutalFinisherClient::tick);
    }

    private static void tick(MinecraftClient client) {
        PlayerEntity self = client.player;
        if (self == null || client.world == null) {
            clearVisuals();
            finisherActive = false;
            stopMusic();
            return;
        }

        if (combatMemoryTicks > 0) combatMemoryTicks--;
        if (client.options.attackKey.isPressed() && client.targetedEntity instanceof PlayerEntity target && target != self && self.distanceTo(target) < 5.0f) {
            lastCombatTarget = target.getUuid();
            lastCombatPosition = new Vec3d(target.getX(), target.getY(), target.getZ());
            combatMemoryTicks = 60;
        }

        // Reliable automatic trigger: if the recently hit player dies OR disappears from the client right after the kill,
        // run the finisher at the last position we saw them.
        if (!finisherActive && lastCombatTarget != null && combatMemoryTicks > 0) {
            PlayerEntity target = client.world.getPlayerByUuid(lastCombatTarget);
            if (target == null) {
                startFinisher(client, self, lastCombatPosition);
                clearCombatMemory();
            } else {
                lastCombatPosition = new Vec3d(target.getX(), target.getY(), target.getZ());
                if (!target.isAlive() || target.getHealth() <= 0.0f) {
                    startFinisher(client, self, lastCombatPosition);
                    clearCombatMemory();
                }
            }
        }

        // H stays as a manual preview/test key.
        while (finisherKey.wasPressed()) {
            if (client.targetedEntity instanceof PlayerEntity target && target != self && self.distanceTo(target) < 8.0f) {
                startFinisher(client, self, new Vec3d(target.getX(), target.getY(), target.getZ()));
            }
        }

        if (finisherActive) tickFinisher(client, self);
        tickSwordVisuals();
        tickHeadVisual();
    }

    private static void clearCombatMemory() {
        lastCombatTarget = null;
        combatMemoryTicks = 0;
    }

    private static void startFinisher(MinecraftClient client, PlayerEntity self, Vec3d origin) {
        clearVisuals();
        stopMusic();

        savedForward = client.options.forwardKey.isPressed();
        savedBack = client.options.backKey.isPressed();
        savedLeft = client.options.leftKey.isPressed();
        savedRight = client.options.rightKey.isPressed();
        savedJump = client.options.jumpKey.isPressed();
        savedSneak = client.options.sneakKey.isPressed();

        finisherActive = true;
        finisherTick = 0;
        beatIndex = 0;
        activeTrack = TRACKS[ThreadLocalRandom.current().nextInt(TRACKS.length)];
        beatIntervalTicks = Math.max(8, Math.round((float)(1200.0 / activeTrack.bpm)));
        nextBeatTick = 1;
        finisherEndTick = beatIntervalTicks * 32 + 52;
        finisherOrigin = origin;
        self.swingHand(Hand.MAIN_HAND);
        self.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.65f, 0.72f);
        startMusic(activeTrack.path);
        bloodBurst(client, origin.add(0, 0.95, 0), 28);
    }

    private static void tickFinisher(MinecraftClient client, PlayerEntity self) {
        finisherTick++;

        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sneakKey.setPressed(false);

        if (beatIndex < 32 && finisherTick >= nextBeatTick) {
            beatStab(client, self, beatIndex);
            beatIndex++;
            nextBeatTick += beatIntervalTicks;
        }

        if (beatIndex >= 32 && finisherTick == nextBeatTick + 2) {
            decapitationEffect(client);
        }

        if (finisherTick > finisherEndTick) {
            finishFinisher(client);
        }
    }

    private static void finishFinisher(MinecraftClient client) {
        finisherActive = false;
        stopMusic();
        client.options.forwardKey.setPressed(savedForward);
        client.options.backKey.setPressed(savedBack);
        client.options.leftKey.setPressed(savedLeft);
        client.options.rightKey.setPressed(savedRight);
        client.options.jumpKey.setPressed(savedJump);
        client.options.sneakKey.setPressed(savedSneak);
    }

    private static void beatStab(MinecraftClient client, PlayerEntity self, int index) {
        double a = index * 2.3999632297;
        double radius = 0.72 + (index % 3) * 0.10;
        double y = finisherOrigin.y + 0.45 + (index % 4) * 0.27;
        double x = finisherOrigin.x + Math.cos(a) * radius;
        double z = finisherOrigin.z + Math.sin(a) * radius;

        ArmorStandEntity stand = new ArmorStandEntity(client.world, x, y, z);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setShowArms(true);
        stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(selectSword(index)));
        stand.setYaw((float)Math.toDegrees(Math.atan2(finisherOrigin.z - z, finisherOrigin.x - x)) - 90f);
        stand.setRightArmRotation(new EulerAngle(-82f + (index % 3) * 8f, 0f, 0f));
        client.world.addEntity(stand);
        swords.add(new SwordVisual(stand, Math.max(70, beatIntervalTicks * 7)));

        self.swingHand((index & 1) == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND);
        self.playSound((index % 4 == 0) ? SoundEvents.ENTITY_PLAYER_ATTACK_CRIT : SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                0.42f, 0.72f + (index % 5) * 0.055f);
        bloodBurst(client, new Vec3d(finisherOrigin.x, y, finisherOrigin.z), index % 4 == 0 ? 34 : 18);
    }

    private static net.minecraft.item.Item selectSword(int i) {
        return switch (i % 5) {
            case 0 -> Items.NETHERITE_SWORD;
            case 1 -> Items.DIAMOND_SWORD;
            case 2 -> Items.IRON_SWORD;
            case 3 -> Items.GOLDEN_SWORD;
            default -> Items.STONE_SWORD;
        };
    }

    private static void decapitationEffect(MinecraftClient client) {
        Vec3d neck = finisherOrigin.add(0, 1.55, 0);
        bloodBurst(client, neck, 85);
        client.player.playSound(SoundEvents.ENTITY_PLAYER_HURT, 0.55f, 0.46f);

        headVisual = new ArmorStandEntity(client.world, neck.x, neck.y + 0.18, neck.z);
        headVisual.setInvisible(true);
        headVisual.setNoGravity(true);
        headVisual.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
        client.world.addEntity(headVisual);
        headTick = 36;
    }

    private static void tickHeadVisual() {
        if (headVisual == null) return;
        if (headTick-- <= 0 || !headVisual.isAlive()) {
            headVisual.discard();
            headVisual = null;
            return;
        }
        int age = 36 - headTick;
        double x = finisherOrigin.x + age * 0.025;
        double z = finisherOrigin.z + age * 0.012;
        double y = finisherOrigin.y + 1.73 + Math.sin(age / 36.0 * Math.PI) * 0.70 - age * 0.010;
        headVisual.setPosition(x, y, z);
        headVisual.setYaw(age * 17f);
    }

    private static void tickSwordVisuals() {
        Iterator<SwordVisual> it = swords.iterator();
        while (it.hasNext()) {
            SwordVisual s = it.next();
            s.life--;
            if (s.life <= 0 || !s.entity.isAlive()) {
                s.entity.discard();
                it.remove();
            }
        }
    }

    private static void bloodBurst(MinecraftClient c, Vec3d p, int n) {
        for (int i = 0; i < n; i++) {
            double a = c.world.random.nextDouble() * Math.PI * 2.0;
            double s = 0.025 + c.world.random.nextDouble() * 0.105;
            c.world.addParticleClient(i % 4 == 0 ? DARK : BLOOD,
                    p.x + (c.world.random.nextDouble() - .5) * .30,
                    p.y + (c.world.random.nextDouble() - .5) * .34,
                    p.z + (c.world.random.nextDouble() - .5) * .30,
                    Math.cos(a) * s,
                    (c.world.random.nextDouble() - .20) * .10,
                    Math.sin(a) * s);
        }
    }

    private static void startMusic(String resourcePath) {
        Thread t = new Thread(() -> {
            try (InputStream in = BrutalFinisherClient.class.getResourceAsStream(resourcePath)) {
                if (in == null) return;
                Player p = new Player(in);
                musicPlayer = p;
                p.play();
            } catch (Exception ignored) {
            } finally {
                musicPlayer = null;
            }
        }, "ArrowRip-Random-Finisher-Music");
        t.setDaemon(true);
        t.start();
    }

    private static void stopMusic() {
        try {
            Player old = musicPlayer;
            if (old != null) old.close();
        } catch (Exception ignored) {}
        musicPlayer = null;
    }

    private static void clearVisuals() {
        for (SwordVisual s : swords) if (s.entity != null) s.entity.discard();
        swords.clear();
        if (headVisual != null) headVisual.discard();
        headVisual = null;
        headTick = 0;
    }

    private record Track(String path, double bpm) {}

    private static final class SwordVisual {
        final ArmorStandEntity entity;
        int life;
        SwordVisual(ArmorStandEntity entity, int life) { this.entity = entity; this.life = life; }
    }
}
