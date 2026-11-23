"""Writes the four campaign background loops into assets/music/ (44.1 kHz, 16-bit, stereo WAV):
  levels_1_3_bgm.wav  Residential Area / Public Park / River   110 bpm  B major        16 bars (34.9 s)
  levels_4_6_bgm.wav  Industrial Zone / Market Rush / Beach    145 bpm  E major/C#m    20 bars (33.1 s)
  levels_7_9_bgm.wav  Toxic Landfill / Arctic / Jungle Delta   100 bpm  C major/A minor 16 bars (38.4 s)
  level_10_bgm.wav    Mega City (final stage)                  185 bpm  Ab major/C min  28 bars (36.3 s)
Every loop is a whole number of bars, and the reverb tail is folded back onto the start, so it repeats without a click.
Run:  python tools/gen_level_music.py [1|2|3|4 ...]   (needs numpy)"""
import os, sys, wave
import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "music")
rng = np.random.default_rng(7)


# ------------------------------------------------------------------ helpers
def hz(m): return 440.0 * 2 ** ((m - 69) / 12)
def tt(n): return np.arange(n) / SR
def noise(n, hp=1.0):
    x = rng.standard_normal(n)
    return x - hp * np.concatenate(([0], x[:-1]))
def gate(n, rel=.01): return np.clip((n / SR - tt(n)) / rel, 0, 1)
def atk(t, a): return np.minimum(1, t / a)
def scale(pcs): return [m for m in range(24, 120) if m % 12 in pcs]


def mel_note(sc, pc, off, center, raise_=None):
    """Note `off` scale steps above the chord root (the root octave nearest `center`)."""
    root = min((pc + 12 * k for k in range(2, 9)), key=lambda x: abs(x - center))
    m = sc[sc.index(root) + off]
    if raise_ and m % 12 in raise_: m += raise_[m % 12]
    return m


class Mix:
    def __init__(self, bpm, bars):
        self.step = 60.0 / bpm / 4                       # one 16th note, in seconds
        self.n = int(round(SR * self.step * 16 * bars))
        self.buf = np.zeros((self.n + SR * 5, 2))

    def add(self, step, sig, vol=1.0, pan=0.0):
        i = int(round(step * self.step * SR))
        j = min(len(self.buf), i + len(sig))
        if i < 0 or j <= i: return
        a = (pan + 1) * np.pi / 4
        s = sig[:j - i] * vol
        self.buf[i:j, 0] += s * np.cos(a)
        self.buf[i:j, 1] += s * np.sin(a)

    def save(self, name, echo=(), rms=.17, drive=1.0):
        out = self.buf[:self.n].copy()
        out[:len(self.buf) - self.n] += self.buf[self.n:]            # fold the tail onto the start: seamless loop
        base = out.copy()
        for dl, dr, g in echo:                                       # circular echoes: stereo and still seamless
            out[:, 0] += g * np.roll(base[:, 0], int(dl * SR))
            out[:, 1] += g * np.roll(base[:, 1], int(dr * SR))
        out *= rms / np.sqrt((out ** 2).mean())
        out = np.tanh(out * drive)
        out *= min(1.0, .92 / np.max(np.abs(out)))
        os.makedirs(OUT, exist_ok=True)
        path = os.path.join(OUT, name)
        with wave.open(path, "wb") as w:
            w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
            w.writeframes((out * 32767).astype("<i2").tobytes())
        print("wrote %-20s %.1fs  rms %.3f  peak %.2f" % (name, self.n / SR, np.sqrt((out ** 2).mean()), np.abs(out).max()))


# ------------------------------------------------------------------ instruments (mono arrays)
def marimba(m, dur=.7):
    n = int(SR * dur); t = tt(n); f = hz(m)
    s = (np.sin(2 * np.pi * f * t) * np.exp(-t / .22) + .5 * np.sin(2 * np.pi * 4 * f * t) * np.exp(-t / .07)
         + .15 * np.sin(2 * np.pi * 10 * f * t) * np.exp(-t / .025))
    return (s * atk(t, .001) + noise(n) * np.exp(-t / .003) * .08) * gate(n, .02)


def epiano(m, dur=.5):
    n = int(SR * dur); t = tt(n); f = hz(m)
    s = (np.sin(2 * np.pi * f * t) * np.exp(-t / .6) + .4 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / .3)
         + .12 * np.sin(2 * np.pi * 7 * f * t) * np.exp(-t / .04))
    return s * atk(t, .004) * gate(n, .03)


def stab(notes, dur=.3): return sum(epiano(m, dur) for m in notes) / len(notes) * 1.6


