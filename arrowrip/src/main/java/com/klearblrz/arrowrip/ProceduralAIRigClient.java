package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Lightweight client-side motion brain. It does not replay authored animation clips:
 * every tick it derives continuous motion intent from movement, combat, aim and health.
 */
public final class ProceduralAIRigClient implements ClientModInitializer {
    private static float combat;
    private static float energy;
    private static float focus;
    private static float strain;
    private static float turnVelocity;
    private static float lastYaw;
    private static boolean initialized;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(ProceduralAIRigClient::tick);
    }

    private static void tick(MinecraftClient client) {
        PlayerEntity p = client.player;
        if (p == null || client.world == null) {
            combat = energy = focus = strain = turnVelocity = 0.0F;
            initialized = false;
            return;
        }

        float yaw = p.getYaw();
        if (!initialized) {
            lastYaw = yaw;
            initialized = true;
        }
        float rawTurn = MathHelper.wrapDegrees(yaw - lastYaw) / 35.0F;
        lastYaw = yaw;
        turnVelocity = smooth(turnVelocity, MathHelper.clamp(rawTurn, -1.0F, 1.0F), 0.26F);

        double horizontal = Math.sqrt(p.getVelocity().x * p.getVelocity().x + p.getVelocity().z * p.getVelocity().z);
        float wantedEnergy = MathHelper.clamp((float)(horizontal * 4.6), 0.0F, 1.0F);
        if (p.isSprinting()) wantedEnergy = Math.max(wantedEnergy, 0.82F);
        if (!p.isOnGround()) wantedEnergy = Math.max(wantedEnergy, 0.45F);

        Entity target = client.targetedEntity;
        float wantedFocus = 0.0F;
        float wantedCombat = 0.0F;
        if (target != null && target != p) {
            float distance = p.distanceTo(target);
            wantedFocus = MathHelper.clamp(1.0F - distance / 9.0F, 0.0F, 1.0F);
            if (distance < 5.0F) wantedCombat = 0.62F + 0.38F * wantedFocus;
        }
        if (client.options.attackKey.isPressed()) wantedCombat = 1.0F;
        if (p.handSwinging) wantedCombat = 1.0F;
        if (AutoPvPAnimationClient.getFinisherTicks() > 0) wantedCombat = 1.0F;

        float hp = p.getHealth() / Math.max(1.0F, p.getMaxHealth());
        float wantedStrain = MathHelper.clamp(1.0F - hp * 1.20F, 0.0F, 1.0F);
        if (AutoPvPAnimationClient.getHurtTicks() > 0) wantedStrain = 1.0F;

        energy = smooth(energy, wantedEnergy, 0.18F);
        focus = smooth(focus, wantedFocus, 0.16F);
        combat = smooth(combat, wantedCombat, wantedCombat > combat ? 0.30F : 0.09F);
        strain = smooth(strain, wantedStrain, 0.12F);
    }

    private static float smooth(float from, float to, float speed) {
        return from + (to - from) * MathHelper.clamp(speed, 0.0F, 1.0F);
    }

    public static float combat() { return combat; }
    public static float energy() { return energy; }
    public static float focus() { return focus; }
    public static float strain() { return strain; }
    public static float turnVelocity() { return turnVelocity; }
}
