#!/usr/bin/env python3
"""Draws the seed and the wall of living wood of the knight chase (world/seed.png, seed_glow.png, wood_trunk_0..6.png,
wood_crown_0..6.png). 3 px per art pixel, like the rest of the game's art.

Usage:  python3 tools/gen_wood.py      (needs Pillow)
"""
import math
import os
import random
from PIL import Image

PS = 3
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "textures", "world")


def hx(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def save(name, im):
    os.makedirs(OUT, exist_ok=True)
    im.resize((im.width * PS, im.height * PS), Image.NEAREST).save(os.path.join(OUT, name))


BARK_OUT, BARK = hx("#2b1a10"), [hx("#c8955f"), hx("#a8774a"), hx("#8a5a36"), hx("#6b4429"), hx("#4a2e1c")]    # highlight .. deep
MOSS = [hx("#7cc47a"), hx("#4fa35a")]
LEAF_OUT, LEAF = hx("#1f6b45"), [hx("#c8ffc0"), hx("#8fe59a"), hx("#5fcf7a"), hx("#3fae62"), hx("#2f8f55")]    # highlight .. deep


def trunk(rows, seed, tw=17):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (tw, rows))
    c, phase = tw // 2, rnd.random() * 6
    groove_cols = [3, 7, tw - 5]
    groove = {g: [] for g in groove_cols}
    for g in groove_cols:                                   # dashed vertical cracks: (start, length)
        y = rnd.randint(2, 8)
        while y < rows - 6:
            n = rnd.randint(4, 9)
            groove[g].append((y, n))
            y += n + rnd.randint(2, 5)
    knots = [(rnd.randint(3, 11), rnd.randint(rows // 4, rows * 2 // 3)) for _ in range(2)]
    for y in range(rows):
        fl = max(0, y - (rows - 9))
        half = 4.2 + 0.8 * y / rows + (fl / 8.0) ** 2 * 3.2
        shift = 0 if fl else round(1.2 * math.sin(y * 0.18 + phase))
        left, right = int(round(c - half + shift)), int(round(c + half - 1 + shift))
        left, right = max(0, left), min(tw - 1, right)
        for x in range(left, right + 1):
            u = (x - left) / max(1, right - left)
            if y == 0 or y == rows - 1 or x in (left, right):
                col = BARK_OUT
            else:
                band = 1 if u < 0.28 else 2 if u < 0.55 else 3 if u < 0.8 else 4
                if band == 1 and u > 0.22 and (x + y) % 2 == 0:
                    band = 2                                # dithered edge between light and base
                if u < 0.12:
                    band = 0                                # rim light
                col = BARK[band]
            for g, segs in groove.items():
                gx = left + g if g < tw // 2 else right - (tw - 1 - g)
                if x == gx and col != BARK_OUT and any(s <= y < s + n for s, n in segs) and fl == 0:
                    col = BARK[4] if x > c else BARK[3]
            im.putpixel((x, y), col)
        if fl > 3 and left + 3 < c:                         # the roots split in two at the foot
            for x in range(c - 1, c + 1):
                im.putpixel((x, y), BARK[4])
    for kx, ky in knots:                                    # knots: dark ring, lighter heart
        for dx, dy in [(-1, -2), (0, -2), (1, -2), (-2, -1), (2, -1), (-2, 0), (2, 0), (-1, 1), (0, 1), (1, 1)]:
            x, y = kx + dx - 1, ky + dy
            if 0 < x < tw - 1 and 0 < y < rows - 2 and im.getpixel((x, y))[:3] != BARK_OUT[:3]:
                im.putpixel((x, y), BARK[4])
        for dx, dy in [(-1, -1), (0, -1), (-1, 0), (0, 0)]:
            x, y = kx + dx - 1, ky + dy
            if 0 < x < tw - 1 and 0 < y < rows - 2 and im.getpixel((x, y))[:3] != BARK_OUT[:3]:
                im.putpixel((x, y), BARK[1])
    for y in range(rows - 16, rows - 1):                    # moss creeping up from the roots
        p = (y - (rows - 16)) / 15.0
        for x in range(tw):
            px = im.getpixel((x, y))
            if px[3] and px[:3] != BARK_OUT[:3] and rnd.random() < 0.55 * p * p and (x < c - 1 or x > c + 2 or rnd.random() < 0.3):
                im.putpixel((x, y), MOSS[0] if rnd.random() < 0.4 else MOSS[1])
    for x in (0, tw - 1):                                   # tiny root tips
        if x == 0 and im.getpixel((1, rows - 2))[3]:
            im.putpixel((0, rows - 1), BARK_OUT)
    return im


def crown(seed, w=34, h=26):
    rnd = random.Random(seed)
    blobs = [[17, 12, 9], [8, 15, 7], [26, 15, 7], [17, 18, 8], [11, 8, 6], [23, 8, 6], [17, 6, 5]]
    for b in blobs:
        b[0] += rnd.randint(-1, 1)
        b[1] += rnd.randint(-1, 1)
    owner = [[-1] * h for _ in range(w)]
    depth = [[0.0] * h for _ in range(w)]
    for x in range(w):
        for y in range(h):
            best, bi = 0.0, -1
            for i, (bx, by, r) in enumerate(blobs):
                d = r - math.hypot(x - bx, y - by)
                if d > best:
                    best, bi = d, i
            owner[x][y], depth[x][y] = bi, best
    tone = [[None] * h for _ in range(w)]
    for x in range(w):
        for y in range(h):
            i = owner[x][y]
            if i < 0:
                continue
            bx, by, r = blobs[i]
            s = ((x - bx) * 0.7 + (y - by) * 0.9) / r
            t = 0 if s < -0.62 else 1 if s < -0.2 else 2 if s < 0.25 else 3 if s < 0.6 else 4
            if depth[x][y] < 1.3 and i != 0:                # seam creases between the clumps
                t = min(4, t + 1)
            tone[x][y] = t
    for _ in range(46):                                     # leaf flecks: a light pixel with a dark one under it
        x, y = rnd.randint(2, w - 3), rnd.randint(2, h - 3)
        if tone[x][y] is not None and tone[x + 1][y + 1] is not None and 1 <= tone[x][y] <= 3:
            tone[x][y] = max(0, tone[x][y] - 1)
            tone[x + 1][y + 1] = min(4, tone[x + 1][y + 1] + 1)
    im = Image.new("RGBA", (w, h))
    for x in range(w):
        for y in range(h):
            if tone[x][y] is None:
                continue
            edge = x in (0, w - 1) or y in (0, h - 1) or any(tone[nx][ny] is None for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)))
            im.putpixel((x, y), LEAF_OUT if edge else LEAF[tone[x][y]])
    for _ in range(5):                                      # a few blossoms, to match the park's pastel look
        x, y = rnd.randint(4, w - 5), rnd.randint(4, h - 6)
        if all(tone[x + dx][y + dy] is not None and 0 <= tone[x + dx][y + dy] <= 3 for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1))):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                im.putpixel((x + dx, y + dy), hx("#ff9fc4"))
            im.putpixel((x, y), hx("#fff2a0"))
    return im