def warm_bass(m, dur=.4):
    n = int(SR * dur); t = tt(n); f = hz(m)
    s = np.sin(2 * np.pi * f * t) + .3 * np.sin(2 * np.pi * 2 * f * t) + .08 * np.sin(2 * np.pi * 3 * f * t)
    return s * atk(t, .004) * (.4 + .6 * np.exp(-t / .15)) * gate(n, .03)


def bell(m, dur=2.5, tau=1.0):
    n = int(SR * dur); t = tt(n); f = hz(m)
    s = (np.sin(2 * np.pi * f * t) * np.exp(-t / (.7 * tau)) + .35 * np.sin(2 * np.pi * 2.76 * f * t) * np.exp(-t / (.35 * tau))
         + .15 * np.sin(2 * np.pi * 5.4 * f * t) * np.exp(-t / (.15 * tau)) + .08 * np.sin(2 * np.pi * 8.9 * f * t) * np.exp(-t / (.07 * tau)))
    return s * atk(t, .002) * gate(n, .05)


def pluck(m, dur=1.2, tau=.5):
    n = int(SR * dur); t = tt(n); f = hz(m)
    s = (np.sin(2 * np.pi * f * t) * np.exp(-t / tau) + .3 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / (.45 * tau))
         + .08 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t / (.2 * tau)))
    return s * atk(t, .003) * gate(n, .05)


def pad(m, dur):
    n = int(SR * dur); t = tt(n); f = hz(m)
    s = np.sin(2 * np.pi * f * .9975 * t) + np.sin(2 * np.pi * f * 1.0025 * t) + .3 * np.sin(2 * np.pi * 2 * f * t) + .1 * np.sin(2 * np.pi * 3 * f * t)
    return s * np.minimum(1, t / .9) * gate(n, 1.0)


def phase(f, n, slide=0.0, tau=.04):
    t = tt(n)
    return np.cumsum(f * 2 ** (-slide / 12 * np.exp(-t / tau))) / SR


def saw(ph): return 2 * (ph % 1) - 1
def pulse(ph, duty): return np.where(ph % 1 < duty, 1.0, -1.0)


def surf(m, dur=.3, slide=0.0):
    n = int(SR * dur); t = tt(n); ph = phase(hz(m), n, slide)
    return (.55 * saw(ph) + .3 * pulse(ph, .3)) * atk(t, .002) * np.exp(-t / .22) * gate(n, .01)


def organ(notes, dur):
    n = int(SR * dur); t = tt(n); out = np.zeros(n)
    for m in notes:
        f = hz(m)
        out += sum(w * np.sin(2 * np.pi * h * f * t) for h, w in ((1, 1), (2, .6), (3, .35), (4, .2)))
    return out / len(notes) * atk(t, .004) * (.55 + .45 * np.exp(-t / .07)) * gate(n, .015)


def surf_bass(m, dur):
    n = int(SR * dur); t = tt(n); ph = phase(hz(m), n)
    return (saw(ph) * .7 + pulse(ph, .5) * .4) * atk(t, .003) * (.5 + .5 * np.exp(-t / .12)) * gate(n, .015)


def chip(m, dur, duty=.125):
    n = int(SR * dur); t = tt(n); ph = phase(hz(m), n)
    s = pulse(ph, duty) * .7 + pulse(ph * 2, .5) * .25
    return np.tanh(s * 1.8) * atk(t, .001) * (.6 + .4 * np.exp(-t / .1)) * gate(n, .008)


def toy(m, dur=.25):
    n = int(SR * dur); t = tt(n); f = hz(m)
    s = np.sin(2 * np.pi * f * t) + .5 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t / .05) + .25 * np.sin(2 * np.pi * 5.2 * f * t) * np.exp(-t / .03)
    return (s * np.exp(-t / .09) * atk(t, .001) + noise(n) * np.exp(-t / .003) * .1) * gate(n, .01)


def sub(m, dur):
    n = int(SR * dur); t = tt(n); f = hz(m); ph = phase(f, n, 7, .03)
    s = np.sin(2 * np.pi * ph) + .45 * np.sin(2 * np.pi * 2 * ph) * np.exp(-t / .2) + .25 * saw(ph)
    return np.tanh(s * 1.8) * atk(t, .002) * (.6 + .4 * np.exp(-t / .1)) * gate(n, .012)


def kick(pitch=46, sweep=120, decay=.12, dur=.25):
    n = int(SR * dur); t = tt(n)
    return np.sin(2 * np.pi * np.cumsum(pitch + sweep * np.exp(-t / .03)) / SR) * np.exp(-t / decay)


