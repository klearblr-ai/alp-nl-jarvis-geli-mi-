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

public final class BowGunClient implements ClientModInitializer {
    private static KeyBinding inspectKey, reloadKey, fireModeKey;

    private static final int MAG_SIZE = 19;
    private static final int SERVER_BOW_FULL_CHARGE_TICKS = 20;
    private static final int VIRTUAL_FIRE_INTERVAL = 2;
    private static final int RELOAD_TICKS_MAX = 16;
    private static final int INSPECT_TICKS_MAX = 52;
    private static final double VIRTUAL_RANGE = 120.0;
    private static final double AUTO_AIM_DOT = 0.955;
    private static final DustParticleEffect BULLET = new DustParticleEffect(0xFFD77A, 0.46F);
    private static final DustParticleEffect BULLET_HOT = new DustParticleEffect(0xFFF4D6, 0.30F);

    private static int magazine = MAG_SIZE;
    private static int reloadTicks, inspectTicks, recoilTicks, restartDelay, virtualFireCooldown, muzzleFlashTicks;
    private static boolean releasedThisUse, reloadStopSent, autoMode = true, semiLatched;
    private static String lockName = "";
    private static int lockDistance;

    @Override
    public void onInitializeClient() {
        inspectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.bowgun_inspect", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_I, ArrowRipClient.CATEGORY));
        reloadKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.bowgun_reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, ArrowRipClient.CATEGORY));
        fireModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.bowgun_firemode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, ArrowRipClient.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(BowGunClient::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> renderHud(ctx));
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) { resetAll(); return; }
        PlayerEntity shooter = client.player;
        if (!shooter.getMainHandStack().isOf(Items.BOW)) {
            resetTransient(); lockName = ""; lockDistance = 0; return;
        }

        PlayerEntity aim = findAutoTarget(client, shooter);
        if (aim != null) {
            lockName = aim.getName().getString();
            lockDistance = Math.round(shooter.distanceTo(aim));
        } else {
            lockName = ""; lockDistance = 0;
        }

        while (inspectKey.wasPressed()) {
            if (reloadTicks <= 0) { inspectTicks = INSPECT_TICKS_MAX; shooter.sendMessage(Text.literal("SİLAH INSPECT"), true); }
        }
        while (reloadKey.wasPressed()) if (reloadTicks <= 0 && magazine < MAG_SIZE) startReload(client);
        while (fireModeKey.wasPressed()) {
            autoMode = !autoMode; semiLatched = false;
            shooter.sendMessage(Text.literal(autoMode ? "ATEŞ MODU: AUTO 120m" : "ATEŞ MODU: SEMI"), true);
        }

        if (inspectTicks > 0) inspectTicks--;
        if (recoilTicks > 0) recoilTicks--;
        if (restartDelay > 0) restartDelay--;
        if (virtualFireCooldown > 0) virtualFireCooldown--;
        if (muzzleFlashTicks > 0) muzzleFlashTicks--;

        boolean usePressed = client.options.useKey.isPressed();
        if (!usePressed) semiLatched = false;

        if (reloadTicks > 0) {
            if (!reloadStopSent && shooter.isUsingItem() && client.interactionManager != null) {
                client.interactionManager.stopUsingItem(shooter);
                reloadStopSent = true;
            }
            reloadTicks--;
            if (reloadTicks == 0) {
                magazine = MAG_SIZE; releasedThisUse = false; reloadStopSent = false;
                restartDelay = 1; virtualFireCooldown = 1;
                shooter.sendMessage(Text.literal("SANAL ŞARJÖR 19/19"), true);
            }
            return;
        }

        // Instant client-side gun shot. It does not need a real arrow item.
        if (client.currentScreen == null && usePressed && inspectTicks <= 0
                && virtualFireCooldown <= 0 && magazine > 0 && (autoMode || !semiLatched)) {
            fireVirtualBullet(client, shooter, aim);
            magazine--;
            recoilTicks = 6;
            muzzleFlashTicks = 3;
            virtualFireCooldown = VIRTUAL_FIRE_INTERVAL;
            if (!autoMode) semiLatched = true;
            if (magazine <= 0) startReload(client);
        }

        // If the server accepts a normal bow shot, silently keep it charging to full power in the background.
        if (shooter.isUsingItem() && shooter.getActiveItem().isOf(Items.BOW)) {
            int used = shooter.getActiveItem().getMaxUseTime(shooter) - shooter.getItemUseTimeLeft();
            if (used >= SERVER_BOW_FULL_CHARGE_TICKS && !releasedThisUse && client.interactionManager != null) {
                client.interactionManager.stopUsingItem(shooter);
                releasedThisUse = true;
                restartDelay = 1;
            }
        } else {
            releasedThisUse = false;
        }

        if (client.currentScreen == null && usePressed && restartDelay <= 0 && reloadTicks <= 0
                && magazine > 0 && !shooter.isUsingItem() && client.interactionManager != null) {
            client.interactionManager.interactItem(shooter, Hand.MAIN_HAND);
        }
    }

    private static PlayerEntity findAutoTarget(MinecraftClient client, PlayerEntity shooter) {
        if (client.targetedEntity instanceof PlayerEntity direct && direct != shooter && direct.isAlive()
                && shooter.distanceTo(direct) <= VIRTUAL_RANGE) return direct;

        Vec3d eye = new Vec3d(shooter.getX(), shooter.getEyeY(), shooter.getZ());
        Vec3d look = shooter.getRotationVec(1.0F).normalize();
        PlayerEntity best = null;
        double bestScore = -999.0;
        for (PlayerEntity p : client.world.getPlayers()) {
            if (p == shooter || !p.isAlive()) continue;
            Vec3d center = new Vec3d(p.getX(), p.getY() + p.getHeight() * 0.58, p.getZ());
            Vec3d d = center.subtract(eye);
            double dist = d.length();
            if (dist < 0.01 || dist > VIRTUAL_RANGE) continue;
            double dot = look.dotProduct(d.multiply(1.0 / dist));
            if (dot < AUTO_AIM_DOT) continue;
            double score = dot * 8.0 - dist * 0.012;
            if (score > bestScore) { bestScore = score; best = p; }
        }
        return best;
    }

    private static void fireVirtualBullet(MinecraftClient client, PlayerEntity shooter, PlayerEntity target) {
        Vec3d look = shooter.getRotationVec(1.0F).normalize();
        Vec3d start = new Vec3d(shooter.getX(), shooter.getEyeY() - 0.12, shooter.getZ()).add(look.multiply(0.45));
        Vec3d end = target != null
                ? new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.58, target.getZ())
                : start.add(look.multiply(VIRTUAL_RANGE));
        Vec3d delta = end.subtract(start);
        double distance = Math.min(VIRTUAL_RANGE, delta.length());
        Vec3d dir = delta.lengthSquared() < 0.0001 ? look : delta.normalize();
        int steps = Math.max(22, Math.min(80, (int)(distance * 0.70)));
        for (int i = 0; i <= steps; i++) {
            double dd = distance * (i / (double)steps);
            Vec3d q = start.add(dir.multiply(dd));
            client.world.addParticleClient(i % 5 == 0 ? BULLET_HOT : BULLET,
                    q.x, q.y, q.z, dir.x * 0.05, dir.y * 0.05, dir.z * 0.05);
        }
        if (target != null) {
            for (int i = 0; i < 18; i++) {
                double vx = (client.world.random.nextDouble() - 0.5) * 0.13;
                double vy = (client.world.random.nextDouble() - 0.5) * 0.13;
                double vz = (client.world.random.nextDouble() - 0.5) * 0.13;
                client.world.addParticleClient(i % 3 == 0 ? BULLET_HOT : BULLET,
                        end.x + vx * 0.35, end.y + vy * 0.35, end.z + vz * 0.35, vx, vy, vz);
            }
        }
        shooter.swingHand(Hand.MAIN_HAND);
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || !mc.player.getMainHandStack().isOf(Items.BOW)) return;
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        int cx = w / 2, cy = h / 2;
        int gap = !lockName.isEmpty() ? 2 : 4, arm = 4;
        int cross = !lockName.isEmpty() ? 0xFFFFD56A : 0xE8FFFFFF;
        ctx.fill(cx-gap-arm,cy,cx-gap,cy+1,cross); ctx.fill(cx+gap+1,cy,cx+gap+arm+1,cy+1,cross);
        ctx.fill(cx,cy-gap-arm,cx+1,cy-gap,cross); ctx.fill(cx,cy+gap+1,cx+1,cy+gap+arm+1,cross);

        String ammo = (autoMode ? "AUTO 120m  " : "SEMI  ") + magazine + "/" + MAG_SIZE;
        int tx = w - mc.textRenderer.getWidth(ammo) - 12, ty = h - 39;
        ctx.fill(tx-5,ty-4,w-7,ty+11,0x88000000);
        ctx.drawTextWithShadow(mc.textRenderer, ammo, tx, ty, 0xFFFFFFFF);
        if (!lockName.isEmpty()) {
            String lock = "LOCK  " + lockName + "  " + lockDistance + "m";
            ctx.drawTextWithShadow(mc.textRenderer, lock, (w-mc.textRenderer.getWidth(lock))/2, cy+18, 0xFFFFD56A);
        }
        if (reloadTicks > 0) {
            int bw=58,bx=w-bw-12,by=h-22;
            float p=reloadProgress(0.0F);
            ctx.fill(bx,by,bx+bw,by+3,0x66000000);
            ctx.fill(bx,by,bx+Math.max(1,(int)(bw*p)),by+3,0xFFFFFFFF);
            ctx.drawTextWithShadow(mc.textRenderer,"RELOAD",bx,by-10,0xFFFFFFFF);
        }
    }

    private static void startReload(MinecraftClient client) {
        reloadTicks=RELOAD_TICKS_MAX; inspectTicks=0; recoilTicks=0; releasedThisUse=true;
        reloadStopSent=false; semiLatched=true;
        if (client.player!=null) client.player.sendMessage(Text.literal("ŞARJÖR DEĞİŞTİRİLİYOR…"),true);
    }

    private static void resetTransient() {
        reloadTicks=inspectTicks=recoilTicks=restartDelay=virtualFireCooldown=muzzleFlashTicks=0;
        releasedThisUse=reloadStopSent=semiLatched=false;
    }
    private static void resetAll() { magazine=MAG_SIZE; resetTransient(); }

    public static float recoilProgress(float tickDelta) {
        if (recoilTicks<=0) return 0.0F;
        return MathHelper.clamp((recoilTicks-tickDelta)/6.0F,0.0F,1.0F);
    }
    public static float reloadProgress(float tickDelta) {
        if (reloadTicks<=0) return 0.0F;
        return 1.0F-MathHelper.clamp((reloadTicks-tickDelta)/(float)RELOAD_TICKS_MAX,0.0F,1.0F);
    }
    public static float inspectProgress(float tickDelta) {
        if (inspectTicks<=0) return 0.0F;
        return 1.0F-MathHelper.clamp((inspectTicks-tickDelta)/(float)INSPECT_TICKS_MAX,0.0F,1.0F);
    }
    public static boolean isReloading(){return reloadTicks>0;}
    public static boolean isInspecting(){return inspectTicks>0;}
    public static boolean isAutoMode(){return autoMode;}
    public static int magazine(){return magazine;}
    public static int magazineSize(){return MAG_SIZE;}
    public static int muzzleFlashTicks(){return muzzleFlashTicks;}
}
