#!/usr/bin/env python3
"""Builds the Discarded Knight sprites (sir_filament, sir_filament_rage, sir_filament_stun, sir_filament_story) from tools/knight_source.png.

Usage:  python3 tools/gen_knight_sprites.py      (needs Pillow and numpy)
"""
import os
import numpy as np
from PIL import Image, ImageDraw
import colorsys, math
INK=(26,18,38); PINK=(255,46,147)
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "textures", "boss")


def load_base():
    src = Image.open(os.path.join(HERE, "knight_source.png")).convert("RGB")
    a = np.array(src).astype(int); ys, xs = np.where(a.sum(axis=2) > 60)
    crop = src.crop((xs.min() - 6, ys.min() - 6, xs.max() + 7, ys.max() + 7))
    low = crop.resize((round(crop.width / 6), round(crop.height / 6)), Image.BOX)
    return low.quantize(colors=40, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert("RGB")


BASE = load_base(); W, H = BASE.size

def common():
    q=BASE.copy(); px=q.load(); d=ImageDraw.Draw(q)
    hair=(192,42,166)
    for y in range(24,38):
        for x in range(58,88):
            r,g,b=px[x,y]
            if r>170 and g>150 and b>200: px[x,y]=hair
    CR=(122,16,104); CR_HI=(176,42,150)
    d.polygon([(60,37),(60,29),(64,33),(66,26),(70,33),(74,33),(78,33),(79,26),(82,33),(84,29),(84,37)], fill=CR, outline=INK)
    d.line([(62,35),(82,35)], fill=CR_HI)
    d.polygon([(72,22),(77,29),(72,37),(67,29)], fill=PINK, outline=INK)
    for p in ((70,27),(71,26),(70,28)): px[p]=(255,208,232)
    px[(73,31)]=(200,20,110); px[(72,33)]=(200,20,110)
    jack=px[(62,80)]
    for y in range(64,90):
        for x in range(62,85):
            r,g,b=px[x,y]
            if r>150 and g<110 and b>120: px[x,y]=jack
    def heart(g,col):
        d.ellipse([65.4-g,71.4-g,72.6+g,78.6+g], fill=col); d.ellipse([71.4-g,71.4-g,78.6+g,78.6+g], fill=col)
        d.polygon([(65-g,76),(79+g,76),(72,84+g*1.5)], fill=col)
    heart(1.2,INK); heart(0,PINK)
    for p in ((67,73),(68,73),(67,74)): px[p]=(255,208,232)
    refs=[((92,226,246),(192,48,154)),((62,134,200),(138,31,112)),((55,48,125),(90,26,122))]
    for y in range(86,H):
        for x in range(0,50):
            if 37<=x<=53 and 112<=y<=137: continue
            r,g,b=px[x,y]; h,s,v=colorsys.rgb_to_hsv(r/255,g/255,b/255)
            if 0.46<h<0.72 and s>0.3 and v>0.25:
                px[x,y]=min(refs,key=lambda t:sum((a-c)**2 for a,c in zip(t[0],(r,g,b))))[1]
    return q

def star(d,x,y,c=(255,240,106)):
    for ox,oy in ((0,0),(-1,0),(1,0),(0,-1),(0,1),(-2,0),(2,0),(0,-2),(0,2)): d.point((x+ox,y+oy),fill=c)

def rage(q):
    px=q.load(); d=ImageDraw.Draw(q)
    for y in range(43,51):
        for x in range(75,88):
            r,g,b=px[x,y]
            if r>150 and g<110 and b>100: px[x,y]=(255,26,26)
    for p in ((79,46),(80,46),(79,47),(80,47)): px[p]=(255,26,26)
    d.line([(88,40),(76,45)], fill=INK, width=2); d.line([(57,41),(68,45)], fill=INK, width=2)
    d.rectangle([66,54,76,58], fill=(42,16,32), outline=INK)
    for x in (67,69,71,73,75): d.point((x,55), fill=(244,242,248))
    for x in (68,72,74): d.point((x,57), fill=(244,242,248))
    src=q.copy(); fl=ImageDraw.Draw(q); cx,cy=123,22
    for pts,col in (([(cx-12,cy+2),(cx-17,cy-10),(cx-7,cy-4)],(176,76,255)),([(cx-8,cy-6),(cx-7,cy-17),(cx-2,cy-8)],(176,76,255)),
                    ([(cx-3,cy-8),(cx,cy-19),(cx+4,cy-8)],(176,76,255)),([(cx+4,cy-8),(cx+9,cy-17),(cx+10,cy-5)],(176,76,255)),
                    ([(cx+10,cy-2),(cx+17,cy-10),(cx+13,cy+6)],(176,76,255)),([(cx-4,cy-8),(cx,cy-14),(cx+3,cy-8)],(255,90,200))):
        fl.polygon([(round(cx+(px_-cx)*1.4), max(0, round(cy+(py_-cy)*1.4))) for px_,py_ in pts],fill=col)
    a=src.load(); b=q.load()
    for y in range(H):
        for x in range(W):
            if sum(a[x,y])>36: b[x,y]=a[x,y]
    return q

def stun(q):
    px=q.load(); d=ImageDraw.Draw(q)
    for y in range(0,H):
        for x in range(104,W):
            if 84<=y<=102 and 117<=x<=133: continue
            r,g,b=px[x,y]; h,s,v=colorsys.rgb_to_hsv(r/255,g/255,b/255)
            if x>=117 and x<=133 and not(0.82<h<0.97 and s>0.4): px[x,y]=(0,0,0)
            elif y<60 and sum((r,g,b))>0 and not(0.82<h<0.97 and s>0.4): px[x,y]=(0,0,0)
    head=q.crop((44,10,102,62)); hp=head.load(); out=Image.new('RGB',head.size,(0,0,0))
    q.paste((0,0,0),(44,10,102,62))
    qq=q.copy(); qq.paste(head,(46,15)) 
    mask=Image.new('L',head.size,0); mp=mask.load()
    for y in range(head.height):
        for x in range(head.width):
            if sum(hp[x,y])>36: mp[x,y]=255
    q.paste(head,(46,15),mask); d=ImageDraw.Draw(q)
    d.ellipse([75,48,88,58], fill=(244,242,248), outline=INK)
    cx,cy=81.5,53
    for t in range(0,42):
        a=t*0.55; r=0.17*t; d.point((round(cx+math.cos(a)*r),round(cy+math.sin(a)*r)), fill=INK)
    d.line([(74,47),(88,47)], fill=INK, width=2)
    for p in ((67,64),(68,63),(69,62),(70,62),(71,62),(72,62),(73,62),(74,63),(75,64)): d.point(p,fill=INK)
    for x,y in ((46,12),(100,10),(72,5)): star(d,x,y)
    d.rectangle([56,3,60,8], fill=(201,205,212), outline=INK); d.rectangle([88,6,91,11], fill=(134,219,166), outline=INK)
    d.line([(103,176),(138,176)], fill=INK, width=4); d.line([(103,176),(138,176)], fill=(138,74,208), width=2)
    d.ellipse([136,170,146,180], fill=(154,80,255), outline=INK); d.point((139,173),fill=(240,224,255))
    return q

def add_outline(im, ink=(11,4,23,255)):
    px=im.load(); w,h=im.size; add=[]
    for y in range(h):
        for x in range(w):
            if px[x,y][3]==0 and any(0<=x+dx<w and 0<=y+dy<h and px[x+dx,y+dy][3]==255 for dx,dy in ((1,0),(-1,0),(0,1),(0,-1))): add.append((x,y))
    for x,y in add: px[x,y]=ink
    return im

def finish(q, canvas, scale=None, pos=None, outline=False):
    bg=Image.new('RGBA',q.size); bg.paste(q); bp=bg.load(); seen=set()
    stack=[(0,0),(W-1,0),(0,H-1),(W-1,H-1),(114,119)]
    while stack:
        x,y=stack.pop()
        if (x,y) in seen or not(0<=x<W and 0<=y<H): continue
        if sum(bp[x,y][:3])>36: continue
        seen.add((x,y)); bp[x,y]=(0,0,0,0); stack+=[(x+1,y),(x-1,y),(x,y+1),(x,y-1)]
    if scale:
        w,h=round(W*scale),round(H*scale); bg=bg.resize((w,h),Image.BOX)
        a=bg.getchannel('A').point(lambda v:255 if v>110 else 0)
        rgb=bg.convert('RGB').quantize(colors=40,method=Image.Quantize.MEDIANCUT,dither=Image.Dither.NONE).convert('RGBA'); rgb.putalpha(a); bg=rgb
    out=Image.new('RGBA',canvas,(0,0,0,0)); out.alpha_composite(bg,pos)
    if outline: out=add_outline(out)
    return out

def main():
    S = 0.52
    variants = {"sir_filament": common(), "sir_filament_rage": rage(common()), "sir_filament_stun": stun(common())}
    os.makedirs(OUT, exist_ok=True)
    for name, q in variants.items():
        finish(q, (96, 96), S, (11, 96 - round(H * S) - 1)).save(os.path.join(OUT, name + ".png"))
    finish(variants["sir_filament"], (200, 200), None, (26, 200 - 4 - H), outline=True).save(os.path.join(OUT, "sir_filament_story.png"))


if __name__ == "__main__":
    main()
