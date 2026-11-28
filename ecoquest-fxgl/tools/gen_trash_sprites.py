from PIL import Image, ImageDraw
import os, sys

OUT = sys.argv[1] if len(sys.argv) > 1 else "."
OUTLINE = (42, 27, 61, 255)
WHITE = (255, 255, 255, 255)

def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def body_canvas(n):
    return Image.new("RGBA", (n, n), (0, 0, 0, 0))

def poly(img, pts, col):
    ImageDraw.Draw(img).polygon(pts, fill=col)

def rect(img, x0, y0, x1, y1, col):
    ImageDraw.Draw(img).rectangle([x0, y0, x1, y1], fill=col)

def ell(img, box, col):
    ImageDraw.Draw(img).ellipse(box, fill=col)

def px(img, pts, col):
    for x, y in pts:
        if 0 <= x < img.width and 0 <= y < img.height:
            img.putpixel((x, y), col)

def solid(img):
    return {(x, y) for x in range(img.width) for y in range(img.height) if img.getpixel((x, y))[3] > 0}

def grow(cells, w, h):
    out = set(cells)
    for x, y in cells:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h:
                out.add((nx, ny))
    return out

def finish(body, aura_hex, sparkles, aura=True):
    w, h = body.size
    base = solid(body)
    ring0 = grow(base, w, h)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    if aura:
        ring1 = grow(ring0, w, h)
        ring2 = grow(ring1, w, h)
        for x, y in ring2 - ring1:
            if (x + y) % 2 == 0:
                img.putpixel((x, y), hexc(aura_hex, 90))
        for x, y in ring1 - ring0:
            img.putpixel((x, y), hexc(aura_hex, 170))
    for x, y in ring0 - base:
        img.putpixel((x, y), OUTLINE)
    img.alpha_composite(body)
    for x, y in sparkles:
        px(img, [(x, y)], WHITE)
        px(img, [(x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)], hexc("#f0d8ff", 200)) if False else None
    return img

def shade(img, light, mid, dark):
    w, h = img.size
    for x in range(w):
        for y in range(h):
            if img.getpixel((x, y))[3] > 0 and img.getpixel((x, y)) == hexc("#ff00ff"):
                t = (x + y) / (w + h - 2)
                img.putpixel((x, y), light if t < 0.42 else mid if t < 0.62 else dark)

def soda_can():
    n = 20
    b = body_canvas(n)
    poly(b, [(6, 4), (12, 3), (14, 5), (12, 8), (14, 11), (12, 15), (6, 16), (4, 12), (6, 9), (4, 6)], hexc("#ff00ff"))
    shade(b, hexc("#e4b4ff"), hexc("#b455ff"), hexc("#7a2fc0"))
    rect(b, 6, 3, 12, 4, hexc("#dfe3f0"))
    px(b, [(6, 4), (7, 4)], hexc("#9a9fb8"))
    rect(b, 6, 15, 12, 16, hexc("#9a9fb8"))
    px(b, [(7, 3), (8, 3)], hexc("#ffffff"))
    px(b, [(6, 9), (7, 8), (8, 9), (9, 8), (10, 9)], hexc("#5a1f9a"))
    px(b, [(7, 12), (8, 11), (9, 12)], hexc("#5a1f9a"))
    px(b, [(8, 6), (9, 6)], hexc("#f4dcff"))
    return finish(b, "#b455ff", [(2, 3), (17, 9), (3, 15), (16, 2)])

