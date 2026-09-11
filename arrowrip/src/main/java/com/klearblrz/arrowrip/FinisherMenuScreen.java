package com.klearblrz.arrowrip;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class FinisherMenuScreen extends Screen {
    public FinisherMenuScreen() {
        super(Text.literal("ArrowRip Finisher"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 62;

        ButtonWidget mode = ButtonWidget.builder(
                Text.literal("Finisher: " + BrutalFinisherClient.getModeName()),
                button -> {
                    BrutalFinisherClient.cycleMode();
                    button.setMessage(Text.literal("Finisher: " + BrutalFinisherClient.getModeName()));
                }).dimensions(x, y, 200, 20).build();
        addDrawableChild(mode);

        ButtonWidget song = ButtonWidget.builder(
                Text.literal("Şarkı: " + BrutalFinisherClient.getTrackName()),
                button -> {
                    BrutalFinisherClient.cycleTrack();
                    button.setMessage(Text.literal("Şarkı: " + BrutalFinisherClient.getTrackName()));
                }).dimensions(x, y + 26, 200, 20).build();
        addDrawableChild(song);

        addDrawableChild(ButtonWidget.builder(Text.literal("Finisher Test"), button -> {
            BrutalFinisherClient.previewCurrentTarget(MinecraftClient.getInstance());
            MinecraftClient.getInstance().setScreen(null);
        }).dimensions(x, y + 52, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Kapat"), button ->
                MinecraftClient.getInstance().setScreen(null)
        ).dimensions(x, y + 84, 200, 20).build());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
