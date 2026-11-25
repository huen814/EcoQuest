#!/usr/bin/env python3
"""Draws the Discarded Knight (Boss 2) for the chase: three 128x128 poses (normal, rage, stun) on a transparent background, then
scaled to 96x96 so the sprite is shown 1:1 in the game (he stands about 83 px tall: the Guardian is 72).

Usage:  python3 tools/gen_knight.py          (needs Pillow; gen_sprites.py also calls it)
Output: src/main/resources/assets/textures/boss/knight.png, knight_rage.png, knight_stun.png
"""
import os
import math
from PIL import Image, ImageDraw
def hx(s, a=255):
    s = s.lstrip("#"); return (int(s[0:2],16), int(s[2:4],16), int(s[4:6],16), a)
INK = hx("#1a1226")
skin, skin_sh = hx("#a4dcee"), hx("#6fb0c8")
hair, hair_hi, hair_dk = hx("#d0289c"), hx("#ff6ac8"), hx("#8a1470")
crown, crown_hi, pink = hx("#7a1068"), hx("#b02a96"), hx("#ff2e93")
jacket, jacket_hi, jacket_sh = hx("#2a2c44"), hx("#40446a"), hx("#1e2034")
purple, purple_hi, purple_dk = hx("#8a4ad0"), hx("#c0a0f8"), hx("#5a2a9a")
cape, cape_sh, lining = hx("#c0309a"), hx("#9a2478"), hx("#5a1a7a")
white, glove_sh, black = hx("#f4f2f8"), hx("#c4c2d4"), hx("#0c0a12")
trousers, boot = hx("#1e2238"), hx("#16141c")
metal, metal_hi = hx("#3a3e4a"), hx("#6a7084")
TEAL = hx("#2a6a80")
SZ = 128
im = d = None
def layer():
    global im, d
    im = Image.new("RGBA", (SZ,SZ), (0,0,0,0)); d = ImageDraw.Draw(im); return im
