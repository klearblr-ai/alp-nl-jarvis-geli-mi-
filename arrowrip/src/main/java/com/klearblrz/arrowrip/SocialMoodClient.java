package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Session-only client personality layer.
 * Tracks bond/anger toward nearby players without sending autonomous server chat.
 */
public final class SocialMoodClient implements ClientModInitializer {
    private static final Map<UUID, Mood> MOODS = new HashMap<>();
    private static final Set<Integer> SEEN_GIFT_ITEMS = new HashSet<>();

    private static float lastHealth = -1.0F;
    private static boolean wasAlive = true;
    private static int tickCounter;
    private static int thoughtCooldown;
    private static UUID focusUuid;
    private static int focusTicks;

    private static final String[] LOW_RAGE = {
            "%s, don't push it.",
            "%s... sakin ol.",
            "I'm watching you, %s.",
            "%s, not funny."
    };
    private static final String[] MID_RAGE = {
            "%s, one more hit and we're fighting.",
            "Don't test me, %s.",
            "%s, you're getting on my nerves.",
            "Back off, %s."
    };
    private static final String[] HIGH_RAGE = {
            "%s... I'll take you down.",
            "You asked for this, %s.",
            "%s, this ends now.",
            "Come on then, %s."
    };
    private static final String[] MAX_RAGE = {
            "%s... I'll kill you.",
            "You're dead to me, %s.",
            "%s, I won't forget this.",
            "No more warnings, %s."
    };
    private static final String[] GIFT_LINES = {
            "Thanks, %s. I remember that.",
            "%s, we're good.",
            "Respect + %s.",
            "%s... okay, I like you more now."
    };

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(SocialMoodClient::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> renderHud(ctx));
    }

    private static void tick(MinecraftClient client) {
        PlayerEntity self = client.player;
        if (self == null || client.world == null) {
            lastHealth = -1.0F;
            wasAlive = true;
            tickCounter = 0;
            thoughtCooldown = 0;
            focusTicks = 0;
            focusUuid = null;
            SEEN_GIFT_ITEMS.clear();
            MOODS.clear();
            return;
        }

        tickCounter++;
        if (thoughtCooldown > 0) thoughtCooldown--;
        if (focusTicks > 0) focusTicks--;

        if ((tickCounter & 1) == 0) scanForGiftItems(client, self);

        float hp = self.getHealth();
        if (lastHealth >= 0.0F && hp < lastHealth - 0.01F && hp > 0.0F) {
            Entity attacker = self.getAttacker();
            if (attacker instanceof PlayerEntity player && player != self) {
                float damage = Math.max(0.5F, lastHealth - hp);
                addAnger(client, player, 14.0F + damage * 3.2F, "hit");
            }
        }

        boolean alive = self.isAlive();
        if (wasAlive && !alive) {
            Entity attacker = self.getAttacker();
            if (attacker instanceof PlayerEntity player && player != self) {
                addAnger(client, player, 46.0F, "killed");
                sayLocal(client, player.getName().getString() + "... you killed me. I remember that.", true);
            }
        }
        wasAlive = alive;
        lastHealth = hp;

        if (tickCounter % 20 == 0) {
            for (Mood mood : MOODS.values()) {
                if (mood.calmTicks > 0) mood.calmTicks -= 20;
                float calm = 0.22F + mood.bond * 0.0040F;
                if (mood.calmTicks > 0) calm += 0.32F;
                mood.anger = Math.max(0.0F, mood.anger - calm);
                mood.bond = Math.max(0.0F, mood.bond - 0.018F);
            }
        }

        if (SEEN_GIFT_ITEMS.size() > 768) SEEN_GIFT_ITEMS.clear();
    }

    private static void scanForGiftItems(MinecraftClient client, PlayerEntity self) {
        for (ItemEntity item : client.world.getEntitiesByClass(
                ItemEntity.class,
                self.getBoundingBox().expand(1.55),
                e -> e.isAlive() && !e.getStack().isEmpty())) {
            Entity owner = item.getOwner();
            if (!(owner instanceof PlayerEntity giver) || giver == self) continue;
            if (self.distanceTo(item) > 1.55F) continue;
            if (!SEEN_GIFT_ITEMS.add(item.getId())) continue;

            Mood mood = mood(giver);
            int count = Math.max(1, item.getStack().getCount());
            float gain = Math.min(20.0F, 9.0F + count * 0.65F);
            mood.bond = MathHelper.clamp(mood.bond + gain, 0.0F, 100.0F);
            mood.anger = Math.max(0.0F, mood.anger - (10.0F + gain * 0.70F));
            mood.calmTicks = Math.max(mood.calmTicks, 20 * 55);
            focus(giver);

            String name = giver.getName().getString();
            String line = String.format(Locale.ROOT,
                    GIFT_LINES[ThreadLocalRandom.current().nextInt(GIFT_LINES.length)], name);
            sayLocal(client, line + "  [BOND " + Math.round(mood.bond) + "]", false);
        }
    }

    private static Mood mood(PlayerEntity player) {
        return MOODS.computeIfAbsent(player.getUuid(), id -> new Mood(player.getName().getString()));
    }

    private static void addAnger(MinecraftClient client, PlayerEntity player, float raw, String reason) {
        Mood mood = mood(player);
        float resistance = Math.min(0.58F, mood.bond * 0.0058F);
        float gain = raw * (1.0F - resistance);
        if (mood.calmTicks > 0) gain *= 0.68F;

        int oldStage = stage(mood.anger);
        mood.anger = MathHelper.clamp(mood.anger + gain, 0.0F, 100.0F);
        mood.bond = Math.max(0.0F, mood.bond - raw * 0.08F);
        int newStage = stage(mood.anger);
        focus(player);

        if (thoughtCooldown <= 0 || newStage > oldStage || "killed".equals(reason)) {
            String line = rageLine(player.getName().getString(), mood.anger);
            sayLocal(client, line + "  [ANGER " + Math.round(mood.anger) + "]", mood.anger >= 55.0F);
            thoughtCooldown = mood.anger >= 70.0F ? 65 : 100;

            if (mood.anger >= 72.0F && ThreadLocalRandom.current().nextInt(100) < 48) {
                EnglishBattleVoiceClient.playRandom(client);
            }
        }
    }

    private static String rageLine(String name, float anger) {
        String[] pool = anger >= 86.0F ? MAX_RAGE : anger >= 62.0F ? HIGH_RAGE : anger >= 34.0F ? MID_RAGE : LOW_RAGE;
        return String.format(Locale.ROOT, pool[ThreadLocalRandom.current().nextInt(pool.length)], name);
    }

    private static int stage(float anger) {
        if (anger >= 86.0F) return 4;
        if (anger >= 62.0F) return 3;
        if (anger >= 34.0F) return 2;
        if (anger >= 12.0F) return 1;
        return 0;
    }

    private static void focus(PlayerEntity player) {
        focusUuid = player.getUuid();
        focusTicks = 20 * 8;
    }

    public static void onChatMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || message == null) return;

        String raw = message.getString();
        if (raw == null || raw.isBlank() || raw.startsWith("[ARROW-AI]")) return;
        String lower = raw.toLowerCase(Locale.ROOT);

        PlayerEntity speaker = findSpeaker(client, raw, lower);
        if (speaker == null || speaker == client.player) return;

        if (containsInsult(lower)) {
            String selfName = client.player.getName().getString().toLowerCase(Locale.ROOT);
            boolean directed = lower.contains(selfName) || lower.contains(" you ") || lower.contains(" sana ") || lower.contains(" sen ");
            addAnger(client, speaker, directed ? 20.0F : 10.0F, "chat");
            return;
        }

        if (containsPositive(lower)) {
            Mood mood = mood(speaker);
            mood.bond = MathHelper.clamp(mood.bond + 2.4F, 0.0F, 100.0F);
            mood.anger = Math.max(0.0F, mood.anger - 2.8F);
        }
    }

    private static PlayerEntity findSpeaker(MinecraftClient client, String raw, String lower) {
        PlayerEntity fallback = null;
        for (PlayerEntity p : client.world.getPlayers()) {
            if (p == client.player) continue;
            String name = p.getName().getString();
            String n = name.toLowerCase(Locale.ROOT);
            if (lower.startsWith("<" + n + ">")
                    || lower.startsWith(n + ":")
                    || lower.startsWith("[" + n + "]")
                    || lower.contains("<" + n + ">")) return p;
            if (lower.contains(n)) fallback = p;
        }
        return fallback;
    }

    private static boolean containsInsult(String s) {
        String padded = " " + s.replaceAll("[^a-z0-9çğıöşü]+", " ") + " ";
        String[] words = {
                " fuck ", " fucking ", " bitch ", " shit ", " idiot ", " stupid ", " trash ", " noob ",
                " ez ", " loser ", " moron ", " asshole ", " amk ", " aq ", " orospu ", " siktir ",
                " sik ", " piç ", " pic ", " salak ", " gerizekalı ", " gerizekali ", " mal "
        };
        for (String w : words) if (padded.contains(w)) return true;
        return false;
    }

    private static boolean containsPositive(String s) {
        String padded = " " + s.replaceAll("[^a-z0-9çğıöşü]+", " ") + " ";
        return padded.contains(" gg ") || padded.contains(" thanks ") || padded.contains(" thank you ")
                || padded.contains(" thx ") || padded.contains(" ty ") || padded.contains(" nice ")
                || padded.contains(" eyw ") || padded.contains(" sağol ") || padded.contains(" sagol ");
    }

    private static void sayLocal(MinecraftClient client, String line, boolean angry) {
        if (client.inGameHud == null || client.player == null) return;
        Text text = Text.literal("[ARROW-AI] " + line).formatted(angry ? Formatting.DARK_RED : Formatting.AQUA);
        client.inGameHud.getChatHud().addMessage(text);
        AnimeAnimationClient.triggerSpeechEmote(5000 + ThreadLocalRandom.current().nextInt(40), client.player.getId(), 72);
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        PlayerEntity shown = null;
        if (mc.targetedEntity instanceof PlayerEntity target && target != mc.player) shown = target;
        if (shown == null && focusTicks > 0 && focusUuid != null) {
            Entity e = mc.world.getPlayerByUuid(focusUuid);
            if (e instanceof PlayerEntity p) shown = p;
        }
        if (shown == null) return;

        Mood mood = MOODS.get(shown.getUuid());
        if (mood == null) return;

        String title = "AI MOOD • " + shown.getName().getString();
        String stats = "ANGER " + Math.round(mood.anger) + "   BOND " + Math.round(mood.bond);
        String state = mood.anger >= 86 ? "RAGE" : mood.anger >= 62 ? "HOSTILE" : mood.anger >= 34 ? "ANNOYED"
                : mood.bond >= 55 ? "TRUST" : mood.bond >= 25 ? "FRIENDLY" : "CALM";

        int x = 8;
        int y = 8;
        int width = Math.max(132, Math.max(mc.textRenderer.getWidth(title), mc.textRenderer.getWidth(stats)) + 12);
        ctx.fill(x, y, x + width, y + 38, 0x99000000);
        ctx.drawTextWithShadow(mc.textRenderer, title, x + 6, y + 5, 0xFFFFFFFF);
        ctx.drawTextWithShadow(mc.textRenderer, stats, x + 6, y + 16, 0xFFE0E0E0);
        int stateColor = mood.anger >= 62 ? 0xFFFF5555 : mood.bond >= 40 ? 0xFF55FFFF : 0xFFFFFFFF;
        ctx.drawTextWithShadow(mc.textRenderer, state, x + 6, y + 27, stateColor);
    }

    private static final class Mood {
        final String lastKnownName;
        float anger;
        float bond;
        int calmTicks;

        Mood(String name) {
            this.lastKnownName = name;
        }
    }
}
