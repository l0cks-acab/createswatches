"""Builds the Swatch Typewriter's models and its own textures.

Run from the repo root: python3 tools/typewriter_model.py
The static block model is the plinth, body and key deck; keys and carriage are separate partial models
so the renderer can bob and slide them. The item model is all three together.
"""
import json
import random
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/createswatches"
MODELS = ASSETS / "models"
TEX = ASSETS / "textures/block"

ANDESITE = "create:block/andesite_casing"
BRASS = "create:block/brass_casing"
BRASS_BLOCK = "create:block/brass_block"
WALNUT = "minecraft:block/dark_oak_planks"


def box(frm, to, tex, uv=None, faces="nsewud", name=None, rot=None):
    names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
    x0, y0, z0 = frm
    x1, y1, z1 = to
    auto = {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
        "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0],
    }
    el = {"from": list(frm), "to": list(to), "faces": {}}
    for f in faces:
        n = names[f]
        el["faces"][n] = {"uv": (uv or {}).get(n, auto[n]), "texture": "#" + tex}
    if name:
        el["name"] = name
    if rot:
        el["rotation"] = rot
    return el


def base_elements():
    return [
        box((1, 0, 1), (15, 3, 15), "andesite", name="plinth"),
        box((2, 3, 5), (14, 8, 14), "brass", name="body"),
        box((2, 3, 1), (14, 4.5, 5), "walnut", name="deck"),
        box((2.5, 8, 6), (13.5, 8.5, 9), "brass_block", faces="nsewu", name="ribbon_cover"),
    ]


def key_elements():
    els = []
    for row in range(3):
        z0 = 1.4 + row * 1.2
        y0 = 4.5 + row * 0.5
        for k in range(5):
            x0 = 3.2 + k * 2.0
            els.append(box((x0, y0, z0), (x0 + 1.3, y0 + 0.8, z0 + 1.0), "key", faces="nsewu",
                           uv={"up": [0, 0, 8, 8], "north": [0, 8, 8, 12], "south": [0, 8, 8, 12],
                               "east": [8, 8, 12, 12], "west": [8, 8, 12, 12]}))
    els.append(box((5, 4.5, 0.6), (11, 5.0, 1.3), "key", faces="nsewu",
                   uv={"up": [0, 12, 16, 14], "north": [0, 14, 16, 16], "south": [0, 14, 16, 16],
                       "east": [0, 14, 2, 16], "west": [0, 14, 2, 16]}, name="space_bar"))
    return els


def carriage_elements():
    return [
        box((1, 8, 9), (15, 10.5, 12), "platen", name="platen",
            uv={"north": [0, 0, 14, 3], "south": [0, 0, 14, 3], "up": [0, 3, 14, 6], "down": [0, 3, 14, 6],
                "east": [14, 0, 16, 3], "west": [14, 0, 16, 3]}),
        box((0, 8.5, 9.5), (1, 10, 11.5), "brass_block", name="knob_left"),
        box((15, 8.5, 9.5), (16, 10, 11.5), "brass_block", name="knob_right"),
        box((0.3, 10, 7.5), (1.0, 10.7, 10.5), "brass_block", name="return_lever"),
        box((4, 9.5, 10), (12, 15.6, 10.2), "paper", name="paper",
            uv={"north": [0, 0, 8, 6.1], "south": [8, 0, 16, 6.1], "up": [0, 0, 8, 0.2],
                "east": [0, 0, 0.2, 6.1], "west": [0, 0, 0.2, 6.1], "down": [0, 0, 8, 0.2]}),
    ]


TEXTURES = {
    "andesite": ANDESITE, "brass": BRASS, "brass_block": BRASS_BLOCK, "walnut": WALNUT,
    "key": "createswatches:block/typewriter_key", "platen": "createswatches:block/typewriter_platen",
    "paper": "createswatches:block/typewriter_paper",
}


