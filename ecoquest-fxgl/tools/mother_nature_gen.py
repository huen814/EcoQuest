import importlib.util, sys
from PIL import Image
spec=importlib.util.spec_from_file_location("g","/home/claude/eq/EcoQuest/ecoquest-fxgl/tools/gen_sprites.py"); g=importlib.util.module_from_spec(spec); spec.loader.exec_module(g)
hx, canvas, rect, INK, BLUSH = g.hx, g.canvas, g.rect, g.INK, g.BLUSH
def mother_nature_new():
    im, d = canvas(16, 24)
    skin = hx("#f6dcc4")
    hair, sh, hi, edge = hx("#efece0"), hx("#d3d0c2"), hx("#fdfcf6"), hx("#b9b5a8")
    moss, moss_d, leaf, leaf_l = hx("#4f8a52"), hx("#356a3c"), hx("#79b866"), hx("#a6d98a")
    bark, bark_d, cream = hx("#8a6a48"), hx("#6a4e32"), hx("#f3ead0")
    d.ellipse([2, 21, 13, 23], fill=(60, 40, 90, 55))
    d.polygon([(2, 5), (13, 5), (13, 14), (12, 20), (3, 20), (2, 14)], fill=hair, outline=edge)
    for x, y in ((3, 9), (3, 13), (3, 17), (12, 11), (12, 15), (12, 19), (4, 19), (11, 19)): d.point((x, y), fill=sh)
    rect(d, 6, 19, 7, 21, skin, None); rect(d, 9, 19, 10, 21, skin, None)
    rect(d, 5, 21, 7, 22, bark, INK); rect(d, 9, 21, 11, 22, bark, INK)
    d.polygon([(5, 14), (10, 14), (12, 19), (3, 19)], fill=moss, outline=moss_d)
    for x in range(3, 13, 2):
        d.polygon([(x, 16), (x + 2, 16), (x + 1, 19)], fill=leaf if (x // 2) % 2 else leaf_l, outline=moss_d)
    rect(d, 5, 11, 10, 14, moss, moss_d)
    d.line([(5, 14), (10, 14)], fill=bark); d.line([(5, 13), (10, 13)], fill=bark_d)
    d.point((7, 12), fill=cream); d.point((8, 12), fill=cream); d.point((7, 11), fill=cream); d.point((8, 11), fill=cream)
    for x0 in (3, 11):
        rect(d, x0, 11, x0 + 1, 14, cream, None); d.point((x0, 12), fill=leaf); d.point((x0 + 1, 13), fill=leaf)
        rect(d, x0, 15, x0 + 1, 15, skin, None)
    rect(d, 3, 2, 12, 10, skin, INK)
    rect(d, 3, 2, 4, 11, hair, edge); rect(d, 11, 2, 12, 11, hair, edge)
    rect(d, 4, 2, 11, 5, hair, None); d.line([(3, 2), (12, 2)], fill=edge)
    d.point((5, 3), fill=hi); d.point((6, 3), fill=hi); d.point((4, 7), fill=sh); d.point((11, 8), fill=sh)
    for (x0, x1, y) in ((5, 11, 5), (6, 11, 6), (8, 11, 7), (10, 11, 8)): rect(d, x0, y, x1, y, hair, None)
    d.point((9, 6), fill=hi); d.point((10, 7), fill=sh); d.line([(5, 5), (11, 5)], fill=sh)
    d.point((5, 7), fill=INK); d.point((6, 7), fill=INK); d.point((6, 8), fill=hx("#3f9a6a"))
    d.point((5, 8), fill=BLUSH); d.point((9, 9), fill=BLUSH); d.point((7, 9), fill=hx("#c0707f")); d.point((8, 9), fill=hx("#c0707f"))
    for x0, x1, hk in ((3, 5, 6), (10, 12, 9)):
        rect(d, x0, 11, x1, 15, hair, edge); d.point((x0 + 1, 12), fill=hi); d.point((x0 + 1, 14), fill=sh)
        d.point((hk, 15), fill=hair); d.point((hk, 16), fill=edge)
    return im
im = mother_nature_new()
g.OUT = "/home/claude/mn_out"; g.save("mother_nature.png", im)
a = Image.open("/home/claude/mn_out/mother_nature.png").convert("RGBA")
bg = Image.new("RGBA", (a.width*4, a.height*4), (200,220,200,255)); bg.alpha_composite(a.resize((a.width*4, a.height*4), Image.NEAREST)); bg.save("/home/claude/mn_new_preview.png")