def P(pts, fill, out=INK): d.polygon(pts, fill=fill, outline=out)
def E(box, fill, out=INK): d.ellipse(box, fill=fill, outline=out)
def R(box, fill, out=INK): d.rectangle(box, fill=fill, outline=out)
def chk(x, y): return white if (x//3 + y//3) % 2 == 0 else black
def checker(pts):
    m = Image.new("L", im.size, 0); ImageDraw.Draw(m).polygon(pts, fill=255)
    xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
    for y in range(min(ys), max(ys)+1):
        for x in range(min(xs), max(xs)+1):
            if m.getpixel((x,y)): im.putpixel((x,y), chk(x,y))
    d.polygon(pts, outline=INK)
def arm(pts):
    d.line(pts, fill=INK, width=10, joint="curve"); d.line(pts, fill=jacket, width=8, joint="curve")
    d.line([(pts[0][0]-2,pts[0][1]+1),(pts[1][0]-2,pts[1][1])], fill=jacket_hi)
    d.line([(pts[1][0]+2,pts[1][1]),(pts[2][0]+2,pts[2][1]-2)], fill=jacket_sh)
    x1, y1 = pts[2]; R([x1-4,y1-2,x1+4,y1+1], purple)               # cuff
def glove(cx, cy):
    E([cx-4,cy-4,cx+5,cy+5], white); d.arc([cx-3,cy-3,cx+4,cy+4], 30, 150, fill=glove_sh)
def lantern(dy=0):                                   # dark metal lantern, tiny skull inside (hand at (46, 82+dy))
    LY = 3 + dy
    d.line([(46,82+dy),(46,84+LY)], fill=INK, width=2)
    R([40,84+LY,52,87+LY], metal); R([41,87+LY,51,101+LY], hx("#4fd8f0")); R([42,88+LY,50,100+LY], hx("#9aeeff"))
    for x in (41,51): d.line([(x,87+LY),(x,101+LY)], fill=INK); d.line([(x,88+LY),(x,100+LY)], fill=metal)
    R([40,101+LY,52,104+LY], metal); d.line([(41,85+LY),(51,85+LY)], fill=metal_hi)
    E([43,89+LY,49,95+LY], white, TEAL); R([44,94+LY,48,98+LY], white, TEAL)
    R([44,91+LY,45,92+LY], INK, None); R([47,91+LY,48,92+LY], INK, None); d.point((46,94+LY), fill=INK)
    for x in (45,47): d.line([(x,96+LY),(x,98+LY)], fill=TEAL)
def aura(cx, cy):
    for r, a in ((24,26),(19,40),(14,58)):
        L = Image.new("RGBA", im.size, (0,0,0,0)); ImageDraw.Draw(L).ellipse([cx-r,cy-r,cx+r,cy+r], fill=(79,216,240,a)); im.alpha_composite(L)
def shaft(sx, top, bottom):
    d.line([(sx,top),(sx,bottom)], fill=INK, width=5); d.line([(sx,top),(sx,bottom)], fill=purple, width=3)
    d.line([(sx-1,top+2),(sx-1,min(bottom,top+38))], fill=purple_hi)
    for y in (top+20, bottom-24):
        if top < y < bottom-4: R([sx-3,y,sx+3,y+2], purple_dk)
def orb(sx, cy, flames=False):
    if flames:
        for pts, col in (([(sx-9,cy+2),(sx-13,cy-9),(sx-5,cy-3)], hx("#b04cff")), ([(sx-6,cy-5),(sx-5,cy-14),(sx-1,cy-7)], hx("#b04cff")),
                         ([(sx-2,cy-6),(sx+1,cy-15),(sx+4,cy-6)], hx("#b04cff")), ([(sx+3,cy-6),(sx+8,cy-13),(sx+8,cy-3)], hx("#b04cff")),
                         ([(sx+8,cy-1),(sx+14,cy-8),(sx+11,cy+5)], hx("#b04cff")), ([(sx-3,cy-5),(sx,cy-10),(sx+3,cy-5)], hx("#ff5ac8"))):
            d.polygon(pts, fill=col)
    E([sx-8,cy-8,sx+8,cy+8], hx("#9a50ff")); E([sx-6,cy-6,sx+6,cy+6], hx("#a868ff"), hx("#7a3ad0"))
    E([sx-4,cy-5,sx,cy-1], hx("#f0e0ff"), None); d.point((sx+3,cy+3), fill=hx("#6a30c0"))
    d.arc([sx-15,cy-15,sx+15,cy+15], 195, 255, fill=purple_dk); d.arc([sx-15,cy-15,sx+15,cy+15], 15, 75, fill=purple_dk)
    d.arc([sx-19,cy-19,sx+19,cy+19], 280, 330, fill=hx("#8a5ac8")); d.arc([sx-19,cy-19,sx+19,cy+19], 100, 150, fill=hx("#8a5ac8"))
def star(x, y):
    for ox, oy in ((0,0),(-1,0),(1,0),(0,-1),(0,1),(-2,0),(2,0),(0,-2),(0,2)): d.point((x+ox,y+oy), fill=hx("#fff06a"))

def build(mode):
    global im, d
    rage, stun = mode == "rage", mode == "stun"
    SX = 94 if rage else 92
    # ---- layer A: cape, lantern aura, legs and boots
    A = layer()
    cx0, cy0 = (2, 8) if stun else (0, 0)                                # slumped shoulders: the cape's top moves with them
    P([(47+cx0,50+cy0),(81+cx0,50+cy0),(98,106),(91,113),(82,108),(73,115),(64,110),(55,115),(46,108),(37,113),(30,106)], cape)
    P([(47+cx0,52+cy0),(53+cx0,52+cy0),(38,108),(30,106)], lining); P([(75+cx0,52+cy0),(81+cx0,52+cy0),(98,106),(90,108)], lining)
    d.line([(64+cx0,54+cy0),(64,108)], fill=cape_sh); d.line([(56+cx0,56+cy0),(48,106)], fill=cape_sh); d.line([(72+cx0,56+cy0),(80,106)], fill=cape_sh)
    if not stun: aura(46, 96 + (-8 if rage else 0))
    R([52,82,63,106], trousers); R([65,82,76,106], trousers)
    d.line([(55,84),(55,104)], fill=hx("#2c3250")); d.line([(68,84),(68,104)], fill=hx("#2c3250"))
    P([(51,103),(63,103),(63,116),(43,116),(47,109)], boot); P([(65,103),(77,103),(81,109),(85,116),(65,116)], boot)
    d.line([(53,107),(60,107)], fill=hx("#4a4658")); d.line([(68,107),(75,107)], fill=hx("#4a4658"))
    # ---- layer B: jacket, collar, heart (slumps when stunned)
    B = layer()
    P([(47,52),(55,48),(73,48),(81,52),(79,78),(49,78)], jacket)
    d.line([(51,53),(52,76)], fill=jacket_hi); d.line([(77,53),(76,76)], fill=jacket_sh)
    P([(58,46),(70,46),(64,54)], jacket_hi)
    for y in (70,74): d.point((64,y), fill=hx("#6a6e9a"))
    def heart(o, col):
        d.ellipse([59-o,58-o,64,63+o], fill=col); d.ellipse([64,58-o,69+o,63+o], fill=col)
        d.polygon([(59-o,61),(69+o,61),(64,69+o*2)], fill=col)
    heart(1, INK); heart(0, pink); d.point((61,60), fill=hx("#ffd0e8"))
    # ---- layer C: the draped sash (stays at his waist)
    C = layer()
    checker([(49,74),(79,74),(80,82),(48,82)])
    checker([(49,80),(63,80),(61,98),(48,95)]); d.line([(48,96),(61,99)], fill=pink); d.line([(48,97),(61,100)], fill=pink)
    checker([(63,80),(77,80),(81,91),(65,92)]); d.line([(65,93),(81,92)], fill=pink)
    d.line([(49,83),(79,83)], fill=pink)
    R([60,74,68,82], purple); R([62,76,66,80], purple_dk); d.line([(61,75),(67,75)], fill=purple_hi)
    # ---- layer D: props, arms, epaulettes, head (slumps when stunned)
    D = layer()
    if rage:
        shaft(SX, 23, 104)
        arm([(48,54),(42,64),(46,71)]); arm([(80,54),(88,48),(94,41)])
        lantern(-8); glove(46,74); glove(94,43)
    elif stun:
        arm([(48,54),(46,69),(47,83)]); arm([(80,54),(82,69),(82,83)])
        glove(47,87); glove(82,87)
    else:
        shaft(SX, 26, 120)
        arm([(48,54),(43,67),(46,79)]); arm([(80,54),(85,67),(92,77)])
        lantern(); glove(46,82); glove(92,79)
    for sgn in (1, -1):
        f = (lambda x: x) if sgn == 1 else (lambda x: 128 - x)
        P([(f(56),49),(f(46),51),(f(42),58),(f(52),59),(f(56),55)], purple)
        d.line([(f(55),50),(f(47),52)], fill=purple_hi); d.line([(f(51),58),(f(43),57)], fill=purple_dk)
    R([58,43,70,50], skin_sh)
    H = layer()                                                       # the head is drawn on its own layer, then set 2 px higher
    hp = [(50,37),(45,30),(47,22),(44,15),(52,15),(54,9),(61,12),(66,7),(72,12),(79,9),(78,15),(87,15),(81,22),(90,25),(79,30),(77,23),(52,26)]
    if rage: hp = [(round(64+(x-64)*1.22), max(3, round(27+(y-27)*1.3))) for x, y in hp]
    P(hp, hair)
    d.line([(48,22),(46,16)], fill=hair_hi); d.line([(82,19),(86,17)], fill=hair_hi)
    P([(52,31),(43,27),(48,37),(52,39)], skin); P([(76,31),(85,27),(80,37),(76,39)], skin)
    d.line([(50,32),(46,30)], fill=skin_sh); d.line([(78,32),(82,30)], fill=skin_sh)
    E([51,19,77,47], skin); d.arc([54,36,75,48], 20, 160, fill=skin_sh)
    for pts in ([(50,24),(49,38),(55,30),(58,22)], [(57,21),(60,33),(65,26),(66,21)], [(65,21),(70,31),(76,25),(77,21)], [(74,21),(79,25),(80,38),(78,22)]):
        P(pts, hair, hair_dk)
    for a,b in (((53,24),(52,32)),((60,24),(61,30)),((71,24),(73,28)),((78,26),(79,33))): d.line([a,b], fill=hair_hi)
    P([(53,20),(52,11),(58,15),(61,8),(64,15),(67,8),(70,15),(76,11),(75,20)], crown)
    d.line([(54,19),(74,19)], fill=crown_hi)
    P([(64,5),(68,12),(64,19),(60,12)], pink); d.line([(64,7),(64,17)], fill=hx("#ff7ab8")); d.point((62,10), fill=hx("#ffe0f0"))
    R([53,30,62,38], black, INK); d.line([(53,31),(49,28)], fill=INK, width=2)
    if stun:
        E([66,29,76,38], white)
        cx, cy = 71, 33.5
        for t in range(0, 30):
            a = t * 0.6; r = 0.16 * t
            d.point((round(cx + math.cos(a) * r), round(cy + math.sin(a) * r)), fill=INK)
        d.line([(65,27),(76,27)], fill=INK, width=2)
        for p in ((60,43),(61,42),(62,41),(63,41),(64,41),(65,41),(66,41),(67,42),(68,43)): d.point(p, fill=INK)
    else:
        E([66,30,75,37], hx("#ffe4e4") if rage else white)
        E([69,31,74,37], hx("#ff1a1a") if rage else hx("#e020b0"), hx("#a01080") if not rage else hx("#a00a0a")); E([71,32,73,36], hx("#2a0a30"), None)
        d.rectangle([66,28,75,31], fill=skin); d.line([(66,32),(75,31)], fill=INK); d.point((70,33), fill=white)
        if rage:
            d.line([(65,30),(77,25)], fill=INK, width=2); d.line([(52,26),(62,30)], fill=INK, width=2)
            R([59,40,69,44], hx("#2a1020"), INK)
            for x in (60,62,64,66,68): d.point((x,41), fill=white)
            for x in (61,65,67): d.point((x,43), fill=white)
        else:
            d.line([(65,28),(76,26)], fill=INK, width=2)
            d.line([(60,41),(68,41)], fill=INK)
    d.point((64,36), fill=skin_sh); d.point((64,37), fill=skin_sh)
    D.alpha_composite(H, (0, -2))
    im, d = D, ImageDraw.Draw(D)
    if not stun: orb(SX, 15 if rage else 18, flames=rage)
    # ---- compose
    out = Image.new("RGBA", (SZ,SZ), (0,0,0,0))
    off = (2, 8) if stun else (0, 0)
    out.alpha_composite(A)
    out.alpha_composite(B, off)
    out.alpha_composite(C)
    out.alpha_composite(D, off)
    if stun:
        f = layer()
        # lantern lies knocked over by his feet, staff on the floor on the other side
        tmp = layer(); lantern(); lan = tmp.crop((38,82,54,108)).rotate(90, expand=True)
        f.alpha_composite(lan, (14, 104))
        tmp = layer(); shaft(20, 36, 62); orb(20, 28); stf = tmp.crop((0,8,40,64)).rotate(-90, expand=True)
        stf = stf.crop((0, 8, stf.width, stf.height-8))
        out_f = Image.new("RGBA", (SZ,SZ), (0,0,0,0)); out_f.alpha_composite(stf, (72, 104))
        fl = layer()
        for x, y in ((36,18),(94,16),(50,6),(80,4)): star(x, y)
        d.rectangle([60,2,63,5], fill=hx("#c9cdd4"), outline=INK); d.rectangle([22,10,24,14], fill=hx("#86dba6"), outline=INK)
        out.alpha_composite(f); out.alpha_composite(out_f); out.alpha_composite(fl)
    return out



OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "textures")


def main(out=OUT):
    folder = os.path.join(out, "boss")
    os.makedirs(folder, exist_ok=True)
    for mode, name in (("normal", "knight"), ("rage", "knight_rage"), ("stun", "knight_stun")):
        build(mode).resize((96, 96), Image.BOX).save(os.path.join(folder, name + ".png"))


if __name__ == "__main__":
    main()