def snare(dur=.22, tone=190, nd=.07):
    n = int(SR * dur); t = tt(n)
    return (noise(n, .6) * np.exp(-t / nd) * .9 + np.sin(2 * np.pi * tone * t) * np.exp(-t / .05) * .6)


def rim():
    n = int(SR * .06); t = tt(n)
    return np.sin(2 * np.pi * 1700 * t) * np.exp(-t / .008) * .7 + noise(n) * np.exp(-t / .006) * .5


def hat(open_=False):
    n = int(SR * (.18 if open_ else .04)); t = tt(n)
    return noise(n) * np.exp(-t / (.07 if open_ else .012))


def shaker():
    n = int(SR * .07); t = tt(n)
    return noise(n) * np.exp(-t / .028) * atk(t, .008)


def tom(f0):
    n = int(SR * .28); t = tt(n)
    return np.sin(2 * np.pi * np.cumsum(f0 * (1 + .6 * np.exp(-t / .04))) / SR) * np.exp(-t / .11)


def crash(dur=1.5):
    n = int(SR * dur); t = tt(n)
    return noise(n, .9) * np.exp(-t / .5)


def ride():
    n = int(SR * .35); t = tt(n)
    return noise(n) * np.exp(-t / .1) * .5 + np.sin(2 * np.pi * 3300 * t) * np.exp(-t / .08) * .12


# ------------------------------------------------------------------ 1. levels 1-3: sunny, bouncy, B major, 110 bpm
def track1():
    mx = Mix(110, 16); S = mx.step
    SC = scale({11, 1, 3, 4, 6, 8, 10})
    CH = {"B": (11, 47, (59, 63, 66, 70)), "G#m": (8, 44, (56, 59, 63, 66)),
          "E": (4, 40, (56, 59, 63, 64)), "F#": (6, 42, (58, 61, 64, 66))}       # (root pc, bass, maj7 stab voicing)
    PROG = ["B", "G#m", "E", "F#"] * 2 + ["E", "F#", "B", "G#m"] + ["E", "F#", "B", "F#"]
    MOT = {"A": [(0, 4, 2), (3, 2, 1), (4, 4, 2), (6, 5, 2), (8, 4, 2), (11, 2, 1), (12, 0, 3)],
           "B": [(0, 7, 2), (2, 5, 1), (3, 4, 1), (6, 5, 2), (8, 7, 2), (10, 5, 1), (12, 4, 2), (14, 2, 2)],
           "C": [(0, 4, 2), (2, 5, 1), (4, 7, 2), (6, 5, 2), (8, 4, 2), (10, 2, 1), (12, 4, 1), (14, 5, 2)]}
    PLAN = "AAAC" "AABC" "BBAA" "BBAC"
    for bar in range(16):
        b0 = bar * 16; pc, bass, voic = CH[PROG[bar]]
        for st, off, ln in MOT[PLAN[bar]]:
            mx.add(b0 + st, marimba(mel_note(SC, pc, off, 70), .45 + ln * S), .24 if st % 4 == 0 else .19, -.25)
        for k in (2, 6, 10, 14):                                                  # maj7 stabs on the off-beats
            mx.add(b0 + k, stab(voic, .3), .075, .3)
        for k, iv in ((0, 0), (3, 0), (4, 7), (8, 0), (11, 0), (12, 7)):            # root-fifth walk
            mx.add(b0 + k, warm_bass(bass + iv, .4), .30)
        for k in (0, 7, 10): mx.add(b0 + k, kick(46, 90, .1), .5)                   # soft indie-pop groove
        for k in (4, 12): mx.add(b0 + k, rim(), .22, .1)
        for k in range(16): mx.add(b0 + k, shaker(), .05 if k % 2 == 0 else .09, .35)
    mx.save("levels_1_3_bgm.wav", echo=[(.21, .26, .12), (.43, .49, .07)])


