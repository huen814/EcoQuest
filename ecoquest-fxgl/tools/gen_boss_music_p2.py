"""Writes assets/music/boss_queen_p2.wav: Queen Bijou's second-phase theme, emotional orchestral rock.
126 bpm, D minor, 4/4, 36 bars (~68 s), mono 44.1 kHz 16-bit. Loops seamlessly (exact sample count, reverb/ring-out folded onto the start;
the finale reprise falls straight back into the lonely piano intro).

  Intro        bars  1-4   lonely piano, theme A
  Development  bars  5-12  cello and bass enter, violin sings theme A then theme B, ambient guitar, soft timpani
  Rising       bars 13-20  spiccato strings, palm-muted guitar chugs, snare/tom build, climbing violin cry
  Climax       bars 21-32  violin + distorted lead guitar on theme C, open power chords, full rock drums, piano hits
  Finale       bars 33-36  everything strips back to piano + cello, violin whispers theme A, ends hanging on the dominant

Instruments: piano (additive), bowed violin/cello/pad (detuned saw + vibrato), Karplus-Strong guitars (tanh-distorted), deep bass, acoustic-style drums.
Run:  python tools/gen_boss_music_p2.py   (needs numpy)"""
import os, wave
import numpy as np

SR = 44100
BPM = 126
STEP = 60.0 / BPM / 4                       # one 16th note
BARS = 36
N = int(round(SR * STEP * 16 * BARS))       # exact loop length in samples
TAIL = SR * 4
D = np.zeros(N + TAIL)                      # dry mix
R = np.zeros(N + TAIL)                      # reverb send
rng = np.random.default_rng(126)


def hz(m): return 440.0 * 2 ** ((m - 69) / 12)
def tt(n): return np.arange(n) / SR
def gate(n, rel=.01): return np.clip((n / SR - tt(n)) / rel, 0, 1)


def lp(x, k):                               # cheap low-pass: moving average
    return np.convolve(x, np.ones(k) / k, mode="same")


def add(step, sig, vol=1.0, rev=0.0):
    i = max(0, int(round(step * STEP * SR)))
    j = min(len(D), i + len(sig))
    if j > i:
        D[i:j] += sig[:j - i] * vol
        if rev: R[i:j] += sig[:j - i] * vol * rev


# =============================================================== instruments
def piano(step, m, steps, vel, rev=.35):
    k = int(SR * STEP * steps); n = k + int(SR * .5); t = tt(n); f = hz(m)
    tau = float(np.clip(1.0 + 2.6 * (1 - (m - 36) / 60.0), .8, 3.2))
    s = (np.sin(2 * np.pi * f * t) + .5 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / .6)
         + .22 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t / .3) + .1 * np.sin(2 * np.pi * 4 * f * t) * np.exp(-t / .18)
         + .04 * np.sin(2 * np.pi * 5.01 * f * t) * np.exp(-t / .1))
    s = s * np.minimum(1, t / .003) * np.exp(-t / tau) + rng.standard_normal(n) * np.exp(-t / .004) * .04
    s *= gate(n, .4)
    add(step, s, vel, rev)


def bow(f, n, a=.08, rel=.12, bright=3, vib=.006, vrate=5.6, vdel=.15, noise=.012):
    t = tt(n)
    if vib:
        v = vib * np.sin(2 * np.pi * vrate * t) * np.clip((t - vdel) / .3, 0, 1)
        ph = np.cumsum(f * (1 + v)) / SR
    else:
        ph = np.arange(1, n + 1) * f / SR
    s = ((2 * (ph % 1) - 1) + .7 * (2 * ((ph * 1.004 + .21) % 1) - 1)) / 1.7      # two slightly detuned saws
    s = lp(s, bright)
    if noise: s = s + lp(rng.standard_normal(n), 6) * noise
    return s * np.minimum(1, t / a) * gate(n, rel)


def violin(step, m, steps, vol, vib=.006, a=.06):
    n = int(SR * STEP * steps) + int(SR * .12)
    add(step, bow(hz(m), n, a=a, rel=.12, bright=3, vib=vib), vol, .45)


def cello(step, m, steps, vol, vib=.005, a=.1):
    n = int(SR * STEP * steps) + int(SR * .14)
    add(step, bow(hz(m), n, a=a, rel=.14, bright=9, vib=vib), vol, .4)


