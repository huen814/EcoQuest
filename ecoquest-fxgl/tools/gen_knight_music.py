"""Writes assets/music/knights_dialogue.wav: a seamless 16-bar loop for the Queen's Knight's dialogue scenes
(park, cell, streets, before the chase). 128 bpm, 4/4, D minor (harmonic minor, chromatic neighbours): tense, creeping, formal.
44.1 kHz, stereo, 16-bit PCM, exactly 16 bars (30.000 s) with no fades, so it loops cleanly.
Layers: harpsichord/pizzicato riff with half-step shifts, low reed counter-melody, staccato bass ostinato,
dry marching percussion (rimshot snare, muted toms, ticking hi-hat).
Run:  python tools/gen_knight_music.py [output folder]   (needs numpy)"""
import os, sys, wave
import numpy as np

SR = 44100
BPM = 128
STEP = 60.0 / BPM / 4                       # one 16th note; a 4/4 bar is 16 steps
BARS = 16
N = int(round(SR * STEP * 16 * BARS))       # 1,323,000 samples = 30.000 s
TAIL = SR * 3                               # rendered past the end, then folded back so the loop has no seam
buf = np.zeros((2, N + TAIL))
rng = np.random.default_rng(13)


def hz(m): return 440.0 * 2 ** ((m - 69) / 12)


def add(t0, sig, pan=0.0, vol=1.0):
    i = max(0, int(round(t0 * SR)))
    j = min(buf.shape[1], i + len(sig))
    if j <= i: return
    a = (pan + 1) * np.pi / 4
    buf[0, i:j] += sig[:j - i] * np.cos(a) * vol
    buf[1, i:j] += sig[:j - i] * np.sin(a) * vol


def hp_noise(n, k=1):
    x = rng.standard_normal(n)
    for _ in range(k): x = np.diff(x, prepend=0)
    return x


def pluck(step, m, vol, pan=-0.4, dur=1.4, echo=True):
    """Harpsichord / pizzicato: a bright, quickly decaying string with a doubled octave and a tiny jack click."""
    n = int(SR * STEP * dur); t = np.arange(n) / SR
    f = hz(m)
    s = np.zeros(n)
    for h in range(1, 9):
        tau = .11 / h ** .45
        s += (1 / h ** .8) * np.sin(2 * np.pi * h * f * t) * np.exp(-t / tau)
        s += .6 * (1 / h ** .8) * np.sin(2 * np.pi * h * f * 1.003 * t + .7) * np.exp(-t / tau)
    s += .35 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / .06)
    s += hp_noise(n) * .18 * np.exp(-t / .004)
    s *= np.clip((n / SR - t) / .01, 0, 1)
    s *= vol / 3.0
    t0 = step * STEP + rng.uniform(-.003, .003)
    add(t0, s, pan)
    if echo:
        add(t0 + 3 * STEP, s * .26, -pan + .3)


def reed(step, m, steps, vol, pan=.4):
    """Low reed / muted brass: band-limited saw with the top rolled off, slow attack, a little late vibrato and breath."""
    dur = STEP * steps
    n = int(SR * (dur + .12)); t = np.arange(n) / SR
    f = hz(m) * (1 + .003 * np.sin(2 * np.pi * 5.2 * t) * np.clip((t - .25) / .3, 0, 1))
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.zeros(n)
    for h in range(1, 14):
        s += (1 / h ** 1.35) * np.sin(h * ph) + (1 / h ** 1.35) * .7 * np.sin(h * ph * 1.004 + 1.1)
    s += 1.2 * np.sin(ph * 2) * np.exp(-t / 2)
    s += hp_noise(n) * .04
    e = np.minimum(1, t / .05) * np.clip((dur + .1 - t) / .1, 0, 1)
    add(step * STEP, s * e * vol / 4.0, pan)
    add(step * STEP + .011, s * e * vol / 4.0 * .7, -pan)        # a short delay on the other side widens the reed


def bass(step, m, vol):
    n = int(SR * .2); t = np.arange(n) / SR
    f = hz(m)
    s = np.sin(2 * np.pi * f * t) + .5 * np.sin(2 * np.pi * 2 * f * t) + .3 * np.sin(2 * np.pi * 3 * f * t)
    s += .35 * np.sin(2 * np.pi * f * (1 + .15 * np.exp(-t / .01)) * t)
    s *= np.minimum(1, t / .004) * np.exp(-t / .075) * np.clip((n / SR - t) / .02, 0, 1)
    add(step * STEP, s * vol / 1.6, 0.0)


def hat(step, vol, pan=.5):
    n = int(SR * .04); t = np.arange(n) / SR
    add(step * STEP, hp_noise(n, 2) * np.exp(-t / .008) * vol * .5, pan)


def rim(step, vol):
    n = int(SR * .12); t = np.arange(n) / SR
    s = hp_noise(n) * np.exp(-t / .012) * .7
    s += np.sin(2 * np.pi * 1750 * t) * np.exp(-t / .006) + .7 * np.sin(2 * np.pi * 480 * t) * np.exp(-t / .02)
    s += .4 * np.sin(2 * np.pi * 3100 * t) * np.exp(-t / .004)
    add(step * STEP, s * vol / 1.5, -.1)


def tom(step, vol, f0=120, pan=-.3):
    n = int(SR * .28); t = np.arange(n) / SR
    f = f0 * (.72 + .28 * np.exp(-t / .05))
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = (np.sin(ph) + .35 * np.sin(2 * ph)) * np.exp(-t / .075)
    s += hp_noise(n) * .12 * np.exp(-t / .006)
    s *= np.minimum(1, t / .002)
    add(step * STEP, s * vol, pan)


