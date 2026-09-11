package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class AnimeAnimationClient implements ClientModInitializer {
    private static KeyBinding emoteKey;
    private static KeyBinding sitKey;
    private static int emoteType = 0;
    private static int emoteTicks = 0;
    private static boolean sitting = false;

    // Every spoken line gets a moving cinematic emote. This can target the local
    // player or the looked-at player because JapaneseVoiceClient chooses the speaker.
    private static int speechEmoteType = 0;
    private static int speechEmoteTicks = 0;
    private static int speechEmoteDuration = 1;
    private static int speechSpeakerId = -1;

    @Override
    public void onInitializeClient() {
        emoteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.anime_emote", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, ArrowRipClient.CATEGORY));
        sitKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.anime_sit", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (emoteKey.wasPressed()) {
                emoteType++;
                if (emoteType > 9) emoteType = 1;
                emoteTicks = 140;
                sitting = false;
                speechEmoteTicks = 0;
                speechSpeakerId = -1;
                if (client.player != null) {
                    String name = switch (emoteType) {
                        case 1 -> "POWER UP";
                        case 2 -> "DÜŞÜNME";
                        case 3 -> "VICTORY";
                        case 4 -> "KILIÇ DURUŞU";
                        case 5 -> "ANIME BOW";
                        case 6 -> "VILLAIN LAUGH";
                        case 7 -> "COOL POSE";
                        case 8 -> "RAGE";
                        default -> "SALUTE";
                    };
                    client.player.sendMessage(Text.literal("Anime emote: " + name), true);
                }
            }

            while (sitKey.wasPressed()) {
                sitting = !sitting;
                emoteTicks = 0;
                speechEmoteTicks = 0;
                speechSpeakerId = -1;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(sitting ? "Anime oturuş: AÇIK" : "Anime oturuş: KAPALI"), true);
                }
            }

            if (emoteTicks > 0) emoteTicks--;
            if (emoteTicks == 0) emoteType = 0;

            if (speechEmoteTicks > 0) speechEmoteTicks--;
            if (speechEmoteTicks == 0) {
                speechEmoteType = 0;
                speechSpeakerId = -1;
            }
        });
    }

    public static void triggerSpeechEmote(int lineIndex, int speakerId, int durationTicks) {
        speechEmoteType = speechStyleFor(lineIndex);
        speechEmoteDuration = Math.max(45, durationTicks);
        speechEmoteTicks = speechEmoteDuration;
        speechSpeakerId = speakerId;
        // Speech is a scene of its own, so do not keep a frozen manual pose layered over it.
        emoteTicks = 0;
        emoteType = 0;
        sitting = false;
    }

    private static int speechStyleFor(int idx) {
        return switch (idx) {
            case 0, 6 -> 2;          // Nani / Nanda: surprise-question gesture
            case 1 -> 5;             // Yamero: stop palm
            case 2 -> 6;             // Ikuzo: battle-ready gesture
            case 3 -> 7;             // Yare yare: calm/cool gesture
            case 4, 13, 19 -> 10;    // Mada mada / mada owatte / mada da: challenge
            case 5 -> 3;             // Sugoi: impressed reaction
            case 7, 11, 16, 17 -> 4; // Kamuda / chikara: power-up declaration
            case 8, 12, 14, 18 -> 9; // threat / "koroshite yaru" style cinematic
            case 9, 15 -> 12;        // final declaration
            case 10 -> 11;           // determined fist-to-chest
            default -> 1 + Math.floorMod(idx * 7 + idx / 3, 12);
        };
    }

    public static int getEmoteType() { return emoteType; }
    public static int getEmoteTicks() { return emoteTicks; }
    public static boolean isSitting() { return sitting; }

    public static int getSpeechEmoteType() { return speechEmoteType; }
    public static int getSpeechEmoteTicks() { return speechEmoteTicks; }
    public static int getSpeechEmoteDuration() { return speechEmoteDuration; }
    public static int getSpeechSpeakerId() { return speechSpeakerId; }
}
