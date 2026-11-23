"""Writes three story loops into assets/music/ (stereo WAV, then shrunk to 22.05 kHz like the rest):
  stage_clear_npc.wav  talking to the villagers after a stage     100 bpm  C major  16 bars
  intro.wav      siblings together, right before the kidnap  112 bpm  G major  16 bars  (spare, no drums)
  ending.wav     the sibling reunion                          116 bpm  G major  16 bars  (same melody + chords as intro_empty, full and warm)
Needs gen_level_music.py (instruments) and shrink_wavs.py next to it.
Run:  python tools/gen_story_music.py [--full]      (--full keeps 44.1 kHz)"""
import sys
from gen_level_music import *            # Mix, instruments, helpers
import shrink_wavs


def piano(m, dur=2.0):
    """Warm acoustic-ish piano: soft hammer, upper partials fade faster than the fundamental."""
    n = int(SR * dur); t = tt(n); f = hz(m)
    s = (np.sin(2 * np.pi * f * t) + .5 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t / .6)
         + .22 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t / .3) + .1 * np.sin(2 * np.pi * 4 * f * t) * np.exp(-t / .18)
         + .04 * np.sin(2 * np.pi * 5.01 * f * t) * np.exp(-t / .1))
    return (s * atk(t, .003) * np.exp(-t / 1.2) + noise(n) * np.exp(-t / .004) * .05) * gate(n, .15)


def synth_pluck(m, dur=.3):
    n = int(SR * dur); t = tt(n); ph = phase(hz(m), n)
    return (.6 * saw(ph) + .3 * pulse(ph, .25)) * atk(t, .002) * np.exp(-t / .11) * gate(n, .01)


# ------------------------------------------------------------------ 1. stage_clear_npc: jaunty, proud, C major, 100 bpm
def npc():
    mx = Mix(100, 16); S = mx.step
    SC = scale({0, 2, 4, 5, 7, 9, 11})
    CH = {"C": (0, 48, (60, 64, 67)), "Am": (9, 45, (57, 60, 64)), "F": (5, 41, (53, 57, 60)),
          "G": (7, 43, (55, 59, 62)), "Em": (4, 40, (55, 59, 64)), "Dm": (2, 38, (53, 57, 62))}
    PROG = ["C", "Am", "F", "G"] * 2 + ["F", "G", "Em", "Am"] + ["Dm", "G", "C", "G"]
    MOT = {"A": [(0, 4, 3), (3, 5, 1), (4, 4, 2), (6, 2, 2), (8, 4, 3), (11, 5, 1), (12, 7, 4)],
           "B": [(0, 7, 3), (3, 5, 1), (4, 4, 2), (6, 5, 2), (8, 4, 2), (10, 2, 2), (12, 0, 4)],
           "C": [(0, 2, 2), (2, 4, 2), (4, 5, 2), (6, 7, 2), (8, 5, 2), (10, 4, 2), (12, 2, 2), (14, 4, 2)]}
    PLAN = "ABAC" "ABAC" "BABC" "ABCA"
    for bar in range(16):
        b0 = bar * 16; pc, bass, tri = CH[PROG[bar]]
        for st, off, ln in MOT[PLAN[bar]]:
            mx.add(b0 + st, piano(mel_note(SC, pc, off, 72), ln * S + .8), .22 if st % 4 == 0 else .18, -.1)
        for k, idx in zip((2, 6, 10, 14), (0, 2, 1, 2)):                 # playful off-beat pluck answers
            mx.add(b0 + k, synth_pluck(tri[idx] + 12, .28), .055, .4)
        for k, iv in ((0, 0), (6, 7), (8, 0), (12, 7)):                    # warm soft bass
            mx.add(b0 + k, warm_bass(bass + iv, .42), .27)
        for k in (0, 10): mx.add(b0 + k, kick(46, 80, .09, .2), .32)
        for k in (4, 12): mx.add(b0 + k, snare(.12, 220, .04), .17, .1)
        for k in range(0, 16, 2): mx.add(b0 + k, hat(), .05 if k % 4 == 0 else .07, .3)
    mx.save("stage_clear_npc.wav", echo=[(.17, .21, .08)], rms=.16)


