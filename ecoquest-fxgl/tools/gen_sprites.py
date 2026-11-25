#!/usr/bin/env python3
"""Generates every texture EcoQuest needs (dreamy pastel, Omori-inspired pixel art, 16 px art scaled x3 = 48 px tiles).

Usage:  python3 tools/gen_sprites.py          (needs Pillow)
Output: src/main/resources/assets/textures/...

Replace any generated PNG with hand-drawn art of the same size and name; the game does not care.
"""
import math
import os
import random
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "textures")
S = 3  # upscale factor (16 px art -> 48 px tiles)
INK = (40, 48, 56, 255)


def hx(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), c[3] if len(c) > 3 else 255)


def save(name, img, scale=S):
    path = os.path.join(OUT, name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.resize((img.width * scale, img.height * scale), Image.NEAREST).save(path)


def canvas(w=16, h=16):
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)


def ell(d, x0, y0, x1, y1, fill, outline=INK):
    d.ellipse([x0, y0, x1, y1], fill=fill, outline=outline)


def rect(d, x0, y0, x1, y1, fill, outline=INK):
    d.rectangle([x0, y0, x1, y1], fill=fill, outline=outline)


# ----------------------------------------------------------------------------- items (16x16)

def item_sprites():
    def make(name, fn):
        im, d = canvas()
        fn(d)
        save("items/%s.png" % name, im)

    def plastic_bottle(d):
        rect(d, 5, 5, 10, 14, hx("#a9d2f7")); rect(d, 6, 2, 9, 4, hx("#a9d2f7")); rect(d, 6, 1, 9, 2, hx("#2f6fd6"))
        rect(d, 5, 8, 10, 11, hx("#ffffff"), None); d.point((6, 6), fill=(255, 255, 255, 255))

    def plastic_bag(d):
        d.polygon([(3, 5), (12, 5), (14, 14), (1, 14)], fill=hx("#f4f4fa"), outline=INK)
        d.arc([4, 0, 7, 6], 180, 360, fill=INK); d.arc([8, 0, 11, 6], 180, 360, fill=INK); d.line([(5, 9), (10, 9)], fill=hx("#c8c8d8"))

    def food_container(d):
        rect(d, 1, 6, 14, 13, hx("#f2a65a")); rect(d, 1, 4, 14, 6, hx("#cde8fa")); d.line([(3, 9), (12, 9)], fill=hx("#ffd8a8"))

    def newspaper(d):
        rect(d, 2, 2, 13, 13, hx("#f4f1e4"), hx("#888888")); rect(d, 3, 3, 12, 5, hx("#333333"), None)
        for y in (7, 9, 11): d.line([(3, y), (12, y)], fill=hx("#999999"))
        rect(d, 8, 7, 12, 11, hx("#b5b5b5"), None)

    def cardboard_box(d):
        rect(d, 2, 4, 13, 13, hx("#c99a62"), hx("#8a6a3a")); rect(d, 6, 4, 9, 9, hx("#e8cf9c"), None)
        d.line([(2, 8), (6, 8)], fill=hx("#8a6a3a")); d.line([(9, 8), (13, 8)], fill=hx("#8a6a3a"))

    def notebook_paper(d):
        rect(d, 3, 1, 12, 14, hx("#eef2fb"), hx("#445566")); rect(d, 3, 1, 5, 14, hx("#d9473a"), None)
        for y in (4, 7, 10): d.line([(7, y), (11, y)], fill=hx("#7a8bb5"))

    def glass_bottle(d):
        rect(d, 5, 6, 10, 14, hx("#2e8b4a")); rect(d, 6, 1, 9, 6, hx("#2e8b4a")); d.line([(6, 8), (6, 12)], fill=hx("#8fe0a8"))

    def glass_jar(d):
        rect(d, 3, 4, 12, 14, hx("#bee4ee")); rect(d, 4, 1, 11, 3, hx("#b8b8b8")); d.line([(5, 6), (5, 12)], fill=(255, 255, 255, 255))

    def tin_can(d):
        rect(d, 3, 3, 12, 14, hx("#b7bec6")); rect(d, 3, 7, 12, 10, hx("#d9a53a"), None)
        d.line([(3, 4), (12, 4)], fill=hx("#6a727a")); d.line([(3, 13), (12, 13)], fill=hx("#6a727a"))

    def aluminum_can(d):
        rect(d, 4, 3, 11, 14, hx("#d9473a")); rect(d, 4, 2, 11, 3, hx("#cfd5da")); d.line([(6, 6), (6, 12)], fill=(255, 255, 255, 255))

    def bottle_cap(d):
        ell(d, 3, 6, 12, 12, hx("#8f9aa5")); ell(d, 6, 8, 9, 10, hx("#c3ccd5"), None)

    def banana_peel(d):
        d.polygon([(2, 8), (5, 13), (11, 12), (14, 4), (11, 7), (7, 9)], fill=hx("#f6d338"), outline=hx("#9a7a10")); d.point((14, 4), fill=hx("#5a3a10"))

    def food_scraps(d):
        ell(d, 3, 5, 12, 14, hx("#d9473a"), hx("#8a1f18")); d.line([(8, 5), (9, 1)], fill=hx("#6b4a2b")); rect(d, 9, 2, 12, 4, hx("#3aa564"), None)
        d.point((5, 8), fill=(255, 255, 255, 200))

    def dry_leaves(d):
        d.polygon([(2, 13), (3, 6), (9, 2), (13, 3), (12, 10), (6, 14)], fill=hx("#d9822b"), outline=hx("#8a4a10")); d.line([(3, 12), (11, 4)], fill=hx("#8a4a10"))

    def seed(d):
        ell(d, 3, 10, 12, 14, hx("#6b4a2b")); d.line([(8, 11), (8, 5)], fill=hx("#2e8b4a"), width=1)
        d.polygon([(8, 7), (3, 4), (5, 2), (8, 5)], fill=hx("#4cb866")); d.polygon([(8, 6), (13, 3), (12, 6), (9, 8)], fill=hx("#3aa564"))

    def boost(d):
        pts = []
        import math
        for i in range(10):
            a = -math.pi / 2 + i * math.pi / 5
            r = 7 if i % 2 == 0 else 3
            pts.append((8 + math.cos(a) * r, 8 + math.sin(a) * r))
        d.polygon(pts, fill=hx("#ffd23c"), outline=hx("#c08a1e")); d.point((7, 6), fill=(255, 255, 255, 255))

    for n, f in [("plastic_bottle", plastic_bottle), ("plastic_bag", plastic_bag), ("food_container", food_container),
                 ("newspaper", newspaper), ("cardboard_box", cardboard_box), ("notebook_paper", notebook_paper),
                 ("glass_bottle", glass_bottle), ("glass_jar", glass_jar), ("tin_can", tin_can), ("aluminum_can", aluminum_can),
                 ("bottle_cap", bottle_cap), ("banana_peel", banana_peel), ("food_scraps", food_scraps), ("dry_leaves", dry_leaves),
                 ("seed", seed), ("boost", boost)]:
        make(n, f)


# ----------------------------------------------------------------------------- bins

BINS = {"plastic": "#7fb2f2", "paper": "#f7d88a", "glass": "#86dba6", "metal": "#f29a9a", "organic": "#c29a7a"}


def bin_sprites():
    for name, col in BINS.items():
        c = hx(col)
        for open_ in (False, True):
            im, d = canvas()
            d.ellipse([2, 14, 13, 15], fill=(0, 0, 0, 60))
            d.polygon([(3, 5), (12, 5), (11, 14), (4, 14)], fill=c, outline=INK)
            d.line([(5, 7), (5, 12)], fill=shade(c, 1.35))
            # label: white recycle-ish triangle
            d.polygon([(8, 7), (6, 11), (10, 11)], fill=(255, 255, 255, 230))
            if open_:
                d.polygon([(2, 1), (13, 1), (13, 4), (2, 4)], fill=shade(c, 0.6), outline=INK)
                d.rectangle([4, 5, 11, 6], fill=(20, 24, 28, 255))
            else:
                d.rectangle([2, 3, 13, 5], fill=shade(c, 0.65), outline=INK)
            save("bins/%s%s.png" % (name, "_open" if open_ else ""), im)


# ----------------------------------------------------------------------------- world objects

def world_sprites():
    im, d = canvas()  # planting spot: soft soil mound with a pale ring
    d.ellipse([1, 8, 14, 15], fill=hx("#eadbe6"), outline=hx("#9a7a90"))
    d.ellipse([3, 9, 12, 14], fill=hx("#c9a09c"), outline=hx("#8a6070"))
    for p in [(5, 11), (8, 13), (10, 11), (7, 10)]: d.point(p, fill=hx("#e2bdb8"))
    save("world/spot.png", im)

    im, d = canvas()  # sapling: tiny teal bubble sprout
    d.ellipse([3, 11, 12, 15], fill=hx("#c9a09c")); d.line([(8, 12), (8, 9)], fill=hx("#3aa5ae"))
    bubbles(d, 8, 8, 3.2)
    save("world/sapling.png", im)

    im, d = canvas()  # tree: big teal bubble canopy like the forest in the reference
    d.ellipse([2, 14, 13, 15], fill=(60, 40, 90, 55))
    rect(d, 7, 10, 8, 14, hx("#9a7aa6"), hx("#5a4470"))
    bubbles(d, 8, 6, 6.2)
    save("world/tree.png", im)

    im, d = canvas(48, 32)  # truck: 3 tiles wide x 2 tall, covers the whole road (both lanes)
    d.ellipse([3, 29, 46, 31], fill=(60, 40, 90, 50))
    rect(d, 1, 3, 31, 25, hx("#f0eef8"), hx("#8a84a0"))            # cargo box
    rect(d, 1, 17, 31, 20, hx("#86dba6"), None)                      # green stripe
    d.ellipse([11, 6, 21, 15], fill=hx("#86dba6"), outline=hx("#3f8f66"))  # leaf badge
    d.line([(16, 14), (16, 8)], fill=hx("#3f8f66")); d.line([(16, 10), (19, 8)], fill=hx("#3f8f66"))
    d.line([(4, 7), (8, 7)], fill=hx("#b8b4d0")); d.line([(4, 10), (8, 10)], fill=hx("#b8b4d0"))
    d.polygon([(31, 9), (40, 9), (46, 17), (46, 25), (31, 25)], fill=hx("#f08a9a"), outline=hx("#8a3a4a"))  # cab
    rect(d, 34, 11, 40, 17, hx("#d4f0ff"), None)                     # window
    d.line([(35, 12), (37, 12)], fill=(255, 255, 255, 255))
    rect(d, 44, 20, 46, 22, hx("#ffe9a8"), None)                     # headlight
    rect(d, 1, 25, 46, 26, hx("#8a84a0"), None)                      # bumper / chassis
    for x in (9, 22, 38):
        d.ellipse([x - 4, 22, x + 4, 30], fill=hx("#3a3550"), outline=INK); d.ellipse([x - 1, 25, x + 1, 27], fill=hx("#aaa6c0"))
    save("world/truck.png", im)

    im, d = canvas()  # boat
    d.ellipse([1, 14, 14, 16], fill=(40, 60, 100, 45))
    d.polygon([(1, 8), (15, 8), (12, 14), (4, 14)], fill=hx("#e0b088"), outline=hx("#6a4a3a"))
    d.line([(3, 10), (13, 10)], fill=hx("#f0cca6")); d.line([(1, 8), (15, 8)], fill=hx("#fbe2c4"))
    d.line([(2, 5), (6, 10)], fill=hx("#6a4a3a")); d.line([(14, 5), (10, 10)], fill=hx("#6a4a3a"))
    save("world/boat.png", im)


# ----------------------------------------------------------------------------- Guardians (the two siblings)
#
# Both siblings share the exact same 16x24 frame layout, so animation, hitbox and gameplay are identical.
# Each sibling has a clean look (the one you play as) and a dirty look (the sibling you did not pick, for the story).

SKIN = hx("#fde4d2")
HAIR = hx("#2d2a4a")
HOOD = hx("#9fe0cf")
SHORTS = hx("#e8e4f8")
SHOE = hx("#ffffff")
PACK = hx("#f4b6d0")
BLUSH = hx("#f5a5b5")
MUD = (122, 90, 60, 255)
MUD_DARK = (92, 66, 44, 255)


def blend(c, t, to=MUD):
    """Mix colour c towards `to` by t (0..1): used to turn clean clothes into grimy ones."""
    return tuple(int(c[i] * (1 - t) + to[i] * t) for i in range(3)) + (255,)


def _look(base, dirty):
    """Clean looks use the colours as given; dirty looks fade and stain every garment and mess up the hair."""
    L = dict(base, dirty=dirty)
    if dirty:
        for k, t in (("hood", .38), ("shorts", .40), ("shoe", .55), ("pack", .35), ("trim", .35)):
            L[k] = blend(L[k], t)
    return L


# Omori-inspired: pale skin, near-black hair, soft off-white clothes (not copies of any existing character).
_SISTER = dict(skin=hx("#fdf0e6"), hair=hx("#2b2838"), hood=hx("#f7ecf1"), shorts=hx("#f7ecf1"), shoe=hx("#ffffff"),
               pack=hx("#d8c4d0"), trim=hx("#e0a8b8"), style="long", skirt=True, clip=hx("#b9a6e8"), blush=hx("#f5c0cc"))
_BROTHER = dict(skin=hx("#fdf0e6"), hair=hx("#2b2838"), hood=hx("#f4f4f8"), shorts=hx("#3a3552"), shoe=hx("#ffffff"),
                pack=hx("#4a4668"), trim=hx("#b8b4d0"), style="short", skirt=False, clip=None, blush=None)

# Both siblings start out clean. The one the player picks stays clean, the OTHER one ends up dirty (story).
LOOKS = {
    "sister": _look(_SISTER, False),
    "brother": _look(_BROTHER, False),
    "sister_dirty": _look(_SISTER, True),
    "brother_dirty": _look(_BROTHER, True),
}


# ----------------------------------------------------------------------------- shop outfits (new designs)
#
# Drawn on the same 16x24 frame as the plain looks. Hair strokes use the exact HAIR colour so the shop's hair
# colours still apply on top of them, and the boat hull is drawn by player_frame as usual.

WITCH_BLACK = hx("#2b2040")
WITCH_PURPLE = hx("#9b3fe8")
WITCH_PURPLE_DK = hx("#5b22a8")
GOLD = hx("#f2b43c")


def draw_witch(d, direction, step, boat, L, hy, ty):
    """Pointed hat with a purple band and gold buckle, a purple collar, and a flared black dress with a purple frill."""
    if direction == "down":
        d.point((7, ty), fill=WITCH_PURPLE); d.point((8, ty), fill=WITCH_PURPLE)
        d.point((6, ty + 1), fill=WITCH_PURPLE); d.point((9, ty + 1), fill=WITCH_PURPLE)
    if not boat:
        rect(d, 3, ty + 6, 12, ty + 6, WITCH_BLACK, None)           # flared skirt
        rect(d, 2, ty + 7, 13, ty + 7, WITCH_PURPLE, None)          # purple frill
        d.point((2, ty + 7), fill=WITCH_PURPLE_DK); d.point((13, ty + 7), fill=WITCH_PURPLE_DK)
    # hat: cone, band, brim
    b = hy + 2
    rect(d, 4, hy + 1, 11, b - 1, WITCH_BLACK, None)
    rect(d, 5, hy, 10, hy, WITCH_BLACK, None)
    rect(d, 6, hy - 1, 9, hy - 1, WITCH_BLACK, None)
    rect(d, 7, hy - 2, 8, hy - 2, WITCH_BLACK, None)
    d.point((9, hy - 2), fill=WITCH_PURPLE_DK); d.point((10, hy - 3 if hy - 3 >= 0 else 0), fill=WITCH_PURPLE_DK)  # bent tip
    rect(d, 4, hy + 1, 11, hy + 1, WITCH_PURPLE, None)               # band
    if direction == "down":
        d.point((7, hy + 1), fill=GOLD); d.point((8, hy + 1), fill=GOLD)  # buckle
    rect(d, 0, b, 15, b + 1, WITCH_PURPLE, WITCH_PURPLE_DK)         # brim
    rect(d, 1, b + 1, 14, b + 1, WITCH_PURPLE_DK, None)


IDOL_RED = hx("#cf2c45")
IDOL_RED_DK = hx("#7e1426")
IDOL_BLACK = hx("#26222c")


def draw_idol(d, direction, step, boat, L, hy, ty):
    """Twin tails with red ties, a black choker, white puff sleeves under a red strapped vest, and a black skirt."""
    hair = HAIR_SRC
    # twin tails (one is hidden behind the head from the side)
    tails = []
    if direction in ("down", "up"):
        tails = [(1, 2), (13, 14)]
    elif direction == "left":
        tails = [(12, 14)]
    else:
        tails = [(1, 3)]
    for x0, x1 in tails:
        out = x0 < 8                                                  # True for the tail on the left of the sprite
        rect(d, x0, hy + 4, x1, hy + 8, hair, None)                   # hangs down beside the head...
        if out:
            rect(d, x0 - 1, hy + 9, x1 - 1, hy + 11, hair, None)      # ...and curls outwards at the tip
        else:
            rect(d, x0 + 1, hy + 9, x1 + 1, hy + 11, hair, None)
        rect(d, x0, hy + 3, x1, hy + 3, IDOL_RED, None)               # red hair tie
    # choker + vest over the torso (white blouse underneath, so the arms read as puff sleeves)
    if direction != "up":
        rect(d, 5, ty + 1, 10, ty + 5, IDOL_RED, IDOL_RED_DK)
        if direction == "down":
            d.point((7, ty), fill=IDOL_BLACK); d.point((8, ty), fill=IDOL_BLACK)  # choker
            d.line([(6, ty + 1), (6, ty + 3)], fill=IDOL_BLACK); d.line([(9, ty + 1), (9, ty + 3)], fill=IDOL_BLACK)  # straps
            d.point((7, ty + 1), fill=hx("#f6c0cc")); d.point((8, ty + 1), fill=hx("#f6c0cc"))
    else:
        rect(d, 5, ty + 1, 10, ty + 5, IDOL_RED, IDOL_RED_DK)
        d.line([(6, ty + 1), (6, ty + 4)], fill=IDOL_BLACK); d.line([(9, ty + 1), (9, ty + 4)], fill=IDOL_BLACK)
    if direction in ("down", "up"):
        sw = hx("#ffffff"); sw_line = hx("#d6cfe2")                   # white puff sleeves, redrawn so they stay white
        a = {0: 0, 1: 1, 2: 0, 3: -1}[step]
        rect(d, 2, ty + 1 + max(0, a), 4, ty + 3 + max(0, a), sw, sw_line)
        rect(d, 11, ty + 1 + max(0, -a), 13, ty + 3 + max(0, -a), sw, sw_line)
    if not boat:
        rect(d, 3, ty + 6, 12, ty + 7, IDOL_BLACK, None)            # skirt
        rect(d, 3, ty + 7, 12, ty + 7, hx("#3a3446"), None)


HAIR_SRC = hx("#2b2838")

REAPER_CLOAK = hx("#343a58")
REAPER_DARK = hx("#1e2238")
REAPER_LIGHT = hx("#4a5278")
BONE = hx("#ece6d4")
EYE_GLOW = hx("#7cc8f4")
ROPE = hx("#7a5230")


def draw_reaper(d, direction, step, boat, L, hy, ty):
    """Big dark hood with a skull face and glowing blue eyes, a rope belt, bony hands and a ragged cloak hem."""
    rect(d, 5, hy - 2, 10, hy - 2, REAPER_DARK, None)                   # hood peak
    rect(d, 3, hy - 1, 12, hy - 1, REAPER_CLOAK, REAPER_DARK)
    rect(d, 2, hy, 13, hy + 8, REAPER_CLOAK, REAPER_DARK)               # hood, a little wider than the head
    d.point((4, hy)); d.point((4, hy), fill=REAPER_LIGHT); d.point((5, hy), fill=REAPER_LIGHT)
    if direction == "down":
        rect(d, 4, hy + 2, 11, hy + 8, REAPER_DARK, None)               # shadow inside the hood
        rect(d, 5, hy + 3, 10, hy + 8, BONE, None)                      # skull
        rect(d, 5, hy + 4, 6, hy + 5, EYE_GLOW, None); rect(d, 9, hy + 4, 10, hy + 5, EYE_GLOW, None)
        d.point((6, hy + 5), fill=REAPER_DARK); d.point((9, hy + 5), fill=REAPER_DARK)   # pupils
        d.point((7, hy + 6), fill=REAPER_DARK); d.point((8, hy + 6), fill=REAPER_DARK)   # nose
        for x in (6, 8): d.point((x, hy + 8), fill=REAPER_DARK)                           # teeth gaps
    elif direction in ("left", "right"):
        left = direction == "left"
        x0, x1 = (3, 8) if left else (7, 12)
        rect(d, x0, hy + 2, x1, hy + 8, REAPER_DARK, None)
        rect(d, x0 + 1, hy + 3, x1, hy + 8, BONE, None) if left else rect(d, x0, hy + 3, x1 - 1, hy + 8, BONE, None)
        ex = 5 if left else 9
        rect(d, ex, hy + 4, ex + 1, hy + 5, EYE_GLOW, None)
        d.point((ex if left else ex + 1, hy + 5), fill=REAPER_DARK)
        d.point((x0 + 1 if left else x1 - 1, hy + 6), fill=REAPER_DARK)       # nose hole
        d.point((x0 + 3 if left else x1 - 3, hy + 8), fill=REAPER_DARK)
    else:
        rect(d, 6, hy + 2, 9, hy + 2, REAPER_LIGHT, None)               # fold in the hood
    # rope belt and bony hands
    rect(d, 5, ty + 3, 10, ty + 3, ROPE, None)
    if direction == "down":
        d.point((8, ty + 4), fill=ROPE)
    if direction in ("down", "up"):
        a = {0: 0, 1: 1, 2: 0, 3: -1}[step]
        d.point((3, ty + 4 + max(0, a)), fill=BONE); d.point((4, ty + 4 + max(0, a)), fill=BONE)
        d.point((11, ty + 4 + max(0, -a)), fill=BONE); d.point((12, ty + 4 + max(0, -a)), fill=BONE)
    if not boat:
        rect(d, 3, ty + 6, 12, ty + 7, REAPER_CLOAK, None)               # cloak hem
        for x in (3, 5, 7, 9, 11):                                       # ragged edge
            d.point((x, ty + 7), fill=(0, 0, 0, 0))
        rect(d, 3, ty + 6, 12, ty + 6, REAPER_LIGHT, None)


STAGE_BLUE = hx("#7fc4ee")
STAGE_WHITE = hx("#f6f2ff")
STAGE_GOLD = hx("#f4c542")
STAGE_PINK = hx("#f58cb8")


