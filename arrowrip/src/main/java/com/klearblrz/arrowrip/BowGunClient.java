package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side bow-as-gun controller.
 * Virtual bullets, magazine, tracer and long-range target assist are visual/client-side.
 * Any real multiplayer damage still follows the vanilla/server bow rules.
 */
public final class BowGunClient implements ClientModInitializer {
    private static KeyBinding inspectKey;
    private static KeyBinding reloadKey;
    private static KeyBinding fireModeKey;

    private static final int MAG_SIZE = 19;
    private static final int FIRE_TICKS = 3;
    private static final int VIRTUAL_FIRE_INTERVAL = 4;
    private static final int RELOAD_TICKS_MAX = 20;
    private static final int INSPECT_TICKS_MAX = 52;
    private static final double VIRTUAL_RANGE = 42.0;
    private static final double AUTO_AIM_DOT = 0.925;
    private static final DustParticleEffect BULLET = new DustParticleEffect(0xFFD77A, 0.50F);
    private static final DustParticleEffect BULLET_HOT = new DustParticleEffect(0xFFF4D6, 0.34F);

    private static int magazine = MAG_SIZE;
    private static int reloadTicks;
    private static int inspectTicks;
    private static int recoilTicks;
    private static int restartDelay;
    private static int virtualFireCooldown;
    private static int muzzleFlashTicks;
    private static boolean releasedThisUse;
    private static boolean reloadStopSent;
    private static boolean autoMode = true;
    private static boolean semiLatched;
    private static String lockName = "";
    private static int lockDistance;

    @Override
    public void onInitializeClient() {
        inspectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.bowgun_inspect",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                ArrowRipClient.CATEGORY));
        reloadKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.bowgun_reload",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                ArrowRipClient.CATEGORY));
        fireModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.bowgun_firemode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(BowGunClient::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> renderHud(ctx));
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            resetAll();
            return;
        }

        boolean bow = client.player.getMainHandStack().isOf(Items.BOW);
        if (!bow) {
            resetTransient();
            lockName = "";
            lockDistance = 0;
            return;
        }

        PlayerEntity aim = findAutoTarget(client, client.player);
        if (aim != null) {
            lockName = aim.getName().getString();
            lockDistance = Math.round(client.player.distanceTo(aim));
        } else {
            lockName = "";
            lockDistance = 0;
        }

        while (inspectKey.wasPressed()) {
            if (reloadTicks <= 0) {
                inspectTicks = INSPECT_TICKS_MAX;
                client.player.sendMessage(Text.literal("SİLAH INSPECT"), true);
            }
        }

        while (reloadKey.wasPressed()) {
            if (reloadTicks <= 0 && magazine < MAG_SIZE) startReload(client);
        }

        while (fireModeKey.wasPressed()) {
            autoMode = !autoMode;
            semiLatched = false;
            client.player.sendMessage(Text.literal(autoMode
                    ? "ATEŞ MODU: AUTO + UZAK HEDEF"
                    : "ATEŞ MODU: TEK TEK"), true);
        }

        if (inspectTicks > 0) inspectTicks--;
        if (recoilTicks > 0) recoilTicks--;
        if (restartDelay > 0) restartDelay--;
        if (virtualFireCooldown > 0) virtualFireCooldown--;
        if (muzzleFlashTicks > 0) muzzleFlashTicks--;

        boolean usePressed = client.options.useKey.isPressed();
        if (!usePressed) semiLatched = false;

        if (reloadTicks > 0) {
            if (!reloadStopSent && client.player.isUsingItem() && client.interactionManager != null) {
                client.interactionManager.stopUsingItem(client.player);
                reloadStopSent = true;
            }
            reloadTicks--;
            if (reloadTicks == 0) {
                magazine = MAG_SIZE;
                releasedThisUse = false;
                reloadStopSent = false;
                restartDelay = 2;
                virtualFireCooldown = 2;
                client.player.sendMessage(Text.literal("SANAL ŞARJÖR 19/19"), true);
            }
            return;
        }

