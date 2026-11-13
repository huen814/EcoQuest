#!/usr/bin/env python3
"""Generates every texture EcoQuest needs (dreamy pastel, Omori-inspired pixel art, 16 px art scaled x3 = 48 px tiles).

Usage:  python3 tools/gen_sprites.py          (needs Pillow)
Output: src/main/resources/assets/textures/...

Replace any generated PNG with hand-drawn art of the same size and name; the game does not care.
"""
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


def player_frame(direction, step, boat, who="sister"):
    """One 16x24 frame, chibi proportions (big head). step 0..3 (0,2 stand; 1,3 stride)."""
    L = LOOKS[who]
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

    # head
    rect(d, 3, hy, 12, hy + 8, skin, INK)
    if direction == "up":
        if short: rect(d, 4, hy + 1, 11, hy + 5, hair, None)
        else: rect(d, 3, hy, 12, hy + 8, hair, INK)
        if long_: rect(d, 4, hy + 9, 11, hy + 11, hair, None)
    elif direction == "down":
        if short:
            rect(d, 4, hy + 1, 11, hy + 2, hair, None); rect(d, 3, hy + 1, 4, hy + 3, hair, None); rect(d, 11, hy + 1, 12, hy + 3, hair, None)
        else:
            side = 9 if long_ else 6
            rect(d, 3, hy, 12, hy + 3, hair, INK); rect(d, 3, hy, 4, hy + side, hair, None); rect(d, 11, hy, 12, hy + side, hair, None)
        d.point((6, hy + 5), fill=INK); d.point((9, hy + 5), fill=INK)
        if L["blush"]:
            d.point((5, hy + 6), fill=L["blush"]); d.point((10, hy + 6), fill=L["blush"])
    else:
        left = direction == "left"
        if short:
            rect(d, 4, hy + 1, 11, hy + 2, hair, None)
            if left: rect(d, 8, hy + 1, 11, hy + 4, hair, None)
            else: rect(d, 4, hy + 1, 7, hy + 4, hair, None)
        else:
            rect(d, 3, hy, 12, hy + 3, hair, INK)
            if left: rect(d, 8, hy, 12, hy + 6, hair, INK)
            else: rect(d, 3, hy, 7, hy + 6, hair, INK)
            if long_:
                if left: rect(d, 8, hy + 7, 12, hy + 10, hair, None)
                else: rect(d, 3, hy + 7, 7, hy + 10, hair, None)
        ex = 5 if left else 10
        d.point((ex, hy + 5), fill=INK)
        if L["blush"]: d.point((ex + (-1 if left else 1), hy + 6), fill=L["blush"])
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


def main():
    item_sprites(); bin_sprites(); world_sprites(); player_sheets(); tile_sprites(); backdrop_sprites()
    n = sum(len(f) for _, _, f in os.walk(OUT))
    print("wrote %d textures to %s" % (n, os.path.normpath(OUT)))


if __name__ == "__main__":
    main()
