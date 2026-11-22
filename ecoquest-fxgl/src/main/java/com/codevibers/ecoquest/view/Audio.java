package com.codevibers.ecoquest.view;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

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

    /**
     * Queen Muck's laugh, modelled on a reference laugh: five bright, squeaky bursts, each starting with a short
     * burst of hiss and then a voice that sweeps upward in pitch while it fades. Later bursts start lower. The voice is
     * a few harmonics of a sweeping pitch; the strong 2nd and 4th harmonics give the thin, bright sound.
     */
    private static byte[] laughSynth(Sfx sfx) {
        double[] start = {0, .25, .49, .73, .97};
        double[] from = {900, 850, 750, 620, 550}, to = {1750, 1600, 1250, 900, 775};   // pitch sweep per burst (Hz)
        double[] harmonic = {.24, 1, .04, .37, .11, .13};
        double len = .15;
        int n = (int) (RATE * sfx.seconds);
        double[] out = new double[n];
        java.util.Random hiss = new java.util.Random(7);
        double phase = 0, lastNoise = 0, peak = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            int k = 0;
            while (k < start.length - 1 && t >= start[k + 1]) k++;
            double u = t - start[k];
            if (u >= len) continue;
            double sweep = Math.min(1, u / (len * .85));
            phase += from[k] * Math.pow(to[k] / from[k], sweep) / RATE;
            double amp = (1 - .45 * Math.min(1, u / .11)) * Math.min(1, (len - u) / .04);
            double voice = 0;
            for (int h = 1; h <= harmonic.length; h++) {
                if (h * to[k] < RATE * .45) voice += harmonic[h - 1] * Math.sin(2 * Math.PI * h * phase);
            }
            double noise = hiss.nextDouble() * 2 - 1;
            double bright = noise - lastNoise;                  // differencing the noise keeps only the high frequencies
            lastNoise = noise;
            out[i] = voice * amp + bright * .5 * Math.exp(-u / .012);
            peak = Math.max(peak, Math.abs(out[i]));
        }
        byte[] buf = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            short v = (short) (out[i] / peak * .4 * 32767);
            buf[i * 2] = (byte) v;
            buf[i * 2 + 1] = (byte) (v >> 8);
        }
        return buf;
    }

    private static byte[] synth(Sfx sfx) {
        if (sfx == Sfx.LAUGH) return laughSynth(sfx);
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

    public static void setMuted(boolean m) {
        muted = m;
        if (music != null) music.setMute(m);
    }
    public static boolean muted() { return muted; }

    // ------------------------------------------------------------------ looping background music (assets/music/*.wav)
    private static MediaPlayer music;
    private static String musicName;

    /** Starts a looping track (call from the JavaFX thread). Does nothing if it is already playing. */
    public static void playMusic(String file, double volume) {
        if (file.equals(musicName) && music != null) return;
        stopMusic();
        try {
            String url = musicUrl(file);
            if (url == null) { System.err.println("Missing music: " + file); return; }
            MediaPlayer p = new MediaPlayer(new Media(url));
            p.setOnError(() -> System.err.println("Music error in " + file + ": " + p.getError()));
            p.setCycleCount(MediaPlayer.INDEFINITE);
            p.setVolume(volume);
            p.setMute(muted);
            p.play();
            music = p;
            musicName = file;
        } catch (Throwable t) {
            System.err.println("Could not play music " + file + ": " + t);
        }
    }

    /**
     * Resolves a music resource. JavaFX can refuse a WAV that carries extra RIFF chunks after the audio (metadata
     * added by some tools), so if there are any, the file is rewritten with just the fmt and data chunks into a temp file.
     */
    private static String musicUrl(String file) throws Exception {
        var url = Audio.class.getResource("/assets/music/" + file);
        if (url == null) return null;
        byte[] d;
        try (var in = url.openStream()) { d = in.readAllBytes(); }
        if (d.length > 44 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F') {
            int fmtOff = -1, fmtLen = 0, dataOff = -1, dataLen = 0, extra = 0;
            for (int i = 12; i + 8 <= d.length; ) {
                int len = (d[i + 4] & 255) | (d[i + 5] & 255) << 8 | (d[i + 6] & 255) << 16 | (d[i + 7] & 255) << 24;
                String id = new String(d, i, 4, java.nio.charset.StandardCharsets.US_ASCII);
                int body = i + 8;
                if (id.equals("fmt ")) { fmtOff = body; fmtLen = len; }
                else if (id.equals("data")) { dataOff = body; dataLen = Math.min(len, d.length - body); }
                else extra++;
                if (len < 0) break;
                i = body + len + (len & 1);
            }
            if (extra > 0 && fmtOff >= 0 && dataOff >= 0) {
                int riff = 4 + 8 + fmtLen + 8 + dataLen;
                java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(8 + riff).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                b.put("RIFF".getBytes()).putInt(riff).put("WAVE".getBytes());
                b.put("fmt ".getBytes()).putInt(fmtLen).put(d, fmtOff, fmtLen);
                b.put("data".getBytes()).putInt(dataLen).put(d, dataOff, dataLen);
                java.nio.file.Path tmp = java.nio.file.Files.createTempFile("ecoquest-", ".wav");
                java.nio.file.Files.write(tmp, b.array());
                tmp.toFile().deleteOnExit();
                return tmp.toUri().toString();
            }
        }
        return url.toExternalForm();
    }

    /** Stops the music only if this track is the one playing. */
    public static void stopMusicIf(String file) {
        if (file.equals(musicName)) stopMusic();
    }

    public static void stopMusic() {
        if (music != null) {
            try { music.stop(); music.dispose(); } catch (Throwable ignored) { }
        }
        music = null;
        musicName = null;
    }

    /** Queen Muck's "ho ho ho". */
    public static void laugh() { play(Sfx.LAUGH); }

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
