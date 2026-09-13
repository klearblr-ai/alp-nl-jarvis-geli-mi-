package com.klearblrz.fingerplus;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class FingerSettingsScreen extends Screen {
    public FingerSettingsScreen() {
        super(Text.literal("FingerPlus - Parmak Düzenleyici"));
    }

    @Override
    protected void init() {
        int x = width / 2 - 120;
        int y = height / 2 - 112;

        ButtonWidget title = ButtonWidget.builder(Text.literal("✋ FingerPlus • Parmak Şekli"), b -> {})
                .dimensions(x, y, 240, 20).build();
        title.active = false;
        addDrawableChild(title);

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Parmaklar: " + (FingerPlusClient.enabled() ? "AÇIK" : "KAPALI")), b -> {
                    FingerPlusClient.toggleEnabled();
                    clearAndInit();
                }).dimensions(x, y + 28, 240, 20).build());

        addPair(x, y + 56, "Uzunluk " + FingerPlusClient.pct(FingerPlusClient.length()),
                () -> FingerPlusClient.changeLength(-0.05F), () -> FingerPlusClient.changeLength(0.05F));
        addPair(x, y + 84, "Kalınlık " + FingerPlusClient.pct(FingerPlusClient.thickness()),
                () -> FingerPlusClient.changeThickness(-0.05F), () -> FingerPlusClient.changeThickness(0.05F));
        addPair(x, y + 112, "Parmak Aralığı " + FingerPlusClient.pct(FingerPlusClient.spacing()),
                () -> FingerPlusClient.changeSpacing(-0.05F), () -> FingerPlusClient.changeSpacing(0.05F));
        addPair(x, y + 140, "Kıvrılma " + FingerPlusClient.curlText(),
                () -> FingerPlusClient.changeCurl(-0.05F), () -> FingerPlusClient.changeCurl(0.05F));
        addPair(x, y + 168, "Yürüme/Koşma Hareketi " + FingerPlusClient.pct(FingerPlusClient.movement()),
                () -> FingerPlusClient.changeMovement(-0.10F), () -> FingerPlusClient.changeMovement(0.10F));

        addDrawableChild(ButtonWidget.builder(Text.literal("Sıfırla"), b -> {
            FingerPlusClient.resetSettings();
            clearAndInit();
        }).dimensions(x, y + 198, 116, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Kapat"), b ->
                MinecraftClient.getInstance().setScreen(null)
        ).dimensions(x + 124, y + 198, 116, 20).build());
    }

    private void addPair(int x, int y, String label, Runnable minus, Runnable plus) {
        ButtonWidget labelButton = ButtonWidget.builder(Text.literal(label), b -> {})
                .dimensions(x, y, 154, 20).build();
        labelButton.active = false;
        addDrawableChild(labelButton);

        addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> {
            minus.run();
            clearAndInit();
        }).dimensions(x + 160, y, 36, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            plus.run();
            clearAndInit();
        }).dimensions(x + 202, y, 38, 20).build());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
