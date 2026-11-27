#!/usr/bin/env python3
"""Draws the five park villagers of the knight cutscene (npc/park_0..4.png), on the same 16x24 chibi grid as the level villagers
(npc_0..9) but with their own hair, hats, glasses and props so nobody in the park looks like a level villager.

Usage:  python3 tools/gen_park_villagers.py      (needs Pillow)
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_sprites import INK, hx, person_frame, save, shade   # noqa: E402


def pal(skin, hair, top, shorts, shoe, style):
    return dict(skin=hx(skin), hair=hx(hair), top=hx(top), shorts=hx(shorts), shoe=hx(shoe), pack=hx("#f4d8a0"), style=style, clip=hx(hair))


def dots(img, color, *pts):
    for x, y in pts:
        img.putpixel((x, y), color)


def jogger():
    """Ponytail, pink headband, teal tracksuit with a white stripe."""
    im = person_frame("down", 0, False, pal("#d9a87c", "#4a2c5a", "#3fb6b0", "#2e4a6a", "#ffffff", "bob"))
    hair, band = hx("#4a2c5a"), hx("#ff5f8f")
    dots(im, band, *[(x, 5) for x in range(3, 13)])
    dots(im, hair, (13, 5), (13, 6), (14, 7), (14, 8), (13, 9))
    dots(im, band, (13, 4))
    dots(im, hx("#ffffff"), (5, 12), (5, 13), (5, 14), (10, 12), (10, 13), (10, 14))
    return im


def grandpa():
    """Bald on top with white side hair, round glasses, a big white moustache, a brown cardigan and a cane."""
    im = person_frame("down", 0, False, pal("#f0d0b8", "#f2f2f6", "#a8683c", "#5a5a6a", "#7a5a3a", "short"))
    skin, white, frame, cane = hx("#f0d0b8"), hx("#f2f2f6"), hx("#d9b030"), hx("#7a4a2a")
    for x in range(4, 12):
        for y in (3, 4):
            im.putpixel((x, y), skin)
    dots(im, white, *[(x, y) for x in (3, 12) for y in (3, 4, 5, 6)])
    dots(im, frame, *[(x, y) for x in (5, 6, 7, 8, 9, 10) for y in (6,)], (5, 7), (7, 7), (8, 7), (10, 7))
    dots(im, white, (6, 9), (7, 9), (8, 9), (9, 9))
    dots(im, hx("#e8d0a0"), (7, 12), (7, 14), (8, 13), (8, 15))
    dots(im, cane, (14, 14), (15, 14), (15, 15), *[(15, y) for y in range(16, 22)])
    dots(im, hx("#f0d0b8"), (13, 14), (13, 15))
    return im


def kid():
    """Orange pigtails, a yellow raincoat, blue shorts and red boots."""
    im = person_frame("down", 0, False, pal("#e8c4a0", "#d9803a", "#f5d040", "#4a7ac8", "#d94a4a", "bob"))
    hair, bow = hx("#d9803a"), hx("#7ad0f0")
    dots(im, hair, (2, 6), (2, 7), (2, 8), (1, 8), (1, 9), (13, 6), (13, 7), (13, 8), (14, 8), (14, 9))
    dots(im, bow, (2, 5), (13, 5))
    dots(im, hx("#c8a020"), (7, 12), (8, 12), (7, 15), (8, 15))
    dots(im, hx("#ffffff"), (7, 13), (8, 14))
    return im


def dad():
    """Red beanie with a pompom, square glasses, a green plaid shirt and backpack straps."""
    im = person_frame("down", 0, False, pal("#c8956a", "#2a2230", "#3a8a5a", "#4a4a6a", "#e8e0d0", "short"))
    beanie, dark, frame = hx("#e8553a"), hx("#a8301e"), hx("#3a6ad0")
    for x in range(3, 13):
        for y in (1, 2, 3):
            im.putpixel((x, y), beanie)
    for x in range(3, 13, 2):
        im.putpixel((x, 3), dark)
    dots(im, hx("#f5f0e0"), (7, 0), (8, 0), (7, 1), (8, 1))
    dots(im, INK, (3, 3), (12, 3), (2, 2), (13, 2))
    dots(im, frame, *[(x, y) for x in (5, 6, 7, 8, 9, 10) for y in (6,)], (5, 7), (7, 7), (8, 7), (10, 7))
    plaid = hx("#2a6a44")
    dots(im, plaid, (6, 12), (8, 12), (6, 14), (9, 14), (7, 13), (9, 13))
    dots(im, hx("#3a2a24"), *[(6, y) for y in range(11, 16)], *[(9, y) for y in range(11, 16)])
    return im


def picnic_mum():
    """Wide cream sunhat with a pink ribbon, a floral sundress with a swishy skirt and a picnic basket."""
    im = person_frame("down", 0, False, pal("#e8c4a0", "#8a4a2a", "#f08aa8", "#f08aa8", "#c8a070", "bob"))
    cream, ribbon, dress, hem = hx("#f6e6c0"), hx("#ff6f9a"), hx("#f08aa8"), hx("#d0607e")
    for x in range(1, 15):
        im.putpixel((x, 2), cream)
    for x in range(3, 13):
        for y in (0, 1):
            im.putpixel((x, y), cream)
    dots(im, INK, *[(x, 3) for x in range(1, 15) if x < 3 or x > 12], (2, 2), (13, 2))
    dots(im, ribbon, *[(x, 1) for x in range(3, 13)])
    for x in range(4, 12):
        for y in (17, 18):
            im.putpixel((x, y), dress)
    dots(im, hem, *[(x, 19) for x in range(4, 12)], (6, 17), (9, 18), (7, 12), (8, 14))
    dots(im, hx("#8a5a32"), *[(x, y) for x in (0, 1, 2, 3) for y in (17, 18, 19)])
    dots(im, hx("#d9a86c"), (1, 16), (2, 16), (0, 17), (3, 17))
    return im


def main():
    for i, make in enumerate((jogger, grandpa, kid, dad, picnic_mum)):
        save("npc/park_%d.png" % i, make())
    print("wrote 5 park villagers")


if __name__ == "__main__":
    main()
