"""Writes assets/music/knights_dialogue.wav: an original, seamless 16-bar loop for the Queen's Knight's dialogue scenes
(before the chase). 128 bpm, 4/4, D minor with harmonic-minor and chromatic tension: tense, suspicious, formal.
Layers: pizzicato/harpsichord riff with sudden half-step shifts, low reed counter-melody, staccato bass ostinato,
dry marching percussion (rim snare, muted toms, ticking hi-hat). 44.1 kHz stereo 16-bit PCM, exactly 16 bars (30.000 s),
no fades: the tail that rings past the last bar is folded back onto the start so the loop has no seam.
Run:  python tools/gen_knight_dialogue_music.py   (needs numpy)"""
import os, wave
import numpy as np

SR = 44100
BPM = 128
STEP = 60.0 / BPM / 4                       # one 16th note; a 4/4 bar is 16 steps
BARS = 16
N = int(round(SR * STEP * 16 * BARS))       # 1,323,000 samples = 30.000 s
TAIL = SR * 3
L = np.zeros(N + TAIL)
R = np.zeros(N + TAIL)
RL = np.zeros(N + TAIL)                      # reverb send
RR = np.zeros(N + TAIL)
rng = np.random.default_rng(13)


def hz(m): return 440.0 * 2 ** ((m - 69) / 12)


def place(step, sig, vol, pan=0.0, rev=0.0, off=0.0):
    i = int(round((step + off) * STEP * SR))
    j = min(len(L), i + len(sig))
    if j <= i: return
    g = np.pi * (pan + 1) / 4
    gl, gr = np.cos(g) * vol, np.sin(g) * vol
    s = sig[:j - i]
    L[i:j] += s * gl
    R[i:j] += s * gr
    RL[i:j] += s * gl * rev
    RR[i:j] += s * gr * rev


def pluck(m):
    """Harpsichord-meets-pizzicato: bright plucked partials that die fast, a short body thump and a tiny click."""
    n = int(SR * .45); t = np.arange(n) / SR; f = hz(m)
    s = np.zeros(n)
    for h in range(1, 10):
        if h * f < SR * .45:
            s += (1.0 / h) * np.sin(2 * np.pi * f * h * t + .3 * h) * np.exp(-t * (7 + 2.4 * h))
    s += .6 * np.sin(2 * np.pi * f * t) * np.exp(-t * 15)
    click = np.diff(rng.standard_normal(n + 1)) * np.exp(-t / .002) * .18
    s = (s + click) * np.minimum(1, t / .001) * np.clip((n / SR - t) / .02, 0, 1)
    return s / 1.6


def reed(m, dur):
    """Low reed / muted brass: odd-harmonic buzz with a slow-blooming vibrato and a breath of noise."""
    n = int(SR * (dur + .09)); t = np.arange(n) / SR; f = hz(m)
    vib = 1 + .005 * np.sin(2 * np.pi * 5.1 * t) * np.clip((t - .12) / .25, 0, 1)
    ph = 2 * np.pi * np.cumsum(f * vib) / SR
    s = np.zeros(n)
    for h in range(1, 12):
        if h * f < 4200:
            w = (1.0 / h ** 1.25) * (1 if h % 2 else .32)
            s += w * np.sin(h * ph)
    s += .05 * np.diff(rng.standard_normal(n + 1))
    env = np.minimum(1, t / .05) * np.clip((dur + .09 - t) / .09, 0, 1) * (.88 + .12 * np.minimum(1, t / .3))
    return s * env * .75


def bass(m, dur=1.25):
    n = int(SR * STEP * dur) + int(SR * .012); t = np.arange(n) / SR; f = hz(m)
    ph = 2 * np.pi * np.cumsum(f * (1 + .14 * np.exp(-t / .012))) / SR
    s = np.sin(ph) + .5 * np.sin(2 * ph) * np.exp(-t / .07) + .3 * np.sin(3 * ph) * np.exp(-t / .045)
    s *= np.exp(-t / .1) * np.minimum(1, t / .002) * np.clip((n / SR - t) / .008, 0, 1)
    return s * 1.1


