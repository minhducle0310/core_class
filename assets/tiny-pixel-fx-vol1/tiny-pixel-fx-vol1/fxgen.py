#!/usr/bin/env python3
"""Tiny Pixel FX Pack Vol.1 - every frame is drawn by this script (no AI images, no samples).
Run: python3 fxgen.py  -> out/  (sheets, frames, gif previews, cover)"""
import math, random, os, shutil, json
from PIL import Image, ImageDraw
S=32; N=8; OUT='out'
PAL={ # dark, mid, bright, core
 'ember':[(122,36,24),(214,82,32),(255,170,51),(255,244,214)],
 'frost':[(28,52,110),(46,120,214),(110,214,255),(236,252,255)],
 'venom':[(24,84,44),(52,160,64),(150,230,90),(240,255,214)],
 'arcane':[(70,30,110),(140,60,200),(214,130,255),(252,236,255)],
 'mono':[(70,70,84),(140,140,156),(214,214,224),(255,255,255)],
}
def px(img,x,y,c):
    x=int(round(x)); y=int(round(y))
    if 0<=x<S and 0<=y<S: img.putpixel((x,y),c+(255,))
def disc(img,cx,cy,r,c):
    for y in range(S):
        for x in range(S):
            if (x+.5-cx)**2+(y+.5-cy)**2<=r*r: img.putpixel((x,y),c+(255,))
def ring(img,cx,cy,r,w,c):
    for y in range(S):
        for x in range(S):
            d=math.hypot(x+.5-cx,y+.5-cy)
            if r-w<=d<=r: img.putpixel((x,y),c+(255,))
def line(img,x0,y0,x1,y1,c):
    n=int(max(abs(x1-x0),abs(y1-y0)))+1
    for i in range(n+1):
        t=i/max(n,1); px(img,x0+(x1-x0)*t,y0+(y1-y0)*t,c)
C=S/2
def hit_spark(f,p,rng):
    im=Image.new('RGBA',(S,S)); t=f/(N-1)
    if f<2: disc(im,C,C,3-f,p[3])
    for k in range(8):
        a=k*math.pi/4+0.2; r0=2+12*t; r1=r0+max(0,6-7*t)
        if r1>r0: line(im,C+r0*math.cos(a),C+r0*math.sin(a),C+r1*math.cos(a),C+r1*math.sin(a),p[2] if t<.5 else p[1])
    return im
