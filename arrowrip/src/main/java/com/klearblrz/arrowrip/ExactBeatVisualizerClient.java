package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.lang.reflect.Field;
import java.util.Arrays;

public final class ExactBeatVisualizerClient implements ClientModInitializer {
    private static final int[][] BEATS = {
            {2,11,20,29,39,48,57,66,75,85,94,103,112,122,131,140,149,159,168,177,186,196,205,214,223,232,242,251,260,269,278,288,297,304,311,320,329,339,348,357,366,375,385,394,403,412,422,431,440,449,459,468,477,486,496,505,514,523,532,542,551,560,569,578,587,597,606,616,625,634,643,652,662,671,680,689,699,708,717,726},
            {0,8,16,26,35,45,55,65,75,85,95,104,114,124,134,144,153,163,173,183,193,203,212,222,232,242,252,262,272,281,291,301,311,321,331,340,350,360,370,380,390,399,409,419,429,439,449,458,468,478,488,498,508,517,527,537,547,557,567,577,585,594,603,611,621,631,640,650,660,670,680,689,699,709,719,729,739,748,758,768},
            {1,11,21,30,40,50,60,70,80,89,99,109,119,129,139,148,158,168,178,188,197,207,217,227,237,247,257,266,276,286,296,306,315,325,335,345,355,365,374,384,394,404,414,424,433,443,453,463,473,483,492,502,512,522,532,542,551,561,571,581,591,601,610,620,630,640,652,660,669,679,689,699,709,719,731,743,753,763,773,783},
            {3,13,23,34,44,54,64,74,84,95,105,115,125,135,146,156,166,176,186,196,206,217,227,237,247,257,267,277,287,297,306,316,326,336,346,356,366,377,387,397,407,417,427,438,448,458,468,478,488,499,509,519,529,539,549,560,570,580,590,600,610,621,631,641,651,660,669,679,689,699,710,720,730,740,750,760,771,781,791,801},
            {2,12,22,32,42,52,62,73,83,93,103,113,124,134,144,154,164,175,185,195,205,215,225,235,246,256,266,276,286,296,307,316,327,336,345,355,365,375,385,395,406,416,426,435,444,454,464,474,484,495,505,515,525,535,545,555,566,576,586,596,607,616,627,637,647,657,667,678,688,698,708,718,728,739,749,759,769,779,789,799}
    };
    private static final double[] AVG_STEP = {9.26,9.76,9.80,10.10,10.09};
    private static Field activeTrackField, playbackTicksField, playingField;
    private static int frameSkip;

    @Override
    public void onInitializeClient() {
        try {
            activeTrackField = BrutalFinisherClient.class.getDeclaredField("activeTrack");
            playbackTicksField = BrutalFinisherClient.class.getDeclaredField("playbackTicks");
            playingField = BrutalFinisherClient.class.getDeclaredField("playing");
            activeTrackField.setAccessible(true);
            playbackTicksField.setAccessible(true);
            playingField.setAccessible(true);
            if (BrutalFinisherClient.isVisualizerEnabled()) BrutalFinisherClient.toggleVisualizer();
        } catch (Throwable ignored) {}
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
    }

    private static void render(DrawContext ctx) {
        try {
            if ((frameSkip++ & 1) != 0) return;
            if (activeTrackField == null || playbackTicksField == null || playingField == null) return;
            if (!(boolean) playingField.get(null)) return;
            int track = (int) activeTrackField.get(null);
            int tick = (int) playbackTicksField.get(null);
            if (track < 0 || track >= BEATS.length) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();
            int cx = w - 76;
            int cy = Math.max(16, h / 2 - 82) + 72;
            double pulse = envelope(track, tick);
            double bass = Math.pow(pulse, 0.72);
            int bars = 36;
            double base = 28.0;

            for (int i = 0; i < bars; i++) {
                double a = Math.PI * 2.0 * i / bars - Math.PI / 2.0;
                double shape = 0.80 + 0.20 * Math.sin(i * 0.71 + tick * 0.08);
                double amp = (3.0 + bass * 20.0) * shape;
                double inner = base - 1.0;
                double outer = base + amp;
                int color = pulse > 0.62 ? 0xFFFF2638 : 0xFFEAEAEA;
                radialFast(ctx, cx, cy, a, inner, outer, color);
            }

            circleFast(ctx, cx, cy, (int)Math.round(base + bass * 7.0), pulse > 0.45 ? 0xCCFF1018 : 0xAAFFFFFF);
            if (pulse > 0.72) circleFast(ctx, cx, cy, (int)Math.round(base + 10 + bass * 8.0), 0x88FF0000);
        } catch (Throwable ignored) {}
    }

    private static double envelope(int track, int tick) {
        int[] beats = BEATS[track];
        int beatTick;
        if (tick <= beats[beats.length - 1]) {
            int pos = Arrays.binarySearch(beats, tick);
            if (pos >= 0) beatTick = beats[pos];
            else {
                int ins = -pos - 1;
                if (ins <= 0) return 0.0;
                beatTick = beats[ins - 1];
            }
        } else {
            double step = AVG_STEP[track];
            int n = Math.max(0, (int)Math.floor((tick - beats[beats.length - 1]) / step));
            beatTick = (int)Math.round(beats[beats.length - 1] + n * step);
        }
        int dt = tick - beatTick;
        if (dt < 0 || dt > 6) return 0.0;
        return Math.exp(-dt * 0.92);
    }

    private static void radialFast(DrawContext ctx, int cx, int cy, double a, double r1, double r2, int color) {
        int steps = Math.max(1, (int)Math.ceil((r2-r1) / 2.0));
        for (int s=0;s<=steps;s++) {
            double r = r1 + (r2-r1) * (s/(double)steps);
            int x = (int)Math.round(cx + Math.cos(a)*r);
            int y = (int)Math.round(cy + Math.sin(a)*r);
            ctx.fill(x,y,x+1,y+1,color);
        }
    }

    private static void circleFast(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int i=0;i<80;i++) {
            double a = Math.PI*2*i/80.0;
            int x=(int)Math.round(cx+Math.cos(a)*r);
            int y=(int)Math.round(cy+Math.sin(a)*r);
            ctx.fill(x,y,x+1,y+1,color);
        }
    }
}
