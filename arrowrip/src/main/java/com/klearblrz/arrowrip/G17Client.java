package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/** Client-side G17-style visual gun layer for the bow. Server still sees a vanilla bow. */
public final class G17Client implements ClientModInitializer {
    private static final Identifier SHOT = Identifier.of("arrowrip", "g17_shot");
    private static final Identifier RACK = Identifier.of("arrowrip", "g17_rack");

    private static boolean wasUsingBow;
    private static int drawTicks;
    private static int recoilTicks;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(G17Client::tick);
    }

    private static void tick(MinecraftClient client) {
        if (recoilTicks > 0) recoilTicks--;

        PlayerEntity player = client.player;
        if (player == null || client.world == null) {
            wasUsingBow = false;
            drawTicks = 0;
            recoilTicks = 0;
            return;
        }

        boolean usingBow = player.isUsingItem()
                && player.getActiveItem().isOf(Items.BOW)
                && player.getMainHandStack().isOf(Items.BOW);

        if (usingBow) {
            drawTicks++;
            if (!wasUsingBow) play(client, RACK, 1.08F, 0.48F);
        } else if (wasUsingBow) {
            if (drawTicks >= 4) fireVisual(client, player);
            drawTicks = 0;
        }

        wasUsingBow = usingBow;
    }

    private static void fireVisual(MinecraftClient client, PlayerEntity p) {
        recoilTicks = 7;
        p.swingHand(Hand.MAIN_HAND);
        play(client, SHOT, 0.96F, 1.0F);

        Vec3d look = p.getRotationVec(1.0F).normalize();
        Vec3d start = new Vec3d(p.getX(), p.getEyeY() - 0.10, p.getZ()).add(look.multiply(0.42));

        // Short muzzle flash/tracer only. Projectile remains the real server-side arrow.
        for (int i = 0; i < 13; i++) {
            double d = 0.18 + i * 0.22;
            Vec3d q = start.add(look.multiply(d));
            client.world.addParticleClient(ParticleTypes.CRIT, q.x, q.y, q.z,
                    look.x * 0.015, look.y * 0.015, look.z * 0.015);
        }
        for (int i = 0; i < 7; i++) {
            Vec3d q = start.add(look.multiply(0.08 + i * 0.025));
            client.world.addParticleClient(ParticleTypes.SMOKE, q.x, q.y, q.z,
                    (client.world.random.nextDouble() - 0.5) * 0.018,
                    0.012 + client.world.random.nextDouble() * 0.018,
                    (client.world.random.nextDouble() - 0.5) * 0.018);
        }
    }

    private static void play(MinecraftClient client, Identifier id, float pitch, float volume) {
        try {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvent.of(id), pitch, volume));
        } catch (Throwable ignored) {}
    }

    public static boolean isGunItem(ItemStack stack) {
        return stack.isOf(Items.BOW);
    }

    public static int recoilTicks() { return recoilTicks; }
}
