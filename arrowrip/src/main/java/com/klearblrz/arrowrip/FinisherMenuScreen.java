package com.klearblrz.arrowrip;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class FinisherMenuScreen extends Screen {
    public FinisherMenuScreen() {
        super(Text.literal("ArrowRip Müzik"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 48;

        ButtonWidget song = ButtonWidget.builder(
                Text.literal("Şarkı: " + BrutalFinisherClient.getTrackName()),
                button -> {
                    BrutalFinisherClient.cycleTrack();
                    button.setMessage(Text.literal("Şarkı: " + BrutalFinisherClient.getTrackName()));
                }).dimensions(x, y, 200, 20).build();
        addDrawableChild(song);

        addDrawableChild(ButtonWidget.builder(Text.literal("Çal"), button ->
                BrutalFinisherClient.playSelected()
        ).dimensions(x, y + 30, 96, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Durdur"), button ->
                BrutalFinisherClient.stopMusic()
        ).dimensions(x + 104, y + 30, 96, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Kapat"), button ->
                MinecraftClient.getInstance().setScreen(null)
        ).dimensions(x, y + 64, 200, 20).build());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