def spic(step, m, vol, cellos=False, steps=1.1):
    n = int(SR * STEP * steps) + int(SR * .03); t = tt(n)
    s = bow(hz(m), n, a=.004, rel=.03, bright=9 if cellos else 3, vib=0, noise=.02) * np.exp(-t / (.12 if cellos else .08))
    add(step, s, vol, .3)


def pad(step, ms, steps, vol):
    for m in ms:
        n = int(SR * STEP * steps) + int(SR * .5)
        add(step, bow(hz(m), n, a=.55, rel=.5, bright=9, vib=.003, vdel=.4, noise=0), vol, .5)


def ks(f, n, decay=.998, vib=0.0):
    """Plucked string (Karplus-Strong): noise burst fed back through 0.5*(buf[i] + buf[i-1]); retuned to exactly f."""
    P = max(2, int(round(SR / f)))
    ratio = f * P / SR
    nsrc = int(n * ratio * 1.02) + 2 * P
    out = np.empty((nsrc // P + 1) * P)
    prev = rng.uniform(-1, 1, P)
    out[:P] = prev
    for b in range(1, len(out) // P):
        prev = 0.5 * (prev + np.concatenate(([prev[-1]], prev[:-1]))) * decay
        out[b * P:(b + 1) * P] = prev
    if vib:
        t = tt(n)
        wob = vib * np.sin(2 * np.pi * 5.5 * t) * np.clip((t - .12) / .2, 0, 1)
        idx = np.cumsum(ratio * 2 ** (wob / 12))
    else:
        idx = np.arange(n) * ratio
    y = np.interp(np.minimum(idx, len(out) - 1), np.arange(len(out)), out)
    return y / (np.max(np.abs(y)) + 1e-9)


def cut(s, k, rel=.04):
    s[k:] *= np.clip(1 - (np.arange(len(s) - k) / (SR * rel)), 0, 1)
    return s


def amb(step, m, vol):                      # atmospheric clean-ish guitar with a dotted-8th echo
    n = int(SR * STEP * 8); t = tt(n)
    s = lp(np.tanh(ks(hz(m), n, .9993) * 1.4), 3) * np.minimum(1, t / .004) * gate(n, .3)
    for dt, g in ((0, 1), (3, .5), (6, .25)):
        add(step + dt, s, vol * g, .5)


def lead(step, m, steps, vol=.15, vibrato=False):      # distorted guitar lead
    k = int(SR * STEP * steps); n = k + int(SR * .05); t = tt(n)
    s = lp(np.tanh(ks(hz(m), n, .9985, .15 if vibrato else 0.0) * 6.0), 3)
    s *= np.minimum(1, t / .002) * (.7 + .3 * np.exp(-t / .15))
    add(step, cut(s, k), vol, .2)


def chord(step, ms, steps, muted=False, vol=.12):    # power chord: root + fifth + octave
    k = int(SR * STEP * steps); n = k + int(SR * .04); t = tt(n)
    s = np.tanh(sum(ks(hz(m), n, .93 if muted else .9985) for m in ms) / len(ms) * 6.0)
    if muted:
        s = lp(s, 6) * np.exp(-t / .045); vol *= 1.5
    else:
        s = lp(s, 2) * (.65 + .35 * np.exp(-t / .2))
    add(step, cut(s * np.minimum(1, t / .002), k, .03), vol, .1)


def bass(step, m, steps, vol):
    k = int(SR * STEP * steps); n = k + int(SR * .06); t = tt(n); f = hz(m)
    s = (np.sin(2 * np.pi * f * t) + .4 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / .25)
         + .25 * lp(2 * ((f * t) % 1) - 1, 8))
    s *= np.minimum(1, t / .005) * (.8 + .2 * np.exp(-t / .2))
    add(step, cut(s, k, .05), vol)


def kick(step, vol=.6):
    n = int(SR * .3); t = tt(n)
    f = 48 + 120 * np.exp(-t / .025)
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / .11) + rng.standard_normal(n) * np.exp(-t / .003) * .1
    add(step, np.tanh(s * 1.4), vol, .08)


def snare(step, vol=.5):
    n = int(SR * .35); t = tt(n)
    x = rng.standard_normal(n); x = x - lp(x, 6)
    s = x * np.exp(-t / .09) + np.sin(2 * np.pi * 195 * t) * np.exp(-t / .06) * .7 + np.sin(2 * np.pi * 330 * t) * np.exp(-t / .035) * .3
    add(step, np.tanh(s * 1.3), vol, .3)


def tom(step, f, vol=.45, ring=.28):
    n = int(SR * (ring * 3)); t = tt(n)
    fr = f * (1 + .35 * np.exp(-t / .03))
    s = np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.exp(-t / ring) + rng.standard_normal(n) * np.exp(-t / .004) * .08
    add(step, s, vol, .25)


def hat(step, vol, open_=False):
    n = int(SR * (.2 if open_ else .05)); t = tt(n)
    x = np.diff(rng.standard_normal(n + 1))
    add(step, x * np.exp(-t / (.07 if open_ else .016)), vol, .1)


def crash(step, vol=.14, dur=2.0):
    n = int(SR * dur); t = tt(n)
    x = np.diff(np.concatenate(([0], np.diff(rng.standard_normal(n + 1)))))
    s = x * np.exp(-t / .55) * np.clip((dur - t) / .2, 0, 1)
    add(step, s / np.max(np.abs(s)), vol, .35)


# =============================================================== harmony
A_ = ["Dm", "Bb", "Gm", "A"]
B_ = ["Dm", "F", "Bb", "A"]
RISE_ = ["Gm", "Bb", "F", "C", "Gm", "Bb", "A", "A"]
CLIM_ = ["Dm", "Bb", "F", "C", "Gm", "Dm", "Bb", "A", "Dm", "Bb", "F", "A"]
PROG = A_ + A_ + B_ + RISE_ + CLIM_ + A_
assert len(PROG) == BARS

CHT = {"Dm": [62, 65, 69, 74], "Bb": [58, 62, 65, 70], "Gm": [55, 58, 62, 67], "A": [57, 61, 64, 69], "F": [53, 57, 60, 65], "C": [55, 60, 64, 67]}
ROOT = {"Dm": 38, "Bb": 34, "Gm": 31, "A": 33, "F": 29, "C": 36}
POWER = {"Dm": (50, 57, 62), "Bb": (46, 53, 58), "Gm": (43, 50, 55), "A": (45, 52, 57), "F": (41, 48, 53), "C": (48, 55, 60)}

# melodies: per bar a list of (step, midi, length in steps)
THEME_A = [  # Dm Bb Gm A: the aching main theme
    [(0, 74, 6), (6, 72, 2), (8, 69, 8)],
    [(0, 70, 4), (4, 74, 4), (8, 72, 6), (14, 70, 2)],
    [(0, 67, 4), (4, 70, 4), (8, 74, 6), (14, 72, 2)],
    [(0, 69, 8), (8, 76, 6), (14, 73, 2)],
]
THEME_B = [  # Dm F Bb A: the answer, reaching higher
    [(0, 77, 4), (4, 76, 2), (6, 74, 2), (8, 77, 4), (12, 81, 4)],
    [(0, 79, 4), (4, 77, 2), (6, 76, 2), (8, 77, 8)],
    [(0, 82, 4), (4, 81, 2), (6, 79, 2), (8, 77, 4), (12, 79, 4)],
    [(0, 81, 6), (6, 79, 2), (8, 77, 4), (12, 76, 2), (14, 73, 2)],
]
THEME_C = [  # Dm Bb F C: the desperate climax theme
    [(0, 74, 2), (2, 81, 6), (8, 79, 2), (10, 77, 2), (12, 76, 4)],
    [(0, 82, 2), (2, 81, 2), (4, 79, 4), (8, 77, 4), (12, 79, 4)],
    [(0, 81, 6), (6, 77, 2), (8, 84, 4), (12, 81, 2), (14, 79, 2)],
    [(0, 79, 4), (4, 76, 4), (8, 79, 4), (12, 84, 4)],
]
THEME_C2 = [  # Gm Dm Bb A
    [(0, 82, 4), (4, 79, 2), (6, 82, 2), (8, 86, 6), (14, 82, 2)],
    [(0, 81, 4), (4, 77, 2), (6, 81, 2), (8, 84, 6), (14, 81, 2)],
    [(0, 82, 6), (6, 79, 2), (8, 77, 4), (12, 74, 4)],
    [(0, 81, 4), (4, 79, 2), (6, 76, 2), (8, 73, 8)],
]
C_END = [(0, 76, 4), (4, 81, 4), (8, 85, 8)]                           # last climax bar (A): climbs and hangs
CLIM_MEL = THEME_C + THEME_C2 + THEME_C[:3] + [C_END]
VR = [74, 77, 81, 79, 82, 86, 85, 81]                                   # violin cry climbing through the rising section


def play(phrase_bar, b0, fn, **kw):
    for st, m, ln in phrase_bar:
        fn(b0 + st, m, ln, **kw)


# =============================================================== arrangement
for bar in range(BARS):
    b0 = bar * 16; ch = PROG[bar]; tones = CHT[ch]; root = ROOT[ch]; low = [m - 12 for m in tones]
    intro, dev, rise, clim, fin = bar < 4, 4 <= bar < 12, 12 <= bar < 20, 20 <= bar < 32, bar >= 32
    rp = (bar - 12) / 7.0 if rise else 0                                # 0..1 build amount

    # ---------------- piano
    if intro or fin:
        for st, m, ln in THEME_A[bar % 4]:
            piano(b0 + st, m, ln, .15 if intro else .12)
        piano(b0, root + 12, 16, .12 if intro else .09)
        for st, i in zip((2, 6, 10, 14), (2, 3, 2, 1)):
            piano(b0 + st, low[i], 3, .05 if intro else .04)
    elif dev:
        piano(b0, root + 12, 16, .2)
        for k in range(0, 16, 2):
            piano(b0 + k, (low if bar < 8 else tones)[[0, 1, 2, 3, 2, 1, 2, 3][k // 2]], 3, .08 if bar < 8 else .10)
    elif rise:
        piano(b0, root + 12, 8, .2); piano(b0 + 8, root + 12, 8, .2)
        for k in range(0, 16, 2):
            piano(b0 + k, tones[2] if k % 4 == 0 else tones[2] + 12, 1.8, .04 + .07 * rp)
    else:
        for k, ln in ((0, 6), (6, 2), (8, 6), (14, 2)):
            for m in tones + [root + 12]:
                piano(b0 + k, m, ln, .12)

    # ---------------- bass
    if 6 <= bar < 12:
        bass(b0, root, 15.5, .22)
    elif rise:
        for k in range(0, 16, 2): bass(b0 + k, root + (12 if k in (6, 14) and rp > .5 else 0), 1.8, .16 + .08 * rp)
    elif clim:
        for k in range(0, 16, 2): bass(b0 + k, root + (12 if k in (6, 14) else 0), 1.8, .32)
    elif bar == 35:
        bass(b0, root, 15.5, .14)

    # ---------------- cello
    croot = root + 12
    if 4 <= bar < 8 or fin:
        cello(b0, croot, 15.5, .11 if dev else .07)
    elif 8 <= bar < 12:
        cello(b0, croot, 7.5, .09); cello(b0 + 8, croot + 7, 7.5, .09)
    elif rise:
        for k in range(0, 16, 2): spic(b0 + k, croot, .05 + .05 * rp, cellos=True, steps=1.4)
    elif clim:
        cello(b0, croot, 7.7, .11, vib=.007); cello(b0 + 8, croot + 7, 7.7, .11, vib=.007)

    # ---------------- violin
    if 4 <= bar < 8:
        play(THEME_A[bar - 4], b0, lambda s, m, ln: violin(s, m, ln, .13, vib=.007 if ln >= 4 else .003))
    elif 8 <= bar < 12:
        play(THEME_B[bar - 8], b0, lambda s, m, ln: violin(s, m, ln, .11, vib=.008 if ln >= 4 else .003))
    elif rise:
        violin(b0, VR[bar - 12], 15.5, .07 + .05 * rp, vib=.006 + .006 * rp, a=.15)
    elif clim:
        play(CLIM_MEL[bar - 20], b0, lambda s, m, ln: violin(s, m, ln, .17, vib=.010 if ln >= 4 else .004, a=.04))
    elif bar >= 34:
        play(THEME_A[bar - 32], b0, lambda s, m, ln: violin(s, m, ln, .06, vib=.008 if ln >= 4 else .003, a=.1))

    # ---------------- string pad / spiccato
    if 8 <= bar < 12:
        pad(b0, tones[:3], 15.5, .04)
    elif 14 <= bar < 20:
        pad(b0, tones[:3], 15.5, .04 + .03 * rp)
    elif clim:
        pad(b0, tones, 15.5, .05)
    elif bar >= 34:
        pad(b0, tones[:3], 15.5, .03)
    if 16 <= bar < 20:
        for k in range(16):
            spic(b0 + k, tones[[0, 1, 2, 3, 2, 1, 2, 3][k % 8]], .02 + .035 * rp)

    # ---------------- guitars
    if 8 <= bar < 12:
        for st, i in zip((3, 7, 11), (2, 3, 2)): amb(b0 + st, tones[i], .06)
    if rise and bar < 16:
        for k in range(0, 16, 2): chord(b0 + k, POWER[ch], 1.3, muted=True, vol=.04 + .06 * rp)
    elif (rise and bar >= 16) or clim:
        g = .04 + .05 * rp if rise else .17
        for k in (0, 8): chord(b0 + k, POWER[ch], 3, vol=g)
        for k in (4, 6, 12, 14): chord(b0 + k, POWER[ch], 1.3, muted=True, vol=g)
    if clim:
        play(CLIM_MEL[bar - 20], b0, lambda s, m, ln: lead(s, m - 12, ln + (.6 if ln >= 2 else 0), .18, vibrato=ln >= 4))

    # ---------------- drums
    if bar in (10, 11):
        tom(b0, 62, .30, .5); tom(b0 + 8, 62, .2, .5)
    if rise:
        if bar < 14:
            for k in (0, 8): kick(b0 + k, .35 + .1 * rp)
            for k in (0, 4, 8, 12): hat(b0 + k, .035)
        elif bar < 16:
            for k in (0, 8): kick(b0 + k, .45)
            snare(b0 + 12, .35)
            for k in range(0, 16, 2): hat(b0 + k, .05)
        elif bar < 18:
            for k in (0, 6, 8, 14): kick(b0 + k, .5)
            for k in (4, 12): snare(b0 + k, .5)
            for k in range(0, 16, 2): hat(b0 + k, .06, open_=(k == 14))
        elif bar == 18:
            for k in (0, 3, 6, 8, 11, 14): kick(b0 + k, .55)
            for k in (4, 12): snare(b0 + k, .6)
            for k in range(0, 16, 2): hat(b0 + k, .07)
        else:                                                           # bar 19: snare roll + tom fall into the climax
            for k in range(16): snare(b0 + k, .10 + .045 * k)
            for k, f in zip((12, 13, 14, 15), (190, 150, 120, 95)): tom(b0 + k, f, .5, .25)
            kick(b0, .6); kick(b0 + 8, .6)
    if clim:
        for k in (0, 6, 8, 11): kick(b0 + k, .62)
        for k in (4, 12): snare(b0 + k, .68)
        for k in range(0, 16, 2): hat(b0 + k, .07, open_=(k == 14))
        if (bar - 20) % 4 == 0: crash(b0)
        if bar in (23, 27):
            for k, f in zip((12, 13, 14, 15), (200, 160, 125, 95)): tom(b0 + k, f, .5, .25)
        if bar == 31:
            for k in range(8, 16): snare(b0 + k, .3 + .07 * (k - 8))
    if bar == 32:
        tom(b0, 62, .3, .6)

# =============================================================== reverb + loop fold
IRN = int(SR * 2.6)
ti = tt(IRN)
ir = rng.standard_normal(IRN) * np.exp(-ti / .75) * np.clip(ti / .02, 0, 1)
ir = lp(ir, 3); ir /= np.sqrt((ir ** 2).sum())
M = 1 << int(np.ceil(np.log2(len(R) + IRN)))
wet = np.fft.irfft(np.fft.rfft(R, M) * np.fft.rfft(ir, M), M)[:len(R) + IRN]
tot = np.zeros(len(wet)); tot[:len(D)] += D; tot += wet * .55
out = np.zeros(N)
for i in range(0, len(tot), N):                                        # fold everything past the end back onto the start: seamless loop
    seg = tot[i:i + N]; out[:len(seg)] += seg
out = np.tanh(out * 1.1)
out *= .85 / np.max(np.abs(out))

dst = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "music", "boss_queen_p2.wav")
os.makedirs(os.path.dirname(dst), exist_ok=True)
with wave.open(dst, "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((out * 32767).astype("<i2").tobytes())
print("wrote", os.path.normpath(dst), "%d samples, %.1fs" % (N, N / SR))
