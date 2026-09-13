package com.klearblrz.fingerplus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class FingerPlusClient implements ClientModInitializer {
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("fingerplus", "main"));
    private static KeyBinding menuKey;
    private static KeyBinding toggleKey;

    private static boolean enabled = true;
    private static float length = 1.0F;
    private static float thickness = 1.0F;
    private static float spacing = 1.0F;
    private static float curl = 0.16F;
    private static float movement = 1.0F;

    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("fingerplus.properties");

    @Override
    public void onInitializeClient() {
        load();
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fingerplus.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fingerplus.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) client.setScreen(new FingerSettingsScreen());
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(enabled ? "Parmaklar: AÇIK" : "Parmaklar: KAPALI"), true);
                }
            }
        });
    }

    public static boolean enabled() { return enabled; }
    public static float length() { return length; }
    public static float thickness() { return thickness; }
    public static float spacing() { return spacing; }
    public static float curl() { return curl; }
    public static float movement() { return movement; }

    public static void toggleEnabled() { enabled = !enabled; save(); }
    public static void changeLength(float d) { length = MathHelper.clamp(length + d, 0.55F, 1.65F); save(); }
    public static void changeThickness(float d) { thickness = MathHelper.clamp(thickness + d, 0.55F, 1.60F); save(); }
    public static void changeSpacing(float d) { spacing = MathHelper.clamp(spacing + d, 0.65F, 1.45F); save(); }
    public static void changeCurl(float d) { curl = MathHelper.clamp(curl + d, 0.0F, 0.95F); save(); }
    public static void changeMovement(float d) { movement = MathHelper.clamp(movement + d, 0.0F, 1.50F); save(); }

    public static void resetSettings() {
        enabled = true;
        length = 1.0F;
        thickness = 1.0F;
        spacing = 1.0F;
        curl = 0.16F;
        movement = 1.0F;
        save();
    }

    public static String pct(float value) { return Math.round(value * 100.0F) + "%"; }
    public static String curlText() { return Math.round(curl * 100.0F) + "%"; }

    private static void load() {
        if (!Files.exists(CONFIG)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG)) {
            p.load(in);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            length = parse(p, "length", 1.0F, 0.55F, 1.65F);
            thickness = parse(p, "thickness", 1.0F, 0.55F, 1.60F);
            spacing = parse(p, "spacing", 1.0F, 0.65F, 1.45F);
            curl = parse(p, "curl", 0.16F, 0.0F, 0.95F);
            movement = parse(p, "movement", 1.0F, 0.0F, 1.50F);
        } catch (IOException ignored) { }
    }

    private static float parse(Properties p, String key, float fallback, float min, float max) {
        try { return MathHelper.clamp(Float.parseFloat(p.getProperty(key, Float.toString(fallback))), min, max); }
        catch (NumberFormatException e) { return fallback; }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("length", Float.toString(length));
        p.setProperty("thickness", Float.toString(thickness));
        p.setProperty("spacing", Float.toString(spacing));
        p.setProperty("curl", Float.toString(curl));
        p.setProperty("movement", Float.toString(movement));
        try {
            Files.createDirectories(CONFIG.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG)) { p.store(out, "FingerPlus"); }
        } catch (IOException ignored) { }
    }
}
