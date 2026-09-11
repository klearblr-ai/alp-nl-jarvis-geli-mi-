package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public final class BloodBendingClient implements ClientModInitializer {
    private static final DustParticleEffect BLOOD = new DustParticleEffect(0x8A0008, 1.30f);
    private static final DustParticleEffect DARK = new DustParticleEffect(0x340000, 1.55f);
    private static final DustParticleEffect BRIGHT = new DustParticleEffect(0xD10A12, 0.95f);

    private static KeyBinding bendKey, throwKey;
    private static UUID targetUuid;
    private static int bendTicks;
    private static float originalYaw, originalPitch;
    private static boolean poseSaved;
    private static int throwTicks;
    private static Vec3d throwDirection = new Vec3d(0,0,1);
    private static int passiveBleedCooldown;

    @Override
    public void onInitializeClient() {
        bendKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.blood_bend", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, ArrowRipClient.CATEGORY));
        throwKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.blood_throw", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, ArrowRipClient.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(BloodBendingClient::tick);
    }

    private static void tick(MinecraftClient client) {
        PlayerEntity caster = client.player;
        if (caster == null || client.world == null) {
            clear(null);
            throwTicks = 0;
            return;
        }

        if (passiveBleedCooldown > 0) passiveBleedCooldown--;
        tickLowHealthBleeding(client, caster);

        while (bendKey.wasPressed()) startBend(client, caster);
        while (throwKey.wasPressed()) startBloodThrow(client, caster);
        if (throwTicks > 0) tickBloodThrow(client, caster);

        if (bendTicks <= 0 || targetUuid == null) return;

        Entity e = client.world.getPlayerByUuid(targetUuid);
        if (!(e instanceof PlayerEntity target) || !target.isAlive() || caster.distanceTo(target) > 10.0f) {
            clear(targetOrNull(e));
            return;
        }

        bendTicks--;
        double phase = (100 - bendTicks) * 0.34;
        target.setYaw(originalYaw + (float)Math.sin(phase * 0.72) * 19.0f);
        target.setPitch(originalPitch + (float)Math.sin(phase) * 16.0f);

        if (bendTicks % 8 == 0) {
            caster.swingHand((bendTicks / 8) % 2 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND);
            target.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL, 0.14f, 0.46f);
        }
        if (bendTicks == 78 || bendTicks == 50 || bendTicks == 24) {
            target.playSound(SoundEvents.ENTITY_PLAYER_HURT, 0.22f, 0.68f);
            burst(client, target, 22);
        }

        orbitBlood(client, target, phase);
        bloodTethers(client, caster, target, phase);
        bloodLift(client, target, phase);

        if (bendTicks == 1) {
            burst(client, target, 34);
            clear(target);
        }
    }

    private static void tickLowHealthBleeding(MinecraftClient c, PlayerEntity p) {
        float ratio = p.getHealth() / Math.max(1.0f, p.getMaxHealth());
        if (ratio > 0.55f || passiveBleedCooldown > 0) return;

        Vec3d look = p.getRotationVec(1.0f).normalize();
        Vec3d right = new Vec3d(-look.z, 0, look.x);
        if (right.lengthSquared() < 0.001) right = new Vec3d(1,0,0);
        right = right.normalize();

        Vec3d face = new Vec3d(p.getX(), p.getEyeY() - 0.08, p.getZ()).add(look.multiply(0.10));
        Vec3d leftEye = face.add(right.multiply(-0.055));
        Vec3d rightEye = face.add(right.multiply(0.055));

        int eyeDrops = ratio <= 0.25f ? 5 : 2;
        spawnEyeBleed(c, leftEye, eyeDrops);
        spawnEyeBleed(c, rightEye, eyeDrops);

        if (ratio <= 0.35f) {
            Vec3d mouth = new Vec3d(p.getX(), p.getEyeY() - 0.26, p.getZ()).add(look.multiply(0.13));
            int mouthCount = ratio <= 0.18f ? 18 : 8;
            for (int i=0;i<mouthCount;i++) {
                double speed = 0.035 + c.world.random.nextDouble() * (ratio <= 0.18f ? 0.10 : 0.055);
                c.world.addParticleClient(i%4==0?DARK:BLOOD,
                        mouth.x + (c.world.random.nextDouble()-.5)*.045,
                        mouth.y + (c.world.random.nextDouble()-.5)*.035,
                        mouth.z + (c.world.random.nextDouble()-.5)*.045,
                        look.x*speed + (c.world.random.nextDouble()-.5)*.025,
                        -0.018 + (c.world.random.nextDouble()-.5)*.018,
                        look.z*speed + (c.world.random.nextDouble()-.5)*.025);
            }
            if (ratio <= 0.18f) p.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL,0.12f,0.48f);
        }

        passiveBleedCooldown = ratio <= 0.18f ? 2 : (ratio <= 0.35f ? 4 : 7);
    }

    private static void spawnEyeBleed(MinecraftClient c, Vec3d eye, int n) {
        for (int i=0;i<n;i++) {
            c.world.addParticleClient(i%3==0?DARK:BLOOD,
                    eye.x + (c.world.random.nextDouble()-.5)*.018,
                    eye.y,
                    eye.z + (c.world.random.nextDouble()-.5)*.018,
                    (c.world.random.nextDouble()-.5)*.006,
                    -0.045-c.world.random.nextDouble()*.035,
                    (c.world.random.nextDouble()-.5)*.006);
        }
    }

    private static void startBloodThrow(MinecraftClient c, PlayerEntity caster) {
        throwTicks = 18;
        throwDirection = caster.getRotationVec(1.0f).normalize();
        caster.swingHand(Hand.MAIN_HAND);
        caster.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL,0.30f,0.62f);
        bloodThrowBurst(c,caster,24);
    }

    private static void tickBloodThrow(MinecraftClient c, PlayerEntity caster) {
        throwTicks--;
        Vec3d origin = new Vec3d(caster.getX(), caster.getEyeY()-0.22, caster.getZ()).add(throwDirection.multiply(0.22));
        double age = 18 - throwTicks;
        double max = 1.8 + age * 0.30;
        for (int i=0;i<18;i++) {
            double d = 0.25 + (max-0.25) * (i/17.0);
            double wave = Math.sin(age*0.6+i*0.75)*0.045;
            Vec3d q = origin.add(throwDirection.multiply(d));
            c.world.addParticleClient(i%5==0?BRIGHT:(i%4==0?DARK:BLOOD),
                    q.x + wave,
                    q.y + Math.cos(i*0.7+age)*0.035,
                    q.z - wave,
                    throwDirection.x*.055,
                    throwDirection.y*.055 - .01,
                    throwDirection.z*.055);
        }
        if (throwTicks % 4 == 0) caster.swingHand(Hand.MAIN_HAND);
        if (throwTicks == 1 && c.targetedEntity instanceof PlayerEntity t && caster.distanceTo(t) < 7.0f) {
            burst(c,t,30);
            t.playSound(SoundEvents.ENTITY_PLAYER_HURT,0.20f,0.75f);
        }
    }

    private static void bloodThrowBurst(MinecraftClient c, PlayerEntity p, int n) {
        Vec3d look = p.getRotationVec(1f).normalize();
        Vec3d o = new Vec3d(p.getX(), p.getEyeY()-0.22, p.getZ()).add(look.multiply(.18));
        for(int i=0;i<n;i++) {
            double s=.04+c.world.random.nextDouble()*.10;
            c.world.addParticleClient(i%4==0?DARK:BLOOD,
                    o.x+(c.world.random.nextDouble()-.5)*.10,
                    o.y+(c.world.random.nextDouble()-.5)*.10,
                    o.z+(c.world.random.nextDouble()-.5)*.10,
                    look.x*s+(c.world.random.nextDouble()-.5)*.04,
                    look.y*s+(c.world.random.nextDouble()-.5)*.04,
                    look.z*s+(c.world.random.nextDouble()-.5)*.04);
        }
    }

    private static void startBend(MinecraftClient client, PlayerEntity caster) {
        if (!(client.targetedEntity instanceof PlayerEntity target) || target == caster || caster.distanceTo(target) > 8.0f) return;
        if (targetUuid != null) {
            Entity old = client.world.getPlayerByUuid(targetUuid);
            clear(targetOrNull(old));
        }
        targetUuid = target.getUuid();
        bendTicks = 100;
        originalYaw = target.getYaw();
        originalPitch = target.getPitch();
        poseSaved = true;
        caster.swingHand(Hand.MAIN_HAND);
        caster.swingHand(Hand.OFF_HAND);
        target.playSound(SoundEvents.ENTITY_PLAYER_HURT, 0.30f, 0.56f);
        burst(client, target, 42);
    }

    private static PlayerEntity targetOrNull(Entity e) { return e instanceof PlayerEntity p ? p : null; }

    private static void orbitBlood(MinecraftClient c, PlayerEntity t, double phase) {
        for (int ring = 0; ring < 3; ring++) {
            double y = t.getY() + 0.42 + ring * 0.48;
            double radius = 0.40 + ring * 0.08;
            for (int i = 0; i < 5; i++) {
                double a = phase + ring * 1.1 + i * (Math.PI * 2.0 / 5.0);
                double x = t.getX() + Math.cos(a) * radius;
                double z = t.getZ() + Math.sin(a) * radius;
                c.world.addParticleClient((i + ring) % 3 == 0 ? DARK : BLOOD,
                        x, y + Math.sin(a * 1.8) * 0.08, z,
                        -Math.sin(a) * 0.025, 0.012, Math.cos(a) * 0.025);
            }
        }
    }

    private static void bloodTethers(MinecraftClient c, PlayerEntity caster, PlayerEntity target, double phase) {
        Vec3d from = new Vec3d(caster.getX(), caster.getEyeY() - 0.38, caster.getZ());
        Vec3d to = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.56, target.getZ());
        Vec3d d = to.subtract(from);
        for (int i = 1; i <= 12; i++) {
            double f = i / 13.0;
            Vec3d q = from.add(d.multiply(f));
            double wave = Math.sin(phase * 1.4 + i * 0.8) * 0.09;
            c.world.addParticleClient(i % 4 == 0 ? BRIGHT : BLOOD,
                    q.x + wave, q.y + Math.cos(phase + i) * 0.045, q.z - wave,
                    d.x * 0.006, d.y * 0.006, d.z * 0.006);
        }
    }

    private static void bloodLift(MinecraftClient c, PlayerEntity t, double phase) {
        for (int i = 0; i < 5; i++) {
            double a = phase * 0.6 + i * 1.26;
            double r = 0.16 + i * 0.025;
            c.world.addParticleClient(i % 2 == 0 ? DARK : BLOOD,
                    t.getX() + Math.cos(a) * r,
                    t.getY() + 0.06 + (i * 0.055),
                    t.getZ() + Math.sin(a) * r,
                    0, 0.075 + i * 0.006, 0);
        }
    }

    private static void burst(MinecraftClient c, PlayerEntity t, int n) {
        double y = t.getY() + t.getHeight() * 0.55;
        for (int i = 0; i < n; i++) {
            double a = c.world.random.nextDouble() * Math.PI * 2.0;
            double s = 0.025 + c.world.random.nextDouble() * 0.09;
            c.world.addParticleClient(i % 4 == 0 ? DARK : (i % 5 == 0 ? BRIGHT : BLOOD),
                    t.getX() + (c.world.random.nextDouble() - .5) * .28,
                    y + (c.world.random.nextDouble() - .5) * .55,
                    t.getZ() + (c.world.random.nextDouble() - .5) * .28,
                    Math.cos(a) * s,
                    (c.world.random.nextDouble() - .25) * .08,
                    Math.sin(a) * s);
        }
    }

    private static void clear(PlayerEntity target) {
        if (poseSaved && target != null) {
            target.setYaw(originalYaw);
            target.setPitch(originalPitch);
        }
        targetUuid = null;
        bendTicks = 0;
        poseSaved = false;
    }
}