SEED = [
    "....ll......gg..",
    "..lllll...ggggg.",
    ".lvlllll.gggvgg.",
    "..lllllllggggg..",
    "...llllsgggg....",
    ".....ssss.......",
    "......s.........",
    "....ooooooo.....",
    "...oBBBBBBBoo...",
    "..oBhhwBBBBBBo..",
    ".oBhwwBBBBBBMMo.",
    ".oBhwBBBBBBBMMo.",
    ".oBBBBBBBBBMMDo.",
    ".oBBBGBBBBMMDDo.",
    ".oBBBBGBBMMDDDo.",
    "..oBBBBGMMDDDo..",
    "..oMBBBBGDDDDo..",
    "...oMMMMMDDDo...",
    "....ooMMDDoo....",
    "......ooooR.....",
]
SEED_COLORS = {"o": hx("#3a2212"), "B": hx("#8a5a32"), "h": hx("#d9a86c"), "w": hx("#fff0d0"), "M": hx("#6e4526"), "D": hx("#523318"),
               "G": hx("#5a3a1c"), "l": hx("#6fd08a"), "g": hx("#4fbf6e"), "v": hx("#b8f5c4"), "s": hx("#3a8a50"), "R": hx("#8a5a32")}


def seed():
    im = Image.new("RGBA", (len(SEED[0]), len(SEED)))
    for y, row in enumerate(SEED):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), SEED_COLORS[ch])
    return im


def glow(radius=23, color=hx("#9fffc0")):
    im = Image.new("RGBA", (radius * 2, radius * 2))
    alpha = [0.7, 0.45, 0.25, 0.1]
    for y in range(radius * 2):
        for x in range(radius * 2):
            r = math.hypot(x + 0.5 - radius, y + 0.5 - radius) / radius
            if r < 1:
                im.putpixel((x, y), color[:3] + (int(255 * alpha[int(r * 4)]),))
    return im


def main():
    save("seed.png", seed())
    save("seed_glow.png", glow())
    for i, rows in enumerate((54, 60, 52, 63, 57, 50, 61)):
        save("wood_trunk_%d.png" % i, trunk(rows, i * 7 + 1))
        save("wood_crown_%d.png" % i, crown(i + 5))
    print("wrote the seed and 7 trunks with crowns")


if __name__ == "__main__":
    main()
