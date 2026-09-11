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

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class JapaneseVoiceClient implements ClientModInitializer {
    private static final String[] BASE_LINES = {
            "Nani?!", "Yamero!", "Ikuzo!", "Yare yare...", "Mada mada!", "Sugoi!", "Nanda?!", "Kamuda!",
            "Omae wa mou shindeiru!", "Koko de owari da!", "Ore wa mada tomaranai!", "Kore ga ore no chikara da!",
            "Kisama... koko made da!", "Mada owatte nai zo!", "Zetsubou shiro... kore de saigo da!", "Ore no subete o misete yaru!",
            "Ore wa... nanda da... KAMUDAAAAAAAAAAAAAAAAAAA!", "Kore wa... nan da... kono chikara wa... mada owattenaiiiii!",
            "Omae... kikoeru ka... ore no koe ga... KAMUDAAAAAAAAAAAAA!", "Yare yare... koko made ka... iya... mada da... MADA DAAAAAAAA!"
    };

    private static final String[] BASE_SOUNDS = {
            "voice_nani", "voice_yamero", "voice_ikuzo", "voice_yareyare",
            "voice_madamada", "voice_sugoi", "voice_nanda", "voice_kamuda",
            "voice_omae_shindeiru", "voice_koko_owari", "voice_ore_tomaranai", "voice_kore_chikara",
            "voice_kisama_koko", "voice_mada_owatte", "voice_zetsubou_saigo", "voice_subete_misete",
            "voice_ore_nanda_kamuda", "voice_kore_wa_nan_da", "voice_omae_kikoeru", "voice_saigo_bakuhatsu"
    };

    private static final String[] EXTRA_STARTS = {
            "Ore wa", "Omae wa", "Kore wa", "Nanda", "Yare yare", "Mada mada",
            "Kisama", "Ikuzo", "Yamero", "Sugoi", "Koko de", "Zetsubou"
    };
    private static final String[] EXTRA_MIDDLES = {
            "mada tomaranai", "kono chikara", "nan da", "kikoeru ka", "owatte nai", "saigo da",
            "subete o misete yaru", "koko made da", "mou shindeiru", "ore no koe ga", "mada da", "doushite"
    };
    private static final String[] EXTRA_ENDS = {
            "Kamuda", "Nandaaa", "Ikuzooo", "Yamerooo", "Madaaaa", "Chikaraaa",
            "Owari daaa", "Kisamaaa", "Sugoiii", "Zetsubouuu", "Ore waaaa", "Kore daaaa"
    };

    private static final int EXTRA_COUNT = 1031;
    private static final int TOTAL_COUNT = BASE_LINES.length + EXTRA_COUNT;

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
        do idx = ThreadLocalRandom.current().nextInt(TOTAL_COUNT);
        while (idx == lastIndex && TOTAL_COUNT > 1);
        lastIndex = idx;

        subtitle = lineFor(idx);
        subtitleTicks = subtitle.length() > 50 ? 190 : (subtitle.length() > 32 ? 155 : 100);

        PlayerEntity target = null;
        if (client.targetedEntity instanceof PlayerEntity looked && looked != client.player) target = looked;

        boolean targetSpeaks = target != null && ThreadLocalRandom.current().nextBoolean();
        PlayerEntity speaker = targetSpeaks ? target : client.player;
        subtitleTarget = speaker.getName().getString();

        // Every single line, including all 1031 generated ones, starts a moving cinematic emote.
        AnimeAnimationClient.triggerSpeechEmote(idx, speaker.getId(), subtitleTicks);

        if (chatMode && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal("[" + subtitleTarget + "] " + subtitle));
        }

        try {
            Identifier id = Identifier.of("arrowrip", soundFor(idx));
            SoundEvent event = SoundEvent.of(id);
            client.getSoundManager().play(new EntityTrackingSoundInstance(
                    event, SoundCategory.MASTER, 1.0f, 0.88f, speaker, System.nanoTime()));
        } catch (Throwable ignored) {
        }
    }

    private static String lineFor(int idx) {
        if (idx < BASE_LINES.length) return BASE_LINES[idx];
        return buildExtraLine(idx - BASE_LINES.length);
    }

    private static String soundFor(int idx) {
        if (idx < BASE_SOUNDS.length) return BASE_SOUNDS[idx];
        return String.format(Locale.ROOT, "voice_extra_%04d", idx - BASE_LINES.length);
    }

    private static String buildExtraLine(int i) {
        String a = EXTRA_STARTS[i % EXTRA_STARTS.length];
        String b = EXTRA_MIDDLES[(i / EXTRA_STARTS.length) % EXTRA_MIDDLES.length];
        String c = EXTRA_ENDS[(i / (EXTRA_STARTS.length * EXTRA_MIDDLES.length)) % EXTRA_ENDS.length];
        if (i % 17 == 0) c = c.toUpperCase(Locale.ROOT) + "AAAAAAAAAAAA";
        else if (i % 11 == 0) c = c + "aaaaaaa";
        return a + "... " + b + "... " + c + "!";
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
        if (s.length() > 58) {
            int a = s.indexOf(' ', s.length() / 3);
            int b = s.indexOf(' ', (s.length() * 2) / 3);
            if (a > 0 && b > a) return new String[]{s.substring(0,a).trim(), s.substring(a+1,b).trim(), s.substring(b+1).trim()};
        }
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
