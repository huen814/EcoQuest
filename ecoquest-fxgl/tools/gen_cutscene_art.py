#!/usr/bin/env python3
"""Pixel art for the Queen's Knight cutscene: the park backdrop and the flower field backdrop.

Usage:  python3 tools/gen_cutscene_art.py <textures folder>      (needs Pillow and numpy)
Writes backdrop/park_plaza.png and backdrop/field.png.
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw

OUT = sys.argv[1] if len(sys.argv) > 1 else "."


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


# ---------------------------------------------------------------- trees
LEAF = hexc("#6fd3d2"); LEAF_LIGHT = hexc("#a9ece6"); LEAF_SHADE = hexc("#49b4bf"); LEAF_DARK = hexc("#2f8fa0")
TRUNK = hexc("#8a6a9c"); TRUNK_DARK = hexc("#5f4478"); TRUNK_LIGHT = hexc("#a98cbb")


def tree(kind, seed, k=1.0):
    """A tree sprite (64x80 at k=1): round canopy built from blobs, shaded underside, trunk with roots, soft ground shadow."""
    rnd = random.Random(seed)
    W, H = int(64 * k), int(80 * k)
    blobs = {
        "a": [(32, 28, 22), (18, 34, 14), (46, 34, 14), (32, 16, 14)],
        "b": [(32, 30, 17), (32, 17, 14), (32, 42, 12), (22, 26, 11), (42, 26, 11)],
        "c": [(22, 30, 15), (42, 30, 15), (32, 20, 16), (32, 38, 13), (12, 40, 8), (52, 40, 8)],
    }[kind]
    blobs = [(cx * k, cy * k, r * k) for cx, cy, r in blobs]
    m = np.zeros((H, W), bool)
    yy, xx = np.mgrid[0:H, 0:W]
    for cx, cy, r in blobs:
        m |= (xx - cx) ** 2 + (yy - cy) ** 2 <= r * r
    shade = np.zeros((H, W), bool)
    for cx, cy, r in blobs:
        shade |= ((xx - cx - 4 * k) ** 2 + (yy - cy - 5 * k) ** 2 <= r * r) & ((xx - cx) ** 2 + (yy - cy) ** 2 > (r - 5 * k) ** 2) & (yy > cy + 2 * k)
    shade &= m
    light = np.zeros((H, W), bool)
    for cx, cy, r in blobs:
        light |= ((xx - cx + 5 * k) ** 2 + (yy - cy + 6 * k) ** 2 <= (r * 0.45) ** 2) & m
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    d = ImageDraw.Draw(img)
    S = lambda v: int(round(v * k))
    d.ellipse((S(12), S(70), S(52), H - 1), fill=(60, 40, 90, 70))
    d.polygon([(S(27), H - 1), (S(29), S(38)), (S(35), S(38)), (S(37), H - 1)], fill=TRUNK)
    d.rectangle((S(29), S(38), S(35), S(66)), fill=TRUNK)
    d.rectangle((S(33), S(38), S(35), S(68)), fill=TRUNK_DARK)
    d.line((S(29), S(38), S(29), S(68)), fill=TRUNK_LIGHT)
    d.polygon([(S(24), H - 1), (S(28), S(72)), (S(30), H - 1)], fill=TRUNK)
    d.polygon([(S(40), H - 1), (S(36), S(72)), (S(34), H - 1)], fill=TRUNK_DARK)
    for y in range(H):
        for x in range(W):
            if m[y, x]:
                c = LEAF
                if shade[y, x]: c = LEAF_SHADE
                if light[y, x]: c = LEAF_LIGHT
                px[x, y] = c
    for y in range(H):
        for x in range(W):
            if m[y, x] and not all(0 <= y + dy < H and 0 <= x + dx < W and m[y + dy, x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = LEAF_DARK
    for _ in range(int(26 * k * k)):
        x, y = rnd.randrange(int(8 * k), int(56 * k)), rnd.randrange(int(8 * k), int(52 * k))
        if m[y, x] and px[x, y] == LEAF:
            px[x, y] = LEAF_SHADE if rnd.random() < 0.6 else LEAF_LIGHT
    return img


# ---------------------------------------------------------------- park backdrop
def park_plaza():
    """720x480 park backdrop drawn at 360x240 and scaled 2x: a fountain plaza, winding paved footpaths, trees, benches, lamps."""
    rnd = random.Random(5)
    W, H = 360, 240
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)

    for y in range(H):
        t = min(1.0, y / 60.0)
        d.line((0, y, W, y), fill=(int(196 + 36 * t), int(222 + 20 * t), int(248 + 2 * t), 255))
    d.ellipse((238, 3, 262, 27), fill=(255, 244, 214, 255)); d.ellipse((241, 6, 259, 24), fill=(255, 238, 190, 255))

    def cloud(cx, cy, s):
        for dx, dy, rx, ry in ((0, 0, 16, 6), (-10, 2, 10, 5), (10, 2, 11, 5), (2, -4, 9, 5)):
            d.ellipse((cx + dx * s - rx * s, cy + dy * s - ry * s, cx + dx * s + rx * s, cy + dy * s + ry * s), fill=(250, 252, 255, 255))
        d.ellipse((cx - 14 * s, cy + 3 * s, cx + 14 * s, cy + 8 * s), fill=(224, 233, 247, 255))
    cloud(50, 16, 1.0); cloud(150, 12, 0.8); cloud(318, 20, 1.0)

    x = 0
    while x < W:
        bw = rnd.randrange(10, 22); bh = rnd.randrange(14, 34)
        d.rectangle((x, 74 - bh, x + bw, 76), fill=(176, 168, 208, 255))
        d.rectangle((x, 74 - bh, x + bw, 74 - bh), fill=(196, 188, 224, 255))
        for wy in range(76 - bh + 3, 72, 5):
            for wx in range(x + 2, x + bw - 1, 4):
                if rnd.random() < 0.45: d.point((wx, wy), fill=(255, 238, 160, 255))
        x += bw + rnd.randrange(0, 3)

    lawn = np.zeros((H, W, 3), float)
    for y in range(H):
        t = max(0.0, (y - 70) / 170.0)
        lawn[y, :] = np.array((190, 228, 202)) * (1 - t) + np.array((204, 236, 207)) * t
    for y in range(70, H):
        for xx_ in range(W):
            n = rnd.random()
            if (y // 6) % 2 == 0: lawn[y, xx_] += 3
            if n < 0.045: lawn[y, xx_] = (172, 212, 184)
            elif n < 0.06: lawn[y, xx_] = (224, 246, 226)
    arr = np.array(img.convert("RGB")).astype(float)
    arr[70:, :] = lawn[70:, :]
    img = Image.fromarray(np.clip(arr, 0, 255).astype("uint8"), "RGB").convert("RGBA")
    d = ImageDraw.Draw(img)

    mask = Image.new("L", (W, H), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse((180 - 58, 126 - 20, 180 + 58, 126 + 20), fill=255)
    md.rectangle((166, 130, 194, 160), fill=255)
    pts_top, pts_bot = [], []
    for x in range(-2, W + 3):
        c = 154 + 5 * math.sin(x * 0.02 + 0.6)
        pts_top.append((x, c - 15)); pts_bot.append((x, c + 15))
    md.polygon(pts_top + pts_bot[::-1], fill=255)
    m = np.array(mask) > 0
    edge = np.zeros_like(m)
    edge[1:-1, 1:-1] = m[1:-1, 1:-1] & ~(m[:-2, 1:-1] & m[2:, 1:-1] & m[1:-1, :-2] & m[1:-1, 2:])
    stones = [(232, 224, 238), (222, 214, 232), (238, 230, 244)]
    for y in range(H):
        for x in range(W):
            if not m[y, x]: continue
            r = y // 5
            off = (r % 2) * 5
            cidx = (x + off) // 10
            if y % 5 == 0 or (x + off) % 10 == 0:
                col = (196, 186, 214)
            else:
                col = stones[(cidx * 7 + r * 3) % 3]
            if edge[y, x]: col = (168, 158, 192)
            img.putpixel((x, y), col + (255,))
    d = ImageDraw.Draw(img)
    for rx_, ry_ in ((46, 15), (34, 11)):
        d.ellipse((180 - rx_, 126 - ry_, 180 + rx_, 126 + ry_), outline=(200, 190, 216, 255))

    def blanket(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                img.putpixel((x, y), ((232, 110, 124) if ((x // 4) + (y // 3)) % 2 else (252, 244, 240)) + (255,))
        d.rectangle((x0, y1, x1, y1), fill=(150, 70, 84, 255))
        d.rectangle((x0 + 7, y0 + 2, x0 + 14, y0 + 6), fill=(176, 120, 80, 255))
    blanket(48, 117, 80, 129)

    flower_cols = [(250, 180, 210), (255, 238, 150), (255, 255, 255), (208, 170, 255), (255, 190, 140)]
    for _ in range(170):
        x, y = rnd.randrange(0, W), rnd.randrange(80, 240)
        if 0 <= y < H and m[y, x]: continue
        c = flower_cols[rnd.randrange(len(flower_cols))] + (255,)
        d.point((x, y), fill=c)
        if rnd.random() < 0.5:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)): d.point((x + dx, y + dy), fill=c)
        d.point((x, y), fill=(255, 226, 120, 255))

    def fountain():
        cx, by = 180, 131
        def el(cx_, cy_, rx, ry, col): d.ellipse((cx_ - rx, cy_ - ry, cx_ + rx, cy_ + ry), fill=col)
        el(cx, by, 41, 12, (142, 135, 170, 255)); el(cx, by - 1, 37, 10, (184, 176, 200, 255)); el(cx, by - 2, 33, 8, (127, 216, 232, 255))
        for i in range(10):
            d.point((cx - 24 + i * 5 + rnd.randrange(3), by - 3 + rnd.randrange(-2, 3)), fill=(200, 244, 252, 255))
        d.rectangle((cx - 4, by - 24, cx + 4, by - 2), fill=(184, 176, 200, 255)); d.rectangle((cx + 2, by - 24, cx + 4, by - 2), fill=(142, 135, 170, 255))
        el(cx, by - 24, 18, 5, (142, 135, 170, 255)); el(cx, by - 25, 15, 3, (159, 232, 245, 255))
        d.rectangle((cx - 1, by - 38, cx + 1, by - 25), fill=(216, 246, 255, 255))
        for s_ in (-1, 1):
            for i in range(1, 12):
                px_ = cx + s_ * i * 1.7; py_ = by - 38 + 0.09 * (i * 1.7) ** 2 * 0.9 + 0 * i
                d.point((int(px_), int(py_)), fill=(230, 250, 255, 255))
        d.point((cx, by - 40), fill=(255, 255, 255, 255))

    def bench(cx, by):
        d.rectangle((cx - 11, by - 10, cx + 11, by - 7), fill=(142, 96, 112, 255)); d.rectangle((cx - 11, by - 10, cx + 11, by - 10), fill=(176, 128, 140, 255))
        d.rectangle((cx - 11, by - 5, cx + 11, by - 2), fill=(166, 112, 128, 255)); d.rectangle((cx - 11, by - 5, cx + 11, by - 5), fill=(196, 144, 156, 255))
        d.rectangle((cx - 10, by - 2, cx - 8, by), fill=(90, 68, 120, 255)); d.rectangle((cx + 8, by - 2, cx + 10, by), fill=(90, 68, 120, 255))
        d.ellipse((cx - 13, by - 1, cx + 13, by + 2), fill=(150, 170, 156, 120))

    def lamp(cx, by):
        d.rectangle((cx - 1, by - 36, cx, by), fill=(78, 70, 110, 255)); d.rectangle((cx - 3, by - 3, cx + 2, by), fill=(60, 54, 92, 255))
        d.rectangle((cx - 4, by - 42, cx + 3, by - 36), fill=(255, 244, 190, 255)); d.rectangle((cx - 5, by - 43, cx + 4, by - 43), fill=(78, 70, 110, 255))
        d.ellipse((cx - 9, by - 46, cx + 8, by - 32), outline=(255, 244, 190, 90))

    def bush(cx, by, r):
        d.ellipse((cx - r - 1, by - 2 * r - 1, cx + r + 1, by + 1), fill=LEAF_DARK)
        d.ellipse((cx - r, by - 2 * r, cx + r, by), fill=LEAF)
        d.ellipse((cx - r, by - r - 2, cx + r, by), fill=LEAF_SHADE)
        d.ellipse((cx - r + 2, by - 2 * r + 2, cx - 1, by - r), fill=LEAF_LIGHT)

    objs = []
    for i, x0 in enumerate(range(4, W + 20, 24)):
        objs.append((78 + rnd.randrange(0, 6), "tree", (x0 + rnd.randrange(-4, 5), "abc"[i % 3], 100 + i, 0.6)))
    for x0 in range(-2, W + 10, 10):
        objs.append((92 + rnd.randrange(-2, 3), "bush", (x0 + rnd.randrange(-2, 3), 5 + rnd.randrange(0, 3))))
    for x0, by, kind, sd in ((20, 122, "a", 1), (74, 113, "c", 2), (128, 107, "b", 3), (318, 108, "c", 4), (346, 119, "a", 5)):
        objs.append((by, "tree", (x0, kind, sd, 0.85)))
    for x0, by, kind, sd in ((16, 188, "a", 6), (346, 190, "c", 7)):
        objs.append((by, "tree", (x0, kind, sd, 1.1)))
    objs.append((131, "fountain", None))
    objs.append((134, "bench", 100))
    objs.append((141, "lamp", 124)); objs.append((141, "lamp", 304))
    objs.sort(key=lambda o: o[0])
    for by, kind, arg in objs:
        if kind == "tree":
            cx, tk, sd, k = arg
            t = tree(tk, sd, k)
            img.alpha_composite(t, (int(cx - t.width / 2), int(by - t.height)))
        elif kind == "bush": bush(arg[0], by, arg[1])
        elif kind == "fountain": fountain()
        elif kind == "bench": bench(arg, by)
        elif kind == "lamp": lamp(arg, by)
    return img.resize((720, 480), Image.NEAREST)


# ---------------------------------------------------------------- flower field
def field():
    """720x480 backdrop drawn at 180x120 and scaled 4x: a mown path through a flowering meadow, treeline behind."""
    rnd = random.Random(11)
    W, H = 180, 120
    HOR = 24
    img = np.zeros((H, W, 3), float)

    def mix(a, b, t):
        return np.array(a) * (1 - t) + np.array(b) * t

    sky_top, sky_low = (206, 226, 238), (246, 236, 204)
    for r in range(HOR):
        img[r, :] = mix(sky_top, sky_low, r / HOR)

    def noise(x, y, s):
        return (math.sin(x * 0.11 * s + y * 0.07) + math.sin(x * 0.05 * s - y * 0.13 + 1.3) + math.sin(x * 0.19 * s + y * 0.17 + 2.1)) / 3

    for c in range(W):
        top = 5 + 5 * noise(c, 0, 1.0) + 2 * math.sin(c * 0.6)
        for r in range(int(top), HOR + 2):
            t = (r - top) / max(1, HOR - top)
            img[r, c] = mix((44, 84, 58), (78, 122, 62), min(1, t))
    for tx, rr in ((6, 8), (20, 6), (34, 9), (50, 7), (66, 6), (84, 8), (100, 7), (116, 9), (134, 6), (150, 8), (166, 7), (178, 6)):
        cy = HOR - rr + 3
        for r in range(cy - rr, cy + rr + 1):
            for c in range(tx - rr, tx + rr + 1):
                if 0 <= c < W and 0 <= r < H and (c - tx) ** 2 + (r - cy) ** 2 <= rr * rr:
                    lit = 0.25 + 0.5 * ((tx - c) / (2.0 * rr) + 0.5) * ((cy - r) / (2.0 * rr) + 0.5)
                    img[r, c] = mix((34, 70, 50), (104, 150, 72), min(1, lit + 0.15 * rnd.random()))
    for bx, by, ph in ((132, 0, 1), (160, 0, 2)):
        for r in range(by, 15):
            img[r, bx + (r // 4) % 2] = (74, 54, 44)
        for k in range(26):
            a = rnd.random() * 6.28; rr = rnd.random() * 9
            cx, cy = int(bx + math.cos(a) * rr * 1.6), int(4 + math.sin(a) * rr * 0.6)
            if 0 <= cx < W and 0 <= cy < H:
                img[cy, cx] = (92, 130, 64)

    def center(r): return 98 - (r - HOR) * 0.95 + 5 * math.sin((r - HOR) * 0.09)
    def width(r): return 2.5 + (r - HOR) * 0.5

    for r in range(HOR, H):
        d = (r - HOR) / (H - HOR)
        for c in range(W):
            n = noise(c, r, 2.4)
            base = mix((170, 186, 72), (74, 138, 46), min(1, d * 1.15)) * (0.93 + 0.08 * n)
            base = base * (0.95 + 0.1 * rnd.random())
            img[r, c] = base
    for r in range(HOR, H):
        d = (r - HOR) / (H - HOR)
        cc, ww = center(r), width(r)
        for c in range(W):
            x = (c - cc) / ww
            if abs(x) < 1.0:
                stripe = 0.96 + 0.05 * math.sin(c * 1.7 + r * 0.2)
                g = mix((150, 200, 70), (96, 178, 52), min(1, d * 1.2)) * stripe
                edge = max(0.0, abs(x) - 0.82) / 0.18
                if x > 0:
                    g = g * (1 - 0.5 * edge) + np.array((34, 80, 34)) * 0.5 * edge
                else:
                    g = g * (1 - 0.18 * edge) + np.array((70, 130, 44)) * 0.18 * edge
                if x > 0.55:
                    g = g * (1 - 0.38 * (x - 0.55) / 0.45 * (0.4 + d))
                img[r, c] = g
            elif x > 1.0 and x < 1.35:
                img[r, c] = img[r, c] * (0.78 - 0.12 * (x - 1.0))
    yel, yel2 = (240, 206, 58), (252, 232, 120)
    pinks = [(196, 106, 190), (222, 140, 214), (160, 92, 186), (236, 168, 226)]
    for r in range(HOR + 1, H):
        d = (r - HOR) / (H - HOR)
        cc, ww = center(r), width(r)
        for c in range(W):
            x = (c - cc) / ww
            if abs(x) < 1.08:
                continue
            dens = 0.10 + 0.55 * d
            side = 1.0 if x > 0 else 0.62
            dens *= side * (0.7 + 0.5 * (noise(c, r, 1.6) + 1) / 2)
            if rnd.random() < dens:
                size = 1 if d < 0.35 else (2 if rnd.random() < 0.55 else 1)
                pn = noise(c * 1.3, r * 1.1, 1.1)
                if pn > 0.15:
                    col = pinks[rnd.randrange(len(pinks))]
                elif rnd.random() < 0.12:
                    col = (250, 246, 236)
                else:
                    col = yel if rnd.random() < 0.65 else yel2
                for dy in range(size):
                    for dx in range(size):
                        if 0 <= r + dy < H and 0 <= c + dx < W:
                            img[r + dy, c + dx] = col
    for r in range(HOR, H):
        d = (r - HOR) / (H - HOR)
        for c in range(W):
            img[r, c] = img[r, c] * (1 - 0.14 * d * (c / W)) + np.array((255, 232, 150)) * 0.05 * (1 - d)
    out = Image.fromarray(np.clip(img, 0, 255).astype("uint8"), "RGB").convert("RGBA")
    return out.resize((720, 480), Image.NEAREST)


if __name__ == "__main__":
    os.makedirs(os.path.join(OUT, "backdrop"), exist_ok=True)
    park_plaza().save(os.path.join(OUT, "backdrop", "park_plaza.png"))
    field().save(os.path.join(OUT, "backdrop", "field.png"))
    print("done")
