"""Recolours the applicator texture from tools/applicator_source.png into Create's brass, andesite and walnut tones."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "tools/applicator_source.png"
OUT = ROOT / "src/main/resources/assets/createswatches/textures/item/swatch_applicator.png"

MAP = {
    (242, 242, 242): (246, 214, 132), (232, 232, 234): (230, 190, 104), (213, 214, 220): (214, 168, 84),
    (212, 214, 224): (214, 168, 84), (203, 204, 212): (196, 146, 66), (191, 193, 205): (176, 128, 56),
    (179, 182, 198): (168, 166, 156), (135, 143, 165): (130, 128, 120), (120, 126, 151): (108, 106, 100),
    (38, 43, 84): (74, 52, 32), (27, 31, 61): (56, 38, 22), (18, 21, 42): (36, 24, 14),
    (90, 58, 154): (150, 72, 32), (138, 90, 216): (206, 120, 52), (199, 168, 255): (255, 206, 132),
}

im = Image.open(SRC).convert("RGBA")
px = im.load()
for y in range(im.height):
    for x in range(im.width):
        r, g, b, a = px[x, y]
        if a and (r, g, b) in MAP:
            px[x, y] = MAP[(r, g, b)] + (a,)
OUT.parent.mkdir(parents=True, exist_ok=True)
im.save(OUT)
