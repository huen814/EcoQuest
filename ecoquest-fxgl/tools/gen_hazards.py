#!/usr/bin/env python3
"""Queen Bijou phase-2 hazards: Stinky Banana Peel + Takeout (junk ring) Meteor.
Usage: python3 tools/gen_hazards.py   (needs Pillow)
Output: boss/stink_peel.png (32px art x2 = 64), boss/takeout_meteor.png (48px art x3 = 144)
"""
import math, os, random
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "textures", "boss")
INK = (26, 14, 38, 255)


def hx(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def save(name, im, scale):
    os.makedirs(OUT, exist_ok=True)
    im.resize((im.width * scale, im.height * scale), Image.NEAREST).save(os.path.join(OUT, name))


def px(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), c)


def disc(im, cx, cy, r, c):
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                px(im, x, y, c)


def outline(im, col, glow=None):
    """1px outline around opaque pixels; optional soft glow ring beyond it."""
    src = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < im.width and 0 <= ny < im.height and src.getpixel((nx, ny))[3] == 255:
                        im.putpixel((x, y), col); break
    if glow:
        s2 = im.copy()
        for y in range(im.height):
            for x in range(im.width):
                if s2.getpixel((x, y))[3] == 0:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < im.width and 0 <= ny < im.height and s2.getpixel((nx, ny)) == col:
                            im.putpixel((x, y), glow); break


def sparkle(im, x, y, c):
    for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
        px(im, x + dx, y + dy, c)


# ------------------------------------------------------------------ stinky banana peel (32x32)
def flap(im, cx, cy, ang, ln, wd, bend, squash, col, edge, tip):
    """One curled peel flap: a leaf-shaped strip radiating from (cx, cy)."""
    ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
    left, right, mid = [], [], []
    n = 14
    for i in range(n + 1):
        t = i / n
        w = wd * (math.sin(math.pi * (0.12 + 0.88 * t)) ** 0.7) * (1 - 0.25 * t)
        bx = ca * ln * t - sa * bend * ln * t * t
        by = (sa * ln * t + ca * bend * ln * t * t) * squash
        nx, ny = -sa, ca * squash
        left.append((cx + bx + nx * w, cy + by + ny * w))
        right.append((cx + bx - nx * w, cy + by - ny * w))
        mid.append((cx + bx, cy + by))
    d = ImageDraw.Draw(im)
    d.polygon(left + right[::-1], fill=col, outline=edge)
    d.line(mid[2:-2], fill=hx("#fff2a0"))          # highlight along the strip
    ex, ey = mid[-1]
    d.rectangle([round(ex) - 1, round(ey) - 1, round(ex), round(ey)], fill=tip)  # brown tip


