package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public final class MouthOverlayClient implements ClientModInitializer {
    private static final Identifier MOUTH = Identifier.of("arrowrip", "textures/mouth_grin.png");
    private static KeyBinding mouthKey, upKey, downKey, leftKey, rightKey, forwardKey, backKey, biggerKey, smallerKey, resetKey;
    private static boolean enabled = true;
    private static double offsetX = 0.0;
    private static double offsetY = 0.0;
    private static double offsetZ = 0.0;
    private static float scale = 1.0f;

    @Override
    public void onInitializeClient() {
        mouthKey = key("key.arrowrip.mouth", GLFW.GLFW_KEY_M);
        upKey = key("key.arrowrip.mouth_up", GLFW.GLFW_KEY_UP);
        downKey = key("key.arrowrip.mouth_down", GLFW.GLFW_KEY_DOWN);
        leftKey = key("key.arrowrip.mouth_left", GLFW.GLFW_KEY_LEFT);
        rightKey = key("key.arrowrip.mouth_right", GLFW.GLFW_KEY_RIGHT);
        forwardKey = key("key.arrowrip.mouth_forward", GLFW.GLFW_KEY_PAGE_UP);
        backKey = key("key.arrowrip.mouth_back", GLFW.GLFW_KEY_PAGE_DOWN);
        biggerKey = key("key.arrowrip.mouth_bigger", GLFW.GLFW_KEY_KP_ADD);
        smallerKey = key("key.arrowrip.mouth_smaller", GLFW.GLFW_KEY_KP_SUBTRACT);
        resetKey = key("key.arrowrip.mouth_reset", GLFW.GLFW_KEY_HOME);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (mouthKey.wasPressed()) enabled = !enabled;
            boolean changed = false;
            while (upKey.wasPressed()) { offsetY += 0.015; changed = true; }
            while (downKey.wasPressed()) { offsetY -= 0.015; changed = true; }
            while (leftKey.wasPressed()) { offsetX -= 0.015; changed = true; }
            while (rightKey.wasPressed()) { offsetX += 0.015; changed = true; }
            while (forwardKey.wasPressed()) { offsetZ -= 0.008; changed = true; }
            while (backKey.wasPressed()) { offsetZ += 0.008; changed = true; }
            while (biggerKey.wasPressed()) { scale = Math.min(2.0f, scale + 0.05f); changed = true; }
            while (smallerKey.wasPressed()) { scale = Math.max(0.35f, scale - 0.05f); changed = true; }
            while (resetKey.wasPressed()) { offsetX = offsetY = offsetZ = 0.0; scale = 1.0f; changed = true; }
            if (changed && client.player != null) {
                client.player.sendMessage(Text.literal(String.format("Ağız: X %.3f  Y %.3f  Z %.3f  Boyut %.2fx", offsetX, offsetY, offsetZ, scale)), true);
            }
        });

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!enabled) return;
            try {
                renderMouth(context.matrices(), context.consumers().getBuffer(RenderLayers.entityTranslucent(MOUTH)));
            } catch (Throwable ignored) {}
        });
    }

    private static KeyBinding key(String name, int glfw) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, glfw, ArrowRipClient.CATEGORY));
    }

    private static void renderMouth(MatrixStack matrices, VertexConsumer vertices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.getPerspective().isFirstPerson()) return;

        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        double x = client.player.getX() - camera.x;
        double y = client.player.getY() - camera.y + 1.62;
        double z = client.player.getZ() - camera.z;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-client.player.getYaw()));
        matrices.translate(offsetX, offsetY, -0.326 + offsetZ);
        Matrix4f m = matrices.peek().getPositionMatrix();

        float halfW = 0.31f * scale;
        float halfH = 0.105f * scale;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        vertices.vertex(m, -halfW,  halfH, 0.0f).texture(0.0f, 0.0f).color(0xFFFFFFFF).light(light).normal(0.0f, 0.0f, -1.0f);
        vertices.vertex(m,  halfW,  halfH, 0.0f).texture(1.0f, 0.0f).color(0xFFFFFFFF).light(light).normal(0.0f, 0.0f, -1.0f);
        vertices.vertex(m,  halfW, -halfH, 0.0f).texture(1.0f, 1.0f).color(0xFFFFFFFF).light(light).normal(0.0f, 0.0f, -1.0f);
        vertices.vertex(m, -halfW, -halfH, 0.0f).texture(0.0f, 1.0f).color(0xFFFFFFFF).light(light).normal(0.0f, 0.0f, -1.0f);
        matrices.pop();
    }
}
