"""Writes assets/music/boss_mound.wav: an original, seamless 16-bar loop for the Muck Mound fight
(146 bpm, E minor, 4/4): gritty chant-like organ/chiptune lead with octave jumps, growling distorted bass,
rock drums with junkyard clanks (cowbell, pipe, trash can) on the off-beats, power-chord and brass stabs.
Run:  python tools/gen_mound_music.py   (needs numpy)"""
import os, wave
import numpy as np

SR = 44100
BPM = 146
STEP = 60.0 / BPM / 4                      # one 16th note
BARS = 16
N = int(round(SR * STEP * 16 * BARS))
TAIL = SR * 2                              # rendered past the end, then folded back so the loop has no seam
buf = np.zeros(N + TAIL)
rng = np.random.default_rng(13)


def hz(m): return 440.0 * 2 ** ((m - 69) / 12)


def add(t0, sig):
    i = int(round(t0 * SR))
    j = min(len(buf), i + len(sig))
    if j > i: buf[i:j] += sig[:j - i]


def drive(x, g): return np.tanh(g * x) / np.tanh(g)


def saw_ph(f, n, dip=0.0):
    t = np.arange(n) / SR
    ph = np.cumsum(f * (1 + dip * np.exp(-t / .025))) / SR
    return 2 * (ph % 1) - 1


def sq_f(f, n, duty=.5):
    t = np.arange(n) / SR
    return np.where((f * t) % 1 < duty, 1.0, -1.0)


# ------------------------------------------------------------------ instruments
def lead(step, m, vol, steps=.6):
    """Gritty organ / chiptune: pulse + square + octave, hard-driven, sharply gated (staccato)."""
    n = int(SR * STEP * steps) + int(SR * .012); t = np.arange(n) / SR
    f = hz(m)
    s = .5 * sq_f(f, n, .25) + .4 * sq_f(f, n) + .25 * sq_f(f * 2, n)
    e = np.minimum(1, t / .001) * np.exp(-t / .25) * np.clip((n / SR - t) / .01, 0, 1)
    add(step * STEP, drive(s * e, 2.5) * vol)


def bass(step, m, vol, steps=1.4):
    """Growling bass: detuned saws with a pitch dip on the attack, a square sub, heavy distortion."""
    n = int(SR * STEP * steps); t = np.arange(n) / SR
    f = hz(m)
    s = saw_ph(f, n, .06) + .7 * saw_ph(f * 1.007, n, .06) + .5 * sq_f(f / 2, n)
    e = np.minimum(1, t / .002) * (.5 + .5 * np.exp(-t / .1)) * np.clip((n / SR - t) / .012, 0, 1)
    add(step * STEP, drive(s * e * .6, 4) * vol)


def power(step, root, vol, steps=1.3):
    """Distorted power chord: root + fifth + octave, two detuned saws each."""
    n = int(SR * STEP * steps); t = np.arange(n) / SR
    s = np.zeros(n)
    for iv in (0, 7, 12):
        f = hz(root + iv)
        s += saw_ph(f * .996, n) + saw_ph(f * 1.004, n)
    e = np.minimum(1, t / .003) * (.45 + .55 * np.exp(-t / .12)) * np.clip((n / SR - t) / .015, 0, 1)
    add(step * STEP, drive(s / 6 * e, 3) * vol)


def brass(step, notes, vol, steps=1.5):
    """Low-mid synth brass: stacked detuned saws with a quick swell, lightly driven."""
    n = int(SR * STEP * steps); t = np.arange(n) / SR
    s = np.zeros(n)
    for m in notes:
        f = hz(m)
        s += saw_ph(f * .995, n) + saw_ph(f, n) + saw_ph(f * 1.005, n)
    e = np.minimum(1, t / .025) * (.5 + .5 * np.exp(-t / .15)) * np.clip((n / SR - t) / .02, 0, 1)
    add(step * STEP, drive(s / (3 * len(notes)) * e, 1.6) * vol)


def kick(step, vol=.95):
    n = int(SR * .24); t = np.arange(n) / SR
    ph = 2 * np.pi * np.cumsum(46 + 130 * np.exp(-t / .03)) / SR
    add(step * STEP, np.sin(ph) * np.exp(-t / .13) * vol)


def snare(step, vol=.65):
    n = int(SR * .22); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate(([0], noise[:-1])) * .6
    add(step * STEP, (noise * np.exp(-t / .07) * .9 + np.sin(2 * np.pi * 190 * t) * np.exp(-t / .05) * .6) * vol)


def tom(step, f0, vol=.5):
    n = int(SR * .26); t = np.arange(n) / SR
    ph = 2 * np.pi * np.cumsum(f0 * (1 + .6 * np.exp(-t / .04))) / SR
    add(step * STEP, np.sin(ph) * np.exp(-t / .11) * vol)


def hat(step, vol=.14, open_=False):
    n = int(SR * (.17 if open_ else .035)); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate(([0], noise[:-1]))
    add(step * STEP, noise * np.exp(-t / (.07 if open_ else .011)) * vol)


