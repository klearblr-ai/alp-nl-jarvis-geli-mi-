package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public final class JapaneseVoiceClient implements ClientModInitializer {
    private static final String[] LINES = {
            "Nani?!",
            "Yamero!",
            "Ikuzo!",
            "Yare yare...",
            "Mada mada!",
            "Sugoi!",
            "Nanda?!",
            "Kamuda!",
            "Omae wa mou shindeiru!",
            "Koko de owari da!",
            "Ore wa mada tomaranai!",
            "Kore ga ore no chikara da!",
            "Kisama... koko made da!",
            "Mada owatte nai zo!",
            "Zetsubou shiro... kore de saigo da!",
            "Ore no subete o misete yaru!"
    };

    private static final String[] SOUNDS = {
            "voice_nani", "voice_yamero", "voice_ikuzo", "voice_yareyare",
            "voice_madamada", "voice_sugoi", "voice_nanda", "voice_kamuda",
            "voice_omae_shindeiru", "voice_koko_owari", "voice_ore_tomaranai", "voice_kore_chikara",
            "voice_kisama_koko", "voice_mada_owatte", "voice_zetsubou_saigo", "voice_subete_misete"
    };

    private static KeyBinding voiceKey;
    private static KeyBinding chatModeKey;
    private static int lastIndex = -1;
    private static boolean chatMode = false;
    private static String subtitle = "";
    private static String subtitleTarget = "";
    private static int subtitleTicks;

    @Override
    public void onInitializeClient() {
        voiceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.random_japanese_voice", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, ArrowRipClient.CATEGORY));
        chatModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.voice_chat_mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (voiceKey.wasPressed()) playRandomVoice(client);
            while (chatModeKey.wasPressed()) {
                chatMode = !chatMode;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Anime local chat: " + (chatMode ? "AÇIK" : "KAPALI")), true);
                }
            }
            if (subtitleTicks > 0) subtitleTicks--;
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> renderSubtitle(ctx));
    }

    public static void playRandomVoice(MinecraftClient client) {
        if (client == null || client.player == null) return;

        int idx;
        if (LINES.length <= 1) idx = 0;
        else {
            do idx = ThreadLocalRandom.current().nextInt(LINES.length);
            while (idx == lastIndex);
        }
        lastIndex = idx;

        subtitle = LINES[idx];
        subtitleTicks = subtitle.length() > 24 ? 122 : 84;

        PlayerEntity target = null;
        if (client.targetedEntity instanceof PlayerEntity looked && looked != client.player) {
            target = looked;
        }

        // Bi o, bi sen: hedef varsa %50 hedef, %50 sen. Hedef yoksa hep sen.
        boolean targetSpeaks = target != null && ThreadLocalRandom.current().nextBoolean();
        PlayerEntity speaker = targetSpeaks ? target : client.player;
        subtitleTarget = speaker.getName().getString();

        if (chatMode && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal("[" + subtitleTarget + "] " + subtitle));
        }

        try {
            Identifier id = Identifier.of("arrowrip", SOUNDS[idx]);
            SoundEvent event = SoundEvent.of(id);
            client.getSoundManager().play(new EntityTrackingSoundInstance(
                    event, SoundCategory.MASTER, 1.0f, 0.90f, speaker, System.nanoTime()));
        } catch (Throwable ignored) {
        }
    }

    private static void renderSubtitle(DrawContext ctx) {
        if (subtitleTicks <= 0 || subtitle.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int y = h - 58;

        int alpha = Math.min(255, subtitleTicks * 10);
        int textColor = (alpha << 24) | 0x00FFFFFF;
        int accentColor = (alpha << 24) | 0x00FF3B3B;
        int bgAlpha = Math.min(180, alpha * 2 / 3);

        String[] parts = splitSubtitle(subtitle);
        int maxWidth = 0;
        for (String p : parts) maxWidth = Math.max(maxWidth, mc.textRenderer.getWidth(p));
        if (!subtitleTarget.isEmpty()) maxWidth = Math.max(maxWidth, mc.textRenderer.getWidth(subtitleTarget + " konuşuyor"));

        int x1 = Math.max(6, w / 2 - maxWidth / 2 - 10);
        int x2 = Math.min(w - 6, w / 2 + maxWidth / 2 + 10);
        int boxTop = y - (parts.length * 12) - (subtitleTarget.isEmpty() ? 6 : 18);
        ctx.fill(x1, boxTop, x2, y + 8, (bgAlpha << 24));
        ctx.fill(x1, boxTop, x1 + 2, y + 8, accentColor);

        int drawY = boxTop + 5;
        if (!subtitleTarget.isEmpty()) {
            String target = subtitleTarget + " konuşuyor";
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(target), (w - mc.textRenderer.getWidth(target)) / 2, drawY, accentColor);
            drawY += 12;
        }
        for (String p : parts) {
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(p), (w - mc.textRenderer.getWidth(p)) / 2, drawY, textColor);
            drawY += 12;
        }
    }

    private static String[] splitSubtitle(String s) {
        if (s.length() <= 34) return new String[]{s};
        int mid = s.length() / 2;
        int left = s.lastIndexOf(' ', mid);
        int right = s.indexOf(' ', mid + 1);
        int cut;
        if (left < 0) cut = right;
        else if (right < 0) cut = left;
        else cut = (mid - left <= right - mid) ? left : right;
        if (cut <= 0 || cut >= s.length() - 1) return new String[]{s};
        return new String[]{s.substring(0, cut).trim(), s.substring(cut + 1).trim()};
    }
}
