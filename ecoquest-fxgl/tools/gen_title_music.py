"""Writes assets/music/title.wav: an original, seamless 16-bar title-screen loop (78 bpm, F major / D minor, 4/4):
warm piano melody with room to breathe, flowing harp arpeggios, a soft sustained pad. No percussion.
Run:  python tools/gen_title_music.py   (needs numpy)"""
import os, wave
import numpy as np

SR = 44100
BPM = 78
STEP = 60.0 / BPM / 2                      # one 8th note; a 4/4 bar is 8 steps
BAR = 8
BARS = 16
N = int(round(SR * STEP * BAR * BARS))
TAIL = SR * 4                              # rendered past the end, then folded back so the loop has no seam
buf = np.zeros(N + TAIL)


def hz(m): return 440.0 * 2 ** ((m - 69) / 12)


def add(t0, sig):
    i = max(0, int(round(t0 * SR)))
    j = min(len(buf), i + len(sig))
    if j > i: buf[i:j] += sig[:j - i]


def piano(step, m, vel, dur=6.0):
    """Warm electric-piano-ish tone: soft attack, gentle upper partials that fade faster than the fundamental."""
    n = int(SR * STEP * dur); t = np.arange(n) / SR
    f = hz(m)
    s = (np.sin(2 * np.pi * f * t)
         + .32 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / .5)
         + .10 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t / .25)
         + .04 * np.sin(2 * np.pi * 4.01 * f * t) * np.exp(-t / .1))
    s *= np.minimum(1, t / .008) * np.exp(-t / 1.3) * np.clip((n / SR - t) / .25, 0, 1)
    add(step * STEP, s * vel)


def harp(step, m, vol):
    """Soft plucked string: quick bloom, round decay."""
    n = int(SR * 2.4); t = np.arange(n) / SR
    f = hz(m)
    s = (np.sin(2 * np.pi * f * t) * np.exp(-t / .9)
         + .28 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / .35)
         + .08 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t / .15))
    s *= np.minimum(1, t / .004) * np.clip((n / SR - t) / .1, 0, 1)
    add(step * STEP, s * vol)


def pad(step, ms, vol, steps=BAR):
    """Warm pad / soft strings: detuned sines with a slow swell, overlapping into the next chord."""
    n = int(SR * (STEP * steps + 1.2)); t = np.arange(n) / SR
    e = np.minimum(1, t / .9) * np.clip((n / SR - t) / 1.2, 0, 1)
    out = np.zeros(n)
    for m in ms:
        f = hz(m)
        out += (np.sin(2 * np.pi * f * .9975 * t) + np.sin(2 * np.pi * f * 1.0025 * t)
                + .35 * np.sin(2 * np.pi * 2 * f * t) + .12 * np.sin(2 * np.pi * 3 * f * t))
    add(step * STEP, out * e * vol / len(ms))


# ------------------------------------------------------------------ harmony: F | Dm | Bb | C   F | Am | Bb | C   Dm | Bb | F | C   Dm | Gm | Bb | C
LADDER = {                                  # six rising chord tones for the harp
    "F":  [53, 57, 60, 65, 69, 72],
    "Dm": [50, 57, 62, 65, 69, 74],
    "Bb": [46, 53, 58, 62, 65, 70],
    "C":  [48, 55, 60, 64, 67, 72],
    "Am": [57, 60, 64, 69, 72, 76],
    "Gm": [55, 58, 62, 67, 70, 74],
}
LOW = {"F": 41, "Dm": 38, "Bb": 34, "C": 36, "Am": 45, "Gm": 43}
PROG = ["F", "Dm", "Bb", "C", "F", "Am", "Bb", "C",
        "Dm", "Bb", "F", "C", "Dm", "Gm", "Bb", "C"]
N_ = None
MEL = [                                     # eight 8th-note steps per bar; rests give the phrases room to breathe
    [72, N_, 77, N_, 76, N_, 74, N_], [74, N_, 69, N_, 72, N_, N_, N_],
    [70, N_, 74, N_, 77, N_, 76, 74], [72, N_, N_, N_, N_, N_, N_, N_],
    [72, N_, 77, N_, 81, N_, 79, N_], [76, N_, 72, N_, 69, N_, 72, N_],
    [74, N_, 77, N_, 74, N_, 70, N_], [67, N_, 72, N_, N_, N_, N_, N_],
    [77, N_, N_, 76, 74, N_, 72, N_], [70, N_, 74, N_, 77, N_, N_, N_],
    [81, N_, 79, N_, 77, N_, 76, N_], [74, N_, 72, N_, N_, N_, N_, N_],
    [74, N_, 77, N_, 81, N_, 77, N_], [79, N_, 74, N_, 70, N_, 74, N_],
    [77, N_, 74, N_, 70, N_, N_, N_], [72, N_, 76, N_, 79, N_, N_, N_],
]
ARP_A = [0, 1, 2, 3, 4, 3, 2, 1]
ARP_B = [0, 2, 1, 3, 2, 4, 3, 5]

for bar in range(BARS):
    s0 = bar * BAR
    ch = PROG[bar]
    lad = LADDER[ch]
    # pad: a deep root and the chord's lower tones
    pad(s0, [LOW[ch], lad[0], lad[1], lad[2]], .10)
    # harp: broken chords, thinning out at the end of each four-bar phrase
    pat = ARP_A if bar % 2 == 0 else ARP_B
    count = 4 if bar % 4 == 3 else 8
    for k in range(count):
        harp(s0 + k, lad[pat[k]], .055 if k % 4 else .07)
    # piano melody, soft and slightly shaped
    line = MEL[bar]
    for k, m in enumerate(line):
        if m is None: continue
        vel = .15 if k == 0 else .12
        piano(s0 + k, m, vel, dur=6.0)

out = buf[:N].copy()
out[:len(buf) - N] += buf[N:]               # fold the tail onto the start: seamless loop
# airy room: circular echoes (circular, so the loop stays seamless)
out = out + .22 * np.roll(out, int(.31 * SR)) + .15 * np.roll(out, int(.53 * SR)) + .10 * np.roll(out, int(.83 * SR))
out = np.tanh(out * 1.0)
out *= .75 / np.max(np.abs(out))
dst = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "music", "title.wav")
os.makedirs(os.path.dirname(dst), exist_ok=True)
with wave.open(dst, "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((out * 32767).astype("<i2").tobytes())
print("wrote", os.path.normpath(dst), "%.1fs" % (N / SR))
