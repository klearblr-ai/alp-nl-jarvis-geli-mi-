package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;

public final class AutoPvPAnimationClient implements ClientModInitializer {
    private static int hurtTicks;
    private static int landingTicks;
    private static int stabTicks;
    private static int kickTicks;
    private static int punchTicks;
    private static int mineTicks;
    private static int bedBreakTicks;
    private static int placeTicks;
    private static int finisherTicks;
    private static int finisherDuration = 38;
    private static int finisherTargetId = -1;
    private static int attackSequence;
    private static float lastHealth = -1.0F;
    private static boolean lastGrounded = true;
    private static boolean lastAttackDown;
    private static boolean lastUseDown;
    private static Perspective savedPerspective;
    private static boolean cinematicPerspective;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoPvPAnimationClient::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> renderCinematic(ctx));
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || !ProceduralAIRigClient.isEnabled()) {
            resetTransient();
            lastHealth = -1.0F;
            return;
        }

        PlayerEntity p = client.player;
        float hp = p.getHealth();
        if (lastHealth >= 0.0F && hp < lastHealth - 0.01F && hp > 0.0F) hurtTicks = 11;
        lastHealth = hp;

        boolean grounded = p.isOnGround();
        if (!lastGrounded && grounded && p.getVelocity().y <= 0.10) landingTicks = 10;
        lastGrounded = grounded;

        boolean attackDown = client.options.attackKey.isPressed();
        boolean useDown = client.options.useKey.isPressed();
        boolean attackPressed = attackDown && !lastAttackDown;

        if (attackDown && client.crosshairTarget instanceof BlockHitResult hit) {
            boolean bed = client.world.getBlockState(hit.getBlockPos()).isIn(BlockTags.BEDS);
            if (bed) {
                bedBreakTicks = 8;
                mineTicks = 0;
            } else if (bedBreakTicks <= 0) {
                mineTicks = 6;
            }
        }

        boolean holdingBlock = p.getMainHandStack().getItem() instanceof BlockItem
                || p.getOffHandStack().getItem() instanceof BlockItem;
        if (useDown && holdingBlock && client.crosshairTarget instanceof BlockHitResult) {
            placeTicks = 6;
        }

        if (attackPressed && client.targetedEntity instanceof PlayerEntity target && target != p && p.distanceTo(target) <= 4.25F) {
            attackSequence++;
            boolean sword = p.getMainHandStack().isIn(ItemTags.SWORDS);
            if (sword) {
                if ((attackSequence & 3) == 0) stabTicks = 13;
                float ratio = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
                if (ratio <= 0.28F && finisherTicks <= 0) startFinisher(client, target);
            } else {
                if (attackSequence % 3 == 0) kickTicks = 15;
                else punchTicks = 9;
            }
        }

        if (finisherTicks > 0) {
            finisherTicks--;
            if (finisherTicks == 0) endCinematic(client);
        }
        if (hurtTicks > 0) hurtTicks--;
        if (landingTicks > 0) landingTicks--;
        if (stabTicks > 0) stabTicks--;
        if (kickTicks > 0) kickTicks--;
        if (punchTicks > 0) punchTicks--;
        if (mineTicks > 0 && !attackDown) mineTicks--;
        if (bedBreakTicks > 0 && !attackDown) bedBreakTicks--;
        if (placeTicks > 0 && !useDown) placeTicks--;

        lastAttackDown = attackDown;
        lastUseDown = useDown;
    }

    private static void startFinisher(MinecraftClient client, PlayerEntity target) {
        finisherTicks = finisherDuration;
        finisherTargetId = target.getId();
        stabTicks = 0;
        kickTicks = 0;
        punchTicks = 0;
        mineTicks = 0;
        bedBreakTicks = 0;
        placeTicks = 0;
        if (!cinematicPerspective) {
            savedPerspective = client.options.getPerspective();
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            cinematicPerspective = true;
        }
        if (client.player != null) client.player.sendMessage(Text.literal("AUTO FINISHER • FULL BODY"), true);
    }

    private static void endCinematic(MinecraftClient client) {
        finisherTargetId = -1;
        if (cinematicPerspective && savedPerspective != null) client.options.setPerspective(savedPerspective);
        cinematicPerspective = false;
        savedPerspective = null;
    }

    private static void renderCinematic(DrawContext ctx) {
        if (finisherTicks <= 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int bar = Math.max(18, h / 9);
        ctx.fill(0, 0, w, bar, 0xEE000000);
        ctx.fill(0, h - bar, w, h, 0xEE000000);

        float p = getFinisherProgress();
        if (p > 0.48F && p < 0.58F) {
            int a = (int)(120 * (1.0F - Math.abs(0.53F - p) / 0.05F));
            a = Math.max(0, Math.min(120, a));
            ctx.fill(0, bar, w, h - bar, (a << 24) | 0x00FFFFFF);
        }
    }

    private static void resetTransient() {
        hurtTicks = landingTicks = stabTicks = kickTicks = punchTicks = mineTicks = bedBreakTicks = placeTicks = finisherTicks = 0;
        finisherTargetId = -1;
        lastAttackDown = false;
        lastUseDown = false;
    }

    public static int getHurtTicks() { return hurtTicks; }
    public static int getLandingTicks() { return landingTicks; }
    public static int getStabTicks() { return stabTicks; }
    public static int getKickTicks() { return kickTicks; }
    public static int getPunchTicks() { return punchTicks; }
    public static int getMineTicks() { return mineTicks; }
    public static int getBedBreakTicks() { return bedBreakTicks; }
    public static int getPlaceTicks() { return placeTicks; }
    public static int getFinisherTicks() { return finisherTicks; }
    public static int getFinisherTargetId() { return finisherTargetId; }
    public static float getFinisherProgress() {
        return finisherTicks <= 0 ? 0.0F : 1.0F - (float)finisherTicks / Math.max(1, finisherDuration);
    }
}
