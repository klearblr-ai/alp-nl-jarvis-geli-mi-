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
        int x = this.width / 2 - 110;
        int y = this.height / 2 - 82;

        ButtonWidget auto = ButtonWidget.builder(
                Text.literal("AUTO Müzik: " + BrutalFinisherClient.getAutoModeName()),
                button -> {
                    BrutalFinisherClient.toggleAutoMode();
                    button.setMessage(Text.literal("AUTO Müzik: " + BrutalFinisherClient.getAutoModeName()));
                }).dimensions(x, y, 220, 20).build();
        addDrawableChild(auto);

        ButtonWidget song = ButtonWidget.builder(
                Text.literal("Manuel Şarkı: " + BrutalFinisherClient.getBackgroundTrackName()),
                button -> {
                    BrutalFinisherClient.cycleBackgroundTrack();
                    button.setMessage(Text.literal("Manuel Şarkı: " + BrutalFinisherClient.getBackgroundTrackName()));
                }).dimensions(x, y + 30, 220, 20).build();
        addDrawableChild(song);

        addDrawableChild(ButtonWidget.builder(Text.literal("▶ Manuel Çal"), button ->
                BrutalFinisherClient.playBackgroundSelected()
        ).dimensions(x, y + 60, 106, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("■ Durdur"), button ->
                BrutalFinisherClient.stopBackgroundMusic()
        ).dimensions(x + 114, y + 60, 106, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Kapat"), button ->
                MinecraftClient.getInstance().setScreen(null)
        ).dimensions(x, y + 94, 220, 20).build());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