def wrapper():
    n = 20
    b = body_canvas(n)
    poly(b, [(3, 5), (7, 3), (11, 4), (16, 4), (17, 8), (15, 10), (16, 14), (11, 16), (7, 15), (4, 16), (3, 11), (4, 8)], hexc("#c8955a"))
    poly(b, [(4, 5), (7, 4), (10, 5), (9, 8), (5, 9)], hexc("#e8b878"))
    px(b, [(8, 9), (9, 10), (10, 11), (11, 12), (12, 13), (6, 12), (7, 13), (14, 7), (13, 6)], hexc("#8a5a2a"))
    poly(b, [(10, 8), (15, 8), (14, 11), (11, 10)], hexc("#f5d02a"))
    px(b, [(11, 9), (12, 9)], hexc("#fff08a"))
    poly(b, [(4, 11), (8, 10), (9, 13), (5, 14)], hexc("#7fd04a"))
    px(b, [(5, 12), (6, 11), (7, 12)], hexc("#b8f07a"))
    px(b, [(12, 5), (13, 5)], hexc("#e03a2a"))
    return finish(b, "#6a2fb0", [(2, 2), (18, 10), (2, 17), (17, 17)])

def apple_core():
    n = 20
    b = body_canvas(n)
    ell(b, (5, 3, 14, 8), hexc("#e04a5a"))
    ell(b, (5, 11, 14, 16), hexc("#e04a5a"))
    rect(b, 7, 7, 12, 12, hexc("#f6ebb8"))
    rect(b, 7, 9, 8, 10, (0, 0, 0, 0))
    px(b, [(7, 7), (8, 8), (7, 11), (8, 11)], hexc("#d8c888"))
    px(b, [(6, 4), (7, 4), (6, 5)], hexc("#ff9aa0"))
    px(b, [(12, 14), (13, 13), (11, 15)], hexc("#a82a45"))
    px(b, [(10, 9), (10, 10), (11, 10)], hexc("#3a1f1a"))
    px(b, [(9, 3), (9, 2), (10, 1)], hexc("#6a4a2a"))
    px(b, [(11, 2), (12, 2), (12, 3)], hexc("#7fe05a"))
    return finish(b, "#b455ff", [(2, 5), (17, 4), (3, 17), (17, 16)])

def shard():
    n = 12
    b = body_canvas(n)
    poly(b, [(1, 8), (9, 1), (10, 5), (4, 10)], hexc("#dfe3f0"))
    poly(b, [(4, 8), (9, 3), (9, 5), (5, 9)], hexc("#9a9fb8"))
    px(b, [(8, 2), (9, 2)], hexc("#ffffff"))
    return finish(b, "#b455ff", [], aura=False)

def soda_burst():
    n = 36
    b = body_canvas(n)
    c = 18
    ell(b, (11, 11, 25, 25), hexc("#b455ff"))
    spikes = [(18, 1, 21, 10, 15, 10), (34, 15, 26, 13, 26, 21), (30, 32, 23, 25, 28, 23),
              (6, 31, 11, 24, 14, 28), (2, 12, 10, 14, 11, 19), (8, 4, 13, 11, 16, 9)]
    for x0, y0, x1, y1, x2, y2 in spikes:
        poly(b, [(x0, y0), (x1, y1), (x2, y2)], hexc("#b455ff"))
    ell(b, (13, 13, 20, 20), hexc("#e4b4ff"))
    px(b, [(15, 15), (16, 15), (15, 16)], WHITE)
    for x in range(n):
        for y in range(n):
            if b.getpixel((x, y)) == hexc("#b455ff") and x + y > 40:
                b.putpixel((x, y), hexc("#7a2fc0"))
    for pts in ([(4, 5), (7, 2), (7, 6)], [(31, 5), (34, 8), (29, 9)], [(30, 28), (34, 25), (33, 30)], [(2, 22), (5, 19), (6, 24)]):
        poly(b, pts, hexc("#dfe3f0"))
        px(b, [pts[0]], WHITE)
    for x, y in [(20, 4), (30, 20), (14, 33), (4, 15), (25, 6), (9, 28)]:
        ell(b, (x - 1, y - 1, x, y), hexc("#e4b4ff"))
    return finish(b, "#b455ff", [(10, 2), (33, 12), (22, 34), (3, 27)], aura=False)

os.makedirs(OUT, exist_ok=True)
for name, fn in [("trash_can", soda_can), ("trash_wrapper", wrapper), ("trash_core", apple_core), ("trash_shard", shard), ("soda_burst", soda_burst)]:
    fn().save(os.path.join(OUT, name + ".png"))
print("done")