def draw_stage(d, direction, step, boat, L, hy, ty):
    """Boy-idol stage look: sky-blue jacket with white lapels, gold buttons and epaulettes, a pink bow tie,
    white cuffs, a gold star hair clip and a headset mic."""
    # star clip + headset mic
    d.point((10, hy + 2), fill=STAGE_GOLD); d.point((11, hy + 3), fill=STAGE_GOLD); d.point((9, hy + 3), fill=STAGE_GOLD)
    d.point((10, hy + 3), fill=hx("#fff2a8")); d.point((10, hy + 4), fill=STAGE_GOLD)
    if direction == "down":
        d.point((12, hy + 5), fill=hx("#4a4668")); d.point((12, hy + 6), fill=hx("#4a4668")); d.point((11, hy + 7), fill=hx("#4a4668"))
        d.point((10, hy + 7), fill=STAGE_PINK)
    elif direction == "left":
        d.point((10, hy + 5), fill=hx("#4a4668")); d.point((9, hy + 7), fill=hx("#4a4668"))
    elif direction == "right":
        d.point((5, hy + 5), fill=hx("#4a4668")); d.point((6, hy + 7), fill=hx("#4a4668"))
    # jacket
    if direction == "down":
        d.line([(6, ty + 1), (7, ty + 3)], fill=STAGE_WHITE); d.line([(9, ty + 1), (8, ty + 3)], fill=STAGE_WHITE)   # lapels
        d.point((7, ty), fill=STAGE_PINK); d.point((8, ty), fill=STAGE_PINK)                                          # bow tie
        d.point((6, ty), fill=hx("#d96a98")); d.point((9, ty), fill=hx("#d96a98"))
        d.point((7, ty + 4), fill=STAGE_GOLD); d.point((8, ty + 4), fill=STAGE_GOLD)                                  # buttons
        d.point((7, ty + 5), fill=STAGE_GOLD)
    elif direction in ("left", "right"):
        bx = 5 if direction == "left" else 10
        d.point((bx, ty + 2), fill=STAGE_GOLD); d.point((bx, ty + 4), fill=STAGE_GOLD)
    if direction in ("down", "up"):
        a = {0: 0, 1: 1, 2: 0, 3: -1}[step]
        for x in (3, 4):
            d.point((x, ty + 1 + max(0, a)), fill=STAGE_GOLD)                                                         # epaulettes
            d.point((x, ty + 4 + max(0, a)), fill=STAGE_WHITE)                                                        # cuffs
        for x in (11, 12):
            d.point((x, ty + 1 + max(0, -a)), fill=STAGE_GOLD)
            d.point((x, ty + 4 + max(0, -a)), fill=STAGE_WHITE)
    if direction == "up":
        d.line([(5, ty + 5), (10, ty + 5)], fill=STAGE_GOLD)                                                          # back sash
    if not boat:
        rect(d, 4, ty + 6, 11, ty + 6, STAGE_BLUE, None)                                                              # short jacket tails
        d.point((4, ty + 6), fill=STAGE_WHITE); d.point((11, ty + 6), fill=STAGE_WHITE)


def paint_arms(d, step, ty, r0, r1, color):
    """Paints torso rows r0..r1 of both swinging arms (front and back views): short sleeves, cuffs."""
    a = {0: 0, 1: 1, 2: 0, 3: -1}[step]
    rect(d, 3, ty + r0 + max(0, a), 4, ty + r1 + max(0, a), color, None)
    rect(d, 11, ty + r0 + max(0, -a), 12, ty + r1 + max(0, -a), color, None)


FUR = hx("#fbf8f2")
XMAS_RED = hx("#d63a3a")
BELT = hx("#26222c")


def santa_hat(d, direction, hy):
    """Red hat with a fur brim and a pom-pom that droops to the back of the head."""
    flip = direction == "right"
    for x0, x1, y, c in ((6, 9, hy - 2, XMAS_RED), (4, 11, hy - 1, XMAS_RED), (3, 12, hy, XMAS_RED),
                         (10, 11, hy - 2, FUR), (10, 11, hy - 1, FUR), (2, 13, hy + 1, FUR)):
        rect(d, 15 - x1 if flip else x0, y, 15 - x0 if flip else x1, y, c, None)


def draw_santa(d, direction, step, boat, L, hy, ty):
    """Beardless Santa: hat, red coat with fur collar, cuffs and hem, black belt with a gold buckle, red trousers."""
    santa_hat(d, direction, hy)
    rect(d, 5, ty, 10, ty, FUR, None)
    rect(d, 5, ty + 5, 10, ty + 5, FUR, None)
    if direction == "down":
        rect(d, 7, ty + 1, 8, ty + 4, FUR, None)
    if direction != "up":
        rect(d, 5, ty + 3, 10, ty + 3, BELT, None)
    if direction == "down":
        d.point((7, ty + 3), fill=GOLD); d.point((8, ty + 3), fill=GOLD)
    if direction in ("down", "up"):
        paint_arms(d, step, ty, 4, 4, FUR)
    if not boat:
        rect(d, 5, ty + 7, 10, ty + 7, FUR, None)


def draw_santa_girl(d, direction, step, boat, L, hy, ty):
    """Santa hat, red top with a fur collar and cuffs, and a red miniskirt with a fur hem."""
    santa_hat(d, direction, hy)
    rect(d, 5, ty, 10, ty, FUR, None)
    if direction in ("down", "up"):
        paint_arms(d, step, ty, 4, 4, FUR)
    if not boat:
        rect(d, 3, ty + 6, 12, ty + 6, XMAS_RED, None)
        rect(d, 3, ty + 7, 12, ty + 7, FUR, None)


SHIRT_BLUE = hx("#3aaed0")
FLOWER_PINK = hx("#ff8fb3")
FLOWER_YELLOW = hx("#ffe066")
LENS = hx("#e6a23c")
FRAME = hx("#2b2735")
ORANGE = hx("#f2893a")
CREAM = hx("#f6efe2")
TEE = hx("#f1eee8")


def tee_arms(d, direction, step, ty, skin):
    """White collar row and light short sleeves, so a white top is not drawn as a grey one."""
    rect(d, 5, ty, 10, ty, TEE, None)
    if direction in ("down", "up"):
        paint_arms(d, step, ty, 1, 2, TEE)
        paint_arms(d, step, ty, 3, 4, skin)


def draw_summer_boy(d, direction, step, boat, L, hy, ty):
    """Open-collar floral shirt with short sleeves, and tinted sunglasses."""
    if direction in ("down", "up"):
        paint_arms(d, step, ty, 3, 4, L["skin"])
    for x, y, c in ((6, 2, FLOWER_PINK), (9, 4, FLOWER_PINK), (8, 1, FLOWER_YELLOW), (6, 4, FLOWER_YELLOW),
                    (7, 3, FUR), (9, 2, FUR)):
        d.point((x, ty + y), fill=c)
    if direction == "down":
        d.point((7, ty), fill=L["skin"]); d.point((8, ty), fill=L["skin"])
    if direction != "up":
        rect(d, 4, hy + 4, 11, hy + 4, FRAME, None)
        for x0, x1 in {"down": ((4, 6), (9, 11)), "left": ((4, 7),), "right": ((8, 11),)}[direction]:
            rect(d, x0, hy + 5, x1, hy + 6, LENS, None)
            d.point((x0, hy + 5), fill=hx("#f6cf86"))


def draw_summer_girl(d, direction, step, boat, L, hy, ty):
    """Bucket hat, sheer white crop top over an orange bra, orange high-waist shorts with black straps."""
    tee_arms(d, direction, step, ty, L["skin"])
    rect(d, 5, ty + 4, 10, ty + 4, L["skin"], None)
    rect(d, 5, ty + 5, 10, ty + 5, ORANGE, None)
    if direction == "down":
        rect(d, 6, ty + 1, 9, ty + 2, ORANGE, None)
        for x in (6, 9):
            d.line([(x, ty + 4), (x, ty + 6)], fill=BELT)
    rect(d, 4, hy - 1, 11, hy, CREAM, None)
    rect(d, 4, hy, 11, hy, ORANGE, None)
    rect(d, 2, hy + 1, 13, hy + 1, CREAM, None)
    rect(d, 3, hy + 2, 12, hy + 2, shade(CREAM, 0.85), None)


SWEATER_PINK = hx("#f4a6bd")
NAVY = hx("#2c3a66")
NAVY_LT = hx("#46588c")
UNI_RED = hx("#d9473a")


def uniform_top(d, direction, step, ty):
    """Pink sweater sleeves, a white collar and a v-neck showing the shirt."""
    if direction in ("down", "up"):
        paint_arms(d, step, ty, 1, 3, SWEATER_PINK)
    rect(d, 5, ty, 10, ty, FUR, None)
    if direction == "down":
        rect(d, 7, ty + 1, 8, ty + 1, FUR, None)


def draw_uniform_girl(d, direction, step, boat, L, hy, ty):
    """Pink knit v-neck over a white collar, loose red bow, black choker with a green charm, short pleated navy skirt."""
    uniform_top(d, direction, step, ty)
    if direction == "down":
        d.point((7, ty), fill=BELT); d.point((8, ty), fill=hx("#4cc98a"))
        rect(d, 6, ty + 2, 9, ty + 2, UNI_RED, None)
        d.point((8, ty + 3), fill=UNI_RED)
    if not boat:
        rect(d, 3, ty + 6, 12, ty + 7, NAVY, None)
        for x in (4, 6, 8, 10):
            d.point((x, ty + 7), fill=NAVY_LT)


PANTS = hx("#23212b")


def long_legs(d, direction, color):
    """Pulls trousers down over the legs to the shoes."""
    for x in (5, 9) if direction in ("down", "up") else (7,):
        rect(d, x, 19, x + 1, 19, color, None)


def draw_uniform_boy(d, direction, step, boat, L, hy, ty):
    """White polo with a button placket, black pants and black shoes."""
    tee_arms(d, direction, step, ty, L["skin"])
    if direction == "down":
        rect(d, 7, ty, 8, ty + 2, hx("#cfd2da"), None)
    if not boat:
        long_legs(d, direction, PANTS)


BLOUSE = hx("#f4eee3")
PERI = hx("#98a4e8")
PERI_LT = hx("#c9d0f5")


def draw_casual_girl(d, direction, step, boat, L, hy, ty):
    """Ruffled off-white sailor blouse with a navy bow, and a high-waisted ruffled periwinkle suspender skirt."""
    rect(d, 5, ty, 10, ty + 2, BLOUSE, shade(BLOUSE, 0.88))
    for x in range(5, 11):
        d.point((x, ty), fill=BLOUSE if x % 2 else PERI_LT)
    if direction in ("down", "up"):
        paint_arms(d, step, ty, 1, 2, BLOUSE)
        paint_arms(d, step, ty, 3, 3, shade(BLOUSE, 0.88))
        paint_arms(d, step, ty, 4, 4, L["skin"])
    rect(d, 5, ty + 3, 10, ty + 5, PERI, shade(PERI, 0.8))
    for x in (6, 9):
        d.line([(x, ty + 1), (x, ty + 2)], fill=PERI)
    if direction == "down":
        rect(d, 6, ty, 9, ty, NAVY, None)
        rect(d, 7, ty + 1, 8, ty + 2, NAVY, None)
    if not boat:
        rect(d, 3, ty + 6, 12, ty + 6, PERI, None)
        for x in range(3, 13):
            d.point((x, ty + 7), fill=PERI_LT if x % 2 else PERI)


JACKET = hx("#1f1d27")
FUR_TRIM = hx("#d4d1da")
HUSSAR_SHIRT = hx("#4d7fd0")


def draw_casual_boy(d, direction, step, boat, L, hy, ty):
    """Black fur-trimmed hussar jacket with gold frogs, worn open over a blue shirt with a white collar."""
    rect(d, 5, ty, 10, ty, FUR_TRIM, None)
    rect(d, 5, ty + 5, 10, ty + 5, FUR_TRIM, None)
    if direction in ("down", "up"):
        paint_arms(d, step, ty, 4, 4, FUR_TRIM)
    if direction == "down":
        rect(d, 7, ty + 2, 8, ty + 5, HUSSAR_SHIRT, None)
        rect(d, 7, ty, 8, ty + 1, FUR, None)
        for y in (ty + 2, ty + 4):
            rect(d, 5, y, 6, y, GOLD, None); rect(d, 9, y, 10, y, GOLD, None)
    if not boat:
        long_legs(d, direction, PANTS)


# Each outfit holds a (look overrides, draw function) pair per sibling it was drawn for.
OUTFITS = {
    "witch": {"sister": (dict(hood=WITCH_BLACK, shorts=WITCH_BLACK, shoe=hx("#2a2038"), pack=WITCH_BLACK,
                              trim=WITCH_PURPLE, skirt=True, clip=None), draw_witch)},
    "idol": {"sister": (dict(hood=hx("#fbf4f8"), shorts=IDOL_BLACK, shoe=hx("#2a2630"), pack=IDOL_BLACK,
                             trim=IDOL_BLACK, skirt=True, clip=hx("#ffffff")), draw_idol)},
    "reaper": {"brother": (dict(hood=REAPER_CLOAK, shorts=hx("#2a2e48"), shoe=hx("#20243a"), pack=REAPER_CLOAK,
                                trim=REAPER_CLOAK, skirt=True, clip=None, blush=None), draw_reaper)},
    "stage": {"brother": (dict(hood=STAGE_BLUE, shorts=STAGE_WHITE, shoe=hx("#ffffff"), pack=STAGE_BLUE,
                               trim=STAGE_WHITE, skirt=False, clip=None, blush=None), draw_stage)},
    "christmas": {
        "sister": (dict(hood=XMAS_RED, shorts=XMAS_RED, shoe=hx("#3a2a2a"), pack=XMAS_RED, trim=XMAS_RED,
                        skirt=True, clip=None), draw_santa_girl),
        "brother": (dict(hood=XMAS_RED, shorts=XMAS_RED, shoe=BELT, pack=hx("#8a5a3a"), trim=FUR,
                         skirt=False), draw_santa)},
    "summer": {
        "sister": (dict(hood=hx("#f7f4ee"), shorts=ORANGE, shoe=hx("#c98a5a"), pack=hx("#f7f4ee"),
                        trim=hx("#f7f4ee"), skirt=False, clip=None), draw_summer_girl),
        "brother": (dict(hood=SHIRT_BLUE, shorts=hx("#fafafa"), shoe=FRAME, pack=SHIRT_BLUE, trim=SHIRT_BLUE,
                         skirt=False), draw_summer_boy)},
    "uniform": {
        "sister": (dict(hood=SWEATER_PINK, shorts=NAVY, shoe=hx("#f4f1f8"), pack=SWEATER_PINK, trim=SWEATER_PINK,
                        skirt=True, clip=None), draw_uniform_girl),
        "brother": (dict(hood=hx("#f6f6f8"), shorts=PANTS, shoe=PANTS, pack=hx("#f6f6f8"), trim=hx("#f6f6f8"),
                         skirt=False), draw_uniform_boy)},
    "casual": {
        "sister": (dict(hood=BLOUSE, shorts=PERI, shoe=hx("#fffdf5"), pack=BLOUSE, trim=BLOUSE,
                        skirt=True), draw_casual_girl),
        "brother": (dict(hood=JACKET, shorts=PANTS, shoe=PANTS, pack=JACKET, trim=JACKET,
                         skirt=False), draw_casual_boy)},
}


def player_frame(direction, step, boat, who="sister", outfit=None):
    """One 16x24 frame, chibi proportions (big head). step 0..3 (0,2 stand; 1,3 stride).
    `outfit` is a shop outfit design ("witch", "idol"): it restyles the same frame, so animation and hitbox never change."""
    L = LOOKS[who]
    if outfit:
        look, draw = OUTFITS[outfit][who]
        L = dict(L, **look)
    skin, hair, hood, shorts, shoe, pack = L["skin"], L["hair"], L["hood"], L["shorts"], L["shoe"], L["pack"]
    short = L["style"] == "short"
    long_ = L["style"] == "long"
    skirt = L["skirt"]
    trim = L["trim"]
    sx0, sx1 = (4, 11) if skirt else (5, 10)
    im, d = canvas(16, 24)
    dy = 2 if boat else 0
    bob = 1 if step in (1, 3) and not boat else 0
    d.ellipse([3, 21, 12, 23], fill=(60, 40, 90, 55))
    hy = 2 + dy + bob          # head top
    ty = hy + 9                # torso top
    dark = shade(hood, 0.62)

    if not boat:  # legs + shoes
        l_off = {0: 0, 1: -1, 2: 0, 3: 1}[step]
        r_off = {0: 0, 1: 1, 2: 0, 3: -1}[step]
        if direction in ("down", "up"):
            rect(d, sx0, ty + 6, sx1, ty + 7, shorts, None)
            rect(d, 5, min(19, ty + 8 + max(0, l_off)), 6, 19, skin, None); rect(d, 9, min(19, ty + 8 + max(0, r_off)), 10, 19, skin, None)
            rect(d, 4, 20 + min(0, l_off), 7, 21 + min(0, l_off), shoe); rect(d, 8, 20 + min(0, r_off), 11, 21 + min(0, r_off), shoe)
        else:
            sx = 1 if direction == "right" else -1
            rect(d, sx0 + 1, ty + 6, sx1 - 1, ty + 7, shorts, None); rect(d, 7, min(19, ty + 8), 8, 19, skin, None)
            fx = 7 + sx * {0: 0, 1: 2, 2: 0, 3: -2}[step]
            rect(d, fx - 1, 20, fx + 2, 21, shoe)

    # torso (hoodie)
    rect(d, 5, ty, 10, ty + 5, hood, dark)
    if direction == "up":
        rect(d, 5, ty + 1, 10, ty + 5, pack, shade(pack, 0.6))
    elif direction == "down":
        d.point((7, ty + 1), fill=trim); d.point((8, ty + 1), fill=trim); d.point((8, ty + 3), fill=shade(trim, 0.75))
    else:
        bx = 4 if direction == "right" else 11
        rect(d, bx - 1, ty + 1, bx + 1, ty + 5, pack, shade(pack, 0.6))
    if direction in ("down", "up"):
        a = {0: 0, 1: 1, 2: 0, 3: -1}[step]
        rect(d, 3, ty + 1 + max(0, a), 4, ty + 4 + max(0, a), hood, dark); rect(d, 11, ty + 1 + max(0, -a), 12, ty + 4 + max(0, -a), hood, dark)

    # head: the border is a darker shade of the skin tone (hair covers it wherever there is hair), never black
    rect(d, 3, hy, 12, hy + 8, skin, shade(skin, 0.80))
    if direction == "up":
        if short: rect(d, 3, hy, 12, hy + 7, hair, None)   # hair covers the whole back of the head; only the nape shows skin
        else: rect(d, 3, hy, 12, hy + 8, hair, hair)
        if long_: rect(d, 4, hy + 9, 11, hy + 11, hair, None)
    elif direction == "down":
        if short:
            rect(d, 3, hy, 12, hy + 2, hair, None); rect(d, 3, hy + 1, 4, hy + 3, hair, None); rect(d, 11, hy + 1, 12, hy + 3, hair, None)
        else:
            side = 9 if long_ else 6
            rect(d, 3, hy, 12, hy + 3, hair, hair); rect(d, 3, hy, 4, hy + side, hair, None); rect(d, 11, hy, 12, hy + side, hair, None)
        d.point((6, hy + 5), fill=INK); d.point((9, hy + 5), fill=INK)
        if L["blush"]:
            d.point((5, hy + 6), fill=L["blush"]); d.point((10, hy + 6), fill=L["blush"])
    else:
        left = direction == "left"
        if short:
            rect(d, 3, hy, 12, hy + 2, hair, None)
            if left: rect(d, 8, hy, 12, hy + 5, hair, None)
            else: rect(d, 3, hy, 7, hy + 5, hair, None)
        else:
            rect(d, 3, hy, 12, hy + 3, hair, None)
            if left: rect(d, 8, hy, 12, hy + 6, hair, None)
            else: rect(d, 3, hy, 7, hy + 6, hair, None)
            if long_:
                if left: rect(d, 8, hy + 7, 12, hy + 10, hair, None)
                else: rect(d, 3, hy + 7, 7, hy + 10, hair, None)
        ex = 5 if left else 10
        d.point((ex, hy + 5), fill=INK)   # (no blush dot in the side views)
    if L["clip"]:
        d.point((7, hy + 1), fill=L["clip"]); d.point((8, hy + 1), fill=L["clip"])  # hair clip

    if L["dirty"]:
        for x in (5, 8, 10):                       # messy hair tufts sticking up
            d.point((x, hy - 1), fill=hair)
        if direction == "down":                    # smudged face
            d.point((4, hy + 7), fill=MUD); d.point((5, hy + 8), fill=MUD_DARK); d.point((11, hy + 7), fill=MUD); d.point((8, hy + 4), fill=MUD)
        elif direction != "up":
            d.point((6, hy + 7), fill=MUD); d.point((9, hy + 8), fill=MUD_DARK); d.point((8, hy + 4), fill=MUD)
        if direction != "up":                      # stains on the hoodie, shorts and legs
            for pt in ((6, ty + 4), (7, ty + 4), (9, ty + 2), (5, ty + 5)):
                d.point(pt, fill=MUD)
            d.point((10, ty + 5), fill=MUD_DARK)
        else:
            for pt in ((6, ty + 3), (9, ty + 4), (7, ty + 5)):
                d.point(pt, fill=MUD)
        if not boat:
            d.point((5, ty + 7), fill=MUD); d.point((9, ty + 7), fill=MUD_DARK)
            d.point((5 if direction != "right" else 8, 18), fill=MUD)

    if outfit:
        draw(d, direction, step, boat, L, hy, ty)

    if boat:  # hull + oars over the legs
        d.polygon([(0, 17), (15, 17), (13, 23), (2, 23)], fill=hx("#e0b088"), outline=hx("#6a4a3a"))
        d.line([(2, 19), (13, 19)], fill=hx("#f0cca6"))
        o = 2 if step in (1, 3) else 0
        d.line([(1, 13 + o), (3, 19)], fill=hx("#6a4a3a")); d.line([(14, 13 + o), (12, 19)], fill=hx("#6a4a3a"))
    return im


def player_sheets():
    """player_<sibling>.png + player_<sibling>_boat.png (clean, playable) and player_<sibling>_dirty.png (story).
    Each sheet is 4 columns x 4 rows of 16x24 frames, scaled x3."""
    order = ["down", "left", "right", "up"]
    for who in LOOKS:
        variants = ((False, "player_%s_dirty.png" % who[:-6]),) if who.endswith("_dirty") else \
                   ((False, "player_%s.png" % who), (True, "player_%s_boat.png" % who))
        for boat, name in variants:
            sheet = Image.new("RGBA", (16 * 4, 24 * 4), (0, 0, 0, 0))
            for r, dr in enumerate(order):
                for c in range(4):
                    sheet.paste(player_frame(dr, c, boat, who), (c * 16, r * 24))
            save(name, sheet)


def outfit_sheets():
    """player_<sibling>_<outfit>.png and ..._boat.png for each shop outfit design, for the sibling it was drawn for."""
    order = ["down", "left", "right", "up"]
    for who in ("sister", "brother"):
        for outfit, drawn_for in OUTFITS.items():
            if who not in drawn_for:
                continue
            for boat in (False, True):
                sheet = Image.new("RGBA", (16 * 4, 24 * 4), (0, 0, 0, 0))
                for r, dr in enumerate(order):
                    for c in range(4):
                        sheet.paste(player_frame(dr, c, boat, who, outfit), (c * 16, r * 24))
                save("player_%s_%s%s.png" % (who, outfit, "_boat" if boat else ""), sheet)


# ----------------------------------------------------------------------------- scenes