def impact_ring(f,p,rng):
    im=Image.new('RGBA',(S,S)); r=3+f*1.7; w=max(1,3-f//3)
    if f<N-1: ring(im,C,C,r,w,p[2] if f<4 else p[1])
    if f<3: disc(im,C,C,3-f,p[3])
    return im
def explosion(f,p,rng):
    im=Image.new('RGBA',(S,S)); t=f/(N-1); r=4+10*math.sin(min(1,t*1.4)*math.pi/2)
    for y in range(S):
        for x in range(S):
            d=math.hypot(x+.5-C,y+.5-C)+rng.random()*1.5
            if d>r: continue
            q=d/r
            if t>0.55 and (x+y)%2==0 and q>1-(t-.55)*2.2: continue  # dithered break-up
            c=p[3] if q<.35-t*.3 else p[2] if q<.65-t*.2 else p[1] if q<.9 else p[0]
            im.putpixel((x,y),c+(255,))
    return im
def slash_arc(f,p,rng):
    im=Image.new('RGBA',(S,S)); a0=-2.4; sweep=min(1,(f+1)/4)*2.6; fade=max(0,f-3)
    for i in range(60):
        a=a0+sweep*i/59
        if i/59<fade/4: continue
        for r,c in ((13,p[3]),(12,p[2]),(11,p[1])):
            th=1 if r!=12 else 1
            px(im,C+r*math.cos(a),C+r*math.sin(a),c)
    return im
def dash_dust(f,p,rng):
    im=Image.new('RGBA',(S,S)); g=random.Random(7)
    for k in range(5):
        bx=26-k*5-f*1.2; by=24+g.uniform(-2,2)-f*0.6; r=max(0,2.6+k*0.3-f*0.4+ (1 if f<3 else 0))
        if r>0.6: disc(im,bx,by,r,p[1] if k%2 else p[2])
    return im
def sparkle(f,p,rng):
    im=Image.new('RGBA',(S,S)); L=[1,3,6,8,6,4,2,1][f]
    for d in range(-L,L+1):
        c=p[3] if abs(d)<2 else p[2] if abs(d)<L-1 else p[1]
        px(im,C+d,C,c); px(im,C,C+d,c)
    if 2<=f<=5:
        for dx,dy in((1,1),(-1,1),(1,-1),(-1,-1)):
            for k in range(1,3): px(im,C+dx*k,C+dy*k,p[2])
    return im
def heal_plus(f,p,rng):
    im=Image.new('RGBA',(S,S)); g=random.Random(3)
    for k in range(5):
        x=6+k*5+g.randint(-1,1); ph=(f+k*2)%N; y=28-ph*3
        if ph<N-1:
            c=p[3] if ph<2 else p[2] if ph<5 else p[1]
            for d in(-1,0,1): px(im,x+d,y,c); px(im,x,y+d,c)
    return im
def zap(f,p,rng):
    im=Image.new('RGBA',(S,S)); g=random.Random(f*13+1)
    if f in(6,7) : 
        if f==6: disc(im,C,28,2,p[1])
        return im
    x=C; y=2
    while y<29:
        nx=x+g.randint(-4,4); ny=y+g.randint(3,6)
        line(im,x,y,nx,ny,p[3] if f%2==0 else p[2]); line(im,x+1,y,nx+1,ny,p[1]); x,y=max(4,min(28,nx)),ny
    return im
def parry(f,p,rng):
    im=Image.new('RGBA',(S,S)); r=[6,9,11,12,12,12,12,12][f]
    if f<6:
        for a in range(-50,51,2):
            ar=math.radians(a); c=p[3] if f<2 else p[2] if f<4 else p[1]
            px(im,C-4+r*math.cos(ar),C+r*math.sin(ar),c)
            px(im,C-5+r*math.cos(ar),C+r*math.sin(ar),p[1])
    if 1<=f<=4:
        for k in range(3): px(im,C+8+f*1.5+k,C-6+k*6,p[3])
    return im
def fire_burst(f,p,rng):
    im=Image.new('RGBA',(S,S)); g=random.Random(5)
    for k in range(14):
        a=g.uniform(-math.pi*0.85,-math.pi*0.15); sp=g.uniform(1.2,2.4); life=g.randint(5,8)
        if f>=life: continue
        d=sp*f*1.6; x=C+d*math.cos(a); y=26+d*math.sin(a)+0.15*f*f
        r=max(0.6,2.2-f*0.25); c=p[3] if f<2 else p[2] if f<4 else p[1] if f<6 else p[0]
        disc(im,x,y,r,c)
    return im
FX=[('hit_spark',hit_spark),('impact_ring',impact_ring),('explosion',explosion),('slash_arc',slash_arc),('dash_dust',dash_dust),
    ('sparkle',sparkle),('heal_plus',heal_plus),('zap',zap),('parry_flash',parry),('fire_burst',fire_burst)]
if os.path.exists(OUT): shutil.rmtree(OUT)
meta=[]; previews=[]
for name,fn in FX:
    for pn,p in PAL.items():
        rng=random.Random(hash((name,pn))%999)
        frames=[fn(f,p,random.Random(f*31+7)) for f in range(N)]
        sheet=Image.new('RGBA',(S*N,S))
        for i,fr in enumerate(frames): sheet.paste(fr,(i*S,0))
        d=f'{OUT}/sheets_32px'; os.makedirs(d,exist_ok=True); sheet.save(f'{d}/{name}_{pn}.png')
        d2=f'{OUT}/sheets_128px'; os.makedirs(d2,exist_ok=True); sheet.resize((S*N*4,S*4),Image.NEAREST).save(f'{d2}/{name}_{pn}.png')
        fd=f'{OUT}/frames/{name}_{pn}'; os.makedirs(fd,exist_ok=True)
        for i,fr in enumerate(frames): fr.save(f'{fd}/{i:02d}.png')
        bg=(24,20,37)
        gif=[Image.alpha_composite(Image.new('RGBA',(S,S),bg+(255,)),fr).resize((128,128),Image.NEAREST).convert('P',palette=Image.ADAPTIVE) for fr in frames]
        gd=f'{OUT}/preview_gif'; os.makedirs(gd,exist_ok=True)
        gif[0].save(f'{gd}/{name}_{pn}.gif',save_all=True,append_images=gif[1:],duration=70,loop=0,disposal=2)
        meta.append({'name':f'{name}_{pn}','frames':N,'size':S,'fps_suggested':14}); previews.append((name,pn,frames))
json.dump(meta,open(f'{OUT}/fx_index.json','w'),indent=1)
# cover 630x500 : grid of frame 2/3 of each fx in ember/frost
cov=Image.new('RGBA',(630,500),(24,20,37,255)); d=ImageDraw.Draw(cov)
pick=[x for x in previews if x[1] in('ember','frost','arcane','venom','mono')]
for i,(n,pn,fr) in enumerate(pick):
    col=i%10; row=i//10
    if row>4: break
    im=fr[2 if n not in('heal_plus','dash_dust') else 3].resize((56,56),Image.NEAREST)
    cov.alpha_composite(im,(10+col*62,90+row*80))
d.text((14,18),"TINY PIXEL FX PACK  Vol.1",fill=(255,244,214)); d.text((14,40),"10 effects x 5 palettes  |  32px  8 frames  |  CC0  |  drawn by code",fill=(214,214,224))
cov.convert('RGB').save(f'{OUT}/cover.png')
# animated cover gif (all 10 ember+frost)
fr_all=[]
for f in range(N):
    c=Image.new('RGBA',(640,256),(24,20,37,255))
    for i,(n,fn) in enumerate(FX):
        for j,pn in enumerate(('ember','frost')):
            im=[x for x in previews if x[0]==n and x[1]==pn][0][2][f].resize((64,64),Image.NEAREST); c.alpha_composite(im,(i*64,j*128+32))
    fr_all.append(c.convert('P',palette=Image.ADAPTIVE))
fr_all[0].save(f'{OUT}/showcase.gif',save_all=True,append_images=fr_all[1:],duration=80,loop=0)
print(len(meta),'sheets')
