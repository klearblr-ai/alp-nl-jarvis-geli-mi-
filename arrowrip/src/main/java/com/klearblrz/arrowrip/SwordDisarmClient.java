package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Pure client-side sword clash/disarm illusion.
 * It never edits server inventory: held-sword rendering is hidden briefly and a local ItemEntity is thrown away.
 */
public final class SwordDisarmClient implements ClientModInitializer {
    private static int targetId = -1;
    private static int attackerId = -1;
    private static int disarmTicks;
    private static final int DURATION = 42;
    private static int cooldown;
    private static boolean lastAttackDown;
    private static ItemEntity droppedSword;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(SwordDisarmClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (cooldown > 0) cooldown--;

        if (client.player == null || client.world == null) {
            clear();
            lastAttackDown = false;
            return;
        }

        boolean attackDown = client.options.attackKey.isPressed();
        boolean pressed = attackDown && !lastAttackDown;
        lastAttackDown = attackDown;

        if (pressed && cooldown <= 0 && client.targetedEntity instanceof PlayerEntity target
                && target != client.player && client.player.distanceTo(target) <= 4.35F
                && client.player.getMainHandStack().isIn(ItemTags.SWORDS)
                && target.getMainHandStack().isIn(ItemTags.SWORDS)) {
            trigger(client, client.player, target);
        }

        if (disarmTicks > 0) {
            disarmTicks--;
            if (disarmTicks == 0) clear();
        }
    }

    public static boolean trigger(MinecraftClient client, PlayerEntity attacker, PlayerEntity target) {
        if (client.world == null || cooldown > 0 || target == attacker
                || !target.getMainHandStack().isIn(ItemTags.SWORDS)) return false;

        clearVisualOnly();
        attackerId = attacker.getId();
        targetId = target.getId();
        disarmTicks = DURATION;
        cooldown = 56;

        ItemStack sword = target.getMainHandStack().copyWithCount(1);
        droppedSword = new ItemEntity(client.world,
                target.getX(), target.getY() + target.getHeight() * 0.68, target.getZ(), sword);
        droppedSword.setPickupDelayInfinite();

        Vec3d away = new Vec3d(target.getX() - attacker.getX(), 0.0, target.getZ() - attacker.getZ());
        if (away.lengthSquared() < 0.0001) {
            Vec3d look = attacker.getRotationVec(1.0F);
            away = new Vec3d(look.x, 0.0, look.z);
        }
        away = away.normalize();
        Vec3d side = new Vec3d(-away.z, 0.0, away.x);
        droppedSword.setVelocity(
                away.x * 0.34 + side.x * 0.24,
                0.38,
                away.z * 0.34 + side.z * 0.24);
        droppedSword.setYaw(target.getYaw() + 90.0F);
        client.world.addEntity(droppedSword);

        target.playSound(SoundEvents.ITEM_SHIELD_BLOCK, 0.75F, 1.48F);
        attacker.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 0.55F, 0.92F);
        attacker.sendMessage(Text.literal("KILIÇ ÇARPIŞMASI • RAKİBİN KILICI DÜŞTÜ"), true);
        return true;
    }

    private static void clearVisualOnly() {
        if (droppedSword != null) droppedSword.discard();
        droppedSword = null;
    }

    private static void clear() {
        clearVisualOnly();
        targetId = -1;
        attackerId = -1;
        disarmTicks = 0;
    }

    public static boolean shouldHideSword(int entityId, ItemStack stack) {
        return disarmTicks > 0 && entityId == targetId && stack.isIn(ItemTags.SWORDS);
    }

    public static int getTargetId() { return targetId; }
    public static int getAttackerId() { return attackerId; }
    public static int getTicks() { return disarmTicks; }
    public static float getProgress() {
        return disarmTicks <= 0 ? 0.0F : 1.0F - disarmTicks / (float)DURATION;
    }
    public static boolean isActive() { return disarmTicks > 0; }
}