SCENES = {
    #            ground a     ground b     road         water        sky top      sky bottom
    "house":  ("#d4f2d8", "#bfe8c8", "#cbc7da", "#8fd0e8", "#bfe3ff", "#fdeef6"),
    "park":   ("#cdf0d0", "#b6e4c0", "#d0cadd", "#8fd0e8", "#bfe3ff", "#fdeef6"),
    "river":  ("#d0f0d6", "#bde6c6", "#c4c2d4", "#7fc4e0", "#bfe3ff", "#fdeef6"),
    "ind":    ("#cfe0c8", "#b9d0b4", "#b8b8c8", "#8fb4c8", "#c0cde0", "#ece8f4"),
    "market": ("#d4f2d8", "#bfe8c8", "#e2cfb4", "#8fd0e8", "#bfe3ff", "#fdeef6"),
    "beach":  ("#f8ecc4", "#efdcab", "#e6c9a0", "#8fe0e8", "#bfe8ff", "#fff4e4"),
    "dump":   ("#d4ccaa", "#c0b490", "#b8b2a4", "#a8d078", "#d4d0c0", "#efe8d0"),
    "arctic": ("#f4f8fc", "#dfe9f4", "#cfd8e6", "#a8d4f0", "#d4e8fa", "#f8f4ff"),
    "jungle": ("#a8dca8", "#8fcc98", "#c8ae88", "#78d0b0", "#a8e0c0", "#e4f6dc"),
    "city":   ("#aeaec8", "#9a9cb8", "#80809c", "#6a8ad0", "#9a8ad0", "#e0c0e8"),
}

# Omori-style teal "bubble" trees: fill, outline, highlight
TEAL = (hx("#63d3d0"), hx("#3aa5ae"), hx("#b4f4f0"))
TREE_PAL = {"arctic": (hx("#9fd8f0"), hx("#5a9ac0"), hx("#e4f6ff")), "dump": (hx("#9ec27a"), hx("#5e7a4a"), hx("#d4e8b0")),
            "ind": (hx("#8fc8c0"), hx("#4f8f90"), hx("#cdf0ea")), "jungle": (hx("#58c890"), hx("#2a8a60"), hx("#a8f0c8")),
            "city": (hx("#8a8ad8"), hx("#4a4a9a"), hx("#c8c8f8"))}


def pastel(im, amt=0.2):
    """Wash an image toward soft lilac-white (keeps alpha) for the dreamy look."""
    layer = Image.new("RGBA", im.size, (238, 230, 252, 255))
    out = Image.blend(im, layer, amt)
    out.putalpha(im.getchannel("A"))
    return out


def bubbles(d, x, y, r, pal=TEAL):
    """A round cluster of three overlapping bubbles (outline pass first, then fill, then shine)."""
    fill, out, hi = pal
    parts = [(-0.62 * r, 0.0, 0.78 * r), (0.62 * r, 0.0, 0.78 * r), (0.0, -0.55 * r, 0.95 * r)]
    for grow, col in ((1, out), (0, fill)):
        for dx, dy, rr in parts:
            d.ellipse([x + dx - rr - grow, y + dy - rr - grow, x + dx + rr + grow, y + dy + rr + grow], fill=col)
    d.ellipse([x - 0.35 * r - 1, y - 0.95 * r, x - 0.35 * r + 2, y - 0.95 * r + 3], fill=hi)
    d.point((int(x + 0.5 * r), int(y - 0.2 * r)), fill=hi)


def noise(d, w, h, base, seed, n, lighter=True, y0=0):
    rnd = random.Random(seed)
    for _ in range(n):
        x, y = rnd.randrange(w), y0 + rnd.randrange(h - y0)
        d.point((x, y), fill=shade(base, 1.12 if lighter and rnd.random() < .5 else 0.88))