# ------------------------------------------------------------------ the shared theme (G major): used by intro_empty AND ending_tulip
PROG8 = ["G", "Em", "C", "D", "G", "Bm", "Am", "D"]
LOW = {"G": 43, "Em": 40, "C": 48, "D": 50, "Bm": 47, "Am": 45}
TRI = {"G": (55, 59, 62), "Em": (52, 55, 59), "C": (52, 55, 60), "D": (50, 57, 62), "Bm": (54, 59, 62), "Am": (52, 57, 60)}
LADDER = {"G": [55, 59, 62, 67, 71, 74], "Em": [52, 55, 59, 64, 67, 71], "C": [55, 60, 64, 67, 72, 76],
          "D": [57, 62, 66, 69, 74, 78], "Bm": [54, 59, 62, 66, 71, 74], "Am": [57, 60, 64, 69, 72, 76]}
THEME = [                                                          # (step, midi, length in steps) per bar
    [(0, 74, 4), (4, 71, 2), (6, 74, 2), (8, 79, 6)],
    [(0, 76, 4), (4, 74, 2), (6, 71, 2), (8, 76, 6)],
    [(0, 72, 4), (4, 76, 2), (6, 79, 2), (8, 76, 4), (12, 74, 2), (14, 72, 2)],
    [(0, 78, 4), (4, 76, 2), (6, 74, 2), (8, 69, 8)],
    [(0, 74, 4), (4, 71, 2), (6, 74, 2), (8, 79, 4), (12, 81, 2), (14, 79, 2)],
    [(0, 78, 4), (4, 74, 2), (6, 71, 2), (8, 74, 6)],
    [(0, 72, 4), (4, 76, 2), (6, 72, 2), (8, 69, 6)],
    [(0, 74, 4), (4, 78, 2), (6, 76, 2), (8, 74, 8)],
]


# ------------------------------------------------------------------ 2. intro_empty: fragile and hollow, G major, 112 bpm, no drums
def intro():
    mx = Mix(112, 16); S = mx.step
    for bar in range(16):
        b0 = bar * 16; tb = bar % 8; ch = PROG8[tb]; second = bar >= 8
        for st, m, ln in THEME[tb]:
            mx.add(b0 + st, epiano(m, ln * S + 1.2), .21 if st == 0 else .17, -.05)
            if second and st == 0:                                     # a music-box shadow on the second pass
                mx.add(b0 + st, bell(m + 12, 2.2, .45), .035, .3)
        mx.add(b0, epiano(LOW[ch], 2.4), .11, -.1)                    # sparse lower notes
        if tb % 2 == 1: mx.add(b0 + 8, epiano(LOW[ch] + 7, 1.8), .06, -.1)
    mx.save("intro.wav", echo=[(.27, .34, .28), (.55, .68, .18), (.9, 1.1, .12)], rms=.11)


# ------------------------------------------------------------------ 3. ending_tulip: the same theme, full and joyful, G major, 116 bpm
def ending():
    mx = Mix(116, 16); S = mx.step
    PAT = ([0, 1, 2, 3, 4, 3, 2, 1], [0, 2, 1, 3, 2, 4, 3, 5])
    for bar in range(16):
        b0 = bar * 16; tb = bar % 8; ch = PROG8[tb]; second = bar >= 8; lad = LADDER[ch]
        for st, m, ln in THEME[tb]:                                    # the same melody as intro_empty
            mx.add(b0 + st, piano(m, ln * S + 1.4), .22 if st == 0 else .18, -.05)
            if second: mx.add(b0 + st, piano(m - 12, ln * S + 1.0), .06, -.1)
        pat = PAT[bar % 2]
        for j, k in enumerate(range(0, 16, 2)):                        # flowing harp arpeggios
            mx.add(b0 + k, pluck(lad[pat[j]], 1.6, .6), .075 if second else .055, -.4 + .8 * (j % 2))
        for j, m in enumerate((LOW[ch],) + TRI[ch]):                    # lush swelling pad / strings
            mx.add(b0, pad(m, 16 * S + 1.2), .09 if second else .05, -.6 + .4 * j)
        for k, iv in ((0, 0), (8, 0), (12, 7)):                         # gentle warm bass
            mx.add(b0 + k, warm_bass(LOW[ch] + iv, .5), .22)
        if second:                                                      # soft glockenspiel accents
            for k in (0, 8): mx.add(b0 + k, bell(TRI[ch][2] + 24, 1.4, .3), .035, .35 if k else -.35)
        elif tb in (3, 7):
            mx.add(b0 + 8, bell(TRI[ch][2] + 24, 1.4, .3), .03, .3)
    mx.save("ending.wav", echo=[(.23, .29, .18), (.47, .61, .12)], rms=.15)


if __name__ == "__main__":
    for fn in (npc, intro, ending): fn()
    if "--full" not in sys.argv:
        for name in ("stage_clear_npc.wav", "intro.wav", "ending.wav"): shrink_wavs.shrink(os.path.join(OUT, name), False)
