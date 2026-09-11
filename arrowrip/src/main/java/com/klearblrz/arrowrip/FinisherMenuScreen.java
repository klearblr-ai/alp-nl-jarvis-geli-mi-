package com.klearblrz.arrowrip;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class FinisherMenuScreen extends Screen {
    public FinisherMenuScreen() {
        super(Text.literal("ArrowRip Finisher + Müzik"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 110;
        int y = this.height / 2 - 92;

        ButtonWidget mode = ButtonWidget.builder(
                Text.literal("Finisher: " + BrutalFinisherClient.getModeName()),
                button -> {
                    BrutalFinisherClient.cycleMode();
                    button.setMessage(Text.literal("Finisher: " + BrutalFinisherClient.getModeName()));
                }).dimensions(x, y, 220, 20).build();
        addDrawableChild(mode);

        ButtonWidget finisherSong = ButtonWidget.builder(
                Text.literal("Finisher şarkısı: " + BrutalFinisherClient.getFinisherTrackName()),
                button -> {
                    BrutalFinisherClient.cycleFinisherTrack();
                    button.setMessage(Text.literal("Finisher şarkısı: " + BrutalFinisherClient.getFinisherTrackName()));
                }).dimensions(x, y + 28, 220, 20).build();
        addDrawableChild(finisherSong);

        ButtonWidget normalSong = ButtonWidget.builder(
                Text.literal("Dinlenecek şarkı: " + BrutalFinisherClient.getBackgroundTrackName()),
                button -> {
                    BrutalFinisherClient.cycleBackgroundTrack();
                    button.setMessage(Text.literal("Dinlenecek şarkı: " + BrutalFinisherClient.getBackgroundTrackName()));
                }).dimensions(x, y + 62, 220, 20).build();
        addDrawableChild(normalSong);

        addDrawableChild(ButtonWidget.builder(Text.literal("▶ Çal"), button ->
                BrutalFinisherClient.playBackgroundSelected()
        ).dimensions(x, y + 90, 106, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("■ Durdur"), button ->
                BrutalFinisherClient.stopBackgroundMusic()
        ).dimensions(x + 114, y + 90, 106, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Finisher Test"), button ->
                BrutalFinisherClient.previewCurrentTarget(MinecraftClient.getInstance())
        ).dimensions(x, y + 122, 220, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Kapat"), button ->
                MinecraftClient.getInstance().setScreen(null)
        ).dimensions(x, y + 150, 220, 20).build());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