# ------------------------------------------------------------------ 2. levels 4-6: surf-rock / arcade, E major, 145 bpm
def track2():
    mx = Mix(145, 20); S = mx.step
    SC = scale({4, 6, 8, 9, 11, 1, 3})
    CH = {"E": (4, 40, (52, 56, 59)), "C#m": (1, 37, (49, 52, 56)), "A": (9, 45, (57, 61, 64)),
          "B": (11, 47, (59, 63, 66)), "G#m": (8, 44, (56, 59, 63))}
    PROG = ["E", "C#m", "A", "B"] * 2 + ["C#m", "A", "E", "B"] + ["A", "B", "C#m", "G#m"] + ["E", "C#m", "A", "B"]
    MOT = {"A": [(0, 4, 4, "t"), (4, 5, 1, "s"), (6, 4, 1, "n"), (8, 2, 2, "s"), (10, 4, 1, "n"), (12, 5, 2, "n"), (14, 7, 2, "s")],
           "B": [(0, 7, 2, "s"), (2, 5, 1, "n"), (3, 4, 1, "n"), (4, 5, 2, "n"), (6, 4, 1, "n"), (8, 2, 4, "t"), (12, 4, 2, "s"), (14, 5, 2, "n")],
           "C": [(0, 0, 1, "n"), (1, 1, 1, "n"), (2, 2, 1, "n"), (3, 3, 1, "n"), (4, 4, 1, "n"), (5, 5, 1, "n"), (6, 6, 1, "n"), (7, 7, 1, "n"),
                 (8, 8, 4, "t"), (12, 7, 1, "n"), (13, 5, 1, "n"), (14, 4, 1, "n"), (15, 2, 1, "n")]}
    PLAN = "ABAC" "ABAC" "BBAC" "ABAB" "BBAC"
    for bar in range(20):
        b0 = bar * 16; pc, bass, tri = CH[PROG[bar]]
        for st, off, ln, sty in MOT[PLAN[bar]]:
            note = mel_note(SC, pc, off, 70)
            if sty == "t":                                                         # tremolo picking
                for k in range(int(ln * 2)):
                    mx.add(b0 + st + k * .5, surf(note, .11), .15 * (1 - .03 * k), -.1)
            else:                                                                  # quick slide up into the note
                mx.add(b0 + st, surf(note, .12 + ln * S, 2.0 if sty == "s" else 0.0), .2, -.1)
        for k in range(0, 16, 2):                                                  # driving eighth-note organ chords
            mx.add(b0 + k, organ(tri, 1.6 * S), .075 if k % 4 == 0 else .06, .35)
        for j, iv in enumerate((0, 12, 0, 12, 0, 12, 7, 12)):                      # bouncy surf bass
            mx.add(b0 + j * 2, surf_bass(bass + iv, 1.7 * S), .26)
        for k in (0, 8) + ((10,) if bar % 2 else (6,)): mx.add(b0 + k, kick(48, 120, .11), .85 if k % 8 == 0 else .55)
        for k in (4, 12): mx.add(b0 + k, snare(), .6)
        for k in range(0, 16, 2): mx.add(b0 + k, ride() if k % 4 == 0 else hat(), .08 if k % 4 == 0 else .05, .2)
        if bar % 4 == 0: mx.add(b0, crash(), .18)
        if bar % 4 == 3:                                                           # snare-roll fill
            for k in range(4): mx.add(b0 + 12 + k, snare(), .4 + .08 * k)
    mx.save("levels_4_6_bgm.wav", echo=[(.07, .09, .18), (.13, .16, .08)])