# ------------------------------------------------------------------ the music
ROOT = [38, 38, 43, 45,  38, 46, 43, 45,  46, 43, 40, 45,  38, 39, 45, 45]   # D D G A | D Bb G A | Bb G E A | D Eb A A

# riff shapes: (step, midi). Every bar is a variation on D C# D . . A . . / F E D . . Bb: a nervous tiptoe with half-step shifts
RIFF = {
    "Dm": [(0, 74), (2, 73), (3, 74), (6, 69), (8, 77), (10, 76), (11, 74), (14, 70)],
    "Dm2": [(0, 74), (2, 73), (3, 74), (6, 69), (8, 70), (9, 69), (10, 67), (12, 65), (14, 64)],
    "Gm": [(0, 67), (2, 66), (3, 67), (6, 62), (8, 70), (10, 69), (11, 67), (14, 63)],
    "A7": [(0, 73), (2, 72), (3, 73), (6, 69), (8, 76), (10, 75), (11, 76), (14, 73)],
    "Bb": [(0, 70), (2, 69), (3, 70), (6, 65), (8, 74), (10, 72), (11, 70), (14, 69)],
    "Edim": [(0, 76), (2, 75), (3, 76), (6, 70), (8, 79), (10, 78), (11, 79), (14, 76)],
    "Eb": [(0, 75), (2, 74), (3, 75), (6, 70), (8, 79), (10, 78), (11, 79), (14, 75)],
    "A7end": [(0, 73), (2, 72), (3, 73), (6, 69), (8, 76), (10, 75), (11, 76), (12, 73), (13, 69), (14, 73), (15, 76)],
}
SHAPE = ["Dm", "Dm2", "Gm", "A7",  "Dm", "Bb", "Gm", "A7",  "Bb", "Gm", "Edim", "A7",  "Dm", "Eb", "A7", "A7end"]

# counter-melody: (step, midi, length in steps)
COUNTER = [
    [(0, 62, 8), (8, 61, 4), (12, 62, 4)],
    [(0, 65, 8), (8, 64, 4), (12, 62, 4)],
    [(0, 67, 8), (8, 66, 4), (12, 67, 4)],
    [(0, 64, 8), (8, 61, 8)],
    [(0, 62, 8), (8, 63, 4), (12, 62, 4)],
    [(0, 65, 8), (8, 64, 4), (12, 65, 4)],
    [(0, 62, 8), (8, 60, 4), (12, 58, 4)],
    [(0, 57, 4), (4, 61, 4), (8, 64, 4), (12, 67, 4)],
    [(0, 70, 8), (8, 69, 4), (12, 70, 4)],
    [(0, 67, 8), (8, 65, 4), (12, 63, 4)],
    [(0, 64, 8), (8, 63, 4), (12, 62, 4)],
    [(0, 61, 8), (8, 64, 4), (12, 61, 4)],
    [(0, 62, 8), (8, 61, 4), (12, 60, 4)],
    [(0, 63, 8), (8, 62, 4), (12, 58, 4)],
    [(0, 61, 8), (8, 64, 8)],
    [(0, 57, 4), (4, 61, 4), (8, 64, 4), (12, 67, 4)],
]

for bar in range(BARS):
    b0 = bar * 16
    loud = 1.0 + (.15 if 8 <= bar < 12 else 0) + (.1 if bar >= 12 else 0)
    for st, m in RIFF[SHAPE[bar]]:
        pluck(b0 + st, m, .55 * loud * (1.15 if st == 0 else 1.0))
    for st, m, ln in COUNTER[bar]:
        reed(b0 + st, m - 12 if m > 66 else m, ln, .42 * loud)
    r = ROOT[bar]
    for st in range(0, 16, 2):
        note = r + (12 if st in (6, 14) else 0)
        bass(b0 + st, note, .62 if st == 0 else .5)
    for st in range(16):
        if st % 2 == 0: hat(b0 + st, .16 if st % 4 == 0 else .1)
        elif st % 4 == 3: hat(b0 + st, .045)
    rim(b0 + 4, .42); rim(b0 + 12, .45)
    tom(b0 + 0, .45, 110); tom(b0 + 10, .32, 138)
    if bar % 4 == 3:                          # a tight two-hit fill at the end of every phrase
        rim(b0 + 14, .22); rim(b0 + 15, .3)
        tom(b0 + 14, .3, 150); tom(b0 + 15, .36, 120)
    if bar == 15:
        tom(b0 + 13, .3, 165)

# ------------------------------------------------------------------ small room: early reflections, then fold the tail back so the loop has no seam
dry = buf.copy()
for ch, taps in ((0, (.037, .083, .131)), (1, (.043, .091, .149))):
    for d_, g in zip(taps, (.20, .13, .08)):
        k = int(d_ * SR)
        buf[ch, k:] += dry[ch, :-k] * g
        buf[1 - ch, k:] += dry[ch, :-k] * g * .35

out = buf[:, :N].copy()
out[:, :TAIL] += buf[:, N:N + TAIL]
out = np.tanh(out * 1.1)
out *= .88 / np.max(np.abs(out))
pcm = (out.T * 32767).astype("<i2")

folder = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "music")
os.makedirs(folder, exist_ok=True)
path = os.path.join(folder, "knights_dialogue.wav")
with wave.open(path, "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(pcm.tobytes())
print("wrote", path, "%.3f s" % (len(pcm) / SR))
