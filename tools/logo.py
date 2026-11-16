"""Create-addon style logo: the carousel item on a round blue blueprint badge with a white rim.
python3 tools/logo.py <item_render_on_magenta.png> <out_dir>
The render is a screenshot of the item on a pure magenta backdrop (keyed out here)."""
import os
import sys
from PIL import Image, ImageChops, ImageDraw, ImageFilter

SRC, OUT = sys.argv[1], sys.argv[2]
S = 1024
C = S // 2

shot = Image.open(SRC).convert('RGB')
w, h = shot.size
shot = shot.crop((w // 5, h // 5, w * 4 // 5, h * 17 // 18))  # stays below the chat toast
px = shot.load()
item = Image.new('RGBA', shot.size, (0, 0, 0, 0))
ip = item.load()
for y in range(shot.height):
    for x in range(shot.width):
        r, g, b = px[x, y]
        if not (r > 230 and g < 40 and b > 230):
            ip[x, y] = (r, g, b, 255)
item = item.crop(item.getbbox())
k = 660 / max(item.size)
item = item.resize((round(item.width * k), round(item.height * k)), Image.NEAREST)

# Badge: radial blue, blueprint grid, soft centre glow.
badge = Image.new('RGBA', (S, S), (0, 0, 0, 0))
grad = Image.radial_gradient('L').resize((S, S))
inner, outer = (96, 170, 238), (24, 92, 182)
blue = Image.composite(Image.new('RGB', (S, S), outer), Image.new('RGB', (S, S), inner), grad.point(lambda v: min(255, int(v * 1.25))))
grid = Image.new('RGBA', (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(grid)
for i in range(0, S, 32):
    a = 70 if (i // 32) % 4 == 0 else 34
    d.line([(i, 0), (i, S)], fill=(190, 225, 255, a), width=2 if a > 40 else 1)
    d.line([(0, i), (S, i)], fill=(190, 225, 255, a), width=2 if a > 40 else 1)
blue = blue.convert('RGBA')
blue.alpha_composite(grid)
glow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
ImageDraw.Draw(glow).ellipse([C - 300, C - 300, C + 300, C + 300], fill=(170, 220, 255, 90))
blue.alpha_composite(glow.filter(ImageFilter.GaussianBlur(90)))

R = 492
disc = Image.new('L', (S * 4, S * 4), 0)
ImageDraw.Draw(disc).ellipse([4 * (C - R), 4 * (C - R), 4 * (C + R), 4 * (C + R)], fill=255)
disc = disc.resize((S, S), Image.LANCZOS)
badge.paste(blue, (0, 0), disc)

# Item with a soft shadow under it.
x, y = C - item.width // 2, C - item.height // 2 + 10
shadow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
sm = Image.new('L', (S, S), 0)
sm.paste(item.split()[3], (x + 14, y + 22))
shadow.putalpha(sm.point(lambda v: v * 110 // 255))
badge.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(14)))
badge.alpha_composite(item, (x, y))

# White rim, with a faint dark edge outside like the Create badges.
rim = Image.new('L', (S * 4, S * 4), 0)
rd = ImageDraw.Draw(rim)
rd.ellipse([4 * (C - R - 8), 4 * (C - R - 8), 4 * (C + R + 8), 4 * (C + R + 8)], fill=255)
rd.ellipse([4 * (C - R + 26), 4 * (C - R + 26), 4 * (C + R - 26), 4 * (C + R - 26)], fill=0)
rim = rim.resize((S, S), Image.LANCZOS)
white = Image.new('RGBA', (S, S), (255, 255, 255, 0))
white.putalpha(rim)
edge = Image.new('RGBA', (S, S), (0, 0, 0, 0))
ImageDraw.Draw(edge).ellipse([C - R - 12, C - R - 12, C + R + 12, C + R + 12], outline=(20, 30, 50, 120), width=5)
badge.alpha_composite(edge.filter(ImageFilter.GaussianBlur(2)))
badge.alpha_composite(white)

os.makedirs(OUT, exist_ok=True)
clean = Image.frombytes('RGBA', badge.size, badge.tobytes())
clean.save(os.path.join(OUT, 'icon-1024.png'))
clean.resize((400, 400), Image.LANCZOS).save(os.path.join(OUT, 'icon.png'))
