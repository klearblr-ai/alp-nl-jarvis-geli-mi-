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

/** Client-side G17-style visual gun layer for the bow/arrow. */
public final class G17Client implements ClientModInitializer {
    private static final Identifier SHOT = Identifier.of("arrowrip", "g17_shot");
    private static boolean lastUse;
    private static int recoilTicks;
    private static int cooldown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(G17Client::tick);
    }

    private static void tick(MinecraftClient client) {
        if (recoilTicks > 0) recoilTicks--;
        if (cooldown > 0) cooldown--;
        PlayerEntity player = client.player;
        if (player == null || client.world == null) {
            lastUse = false;
            recoilTicks = 0;
            cooldown = 0;
            return;
        }

        boolean use = client.options.useKey.isPressed();
        ItemStack held = player.getMainHandStack();
        boolean gunItem = isGunItem(held);

        if (use && !lastUse && gunItem && cooldown <= 0) {
            fireVisual(client, player);
            cooldown = 3;
        }
        lastUse = use;
    }

    private static void fireVisual(MinecraftClient client, PlayerEntity p) {
        recoilTicks = 7;
        p.swingHand(Hand.MAIN_HAND);
        try {
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvent.of(SHOT), 0.96F, 1.0F));
        } catch (Throwable ignored) {}

        Vec3d look = p.getRotationVec(1.0F).normalize();
        Vec3d start = new Vec3d(p.getX(), p.getEyeY() - 0.10, p.getZ()).add(look.multiply(0.36));
        for (int i = 0; i < 34; i++) {
            double d = 0.35 + i * 0.34;
            Vec3d q = start.add(look.multiply(d));
            client.world.addParticleClient(ParticleTypes.CRIT, q.x, q.y, q.z,
                    look.x * 0.02, look.y * 0.02, look.z * 0.02);
        }
        for (int i = 0; i < 8; i++) {
            double d = 0.18 + i * 0.035;
            Vec3d q = start.add(look.multiply(d));
            client.world.addParticleClient(ParticleTypes.SMOKE, q.x, q.y, q.z,
                    (client.world.random.nextDouble() - 0.5) * 0.02,
                    0.01 + client.world.random.nextDouble() * 0.02,
                    (client.world.random.nextDouble() - 0.5) * 0.02);
        }
    }

    public static boolean isGunItem(ItemStack stack) {
        return stack.isOf(Items.BOW) || stack.isOf(Items.ARROW) || stack.isOf(Items.SPECTRAL_ARROW) || stack.isOf(Items.TIPPED_ARROW);
    }

    public static int recoilTicks() { return recoilTicks; }
}
