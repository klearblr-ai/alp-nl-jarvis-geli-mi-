package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.concurrent.ThreadLocalRandom;

/** Automatic deep English battle shouts for AUTO PLAY FULL BODY mode. */
public final class EnglishBattleVoiceClient implements ClientModInitializer {
    private static final String[] LINES = {
            "Come on!",
            "Fight me!",
            "I'm coming for you!",
            "You're done!",
            "Fuck, man!",
            "Get up!",
            "Come on, fight me!",
            "I'll kill you!",
            "Let's end this!",
            "Try me!",
            "Fuck it, come on!",
            "You want a fight? Come on!",
            "Kill you!",
            "Murder!",
            "Kill! I'm coming!",
            "Fuck you!",
            "Fuck up!",
            "You're fucking done!",
            "I'm gonna end you!",
            "Come here!",
            "Don't run!",
            "Stand and fight!",
            "You're mine!",
            "This ends now!"
    };

    private static final String[] SOUNDS = {
            "voice_en_00", "voice_en_01", "voice_en_02", "voice_en_03",
            "voice_en_04", "voice_en_05", "voice_en_06", "voice_en_07",
            "voice_en_08", "voice_en_09", "voice_en_10", "voice_en_11",
            "voice_en_12", "voice_en_13", "voice_en_14", "voice_en_15",
            "voice_en_16", "voice_en_17", "voice_en_18", "voice_en_19",
            "voice_en_20", "voice_en_21", "voice_en_22", "voice_en_23"
    };

    private static String subtitle = "";
    private static int subtitleTicks;
    private static int last = -1;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (subtitleTicks > 0) subtitleTicks--;
        });
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
    }

    public static void playRandom(MinecraftClient client) {
        if (client == null || client.player == null) return;
        int idx;
        do idx = ThreadLocalRandom.current().nextInt(LINES.length);
        while (idx == last && LINES.length > 1);
        last = idx;

        PlayerEntity speaker = client.player;
        subtitle = LINES[idx];
        subtitleTicks = 70;
        AnimeAnimationClient.triggerSpeechEmote(3000 + idx, speaker.getId(), subtitleTicks);

        try {
            SoundEvent event = SoundEvent.of(Identifier.of("arrowrip", SOUNDS[idx]));
            client.getSoundManager().play(new EntityTrackingSoundInstance(
                    event, SoundCategory.MASTER, 1.0F, 0.92F, speaker, System.nanoTime()));
        } catch (Throwable ignored) {}
    }

    private static void render(DrawContext ctx) {
        if (subtitleTicks <= 0 || subtitle.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int tw = mc.textRenderer.getWidth(subtitle);
        int x = (w - tw) / 2;
        int y = h - 82;
        int alpha = Math.min(255, subtitleTicks * 8);
        ctx.fill(x - 8, y - 5, x + tw + 8, y + 13, (Math.min(150, alpha) << 24));
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(subtitle), x, y, (alpha << 24) | 0x00FFFFFF);
    }
}