        // Virtual gun layer: works even when no physical arrow item exists.
        if (client.currentScreen == null
                && usePressed
                && inspectTicks <= 0
                && virtualFireCooldown <= 0
                && magazine > 0
                && (autoMode || !semiLatched)) {
            fireVirtualBullet(client, client.player, aim);
            magazine--;
            recoilTicks = 6;
            muzzleFlashTicks = 3;
            virtualFireCooldown = VIRTUAL_FIRE_INTERVAL;
            if (!autoMode) semiLatched = true;
            if (magazine <= 0) startReload(client);
        }

        // In parallel, keep the vanilla bow use/release path alive when the server allows it.
        if (client.player.isUsingItem() && client.player.getActiveItem().isOf(Items.BOW)) {
            int used = client.player.getActiveItem().getMaxUseTime(client.player) - client.player.getItemUseTimeLeft();
            if (used >= FIRE_TICKS && !releasedThisUse && client.interactionManager != null) {
                client.interactionManager.stopUsingItem(client.player);
                releasedThisUse = true;
                restartDelay = 2;
            }
        } else {
            releasedThisUse = false;
        }

        if (client.currentScreen == null
                && usePressed
                && restartDelay <= 0
                && reloadTicks <= 0
                && magazine > 0
                && !client.player.isUsingItem()
                && client.interactionManager != null) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        }
    }

    private static PlayerEntity findAutoTarget(MinecraftClient client, PlayerEntity shooter) {
        if (client.targetedEntity instanceof PlayerEntity direct
                && direct != shooter
                && direct.isAlive()
                && shooter.distanceTo(direct) <= VIRTUAL_RANGE) {
            return direct;
        }

        Vec3d eye = new Vec3d(shooter.getX(), shooter.getEyeY(), shooter.getZ());
        Vec3d look = shooter.getRotationVec(1.0F).normalize();
        PlayerEntity best = null;
        double bestScore = -999.0;

        for (PlayerEntity candidate : client.world.getPlayers()) {
            if (candidate == shooter || !candidate.isAlive()) continue;
            Vec3d center = new Vec3d(candidate.getX(), candidate.getY() + candidate.getHeight() * 0.58, candidate.getZ());
            Vec3d delta = center.subtract(eye);
            double dist = delta.length();
            if (dist < 0.01 || dist > VIRTUAL_RANGE) continue;
            double dot = look.dotProduct(delta.multiply(1.0 / dist));
            if (dot < AUTO_AIM_DOT) continue;
            double score = dot * 5.0 - dist * 0.018;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static void fireVirtualBullet(MinecraftClient client, PlayerEntity shooter, PlayerEntity target) {
        Vec3d look = shooter.getRotationVec(1.0F).normalize();
        Vec3d start = new Vec3d(shooter.getX(), shooter.getEyeY() - 0.12, shooter.getZ()).add(look.multiply(0.45));
        Vec3d end;

        if (target != null) {
            end = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.58, target.getZ());
        } else {
            end = start.add(look.multiply(VIRTUAL_RANGE));
        }

        Vec3d delta = end.subtract(start);
        double distance = Math.min(VIRTUAL_RANGE, delta.length());
        Vec3d dir = delta.lengthSquared() < 0.0001 ? look : delta.normalize();
        int steps = Math.max(18, Math.min(54, (int)(distance * 1.35)));

        for (int i = 0; i <= steps; i++) {
            double d = distance * (i / (double)steps);
            Vec3d q = start.add(dir.multiply(d));
            client.world.addParticleClient(i % 5 == 0 ? BULLET_HOT : BULLET,
                    q.x, q.y, q.z,
                    dir.x * 0.035, dir.y * 0.035, dir.z * 0.035);
        }

        // Local-only impact flash. No fake server damage is applied here.
        if (target != null) {
            Vec3d hit = end;
            for (int i = 0; i < 14; i++) {
                double vx = (client.world.random.nextDouble() - 0.5) * 0.11;
                double vy = (client.world.random.nextDouble() - 0.5) * 0.11;
                double vz = (client.world.random.nextDouble() - 0.5) * 0.11;
                client.world.addParticleClient(i % 3 == 0 ? BULLET_HOT : BULLET,
                        hit.x + vx * 0.4, hit.y + vy * 0.4, hit.z + vz * 0.4,
                        vx, vy, vz);
            }
        }

        shooter.swingHand(Hand.MAIN_HAND);
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.getMainHandStack().isOf(Items.BOW)) return;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        int gap = mc.player.isUsingItem() ? 2 : 4;
        if (!lockName.isEmpty()) gap = 2;
        int arm = 4;
        int cross = !lockName.isEmpty() ? 0xFFFFD56A : 0xE8FFFFFF;
        ctx.fill(cx - gap - arm, cy, cx - gap, cy + 1, cross);
        ctx.fill(cx + gap + 1, cy, cx + gap + arm + 1, cy + 1, cross);
        ctx.fill(cx, cy - gap - arm, cx + 1, cy - gap, cross);
        ctx.fill(cx, cy + gap + 1, cx + 1, cy + gap + arm + 1, cross);

        String mode = autoMode ? "AUTO 42m" : "SEMI";
        String ammo = mode + "  " + magazine + "/" + MAG_SIZE;
        int tx = w - mc.textRenderer.getWidth(ammo) - 12;
        int ty = h - 39;
        ctx.fill(tx - 5, ty - 4, w - 7, ty + 11, 0x88000000);
        ctx.drawTextWithShadow(mc.textRenderer, ammo, tx, ty, 0xFFFFFFFF);

        if (!lockName.isEmpty()) {
            String lock = "LOCK  " + lockName + "  " + lockDistance + "m";
            int lx = (w - mc.textRenderer.getWidth(lock)) / 2;
            ctx.drawTextWithShadow(mc.textRenderer, lock, lx, cy + 18, 0xFFFFD56A);
        }

        if (reloadTicks > 0) {
            int barW = 58;
            int bx = w - barW - 12;
            int by = h - 22;
            float p = reloadProgress(0.0F);
            ctx.fill(bx, by, bx + barW, by + 3, 0x66000000);
            ctx.fill(bx, by, bx + Math.max(1, (int)(barW * p)), by + 3, 0xFFFFFFFF);
            ctx.drawTextWithShadow(mc.textRenderer, "RELOAD", bx, by - 10, 0xFFFFFFFF);
        }
    }

    private static void startReload(MinecraftClient client) {
        reloadTicks = RELOAD_TICKS_MAX;
        inspectTicks = 0;
        recoilTicks = 0;
        releasedThisUse = true;
        reloadStopSent = false;
        semiLatched = true;
        if (client.player != null) client.player.sendMessage(Text.literal("ŞARJÖR DEĞİŞTİRİLİYOR…"), true);
    }

    private static void resetTransient() {
        reloadTicks = 0;
        inspectTicks = 0;
        recoilTicks = 0;
        restartDelay = 0;
        virtualFireCooldown = 0;
        muzzleFlashTicks = 0;
        releasedThisUse = false;
        reloadStopSent = false;
        semiLatched = false;
    }

    private static void resetAll() {
        magazine = MAG_SIZE;
        resetTransient();
    }

    public static float recoilProgress(float tickDelta) {
        if (recoilTicks <= 0) return 0.0F;
        float p = (recoilTicks - tickDelta) / 6.0F;
        return MathHelper.clamp(p, 0.0F, 1.0F);
    }

    public static float reloadProgress(float tickDelta) {
        if (reloadTicks <= 0) return 0.0F;
        return 1.0F - MathHelper.clamp((reloadTicks - tickDelta) / (float) RELOAD_TICKS_MAX, 0.0F, 1.0F);
    }

    public static float inspectProgress(float tickDelta) {
        if (inspectTicks <= 0) return 0.0F;
        return 1.0F - MathHelper.clamp((inspectTicks - tickDelta) / (float) INSPECT_TICKS_MAX, 0.0F, 1.0F);
    }

    public static boolean isReloading() { return reloadTicks > 0; }
    public static boolean isInspecting() { return inspectTicks > 0; }
    public static boolean isAutoMode() { return autoMode; }
    public static int magazine() { return magazine; }
    public static int magazineSize() { return MAG_SIZE; }
    public static int muzzleFlashTicks() { return muzzleFlashTicks; }
}
