package com.codevibers.ecoquest.view;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

import com.codevibers.ecoquest.model.Sfx;

/** Synthesised beeps, so the game needs no sound files. */
public final class Audio {
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sfx");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean muted;

    private Audio() { }

    private static final int RATE = 22050;
    /** Each effect is synthesised once, then reused (single sfx thread, so a plain map is safe). */
    private static final java.util.Map<Sfx, byte[]> BUFFERS = new java.util.EnumMap<>(Sfx.class);

    private static byte[] synth(Sfx sfx) {
        int rate = RATE;
        int n = (int) (rate * sfx.seconds);
        byte[] buf = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double t = i / (double) rate;
            double ph = (sfx.hz * t) % 1;
            double s = switch (sfx.wave) {
                case 0 -> Math.sin(2 * Math.PI * ph);
                case 1 -> 4 * Math.abs(ph - .5) - 1;
                case 2 -> ph < .5 ? 1 : -1;
                default -> 2 * ph - 1;
            };
            double env = sfx.volume * Math.pow(0.0001 / sfx.volume, t / sfx.seconds);
            short v = (short) (s * Math.min(.35, env * 5) * 32767);
            buf[i * 2] = (byte) v;
            buf[i * 2 + 1] = (byte) (v >> 8);
        }
        return buf;
    }

    public static void setMuted(boolean m) { muted = m; }
    public static boolean muted() { return muted; }

    public static void play(Sfx sfx) {
        if (muted) return;
        POOL.submit(() -> {
            try {
                byte[] buf = BUFFERS.computeIfAbsent(sfx, Audio::synth);
                Clip clip = AudioSystem.getClip();
                clip.open(new AudioFormat(RATE, 16, 1, true, false), buf, 0, buf.length);
                clip.addLineListener(e -> { if (e.getType() == LineEvent.Type.STOP) clip.close(); });
                clip.start();
            } catch (Throwable ignored) { }
        });
    }
}
