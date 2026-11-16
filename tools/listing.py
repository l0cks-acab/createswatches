"""CreateSwatches CurseForge / README art: icon, banner and captioned gallery images.
python3 tools/listing.py <shots_dir> <out_dir>"""
import os
import sys
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

SH, OUT = sys.argv[1], sys.argv[2]
os.makedirs(os.path.join(OUT, 'gallery'), exist_ok=True)
FD = '/mnt/skills/examples/canvas-design/canvas-fonts/'
TITLE = lambda s: ImageFont.truetype(FD + 'Tektur-Medium.ttf', s)
BODY = lambda s: ImageFont.truetype(FD + 'Outfit-Bold.ttf', s)
W, H = 1280, 720
BRASS, BRASS_HI, BRASS_DIM = (226, 172, 74), (255, 214, 128), (132, 92, 34)
WALNUT, WALNUT_DARK = (46, 33, 22), (28, 20, 13)
CREAM = (238, 228, 206)
# Swatch chips: a little palette under the title.
CHIPS = [(196, 92, 58), (226, 172, 74), (88, 160, 140), (70, 112, 168), (120, 82, 150)]


def shot(name):
    return Image.open(os.path.join(SH, name)).convert('RGB')


def bg(img, size=(W, H)):
    w, h = size
    im = img.resize((w, round(img.height * w / img.width)), Image.LANCZOS)
    top = max(0, (im.height - h) // 2)
    im = im.crop((0, top, w, top + h)) if im.height >= h else im.resize((w, h))
    im = im.filter(ImageFilter.GaussianBlur(16))
    im = ImageEnhance.Brightness(im).enhance(0.42)
    im = Image.blend(im, Image.new('RGB', (w, h), (24, 16, 8)), 0.4)
    grad = Image.linear_gradient('L').rotate(90).resize((w, h))
    shade = Image.new('RGB', (w, h), (10, 7, 4))
    im = Image.composite(im, shade, grad.point(lambda v: int(80 + v * 0.68)))
    return im.convert('RGBA')


def window(img, w, crop=None):
    if crop:
        img = img.crop(crop)
    h = round(img.height * w / img.width)
    img = img.resize((w, h), Image.LANCZOS)
    f = Image.new('RGBA', (w + 8, h + 8), WALNUT_DARK + (255,))
    d = ImageDraw.Draw(f)
    d.rectangle([1, 1, w + 6, h + 6], outline=BRASS_DIM + (255,), width=2)
    for x, y in [(0, 0), (w + 2, 0), (0, h + 2), (w + 2, h + 2)]:
        d.ellipse([x, y, x + 5, y + 5], fill=BRASS + (255,))
    f.paste(img, (4, 4))
    return f


def place(canvas, win, x, y):
    sh = Image.new('RGBA', (win.width + 80, win.height + 80), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rectangle([40, 50, 40 + win.width, 50 + win.height], fill=(0, 0, 0, 200))
    canvas.alpha_composite(sh.filter(ImageFilter.GaussianBlur(18)), (x - 40, y - 40))
    canvas.alpha_composite(win, (x, y))


def glow_text(canvas, xy, text, font, fill=(255, 255, 255), glow=BRASS, radius=16, strength=200):
    layer = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(layer).text(xy, text, font=font, fill=glow + (strength,))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(radius)))
    ImageDraw.Draw(canvas).text(xy, text, font=font, fill=fill + (255,))


def chips(canvas, x, y):
    d = ImageDraw.Draw(canvas)
    for i, c in enumerate(CHIPS):
        d.rectangle([x + i * 46, y, x + i * 46 + 38, y + 12], fill=c + (255,))


def trace(canvas, x0, x1, y):
    d = ImageDraw.Draw(canvas)
    d.line([x0, y, x1, y], fill=BRASS_DIM + (255,), width=2)
    d.rectangle([x1 - 3, y - 3, x1 + 3, y + 3], fill=BRASS + (255,))


def text_block(canvas, x, y, title, lines, tsize=60):
    glow_text(canvas, (x, y), title, TITLE(tsize), radius=14, strength=160)
    y += tsize + 26
    trace(canvas, x, x + 300, y)
    y += 24
    d = ImageDraw.Draw(canvas)
    for ln in lines:
        d.text((x, y), ln, font=BODY(26), fill=CREAM + (255,))
        y += 38
    return y