def stink_peel():
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    glow = hx("#b455ff", 110)
    # thick toxic fumes rising behind the peel
    cols = [hx("#2c1146", 235), hx("#4a2078", 235), hx("#6e35a8", 215), hx("#9a5fd0", 170)]
    for i, (fx, h) in enumerate(((9, 22), (16, 27), (23, 20))):
        for t in range(h):
            x = fx + int(round(math.sin(t * 0.55 + i * 1.9) * 2.2))
            y = 20 - t
            wdt = 3 if t < h - 5 else 1
            col = cols[min(3, t * 4 // h)]
            for k in range(-wdt // 2, wdt // 2 + 1):
                px(im, x + k, y, col)
    peel = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    Y, YD, E = hx("#f7d83a"), hx("#e0b420"), hx("#8a6a10")
    BRN = hx("#4a2e10")
    # three long strips hanging from the stem, side ones drooping out and curling up
    flap(peel, 14, 10, 104, 17, 3.0, 0.62, 1.0, YD, E, BRN)
    flap(peel, 18, 10, 76, 17, 3.0, -0.62, 1.0, YD, E, BRN)
    flap(peel, 16, 10, 90, 16, 3.4, 0.0, 1.0, Y, E, BRN)
    d = ImageDraw.Draw(peel)
    d.ellipse([11, 8, 21, 14], fill=Y, outline=E)                 # shoulder where the strips join
    d.line([(13, 11), (19, 11)], fill=hx("#fff2a0"))
    d.rectangle([15, 3, 17, 8], fill=hx("#7a5a22"), outline=BRN)  # brown stem
    d.line([(16, 4), (16, 7)], fill=hx("#a98a4a"))
    outline(peel, INK, glow)
    im.alpha_composite(peel)
    for p in ((16, 18), (15, 22), (17, 24), (8, 21), (24, 21)):
        px(im, p[0], p[1], hx("#8a5a1a"))
    sparkle(im, 4, 8, hx("#e8c8ff")); sparkle(im, 28, 12, hx("#ffffff")); sparkle(im, 6, 25, hx("#e8c8ff")); px(im, 27, 4, hx("#e8c8ff")); px(im, 2, 17, hx("#ffffff"))
    return im


# ------------------------------------------------------------------ takeout meteor (48x48)
def rrect(im, cx, cy, w, h, ang, fill, line=None):
    a = math.radians(ang)
    ca, sa = math.cos(a), math.sin(a)
    pts = [(cx + ca * x - sa * y, cy + sa * x + ca * y) for x, y in ((-w / 2, -h / 2), (w / 2, -h / 2), (w / 2, h / 2), (-w / 2, h / 2))]
    d = ImageDraw.Draw(im)
    d.polygon(pts, fill=fill, outline=INK)
    if line:
        d.line([(cx + ca * (-w / 2 + 1) - sa * 0, cy + sa * (-w / 2 + 1)), (cx + ca * (w / 2 - 1), cy + sa * (w / 2 - 1))], fill=line)


def piece(im, kind, cx, cy, ang, rnd):
    if kind == "can":
        c = rnd.choice(("#c9cdd4", "#7fc4ee", "#e8584a", "#8fd68a"))
        rrect(im, cx, cy, 8, 5, ang, hx(c), hx("#ffffff"))
        rrect(im, cx + math.cos(math.radians(ang)) * 4, cy + math.sin(math.radians(ang)) * 4, 1.5, 4, ang, hx("#8f9aa5"))
    elif kind == "core":
        rrect(im, cx, cy, 4, 7, ang, hx("#d93b3b"))
        rrect(im, cx, cy, 3, 3, ang, hx("#f2e6b0"))
    elif kind == "wrap":
        rrect(im, cx, cy, 9, 6, ang, hx(rnd.choice(("#e8453c", "#f2c230", "#4aa3e0"))), hx("#fff2a8"))
    elif kind == "box":
        rrect(im, cx, cy, 8, 7, ang, hx("#f4f0e6"), hx("#d93b3b"))
    elif kind == "bottle":
        rrect(im, cx, cy, 10, 4, ang, hx("#6fd0a0"), hx("#d8ffe8"))
        rrect(im, cx + math.cos(math.radians(ang)) * 6, cy + math.sin(math.radians(ang)) * 6, 3, 2, ang, hx("#2f6fd6"))
    elif kind == "bag":
        rrect(im, cx, cy, 8, 8, ang, hx("#eceaf4"), hx("#ffffff"))
    elif kind == "box2":
        rrect(im, cx, cy, 9, 6, ang, hx("#c99a5a"), hx("#e8c890"))


def takeout_meteor():
    N = 48
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    cx = cy = N / 2 - 0.5
    rnd = random.Random(11)

    def R(a):  # lumpy ring radius
        return 14 + 2.4 * math.sin(3 * a + 1.0) + 1.8 * math.sin(5 * a + 2.3) + 1.2 * math.sin(8 * a)

    # neon purple flames licking out past the lumps
    for deg in range(0, 360, 4):
        a = math.radians(deg)
        reach = R(a) + 4 + rnd.randint(0, 3) + (4 if deg % 20 == 0 else 0)
        for rr in range(int(R(a)) - 1, int(reach) + 1):
            t = (rr - R(a)) / max(1, reach - R(a))
            col = hx("#ff9bff") if t < 0.15 else hx("#c060ff") if t < 0.5 else hx("#7a2fd0") if t < 0.85 else hx("#4a1a8a")
            x = int(round(cx + math.cos(a) * rr)); y = int(round(cy + math.sin(a) * rr))
            px(im, x, y, col); px(im, x + 1, y, col)
    # compacted garbage mass (lumpy annulus)
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - cx, y - cy)
            if 6 <= r <= R(math.atan2(y - cy, x - cx)):
                px(im, x, y, hx("#4a4256") if (x * 7 + y * 3) % 4 else hx("#352f40"))
    # junk piled on, some poking out past the edge so the shape is ragged
    kinds = ["can", "wrap", "core", "bottle", "box", "can", "bag", "wrap", "core", "box2", "can", "bottle", "wrap", "core", "bag", "can", "box", "wrap"]
    for i, k in enumerate(kinds):
        a = math.radians(i * 360 / len(kinds) + rnd.uniform(-8, 8))
        rad = rnd.uniform(9, 17) if i % 2 else rnd.uniform(11, 15)
        piece(im, k, cx + math.cos(a) * rad, cy + math.sin(a) * rad, rnd.uniform(0, 180), rnd)
    # glowing purple core
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - cx, y - cy)
            if r < 7:
                col = hx("#ffffff") if r < 2 else hx("#f0c8ff") if r < 3.8 else hx("#c060ff") if r < 5.4 else hx("#7a2fd0")
                px(im, x, y, col)
            elif r < 7.8:
                px(im, x, y, INK)
    outline(im, INK)
    for p in ((3, 6), (44, 9), (5, 41), (42, 40), (24, 1), (1, 24), (46, 26), (10, 2), (38, 3)):
        px(im, p[0], p[1], hx("#e8a8ff"))
    return im


if __name__ == "__main__":
    save("stink_peel.png", stink_peel(), 2)
    save("takeout_meteor.png", takeout_meteor(), 3)
    print("ok")