def rim():
    n = int(SR * .12); t = np.arange(n) / SR
    s = .6 * np.sin(2 * np.pi * 1850 * t) * np.exp(-t / .012) + .4 * np.sin(2 * np.pi * 420 * t) * np.exp(-t / .03)
    s += .7 * np.diff(rng.standard_normal(n + 1)) * np.exp(-t / .02)
    return s * np.minimum(1, t / .0005)


def tom(f0, f1=None):
    n = int(SR * .2); t = np.arange(n) / SR
    f1 = f1 or f0 * .6
    ph = 2 * np.pi * np.cumsum(f1 + (f0 - f1) * np.exp(-t / .03)) / SR
    s = np.sin(ph) * np.exp(-t / .065) + .15 * np.diff(rng.standard_normal(n + 1)) * np.exp(-t / .006)
    return s * np.minimum(1, t / .001) * np.clip((n / SR - t) / .02, 0, 1)


def hat():
    n = int(SR * .045); t = np.arange(n) / SR
    z = rng.standard_normal(n + 2)
    return (z[2:] - 2 * z[1:-1] + z[:-2]) * np.exp(-t / .008) * np.minimum(1, t / .0004) * .5


# ----------------------------------------------------------------- harmony: 16 bars of D minor tension
# name: (bass root, riff root, riff third)
CH = {"Dm": (38, 74, 3), "Gm": (43, 67, 3), "Bb": (46, 70, 4), "A7": (45, 69, 4), "Edim": (40, 64, 3), "Eb": (39, 63, 4)}
PROG = ["Dm", "Dm", "Bb", "A7",  "Dm", "Gm", "Bb", "A7",  "Gm", "Dm", "Edim", "A7",  "Dm", "Bb", "Eb", "A7"]
SHIFT = {5: 1, 9: -1, 13: 1}                # bars whose riff jumps a half step: the sudden chromatic shifts

RIFF_A = [(0, 0), (2, 3), (3, 2), (4, 0), (6, -1), (8, -5), (10, -4), (11, -5), (13, -2), (14, -5)]
RIFF_B = [(0, 0), (1, -1), (2, 0), (4, 3), (6, 2), (7, 3), (8, 5), (10, 3), (12, 2), (13, 1), (14, 0)]
LAST = [(0, 69), (1, 70), (2, 71), (3, 72), (4, 73), (6, 73), (8, 69), (10, 73), (12, 76), (13, 74), (14, 73)]

# counter melody (step, midi, length in steps): low-mid, stern, moving against the riff
CM = [
    [(0, 50, 7), (8, 53, 3), (12, 52, 4)], [(0, 50, 6), (8, 57, 3), (12, 55, 4)],
    [(0, 58, 6), (8, 57, 3), (12, 53, 4)], [(0, 52, 6), (8, 49, 3), (12, 50, 4)],
    [(0, 53, 6), (8, 52, 2), (10, 50, 2), (12, 53, 4)], [(0, 55, 6), (8, 58, 3), (12, 57, 4)],
    [(0, 58, 6), (8, 60, 3), (12, 58, 4)], [(0, 57, 6), (8, 55, 3), (12, 56, 4)],
    [(0, 55, 7), (8, 53, 3), (12, 55, 4)], [(0, 57, 6), (8, 53, 3), (12, 50, 4)],
    [(0, 52, 6), (8, 55, 3), (12, 58, 4)], [(0, 57, 4), (4, 58, 4), (8, 61, 3), (12, 57, 4)],
    [(0, 50, 7), (8, 57, 3), (12, 53, 4)], [(0, 58, 6), (8, 53, 3), (12, 50, 4)],
    [(0, 51, 7), (8, 58, 3), (12, 55, 4)], [(0, 57, 4), (4, 55, 2), (6, 53, 2), (8, 52, 4), (12, 49, 4)],
]

