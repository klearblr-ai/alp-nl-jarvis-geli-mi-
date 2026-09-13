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
    private static int drawDuration = 18;
    private static int drawDirection = 1; // 1 draw, -1 sheath

    private static int finisherStyle = 0;
    private static int finisherTicks = 0;
    private static int finisherDuration = 1;
    private static int finisherTargetId = -1;
    private static int finisherAttackerId = -1;

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
        if (finisherStyle > 19) finisherStyle = 1;
        finisherDuration = durationFor(finisherStyle);
        finisherTicks = finisherDuration;
        finisherTargetId = target.getId();
        finisherAttackerId = client.player.getId();
        swordDrawn = true;
        drawTicks = 0;
        client.player.sendMessage(Text.literal(
                finisherStyle == 19
                        ? "FINISHER 19: 2 DAKİKALIK SİNEMATİK KOREOGRAFİ"
                        : "FINISHER " + finisherStyle), true);
    }

    private static int durationFor(int style) {
        if (style == 19) return 2400; // 2 minutes at 20 tps
        return switch (style) {
            case 1, 2, 3, 4 -> 72;
            case 5, 6, 7, 8 -> 86;
            case 9, 10, 11, 12 -> 98;
            case 13, 14, 15 -> 112;
            case 16, 17, 18 -> 132;
            default -> 80;
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
}