def crash(step, vol=.3):
    n = int(SR * 1.5); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate(([0], noise[:-1])) * .9
    add(step * STEP, noise * np.exp(-t / .5) * vol)


def cowbell(step, vol=.13):
    n = int(SR * .3); t = np.arange(n) / SR
    s = sq_f(587, n) + sq_f(845, n)
    add(step * STEP, s * np.exp(-t / .07) * np.minimum(1, t / .001) * vol)


def pipe(step, vol=.13):
    """Metal pipe clank: inharmonic ringing partials plus a tick of noise."""
    n = int(SR * .45); t = np.arange(n) / SR
    s = sum(a * np.sin(2 * np.pi * 410 * r * t) * np.exp(-t / d)
            for r, a, d in ((1, 1, .2), (2.76, .6, .14), (5.4, .4, .08), (8.93, .25, .05)))
    s += rng.standard_normal(n) * np.exp(-t / .005) * .5
    add(step * STEP, s * np.minimum(1, t / .001) * vol)


def trashcan(step, vol=.14):
    """Dented trash can: a dull noisy bang with a few low metallic resonances."""
    n = int(SR * .3); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    noise = noise - np.concatenate(([0], noise[:-1])) * .5
    s = noise * np.exp(-t / .045) * .8
    for f0, d in ((230, .09), (361, .07), (587, .05)):
        s += np.sin(2 * np.pi * f0 * t) * np.exp(-t / d) * .5
    add(step * STEP, s * np.minimum(1, t / .001) * vol)


# ------------------------------------------------------------------ harmony (E minor, with a lurching flat-II for the unhinged feel)
#          bass  lead  power-chord-root  stab triad
CH = {
    "Em": dict(b=40, l=64, p=52, tri=(52, 55, 59), third=3),
    "C":  dict(b=36, l=60, p=48, tri=(48, 52, 55), third=4),
    "D":  dict(b=38, l=62, p=50, tri=(50, 54, 57), third=4),
    "G":  dict(b=43, l=67, p=55, tri=(55, 59, 62), third=4),
    "B":  dict(b=35, l=59, p=47, tri=(47, 51, 54), third=4),
    "Bb": dict(b=34, l=58, p=46, tri=(46, 50, 53), third=4),
}
PROG = ["Em", "Em", "C", "D", "Em", "Em", "C", "B",
        "Em", "G", "D", "C", "Em", "Em", "Bb", "B"]
N_ = None
T = "t"                                     # the chord's third (minor or major)
M1 = [0, N_, 12, N_, 0, N_, 12, N_, T, N_, T + "+12", N_, 0, 12, 10, N_]
M2 = [0, N_, 12, N_, 7, N_, 12, N_, 0, N_, 12, N_, 7, N_, 10, 12]
M3 = [12, N_, 10, N_, 7, N_, T, N_, 12, 12, 7, 7, 0, 0, 0, N_]       # descending chant fill


def interval(x, third):
    if x == T: return third
    if x == T + "+12": return third + 12
    return x


BASS_HITS = [(0, 0), (3, 0), (6, 12), (8, 0), (10, 0), (11, 12), (14, 7)]      # (step, interval)

for bar in range(BARS):
    b0 = bar * 16
    c = CH[PROG[bar]]
    # lead: chant, repeated, fill every 4th bar
    pat = M3 if bar % 4 == 3 else (M1 if bar % 2 == 0 else M2)
    for k, x in enumerate(pat):
        if x is None: continue
        lead(b0 + k, c["l"] + interval(x, c["third"]), .12 if k % 4 == 0 else .10)
    # bass: bouncy, aggressive 16th groove
    for k, iv in BASS_HITS:
        bass(b0 + k, c["b"] + iv, .45)
    # power chord stabs on the groove, brass accents
    for k in (0, 6, 10):
        power(b0 + k, c["p"], .09)
    for k in (8, 14):
        brass(b0 + k, c["tri"], .08, steps=1.2)
    # drums
    for k in (0, 3, 8, 11): kick(b0 + k, .95 if k in (0, 8) else .6)
    for k in (4, 12): snare(b0 + k)
    for k in range(0, 16, 2): hat(b0 + k, .15 if k % 4 == 0 else .09, open_=(k == 14))
    # junkyard clanks on the off-beats
    cowbell(b0 + 2); pipe(b0 + 6); cowbell(b0 + 10, .10); trashcan(b0 + 14)
    if bar % 8 == 0: crash(b0)
    if bar in (7, 15):                                                           # fills
        for k, f in zip((12, 13, 14, 15), (210, 170, 130, 95)): tom(b0 + k, f)
        snare(b0 + 15, .6)

out = buf[:N].copy()
out[:len(buf) - N] += buf[N:]                                                    # fold the tail onto the start: seamless loop
out = np.tanh(out * 1.2)
out *= .85 / np.max(np.abs(out))
dst = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "music", "boss_mound.wav")
os.makedirs(os.path.dirname(dst), exist_ok=True)
with wave.open(dst, "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((out * 32767).astype("<i2").tobytes())
print("wrote", os.path.normpath(dst), "%.1fs" % (N / SR))