def tile_sprites():
    for sc, (ga, gb, rd, wt, _, _) in SCENES.items():
        for variant in ("a", "b"):
            im, d = canvas()
            base = hx(ga)
            d.rectangle([0, 0, 15, 15], fill=base)
            noise(d, 16, 16, base, hash(sc) % 997 + ord(variant), 10)
            if variant == "b":  # tiny pastel flower (or grass tuft on beach / arctic)
                if sc in ("beach", "arctic", "dump", "city"):
                    d.point((4, 5), fill=shade(base, 0.8)); d.point((4, 4), fill=shade(base, 0.8)); d.point((11, 11), fill=shade(base, 0.8))
                else:
                    for (fx, fy, pc) in ((4, 5, "#ffc2dc"), (11, 11, "#fff2a8")):
                        d.point((fx, fy - 1), fill=hx(pc)); d.point((fx, fy + 1), fill=hx(pc)); d.point((fx - 1, fy), fill=hx(pc)); d.point((fx + 1, fy), fill=hx(pc))
                        d.point((fx, fy), fill=hx("#ffffff"))
            save("tiles/ground_%s_%s.png" % (sc, variant), im)
        im, d = canvas()  # road
        base = hx(rd)
        d.rectangle([0, 0, 15, 15], fill=base)
        noise(d, 16, 16, base, 77 + len(sc), 8)
        if sc == "beach":
            for y in (3, 7, 11, 15): d.line([(0, y), (15, y)], fill=shade(base, 0.85))
        elif sc == "market":
            for y in (0, 8):
                for x in range(0, 16, 4): d.rectangle([x + (y // 8) * 2, y, x + 2 + (y // 8) * 2, y + 6], outline=shade(base, 0.88))
        else:
            d.line([(2, 8), (6, 8)], fill=hx("#fbf9ff")); d.line([(10, 8), (14, 8)], fill=hx("#fbf9ff"))
        save("tiles/road_%s.png" % sc, im)
        base = hx(wt)
        for f in (0, 1):  # water, two frames, with sparkles
            im, d = canvas()
            d.rectangle([0, 0, 15, 15], fill=base)
            for (x, y) in [(2, 3), (9, 6), (4, 11), (12, 13)]:
                xx = (x + f * 2) % 14
                d.line([(xx, y), (xx + 3, y)], fill=shade(base, 1.22))
            sp = [(5, 2), (12, 9), (2, 13)] if f == 0 else [(10, 3), (3, 8), (13, 14)]
            for p in sp: d.point(p, fill=(255, 255, 255, 255))
            save("tiles/water_%s_%d.png" % (sc, f), im)
        im, d = canvas()  # bridge: light planks with side rails (clearly a deck, not water)
        d.rectangle([0, 0, 15, 15], fill=hx("#e4bf94"))
        for y in range(0, 16, 4): d.line([(0, y), (15, y)], fill=hx("#b98a5e"))
        d.rectangle([0, 0, 1, 15], fill=hx("#8a6a8a")); d.rectangle([14, 0, 15, 15], fill=hx("#8a6a8a"))
        for y in (1, 8, 14): d.rectangle([0, y, 2, y + 1], fill=hx("#5a4470")); d.rectangle([13, y, 15, y + 1], fill=hx("#5a4470"))
        save("tiles/bridge_%s.png" % sc, im)


def vgrad(d, w, h, top, bottom):
    for y in range(h):
        t = y / max(1, h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (255,))


def skyline(d, w, y_base, seed, col=hx("#7a8a96"), tall=18):
    rnd = random.Random(seed)
    x = 0
    while x < w:
        bw, bh = rnd.randrange(4, 8), rnd.randrange(8, tall)
        d.rectangle([x, y_base - bh, x + bw, y_base], fill=col)
        for wy in range(y_base - bh + 2, y_base - 1, 3):
            for wx in range(x + 1, x + bw - 1, 2): d.point((wx, wy), fill=hx("#f0e6a0"))
        x += bw + 1


def tree(d, x, y, r=5):
    d.rectangle([x - 1, y, x + 1, y + 5], fill=hx("#6b4a2b"))
    d.ellipse([x - r, y - r - 1, x + r, y + r - 1], fill=hx("#2e8b4a"), outline=hx("#1a5a30"))
    d.ellipse([x - r + 2, y - r + 1, x - 1, y - 1], fill=hx("#4cb866"))


def palm(d, x, y):
    d.line([(x, y + 12), (x + 1, y + 2)], fill=hx("#8a5a2b"), width=2)
    for dx, dy in [(-6, 2), (6, 2), (-4, -2), (4, -2), (0, -4)]: d.line([(x + 1, y + 2), (x + 1 + dx, y + 2 + dy)], fill=hx("#2e8b4a"), width=2)


def house(d, x, y, roof="#c4463d"):
    d.rectangle([x, y + 6, x + 16, y + 15], fill=hx("#f4e3c3"), outline=INK)
    d.polygon([(x - 2, y + 6), (x + 8, y - 2), (x + 18, y + 6)], fill=hx(roof), outline=INK)
    d.rectangle([x + 6, y + 9, x + 10, y + 15], fill=hx("#7a4a2b")); d.rectangle([x + 2, y + 8, x + 4, y + 10], fill=hx("#9ad1f2")); d.rectangle([x + 12, y + 8, x + 14, y + 10], fill=hx("#9ad1f2"))


def factory(d, x, y):
    d.rectangle([x, y + 4, x + 18, y + 15], fill=hx("#6d7278"), outline=INK)
    d.rectangle([x + 2, y - 4, x + 5, y + 4], fill=hx("#50555a")); d.rectangle([x + 11, y - 1, x + 14, y + 4], fill=hx("#50555a"))
    for i, (cx, cy) in enumerate([(x + 3, y - 6), (x + 5, y - 9), (x + 12, y - 4)]): d.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=(120, 120, 120, 160))


def stall(d, x, y, awn):
    d.rectangle([x, y + 4, x + 18, y + 8], fill=hx(awn), outline=INK)
    for i in range(0, 18, 6): d.rectangle([x + i, y + 4, x + i + 2, y + 8], fill=(255, 255, 255, 120))
    d.rectangle([x + 1, y + 8, x + 17, y + 15], fill=hx("#8b6b4a"), outline=INK)
    for i, c in enumerate(["#d9473a", "#f28c28", "#3aa564"]): d.ellipse([x + 3 + i * 5, y + 10, x + 6 + i * 5, y + 13], fill=hx(c))


def mound(d, x, y, junk=0):
    d.polygon([(x - 9, y + 15), (x, y + 4), (x + 9, y + 15)], fill=hx("#6b5d43"), outline=INK)
    if junk == 0: d.rectangle([x - 3, y + 2, x + 3, y + 9], fill=hx("#3a6ea5"), outline=INK)
    elif junk == 1: d.rectangle([x - 3, y + 3, x + 3, y + 9], fill=hx("#7b848c"), outline=INK)
    else: d.ellipse([x - 4, y + 4, x + 4, y + 12], fill=hx("#2a2a2a")); d.ellipse([x - 1, y + 7, x + 1, y + 9], fill=hx("#888888"))


def iceberg(d, x, y):
    d.polygon([(x - 8, y + 15), (x, y + 1), (x + 8, y + 15)], fill=hx("#dbe8f2"), outline=hx("#9fb8c9")); d.polygon([(x - 3, y + 6), (x, y + 1), (x + 3, y + 6)], fill=(255, 255, 255, 255))


def backdrop_sprites():
    for sc, (ga, gb, _, _, st, sb) in SCENES.items():
        W, H = 240, 48  # logical; x3 => 720x144 (rows 0..2)
        im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        vgrad(d, W, 32, hx(st), hx(sb))
        d.ellipse([160, 4, 172, 16], fill=hx("#fff2c8"))
        for cx in (30, 95, 210):
            d.ellipse([cx, 8, cx + 14, 15], fill=(255, 255, 255, 225)); d.ellipse([cx + 8, 5, cx + 22, 14], fill=(255, 255, 255, 225))
        sky_col = {"city": hx("#6a6ea8"), "ind": hx("#a0a8c0"), "dump": hx("#aaa890"), "arctic": hx("#c4d8ea"), "jungle": hx("#6ab890")}.get(sc, hx("#b8b0d4"))
        skyline(d, W, 32, hash(sc) % 1000, sky_col, 20 if sc == "city" else 16)
        d.rectangle([0, 32, W, H], fill=hx(ga))
        noise(d, W, H, hx(ga), 5, 40, y0=32)
        pal = TREE_PAL.get(sc, TEAL)
        # back hedge of bubble trees, like the forest wall in the reference
        for x in range(2, W + 8, 15): bubbles(d, x, 33 + (x * 7) % 4, 7, pal)
        if sc == "house":
            for x in (12, 62, 112, 162, 205): house(d, x, 32, "#e8808a" if x % 3 else "#7aa8e0")
        elif sc in ("park", "jungle"):
            for x in range(14, 240, 40 if sc == "park" else 30): bubbles(d, x, 40 + (x % 3), 7 if sc == "park" else 8, pal)
        elif sc == "beach":
            for x in (20, 70, 120, 175, 220): palm(d, x, 34)
        elif sc == "market":
            for i, x in enumerate((6, 44, 82, 120, 158, 196)): stall(d, x, 32, ["#f08a8a", "#f7d88a", "#8fb8f0"][i % 3])
        elif sc == "dump":
            for i, x in enumerate((25, 70, 125, 175, 220)): mound(d, x, 32, i % 3)
        elif sc == "arctic":
            for x in (40, 100, 160, 215): iceberg(d, x, 32)
        elif sc in ("river", "ind"):
            for x in (8, 42, 175, 205): factory(d, x, 32)
        elif sc == "city":
            for i, x in enumerate((10, 60, 110, 160, 205)):
                h = [14, 18, 15, 19, 13][i]
                d.rectangle([x, 47 - h, x + 18, 47], fill=hx("#5a5e98"), outline=hx("#34386a"))
                for wy in range(47 - h + 2, 45, 3):
                    for wx in (x + 3, x + 8, x + 13): d.point((wx, wy), fill=hx("#ffe9a8"))
        save("backdrop/%s.png" % sc, pastel(im, 0.14))

    # title screen: 240x160 -> 720x480
    W, H = 240, 160
    im = Image.new("RGBA", (W, H))
    d = ImageDraw.Draw(im)
    vgrad(d, W, 100, hx("#bfe3ff"), hx("#fdeef6"))
    d.ellipse([190, 10, 206, 26], fill=hx("#fff2c8"))
    for cx, cy in ((20, 20), (90, 34), (150, 14)):
        d.ellipse([cx, cy, cx + 18, cy + 8], fill=(255, 255, 255, 235)); d.ellipse([cx + 8, cy - 3, cx + 26, cy + 7], fill=(255, 255, 255, 235))
    skyline(d, W, 96, 11, hx("#b8b0d4"), 26)
    d.rectangle([0, 96, W, H], fill=hx("#d4f2d8"))
    noise(d, W, H, hx("#d4f2d8"), 3, 160, y0=96)
    d.rectangle([0, 120, W, 134], fill=hx("#cbc7da"))
    for x in range(4, W, 22): d.rectangle([x, 127, x + 8, 128], fill=hx("#fbf9ff"))
    for x in range(-4, W + 12, 17): bubbles(d, x, 100 + (x * 5) % 5, 9)
    for x, y in ((10, 112), (60, 110), (190, 112), (228, 110)): bubbles(d, x, y, 10)
    save("backdrop/title.png", pastel(im, 0.1))


# ----------------------------------------------------------------------------- story mode art

def person_frame(direction, step, boat, p):
    """Same chibi body as player_frame, but palette / hair style / messiness come from dict p."""
    im, d = canvas(16, 24)
    SKIN_, HAIR_, TOP_, SHORTS_, SHOE_, PACK_ = p["skin"], p["hair"], p["top"], p["shorts"], p["shoe"], p["pack"]
    dy = 2 if boat else 0
    bob = 1 if step in (1, 3) and not boat else 0
    d.ellipse([3, 21, 12, 23], fill=(60, 40, 90, 55))
    hy = 2 + dy + bob
    ty = hy + 9
    dark = shade(TOP_, 0.62)
    short = p["style"] == "short"

    if not boat:
        l_off = {0: 0, 1: -1, 2: 0, 3: 1}[step]
        r_off = {0: 0, 1: 1, 2: 0, 3: -1}[step]
        if direction in ("down", "up"):
            rect(d, 5, ty + 6, 10, ty + 7, SHORTS_, None)
            rect(d, 5, min(19, ty + 8 + max(0, l_off)), 6, 19, SKIN_, None); rect(d, 9, min(19, ty + 8 + max(0, r_off)), 10, 19, SKIN_, None)
            rect(d, 4, 20 + min(0, l_off), 7, 21 + min(0, l_off), SHOE_); rect(d, 8, 20 + min(0, r_off), 11, 21 + min(0, r_off), SHOE_)
        else:
            sx = 1 if direction == "right" else -1
            rect(d, 6, ty + 6, 9, ty + 7, SHORTS_, None); rect(d, 7, min(19, ty + 8), 8, 19, SKIN_, None)
            fx = 7 + sx * {0: 0, 1: 2, 2: 0, 3: -2}[step]
            rect(d, fx - 1, 20, fx + 2, 21, SHOE_)

    rect(d, 5, ty, 10, ty + 5, TOP_, dark)
    if direction == "up":
        rect(d, 5, ty + 1, 10, ty + 5, PACK_, shade(PACK_, 0.6))
    elif direction == "down":
        d.point((7, ty + 1), fill=hx("#ffffff")); d.point((8, ty + 1), fill=hx("#ffffff")); d.point((8, ty + 3), fill=shade(TOP_, 0.8))
    else:
        bx = 4 if direction == "right" else 11
        rect(d, bx - 1, ty + 1, bx + 1, ty + 5, PACK_, shade(PACK_, 0.6))
    if direction in ("down", "up"):
        a = {0: 0, 1: 1, 2: 0, 3: -1}[step]
        rect(d, 3, ty + 1 + max(0, a), 4, ty + 4 + max(0, a), TOP_, dark); rect(d, 11, ty + 1 + max(0, -a), 12, ty + 4 + max(0, -a), TOP_, dark)

    # head
    rect(d, 3, hy, 12, hy + 8, SKIN_, INK)
    if direction == "up":
        if short:
            rect(d, 3, hy, 12, hy + 5, HAIR_, INK)
        else:
            rect(d, 3, hy, 12, hy + 8, HAIR_, INK)
    elif direction == "down":
        if short:
            rect(d, 3, hy, 12, hy + 2, HAIR_, INK); d.point((3, hy + 3), fill=HAIR_); d.point((12, hy + 3), fill=HAIR_)
            for sx_ in (5, 8, 10): d.point((sx_, hy - 1), fill=HAIR_)
        else:
            rect(d, 3, hy, 12, hy + 3, HAIR_, INK); rect(d, 3, hy, 4, hy + 6, HAIR_, None); rect(d, 11, hy, 12, hy + 6, HAIR_, None)
        d.point((6, hy + 5), fill=INK); d.point((9, hy + 5), fill=INK)
        d.point((5, hy + 6), fill=BLUSH); d.point((10, hy + 6), fill=BLUSH)
    else:
        left = direction == "left"
        rect(d, 3, hy, 12, hy + (2 if short else 3), HAIR_, INK)
        if left: rect(d, 8, hy, 12, hy + (4 if short else 6), HAIR_, INK)
        else: rect(d, 3, hy, 7, hy + (4 if short else 6), HAIR_, INK)
        ex = 5 if left else 10
        d.point((ex, hy + 5), fill=INK); d.point((ex + (-1 if left else 1), hy + 6), fill=BLUSH)
        if short:
            for sx_ in (5, 8): d.point((sx_, hy - 1), fill=HAIR_)
    if not short:
        d.point((7, hy + 1), fill=p["clip"]); d.point((8, hy + 1), fill=p["clip"])

    if p.get("messy"):
        for (x, y) in ((6, ty + 1), (9, ty + 3), (7, ty + 4), (5, ty + 5)): d.point((x, y), fill=hx("#8a7a4a"))
        d.point((10, ty + 1), fill=hx("#5e7a4a")); d.point((6, ty + 3), fill=hx("#5e7a4a"))
        if direction == "down": d.point((10, hy + 7), fill=hx("#b49a80")); d.point((5, hy + 4), fill=hx("#b49a80"))
        for x in (4, 7, 10, 12): d.point((x, hy - 1), fill=HAIR_)
        d.point((6, hy - 2), fill=HAIR_); d.point((11, hy - 2), fill=HAIR_)

    if boat:
        d.polygon([(0, 17), (15, 17), (13, 23), (2, 23)], fill=hx("#e0b088"), outline=hx("#6a4a3a"))
        d.line([(2, 19), (13, 19)], fill=hx("#f0cca6"))
        o = 2 if step in (1, 3) else 0
        d.line([(1, 13 + o), (3, 19)], fill=hx("#6a4a3a")); d.line([(14, 13 + o), (12, 19)], fill=hx("#6a4a3a"))
    return im


NPCS = [  # villagers, one per campaign level (front-facing, single frame)
    dict(skin="#e8c4a0", hair="#9a9aa8", top="#e89ab0", shorts="#7a6a9a", style="bob", clip="#ffffff"),    # Aling Nena
    dict(skin="#d9a87c", hair="#6a6a70", top="#6aa86a", shorts="#5a4a3a", style="short", clip="#ffffff"),  # Mang Ben
    dict(skin="#c8956a", hair="#2a2a30", top="#6a8ac8", shorts="#4a5a6a", style="short", clip="#ffffff"),  # Tatay Ramon
    dict(skin="#e8c4a0", hair="#4a2a20", top="#e0a040", shorts="#4a4a5a", style="bob", clip="#e0a040"),    # Ate Rosa
    dict(skin="#d9a87c", hair="#b0b0b8", top="#d9566f", shorts="#7a5a4a", style="bob", clip="#ffffff"),    # Lola Remy
    dict(skin="#c8956a", hair="#1a1a20", top="#e85a4a", shorts="#2a6a9a", style="short", clip="#ffffff"),  # Kuya Jun
    dict(skin="#b8855a", hair="#5a5a60", top="#8a8a50", shorts="#5a5a4a", style="short", clip="#ffffff"),  # Mang Dodoy
    dict(skin="#f0d0b8", hair="#e8e8f0", top="#9fd8f0", shorts="#5a7a9a", style="bob", clip="#4a8ac0"),    # Dr. Frost
    dict(skin="#c8956a", hair="#3a2a20", top="#3a8a5a", shorts="#6a5a3a", style="short", clip="#ffffff"),  # Ka Tomas
    dict(skin="#e8c4a0", hair="#2a2a3a", top="#8a6ac8", shorts="#3a3a5a", style="bob", clip="#f0d060"),    # Mayor Cruz
]


def npc_frame(p, i):
    """One front-facing 16x24 villager. No black outline: the head is flat skin framed by hair, clothes use a darker shade of
    their own colour, and each villager gets props that fit their level (see the per-index block at the end)."""
    SK, HR, TP, SH, SHOE = p["skin"], p["hair"], p["top"], p["shorts"], p["shoe"]
    im, d = canvas(16, 24)
    CLEAR = (0, 0, 0, 0)
    EYE = (52, 40, 60, 255)

    def P(x, y, c): d.point((x, y), fill=c)
    def R(x0, y0, x1, y1, c, o=None): rect(d, x0, y0, x1, y1, c, o)

    hy, ty = 2, 11
    dark, lite = shade(TP, 0.66), shade(TP, 1.18)
    skin_dk = shade(SK, 0.84)
    d.ellipse([3, 21, 12, 23], fill=(60, 40, 90, 55))

    # legs, shorts, shoes (soft sole line instead of black)
    R(5, ty + 6, 10, ty + 7, SH, None); P(5, ty + 7, shade(SH, 0.8)); P(10, ty + 7, shade(SH, 0.8))
    R(5, ty + 8, 6, 19, SK, None); R(9, ty + 8, 10, 19, SK, None)
    sole = shade(SHOE, 0.72)
    R(4, 20, 7, 21, SHOE, sole); R(8, 20, 11, 21, SHOE, sole)

    # torso + arms + hands
    R(5, ty, 10, ty + 5, TP, dark); R(6, ty + 1, 7, ty + 1, lite, None)
    R(3, ty + 1, 4, ty + 4, TP, None); R(11, ty + 1, 12, ty + 4, TP, None)
    for y in range(ty + 1, ty + 5): P(3, y, dark); P(12, y, dark)
    R(3, ty + 5, 4, ty + 5, SK, None); R(11, ty + 5, 12, ty + 5, SK, None)

    # head: flat skin, rounded chin, no outline
    R(3, hy, 12, hy + 8, SK, None)
    P(3, hy + 8, CLEAR); P(12, hy + 8, CLEAR)
    P(4, hy + 8, skin_dk); P(11, hy + 8, skin_dk)
    glint = shade(HR, 1.3)
    if p["style"] == "short":
        R(3, hy, 12, hy + 2, HR, None); P(3, hy + 3, HR); P(12, hy + 3, HR)
        for x in (5, 6, 7): P(x, hy, glint)
        P(2, hy + 5, skin_dk); P(13, hy + 5, skin_dk)   # ears
    else:
        R(3, hy, 12, hy + 3, HR, None); R(3, hy, 4, hy + 7, HR, None); R(11, hy, 12, hy + 7, HR, None)
        P(5, hy + 3, HR); P(10, hy + 3, HR)
        for x in (5, 6, 7): P(x, hy, glint)
    # face
    P(6, hy + 5, EYE); P(9, hy + 5, EYE)
    P(5, hy + 6, (244, 176, 190, 255)); P(10, hy + 6, (244, 176, 190, 255))
    P(7, hy + 7, (190, 100, 112, 255)); P(8, hy + 7, (190, 100, 112, 255))

    W = (252, 250, 246, 255)
    if i == 0:      # Aling Nena: bun, round glasses, apron with a pink pocket
        R(6, 0, 9, 1, HR, None); P(7, 0, glint); P(8, 0, glint)
        GL = (176, 166, 196, 255)
        d.rectangle([5, hy + 4, 7, hy + 6], outline=GL); d.rectangle([8, hy + 4, 10, hy + 6], outline=GL)
        R(6, ty + 1, 9, ty + 5, W, None); R(6, ty + 1, 9, ty + 1, (244, 176, 196, 255), None)
        R(7, ty + 4, 8, ty + 5, (244, 176, 196, 255), None)
    elif i == 1:    # Mang Berting: straw hat, overall bib
        STRAW, BAND = (238, 206, 122, 255), (156, 96, 56, 255)
        R(4, 0, 11, 1, STRAW, None); R(4, 1, 11, 1, BAND, None); R(1, 2, 14, 2, STRAW, None)
        for x in range(2, 14, 2): P(x, 2, shade(STRAW, 0.86))
        BIB = (112, 86, 58, 255)
        P(6, ty, BIB); P(9, ty, BIB); R(6, ty + 2, 9, ty + 5, BIB, None); R(7, ty + 3, 8, ty + 4, shade(BIB, 1.25), None)
    elif i == 2:    # Tatay Isko: fisherman cap with a visor, holding up a fresh catch
        CAP = (70, 98, 134, 255)
        R(4, 0, 11, 1, CAP, None); R(3, 2, 12, 2, shade(CAP, 0.85), None); R(4, 3, 11, 3, shade(CAP, 0.7), None)
        P(7, 0, shade(CAP, 1.25)); P(8, 0, shade(CAP, 1.25))
        FISH, FISH_D = (150, 196, 226, 255), (96, 142, 184, 255)
        R(11, ty + 6, 13, ty + 8, FISH, None); P(14, ty + 6, FISH_D); P(14, ty + 8, FISH_D); P(13, ty + 7, FISH_D)
        for x in (11, 12): P(x, ty + 8, (236, 244, 250, 255))
        P(11, ty + 6, (40, 40, 60, 255)); P(12, ty + 7, FISH_D)
    elif i == 3:    # Ate Cora: yellow hard hat, hi-vis stripes
        HAT = (244, 196, 48, 255)
        R(4, 0, 11, 1, HAT, None); R(7, 0, 8, 1, shade(HAT, 0.84), None); R(3, 2, 12, 2, shade(HAT, 0.84), None)
        R(5, ty + 2, 10, ty + 2, (250, 250, 216, 255), None); R(5, ty + 4, 10, ty + 4, (250, 250, 216, 255), None)
    elif i == 4:    # Aling Pacing: polka-dot headscarf, basket of veggies
        RED = (214, 78, 96, 255)
        R(3, 1, 12, 3, RED, None); R(12, 3, 13, 4, shade(RED, 0.8), None); P(14, 5, shade(RED, 0.8))
        for (x, y) in ((5, 2), (8, 1), (10, 3), (7, 3)): P(x, y, (252, 232, 238, 255))
        BR = (200, 150, 90, 255)
        P(0, ty + 4, BR); P(1, ty + 3, BR); P(2, ty + 4, BR)
        P(0, ty + 5, (96, 180, 96, 255)); P(1, ty + 5, (240, 140, 50, 255)); P(2, ty + 5, (96, 180, 96, 255))
        R(0, ty + 6, 2, ty + 7, BR, None); P(1, ty + 6, shade(BR, 0.8)); P(0, ty + 7, shade(BR, 0.8)); P(2, ty + 7, shade(BR, 0.8))
        R(6, ty + 3, 9, ty + 5, (250, 240, 220, 255), None); R(6, ty + 4, 9, ty + 4, (230, 120, 130, 255), None)
    elif i == 5:    # Kuya Bogs: red lanyard with a whistle, zinc nose
        CORD = (232, 70, 70, 255)
        P(6, ty, CORD); P(9, ty, CORD); P(7, ty + 1, CORD); P(8, ty + 1, CORD)
        R(7, ty + 2, 8, ty + 2, (250, 214, 70, 255), None)
        P(8, hy + 6, W)
    elif i == 6:    # Mang Basyo: flat cap, stubble, work gloves, sack of junk
        CAP = (122, 126, 136, 255)
        R(3, 1, 12, 2, CAP, None); R(4, 3, 11, 3, shade(CAP, 0.7), None)
        for (x, y) in ((5, hy + 7), (10, hy + 7), (6, hy + 8), (9, hy + 8)): P(x, y, shade(SK, 0.68))
        R(3, ty + 5, 4, ty + 5, (244, 200, 60, 255), None); R(11, ty + 5, 12, ty + 5, (244, 200, 60, 255), None)
        R(0, ty + 3, 2, ty + 7, (192, 172, 132, 255), None); P(1, ty + 2, (120, 90, 60, 255)); P(1, ty + 4, (150, 132, 100, 255)); P(0, ty + 6, (150, 132, 100, 255))
        R(8, ty + 3, 9, ty + 4, (108, 108, 72, 255), None)
    elif i == 7:    # Dr. Aurora Vega: fur-trim hood, goggles, clipboard
        FUR = (244, 248, 252, 255)
        d.line([(2, hy - 1), (13, hy - 1)], fill=FUR); d.line([(2, hy - 1), (2, hy + 6)], fill=FUR); d.line([(13, hy - 1), (13, hy + 6)], fill=FUR)
        R(4, ty, 11, ty, FUR, None)
        LENS = (122, 192, 238, 255)
        R(5, hy + 1, 7, hy + 2, LENS, None); R(8, hy + 1, 10, hy + 2, LENS, None); P(5, hy + 1, W); P(8, hy + 1, W)
        R(3, hy + 1, 4, hy + 1, (60, 72, 96, 255), None); R(11, hy + 1, 12, hy + 1, (60, 72, 96, 255), None)
        R(12, ty + 3, 14, ty + 7, (150, 106, 64, 255), None); R(12, ty + 4, 14, ty + 6, (250, 250, 252, 255), None); P(13, ty + 5, (120, 150, 200, 255))
    elif i == 8:    # Ka Lando: ranger hat sitting on his head, red neckerchief, khaki vest, gold badge
        R(4, 0, 11, 2, (122, 94, 58, 255), None); R(7, 0, 8, 0, (96, 72, 44, 255), None); R(4, 2, 11, 2, (212, 172, 62, 255), None)
        R(1, 3, 14, 3, (150, 112, 68, 255), None); R(2, 4, 13, 4, shade(SK, 0.78), None)
        KH = (180, 154, 96, 255)
        R(5, ty, 6, ty + 5, KH, None); R(9, ty, 10, ty + 5, KH, None); P(5, ty + 4, shade(KH, 0.7)); P(10, ty + 4, shade(KH, 0.7))
        R(7, ty, 8, ty + 1, (206, 66, 66, 255), None)
        P(9, ty + 2, (250, 212, 72, 255))
    elif i == 9:    # Mayor Liwanag: white collar, teal-and-gold sash, brooch
        R(6, ty, 9, ty, W, None)
        SASH, GOLD2 = (76, 196, 168, 255), (244, 208, 96, 255)
        for k in range(5):
            P(5 + k, ty + 1 + k, SASH); P(6 + k, ty + 1 + k, SASH); P(7 + k, ty + 1 + k, GOLD2) if 7 + k <= 10 else None
        R(5, ty + 1, 6, ty + 2, GOLD2, None)
    return im


def npc_sprites():
    for i, n in enumerate(NPCS):
        p = dict(skin=hx(n["skin"]), hair=hx(n["hair"]), top=hx(n["top"]), shorts=hx(n["shorts"]), shoe=hx("#ffffff"),
                 pack=hx("#f4d8a0"), style=n["style"], clip=hx(n["clip"]))
        save("npc/npc_%d.png" % i, npc_frame(p, i))


def queen_muck(mode="normal"):
    """Queen Muck, on the same 16x24 chibi grid as Mother Nature, the siblings and the villagers (scaled x3 = 48x72), drawn from
    the Gothic Trash Princess sheet: a big messy burgundy mane with magenta streaks, a silver tiara with pink gems, a black bow
    with a can charm, pin and crown tag, teal eyes, a choker, a black off-shoulder gown with a pink cross, a belt with a chain
    and crown tags, a torn layered skirt with X marks, and cross-laced platform boots with pink soles.
    mode: "normal" (hand on chest, frown), "rage" (hand on hip, anger vein), "laugh" (> < eyes, open mouth, fists up, one boot
    kicked up, 'ho ho ho'), "sad" (defeated, a tear, hands clasped) or "clean" (after the change: pastel gown, mint and pink)."""
    clean, rage, laugh, sad = mode == "clean", mode == "rage", mode == "laugh", mode == "sad"
    im, d = canvas(16, 24)

    def P(x, y, c):
        d.point((x, y), fill=c)

    def seg(y, x, chars):
        for i, ch in enumerate(chars):
            if ch != ".":
                P(x + i, y, C[ch])

    def box(x0, y0, x1, y1, c):
        d.rectangle([x0, y0, x1, y1], fill=c)

    if clean:
        C = dict(h="#c4557a", H="#8e3456", m="#dc7898", p="#ffc4d8", s="#fbe4cc", S="#eec8a8", J="#e6b890", E="#141016", W="#fff0e0", b=BLUSH, t="#7fc0c4", T="#2d5a66",
                 O="#3a2438", d="#f8eff2", D="#ffffff", k="#ee5f90", K="#c23c6c", u="#bfe8d0", U="#f6c2d6", v="#fff3b0", V="#e0c070",
                 g="#a8dcb6", G="#5faa7c", B="#fbf6f2", o="#ee7aa4", q="#c23c6c", y="#ffc9dc", Y="#ffffff", r="#8fd49a", z="#ee9ab8",
                 x="#c4506a")
    else:
        C = dict(h="#74204a", H="#3c1028", m="#98285a", p="#ff3a80" if rage else "#e02c78", s="#f9d9bd", S="#efc09c", J="#d99b72", E="#101010", W="#fce8d2", b=BLUSH,
                 t="#7fc0c4", T="#2d5a66", O="#14091a", d="#22142a", D="#3c2448", k="#e8306f", K="#a01c52", u="#52285a",
                 U="#c01c64", v="#e2e2ee", V="#8c8ca4", g="#bcaec0", G="#7c6c84", B="#2c1832", o="#d0236f", q="#8a1450",
                 y="#2a1a38", Y="#5a4070", r="#d0344e", z="#0e0712", x="#a04058")
    C = {k: (v if isinstance(v, tuple) else hx(v)) for k, v in C.items()}
    d.ellipse([2, 21, 13, 23], fill=(40, 10, 40, 70))

    # long wavy side hair behind the body
    for y in range(8, 17):
        w = 1 if (y // 3) % 2 else 0
        seg(y, 1 + w, "hh"); seg(y, 13 - w, "hh")
    seg(17, 1, "mh"); seg(17, 13, "hm")
    P(1, 12, C["p"]); P(14, 13, C["p"])

    # skirt: a purple underlayer with a torn pink hem, black tiers over it, little X marks (flowers when clean)
    for y, (a, b) in {18: (2, 13), 19: (1, 14), 20: (1, 14)}.items():
        for x in range(a, b + 1):
            P(x, y, C["u"])
    for x in range(1, 15):
        P(x, 20, C["U"] if x % 2 else C["u"])
    for x in (3, 7, 11):
        P(x, 20, (0, 0, 0, 0))
    for y, (a, b) in {16: (3, 12), 17: (3, 12), 18: (3, 12), 19: (4, 11)}.items():
        for x in range(a, b + 1):
            P(x, y, C["d"])
    for x in range(4, 12):
        P(x, 19, C["U"] if x % 2 == 0 else C["d"])
    for y in (14, 15):
        for x in range(4, 12):
            P(x, y, C["d"])
    for x in range(4, 12):
        P(x, 16, C["D"])
    for x, y in ((6, 17), (7, 18), (9, 17), (10, 18)):
        P(x, y, C["k"])
    for x, y in ((7, 17), (6, 18), (10, 17), (9, 18)):
        P(x, y, C["v"] if clean else C["K"])

    # legs and platform boots with cross lacing (laugh: the left boot is kicked up)
    for bx in (4, 9):
        P(bx + 1, 21, C["s"]); P(bx + 2, 21, C["S"])
    for bx in (4, 9):
        y0, bxx = (19, 2) if laugh and bx == 4 else (21, bx)
        box(bxx, y0, bxx + 3, y0 + 1, C["B"])
        P(bxx + 1, y0, C["V"]); P(bxx + 2, y0 + 1, C["V"])
        seg(y0 + 2, bxx, "qooq")
    if laugh:
        P(3, 20, C["B"]); P(4, 21, C["B"]); seg(21, 3, "qq")

    # bodice with the pink cross, belt with buckle, chain, crown tags
    box(5, 11, 10, 13, C["d"])
    seg(11, 7, "kk"); seg(12, 6, "kkkk"); seg(13, 7, "kk")
    seg(14, 4, "dzzvvzzd")
    for x in (5, 7, 9):
        P(x, 15, C["V"])
    seg(16, 4, "gg"); seg(17, 4, "gG")
    P(10, 15, C["G"]); P(10, 16, C["g"])

    # ruffled off-shoulder sleeves
    for sx, ru in ((4, 3), (11, 12)):
        P(sx, 11, C["s"]); P(sx, 12, C["d"])
        P(ru, 11, C["d"]); P(ru, 12, C["D"]); P(ru, 13, C["d"])

    # arms
    if mode in ("normal", "clean"):                       # hand on the chest, other arm down
        P(4, 13, C["s"]); P(5, 13, C["s"]); P(6, 13, C["S"])
        P(12, 14, C["s"]); P(12, 15, C["s"]); P(12, 16, C["S"])
    elif rage:                                            # attacking: right fist thrown out in a punch, left fist cocked back
        P(12, 13, C["s"]); P(13, 13, C["s"])               # punching arm, straight out
        box(14, 12, 15, 13, C["s"]); P(14, 13, C["S"]); P(15, 13, C["S"])   # fist
        for x, y in ((15, 10), (14, 10), (15, 15), (14, 15)):  # impact streaks
            P(x, y, hx("#ff3a2a"))
        P(15, 11, hx("#ffd23a")); P(15, 14, hx("#ffd23a"))
        P(3, 12, C["s"]); P(3, 11, C["s"])                  # cocked arm
        box(2, 9, 3, 10, C["s"]); P(2, 10, C["S"]); P(3, 10, C["S"])        # raised fist
    elif laugh:                                           # both fists up
        box(4, 12, 5, 13, C["s"]); box(10, 12, 11, 13, C["s"]); P(4, 13, C["S"]); P(11, 13, C["S"])
    elif sad:                                             # hands clasped low
        P(4, 13, C["s"]); P(5, 14, C["s"]); P(11, 13, C["s"]); P(10, 14, C["s"])
        seg(15, 6, "sSSs")

    # face, like the reference sprite: pale peach, single-dot eyes set wide apart, a darker cheek edge, a tan jaw band
    box(4, 5, 11, 9, C["s"])
    for y in (7, 8):
        P(4, y, C["S"]); P(11, y, C["S"])                    # cheek edges
    P(4, 9, C["h"]); P(11, 9, C["h"])                        # rounded chin
    seg(10, 4, "hhSvvShh")                                   # small neck + choker, much narrower than the face
    P(7, 6, C["W"]); P(8, 6, C["W"])
    if laugh:
        for x, y in ((4, 7), (5, 6), (6, 7), (9, 7), (10, 6), (11, 7)):               # ^ ^ eyes
            P(x, y, C["E"])
        P(4, 8, C["b"]); P(11, 8, C["b"])
    elif rage:
        P(6, 7, C["E"]); P(6, 8, C["T"]); P(9, 7, C["E"]); P(9, 8, C["T"])   # | |
        P(6, 6, C["E"]); P(9, 6, C["E"])                                              # brows slant down to the middle
    elif sad:
        P(6, 7, C["E"]); P(6, 8, C["T"]); P(9, 7, C["E"]); P(9, 8, C["T"])
        P(5, 8, hx("#bfe6ff")); P(5, 9, hx("#bfe6ff"))
    else:
        P(6, 7, C["E"]); P(6, 8, C["T"]); P(9, 7, C["E"]); P(9, 8, C["T"])   # | |

    # front hair: fringe, side locks, ahoge (it droops when she is sad)
    for y in (3, 4):
        for x in range(2, 14):
            P(x, y, C["h"])
    seg(5, 3, "hhhh"); seg(5, 7, "hh"); seg(5, 10, "hhh")
    seg(6, 3, "hh"); seg(6, 7, "h"); seg(6, 11, "hh")
    for y in range(5, 10):
        P(3, y, C["h"]); P(12, y, C["h"])
    for y in (5, 6, 9):                                     # temple and jaw locks frame the face
        P(4, y, C["h"]); P(11, y, C["h"])
    P(9, 6, C["m"]); P(10, 6, C["m"])                       # a lighter strand swept over the right forehead
    for y in range(7, 14):
        P(2, y, C["h"]); P(13, y, C["h"])
    seg(10, 2, "hh"); seg(10, 12, "hh"); seg(11, 2, "hh"); seg(11, 12, "hh")
    P(2, 12, C["m"]); P(3, 12, C["h"]); P(13, 12, C["m"]); P(12, 12, C["h"])
    for x, y in ((4, 3), (11, 4)):
        P(x, y, C["p"])
    for x, y in ((8, 4), (5, 4), (3, 7), (12, 8), (7, 6)):
        P(x, y, C["m"])
    if sad:
        seg(1, 4, "h"); seg(1, 11, "h"); seg(1, 7, "hh"); seg(2, 4, "hhhhhhhh")
    else:
        seg(0, 5, "H"); seg(0, 8, "H"); seg(0, 11, "H"); seg(1, 4, "hhhhh"); seg(1, 10, "hhhh")
        P(3, 2, C["H"]); P(13, 2, C["H"]); P(14, 3, C["H"]); P(5, 1, C["p"]); P(12, 1, C["m"])

    # tiara; black bow with can charm, hanging pin and crown tag
    seg(1, 6, "vkkv"); seg(2, 5, "VvkkvV")
    seg(1, 0, "yYyy"); seg(2, 0, "yyYy"); seg(3, 1, "yyy")
    seg(0, 2, "vr"); P(2, 4, C["V"]); P(2, 5, C["v"]); seg(6, 1, "gg"); seg(7, 1, "gG")

    if rage:                                              # anger vein
        for x, y in ((11, 3), (13, 3), (12, 4), (11, 5), (13, 5)):
            P(x, y, hx("#ff2a2a"))
    if laugh:                                             # "!" burst lines
        P(13, 0, C["k"]); P(14, 1, C["k"]); P(15, 2, C["k"]); P(14, 0, C["K"]); P(15, 1, C["K"])
    if clean:                                             # sparkles
        for x, y in ((0, 6), (15, 5), (0, 14), (15, 12), (3, 0)):
            P(x, y, hx("#fff6a0"))
    if rage:                                              # cocked fist drawn last so the hair does not cover it
        P(3, 11, C["s"]); P(3, 12, C["s"])
        box(2, 9, 3, 10, C["s"]); P(2, 10, C["S"]); P(3, 10, C["S"])
        for x, y in ((1, 8), (1, 11)):
            P(x, y, hx("#ff3a2a"))
    return im


def muck_mound(mode="normal"):
    """Boss 1: a heap of unsorted trash that came to life. A squat mound studded with garbage, two googly eyes, a small
    mouth, a plastic-bag flap on each side and a melted, dripping base. mode: "normal" or "rage" (red eyes, big jagged
    mouth, flaps thrown up, a redder body)."""
    rage = mode == "rage"
    im, d = canvas(40, 32)
    body = hx("#6b6a46") if not rage else hx("#7a4a3a")
    slime = hx("#8ab04a") if not rage else hx("#b0d040")
    wing, wing_shade = hx("#d8d8e0"), hx("#a8a8b8")
    up = -5 if rage else 0
    d.ellipse([1, 27, 38, 31], fill=slime, outline=INK)                          # the melted puddle it sits in
    for x in (7, 13, 27, 33):                                                    # drips running down
        d.line([(x, 29), (x, 31)], fill=slime)
    for sgn, base in ((-1, 9), (1, 31)):                                         # the two plastic-bag flaps
        tip = base + sgn * 7
        d.polygon([(base, 18), (tip, 12 + up), (tip + sgn, 19 + up), (base + sgn * 6, 24), (base, 24)], fill=wing, outline=INK)
        d.line([(base + sgn * 2, 20), (tip, 15 + up)], fill=wing_shade)
    d.polygon([(6, 28), (5, 22), (9, 13), (15, 7), (20, 5), (26, 7), (31, 13), (35, 22), (34, 28)], fill=body, outline=INK)
    d.line([(10, 14), (15, 9), (20, 7)], fill=shade(body, 1.3))
    for x, y, kind in ((8, 17, 0), (28, 8, 1), (29, 19, 2), (7, 23, 3), (28, 23, 4), (14, 6, 3), (25, 25, 0)):
        _junk(d, x, y, kind)                                                     # garbage stuck all over it
    # googly eyes that look in different directions
    d.ellipse([11, 11, 19, 19], fill=hx("#ffffff") if not rage else hx("#ffe0e0"), outline=INK)
    d.ellipse([21, 11, 29, 19], fill=hx("#ffffff") if not rage else hx("#ffe0e0"), outline=INK)
    eye = hx("#e03030") if rage else INK
    if rage:
        d.ellipse([14, 13, 17, 17], fill=eye); d.ellipse([23, 13, 26, 17], fill=eye)
        d.line([(10, 9), (18, 13)], fill=INK); d.line([(30, 9), (22, 13)], fill=INK)
        d.polygon([(14, 21), (26, 21), (25, 27), (15, 27)], fill=hx("#2a1a2a"), outline=INK)
        for x in (15, 18, 21, 24):
            d.polygon([(x, 21), (x + 1, 24), (x + 2, 21)], fill=hx("#ffffff"))
    else:
        d.ellipse([12, 14, 15, 18], fill=eye); d.ellipse([25, 12, 28, 16], fill=eye)
        d.ellipse([17, 21, 23, 25], fill=hx("#2a1a2a"), outline=INK)             # a small mouth with one tooth
        d.point((19, 21), fill=hx("#ffffff")); d.point((19, 22), fill=hx("#ffffff"))
    return im


def experiment(mode="normal"):
    """Boss 2, the Queen's experiment: a hunched, horned nightmare with a wide jagged-toothed grin, slanted glowing eyes and
    long bone claws, whose wings are made of trash: fanned-out sheets of crumpled newspaper, paper and cardboard with torn
    edges (48x44 art, drawn at 3x: the body is 32 wide and the wings stick out either side).
    mode: "normal" (burning orange eyes), "rage" (blazing red eyes, jaws wide open, wings flared) or "stun" (X eyes, tongue out, stars)."""
    import math
    im, d = canvas(32, 44)                                          # the body
    wing_im, dw = canvas(48, 44)                                    # wings behind it, with room to spread
    skin, skin_dark, skin_hi = hx("#5a4a6e"), hx("#3a2f4c"), hx("#7a6892")
    bone = hx("#efe6cf")
    mouth_in, tongue, tooth = hx("#16091a"), hx("#c0304a"), hx("#f6f0dc")
    rage, stun = mode == "rage", mode == "stun"
    glow = hx("#ff2a1a") if rage else hx("#ffa21c")
    core = hx("#fff0a0") if rage else hx("#fff6c0")
    d.ellipse([5, 41, 27, 44], fill=(20, 10, 30, 90))

    # wings of trash: fanned sheets of newspaper, paper and cardboard
    news, paper, card, ink_txt = hx("#e2dcc8"), hx("#f2f0e8"), hx("#a67c52"), hx("#857f6e")

    def sheet(ax, ay, sgn, ang, length, width, color, kind):
        a = math.radians(ang)
        dx, dy = sgn * math.cos(a), -math.sin(a)
        px, py = -dy, dx
        def pt(f, o): return (ax + dx * length * f + px * width * o, ay + dy * length * f + py * width * o)
        pts = [pt(0, -.25), pt(0, .25), pt(.55, .5), pt(.9, .5), pt(1.0, .3), pt(.93, .12), pt(1.05, -.05),
               pt(.95, -.25), pt(1.0, -.5), pt(.6, -.5)]
        dw.polygon(pts, fill=color, outline=INK)
        for f in (.25, .45, .65):                                   # printed lines on newspaper / paper
            if kind != "card":
                dw.line([pt(f, -.3), pt(f + .12, -.3)], fill=ink_txt)
                dw.line([pt(f, .05), pt(f + .12, .05)], fill=ink_txt)
        if kind == "card":
            dw.line([pt(.2, -.4), pt(.9, .4)], fill=hx("#e8d9a0"))   # a strip of tape
        dw.line([pt(.1, 0), pt(.8, .1)], fill=shade(color, .78))      # crumple fold

    spread = 9 if rage else 0
    fan = ((8, 14, paper, "paper"), (30, 17, card, "card"), (55 + spread, 20, news, "news"), (80 + spread // 2, 22, paper, "paper"))
    for sgn, ax in ((-1, 17), (1, 31)):                              # shoulders, in wide-canvas coordinates
        for ang, length, color, kind in fan:
            sheet(ax, 27, sgn, ang, length, 7, color, kind)


    # thick legs, clawed feet
    for x in (11, 18):
        rect(d, x, 33, x + 3, 40, skin_dark, INK)
        d.polygon([(x - 2, 42), (x, 40), (x + 4, 40), (x + 6, 42), (x + 4, 43), (x - 1, 43)], fill=skin_dark, outline=INK)
        for cx in (x - 2, x + 1, x + 4):
            d.polygon([(cx, 43), (cx + 1, 43), (cx, 44)], fill=INK)
    # broad hunched torso with ribs
    d.polygon([(7, 25), (25, 25), (22, 36), (10, 36)], fill=skin_dark, outline=INK)
    for y in (29, 32):
        d.line([(12, y), (19, y)], fill=skin)
    d.line([(15, 27), (15, 35)], fill=skin)
    # long arms hanging down to wicked claws
    for sgn, x0 in ((-1, 8), (1, 24)):
        d.polygon([(x0 - sgn * 2, 25), (x0 + sgn * 2, 27), (x0 + sgn * 3, 38), (x0 - sgn * 1, 38)], fill=skin, outline=INK)
        for k in range(3):
            bx = x0 - 3 + k * 3 + (1 if sgn > 0 else 0)
            d.polygon([(bx, 38), (bx + 1, 38), (bx + (k - 1), 41)], fill=bone, outline=INK)
    # the head, wide and low-browed, with two curved horns
    for sgn, x0 in ((-1, 9), (1, 22)):
        d.polygon([(x0, 13), (x0 + sgn * 2, 6), (x0 + sgn * 4, 2), (x0 + sgn * 5, 0), (x0 + sgn * 6, 4), (x0 + sgn * 6, 9), (x0 + sgn * 4, 13)],
                  fill=bone, outline=INK)
    d.ellipse([5, 7, 26, 28], fill=skin, outline=INK)
    d.arc([7, 9, 18, 18], 190, 260, fill=skin_hi)
    d.line([(20, 8), (17, 13)], fill=skin_dark); d.line([(17, 13), (19, 15)], fill=skin_dark)      # a scar
    # slanted glowing eyes under angry brows
    for sgn, ex in ((-1, 11), (1, 20)):
        if stun:
            d.ellipse([ex - 3, 13, ex + 3, 19], fill=hx("#d8d0e8"), outline=INK)
            d.line([(ex - 2, 14), (ex + 2, 18)], fill=INK); d.line([(ex + 2, 14), (ex - 2, 18)], fill=INK)
            continue
        out, inn = ex + sgn * 4, ex - sgn * 3
        d.polygon([(out, 14), (inn, 16 + (1 if rage else 0)), (inn, 19), (out + sgn * -1, 18)], fill=glow, outline=INK)
        d.rectangle([ex - 1, 16, ex, 18], fill=core)
        d.line([(ex, 15), (ex, 19)], fill=INK)                                                  # slit pupil
        d.line([(out + sgn, 11 - (1 if rage else 0)), (inn, 15)], fill=INK)                    # brow
        d.line([(out + sgn, 12 - (1 if rage else 0)), (inn, 16)], fill=INK)
    d.point((15, 20), fill=INK); d.point((16, 20), fill=INK)
    # the grin: a wide mouth full of jagged teeth
    top, bot = (20, 28) if rage else (22, 27)
    d.polygon([(8, top + 1), (12, top - 1), (19, top - 1), (23, top + 1), (22, bot), (16, bot + 1), (9, bot)], fill=mouth_in, outline=INK)
    if rage or stun:
        d.polygon([(12, bot), (19, bot), (17, bot - 3), (14, bot - 3)], fill=tongue)
    for x in (10, 13, 16, 19, 22):
        d.polygon([(x - 1, top), (x, top), (x - 1, top + (4 if rage else 3))], fill=tooth)
    for x in (11, 14, 17, 20):
        d.polygon([(x - 1, bot), (x, bot), (x - 1, bot - (3 if rage else 2))], fill=tooth)
    # green drool
    for x in (9, 22):
        d.line([(x, bot), (x, bot + (4 if rage else 2))], fill=hx("#9ac060"))
    if stun:
        for x, y in ((12, 2), (19, 0), (16, 4)):
            for ox, oy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
                d.point((x + ox, y + oy), fill=hx("#fff06a"))
    wing_im.alpha_composite(im, (8, 0))
    return wing_im


def stink_ghost():
    """A small toothy ghost that haunts Boss 2's street (16x20 art, drawn at 3x)."""
    im, d = canvas(16, 20)
    body = hx("#e4def0")
    d.ellipse([5, 17, 12, 19], fill=(40, 30, 60, 60))
    d.polygon([(2, 17), (2, 9), (4, 4), (8, 2), (12, 4), (14, 9), (14, 17), (11, 15), (8, 17), (5, 15)], fill=body, outline=INK)
    for x in (5, 11):
        d.ellipse([x - 2, 6, x + 2, 10], fill=hx("#ffffff"), outline=INK); d.point((x, 8), fill=INK)
    rect(d, 4, 12, 12, 15, hx("#3a1a3a"), INK)
    for x in (5, 7, 9, 11):
        d.polygon([(x - 1, 12), (x + 1, 12), (x, 14)], fill=hx("#ffffff"))
    for x in (4, 10):
        rect(d, x, 17, x + 2, 19, body, INK)
    return im


def junk_cube(variant=0):
    """A cube of crushed junk, like a scrapyard bale: pressed garbage with bits of bottles, cans, cardboard and bags
    showing, a lighter top face and metal bands (16x16 art, drawn at 3x). Three colour variants."""
    im, d = canvas(16, 16)
    bodies = ("#7a7c5a", "#6f7f86", "#86705c")
    body = hx(bodies[variant % 3])
    rnd = random.Random(20 + variant)
    rect(d, 0, 0, 15, 15, body, INK)
    d.rectangle([1, 1, 14, 4], fill=shade(body, 1.3))                              # the top face
    for _ in range(22):                                                            # pressed-in specks
        d.point((rnd.randrange(1, 15), rnd.randrange(5, 15)), fill=shade(body, rnd.choice((0.7, 0.85, 1.2))))
    junk = (("#a9d2f7", 3, 1), ("#c9cdd4", 1, 3), ("#d9b887", 3, 2), ("#f2d64a", 3, 1), ("#f4a8c0", 2, 2), ("#86dba6", 1, 2))
    for i in range(9):
        c, w, h = junk[(variant * 2 + i) % len(junk)]
        x, y = rnd.randrange(1, 14 - w), rnd.randrange(5, 14 - h)
        d.rectangle([x, y, x + w - 1, y + h - 1], fill=hx(c))
    band = hx("#4a4f58")
    d.line([(5, 5), (5, 14)], fill=band); d.line([(10, 5), (10, 14)], fill=band)   # metal straps
    d.line([(1, 4), (14, 4)], fill=INK)
    for x in (3, 7, 11):                                                            # junk poking out of the top face
        d.point((x + variant % 2, 2), fill=hx(junk[(x + variant) % len(junk)][0]))
    for x in (5, 10):
        d.point((x, 9), fill=hx("#c9cdd4"))                                        # rivets
    return im


def imp_sprite():
    im, d = canvas(16, 16)
    bagc = hx("#4a4a58")
    d.ellipse([3, 12, 12, 15], fill=(40, 30, 60, 70))
    d.polygon([(4, 4), (7, 1), (9, 1), (12, 4), (13, 12), (3, 12)], fill=bagc, outline=INK)
    d.polygon([(7, 1), (6, 0), (8, 2)], fill=bagc, outline=INK); d.polygon([(9, 1), (10, 0), (8, 2)], fill=bagc, outline=INK)
    for ex in (5, 9):
        rect(d, ex, 6, ex + 2, 8, hx("#ffe060"), INK); d.point((ex + 1, 7), fill=hx("#d02020"))
    d.line([(5, 10), (11, 10)], fill=INK)
    for tx in (6, 8, 10): d.point((tx, 10), fill=hx("#ffffff"))
    d.point((2, 8), fill=INK); d.point((13, 8), fill=INK)
    return im


def weapon_sprites():
    im, d = canvas(16, 16)  # the Spirit Bottle
    glass = hx("#86dba6"); liquid = hx("#d8f6ff")
    rect(d, 6, 0, 9, 2, hx("#c29a5a"), INK)
    rect(d, 6, 3, 9, 6, shade(glass, 1.1), INK)
    d.polygon([(6, 6), (9, 6), (12, 8), (12, 14), (11, 15), (4, 15), (3, 14), (3, 8)], fill=glass, outline=INK)
    rect(d, 5, 8, 10, 14, liquid, None)
    rect(d, 4, 9, 11, 12, hx("#ffffff"), shade(glass, 0.5))
    d.point((7, 10), fill=hx("#d9566f")); d.point((8, 10), fill=hx("#d9566f")); d.point((7, 11), fill=hx("#d9566f")); d.point((8, 11), fill=hx("#d9566f"))
    d.point((6, 10), fill=hx("#d9566f")); d.point((9, 10), fill=hx("#d9566f"))
    d.point((4, 7), fill=hx("#ffffff")); d.point((4, 8), fill=hx("#ffffff"))
    save("weapon/bottle.png", im)

    im, d = canvas(8, 8)  # droplet
    d.ellipse([1, 1, 6, 6], fill=hx("#e8fbff"), outline=hx("#3aa5ae"))
    d.point((3, 2), fill=hx("#ffffff")); d.point((2, 3), fill=hx("#ffffff"))
    save("weapon/drop.png", im)

    for name, fill, out in (("heart", "#e0506a", "#7a2438"), ("heart_empty", "#e6e0f2", "#9a8fd6")):
        im, d = canvas(9, 8)
        pix = ["012340210", "123334320"]
        rows = [".11...11.", "1221.1221", "122211221", "122222221", ".1222221.", "..12221..", "...121...", "....1...."]
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == "1": d.point((x, y), fill=hx(out))
                elif ch == "2": d.point((x, y), fill=hx(fill))
        if name == "heart": d.point((2, 2), fill=hx("#ffffff"))
        save("weapon/%s.png" % name, im)


# ----------------------------------------------------------------------------- story scenery (full 720x480 backdrops)

def _junk(d, x, y, kind):
    """One piece of garbage (about 6 px): bottle, box, banana peel, can or bag."""
    if kind == 0:
        rect(d, x, y + 1, x + 2, y + 6, hx("#a9d2f7"), INK); rect(d, x, y, x + 2, y + 1, hx("#2f6fd6"), INK)
    elif kind == 1:
        rect(d, x, y, x + 7, y + 5, hx("#d9b887"), INK); d.line([(x + 3, y), (x + 3, y + 5)], fill=hx("#b08a5a"))
    elif kind == 2:
        d.polygon([(x, y + 4), (x + 3, y), (x + 7, y + 4), (x + 3, y + 3)], fill=hx("#f2d64a"), outline=INK)
    elif kind == 3:
        rect(d, x, y, x + 3, y + 5, hx("#c9cdd4"), INK); d.point((x + 1, y + 2), fill=hx("#d9566f"))
    else:
        d.ellipse([x, y, x + 6, y + 6], fill=hx("#f4f4fa"), outline=INK)


def _heap(d, cx, base, half_w, height, seed):
    """A heap of garbage that can be piled right up to the ceiling."""
    rnd = random.Random(seed)
    d.polygon([(cx - half_w, base), (cx - half_w * 0.55, base - height * 0.55), (cx - half_w * 0.2, base - height),
               (cx + half_w * 0.25, base - height * 0.9), (cx + half_w * 0.6, base - height * 0.5), (cx + half_w, base)],
              fill=hx("#6b5d43"), outline=INK)
    for _ in range(int(half_w * height / 14)):
        y = base - rnd.random() * height * 0.92
        span = half_w * (1 - (base - y) / height * 0.75)
        x = cx + (rnd.random() * 2 - 1) * span * 0.85
        _junk(d, int(x) - 3, int(y) - 3, rnd.randrange(5))


def room_scene(hole=False):
    """The messy sibling's bedroom, buried in garbage (or, with hole=True, after Queen Muck smashed out through the wall)."""
    W, H = 240, 160
    im = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(im)
    rnd = random.Random(5)

    def wash(box, rgba, kind="ellipse"):                      # translucent shapes must be blended, not drawn over the pixels
        layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        getattr(ImageDraw.Draw(layer), kind)(box, fill=rgba)
        im.alpha_composite(layer)

    # wallpaper with grime, floor boards
    d.rectangle([0, 0, W, 86], fill=hx("#d8cfe8"))
    for x in range(0, W, 12):
        d.rectangle([x, 0, x + 5, 86], fill=hx("#d0c6e2"))
    for _ in range(26):
        x, y = rnd.randrange(W), rnd.randrange(86)
        wash([x, y, x + rnd.randrange(4, 12), y + rnd.randrange(3, 9)], (150, 160, 110, 70))
    d.rectangle([0, 0, W, 5], fill=hx("#b4a8cc"))
    d.rectangle([0, 86, W, H], fill=hx("#c9a97a"))
    for y in range(88, H, 9):
        d.line([(0, y), (W, y)], fill=hx("#a98a5e"))
        for x in range((y // 9 % 3) * 7, W, 22):
            d.line([(x, y), (x, min(H, y + 9))], fill=hx("#a98a5e"))
    noise(d, W, H, hx("#c9a97a"), 8, 140, y0=88)
    for _ in range(14):
        x, y = rnd.randrange(W), rnd.randrange(92, H)
        wash([x, y, x + rnd.randrange(5, 12), y + rnd.randrange(2, 5)], (110, 120, 70, 80))
    d.rectangle([0, 84, W, 86], fill=hx("#8a7a6a")); d.line([(0, 84), (W, 84)], fill=INK)

    # the doorway on the left (the tidy sibling pushed the door open)
    rect(d, 10, 18, 40, 86, hx("#fdf6e0"), INK)
    d.polygon([(40, 18), (50, 24), (50, 84), (40, 86)], fill=hx("#a0764a"), outline=INK)
    d.point((47, 56), fill=hx("#f7d88a")); d.point((47, 57), fill=hx("#f7d88a"))
    # a window, with grimy curtains
    rect(d, 100, 12, 140, 52, hx("#ffffff"), INK)
    for y in range(14, 50):
        t = (y - 14) / 36
        d.line([(102, y), (138, y)], fill=(int(190 + 60 * t), int(215 + 20 * t), int(250 - 5 * t), 255))
    wash([102, 14, 138, 50], (150, 170, 110, 60), "rectangle")
    d.line([(120, 12), (120, 52)], fill=INK); d.line([(100, 32), (140, 32)], fill=INK)
    d.polygon([(96, 10), (110, 10), (104, 52), (96, 52)], fill=hx("#9ec27a"), outline=INK)
    d.polygon([(144, 10), (130, 10), (136, 52), (144, 52)], fill=hx("#9ec27a"), outline=INK)
    rect(d, 96, 52, 144, 55, hx("#fdf6e0"), INK)
    # torn posters
    rect(d, 62, 22, 78, 44, hx("#f4a8c0"), INK); d.line([(62, 30), (78, 38)], fill=INK)
    rect(d, 160, 18, 176, 36, hx("#9ad1f2"), INK)

    if hole:
        # a smashed hole in the right wall, with the polluted outskirts and Queen Muck's palace beyond
        view = palace_scene().convert("RGBA")
        mask = Image.new("L", (W, H), 0)
        ImageDraw.Draw(mask).polygon([(150, 20), (162, 8), (176, 14), (192, 6), (210, 12), (226, 8), (232, 24), (226, 44), (234, 64),
                                      (226, 86), (150, 86), (156, 66), (148, 46)], fill=255)
        im.paste(view.crop((66, 4, 174, 104)).resize((82, 80), Image.NEAREST), (150, 6), mask.crop((150, 6, 232, 86)))
        d = ImageDraw.Draw(im)
        d.line([(150, 20), (162, 8), (176, 14), (192, 6), (210, 12), (226, 8), (232, 24), (226, 44), (234, 64), (226, 86)], fill=INK)
        d.line([(150, 20), (148, 46), (156, 66), (150, 86)], fill=INK)
        for x, y, w in ((146, 80, 8), (152, 84, 6), (230, 82, 8), (220, 88, 7), (170, 92, 6), (200, 94, 8), (160, 100, 5)):
            rect(d, x, y, x + w, y + 4, hx("#c9a38a"), INK)                      # fallen bricks and rubble
        for i, (x, y) in enumerate(((176, 96), (190, 102), (206, 98), (222, 106), (168, 110))):
            _junk(d, x, y, i % 5)
    else:
        _heap(d, 188, 100, 46, 82, 3)                                            # the heap she will rise from
    _heap(d, 70, 94, 30, 56, 9)                                                  # a second pile in the back corner
    _heap(d, 22, 112, 18, 26, 4)
    # a bed somewhere under it all
    rect(d, 112, 78, 150, 90, hx("#e8a0b4"), INK); rect(d, 112, 72, 120, 80, hx("#fdf6e0"), INK)
    wash([0, 0, W, H], (120, 150, 70, 26), "rectangle")                          # sour, greenish light
    return pastel(im, 0.08)


def palace_scene():
    """Queen Muck's palace in the polluted outskirts: trash, but elegant, in greys with pink accents. Cardboard-box brickwork,
    bottle-glass windows, a CD rosette, pink bag-plastic roofs with can spires, a bottle battlement,
    a pink carpet and a fence."""
    W, H = 240, 160
    im = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(im)
    rnd = random.Random(11)
    ivory, tan, gold = hx("#e4e4ec"), hx("#c4c4d2"), hx("#f2f2fa")                  # greys with silver trim
    pink, lilac, hot = hx("#f08fbe"), hx("#f8c0d8"), hx("#e0507f")                    # the pink accents
    silver = hx("#c9cdd4")
    glass = ("#8a94b0", "#a6aec8", "#c8cee0", "#7a84a0")

    vgrad(d, W, 104, hx("#8f7fb0"), hx("#f0c0cc"))
    d.ellipse([150, 42, 178, 70], fill=hx("#f4a8a0"))                            # a dim sun behind the smog
    for cx, cy, r in ((20, 20, 30), (70, 34, 26), (200, 24, 34), (150, 10, 28), (230, 56, 22), (10, 58, 18)):
        d.ellipse([cx - r, cy - r // 2, cx + r, cy + r // 2], fill=(110, 96, 128, 200))
        d.ellipse([cx - r // 2, cy - r // 2 - 6, cx + r, cy + r // 2 - 4], fill=(150, 134, 160, 170))
    for i, x in enumerate((12, 52, 188, 226)):                                    # far hills of rubbish
        mound(d, x, 84, i % 3)
    d.rectangle([0, 100, W, H], fill=hx("#7e8a62"))
    noise(d, W, H, hx("#7e8a62"), 4, 260, y0=100)
    for _ in range(9):
        x, y = rnd.randrange(W), rnd.randrange(108, H - 6)
        d.ellipse([x, y, x + rnd.randrange(8, 18), y + rnd.randrange(3, 6)], fill=(110, 150, 60, 150))
    d.polygon([(104, 100), (136, 100), (190, H), (50, H)], fill=hx("#a89a78"))   # a path of trampled rubbish
    d.polygon([(114, 102), (126, 102), (152, H), (88, H)], fill=hot)               # the pink carpet up to the gate
    d.line([(114, 102), (88, H)], fill=gold); d.line([(126, 102), (152, H)], fill=gold)
    for i in range(14):
        _junk(d, 60 + rnd.randrange(30), 108 + rnd.randrange(46), i % 5)
        _junk(d, 150 + rnd.randrange(30), 108 + rnd.randrange(46), (i + 2) % 5)

    def bag_roof(cx, base, half, top, c1, c2):                                   # scalloped roof of overlapping plastic bags
        d.polygon([(cx - half, base), (cx, top), (cx + half, base)], fill=c1, outline=INK)
        for i, y in enumerate(range(top + 3, base, 3)):
            w = half * (y - top) / (base - top)
            for x in range(int(cx - w), int(cx + w) - 2, 4):
                d.ellipse([x, y - 1, x + 5, y + 3], fill=c1 if i % 2 else c2, outline=INK)
        rect(d, cx - 1, top - 6, cx + 1, top, silver, INK)                       # a soda can for a spire
        d.point((cx, top - 7), fill=gold)

    def bricks(x0, y0, x1, y1, seed):                                             # cardboard-box brickwork
        rect(d, x0, y0, x1, y1, ivory, INK)
        r = random.Random(seed)
        for row, y in enumerate(range(y0 + 1, y1, 4)):
            for x in range(x0 + 1 + (row % 2) * 4, x1, 8):
                d.rectangle([x, y, min(x + 6, x1 - 1), min(y + 2, y1 - 1)], fill=r.choice((tan, hx("#d0d0dc"), hx("#dcdce6"), hx("#e0c8d4"))))

    def arch_window(cx, top, bottom):                                              # bottle-glass window with a gold frame
        d.pieslice([cx - 4, top, cx + 4, top + 8], 180, 360, fill=hx(glass[0]), outline=INK)
        rect(d, cx - 4, top + 4, cx + 4, bottom, hx(glass[0]), INK)
        for i, y in enumerate(range(top + 5, bottom, 3)):
            d.line([(cx - 3, y), (cx + 3, y)], fill=hx(glass[(i + 1) % 4]))
        d.line([(cx, top + 1), (cx, bottom)], fill=gold)

    # the long hall
    bricks(76, 64, 164, 102, 3)
    for x in range(76, 166, 5):                                                    # bottle battlement
        rect(d, x, 58, x + 2, 64, hx(("#a8b0c8", "#c8cede", "#8f98b0")[(x // 5) % 3]), INK); d.point((x + 1, 57), fill=hx("#4a5068"))
    arch_window(90, 72, 92); arch_window(146, 72, 92)
    # inner turrets and the keep
    for x0 in (90, 138):
        bricks(x0, 46, x0 + 12, 64, x0 + 1)
        d.line([(x0 + 6, 50), (x0 + 6, 58)], fill=hx(glass[1]))
        bag_roof(x0 + 6, 46, 9, 32, pink, lilac)
    bricks(106, 40, 134, 64, 7)
    bricks(112, 26, 128, 40, 8)
    bag_roof(120, 26, 12, 8, pink, lilac)
    d.line([(120, 2), (120, 1)], fill=INK); d.polygon([(121, 1), (129, 3), (121, 5)], fill=hot, outline=INK)
    d.ellipse([113, 44, 127, 58], fill=hx("#d8d8e4"), outline=gold)                # a CD rosette
    for i, c in enumerate(("#f4a6c8", "#c9cdd4", "#8f98b0", "#e4e4ec", "#f08fbe", "#a8b0c8")):
        d.pieslice([114, 45, 126, 57], i * 60, i * 60 + 60, fill=hx(c))
    d.ellipse([118, 49, 122, 53], fill=hx("#f4f4fa"), outline=INK); d.point((120, 51), fill=INK)
    arch_window(120, 28, 38)
    # the gate, with lanterns and a heart
    d.pieslice([109, 80, 131, 104], 180, 360, fill=hx("#3a3a48"), outline=gold)
    rect(d, 109, 92, 131, 102, hx("#3a3a48"), gold)
    d.line([(120, 83), (120, 102)], fill=gold)
    for x in (105, 135):
        rect(d, x, 86, x + 2, 90, hx("#ffd0e4"), INK)
    heart = [(120, 74), (117, 71), (115, 71), (114, 73), (114, 75), (120, 80), (126, 75), (126, 73), (125, 71), (123, 71)]
    layer = Image.new("RGBA", (16, 14), (0, 0, 0, 0))
    ImageDraw.Draw(layer).polygon([(x - 112, y - 70) for x, y in heart], fill=hx("#ff8fb0"), outline=gold)
    crack = [0, 0, -1, 0, 1, 0, -1, 0, 1, 0, 0, 0, 0, 0]               # the zigzag the heart has split along
    left, right = Image.new("RGBA", (16, 14), (0, 0, 0, 0)), Image.new("RGBA", (16, 14), (0, 0, 0, 0))
    for yy in range(14):
        for xx in range(16):
            px = layer.getpixel((xx, yy))
            if px[3]:
                (left if xx < 8 + crack[yy] else right).putpixel((xx, yy), px)
    im.alpha_composite(left, (112, 70))
    im.alpha_composite(right, (113, 71))                               # the right half slips down and sideways
    for yy in range(14):
        if layer.getpixel((8 + crack[yy], yy))[3]:
            d.point((112 + 8 + crack[yy], 70 + yy), fill=INK)          # the dark crack between the halves
    # steps made of boxes and a picket fence
    d.polygon([(100, 106), (140, 106), (136, 102), (104, 102)], fill=tan, outline=INK)
    for x0, x1 in ((4, 64), (176, 236)):
        d.line([(x0, 108), (x1, 108)], fill=INK); d.line([(x0, 112), (x1, 112)], fill=INK)
        for x in range(x0, x1, 5):
            d.line([(x, 104), (x, 114)], fill=INK); d.point((x, 103), fill=gold)
    for bx, by in ((68, 104), (76, 106), (162, 105), (170, 103), (62, 108), (176, 108)):   # knotted garbage bags at the walls
        d.ellipse([bx, by - 4, bx + 8, by + 3], fill=hx("#3a3a4a"), outline=INK)
        d.line([(bx + 3, by - 4), (bx + 4, by - 6)], fill=INK); d.point((bx + 2, by - 2), fill=hx("#7a7a90"))
    for x, y in ((24, 118), (216, 120)):                                         # dead trees
        d.line([(x, y + 22), (x, y)], fill=INK, width=2)
        d.line([(x, y + 8), (x - 8, y - 2)], fill=INK); d.line([(x, y + 12), (x + 8, y + 2)], fill=INK)
    _heap(d, 30, 152, 26, 20, 6)
    _heap(d, 214, 154, 28, 24, 8)
    im.putalpha(255)
    return pastel(im, 0.1)


def meadow_scene():
    """The outskirts, bloomed into meadows overnight: sunny hills, a rainbow, flowers and the village beyond."""
    W, H = 240, 160
    im = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(im)
    rnd = random.Random(2)
    vgrad(d, W, 96, hx("#bfe3ff"), hx("#fdeef6"))
    d.ellipse([186, 8, 204, 26], fill=hx("#fff2c8"))
    for i, c in enumerate(("#ffb8c8", "#ffe0a8", "#fff6b0", "#c8f0c0", "#b8dcff", "#d8c8f8")):
        d.arc([30 - i, 24 - i, 210 + i, 190 + i], 192, 348, fill=hx(c), width=2)
    for cx, cy in ((24, 18), (110, 12), (160, 34)):
        d.ellipse([cx, cy, cx + 20, cy + 8], fill=(255, 255, 255, 235)); d.ellipse([cx + 8, cy - 3, cx + 28, cy + 7], fill=(255, 255, 255, 235))
    d.rectangle([0, 80, W, H], fill=hx("#cdf0d0"))
    for x in (10, 60, 110, 160, 205):
        house(d, x, 66, "#e8808a" if x % 3 else "#7aa8e0")
    for cx, r in ((20, 40), (95, 50), (180, 56), (235, 36)):
        d.ellipse([cx - r, 92 - r // 3, cx + r, 92 + r], fill=hx("#bfe8c8"), outline=None)
    d.rectangle([0, 104, W, H], fill=hx("#d4f2d8"))
    noise(d, W, H, hx("#d4f2d8"), 3, 220, y0=104)
    for x in range(-2, W + 12, 17):
        bubbles(d, x, 90 + (x * 5) % 5, 8, TREE_PAL.get("park", TEAL))
    for _ in range(150):                                                         # flowers everywhere
        x, y = rnd.randrange(W), rnd.randrange(100, H)
        c = hx(rnd.choice(("#ff8fb8", "#fff6a0", "#ffffff", "#b8a0f0", "#ffb070")))
        d.point((x, y + 1), fill=hx("#5fae6a")); d.point((x, y), fill=c)
        if rnd.random() < 0.5:
            d.point((x + 1, y), fill=c); d.point((x - 1, y), fill=c)
    im.putalpha(255)
    return pastel(im, 0.08)


# ----------------------------------------------------------------------------- Mother Nature and her grove

def mother_nature():
    """Mother Nature, on the same 16x24 chibi grid as the siblings, villagers and Queen Muck (square head, dot-style face,
    blush, 1 px ink outlines). Her hair: very long, wavy platinum / pale ash blonde, reaching behind her knees; swept bangs
    over her left eye (the right one as you look at her); two big side curls framing her face and curling inward in front of
    her shoulders; the side hair covers her ears and the back of her neck. A leaf-green gown and a little flower crown."""
    im, d = canvas(16, 24)
    skin = hx("#f6dcc4")
    hair, hair_sh, hair_hi = hx("#ebe9f3"), hx("#c4c3dc"), hx("#ffffff")
    gown, gown_dk, leaf = hx("#7cc79a"), hx("#4e9d78"), hx("#b4ecc0")
    d.ellipse([2, 21, 13, 23], fill=(60, 40, 90, 55))

    # the long back hair: a wavy panel behind her, from the crown to behind the knees
    for y in range(3, 20):
        l = 1 + (1 if (y // 3) % 2 else 0)
        r = 14 - (1 if ((y + 1) // 3) % 2 else 0)
        d.line([(l, y), (r, y)], fill=hair)
        d.point((l, y), fill=shade(hair_sh, 0.55)); d.point((r, y), fill=shade(hair_sh, 0.55))
        if y % 3 == 0:
            d.point((l + 2, y), fill=hair_sh); d.point((r - 2, y), fill=hair_sh)
        if y % 4 == 1:
            d.point((l + 1, y), fill=hair_hi); d.point((r - 1, y), fill=hair_hi)
    d.line([(2, 20), (13, 20)], fill=INK)
    for x in (3, 6, 9, 12):                                                      # wavy tips
        d.point((x, 19), fill=hair_sh); d.point((x + 1, 20), fill=INK)

    # gown: a long leaf-green skirt with a leaf hem, a bodice with a vine sash
    d.polygon([(4, 14), (11, 14), (13, 21), (2, 21)], fill=gown, outline=INK)
    for i, x in enumerate(range(2, 14, 2)):                                      # leaf-shaped hem
        d.point((x, 21), fill=leaf); d.point((x, 20), fill=gown_dk if i % 2 else leaf)
    d.line([(6, 15), (5, 20)], fill=gown_dk); d.line([(9, 15), (10, 20)], fill=gown_dk)
    rect(d, 5, 11, 10, 14, gown, INK)
    d.line([(5, 13), (10, 13)], fill=gown_dk)
    d.point((7, 12), fill=hx("#ffd0e0")); d.point((8, 12), fill=hx("#fff6a0"))
    for x0 in (3, 11):                                                           # leaf sleeves
        rect(d, x0, 11, x0 + 1, 13, leaf, None); d.line([(x0, 14), (x0 + 1, 14)], fill=INK)
    rect(d, 7, 15, 8, 16, skin, INK)                                             # hands together at the waist

    # head and hair
    rect(d, 3, 2, 12, 10, skin, INK)
    rect(d, 4, 3, 11, 4, hair, None)                                             # hair over the forehead
    rect(d, 4, 3, 4, 9, hair, None); rect(d, 11, 3, 11, 9, hair, None)           # side hair covers the ears
    d.point((4, 6), fill=hair_sh); d.point((11, 6), fill=hair_sh); d.point((5, 3), fill=hair_hi); d.point((7, 3), fill=hair_hi)
    # swept bangs over her left eye (the right one as we look at her)
    for x, y in ((5, 5), (6, 5), (7, 5), (8, 5), (8, 6), (9, 6), (9, 7), (10, 7), (10, 8), (10, 5), (10, 6)):
        d.point((x, y), fill=hair)
    for x, y in ((7, 5), (9, 8), (10, 9), (8, 6)):
        d.point((x, y), fill=hair_sh)
    d.point((6, 4), fill=hair_hi); d.point((9, 5), fill=hair_hi)
    # the face: one visible eye, blush, a soft smile
    d.point((6, 7), fill=INK); d.point((6, 8), fill=hx("#2f7a5a"))
    d.point((5, 9), fill=BLUSH); d.point((10, 9), fill=BLUSH)
    d.point((7, 9), fill=hx("#c46a7a")); d.point((8, 9), fill=hx("#c46a7a"))

    # two big side curls: frame the face and curl inward in front of the shoulders
    for x0 in (2, 11):
        rect(d, x0, 9, x0 + 2, 12, hair, None)
        d.point((x0 + 1, 10), fill=hair_hi); d.point((x0 + 1, 12), fill=hair_sh)
    for x, y in ((5, 12), (5, 13), (4, 13), (3, 13), (10, 12), (10, 13), (11, 13), (12, 13)):   # both curls hook inward
        d.point((x, y), fill=hair)
    for x, y in ((6, 13), (3, 14), (4, 14), (5, 14), (9, 13), (10, 14), (11, 14), (12, 14)):
        d.point((x, y), fill=shade(hair_sh, 0.62))

    # a little flower crown
    for x in range(3, 13):
        d.point((x, 1), fill=hx("#5fae6a") if x % 2 else hx("#8fd49a"))
    for x, c in ((4, "#ff9cc0"), (6, "#ffffff"), (8, "#fff08a"), (10, "#ff9cc0"), (12, "#ffffff")):
        d.point((x, 1), fill=hx(c)); d.point((x, 0), fill=hx(c))
    return im


def grove_guard():
    """A forest guardian: sage skin, a leaf helm, bark armour and a spear tipped with a leaf. Same 16x24 grid."""
    im, d = canvas(16, 24)
    skin, bark, bark_dk = hx("#cfe6c0"), hx("#9a7a52"), hx("#6e5538")
    leaf, leaf_dk = hx("#7cc79a"), hx("#4e9d78")
    d.ellipse([3, 21, 12, 23], fill=(60, 40, 90, 55))
    d.line([(13, 3), (13, 21)], fill=hx("#8a6a4a")); d.line([(12, 3), (12, 21)], fill=INK)   # spear shaft
    d.polygon([(12, 4), (14, 4), (13, 0)], fill=leaf, outline=INK)                              # leaf tip
    rect(d, 5, 19, 6, 21, bark_dk, INK); rect(d, 9, 19, 10, 21, bark_dk, INK)                    # boots
    rect(d, 5, 16, 10, 19, leaf_dk, INK)                                                         # kilt of leaves
    for x in (5, 7, 9): d.point((x, 19), fill=leaf)
    rect(d, 4, 10, 11, 16, bark, INK)                                                            # bark armour
    for y in (12, 14): d.line([(5, y), (10, y)], fill=bark_dk)
    d.point((7, 11), fill=leaf); d.point((8, 11), fill=leaf)
    rect(d, 2, 11, 3, 14, bark, INK); rect(d, 12, 11, 13, 14, skin, INK)                         # arms
    rect(d, 3, 2, 12, 10, skin, INK)                                                             # head
    rect(d, 3, 1, 12, 4, leaf, INK)                                                              # leaf helm with a crest
    d.line([(7, 0), (8, 0)], fill=leaf_dk); d.point((7, 2), fill=leaf_dk); d.point((9, 2), fill=leaf_dk)
    d.point((6, 7), fill=INK); d.point((9, 7), fill=INK)
    d.point((5, 8), fill=BLUSH); d.point((10, 8), fill=BLUSH)
    d.point((7, 9), fill=INK); d.point((8, 9), fill=INK)
    return im


def grove_scene(open_way=True):
    """Mother Nature's grove: a long hall of living moss between arching trees, with waterfalls pouring down the cliffs on
    both sides, little streams along the path and a throne woven from roots and flowers on a dais. Behind the throne a
    glowing arch looks out toward Queen Muck's far-off palace (the cutscene hides it behind a waterfall until Mother Nature
    shows the way).
    open_way=False draws the same grove with a waterfall curtain across the arch instead (grove.png); open_way=True is
    the arch opened onto the palace (grove_open.png), and the cutscene fades from one to the other."""
    W, H = 240, 160
    im = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(im)
    rnd = random.Random(21)
    bark, bark_dk, bark_hi = hx("#8a6a4a"), hx("#5e4630"), hx("#b08a62")
    rock, rock_dk, moss = hx("#7d8f8c"), hx("#566a6a"), hx("#6fbf7a")
    water, water_hi, water_dk = hx("#bfeaf6"), hx("#f2fcff"), hx("#6ec0d6")
    dark = hx("#25402f")

    # sky and misty forest far away
    vgrad(d, W, 108, hx("#3a7e66"), hx("#d4f4d8"))
    for x in range(-6, W + 10, 12):
        h = 40 + (x * 7) % 18
        d.rectangle([x + 3, 100 - h // 2, x + 4, 100], fill=hx("#6aa88a"))
        d.ellipse([x - 3, 100 - h, x + 11, 100 - h // 2 + 4], fill=hx("#8fd0a4"))
    for _ in range(26):                                                            # light shafts through the leaves
        x = rnd.randrange(W)
        d.polygon([(x, 0), (x + 3, 0), (x + 16, 104), (x + 8, 104)], fill=(255, 255, 220, 22))

    # the glowing arch behind the throne, with Queen Muck's palace far away inside it
    if open_way:
        d.rectangle([84, 40, 156, 98], fill=hx("#e8fff0"))
        d.pieslice([84, 14, 156, 66], 180, 360, fill=hx("#e8fff0"))
        vgrad(d, W, 1, hx("#e8fff0"), hx("#e8fff0"))
        for i in range(0, 72, 2):                                                      # warm light, brighter toward the middle
            c = int(40 * (1 - abs(i - 36) / 36))
            d.line([(84 + i, 20), (84 + i, 98)], fill=(255, 244, 200, c))
        base = 78
        for cx, w_, top in ((104, 8, 56), (112, 9, 48), (122, 14, 38), (133, 9, 50), (141, 8, 58)):   # palace silhouette
            d.rectangle([cx - w_ // 2, top, cx + w_ // 2, base], fill=hx("#b9a8c8"))
            d.polygon([(cx - w_ // 2 - 1, top), (cx, top - 8), (cx + w_ // 2 + 1, top)], fill=hx("#e8a0c4"))
        d.rectangle([98, 70, 146, base + 10], fill=hx("#b9a8c8"))
        d.pieslice([116, 70, 128, 86], 180, 360, fill=hx("#6a5a86"))
        for x in range(92, 150, 4):
            d.point((x, 98), fill=hx("#9acb90"))
    else:                                                                          # a waterfall curtain hides the way
        d.rectangle([84, 40, 156, 98], fill=water)
        d.pieslice([84, 14, 156, 66], 180, 360, fill=water)
        for x in range(86, 156, 4):
            d.line([(x, 20 if 96 < x < 144 else 40), (x, 98)], fill=water_hi)
        for x in range(90, 156, 6):
            d.line([(x, 46), (x, 90)], fill=water_dk)
    d.line([(84, 98), (84, 36)], fill=dark); d.line([(156, 98), (156, 36)], fill=dark)
    d.arc([84, 14, 156, 66], 180, 360, fill=dark, width=2)
    for i in range(18):                                                            # vines on the arch
        a = 180 + i * 10
        x = 120 + 36 * math.cos(math.radians(a)); y = 40 + 26 * math.sin(math.radians(a))
        d.ellipse([x - 2, y - 2, x + 2, y + 1], fill=hx("#5fae6a" if i % 2 else "#8fd49a"), outline=dark)
        if i % 3 == 0:
            d.point((int(x), int(y) - 3), fill=hx("#ff9cc0"))

    # mossy cliffs on both sides, each with a waterfall
    for side in (0, 1):
        x0, x1 = (0, 66) if side == 0 else (174, 240)
        poly = [(x0, 0), (x1, 0), (x1 - (8 if side == 0 else -8), 70), (x1 - (14 if side == 0 else -14), 104), (x0, 104)]
        d.polygon(poly, fill=rock)
        for _ in range(110):
            x, y = rnd.randrange(x0, x1), rnd.randrange(0, 104)
            d.point((x, y), fill=shade(rock, 1.15 if rnd.random() < .5 else 0.82))
        for y in range(8, 100, 14):
            d.line([(x0, y), (x1 - 6, y + rnd.randrange(-2, 3))], fill=rock_dk)
        for _ in range(30):
            x, y = rnd.randrange(x0, x1 - 8), rnd.randrange(60, 104)
            d.ellipse([x, y, x + rnd.randrange(3, 8), y + 2], fill=moss)
    for fx0 in (18, 198):                                                          # the waterfalls
        d.rectangle([fx0, 0, fx0 + 24, 96], fill=water)
        for x in range(fx0 + 2, fx0 + 24, 4):
            d.line([(x, 0), (x, 96)], fill=water_hi)
        for x in range(fx0 + 4, fx0 + 24, 6):
            d.line([(x, 20), (x, 70)], fill=water_dk)
        d.line([(fx0, 0), (fx0, 96)], fill=rock_dk); d.line([(fx0 + 24, 0), (fx0 + 24, 96)], fill=rock_dk)
        for i in range(7):                                                         # foam where it lands
            d.ellipse([fx0 - 4 + i * 5, 92, fx0 + 3 + i * 5, 100], fill=water_hi)
    for px in (30, 210):                                                           # pools
        d.ellipse([px - 26, 94, px + 26, 116], fill=water_dk, outline=dark)
        d.ellipse([px - 22, 96, px + 22, 112], fill=water)
        d.arc([px - 14, 99, px + 6, 109], 200, 330, fill=water_hi)

    # the ground: grass, with a hall of mossy stone leading to the dais
    d.rectangle([0, 104, W, H], fill=hx("#8fd09a"))
    noise(d, W, H, hx("#8fd09a"), 5, 420, y0=104)
    d.polygon([(100, 100), (140, 100), (214, H), (26, H)], fill=hx("#b6c8a2"), outline=dark)
    for i in range(7):
        t = i / 6.0
        y = int(104 + t * t * 56)
        xl = 100 + (26 - 100) * ((y - 100) / 60.0); xr = 140 + (214 - 140) * ((y - 100) / 60.0)
        d.line([(xl, y), (xr, y)], fill=hx("#8aa07a"))
        for k in range(1, 7):
            x = xl + (xr - xl) * (k / 7.0 + ((i % 2) * 0.07))
            d.line([(x, y), (x + (x - 120) * 0.05, y + 8 * (0.3 + t))], fill=hx("#8aa07a"))
    bloom = random.Random(77)
    for xs in (-1, 1):
        pts = [(120 + xs * 24, 100), (120 + xs * 42, 118), (120 + xs * 70, 138), (120 + xs * 108, H)]
        d.line(pts, fill=hx("#4f9a5c"), width=5)
        steps = 54
        for k in range(steps + 1):
            seg = min(2, int(k / steps * 3)); u = (k / steps * 3) - seg
            if seg == 2: u = min(u, 1.0)
            ax, ay = pts[seg]; bx, by = pts[seg + 1]
            x = int(ax + (bx - ax) * u + bloom.randint(-2, 2)); y = int(ay + (by - ay) * u + bloom.randint(-1, 1))
            petal = hx(bloom.choice(("#ff7fb0", "#ffd0e4", "#ffffff", "#fff08a", "#c8a8ff", "#ff9a7a")))
            d.point((x - 1, y + 1), fill=hx("#6fc688")); d.point((x + 1, y + 1), fill=hx("#6fc688"))
            d.point((x, y + 1), fill=hx("#3f8a4c"))
            for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                d.point((x + dx, y + dy - 1), fill=petal)
            d.point((x, y - 1), fill=hx("#f7c948"))
    for _ in range(90):                                                            # flowers
        x, y = rnd.randrange(W), rnd.randrange(108, H)
        if 120 - (y - 100) * 1.55 < x < 120 + (y - 100) * 1.55:
            continue
        c = hx(rnd.choice(("#ff9cc0", "#fff6a0", "#ffffff", "#c8b0ff")))
        d.point((x, y + 1), fill=hx("#5fae6a")); d.point((x, y), fill=c)
        if rnd.random() < 0.4:
            d.point((x + 1, y), fill=c); d.point((x - 1, y), fill=c)

    # trees that lean together into arches, far to near, like  /\ /\ /\
    def trunk(cx, w_, bot, top=0):
        d.polygon([(cx - w_ // 2 - 3, bot), (cx - w_ // 2, bot - 8), (cx - w_ // 2, top), (cx + w_ // 2, top),
                   (cx + w_ // 2, bot - 8), (cx + w_ // 2 + 3, bot)], fill=bark, outline=dark)
        for x in range(cx - w_ // 2 + 1, cx + w_ // 2, 3):
            d.line([(x, top), (x, bot - 6)], fill=bark_hi)
        d.line([(cx + w_ // 2 - 1, top), (cx + w_ // 2 - 1, bot - 6)], fill=bark_dk)

    def archway(lx, rx, bot, apex, w_):
        trunk(lx, w_, bot); trunk(rx, w_, bot)
        mid = (lx + rx) // 2
        for off in range(w_):
            d.arc([lx - w_ // 2 + off, apex - 18 + off, rx + w_ // 2 - off, apex + 40 - off * 2], 180, 360, fill=bark if off % 2 else bark_dk)
        for i in range(10):                                                        # leaves along the bough
            a = 190 + i * 17
            x = mid + (rx - lx) / 2.0 * math.cos(math.radians(a)); y = apex + 11 + 29 * math.sin(math.radians(a))
            d.ellipse([x - 5, y - 3, x + 5, y + 5], fill=hx("#4fae6a" if i % 2 else "#7cc88a"), outline=dark)
        for k in range(14):                                                        # leaves hanging from the bough
            x = lx + (rx - lx) * k / 13.0
            y = apex + 11 - (24 * 0) + 29 * -math.sin(math.radians(190 + 160 * k / 13.0)) * -1
            d.ellipse([x - 3, y + 1, x + 3, y + 6 + (k % 3)], fill=hx("#6fc47e"), outline=None)

    archway(72, 168, 104, 22, 6)
    archway(46, 194, 118, 4, 8)
    for cx in (6, 234):
        trunk(cx, 12, H)
    for x in range(-4, W + 6, 9):                                                  # a roof of leaves across the top
        d.ellipse([x - 8, -8, x + 10, 8 + (x * 3) % 7], fill=hx("#3f9a5e"), outline=dark)
    for x in range(0, W, 13):
        d.ellipse([x - 5, 2, x + 7, 9 + (x * 5) % 6], fill=hx("#5fb878"))

    # the dais and the throne of roots and flowers
    for i, (x0, x1, y0) in enumerate(((78, 162, 99), (86, 154, 94), (94, 146, 89))):
        d.rectangle([x0, y0, x1, y0 + 5], fill=hx("#cfdcc0") if i % 2 == 0 else hx("#dde8cf"), outline=dark)
        d.line([(x0 + 1, y0 + 1), (x1 - 1, y0 + 1)], fill=hx("#f0f8e8"))
    for x in range(98, 142, 6):
        d.ellipse([x, 85, x + 6, 92], fill=hx("#6fc47e"), outline=dark)
    d.rectangle([108, 84, 132, 90], fill=bark, outline=dark)                       # the seat
    for dx in (-1, 1):                                                             # two root columns arching into a back
        pts = [(120 + dx * 17, 90), (120 + dx * 22, 70), (120 + dx * 19, 50), (120 + dx * 9, 34), (120, 30)]
        d.line(pts, fill=dark, width=5); d.line(pts, fill=bark, width=3)
        d.line([(120 + dx * 21, 74), (120 + dx * 16, 60)], fill=bark_hi)
    d.line([(112, 30), (128, 30)], fill=dark, width=3)
    for i in range(16):                                                            # leaves and blossoms along the back
        a = math.pi * (i / 15.0)
        x = 120 + 20 * math.cos(a) * (-1); y = 62 - 31 * math.sin(a)
        d.ellipse([x - 3, y - 2, x + 3, y + 2], fill=hx("#6fc47e" if i % 2 else "#4fae6a"), outline=dark)
        if i % 3 == 1:
            d.ellipse([x - 1, y - 5, x + 2, y - 2], fill=hx(("#ff9cc0", "#fff6a0", "#ffffff")[(i // 3) % 3]))
    for x, y in ((100, 90), (140, 90), (96, 95), (144, 95)):                       # little glowing lanterns
        d.ellipse([x - 2, y - 7, x + 2, y - 3], fill=hx("#fff6a0"), outline=dark)
        d.line([(x, y - 3), (x, y)], fill=dark)

    im.putalpha(255)
    return pastel(im, 0.05)


def palace_hall_scene(clean=False):
    """The long hall inside Queen Muck's palace, seen from the doorway, in deep one-point perspective: lavender stone-brick walls
    under a gothic vaulted ceiling, tiered chandeliers made of bottles and light bulbs, a magenta carpet strewn with litter running
    to a wooden stage where a towering throne of garbage sits under a golden halo between two pink banners, and four arched sorting
    rooms (cardboard, glass, e-waste, scrap metal) each guarded by a pastel sprout-mole minion in makeshift trash armour.
    clean=False: the palace as Queen Muck keeps it (the state the boss fight and the cutscenes use).
    clean=True: the same hall after her transformation: mint stone, a green carpet, flowers instead of litter, tidy rooms.
    Same layout in both, so the two can be cross-faded."""
    import math
    W, H = 240, 160
    im = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(im)
    rnd = random.Random(21)

    if clean:
        OUT = hx("#2f4a40")
        wall_a, wall_b, brick_hi, brick_lo = hx("#b6e4cc"), hx("#8fcfb0"), hx("#d0f0dc"), hx("#6fb494")
        ceil, rib_hi, rib_lo = hx("#d2e8d2"), hx("#eef8ea"), hx("#8fb8a0")
        tile_a, tile_b, grout = hx("#e4e8d0"), hx("#cfd8b8"), hx("#9fb08c")
        carp, carp_hi, carp_lo = hx("#38b86c"), hx("#6fdc98"), hx("#228a4c")
        far_wall, glass = hx("#c8ecd0"), ("#8fd8c0", "#a8e8d4", "#c8f4e4")
    else:
        OUT = hx("#2e1f40")
        wall_a, wall_b, brick_hi, brick_lo = hx("#b9a0de"), hx("#957cc2"), hx("#cbb8ea"), hx("#7a63a8")
        ceil, rib_hi, rib_lo = hx("#a39fb2"), hx("#c8c4d4"), hx("#6e6882")
        tile_a, tile_b, grout = hx("#c9a8d4"), hx("#b693c6"), hx("#8a72a6")
        carp, carp_hi, carp_lo = hx("#e0187f"), hx("#f45aa8"), hx("#a80f5c")
        far_wall, glass = hx("#8a6aa8"), ("#9a8ad0", "#b8a8e8", "#d8c8f8")
    gold, gold_hi, gold_lo = hx("#f2c14e"), hx("#ffe28a"), hx("#b8862a")

    # ---- perspective: the far wall is the rectangle (BX0,BY0)-(BX1,BY1); the near plane is the whole canvas
    BX0, BX1, BY0, BY1 = 84, 156, 34, 82
    S_FAR = (BX1 - BX0) / W

    def U(z): return (1.0 / z - S_FAR) / (1.0 - S_FAR)        # z=1 near plane -> 1, far wall -> 0
    def xl(u): return BX0 * (1 - u)
    def xr(u): return W - BX0 * (1 - u)
    def ytop(u): return BY0 * (1 - u)
    def yfl(u): return BY1 + (H - BY1) * u
    def wy(u, fr): return ytop(u) + (yfl(u) - ytop(u)) * fr
    def ring_pt(z, th):
        u = U(z); xw = 120 - xl(u)
        return (120 + xw * math.cos(th), ytop(u) - 0.5 * xw * math.sin(th))

    # ================= vaulted ceiling =================
    d.rectangle([0, 0, W, BY0], fill=ceil)
    bands = [i * math.pi / 9 for i in range(10)]
    for i in range(9):
        a, b = bands[i], bands[i + 1]
        near_a, near_b = ring_pt(1.0, a), ring_pt(1.0, b)
        far_a, far_b = ring_pt(1 / S_FAR, a), ring_pt(1 / S_FAR, b)
        d.polygon([near_a, near_b, far_b, far_a], fill=shade(ceil, 1.0 if i % 2 == 0 else 0.93))
    for th in bands:
        d.line([ring_pt(1.0, th), ring_pt(1 / S_FAR, th)], fill=rib_lo)
    for z in (1.18, 1.42, 1.72, 2.08, 2.5, 2.95):
        pts = [ring_pt(z, k * math.pi / 40) for k in range(41)]
        d.line(pts, fill=rib_lo, width=3)
        d.line([(x, y - 1) for x, y in pts], fill=rib_hi, width=1)
        d.line([ring_pt(z, math.pi * 0.0), ring_pt(z, math.pi * 0.0)], fill=rib_lo)
    # far wall, with a round arch on top
    arch = [(120 + 36 * math.cos(k * math.pi / 24), BY0 - 15 * math.sin(k * math.pi / 24)) for k in range(25)]
    d.polygon(arch + [(BX0, BY1), (BX1, BY1)], fill=far_wall)
    for r, a2 in ((44, 70), (32, 90), (20, 120)):                    # warm glow behind the throne
        glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(glow).ellipse([120 - r, 44 - r, 120 + r, 44 + r], fill=(255, 226, 150, a2 // 2))
        im.alpha_composite(glow)
    d = ImageDraw.Draw(im)
    for y in range(BY0 - 14, BY1, 4):                                # far wall bricks
        for x in range(BX0 + (y // 4 % 2) * 4, BX1, 8):
            if y >= BY0 - 14 and (x - 120) ** 2 / 36 ** 2 + (max(0, BY0 - y)) ** 2 / 15 ** 2 < 1:
                d.line([(x, y), (x, y + 3)], fill=shade(far_wall, 0.88))
        d.line([(BX0, y), (BX1, y)], fill=shade(far_wall, 0.9))
    d.line(arch, fill=OUT, width=1)
    d.line([(BX0, BY0), (BX0, BY1)], fill=OUT); d.line([(BX1, BY0), (BX1, BY1)], fill=OUT)

    # ================= floor: perspective tiles =================
    d.rectangle([0, BY1, W, H], fill=tile_a)
    zs = [1.0, 1.1, 1.22, 1.38, 1.58, 1.84, 2.18, 2.62, 3.0, 1 / S_FAR]
    cols = range(-8, 8)
    for r in range(len(zs) - 1):
        ua, ub = U(zs[r]), U(zs[r + 1])
        ya, yb = yfl(ua), yfl(ub)
        sa, sb = 1 / zs[r], 1 / zs[r + 1]
        for c in cols:
            quad = [(120 + c * 40 * sa, ya), (120 + (c + 1) * 40 * sa, ya), (120 + (c + 1) * 40 * sb, yb), (120 + c * 40 * sb, yb)]
            d.polygon(quad, fill=tile_a if (r + c) % 2 == 0 else tile_b)
        d.line([(0, ya), (W, ya)], fill=grout)
    for c in cols:
        d.line([(120 + c * 40, H), (120 + c * 40 * S_FAR, BY1)], fill=grout)

    # ================= side walls =================
    def wall_poly(side):
        X = xl if side < 0 else xr
        return [(X(0), BY0), (0 if side < 0 else W, 0), (0 if side < 0 else W, H), (X(0), BY1)]

    for side in (-1, 1):
        X = xl if side < 0 else xr
        d.polygon(wall_poly(side), fill=wall_a)
        # brick courses and staggered joints, perspective-correct
        rows = 14
        for k in range(rows):
            fa, fb = k / rows, (k + 1) / rows
            for j in range(0, 40):
                z = 1.0 + j * 0.06
                if z > 1 / S_FAR: break
                z2 = min(1 / S_FAR, z + 0.06)
                ua, ub = U(z), U(z2)
                off = 0.03 if k % 2 else 0
                zj = z + off
                if zj >= 1 / S_FAR: continue
                uj = U(zj)
                d.line([(X(uj), wy(uj, fa)), (X(uj), wy(uj, fb))], fill=brick_lo)
            d.line([(X(0), wy(0, fa)), (X(1), wy(1, fa))], fill=brick_lo)
            if k % 3 == 1:                                          # a few lighter stones for texture
                for j in range(0, 40, 3):
                    z = 1.0 + (j + k) % 11 * 0.2
                    if z > 3.2: continue
                    u = U(z)
                    d.line([(X(u), wy(u, fa) + 1), (X(U(z + 0.09)), wy(U(z + 0.09), fa) + 1)], fill=brick_hi)
        # carved frieze under the cornice
        fa, fb = 0.075, 0.125
        d.polygon([(X(1), wy(1, fa)), (X(0), wy(0, fa)), (X(0), wy(0, fb)), (X(1), wy(1, fb))], fill=shade(wall_a, 1.06))
        d.line([(X(1), wy(1, fa)), (X(0), wy(0, fa))], fill=OUT); d.line([(X(1), wy(1, fb)), (X(0), wy(0, fb))], fill=brick_lo)
        for j in range(0, 22):
            z = 1.05 + j * 0.11
            if z > 3.2: break
            u = U(z)
            d.point((X(u), wy(u, 0.1)), fill=shade(wall_b, 0.85))
        # skirting
        d.line([(X(1), wy(1, 0.93)), (X(0), wy(0, 0.93))], fill=OUT)
        d.polygon(wall_poly(side), outline=OUT)

    # ================= side rooms =================
    def door_poly(side, z0, z1, n=16):
        X = xl if side < 0 else xr
        top = []
        for i in range(n + 1):
            t = i / n; z = z0 + (z1 - z0) * t; u = U(z)
            top.append((X(u), wy(u, 0.42 - 0.22 * math.sin(math.pi * t) ** 0.8)))
        u0, u1 = U(z0), U(z1)
        return top, [(X(u1), wy(u1, 0.95)), (X(u0), wy(u0, 0.95))]

    def recycle(cx, cy, col):
        d.polygon([(cx, cy - 5), (cx + 5, cy + 3), (cx - 5, cy + 3)], outline=col)
        d.polygon([(cx, cy - 3), (cx + 3, cy + 2), (cx - 3, cy + 2)], outline=col)
        for x, y in ((cx - 5, cy + 3), (cx + 5, cy + 3), (cx, cy - 5)):
            d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=col)

    def trashcan(cx, cy):
        rect(d, cx - 3, cy - 2, cx + 3, cy + 5, hx("#9aa0b0"), OUT)
        d.line([(cx - 4, cy - 3), (cx + 4, cy - 3)], fill=OUT); d.point((cx, cy - 4), fill=OUT)
        for x in (cx - 1, cx + 1): d.line([(x, cy), (x, cy + 3)], fill=hx("#6a7084"))

    def tools(cx, cy):
        d.line([(cx - 5, cy - 5), (cx + 5, cy + 5)], fill=hx("#c8ccd8"), width=2)
        d.line([(cx + 5, cy - 5), (cx - 5, cy + 5)], fill=hx("#c8ccd8"), width=2)
        d.rectangle([cx - 7, cy - 6, cx - 3, cy - 3], fill=hx("#8a90a4"), outline=OUT)
        d.rectangle([cx + 3, cy - 7, cx + 7, cy - 4], fill=hx("#8a90a4"), outline=OUT)

    def room(side, z0, z1, kind):
        top, bottom = door_poly(side, z0, z1)
        poly = top + bottom
        mask = Image.new("L", (W, H), 0)
        ImageDraw.Draw(mask).polygon(poly, fill=255)
        layer = Image.new("RGBA", (W, H), (0, 0, 0, 255))
        ld = ImageDraw.Draw(layer)
        xs = [p[0] for p in poly]; ys = [p[1] for p in poly]
        bx0, bx1, by0, by1 = int(min(xs)), int(max(xs)) + 1, int(min(ys)), int(max(ys)) + 1
        bg = {"box": ("#fff0b0", "#f2b868"), "glass": ("#8ec8b8", "#587a78"), "ewaste": ("#c8f4d0", "#88c8a0"), "scrap": ("#5a4a52", "#2e2430")}[kind]
        if clean:
            bg = {"box": ("#fffbd8", "#f4e0a0"), "glass": ("#d8f4ec", "#a8d8c8"), "ewaste": ("#e4fae8", "#b4e0c0"), "scrap": ("#d8e8d4", "#a8c0a0")}[kind]
        for y in range(by0, by1 + 1):
            f = (y - by0) / max(1, by1 - by0)
            ld.line([(bx0, y), (bx1, y)], fill=tuple(int(a + (b - a) * f) for a, b in zip(hx(bg[0]), hx(bg[1])))[:3] + (255,))
        gy = by1 - 2                                              # floor of the room
        if kind == "box":
            for i, n in enumerate((3, 2, 3, 1)):
                bx = bx0 + 1 + i * ((bx1 - bx0) // 4)
                for j in range(n):
                    x0, y0 = bx + (j % 2) * 2, gy - (j + 1) * 8
                    rect(ld, x0, y0, x0 + 12, y0 + 8, hx("#c9a066"), hx("#6a4a2a"))
                    ld.line([(x0 + 1, y0 + 1), (x0 + 11, y0 + 1)], fill=hx("#e6c88e"))
                    ld.line([(x0 + 6, y0), (x0 + 6, y0 + 8)], fill=hx("#a07a42"))
                    if j == 0: ld.point((x0 + 3, y0 + 5), fill=hx("#4a9a56")); ld.point((x0 + 4, y0 + 5), fill=hx("#4a9a56"))
        elif kind == "glass":
            for i in range(26):
                x = bx0 + 2 + rnd.randrange(max(1, bx1 - bx0 - 4)); y = gy - rnd.randrange(0, 22)
                col = hx(rnd.choice(("#5ab87a", "#d89a40", "#8ac8f0", "#dff0f4", "#3a7a58", "#c8d8e8")))
                ld.rectangle([x, y - 4, x + 2, y + 2], fill=col, outline=hx("#3a4a50"))
                ld.line([(x + 1, y - 7), (x + 1, y - 5)], fill=col)
            for i in range(6):
                x = bx0 + 3 + rnd.randrange(max(1, bx1 - bx0 - 6)); y = by0 + 4 + rnd.randrange(max(1, by1 - by0 - 10))
                ld.point((x, y), fill=hx("#ffffff")); ld.point((x - 1, y), fill=hx("#ffffff")); ld.point((x + 1, y), fill=hx("#ffffff"))
                ld.point((x, y - 1), fill=hx("#ffffff")); ld.point((x, y + 1), fill=hx("#ffffff"))
        elif kind == "ewaste":
            for i in range(5):
                x0 = bx0 + 1 + i * ((bx1 - bx0 - 8) // 4); y0 = gy - 9 - (i % 2) * 11
                rect(ld, x0, y0, x0 + 9, y0 + 9, hx("#d8d0b8"), OUT)
                ld.rectangle([x0 + 2, y0 + 2, x0 + 7, y0 + 6], fill=hx("#2a4a48"))
                ld.point((x0 + 3, y0 + 3), fill=hx("#8ad8c8"))
            for i in range(3):
                x0 = bx0 + 3 + i * ((bx1 - bx0 - 8) // 2)
                rect(ld, x0, gy - 14, x0 + 6, gy, hx("#cfc8b0"), OUT); ld.point((x0 + 3, gy - 11), fill=hx("#4ac88a"))
        else:
            for i in range(9):
                cx, cy = bx0 + 3 + rnd.randrange(max(1, bx1 - bx0 - 6)), gy - 3 - rnd.randrange(0, 20)
                rr = rnd.randrange(3, 6)
                ld.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=hx(rnd.choice(("#8a90a4", "#a8644a", "#6a7084", "#c8744a"))), outline=OUT)
                ld.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=hx("#2e2430"))
            for i in range(4):
                y = gy - 2 - i * 5
                ld.rectangle([bx0 + 1, y - 2, bx1 - 1, y + 1], fill=hx("#9aa0b4"), outline=OUT)
            ld.polygon([(bx0 + 4, by0 + 6), (bx0 + 12, by0 + 2), (bx0 + 14, by0 + 14), (bx0 + 6, by0 + 16)], fill=hx("#c8ccd8"), outline=OUT)
            for i in range(6):
                ld.ellipse([bx1 - 14 + i * 2, gy - 20 + (i % 2) * 3, bx1 - 10 + i * 2, gy - 17 + (i % 2) * 3], outline=hx("#8a90a4"))
        im.paste(layer.convert("RGB"), (0, 0), mask)
        # stone frame with a keystone
        d_ = ImageDraw.Draw(im)
        d_.line([(x, y - 2) for x, y in top], fill=brick_hi, width=1)
        d_.line(top, fill=OUT, width=1)
        d_.line([top[0], bottom[1]], fill=OUT); d_.line([top[-1], bottom[0]], fill=OUT)
        mx, my = top[len(top) // 2]
        d_.rectangle([mx - 2, my - 4, mx + 2, my], fill=gold, outline=OUT)
        return (xs, ys)

    icons = []
    for side, z0, z1, kind, icon in ((-1, 1.10, 1.70, "box", "recycle_green"), (-1, 1.86, 2.55, "glass", "recycle_gray"),
                                      (1, 1.86, 2.55, "ewaste", "can"), (1, 1.10, 1.70, "scrap", "tools")):
        xs, ys = room(side, z0, z1, kind)
        zm = (z0 + z1) / 2
        u = U(zm); X = xl if side < 0 else xr
        icons.append((icon, X(u), wy(u, 0.10)))
    d = ImageDraw.Draw(im)
    for icon, ix, iy in icons:
        ix, iy = int(ix), int(iy)
        if icon == "recycle_green": recycle(ix, iy + 1, hx("#3aa860"))
        elif icon == "recycle_gray": recycle(ix, iy + 1, hx("#8a90a4") if not clean else hx("#3aa860"))
        elif icon == "can": trashcan(ix, iy)
        else: tools(ix, iy)

    # pilasters between the rooms and a row of pointed windows beyond the last arch
    for side in (-1, 1):
        X = xl if side < 0 else xr
        for zc in (1.78, 1.04, 2.6):
            za, zb = zc - 0.05, zc + 0.05
            if za < 1.0: za = 1.0
            ua, ub = U(za), U(zb)
            d.polygon([(X(ua), wy(ua, 0.1)), (X(ub), wy(ub, 0.1)), (X(ub), wy(ub, 0.96)), (X(ua), wy(ua, 0.96))], fill=shade(wall_a, 1.1), outline=OUT)
        for za, zb in ((2.68, 2.82), (2.9, 3.04), (3.1, 3.22)):
            ua, ub = U(za), U(zb); um = (ua + ub) / 2
            quad = [(X(ua), wy(ua, 0.62)), (X(ua), wy(ua, 0.34)), (X(um), wy(um, 0.22)), (X(ub), wy(ub, 0.34)), (X(ub), wy(ub, 0.62))]
            d.polygon(quad, fill=hx(glass[0]), outline=OUT)
            d.line([(X(um), wy(um, 0.24)), (X(um), wy(um, 0.62))], fill=hx(glass[2]))

    # ================= carpet, stage, throne =================
    cx0, cw = 120, 52.5
    def carpet_x(m, z): return cx0 + m * cw / z
    d.polygon([(carpet_x(-1, 1.0), H), (carpet_x(1, 1.0), H), (carpet_x(1, 1 / S_FAR), BY1), (carpet_x(-1, 1 / S_FAR), BY1)], fill=carp)
    for r in range(len(zs) - 1):                                     # lighter tufted bands
        if r % 2: continue
        ua, ub = U(zs[r]), U(zs[r + 1])
        sa, sb = 1 / zs[r], 1 / zs[r + 1]
        d.polygon([(120 - 40 * sa, yfl(ua)), (120 + 40 * sa, yfl(ua)), (120 + 40 * sb, yfl(ub)), (120 - 40 * sb, yfl(ub))], fill=carp_hi)
    for m in (-1, 1):
        d.line([(carpet_x(m, 1.0), H), (carpet_x(m, 1 / S_FAR), BY1)], fill=gold, width=2)
        d.line([(carpet_x(m * 0.86, 1.0), H), (carpet_x(m * 0.86, 1 / S_FAR), BY1)], fill=carp_lo)
    for y in range(BY1 + 1, H, 2):                                  # dithering on the carpet edges
        for m in (-1, 1):
            u = (y - BY1) / (H - BY1); z = 1 / (u * (1 - S_FAR) + S_FAR)
            xx = int(carpet_x(m * 0.8, z))
            if (xx + y) % 2 == 0: d.point((xx, y), fill=carp_lo)

    def litter(m, z, kind):
        s = 1.0 / z
        x = carpet_x(m, z); y = yfl(U(z))
        w = max(2, int(round(6 * s)))
        if clean:
            for i in range(3):
                d.point((int(x) + i * 2, int(y) - (i % 2)), fill=hx(rnd.choice(("#f4a6c8", "#ffffff", "#f7d88a"))))
            return
        if kind == "can":
            col = hx(rnd.choice(("#4a6ae0", "#d84a4a", "#c8ccd8")))
            d.rectangle([x - w // 2, y - w, x + w // 2, y], fill=col, outline=OUT); d.point((x, y - w + 1), fill=hx("#ffffff"))
        elif kind == "banana":
            d.line([(x - w, y), (x, y - 1), (x + w, y - 2)], fill=hx("#f4d040"), width=max(1, int(2 * s + 0.5)))
            d.point((x + w, y - 3), fill=hx("#6a4a1a"))
        elif kind == "paper":
            d.ellipse([x - w // 2, y - w // 2, x + w // 2, y + w // 2], fill=hx("#ece8f0"), outline=hx("#8a86a0"))
        else:
            d.rectangle([x - w, y - w // 2, x + w, y], fill=hx("#c9a066"), outline=hx("#6a4a2a"))
    items = [(-0.7, 1.12, "can"), (0.5, 1.25, "banana"), (-0.2, 1.4, "paper"), (0.75, 1.5, "can"), (-0.6, 1.7, "banana"),
             (0.15, 1.8, "cardboard"), (-0.35, 2.0, "can"), (0.6, 2.1, "paper"), (-0.75, 2.3, "cardboard"), (0.3, 2.5, "can"),
             (-0.1, 2.7, "banana"), (0.55, 2.9, "paper"), (-0.5, 1.95, "paper"), (0.0, 1.15, "cardboard")]
    for m, z, k in sorted(items, key=lambda t: -t[1]):
        litter(m, z, k)
    if not clean:                                                    # litter on the floor beside the carpet
        for i in range(18):
            side = -1 if i % 2 == 0 else 1
            z = rnd.uniform(1.1, 2.9); m = side * rnd.uniform(1.25, 2.6)
            litter(m, z, rnd.choice(("can", "banana", "paper", "cardboard")))

    # stage: wooden, with two steps and the carpet running up
    wood, wood_hi, wood_lo = hx("#b87a44"), hx("#d89a5c"), hx("#7a4a28")
    for k, (y0, y1, hw) in enumerate(((78, 84, 38), (74, 79, 32))):
        rect(d, 120 - hw, y0, 120 + hw, y1, wood if not clean else hx("#d8c8a0"), OUT)
        for x in range(120 - hw + 3, 120 + hw - 1, 6):
            d.line([(x, y0 + 1), (x, y1 - 1)], fill=wood_lo if not clean else hx("#b8a880"))
        d.line([(120 - hw + 1, y0 + 1), (120 + hw - 1, y0 + 1)], fill=wood_hi if not clean else hx("#f0e4c0"))
    d.polygon([(110, 74), (130, 74), (133, 84), (107, 84)], fill=carp)
    d.line([(110, 74), (107, 84)], fill=gold); d.line([(130, 74), (133, 84)], fill=gold)

    # golden halo behind the throne
    halo = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    for r, a2 in ((40, 45), (34, 70), (28, 100), (22, 140)):
        hd.ellipse([120 - r, 50 - r, 120 + r, 50 + r], fill=(255, 226, 120, a2))
    im.alpha_composite(halo)
    d = ImageDraw.Draw(im)
    d.ellipse([120 - 26, 50 - 26, 120 + 26, 50 + 26], outline=gold_hi, width=2)
    d.ellipse([120 - 28, 50 - 28, 120 + 28, 50 + 28], outline=gold_lo)

    # the throne: a tall heap of garbage with a tufted pink cushion and a giant bow
    hb = hx("#3f8a62") if clean else hx("#5a4a68")
    d.polygon([(94, 74), (98, 56), (106, 46), (120, 42), (134, 46), (142, 56), (146, 74)], fill=hb, outline=OUT)
    d.polygon([(86, 74), (90, 62), (98, 56), (104, 74)], fill=hb, outline=OUT)
    d.polygon([(154, 74), (150, 62), (142, 56), (136, 74)], fill=hb, outline=OUT)

    def scrap(x, y, kind):
        if clean:                                                    # vines and flowers instead of garbage
            if kind < 3:
                d.ellipse([x, y, x + 4, y + 3], fill=hx(rnd.choice(("#6fc688", "#4fae70", "#8fdca0"))), outline=hx("#2f6a4a"))
            else:
                d.point((x + 1, y + 1), fill=hx(rnd.choice(("#f4a6c8", "#ffffff", "#f7d88a")))); d.point((x + 2, y + 1), fill=hx("#f7d88a"))
                d.point((x + 1, y), fill=hx(rnd.choice(("#f4a6c8", "#ffffff")))); d.point((x + 1, y + 2), fill=hx("#ffffff"))
            return
        if kind == 0:   d.rectangle([x, y, x + 3, y + 4], fill=hx(rnd.choice(("#4a6ae0", "#d84a4a", "#c8ccd8"))), outline=OUT)      # can
        elif kind == 1: d.rectangle([x, y, x + 1, y + 5], fill=hx(rnd.choice(("#5ab87a", "#c8884a", "#8ac8f0"))), outline=OUT)        # bottle
        elif kind == 2: d.rectangle([x, y, x + 5, y + 4], fill=hx("#c9a066"), outline=hx("#6a4a2a")); d.line([(x + 2, y), (x + 2, y + 4)], fill=hx("#a07a42"))
        elif kind == 3: d.ellipse([x, y, x + 5, y + 5], fill=hx("#3a4228"), outline=OUT); d.point((x + 1, y + 1), fill=hx("#7a8858"))   # bin bag
        elif kind == 4: d.ellipse([x, y, x + 6, y + 6], fill=hx("#3a3a44"), outline=OUT); d.ellipse([x + 2, y + 2, x + 4, y + 4], fill=hx("#8a90a4"))   # tyre
        else:           d.rectangle([x, y, x + 4, y + 3], fill=hx("#ece8f0"), outline=hx("#8a86a0"))                                      # paper
    for row in range(10):
        y = 44 + row * 3
        half = 9 + row * 1.6
        for i in range(int(half // 2.4)):
            x = 120 + rnd.uniform(-half, half)
            scrap(int(x) - 2, y + rnd.randrange(-1, 2), rnd.randrange(6))
    for row in range(6):
        y = 58 + row * 3
        for sx in (-1, 1):
            for i in range(3):
                scrap(int(120 + sx * (28 + rnd.uniform(0, 20))) - 2, y + rnd.randrange(-1, 2), rnd.randrange(6))
    pink, pink_hi, pink_lo = (hx("#f08fbe"), hx("#ffc0dc"), hx("#c04a86")) if not clean else (hx("#f4a6c8"), hx("#ffd8e8"), hx("#d878a8"))
    rect(d, 111, 46, 129, 62, pink, OUT)                             # tufted back cushion
    rect(d, 106, 63, 134, 71, pink, OUT)                             # seat
    d.line([(108, 65), (132, 65)], fill=pink_hi)
    for bx in (116, 124):
        for by in (51, 57):
            d.rectangle([bx - 1, by - 1, bx + 1, by + 1], fill=pink_lo); d.point((bx, by), fill=pink_hi)
    for bx in (112, 120, 128): d.point((bx, 68), fill=pink_lo)
    bow_a, bow_b = (hx("#e0507f"), hx("#b83050")) if not clean else (hx("#f4a6c8"), hx("#d878a8"))
    for sx in (-1, 1):                                               # the giant bow on top
        d.polygon([(120, 40), (120 + sx * 15, 31), (120 + sx * 17, 45), (120, 43)], fill=bow_a, outline=OUT)
        for dx, dy in ((8, 36), (12, 41), (12, 34)): d.point((120 + sx * dx, dy), fill=hx("#ffffff"))
    d.rectangle([116, 37, 124, 45], fill=bow_b, outline=OUT)
    # banners
    for bx in (90, 150):
        d.line([(bx - 7, 38), (bx + 7, 38)], fill=gold, width=2)
        d.polygon([(bx - 6, 39), (bx + 6, 39), (bx + 6, 62), (bx, 68), (bx - 6, 62)], fill=hx("#e0507f") if not clean else hx("#6fc688"), outline=OUT)
        d.line([(bx - 5, 42), (bx + 5, 42)], fill=gold); d.ellipse([bx - 2, 48, bx + 2, 52], fill=gold, outline=OUT)

    # ================= chandeliers of bottles, chains and bulbs =================
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    bulbs = []

    def chandelier(cx, y, rx, bh, n):
        d.line([(cx - 3, 0), (cx - rx // 2, y - 1)], fill=hx("#7a8094")); d.line([(cx + 3, 0), (cx + rx // 2, y - 1)], fill=hx("#7a8094"))
        d.line([(cx, 0), (cx, y - 1)], fill=hx("#9aa0b4"))
        d.ellipse([cx - rx, y - 2, cx + rx, y + 2], fill=hx("#6a6a7e"), outline=OUT)
        d.line([(cx - rx + 3, y - 1), (cx + rx - 3, y - 1)], fill=gold_hi)
        step = (2 * rx - 6) / (n - 1)
        for i in range(n):
            x = int(cx - rx + 3 + i * step)
            col = hx(rnd.choice(("#5ab87a", "#6aa8e8", "#d89a40", "#e8f0f4", "#c85a8a")))
            d.line([(x, y + 2), (x, y + 3)], fill=hx("#8a90a4"))
            d.rectangle([x - 1, y + 4, x + 1, y + 3 + bh], fill=col)
            d.point((x - 1, y + 5), fill=shade(col, 1.3)); d.line([(x - 1, y + 3 + bh), (x + 1, y + 3 + bh)], fill=shade(col, 0.7))
        for i in range(n - 1):
            x = int(cx - rx + 3 + (i + 0.5) * step)
            bulbs.append((x, y + 6 + bh // 2))
    chandelier(120, 3, 30, 9, 9)
    chandelier(120, 21, 17, 7, 5)
    for x, y in bulbs:
        gd.ellipse([x - 7, y - 7, x + 7, y + 7], fill=(255, 236, 150, 60))
    im.alpha_composite(glow)
    d = ImageDraw.Draw(im)
    for x, y in bulbs:
        d.line([(x, y - 3), (x, y - 1)], fill=hx("#8a90a4"))
        d.ellipse([x - 2, y - 1, x + 2, y + 3], fill=hx("#fff0a0"), outline=hx("#c89a2a")); d.point((x, y + 1), fill=hx("#ffffff"))

    # ================= the minions =================
    def minion(cx, feet, k, body, body_dk, cape, kind, face=1):
        def R(v): return max(1, int(round(v * k)))
        MO = hx("#4a3050")
        d.polygon([(cx - R(9), feet - R(18)), (cx + R(9), feet - R(18)), (cx + R(16) * face, feet - R(1)), (cx - R(14) * face, feet - R(1))], fill=cape, outline=MO)
        for fx in (-6, 6):
            d.ellipse([cx + R(fx) - R(4), feet - R(4), cx + R(fx) + R(4), feet], fill=body_dk, outline=MO)
        d.ellipse([cx - R(13), feet - R(25), cx + R(13), feet - R(2)], fill=body, outline=MO)
        d.ellipse([cx - R(8), feet - R(12), cx + R(8), feet - R(3)], fill=shade(body, 1.08))
        for ex in (-6, 6):                                           # tiny dot eyes, blush, mouth
            d.rectangle([cx + R(ex), feet - R(17), cx + R(ex) + max(1, R(2)) - 1, feet - R(17) + R(3) - 1], fill=hx("#3a2a48"))
            d.rectangle([cx + R(ex * 1.6) - R(2), feet - R(13), cx + R(ex * 1.6) + R(2), feet - R(12)], fill=hx("#f4a0b4"))
        d.line([(cx - R(2), feet - R(12)), (cx + R(2), feet - R(12))], fill=hx("#3a2a48"))
        rect(d, cx - R(6), feet - R(10), cx + R(6), feet - R(4), hx("#c9a066"), hx("#6a4a2a"))   # cardboard breastplate
        d.line([(cx, feet - R(10)), (cx, feet - R(4))], fill=hx("#a07a42"))
        for sx in (-1, 1):                                           # shoulder pads
            rect(d, cx + sx * R(11) - R(2), feet - R(17), cx + sx * R(11) + R(2), feet - R(14), hx("#c9a066"), hx("#6a4a2a"))
        # helmet
        top = feet - R(23)
        if kind == "can":
            rect(d, cx - R(8), top - R(8), cx + R(8), top, hx("#b8bcc8"), MO)
            for yy in (top - R(6), top - R(3)): d.line([(cx - R(7), yy), (cx + R(7), yy)], fill=hx("#8a90a4"))
            d.line([(cx + R(1), top - R(7)), (cx - R(1), top - R(4)), (cx + R(2), top - R(4)), (cx, top - R(1))], fill=hx("#f2d040"))
        elif kind == "bucket":
            d.polygon([(cx - R(9), top), (cx + R(9), top), (cx + R(7), top - R(9)), (cx - R(7), top - R(9))], fill=hx("#a8aab8") if body[2] > 200 else hx("#a86a40"), outline=MO)
            d.line([(cx - R(7), top - R(6)), (cx + R(7), top - R(6))], fill=hx("#6a6a7a") if body[2] > 200 else hx("#7a4a28"))
            d.point((cx - R(5), top - R(3)), fill=hx("#d8744a")); d.point((cx + R(4), top - R(8)), fill=hx("#d8744a"))
        else:
            rect(d, cx - R(9), top - R(13), cx + R(9), top, hx("#d8d0b8"), MO)               # CRT monitor
            rect(d, cx - R(6), top - R(11), cx + R(6), top - R(3), hx("#2a4a48"), MO)
            d.line([(cx - R(4), top - R(9)), (cx - R(2), top - R(9))], fill=hx("#8ad8c8")); d.point((cx + R(7), top - R(2)), fill=hx("#e04a4a"))
        tt = top - (R(13) if kind == "crt" else R(9))
        d.line([(cx, tt), (cx, tt - R(4))], fill=hx("#4a9a56"))                  # the sprout
        for sx in (-1, 1):
            d.ellipse([cx + sx * R(4) - R(3), tt - R(7), cx + sx * R(4) + R(3), tt - R(3)], fill=hx("#7ad06a"), outline=hx("#3a7a46"))

    # floor shadows then the four minions
    for cx_, fy, rr in ((46, 132, 19), (92, 104, 14), (158, 106, 14), (194, 134, 19)):
        sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(sh).ellipse([cx_ - rr, fy - 3, cx_ + rr, fy + 3], fill=(60, 30, 80, 70))
        im.alpha_composite(sh)
    d = ImageDraw.Draw(im)
    minion(92, 104, 0.72, hx("#a8d0f0"), hx("#82b0d8"), hx("#cfe4f8"), "bucket", 1)
    minion(158, 106, 0.72, hx("#f4b4d0"), hx("#d890b0"), hx("#f8d0e0"), "crt", -1)
    minion(46, 132, 1.05, hx("#a8e6a0"), hx("#84c07c"), hx("#f4f0f8"), "can", 1)
    minion(194, 134, 1.05, hx("#d89a70"), hx("#b07650"), hx("#e07a3a"), "bucket", -1)
    # what they are doing: tossed cardboard, a bottle in tongs, a cable, a wrench; each with a little raised arm
    GR, BL, PK, BR = hx("#84c07c"), hx("#82b0d8"), hx("#d890b0"), hx("#b07650")
    d.ellipse([27, 108, 33, 114], fill=hx("#a8e6a0"), outline=hx("#4a3050"))          # green: arm up, box in the air
    rect(d, 14, 92, 24, 99, hx("#c9a066"), hx("#6a4a2a")); d.line([(19, 92), (19, 99)], fill=hx("#a07a42"))
    for x, y in ((26, 104), (23, 101), (21, 99)): d.point((x, y), fill=hx("#ffe28a"))
    d.ellipse([105, 97, 110, 102], fill=hx("#a8d0f0"), outline=hx("#4a3050"))          # blue: tongs holding a bottle
    d.line([(108, 98), (116, 94)], fill=hx("#c8ccd8"), width=1); d.line([(108, 100), (116, 96)], fill=hx("#c8ccd8"), width=1)
    d.rectangle([115, 92, 118, 99], fill=hx("#5ab87a"), outline=OUT); d.line([(116, 90), (116, 92)], fill=hx("#5ab87a"))
    d.ellipse([146, 102, 151, 107], fill=hx("#f4b4d0"), outline=hx("#4a3050"))          # pink: dragging a cable
    d.line([(148, 106), (140, 112), (134, 110), (130, 116)], fill=hx("#4a4a58"), width=1); d.rectangle([128, 116, 132, 119], fill=hx("#8a90a4"), outline=OUT)
    d.ellipse([205, 118, 211, 124], fill=hx("#d89a70"), outline=hx("#4a3050"))          # brown: a wrench
    d.line([(208, 120), (218, 112)], fill=hx("#c8ccd8"), width=2); d.rectangle([217, 108, 222, 113], fill=hx("#8a90a4"), outline=OUT)
    for cx_, fy, kk, col in ((46, 132, 1.05, GR), (92, 104, 0.72, BL), (158, 106, 0.72, PK), (194, 134, 1.05, BR)):     # the other hand
        for sx in (-1, 1):
            pass

    im.putalpha(255)
    return pastel(im, 0.06 if clean else 0.1)


def portal_sheet():
    """The portal: 8 animation frames side by side, 40x56 each. A pixel-art vortex: three spiral arms turn around a bright core
    inside a glowing rim, with a dithered halo around it. Cycling the frames makes it spin."""
    import math
    FR, W, H = 8, 40, 56
    cx, cy, rx, ry = 19.5, 27.5, 15.5, 23.5
    ramp = [hx(c) for c in ("#0c3a3c", "#14665f", "#2fa88c", "#74dcb4", "#c4f6e0", "#ffffff")]
    im = Image.new("RGBA", (FR * W, H), (0, 0, 0, 0))
    px = im.load()
    for f in range(FR):
        phase = f / FR * 2 * math.pi
        for y in range(H):
            for x in range(W):
                u, v = (x - cx) / rx, (y - cy) / ry
                r = math.hypot(u, v)
                th = math.atan2(v, u)
                X = f * W + x
                if r <= 1.0:
                    arm = 0.5 + 0.5 * math.sin(3 * (th + 2.6 * r) - phase)
                    t = 0.18 + 0.5 * arm * (0.35 + 0.65 * r) + 0.55 * (1 - r) ** 2
                    if r > 0.82:
                        t = 0.78 + 0.2 * (0.5 + 0.5 * math.sin(6 * th - phase * 2))
                    idx = min(len(ramp) - 1, int(t * len(ramp)))
                    c = ramp[idx]
                    px[X, y] = (c[0], c[1], c[2], 255)
                elif r < 1.22:
                    chance = (1.22 - r) / 0.22
                    if (x + y) % 2 == 0 and chance > 0.35 + 0.3 * (0.5 + 0.5 * math.sin(5 * th + phase)):
                        c = ramp[3] if chance > 0.7 else ramp[2]
                        px[X, y] = (c[0], c[1], c[2], 255 if chance > 0.55 else 150)
        for k in range(5):
            a = phase + k * 1.26
            rr = 1.05 + 0.1 * ((f + k * 3) % 5)
            sx, sy = int(cx + rx * rr * math.cos(a)), int(cy + ry * rr * math.sin(a))
            if 0 <= sx < W and 0 <= sy < H:
                px[f * W + sx, sy] = (255, 255, 255, 255)
    return im


def story_backdrops():
    save("backdrop/room.png", room_scene(False))
    save("backdrop/room_hole.png", room_scene(True))
    save("backdrop/palace.png", palace_scene())
    save("backdrop/meadow.png", meadow_scene())
    save("backdrop/grove.png", grove_scene(False))
    save("portal.png", portal_sheet())
    import sys as _sys; _sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "palace_hall"))
    import scene as _palace_scene     # v2 hall: native 720x480 art (tools/palace_hall/), replaces palace_hall_scene()
    _palace_scene.main(OUT)


def story_sprites():
    story_backdrops()
    npc_sprites(); weapon_sprites()
    save("npc/mother_nature.png", mother_nature())
    save("npc/grove_guard.png", grove_guard())
    save("boss/queen_muck.png", queen_muck("normal"))
    save("boss/queen_muck_laugh.png", queen_muck("laugh"))
    save("boss/queen_muck_rage.png", queen_muck("rage"))
    save("boss/queen_muck_sad.png", queen_muck("sad"))
    save("boss/queen_muck_clean.png", queen_muck("clean"))
    save("boss/mound.png", muck_mound("normal"), 4)
    save("boss/mound_rage.png", muck_mound("rage"), 4)
    save("boss/experiment.png", experiment("normal"))
    save("boss/experiment_rage.png", experiment("rage"))
    save("boss/experiment_stun.png", experiment("stun"))
    save("boss/ghost.png", stink_ghost())
    for v in range(3):
        save("boss/junk_cube%d.png" % v, junk_cube(v))
    save("boss/imp.png", imp_sprite())


def main():
    item_sprites(); bin_sprites(); world_sprites(); player_sheets(); outfit_sheets(); tile_sprites(); backdrop_sprites(); story_sprites()
    n = sum(len(f) for _, _, f in os.walk(OUT))
    print("wrote %d textures to %s" % (n, os.path.normpath(OUT)))


if __name__ == "__main__":
    main()
