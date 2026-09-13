package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Client-only sword draw/sheath and cinematic finisher controller. */
public final class SwordFinisherClient implements ClientModInitializer {
    private static KeyBinding swordToggleKey;
    private static KeyBinding finisherKey;

    private static boolean swordDrawn = true;
    private static int drawTicks = 0;
    private static final int drawDuration = 18;
    private static int drawDirection = 1; // 1 draw, -1 sheath

    private static int finisherStyle = 0;
    private static int finisherTicks = 0;
    private static int finisherDuration = 1;
    private static int finisherTargetId = -1;
    private static int finisherAttackerId = -1;

    private static final String[] FINISHER_NAMES = {
            "",
            "Diagonal Break",
            "Reverse Cut",
            "Rising Fang",
            "Overhead Crush",
            "Straight Impale",
            "Two-Hand Thrust",
            "Spin Cutter",
            "Kick + Slash",
            "Knee + Pommel",
            "Low Stab",
            "Cross Slash",
            "Backhand Cut",
            "Disarm + Pommel",
            "Elbow + Slash",
            "Feint + Thrust",
            "Jumping Cut",
            "Sweep + Stab",
            "Guard Break",
            "Iaido Draw Cut",
            "Reverse-Grip Stab",
            "Two-Step Combo",
            "Roundhouse + Thrust",
            "Execution Overhead",
            "Final Anime Combo"
    };

    @Override
    public void onInitializeClient() {
        swordToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.sword_draw_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                ArrowRipClient.CATEGORY));
        finisherKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.finisher_19",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (swordToggleKey.wasPressed()) toggleSword(client);
            while (finisherKey.wasPressed()) startNextFinisher(client);

            if (drawTicks > 0) drawTicks--;
            if (finisherTicks > 0) finisherTicks--;
            if (finisherTicks == 0) {
                finisherTargetId = -1;
                finisherAttackerId = -1;
            }
        });
    }

    private static void toggleSword(MinecraftClient client) {
        if (client.player == null) return;
        if (!client.player.getMainHandStack().isIn(ItemTags.SWORDS)) {
            client.player.sendMessage(Text.literal("P için ana elde kılıç olmalı"), true);
            return;
        }
        swordDrawn = !swordDrawn;
        drawDirection = swordDrawn ? 1 : -1;
        drawTicks = drawDuration;
        client.player.sendMessage(Text.literal(swordDrawn ? "Kılıç: ÇEKİLDİ" : "Kılıç: SIRTA KONDU"), true);
    }

    private static void startNextFinisher(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (!(client.targetedEntity instanceof PlayerEntity target) || target == client.player || client.player.distanceTo(target) > 5.0F) {
            client.player.sendMessage(Text.literal("Finisher için yakındaki oyuncuya bak"), true);
            return;
        }
        if (finisherTicks > 0) {
            finisherTicks = 0;
            finisherTargetId = -1;
            finisherAttackerId = -1;
            client.player.sendMessage(Text.literal("Finisher iptal"), true);
            return;
        }

        finisherStyle++;
        if (finisherStyle > 24) finisherStyle = 1;
        finisherDuration = durationFor(finisherStyle);
        finisherTicks = finisherDuration;
        finisherTargetId = target.getId();
        finisherAttackerId = client.player.getId();
        swordDrawn = true;
        drawTicks = 0;

        if ((finisherStyle == 13 || finisherStyle == 18) && target.getMainHandStack().isIn(ItemTags.SWORDS)) {
            SwordDisarmClient.trigger(client, client.player, target);
        }

        client.player.sendMessage(Text.literal(
                "FINISHER " + finisherStyle + "/24 • " + getFinisherName()), true);
    }

    private static int durationFor(int style) {
        return switch (style) {
            case 1, 2, 3, 4 -> 62;
            case 5, 6, 7, 8 -> 70;
            case 9, 10, 11, 12 -> 76;
            case 13, 14, 15, 16 -> 82;
            case 17, 18, 19, 20 -> 88;
            case 21, 22, 23 -> 96;
            case 24 -> 116;
            default -> 72;
        };
    }

    public static boolean isSwordDrawn() { return swordDrawn; }
    public static int getDrawTicks() { return drawTicks; }
    public static int getDrawDuration() { return drawDuration; }
    public static int getDrawDirection() { return drawDirection; }

    public static int getFinisherStyle() { return finisherStyle; }
    public static int getFinisherTicks() { return finisherTicks; }
    public static int getFinisherDuration() { return finisherDuration; }
    public static int getFinisherTargetId() { return finisherTargetId; }
    public static int getFinisherAttackerId() { return finisherAttackerId; }
    public static boolean isFinisherActive() { return finisherTicks > 0; }
    public static String getFinisherName() {
        return finisherStyle >= 1 && finisherStyle < FINISHER_NAMES.length
                ? FINISHER_NAMES[finisherStyle] : "Cinematic";
    }
}
