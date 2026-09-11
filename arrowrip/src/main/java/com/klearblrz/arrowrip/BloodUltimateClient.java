package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class BloodUltimateClient implements ClientModInitializer {
    private static final Identifier SCREAM = Identifier.of("arrowrip", "ultimate_scream");
    private static KeyBinding ultimateKey;
    private static int ultimateTicks;
    private static int cooldown;

    @Override
    public void onInitializeClient() {
        ultimateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.blood_ultimate", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (cooldown > 0) cooldown--;
            if (ultimateTicks > 0) ultimateTicks--;

            while (ultimateKey.wasPressed()) {
                if (client.player == null || client.world == null || cooldown > 0) continue;

                // B artık ulti tuşu. Eski yakın-menzil bite'ın aynı basışta tetiklenmesini engelle.
                client.targetedEntity = null;
                ultimateTicks = 74;
                cooldown = 90;
                client.player.sendMessage(Text.literal("KAN ULTİ ⚡"), true);

                try {
                    client.getSoundManager().play(PositionedSoundInstance.master(
                            SoundEvent.of(SCREAM), 0.82f, 1.25f));
                } catch (Throwable ignored) {}
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            if (ultimateTicks <= 0) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();
            int age = 74 - ultimateTicks;

            // Particle yok: sadece GPU ucuz 2D kan/aura katmanı.
            float attack = Math.min(1.0f, age / 8.0f);
            float release = Math.min(1.0f, ultimateTicks / 24.0f);
            float strength = attack * release;
            int alpha = Math.max(0, Math.min(150, (int)(118 * strength)));
            int red = (alpha << 24) | 0x760000;

            int edge = Math.max(14, (int)(42 * strength));
            ctx.fill(0, 0, w, edge, red);
            ctx.fill(0, h - edge, w, h, red);
            ctx.fill(0, edge, edge, h - edge, red);
            ctx.fill(w - edge, edge, w, h - edge, red);

            // Anime kan kesikleri / enerji çizgileri; sabit geometri, particle değil.
            int phase = age % 18;
            int slashAlpha = Math.max(0, 190 - phase * 9);
            int slash = (slashAlpha << 24) | 0xE00018;
            int y1 = h / 2 - 34 + phase;
            int y2 = h / 2 + 28 - phase / 2;
            ctx.fill(w / 5, y1, w - w / 7, y1 + 3, slash);
            ctx.fill(w / 8, y2, w - w / 4, y2 + 2, slash);

            if (age < 5) {
                int flashA = 150 - age * 28;
                ctx.fill(0, 0, w, h, (flashA << 24) | 0xFF2020);
            }
        });
    }
}
