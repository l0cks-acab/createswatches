"""Builds the Swatch Carousel's models and its own textures.

Run from the repo root: python3 tools/carousel_model.py
The block model is the ribbed base and the centre monitor; the renderer draws the spinning tile ring
and the palette on the screen. The item model adds sixteen fixed tiles.
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


def cylinder(y0, y1, tex, top="top", inset=0.0, name=None):
    """Round-ish disc out of three overlapping boxes (octagon). Sides use the rib texture."""
    els = []
    for i, (a, b) in enumerate(((0.5, 3), (3, 0.5), (1.5, 1.5))):
        a += inset
        b += inset
        els.append(box((a, y0, b), (16 - a, y1, 16 - b), tex,
                       uv={"north": [0, 0, 16, y1 - y0], "south": [0, 0, 16, y1 - y0], "east": [0, 0, 16, y1 - y0],
                           "west": [0, 0, 16, y1 - y0], "up": [a, b, 16 - a, 16 - b], "down": [a, b, 16 - a, 16 - b]},
                       name=f"{name}_{i}" if name else None))
        els[-1]["faces"]["up"]["texture"] = "#" + top
        els[-1]["faces"]["down"]["texture"] = "#andesite"
    return els


# Low base like a filament swatch stand; the back shaft is only a short stub so the ring clears it.
BASE_TOP = 6
TILE_TOP = 9.4


def base_elements():
    els = cylinder(0, BASE_TOP, "ribs", name="base")
    els += cylinder(BASE_TOP, BASE_TOP + 0.6, "brass_block", top="top", inset=2.2, name="hub")
    # Terminal monitor in the middle; screen faces north (towards the player in the model frame).
    els += [
        box((7, 6.6, 7), (9, 7.6, 9), "andesite", name="neck"),
        box((4.5, 7.6, 5.5), (11.5, 13.6, 10.5), "brass", name="monitor"),
        box((5.2, 8.3, 5.1), (10.8, 12.9, 5.5), "screen", name="screen",
            uv={"north": [0, 0, 16, 11], "south": [0, 0, 16, 11], "east": [0, 0, 1, 11], "west": [0, 0, 1, 11],
                "up": [0, 0, 16, 1], "down": [0, 0, 16, 1]}),
        box((5, 13.6, 6), (11, 14.2, 10), "andesite", faces="nsewu", name="vent"),
        box((11.5, 8.8, 8.6), (12.1, 9.6, 9.6), "brass_block", name="knob_a"),
        box((11.5, 10.2, 8.6), (12.1, 11.0, 9.6), "brass_block", name="knob_b"),
    ]
    return els


def item_tiles():
    """Sixteen tiles at fixed angles for the item model; the placed block draws its own spinning ring."""
    els = []
    for k in range(16):
        quarter, step = divmod(k, 4)
        angle = (-22.5, 0, 22.5, 45)[step]
        x0 = 7.5
        el = box((x0, BASE_TOP, 0.7), (x0 + 0.7, TILE_TOP, 3.0), "tiles",
                 uv={f: [k, 0, k + 1, 4] for f in ("north", "south", "east", "west", "up", "down")})
        # Rotate the north tile round the centre to the other quarters by remapping coords.
        if quarter:
            for _ in range(quarter):
                (fx, fy, fz), (tx, ty, tz) = el["from"], el["to"]
                el["from"], el["to"] = [16 - tz, fy, fx], [16 - fz, ty, tx]
        el["rotation"] = {"angle": angle, "axis": "y", "origin": [8, BASE_TOP, 8]}
        els.append(el)
    return els


TEXTURES = {
    "andesite": ANDESITE, "brass": BRASS, "brass_block": BRASS_BLOCK,
    "ribs": "createswatches:block/carousel_ribs", "top": "createswatches:block/carousel_top",
    "screen": "createswatches:block/carousel_screen", "tiles": "createswatches:block/carousel_tiles",
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
    import colorsys
    rnd = random.Random(7)
    TEX.mkdir(parents=True, exist_ok=True)
    for old in ("carousel_key", "carousel_platen", "carousel_paper"):
        (TEX / f"{old}.png").unlink(missing_ok=True)
    # Ribs: brass flutes, light crest / dark groove every 2px, darker band top and bottom.
    ribs = Image.new("RGBA", (16, 16))
    p = ribs.load()
    for y in range(16):
        for x in range(16):
            base = [(226, 182, 96), (196, 148, 70)][x % 2]
            if x % 4 == 3:
                base = (150, 104, 48)
            if y == 0:
                base = (240, 206, 128)
            if y >= 3 and y % 4 == 3:
                base = tuple(int(c * 0.82) for c in base)
            v = rnd.randint(-4, 4)
            p[x, y] = tuple(max(0, min(255, c + v)) for c in base) + (255,)
    ribs.save(TEX / "carousel_ribs.png")
    # Top: dark walnut with a brass rim ring drawn by distance from centre.
    top = Image.new("RGBA", (16, 16))
    p = top.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            v = rnd.randint(-5, 5)
            if d > 7.2:
                c = (200, 154, 74)
            elif d > 6.6:
                c = (120, 84, 40)
            else:
                c = (62 + v, 44 + v, 30 + v) if (y + (x // 5)) % 3 else (54 + v, 38 + v, 26 + v)
            p[x, y] = c + (255,)
    top.save(TEX / "carousel_top.png")
    # Screen: dark green glass with scanlines and a highlight; the renderer draws chips over it.
    scr = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = scr.load()
    for y in range(11):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 10)
            if edge:
                p[x, y] = (40, 34, 28, 255)
            else:
                g = 34 if y % 2 else 42
                p[x, y] = (14, g, 26, 255)
    for i in range(3):
        p[2 + i, 2] = (70, 110, 80, 255)
    scr.save(TEX / "carousel_screen.png")
    # Tiles: 16 hue columns for the item model, lit top rows.
    tiles = Image.new("RGBA", (16, 16))
    p = tiles.load()
    for x in range(16):
        r, g, b = colorsys.hsv_to_rgb(x / 16, 0.75, 0.92)
        for y in range(16):
            k = 1.1 if y == 0 else (0.85 if y > 2 else 1.0)
            p[x, y] = tuple(min(255, int(c * 255 * k)) for c in (r, g, b)) + (255,)
    tiles.save(TEX / "carousel_tiles.png")

    Image.new("RGBA", (4, 4), (255, 255, 255, 255)).save(TEX / "chip.png")


def main():
    textures()
    for old in ("keys", "carriage"):
        (MODELS / f"block/swatch_carousel/{old}.json").unlink(missing_ok=True)
    write(MODELS / "block/swatch_carousel/block.json", model(base_elements(), "minecraft:block/block"))
    write(MODELS / "block/swatch_carousel/shaft_stub.json", {
        "textures": {"0": "create:block/axis", "1": "create:block/axis_top", "particle": "create:block/axis"},
        "elements": [{"from": [6, 6, 15.4], "to": [10, 10, 16], "faces": {
            "north": {"uv": [6, 6, 10, 10], "rotation": 180, "texture": "#1"},
            "east": {"uv": [6, 0, 10, 0.6], "rotation": 270, "texture": "#0"},
            "south": {"uv": [6, 6, 10, 10], "texture": "#1"},
            "west": {"uv": [6, 0, 10, 0.6], "rotation": 90, "texture": "#0"},
            "up": {"uv": [6, 0, 10, 0.6], "rotation": 180, "texture": "#0"},
            "down": {"uv": [6, 0, 10, 0.6], "texture": "#0"}}}]})
    write(MODELS / "item/swatch_carousel.json", model(base_elements() + item_tiles(), "minecraft:block/block"))
    states = {"variants": {f"facing={f}": {"model": "createswatches:block/swatch_carousel/block", **({"y": r} if r else {})}
                           for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}
    write(ASSETS / "blockstates/swatch_carousel.json", states)
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
