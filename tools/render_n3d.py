import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageChops
S=2048
pts=[(8,42),(8,6),(16,6),(32,28),(32,6),(40,6),(40,42),(32,42),(16,20),(16,42)]
sc=S/48*0.70; ox=S*0.5-24*sc-S*0.025; oy=S*0.5-24*sc-S*0.03
P=[(ox+x*sc,oy+y*sc) for x,y in pts]
def mask(dx=0,dy=0):
    m=Image.new('L',(S,S),0); ImageDraw.Draw(m).polygon([(x+dx,y+dy) for x,y in P],fill=255); return m
def grad(c0,c1,c2,dark=1.0):
    y,x=np.mgrid[0:S,0:S]; t=np.clip(((x-ox-8*sc)+(y-oy-6*sc))/((32+36)*sc),0,1)[...,None]
    a=np.array(c0,float);b=np.array(c1,float);c=np.array(c2,float)
    col=np.where(t<.5,a+(b-a)*(t/.5),b+(c-b)*((t-.5)/.5))*dark
    return col
front=mask()
out=np.zeros((S,S,4),float)
def over(rgb,alpha):
    global out
    a=alpha[...,None]; out[...,:3]=rgb*a+out[...,:3]*(1-a); out[...,3:]=a+out[...,3:]*(1-a)
# glow
g=np.array(front.filter(ImageFilter.GaussianBlur(70)),float)/255*0.55
over(grad((79,227,255),(61,123,255),(179,107,255)),g)
# drop shadow
D=int(S*0.06)
sh=np.array(mask(D*1.3,D*1.9).filter(ImageFilter.GaussianBlur(40)),float)/255*0.6
over(np.zeros((S,S,3))+[4,6,24],sh)
# extrusion
side=grad((20,90,140),(22,45,140),(70,30,130))
for i in range(D,0,-1):
    m=np.array(mask(i*0.7,i),float)/255
    k=0.55+0.45*(1-i/D)
    over(side*k,m)
# front face
fm=np.array(front,float)/255
col=grad((79,227,255),(61,123,255),(179,107,255))
# bevel lighting
bl=np.array(front.filter(ImageFilter.GaussianBlur(14)),float)/255
gy,gx=np.gradient(bl); light=(gx*0.6+gy*0.8)*40
inner=np.clip(1-np.array(front.filter(ImageFilter.GaussianBlur(10)),float)/255*1.0,0,1)
edge=np.clip(fm*(1-bl)*3,0,1)
col=col*(1+np.clip(light,-0.6,0.9)[...,None]*0.9)
# gloss: top diagonal sheen
y,x=np.mgrid[0:S,0:S]
gl=np.clip(1-((y-oy)/(36*sc))*1.6,0,1)**1.6*0.42
col=col+(255-col)*gl[...,None]
# subtle inner shade bottom
col=col*(1-np.clip((y-oy-18*sc)/(18*sc),0,1)*0.18)[...,None]
over(np.clip(col,0,255),fm)
# specular rim line top-left
rim=np.clip(light,0,1)*fm*0.8
over(np.full((S,S,3),255.0),np.clip(rim,0,1))
img=Image.fromarray(np.clip(np.dstack([out[...,:3]/np.maximum(out[...,3:],1e-6),out[...,3:]*255]),0,255).astype('uint8'),'RGBA')
img.save('n3d_full.png')
base='/workspace/nibrasnova/app/src/main/res/drawable-'
for d,px in [('mdpi',96),('hdpi',144),('xhdpi',192),('xxhdpi',288),('xxxhdpi',384)]:
    import os; os.makedirs(base+d,exist_ok=True)
    img.resize((px,px),Image.LANCZOS).save(base+d+'/ic_nova_n_3d.webp','WEBP',lossless=True)
bg=Image.new('RGBA',(S,S),(7,13,38,255)); bg.alpha_composite(img); bg.resize((512,512),Image.LANCZOS).save('preview.png')
