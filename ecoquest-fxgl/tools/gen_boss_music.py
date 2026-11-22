"""Writes assets/music/boss_queen.wav: an original, seamless 16-bar Queen Muck boss loop
(155 bpm, C# minor, 4/4): harpsichord baroque 16th-note arpeggios and runs, staccato synth-string minor/diminished
chords, relentless 8th-note octave bass, fast electronic/rock drums.
Run:  python tools/gen_boss_music.py   (needs numpy)"""
import os, wave
import numpy as np

SR = 44100
BPM = 155
STEP = 60.0 / BPM / 4                      # one 16th note
BARS = 16
N = int(round(SR * STEP * 16 * BARS))
TAIL = SR * 2                              # rendered past the end, then folded back so the loop has no seam
buf = np.zeros(N + TAIL)
rng = np.random.default_rng(21)


def hz(m): return 440.0 * 2 ** ((m - 69) / 12)


def add(t0, sig):
    i = int(round(t0 * SR))
    j = min(len(buf), i + len(sig))
    if j > i: buf[i:j] += sig[:j - i]


def saw(f, n, ph0=0.0):
    t = np.arange(n) / SR
    return 2 * ((f * t + ph0) % 1) - 1


def sq(f, n):
    t = np.arange(n) / SR
    return np.where((f * t) % 1 < .5, 1.0, -1.0)


def harp(step, m, vol=.10, steps=3.0):
    """Plucked harpsichord: bright saw + octave-up register, fast decay, tiny key click."""
    n = int(SR * STEP * steps); t = np.arange(n) / SR
    f = hz(m)
    s = .55 * saw(f, n) + .25 * saw(f * 1.004, n) + .35 * saw(f * 2, n) + .15 * sq(f * 4, n)
    e = np.exp(-t / .13) * (1 - np.exp(-t / .0012))
    click = rng.standard_normal(n) * np.exp(-t / .004) * .12
    add(step * STEP, (s * e + click) * vol)


def strings(step, ms, steps=1.1, vol=.05):
    """Staccato synth-string stab: detuned saws, sharp attack, short body."""
    n = int(SR * STEP * steps); t = np.arange(n) / SR
    e = np.minimum(1, t / .006) * np.exp(-t / .09) * np.clip((n / SR - t) / .02, 0, 1)
    out = np.zeros(n)
    for m in ms:
        f = hz(m)
        out += saw(f * .997, n) + saw(f * 1.003, n) + .5 * saw(f * 2.0, n)
    add(step * STEP, out * e * vol / len(ms) * 2.2)


def bass(step, m, steps=1.8, vol=.30):
    n = int(SR * STEP * steps); t = np.arange(n) / SR
    e = np.minimum(1, t / .003) * (.35 + .65 * np.exp(-t / .09)) * np.clip((n / SR - t) / .02, 0, 1)
    add(step * STEP, (saw(hz(m), n) * .8 + np.sin(2 * np.pi * hz(m) * t) * .4) * e * vol)


def sub(step, m, steps, vol=.20):
    n = int(SR * STEP * steps); t = np.arange(n) / SR
    e = np.minimum(1, t / .01) * np.clip((n / SR - t) / .02, 0, 1)
    add(step * STEP, np.sin(2 * np.pi * hz(m) * t) * e * vol)


def kick(step, vol=.9):
    n = int(SR * .22); t = np.arange(n) / SR
    ph = 2 * np.pi * np.cumsum(46 + 125 * np.exp(-t / .03)) / SR
    add(step * STEP, np.sin(ph) * np.exp(-t / .12) * vol)


def snare(step, vol=.6):
    n = int(SR * .22); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate(([0], noise[:-1])) * .6
    add(step * STEP, (noise * np.exp(-t / .065) * .9 + np.sin(2 * np.pi * 200 * t) * np.exp(-t / .05) * .6) * vol)


def tom(step, f0, vol=.5):
    n = int(SR * .28); t = np.arange(n) / SR
    ph = 2 * np.pi * np.cumsum(f0 * (1 + .6 * np.exp(-t / .04))) / SR
    add(step * STEP, np.sin(ph) * np.exp(-t / .11) * vol)


def hat(step, vol=.15, open_=False):
    n = int(SR * (.18 if open_ else .04)); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate(([0], noise[:-1]))
    add(step * STEP, noise * np.exp(-t / (.07 if open_ else .012)) * vol)


def crash(step, vol=.33):
    n = int(SR * 1.6); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate(([0], noise[:-1])) * .9
    add(step * STEP, noise * np.exp(-t / .5) * vol)