# ------------------------------------------------------------------ 3. levels 7-9: crystalline, A minor / C major, 100 bpm
def track3():
    mx = Mix(100, 16); S = mx.step
    SC = scale({0, 2, 4, 5, 7, 9, 11})
    CH = {"Am": (9, 45, (57, 60, 64, 69)), "F": (5, 41, (53, 57, 60, 65)), "C": (0, 48, (55, 60, 64, 67)),
          "G": (7, 43, (55, 59, 62, 67)), "Dm": (2, 38, (57, 62, 65, 69)), "E": (4, 40, (56, 59, 64, 68))}
    PROG = ["Am", "F", "C", "G"] * 2 + ["Dm", "Am", "F", "G"] + ["F", "G", "Am", "E"]
    MOT = {"A": [(0, 4, 4), (4, 2, 2), (6, 3, 2), (8, 4, 4), (12, 2, 4)],
           "B": [(0, 7, 4), (4, 6, 2), (6, 4, 2), (8, 5, 4), (12, 4, 2), (14, 2, 2)],
           "C": [(0, 2, 2), (2, 4, 2), (4, 6, 4), (8, 4, 2), (10, 2, 2), (12, 0, 4)]}
    PLAN = "ABAC" "ABAC" "BABC" "ABCA"
    ARP = [0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 2, 1, 0, 1, 2, 1]
    for bar in range(16):
        b0 = bar * 16; name = PROG[bar]; pc, bass, lad = CH[name]
        rz = {7: 1} if name == "E" else None                                       # G -> G# over the E chord
        for st, off, ln in MOT[PLAN[bar]]:
            mx.add(b0 + st, bell(mel_note(SC, pc, off, 72, rz), 2.6), .17 if st == 0 else .14, .15 if (st // 4) % 2 else -.15)
        for k in range(16):                                                        # flowing 16th-note arpeggio
            mx.add(b0 + k, pluck(lad[ARP[k]] + (12 if k % 8 == 7 else 0), 1.1), .06 if k % 4 else .075, -.35 + .7 * (k % 2))
        for j, m in enumerate((lad[0] - 12,) + lad[:3]):                           # lush pad, wide stereo
            mx.add(b0, pad(m, 16 * S + 1.0), .05, -.6 + .4 * j)
        for k in (0, 8): mx.add(b0 + k, kick(44, 60, .09, .2), .30)                # muted pulse
        for k in range(0, 16, 2): mx.add(b0 + k, hat(), .05, .4)                   # soft ticking hat
    mx.save("levels_7_9_bgm.wav", echo=[(.29, .37, .22), (.61, .71, .16), (.97, 1.09, .10)], rms=.15)


# ------------------------------------------------------------------ 4. level 10: manic, C minor, 185 bpm
def track4():
    mx = Mix(185, 28); S = mx.step
    SC = scale({0, 2, 3, 5, 7, 8, 10})
    CH = {"Cm": (0, 36, (60, 63, 67, 72)), "Ab": (8, 44, (56, 60, 63, 68)), "Eb": (3, 39, (63, 67, 70, 75)),
          "Bb": (10, 46, (58, 62, 65, 70)), "Fm": (5, 41, (53, 56, 60, 65)), "G": (7, 43, (55, 59, 62, 67))}
    PROG = (["Cm", "Ab", "Eb", "Bb"] * 2 + ["Fm", "Ab", "Cm", "G"] + ["Cm", "Ab", "Eb", "Bb"]
            + ["Fm", "Ab", "Cm", "G"] + ["Ab", "Bb", "Cm", "G"] + ["Cm", "Ab", "Bb", "G"])
    CALL = [[(0, 4), (1, 4), (3, 2), (4, 4), (6, 5)], [(0, 7), (2, 5), (3, 4), (4, 2), (5, 4), (6, 5), (7, 4)]]
    RESP = [[(8, 7), (9, 7), (11, 5), (12, 4), (14, 2)], [(8, 9), (9, 7), (10, 5), (12, 7), (13, 9), (14, 7), (15, 4)]]
    for bar in range(28):
        b0 = bar * 16; name = PROG[bar]; pc, bass, lad = CH[name]
        rz = {10: 1} if name == "G" else None                                      # Bb -> B natural over G
        fill = bar % 4 == 3
        notes = CALL[bar % 2] + [x for x in RESP[(bar // 2) % 2] if not (fill and x[0] >= 12)]
        for st, off in notes:                                                      # call (steps 0-7) and answer (8-15)
            mx.add(b0 + st, chip(mel_note(SC, pc, off, 62, rz), .55 * S), .15, -.2 if st < 8 else .2)
        for k, iv, ln in ((0, 0, 2), (3, 0, 1), (4, 12, 1), (6, 0, 1), (8, 0, 2), (11, 12, 1), (12, 0, 1), (14, 7, 1)):
            mx.add(b0 + k, sub(bass + iv, (ln + .6) * S), .40)                     # bouncing sub-bass
        if fill:                                                                   # ultra-fast arpeggio burst
            for j in range(8): mx.add(b0 + 12 + j * .5, toy(lad[(j % 4) if j < 4 else (7 - j)] + 12, .12), .10, -.5 + .14 * j)
        for k in rng.choice([1, 5, 9, 13], 2, replace=False):                      # glitchy bells
            mx.add(b0 + int(k), bell(int(rng.choice(lad)) + 24, .45, .15), .06, float(rng.uniform(-.7, .7)))
        ks, ss = ([0, 6, 8, 11], [4, 12]) if bar % 2 == 0 else ([0, 3, 8, 14], [4, 10, 12, 15])
        for k in ks: mx.add(b0 + k, kick(50, 130, .1), .85)
        for k in ss: mx.add(b0 + k, snare(), .5)
        for k in range(16): mx.add(b0 + k, hat(open_=(k == 14 and bar % 2)), .06 if k % 2 == 0 else .04, .25)
        if fill:
            for j in range(8): mx.add(b0 + 12 + j * .5, snare(.12), .22 + .04 * j)
        if bar % 8 == 0: mx.add(b0, crash(), .18)
    mx.save("level_10_bgm.wav", echo=[(.09, .11, .14)], drive=1.15)


if __name__ == "__main__":
    which = sys.argv[1:] or ["1", "2", "3", "4"]
    for w in which: {"1": track1, "2": track2, "3": track3, "4": track4}[w]()
