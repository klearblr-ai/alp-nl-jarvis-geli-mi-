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

public final class BrutalFinisherClient implements ClientModInitializer {
    private static final DustParticleEffect BLOOD = new DustParticleEffect(0x8B0000, 1.35f);
    private static final DustParticleEffect DARK = new DustParticleEffect(0x300000, 1.55f);
    private static KeyBinding finisherKey;

    // Level Up beat map, detected at about 123.05 BPM. Values are client ticks from song start.
    private static final int[] BEATS = {
            1,11,21,31,41,51,60,70,80,90,100,110,119,129,139,149,159,169,
            178,188,198,208,218,228,238,248,257,267,277,287,297,306
    };

    private static UUID lastCombatTarget;
    private static int combatMemoryTicks;
    private static boolean finisherActive;
    private static int finisherTick;
    private static Vec3d finisherOrigin = Vec3d.ZERO;
    private static final List<SwordVisual> swords = new ArrayList<>();
    private static ArmorStandEntity headVisual;
    private static int headTick;
    private static volatile Player musicPlayer;

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
            return;
        }

        if (combatMemoryTicks > 0) combatMemoryTicks--;
        if (client.options.attackKey.isPressed() && client.targetedEntity instanceof PlayerEntity target && target != self && self.distanceTo(target) < 5.0f) {
            lastCombatTarget = target.getUuid();
            combatMemoryTicks = 50;
        }

        // Automatic trigger when the recently attacked player dies.
        if (!finisherActive && lastCombatTarget != null && combatMemoryTicks > 0) {
            PlayerEntity target = client.world.getPlayerByUuid(lastCombatTarget);
            if (target != null && (!target.isAlive() || target.getHealth() <= 0.0f)) {
                startFinisher(client, self, new Vec3d(target.getX(), target.getY(), target.getZ()));
                lastCombatTarget = null;
                combatMemoryTicks = 0;
            }
        }

        // H is a manual preview/test on the player you are looking at.
        while (finisherKey.wasPressed()) {
            if (client.targetedEntity instanceof PlayerEntity target && target != self && self.distanceTo(target) < 8.0f) {
                startFinisher(client, self, new Vec3d(target.getX(), target.getY(), target.getZ()));
            }
        }

        if (finisherActive) tickFinisher(client, self);
        tickSwordVisuals();
        tickHeadVisual();
    }

    private static void startFinisher(MinecraftClient client, PlayerEntity self, Vec3d origin) {
        clearVisuals();
        finisherActive = true;
        finisherTick = 0;
        finisherOrigin = origin;
        self.swingHand(Hand.MAIN_HAND);
        self.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.65f, 0.72f);
        startMusic();
        bloodBurst(client, origin.add(0, 0.95, 0), 28);
    }

    private static void tickFinisher(MinecraftClient client, PlayerEntity self) {
        finisherTick++;

        // User asked not to move during the sequence: suppress movement keys locally.
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sneakKey.setPressed(false);

        for (int i = 0; i < BEATS.length; i++) {
            if (finisherTick == BEATS[i]) {
                beatStab(client, self, i);
                break;
            }
        }

        if (finisherTick == BEATS[BEATS.length - 1] + 2) {
            decapitationEffect(client);
        }

        if (finisherTick > BEATS[BEATS.length - 1] + 50) {
            finisherActive = false;
        }
    }

    private static void beatStab(MinecraftClient client, PlayerEntity self, int beatIndex) {
        double a = beatIndex * 2.3999632297;
        double radius = 0.72 + (beatIndex % 3) * 0.10;
        double y = finisherOrigin.y + 0.45 + (beatIndex % 4) * 0.27;
        double x = finisherOrigin.x + Math.cos(a) * radius;
        double z = finisherOrigin.z + Math.sin(a) * radius;

        ArmorStandEntity stand = new ArmorStandEntity(client.world, x, y, z);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setShowArms(true);
        stand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(selectSword(beatIndex)));
        stand.setYaw((float)Math.toDegrees(Math.atan2(finisherOrigin.z - z, finisherOrigin.x - x)) - 90f);
        stand.setRightArmRotation(new EulerAngle(-82f + (beatIndex % 3) * 8f, 0f, 0f));
        client.world.addEntity(stand);
        swords.add(new SwordVisual(stand, 90));

        self.swingHand((beatIndex & 1) == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND);
        self.playSound((beatIndex % 4 == 0) ? SoundEvents.ENTITY_PLAYER_ATTACK_CRIT : SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                0.42f, 0.72f + (beatIndex % 5) * 0.055f);
        bloodBurst(client, new Vec3d(finisherOrigin.x, y, finisherOrigin.z), beatIndex % 4 == 0 ? 34 : 18);
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

    private static void startMusic() {
        try {
            Player old = musicPlayer;
            if (old != null) old.close();
        } catch (Exception ignored) {}

        Thread t = new Thread(() -> {
            try (InputStream in = BrutalFinisherClient.class.getResourceAsStream("/assets/arrowrip/levelup.mp3")) {
                if (in == null) return;
                Player p = new Player(in);
                musicPlayer = p;
                p.play();
            } catch (Exception ignored) {
            } finally {
                musicPlayer = null;
            }
        }, "ArrowRip-LevelUp-Music");
        t.setDaemon(true);
        t.start();
    }

    private static void clearVisuals() {
        for (SwordVisual s : swords) if (s.entity != null) s.entity.discard();
        swords.clear();
        if (headVisual != null) headVisual.discard();
        headVisual = null;
        headTick = 0;
    }

    private static final class SwordVisual {
        final ArmorStandEntity entity;
        int life;
        SwordVisual(ArmorStandEntity entity, int life) { this.entity = entity; this.life = life; }
    }
}