# ------------------------------------------------------------------ harmony (C# minor, harmonic: G# major, B#dim7)
CH = {
    "C#m":  (61, 64, 68),
    "F#m":  (54, 57, 61),
    "A":    (57, 61, 64),
    "E":    (64, 68, 71),
    "B":    (59, 63, 66),
    "G#":   (56, 60, 63),
    "B#o7": (60, 63, 66, 69),            # C  D#  F#  A  - the dramatic diminished
}
ROOT = {"C#m": 37, "F#m": 42, "A": 33, "E": 40, "B": 35, "G#": 44, "B#o7": 36}
PROG = ["C#m", "C#m", "A", "G#", "C#m", "F#m", "B#o7", "G#",
        "C#m", "E", "A", "B", "F#m", "B#o7", "G#", "G#"]

# C# harmonic minor scale for the runs
SCALE = [m for m in range(60, 100) if (m % 12) in (1, 3, 4, 6, 8, 9, 0)]


def lift(m, lo=69):
    while m < lo: m += 12
    return m


def ladder(ch):
    l = sorted(lift(t) for t in CH[ch])
    return l + [l[0] + 12]


A_PAT = [0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 2, 1, 2, 3, 2, 1]        # up-down arpeggio
B_PAT = [0, 2, 1, 2, 0, 3, 1, 3, 0, 2, 1, 2, 0, 3, 2, 3]        # broken-chord figure


def run(start_m, direction, length=16):
    i = min(range(len(SCALE)), key=lambda k: abs(SCALE[k] - start_m))
    return [SCALE[max(0, min(len(SCALE) - 1, i + direction * k))] for k in range(length)]


def run_updown(start_m):
    up = run(start_m, 1, 9)
    return up + up[-2::-1][:7]


def bar_notes(bar):
    ch = PROG[bar]
    l = ladder(ch)
    plan = {0: "A", 1: "B", 2: "A", 3: ("up", 68), 4: "A", 5: "B", 6: ("down", 93), 7: ("ud", 68),
            8: "A+", 9: "B+", 10: "A+", 11: "B+", 12: ("down", 96), 13: ("up", 70), 14: "B+", 15: ("ud", 72)}[bar]
    if plan == "A":  return [l[i] for i in A_PAT]
    if plan == "B":  return [l[i] for i in B_PAT]
    if plan == "A+": return [l[i] + 12 if i == 3 else l[i] for i in A_PAT]
    if plan == "B+": return [l[i] + 12 if i == 3 else l[i] for i in B_PAT]
    kind, m0 = plan
    if kind == "up":   return run(m0, 1)
    if kind == "down": return run(m0, -1)
    return run_updown(m0)


for bar in range(BARS):
    b0 = bar * 16
    ch = PROG[bar]
    root = ROOT[ch]
    # lead: harpsichord, relentless 16ths, accented on the beat
    for k, m in enumerate(bar_notes(bar)):
        harp(b0 + k, m, vol=.11 if k % 4 == 0 else .085)
    # staccato strings
    for k in (0, 3, 6, 8, 11, 14):
        strings(b0 + k, CH[ch], vol=.06 if k in (0, 8) else .045)
    # bass: relentless 8th-note octave pumping
    for k in range(0, 16, 2):
        bass(b0 + k, root + (12 if (k // 2) % 2 else 0))
    sub(b0, root - 12, 16)
    # drums
    for k in (0, 8): kick(b0 + k)
    for k in (6, 10, 14 if bar % 2 else 15): kick(b0 + k, .55)
    for k in (4, 12): snare(b0 + k)
    for k in range(16): hat(b0 + k, .17 if k % 2 == 0 else .09, open_=(k == 14))
    if bar % 4 == 0: crash(b0)
    if bar in (7, 15):                                                            # drum fills
        for k, f in zip((12, 13, 14, 15), (210, 170, 130, 95)): tom(b0 + k, f)
        snare(b0 + 15, .6)

out = buf[:N].copy()
out[:len(buf) - N] += buf[N:]                                                     # fold the tail onto the start: seamless loop
out = np.tanh(out * 1.25)
out *= .85 / np.max(np.abs(out))
dst = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "music", "boss_queen.wav")
os.makedirs(os.path.dirname(dst), exist_ok=True)
with wave.open(dst, "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((out * 32767).astype("<i2").tobytes())
print("wrote", os.path.normpath(dst), "%.1fs" % (N / SR))
