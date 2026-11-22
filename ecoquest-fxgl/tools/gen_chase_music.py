"""Writes assets/music/chase.wav: an original, seamless 8-bar chase loop (140 bpm, G, chromatic and tense).
Run:  python tools/gen_chase_music.py   (needs numpy)"""
import os, wave
import numpy as np

SR = 44100
BPM = 140
STEP = 60.0 / BPM / 4                      # one 16th note
BARS = 8
N = int(round(SR * STEP * 16 * BARS))
TAIL = SR                                  # rendered past the end, then folded back so the loop has no seam
buf = np.zeros(N + TAIL)
rng = np.random.default_rng(3)


def hz(m): return 440.0 * 2 ** ((m - 69) / 12)


def add(t0, sig):
    i = int(round(t0 * SR))
    j = min(len(buf), i + len(sig))
    if j > i: buf[i:j] += sig[:j - i]


def osc(f, n, kind, detune=0.0):
    t = np.arange(n) / SR
    ph = (f * t) % 1
    if kind == "saw":   s = 2 * ph - 1
    elif kind == "sq":  s = np.where(ph < .5, 1.0, -1.0)
    elif kind == "p25": s = np.where(ph < .25, 1.0, -1.0)
    else:               s = np.sin(2 * np.pi * ph)
    if detune:
        s = (s + osc(f * (1 + detune), n, kind)) * .5
    return s


def env(n, a, d, sus, rel_start):
    t = np.arange(n) / SR
    e = np.minimum(1, t / max(a, 1e-4)) * (sus + (1 - sus) * np.exp(-t / d))
    r = np.clip((n / SR - t) / max(n / SR - rel_start, 1e-4), 0, 1)
    return e * np.minimum(1, r)


def note(step, m, steps, kind, vol, a=.004, d=.12, sus=.6, detune=0.0, vib=0.0):
    n = int(SR * STEP * steps)
    f = hz(m)
    if vib:
        t = np.arange(n) / SR
        f = f * (1 + vib * np.sin(2 * np.pi * 6 * t) * np.minimum(1, t / .15))
        ph = np.cumsum(f) / SR
        s = np.where((ph % 1) < .25, 1.0, -1.0) if kind == "p25" else np.sin(2 * np.pi * ph)
    else:
        s = osc(f, n, kind, detune)
    add(step * STEP, s * env(n, a, d, sus, STEP * steps * .85) * vol)


def kick(step, vol=.9):
    n = int(SR * .22); t = np.arange(n) / SR
    ph = 2 * np.pi * np.cumsum(48 + 110 * np.exp(-t / .03)) / SR
    add(step * STEP, np.sin(ph) * np.exp(-t / .11) * vol)


def snare(step, vol=.55):
    n = int(SR * .2); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate(([0], noise[:-1])) * .6
    add(step * STEP, (noise * np.exp(-t / .06) * .8 + np.sin(2 * np.pi * 190 * t) * np.exp(-t / .05) * .6) * vol)


def hat(step, vol=.16, open_=False):
    n = int(SR * (.18 if open_ else .045)); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate(([0], noise[:-1]))
    add(step * STEP, noise * np.exp(-t / (.07 if open_ else .014)) * vol)


# -------------------------------------------------------------------- harmony (G, with chromatic neighbours for tension)
ROOT = [43, 43, 44, 42, 43, 43, 39, 38]                      # G G Ab F# | G G Eb D
STAB = {43: (55, 58, 62), 44: (56, 59, 63), 42: (54, 57, 61), 39: (51, 55, 58), 38: (50, 54, 57)}
N_ = None
LEAD = [
    [79, 78, 79, 80, 79, N_, 74, N_, 82, 81, 82, 84, 82, N_, 79, N_],
    [79, 78, 79, 80, 79, N_, 86, N_, 85, 84, 82, N_, 79, N_, 77, N_],
    [80, 79, 80, 82, 80, N_, 75, N_, 83, 82, 83, 85, 83, N_, 80, N_],
    [78, 77, 78, 79, 78, N_, 73, N_, 81, 80, 81, 83, 81, N_, 78, 77],
    [79, 82, 86, 82, 79, 82, 86, 82, 79, 82, 86, 82, 79, 82, 84, 82],
    [79, 82, 86, 82, 79, 82, 86, 82, 80, 83, 87, 83, 80, 83, 86, 83],
    [75, 79, 82, 79, 75, 79, 82, 79, 77, 80, 84, 80, 77, 80, 84, 80],
    [74, 78, 81, 78, 74, 78, 81, 78, 74, 78, 81, 86, 85, 84, 82, 81],
]

for bar in range(BARS):
    b0 = bar * 16
    root = ROOT[bar]
    # driving 8th-note bass with octave jumps
    for k in range(0, 16, 2):
        m = root + (12 if k in (6, 14) else 0)
        note(b0 + k, m, 1.8, "saw", .30, a=.003, d=.09, sus=.35)
    # sub
    note(b0, root - 12, 16, "sine", .22, a=.01, d=5, sus=1)
    # off-beat chord stabs
    for k in (3, 7, 10, 15):
        for m in STAB[root]:
            note(b0 + k, m, 1.2, "sq", .055, a=.002, d=.07, sus=.1)
    # lead
    line = LEAD[bar]
    for k, m in enumerate(line):
        if m is None: continue
        hold = 1
        while k + hold < 16 and line[k + hold] is None: hold += 1
        note(b0 + k, m, hold * .95, "p25", .085, a=.003, d=.2, sus=.55, vib=.004 if hold > 1 else 0)
    # drums
    for k in (0, 8): kick(b0 + k)
    for k in (4, 12): snare(b0 + k)
    for k in (6, 10, 14 if bar % 2 else 15): kick(b0 + k, .55)
    for k in range(16): hat(b0 + k, .17 if k % 2 == 0 else .09, open_=(k == 14))
    if bar == BARS - 1:                                         # fill into the loop
        for k in (13, 14, 15): snare(b0 + k, .5)

out = buf[:N].copy()
out[:TAIL] += buf[N:N + TAIL]                                   # fold the tail onto the start: seamless loop
out = np.tanh(out * 1.25)
out *= .85 / np.max(np.abs(out))
dst = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "music", "chase.wav")
os.makedirs(os.path.dirname(dst), exist_ok=True)
with wave.open(dst, "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((out * 32767).astype("<i2").tobytes())
print("wrote", os.path.normpath(dst), "%.1fs" % (N / SR))