BASS_PAT = [(0, 0), (2, 0), (3, 0), (4, 0), (6, 12), (8, 0), (10, 0), (11, 7), (12, 0), (14, 12)]
SNARE_FILL = {3: [(12, .5), (13, .6), (14, .8), (15, 1)], 7: [(12, .5), (13, .6), (14, .8), (15, 1)],
              11: [(12, .5), (13, .6), (14, .8), (15, 1)],
              15: [(8, .4), (10, .5), (12, .6), (13, .7), (14, .85), (15, 1)]}

hat_s, rim_s = hat(), rim()
for bar in range(BARS):
    b0 = bar * 16
    bass_root, riff_root, third = CH[PROG[bar]]
    # percussion
    for s in range(0, 16, 2):
        place(b0 + s, hat_s, .15 if s % 4 == 0 else .09, pan=.3, off=0)
    if bar % 4 == 3:
        place(b0 + 15, hat_s, .06, pan=.3)
    if bar not in SNARE_FILL:
        place(b0 + 4, rim_s, .4, pan=-.1); place(b0 + 12, rim_s, .4, pan=-.1)
        if bar % 2 == 1: place(b0 + 14, rim_s, .12, pan=-.1)
        place(b0 + 0, tom(150), .42, pan=-.3)
        place(b0 + 10, tom(120), .3, pan=.25)
        if bar % 2 == 1: place(b0 + 7, tom(190, 120), .2, pan=.1)
    else:
        place(b0 + 4, rim_s, .4, pan=-.1); place(b0 + 0, tom(150), .42, pan=-.3)
        for s, v in SNARE_FILL[bar]: place(b0 + s, rim_s, .38 * v, pan=-.1)
    # bass ostinato
    for s, o in BASS_PAT:
        place(b0 + s, bass(bass_root + o), .42 if s % 4 == 0 else .33, pan=0)
    # counter melody
    for s, m, d in CM[bar]:
        place(b0 + s, reed(m, d * STEP), .22, pan=.2, rev=.25)
    # pizzicato / harpsichord riff
    if bar == 15:
        notes = LAST
    else:
        base = RIFF_B if 8 <= bar < 12 else RIFF_A
        sh = SHIFT.get(bar, 0)
        notes = [(s, riff_root + (third if o == 3 else o) + sh) for s, o in base]
    for s, m in notes:
        v = .27 if s % 4 == 0 else .21
        place(b0 + s, pluck(m), v, pan=-.25, rev=.22)
        place(b0 + s, pluck(m), v * .22, pan=.55, rev=.3, off=3)     # a faint tiptoeing echo

# a small room: a decaying noise impulse response on the send bus
ir_n = int(SR * .7); t = np.arange(ir_n) / SR
ir_l = rng.standard_normal(ir_n) * np.exp(-t / .16); ir_r = rng.standard_normal(ir_n) * np.exp(-t / .16)
ir_l[0] = ir_r[0] = 0


def conv(x, h):
    n = len(x) + len(h)
    k = 1 << (n - 1).bit_length()
    return np.fft.irfft(np.fft.rfft(x, k) * np.fft.rfft(h, k), k)[:len(x)]


L += conv(RL, ir_l) * .03
R += conv(RR, ir_r) * .03

# fold the ring-out past the last bar back onto the start: a seamless loop with no fade
L[:TAIL] += L[N:N + TAIL]
R[:TAIL] += R[N:N + TAIL]
L, R = L[:N], R[:N]
peak = max(np.abs(L).max(), np.abs(R).max())
L, R = np.tanh(L / peak * 1.25) / np.tanh(1.25) * .89, np.tanh(R / peak * 1.25) / np.tanh(1.25) * .89
pcm = (np.stack([L, R], 1) * 32767).astype("<i2")

out = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "music", "knights_dialogue.wav")
os.makedirs(os.path.dirname(out), exist_ok=True)
with wave.open(out, "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(pcm.tobytes())
print("wrote", os.path.normpath(out), f"{N / SR:.3f}s")
