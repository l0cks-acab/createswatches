"""Builds the Swatch Applicator's 3D item model: a brass and copper steampunk sprayer with an analogue gauge and a cog.

Run from the repo root: python3 tools/applicator_model.py
The renderer draws the selected block in the sample window on top (see ApplicatorRenderer WINDOW_*).
"""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/createswatches"

TEX = {
    "brass": "create:block/brass_block", "casing": "create:block/brass_casing", "andesite": "create:block/andesite_casing",
    "copper": "minecraft:block/copper_block", "wood": "minecraft:block/stripped_dark_oak_log",
    "cog_side": "minecraft:block/stripped_spruce_log", "cog_top": "minecraft:block/stripped_spruce_log_top",
    "gauge": "createswatches:item/applicator_gauge", "glass": "minecraft:block/tinted_glass",
}


def box(frm, to, tex, uv=None, rot=None, name=None, faces="nsewud"):
    names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
    x0, y0, z0 = frm
    x1, y1, z1 = to
    auto = {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
        "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0],
    }
    auto = {k: [max(0, min(16, v)) for v in uv_] for k, uv_ in auto.items()}
    el = {"from": list(frm), "to": list(to), "faces": {}}
    for f in faces:
        n = names[f]
        el["faces"][n] = {"uv": (uv or {}).get(n, auto[n]), "texture": "#" + tex}
    if rot:
        el["rotation"] = rot
    if name:
        el["name"] = name
    return el


def elements():
    els = [
        # Walnut grip with brass pommel and ferrule.
        box((6.6, 0.8, 7.1), (8.4, 7, 8.9), "wood", name="grip"),
        box((6.2, 0, 6.7), (8.8, 0.8, 9.3), "brass", name="pommel"),
        box((6.3, 6.2, 6.8), (8.7, 7, 9.2), "brass", name="ferrule"),
        # Trigger and guard.
        box((8.4, 5.2, 7.6), (9.2, 7, 8.4), "andesite", name="trigger"),
        box((8.4, 4.6, 7.7), (10.4, 5.0, 8.3), "andesite", name="guard"),
        box((10.0, 5.0, 7.7), (10.4, 7, 8.3), "andesite", name="guard_front"),
        # Brass body.
        box((4, 7, 6), (12, 11, 10), "casing", name="body"),
        # Copper reservoir at the back with brass caps.
        box((1, 7.5, 6.5), (4, 10.5, 9.5), "copper", name="tank"),
        box((0.4, 8, 7), (1, 10, 9), "brass", name="tank_cap"),
        # Andesite barrel and flared copper nozzle.
        box((12, 8, 6.9), (14.6, 10, 9.1), "andesite", name="barrel"),
        box((14.6, 7.5, 6.5), (16, 10.5, 9.5), "copper", name="nozzle"),
        box((15.6, 8.3, 7.3), (16.05, 9.7, 8.7), "andesite", name="nozzle_bore", faces="e"),
        # Sample window frame on top (the renderer draws the selected block inside it).
        box((5.5, 11, 6.5), (10.5, 11.6, 7.0), "brass", name="window_n"),
        box((5.5, 11, 9.0), (10.5, 11.6, 9.5), "brass", name="window_s"),
        box((5.5, 11, 7.0), (6.0, 11.6, 9.0), "brass", name="window_w"),
        box((10.0, 11, 7.0), (10.5, 11.6, 9.0), "brass", name="window_e"),
        # Pipe from the tank up to the gauge.
        box((2.1, 10.5, 7.6), (2.9, 11.6, 8.4), "brass", name="gauge_pipe"),
        # Analogue pressure gauge, dial facing both sides.
        box((1, 11.6, 7.4), (4, 14.6, 8.6), "brass", name="gauge",
            uv={"north": [0, 0, 16, 16], "south": [0, 0, 16, 16]}),
    ]
    els[-1]["faces"]["north"]["texture"] = els[-1]["faces"]["south"]["texture"] = "#gauge"
    # Cog on the body's side, like the gear on a Create wrench.
    tooth = {"north": [0, 0, 6, 1.4], "south": [0, 0, 6, 1.4], "east": [0, 0, 1, 1.4], "west": [0, 0, 1, 1.4],
             "up": [0, 0, 6, 1], "down": [0, 0, 6, 1]}
    for ang in (0, 45, -45, 90):
        e = box((5, 8.3, 5.0), (11, 9.7, 6.0), "cog_side", uv=tooth, name="cog_tooth")
        if ang == 90:
            e = box((7.3, 6, 5.0), (8.7, 12, 6.0), "cog_side", uv={k: [0, 0, 1.4, 6] if k in ("north", "south") else v for k, v in tooth.items()}, name="cog_tooth")
        elif ang:
            e["rotation"] = {"angle": ang, "axis": "z", "origin": [8, 9, 5.5]}
        els.append(e)
    hub = box((6.4, 7.4, 4.8), (9.6, 10.6, 6.0), "cog_top", name="cog_hub",
              uv={"north": [4, 4, 12, 12], "south": [4, 4, 12, 12]})
    els.append(hub)
    els.append(box((7.4, 8.4, 4.5), (8.6, 9.6, 4.8), "brass", name="cog_bolt"))
    return els


def gauge_texture():
    img = Image.new("RGBA", (16, 16))
    p = img.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 7.6:
                c = (150, 104, 48)
            elif d > 6.2:
                c = (226, 182, 96) if (x + y) % 3 else (196, 148, 70)
            else:
                c = (236, 226, 200)
            p[x, y] = c + (255,)
    # Tick marks, red zone, needle and pin.
    for x, y in [(3, 8), (4, 4), (8, 3), (12, 4), (13, 8)]:
        p[x, y] = (60, 50, 40, 255)
    for x, y in [(12, 9), (12, 10), (11, 11)]:
        p[x, y] = (180, 50, 40, 255)
    for x, y in [(7, 8), (8, 7), (9, 6), (10, 5)]:
        p[x, y] = (40, 30, 24, 255)
    p[7, 8] = p[8, 8] = (150, 104, 48, 255)
    (ASSETS / "textures/item").mkdir(parents=True, exist_ok=True)
    img.save(ASSETS / "textures/item/applicator_gauge.png")


def main():
    gauge_texture()
    els = elements()
    used = {f["texture"][1:] for e in els for f in e["faces"].values()}
    tex = {k: v for k, v in TEX.items() if k in used}
    tex["particle"] = TEX["brass"]
    out = {"textures": tex, "elements": els}
    (ASSETS / "models/item/swatch_applicator_base.json").write_text(json.dumps(out, indent=2) + "\n")
    (ASSETS / "textures/item/swatch_applicator.png").unlink(missing_ok=True)


if __name__ == "__main__":
    main()
