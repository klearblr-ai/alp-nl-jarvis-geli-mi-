package com.klearblrz.fingers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class FingerSettingsScreen extends Screen {
    private static final int ROW = 24;

    public FingerSettingsScreen() {
        super(Text.literal("Parmak Ayarları"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 76;

        addAdjustPair(cx, y, () -> FingersClient.length = FingersClient.clamp(FingersClient.length - 0.05F, 0.55F, 1.65F),
                () -> FingersClient.length = FingersClient.clamp(FingersClient.length + 0.05F, 0.55F, 1.65F));
        y += ROW;
        addAdjustPair(cx, y, () -> FingersClient.width = FingersClient.clamp(FingersClient.width - 0.05F, 0.55F, 1.55F),
                () -> FingersClient.width = FingersClient.clamp(FingersClient.width + 0.05F, 0.55F, 1.55F));
        y += ROW;
        addAdjustPair(cx, y, () -> FingersClient.thickness = FingersClient.clamp(FingersClient.thickness - 0.05F, 0.55F, 1.55F),
                () -> FingersClient.thickness = FingersClient.clamp(FingersClient.thickness + 0.05F, 0.55F, 1.55F));
        y += ROW;
        addAdjustPair(cx, y, () -> FingersClient.spread = FingersClient.clamp(FingersClient.spread - 0.05F, 0.35F, 1.80F),
                () -> FingersClient.spread = FingersClient.clamp(FingersClient.spread + 0.05F, 0.35F, 1.80F));
        y += ROW;
        addAdjustPair(cx, y, () -> FingersClient.curl = FingersClient.clamp(FingersClient.curl - 0.05F, 0.00F, 1.00F),
                () -> FingersClient.curl = FingersClient.clamp(FingersClient.curl + 0.05F, 0.00F, 1.00F));

        y += 30;
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Auto Kavrama: " + (FingersClient.autoGrip ? "AÇIK" : "KAPALI")),
                b -> {
                    FingersClient.autoGrip = !FingersClient.autoGrip;
                    b.setMessage(Text.literal("Auto Kavrama: " + (FingersClient.autoGrip ? "AÇIK" : "KAPALI")));
                    FingersClient.save();
                }).dimensions(cx - 102, y, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal(FingersClient.closed ? "El: YUMRUK" : "El: AÇIK"),
                b -> {
                    FingersClient.closed = !FingersClient.closed;
                    b.setMessage(Text.literal(FingersClient.closed ? "El: YUMRUK" : "El: AÇIK"));
                    FingersClient.save();
                }).dimensions(cx + 2, y, 100, 20).build());

        y += 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Sıfırla"), b -> {
            FingersClient.resetDefaults();
            clearAndInit();
        }).dimensions(cx - 102, y, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Bitti"), b -> {
            FingersClient.save();
            if (client != null) client.setScreen(null);
        }).dimensions(cx + 2, y, 100, 20).build());
    }

    private void addAdjustPair(int cx, int y, Runnable minus, Runnable plus) {
        addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> {
            minus.run();
            FingersClient.save();
        }).dimensions(cx - 104, y, 28, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            plus.run();
            FingersClient.save();
        }).dimensions(cx + 76, y, 28, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        super.render(context, mouseX, mouseY, deltaTicks);

        int cx = width / 2;
        int y = height / 2 - 98;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("FINGERS ONLY — ŞEKİL AYARI"), cx, y, 0xFFFFFF);

        int rowY = height / 2 - 72;
        drawValue(context, "Uzunluk", FingersClient.length, rowY);
        drawValue(context, "Genişlik", FingersClient.width, rowY + ROW);
        drawValue(context, "Kalınlık", FingersClient.thickness, rowY + ROW * 2);
        drawValue(context, "Parmak Aralığı", FingersClient.spread, rowY + ROW * 3);
        drawValue(context, "Kıvrım", FingersClient.curl, rowY + ROW * 4);
    }

    private void drawValue(DrawContext context, String label, float value, int y) {
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(label + ": " + String.format(java.util.Locale.ROOT, "%.2f", value)),
                width / 2, y + 6, 0xE8E8E8);
    }
}
