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
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Session-only client personality layer.
 * Every nearby player gets independent LOVE (sevgi) and ANGER (sinir) memory.
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
            "%s... enough.",
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
    private static final String[] FRIENDLY_LINES = {
            "%s, tamam... dostça geldin.",
            "%s, selamını aldım.",
            "%s... iyi davranıyorsun.",
            "Respect, %s."
    };

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(SocialMoodClient::tick);
        HudRenderCallback.EVENT.register((ctx, ignored) -> renderHud(ctx));
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

        if ((tickCounter & 1) == 0) {
            scanForGiftItems(client, self);
            scanFriendlyGestures(client, self);
        }

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
                sayLocal(client, player.getName().getString() + "... bunu unutmayacağım.", true);
            }
        }
        wasAlive = alive;
        lastHealth = hp;

        if (tickCounter % 20 == 0) {
            for (Mood mood : MOODS.values()) {
                if (mood.calmTicks > 0) mood.calmTicks -= 20;
                if (mood.gestureCooldown > 0) mood.gestureCooldown -= 20;
                float calm = 0.22F + mood.love * 0.0040F;
                if (mood.calmTicks > 0) calm += 0.32F;
                mood.anger = Math.max(0.0F, mood.anger - calm);
                mood.love = Math.max(0.0F, mood.love - 0.010F);
            }
        }

        if (SEEN_GIFT_ITEMS.size() > 768) SEEN_GIFT_ITEMS.clear();
    }

    private static void scanFriendlyGestures(MinecraftClient client, PlayerEntity self) {
        for (PlayerEntity other : client.world.getPlayers()) {
            if (other == self || !other.isAlive() || self.distanceTo(other) > 4.5F) continue;
            Mood mood = mood(other);

            boolean greeting = other.isSneaking() && isFacing(other, self, 0.42F);
            if (greeting) mood.greetingTicks = Math.min(40, mood.greetingTicks + 2);
            else mood.greetingTicks = Math.max(0, mood.greetingTicks - 2);

            if (mood.greetingTicks >= 12 && mood.gestureCooldown <= 0) {
                float gain = mood.anger >= 60.0F ? 1.5F : (mood.anger >= 30.0F ? 2.5F : 4.5F);
                mood.love = MathHelper.clamp(mood.love + gain, 0.0F, 100.0F);
                mood.anger = Math.max(0.0F, mood.anger - 4.0F);
                mood.calmTicks = Math.max(mood.calmTicks, 20 * 20);
                mood.gestureCooldown = 20 * 8;
                focus(other);

                if (thoughtCooldown <= 0) {
                    String line = String.format(Locale.ROOT,
                            FRIENDLY_LINES[ThreadLocalRandom.current().nextInt(FRIENDLY_LINES.length)],
                            other.getName().getString());
                    sayLocal(client, line + "  [SEVGİ " + Math.round(mood.love) + "]", false);
                    thoughtCooldown = 80;
                }
            }

            boolean peacefulClose = self.distanceTo(other) < 3.2F && !other.handSwinging && mood.anger < 25.0F;
            if (peacefulClose) mood.peaceTicks += 2;
            else mood.peaceTicks = Math.max(0, mood.peaceTicks - 4);

            if (mood.peaceTicks >= 20 * 12 && mood.gestureCooldown <= 0) {
                mood.love = MathHelper.clamp(mood.love + 1.2F, 0.0F, 100.0F);
                mood.peaceTicks = 0;
                mood.gestureCooldown = 20 * 6;
                focus(other);
            }
        }
    }

    private static boolean isFacing(PlayerEntity from, PlayerEntity to, float minimumDot) {
        Vec3d look = from.getRotationVec(1.0F).normalize();
        Vec3d toward = to.getEyePos().subtract(from.getEyePos());
        if (toward.lengthSquared() < 0.0001) return true;
        return look.dotProduct(toward.normalize()) >= minimumDot;
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
            mood.love = MathHelper.clamp(mood.love + gain, 0.0F, 100.0F);
            mood.anger = Math.max(0.0F, mood.anger - (10.0F + gain * 0.70F));
            mood.calmTicks = Math.max(mood.calmTicks, 20 * 55);
            focus(giver);

            String name = giver.getName().getString();
            String line = String.format(Locale.ROOT,
                    GIFT_LINES[ThreadLocalRandom.current().nextInt(GIFT_LINES.length)], name);
            sayLocal(client, line + "  [SEVGİ " + Math.round(mood.love) + "]", false);
        }
    }

    private static Mood mood(PlayerEntity player) {
        return MOODS.computeIfAbsent(player.getUuid(), id -> new Mood(player.getName().getString()));
    }

    private static void addAnger(MinecraftClient client, PlayerEntity player, float raw, String reason) {
        Mood mood = mood(player);
        float resistance = Math.min(0.58F, mood.love * 0.0058F);
        float gain = raw * (1.0F - resistance);
        if (mood.calmTicks > 0) gain *= 0.68F;

        int oldStage = stage(mood.anger);
        mood.anger = MathHelper.clamp(mood.anger + gain, 0.0F, 100.0F);
        mood.love = Math.max(0.0F, mood.love - raw * 0.10F);
        int newStage = stage(mood.anger);
        focus(player);

        if (thoughtCooldown <= 0 || newStage > oldStage || "killed".equals(reason)) {
            String line = rageLine(player.getName().getString(), mood.anger);
            sayLocal(client, line + "  [SİNİR " + Math.round(mood.anger) + "]", mood.anger >= 55.0F);
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

        PlayerEntity speaker = findSpeaker(client, lower);
        if (speaker == null || speaker == client.player) return;

        if (containsInsult(lower)) {
            String selfName = client.player.getName().getString().toLowerCase(Locale.ROOT);
            boolean directed = lower.contains(selfName) || lower.contains(" you ") || lower.contains(" sana ") || lower.contains(" sen ");
            addAnger(client, speaker, directed ? 20.0F : 10.0F, "chat");
            return;
        }

        if (containsPositive(lower)) {
            Mood mood = mood(speaker);
            mood.love = MathHelper.clamp(mood.love + 2.4F, 0.0F, 100.0F);
            mood.anger = Math.max(0.0F, mood.anger - 2.8F);
            mood.calmTicks = Math.max(mood.calmTicks, 20 * 10);
            focus(speaker);
        }
    }

    private static PlayerEntity findSpeaker(MinecraftClient client, String lower) {
        PlayerEntity fallback = null;
        for (PlayerEntity p : client.world.getPlayers()) {
            if (p == client.player) continue;
            String n = p.getName().getString().toLowerCase(Locale.ROOT);
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
                || padded.contains(" eyw ") || padded.contains(" sağol ") || padded.contains(" sagol ")
                || padded.contains(" iyi oyun ") || padded.contains(" güzel ") || padded.contains(" guzel ");
    }

    private static void sayLocal(MinecraftClient client, String line, boolean angry) {
        if (client.inGameHud == null || client.player == null) return;
        Text text = Text.literal("[ARROW-AI] " + line).formatted(angry ? Formatting.DARK_RED : Formatting.AQUA);
        client.inGameHud.getChatHud().addMessage(text);
        AnimeAnimationClient.triggerSpeechEmote(5000 + ThreadLocalRandom.current().nextInt(40), client.player.getId(), 72);
    }

    private static PlayerEntity activePlayer(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return null;
        if (mc.targetedEntity instanceof PlayerEntity target && target != mc.player) return target;
        if (focusTicks > 0 && focusUuid != null) {
            Entity e = mc.world.getPlayerByUuid(focusUuid);
            if (e instanceof PlayerEntity p) return p;
        }
        return null;
    }

    public static float activeLove() {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = activePlayer(mc);
        Mood mood = p == null ? null : MOODS.get(p.getUuid());
        return mood == null ? 0.0F : mood.love;
    }

    public static float activeAnger() {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = activePlayer(mc);
        Mood mood = p == null ? null : MOODS.get(p.getUuid());
        return mood == null ? 0.0F : mood.anger;
    }

    public static float loveFor(PlayerEntity player) {
        Mood mood = player == null ? null : MOODS.get(player.getUuid());
        return mood == null ? 0.0F : mood.love;
    }

    public static float angerFor(PlayerEntity player) {
        Mood mood = player == null ? null : MOODS.get(player.getUuid());
        return mood == null ? 0.0F : mood.anger;
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity shown = activePlayer(mc);
        if (shown == null) return;

        Mood mood = MOODS.get(shown.getUuid());
        if (mood == null) return;

        String title = "DUYGU • " + shown.getName().getString();
        String stats = "SİNİR " + Math.round(mood.anger) + "   SEVGİ " + Math.round(mood.love);
        String state = mood.anger >= 86 ? "ÖFKELİ" : mood.anger >= 62 ? "DÜŞMANCA" : mood.anger >= 34 ? "GERGİN"
                : mood.love >= 55 ? "GÜVEN" : mood.love >= 25 ? "SEVİYOR" : "SAKİN";

        int x = 8;
        int y = 8;
        int width = Math.max(132, Math.max(mc.textRenderer.getWidth(title), mc.textRenderer.getWidth(stats)) + 12);
        ctx.fill(x, y, x + width, y + 38, 0x99000000);
        ctx.drawTextWithShadow(mc.textRenderer, title, x + 6, y + 5, 0xFFFFFFFF);
        ctx.drawTextWithShadow(mc.textRenderer, stats, x + 6, y + 16, 0xFFE0E0E0);
        int stateColor = mood.anger >= 62 ? 0xFFFF5555 : mood.love >= 40 ? 0xFFFF77CC : 0xFFFFFFFF;
        ctx.drawTextWithShadow(mc.textRenderer, state, x + 6, y + 27, stateColor);
    }

    private static final class Mood {
        final String lastKnownName;
        float anger;
        float love;
        int calmTicks;
        int gestureCooldown;
        int greetingTicks;
        int peaceTicks;

        Mood(String name) {
            this.lastKnownName = name;
        }
    }
}
