package com.klearblrz.fingers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class FingersClient implements ClientModInitializer {
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("fingersonly", "main"));

    private static KeyBinding menuKey;
    private static KeyBinding poseKey;

    public static float length = 1.00F;
    public static float width = 1.00F;
    public static float thickness = 1.00F;
    public static float spread = 1.00F;
    public static float curl = 0.00F;
    public static boolean closed = false;
    public static boolean autoGrip = true;

    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("fingersonly.properties");

    @Override
    public void onInitializeClient() {
        load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fingersonly.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
        poseKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fingersonly.pose", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                client.setScreen(new FingerSettingsScreen());
            }
            while (poseKey.wasPressed()) {
                closed = !closed;
                save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(closed ? "EL: YUMRUK" : "EL: AÇIK"), true);
                }
            }
        });
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void resetDefaults() {
        length = 1.00F;
        width = 1.00F;
        thickness = 1.00F;
        spread = 1.00F;
        curl = 0.00F;
        closed = false;
        autoGrip = true;
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG.getParent());
            Properties p = new Properties();
            p.setProperty("length", Float.toString(length));
            p.setProperty("width", Float.toString(width));
            p.setProperty("thickness", Float.toString(thickness));
            p.setProperty("spread", Float.toString(spread));
            p.setProperty("curl", Float.toString(curl));
            p.setProperty("closed", Boolean.toString(closed));
            p.setProperty("autoGrip", Boolean.toString(autoGrip));
            try (OutputStream out = Files.newOutputStream(CONFIG)) {
                p.store(out, "Fingers Only settings");
            }
        } catch (Exception ignored) {
        }
    }

    private static void load() {
        if (!Files.exists(CONFIG)) return;
        try (InputStream in = Files.newInputStream(CONFIG)) {
            Properties p = new Properties();
            p.load(in);
            length = clamp(Float.parseFloat(p.getProperty("length", "1.0")), 0.55F, 1.65F);
            width = clamp(Float.parseFloat(p.getProperty("width", "1.0")), 0.55F, 1.55F);
            thickness = clamp(Float.parseFloat(p.getProperty("thickness", "1.0")), 0.55F, 1.55F);
            spread = clamp(Float.parseFloat(p.getProperty("spread", "1.0")), 0.35F, 1.80F);
            curl = clamp(Float.parseFloat(p.getProperty("curl", "0.0")), 0.0F, 1.0F);
            closed = Boolean.parseBoolean(p.getProperty("closed", "false"));
            autoGrip = Boolean.parseBoolean(p.getProperty("autoGrip", "true"));
        } catch (Exception ignored) {
            resetDefaults();
        }
    }
}
