package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public final class MouthOverlayClient implements ClientModInitializer {
    private static final Identifier MOUTH = Identifier.of("arrowrip", "textures/mouth_grin.png");
    private static KeyBinding mouthKey;
    private static boolean enabled = true;

    @Override
    public void onInitializeClient() {
        mouthKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.arrowrip.mouth", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, ArrowRipClient.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (mouthKey.wasPressed()) enabled = !enabled;
        });

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!enabled) return;
            try {
                renderMouth(context.matrices(), context.consumers().getBuffer(RenderLayer.getEntityTranslucent(MOUTH)));
            } catch (Throwable ignored) {
                // Cosmetic overlay must never crash the client.
            }
        });
    }

    private static void renderMouth(MatrixStack matrices, VertexConsumer vertices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.getPerspective().isFirstPerson()) return;

        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        double x = client.player.getX() - camera.x;
        double y = client.player.getY() - camera.y + 1.62;
        double z = client.player.getZ() - camera.z;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-client.player.getYaw()));

        // Slightly in front of the face so it does not z-fight with the skin.
        matrices.translate(0.0, 0.0, -0.326);
        Matrix4f m = matrices.peek().getPositionMatrix();

        float halfW = 0.31f;
        float halfH = 0.105f;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        vertices.vertex(m, -halfW,  halfH, 0.0f).texture(0.0f, 0.0f).color(0xFFFFFFFF).light(light).normal(0.0f, 0.0f, -1.0f);
        vertices.vertex(m,  halfW,  halfH, 0.0f).texture(1.0f, 0.0f).color(0xFFFFFFFF).light(light).normal(0.0f, 0.0f, -1.0f);
        vertices.vertex(m,  halfW, -halfH, 0.0f).texture(1.0f, 1.0f).color(0xFFFFFFFF).light(light).normal(0.0f, 0.0f, -1.0f);
        vertices.vertex(m, -halfW, -halfH, 0.0f).texture(0.0f, 1.0f).color(0xFFFFFFFF).light(light).normal(0.0f, 0.0f, -1.0f);

        matrices.pop();
    }
}
