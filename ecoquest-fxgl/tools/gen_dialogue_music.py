"""Writes assets/music/queen_dialogue.wav: an original, seamless 16-bar loop for Queen Muck's dialogue scenes
(kidnap cutscene + palace entrance). ~104 bpm waltz in 3/4, A minor into C major: whimsical, delicate, bittersweet.
Layers: bell-like piano lead with grace notes, soft pad, gentle root-note bass, music-box tines + airy shaker.
Run:  python tools/gen_dialogue_music.py   (needs numpy)"""
import os, wave
import numpy as np

SR = 44100
BPM = 104
STEP = 60.0 / BPM / 2                      # one 8th note; a 3/4 bar is 6 steps
BAR = 6
BARS = 16
N = int(round(SR * STEP * BAR * BARS))
TAIL = SR * 3                              # rendered past the end, then folded back so the loop has no seam
buf = np.zeros(N + TAIL)
rng = np.random.default_rng(5)


def hz(m): return 440.0 * 2 ** ((m - 69) / 12)


def add(t0, sig):
    i = max(0, int(round(t0 * SR)))
    j = min(len(buf), i + len(sig))
    if j > i: buf[i:j] += sig[:j - i]


def bell(step, m, vol, dur=3.0, tau=1.0, off=0.0):
    """Soft bell-piano: a sine with decaying upper partials and a faint inharmonic ping."""
    n = int(SR * STEP * dur); t = np.arange(n) / SR
    f = hz(m)
    s = (np.sin(2 * np.pi * f * t) * np.exp(-t / (.55 * tau))
         + .35 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / (.28 * tau))
         + .12 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t / (.15 * tau))
         + .06 * np.sin(2 * np.pi * 5.4 * f * t) * np.exp(-t / (.06 * tau)))
    s *= (1 - np.exp(-t / .002)) * np.clip((n / SR - t) / .04, 0, 1)
    add((step + off) * STEP, s * vol)


def pad(step, ms, steps, vol):
    """Light strings: pairs of slightly detuned sines, slow swell, overlapping into the next chord."""
    n = int(SR * (STEP * steps + .5)); t = np.arange(n) / SR
    e = np.minimum(1, t / .3) * np.clip((n / SR - t) / .45, 0, 1)
    out = np.zeros(n)
    for m in ms:
        f = hz(m)
        out += np.sin(2 * np.pi * f * .998 * t) + np.sin(2 * np.pi * f * 1.002 * t) + .3 * np.sin(2 * np.pi * 2 * f * t)
    add(step * STEP, out * e * vol / len(ms))


def bass(step, m, vol, dur=2.6):
    n = int(SR * STEP * dur); t = np.arange(n) / SR
    f = hz(m)
    e = np.minimum(1, t / .01) * np.exp(-t / .55) * np.clip((n / SR - t) / .05, 0, 1)
    add(step * STEP, (np.sin(2 * np.pi * f * t) + .25 * np.sin(2 * np.pi * 2 * f * t)) * e * vol)


def shaker(step, vol):
    n = int(SR * .09); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate(([0], noise[:-1]))
    add(step * STEP, noise * np.exp(-t / .028) * np.minimum(1, t / .004) * vol)


# ------------------------------------------------------------------ harmony: A minor, then C major (E / E7 turns back home)
CH = {"Am": (57, 60, 64), "F": (53, 57, 60), "C": (55, 60, 64), "G": (55, 59, 62),
      "Dm": (57, 62, 65), "E": (56, 59, 62, 64)}
ROOT = {"Am": 45, "F": 41, "C": 48, "G": 43, "Dm": 38, "E": 40}
PROG = ["Am", "Am", "F", "C", "Dm", "Am", "E", "Am",
        "C", "G", "Am", "F", "Dm", "Am", "E", "E"]
N_ = None
MEL = [                                    # six 8th-note steps per bar
    [76, N_, 72, 74, 76, N_], [81, N_, 79, N_, 76, N_],
    [77, N_, 72, 74, 77, N_], [76, N_, 74, 72, 67, N_],
    [74, N_, 77, 76, 74, N_], [72, N_, 76, 74, 72, N_],
    [68, 71, 74, N_, 71, 68], [69, N_, N_, N_, N_, N_],
    [79, N_, 76, 79, 84, N_], [83, N_, 79, 74, 79, N_],
    [81, N_, 76, 72, 76, N_], [77, N_, 81, 77, 72, N_],
    [74, 77, 81, 77, 74, N_], [72, 76, 81, 76, 72, N_],
    [76, N_, 71, 68, 71, 74], [76, N_, 74, 71, 68, 71],
]
GRACE_BARS = (0, 2, 4, 8, 10, 12)          # a little grace-note ornament on the first note of these bars


def neighbour(m): return m + 1 if m % 12 in (4, 11) else m + 2


for bar in range(BARS):
    s0 = bar * BAR
    ch = PROG[bar]
    tones = CH[ch]
    # lead
    for k, m in enumerate(MEL[bar]):
        if m is None: continue
        if k == 0 and bar in GRACE_BARS:
            bell(s0, neighbour(m), .05, dur=.8, tau=.4, off=-.3)
        bell(s0 + k, m, .12)
    if bar == 7:                                           # little turn at the end of the first half
        for k, m in enumerate((71, 69, 71, 69)):
            bell(s0 + 3 + k * .5, m, .07, dur=1.2, tau=.5)
    # soft pad
    pad(s0, tones, BAR, .075)
    # gentle bass: root on beat 1, a lighter one on beat 3
    bass(s0, ROOT[ch], .28)
    bass(s0 + 4, ROOT[ch], .13, dur=1.6)
    # music-box tines on beats 2 and 3, airy shaker
    hi = sorted(t + 24 for t in tones)
    for j, k in enumerate((2, 4)):
        bell(s0 + k, hi[(bar + j) % len(hi)], .03, dur=1.5, tau=.35)
    shaker(s0 + 2, .035); shaker(s0 + 4, .03)
    if bar % 4 == 0:                                       # soft chime on the downbeat of each phrase
        bell(s0, 96, .03, dur=4, tau=1.6)

out = buf[:N].copy()
out[:len(buf) - N] += buf[N:]                              # fold the tail onto the start: seamless loop
out = np.tanh(out * 1.1)
out *= .8 / np.max(np.abs(out))
dst = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "music", "queen_dialogue.wav")
os.makedirs(os.path.dirname(dst), exist_ok=True)
with wave.open(dst, "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((out * 32767).astype("<i2").tobytes())
print("wrote", os.path.normpath(dst), "%.1fs" % (N / SR))