def save(canvas, name, size=None):
    im = canvas.convert('RGB')
    if size:
        im = im.resize(size, Image.LANCZOS)
    Image.frombytes('RGB', im.size, im.tobytes()).save(os.path.join(OUT, name), optimize=True)


def icon():
    S = 1024
    src = shot('hero2.png').crop((540, 150, 1040, 650)).resize((S - 160, S - 160), Image.LANCZOS)
    c = Image.new('RGBA', (S, S), WALNUT_DARK + (255,))
    mask = Image.new('L', src.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, src.width - 1, src.height - 1], radius=110, fill=255)
    glow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).rounded_rectangle([60, 60, S - 60, S - 60], radius=140, fill=BRASS + (210,))
    c.alpha_composite(glow.filter(ImageFilter.GaussianBlur(30)))
    ImageDraw.Draw(c).rounded_rectangle([64, 64, S - 64, S - 64], radius=130, fill=BRASS_DIM + (255,))
    c.paste(src, (80, 80), mask)
    full = Image.new('L', (S, S), 0)
    ImageDraw.Draw(full).rounded_rectangle([0, 0, S - 1, S - 1], radius=180, fill=255)
    c.putalpha(full)
    c.save(os.path.join(OUT, 'icon-1024.png'))
    Image.frombytes('RGBA', c.size, c.tobytes()).resize((400, 400), Image.LANCZOS).save(os.path.join(OUT, 'icon.png'))
    return c


def banner(ic):
    BW, BH = 1600, 400
    c = bg(shot('hero.png'), (BW, BH))
    c.alpha_composite(ic.resize((300, 300), Image.LANCZOS), (70, 50))
    glow_text(c, (420, 70), 'CreateSwatches', TITLE(120), radius=22, strength=200)
    trace(c, 424, 1000, 238)
    ImageDraw.Draw(c).text((424, 262), 'A spinning swatch stand for your Create storage.', font=BODY(38), fill=CREAM + (255,))
    chips(c, 426, 330)
    save(c, 'banner.png')


GUI = (260, 100, 1345, 790)


def gallery():
    pages = [
        ('01-createswatches.png', 'hero.png', 'CreateSwatches', ['Put a Swatch Carousel on your', 'storage and pull out the blocks', 'that match a colour or theme.'], (240, 0, 1600, 900), 600),
        ('02-swatch.png', 'gui_swatch.png', 'Pick a colour', ['HSV picker or hex, six colour', 'schemes, tolerance and amount.', 'Export pulls the matches out.'], GUI, 640),
        ('03-themes.png', 'gui_card.png', 'Themes & cards', ['14 built-in themes, plus your own.', 'Print a palette onto a Swatch', 'Card and share it.'], GUI, 640),
        ('04-cog.png', 'cog_mesh.png', 'A real cog', ['The base is a Create small', 'cogwheel. Mesh it with your', 'gear train; the cards ride on it.'], (260, 0, 1500, 820), 620),
        ('05-network.png', 'gui_network.png', 'Logistics', ['Tune it on a Stock Link to read', 'the whole network. Export sends', 'packages to an address.'], GUI, 640),
        ('06-applicator.png', 'applicator_wall.png', 'Swatch Applicator', ['A brass sprayer that places', 'palette blocks straight from', 'the carousel: wand, line, square.'], (200, 60, 1500, 880), 560),
        ('07-room.png', 'overview.png', 'Built for Create', ['Small SU cost, Create materials,', 'no other dependencies.'], (0, 0, 1600, 900), 660),
    ]
    for name, src, title, lines, crop, ww in pages:
        img = shot(src)
        c = bg(img)
        y = text_block(c, 70, 200 if len(lines) < 3 else 180, title, lines, 54 if len(title) > 13 else 60)
        if name.startswith('01'):
            chips(c, 72, y + 16)
        win = window(img, ww, crop)
        place(c, win, W - win.width - 60, (H - win.height) // 2)
        save(c, os.path.join('gallery', name))


ic = icon()
banner(ic)
gallery()
print('done')