def model(elements, parent=None, particle=BRASS):
    tex = {k: v for k, v in TEXTURES.items() if any(f"#{k}" == face["texture"] for e in elements for face in e["faces"].values())}
    tex["particle"] = particle
    out = {"textures": tex, "elements": elements}
    if parent:
        out["parent"] = parent
    return out


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def textures():
    rnd = random.Random(7)
    TEX.mkdir(parents=True, exist_ok=True)
    # Key: dark cap with a cream rim and a printed glyph on top; 4px band below for the sides.
    key = Image.new("RGBA", (16, 16))
    p = key.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = (44, 34, 26, 255)
    for x in range(8):
        for y in range(8):
            edge = x in (0, 7) or y in (0, 7)
            p[x, y] = (178, 150, 104, 255) if edge else (36, 28, 22, 255)
    for x, y in [(3, 2), (4, 2), (2, 3), (5, 3), (2, 4), (3, 4), (4, 4), (5, 4), (2, 5), (5, 5)]:
        p[x, y] = (236, 222, 190, 255)
    for x in range(16):
        for y in range(8, 16):
            p[x, y] = (30, 24, 18, 255) if y in (8, 15) else (56, 44, 34, 255)
    for x in range(16):
        p[x, 12] = (196, 168, 120, 255)
        p[x, 13] = (178, 150, 104, 255)
    key.save(TEX / "typewriter_key.png")

    # Platen: dark rubber roller with grooves; brass end caps in the last two columns.
    platen = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = platen.load()
    for y in range(6):
        for x in range(14):
            v = 34 + (10 if y in (0, 3) else 0) + rnd.randint(-3, 3)
            p[x, y] = (v, v - 4, v - 8, 255)
        for x in (14, 15):
            p[x, y] = (214, 168, 84, 255) if y % 3 else (176, 128, 56, 255)
    platen.save(TEX / "typewriter_platen.png")

    # Paper: cream, faint typed lines on the back half (the front gets the BER's chips).
    paper = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = paper.load()
    for y in range(7):
        for x in range(16):
            v = rnd.randint(-4, 2)
            p[x, y] = (240 + v, 232 + v, 212 + v, 255)
    for y in (2, 4):
        for x in range(9, 15):
            if rnd.random() < 0.7:
                p[x, y] = (150, 140, 128, 255)
    paper.save(TEX / "typewriter_paper.png")

    Image.new("RGBA", (4, 4), (255, 255, 255, 255)).save(TEX / "chip.png")


def main():
    textures()
    write(MODELS / "block/swatch_typewriter/block.json", model(base_elements(), "minecraft:block/block"))
    write(MODELS / "block/swatch_typewriter/keys.json", model(key_elements()))
    write(MODELS / "block/swatch_typewriter/carriage.json", model(carriage_elements()))
    item = model(base_elements() + key_elements() + carriage_elements(), "minecraft:block/block")
    write(MODELS / "item/swatch_typewriter.json", item)
    states = {"variants": {f"facing={f}": {"model": "createswatches:block/swatch_typewriter/block", **({"y": r} if r else {})}
                           for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}
    write(ASSETS / "blockstates/swatch_typewriter.json", states)
    write(MODELS / "item/swatch_card.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "createswatches:item/swatch_card"}})
    card()


def card():
    """Paper card with a row of colour chips and typed lines."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = img.load()
    for y in range(3, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (3, 13)
            p[x, y] = (196, 180, 150, 255) if edge else (242, 234, 214, 255)
    chips = [(214, 92, 64), (232, 176, 72), (110, 160, 90), (70, 120, 170)]
    for i, c in enumerate(chips):
        for dx in range(2):
            for dy in range(2):
                p[3 + i * 2 + i // 2 * 0 + dx + (i * 0), 5 + dy] = c + (255,)
    for x in range(4, 12):
        p[x, 9] = (120, 108, 96, 255)
    for x in range(4, 10):
        p[x, 11] = (150, 140, 128, 255)
    (ASSETS / "textures/item").mkdir(parents=True, exist_ok=True)
    img.save(ASSETS / "textures/item/swatch_card.png")


if __name__ == "__main__":
    main()
