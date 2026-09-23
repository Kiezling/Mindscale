"""Render the frozen MindScale launcher and Play artwork from one geometric mark.

Run with the bundled workspace Python (Pillow required) from any directory.
"""

from pathlib import Path
from xml.sax.saxutils import escape

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[3]
RES = ROOT / "app/src/main/res"
OUT = Path(__file__).resolve().parent
INK = "#17130C"
GOLD = "#E3CE9F"
MARK = [(34, 70), (34, 38), (41, 38), (54, 55), (67, 38), (74, 38),
        (74, 70), (66, 70), (66, 51), (54, 66), (42, 51), (42, 70)]
PATH = "M34,70L34,38L41,38L54,55L67,38L74,38L74,70L66,70L66,51L54,66L42,51L42,70Z"
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def vector(filename: str, color: str, background: bool = False) -> None:
    path = "M0,0h108v108h-108z" if background else PATH
    content = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
               "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
               "    android:width=\"108dp\"\n"
               "    android:height=\"108dp\"\n"
               "    android:viewportWidth=\"108\"\n"
               "    android:viewportHeight=\"108\">\n"
               f"    <path android:fillColor=\"{escape(color)}\" android:pathData=\"{path}\" />\n"
               "</vector>\n")
    (RES / "drawable" / filename).write_text(content, encoding="utf-8")


def mark(canvas: Image.Image, box: tuple[int, int, int, int], color: str = GOLD) -> None:
    x, y, width, height = box
    scale = 4
    layer = Image.new("RGBA", (width * scale, height * scale), (0, 0, 0, 0))
    points = [((px - 34) * width * scale / 40, (py - 38) * height * scale / 32)
              for px, py in MARK]
    ImageDraw.Draw(layer).polygon(points, fill=color)
    layer = layer.resize((width, height), Image.Resampling.LANCZOS)
    canvas.paste(layer, (x, y), layer)


def icon(size: int, round_icon: bool = False) -> Image.Image:
    scale = 4
    large = size * scale
    image = Image.new("RGBA", (large, large), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if round_icon:
        draw.ellipse((0, 0, large - 1, large - 1), fill=INK)
    else:
        draw.rectangle((0, 0, large, large), fill=INK)
    # Same proportions as the 108dp adaptive vector, with its full safe-area margin.
    mark(image, (int(34 / 108 * large), int(38 / 108 * large),
                 int(40 / 108 * large), int(32 / 108 * large)))
    return image.resize((size, size), Image.Resampling.LANCZOS)


def feature() -> Image.Image:
    image = Image.new("RGB", (1024, 500), INK)
    draw = ImageDraw.Draw(image)
    # The monogram is centered in a broad left field; text clears its right edge.
    mark(image, (103, 123, 250, 200))
    font_dir = RES / "font"
    title_font = ImageFont.truetype(str(font_dir / "instrument_sans_semibold.ttf"), 79)
    subtitle_font = ImageFont.truetype(str(font_dir / "instrument_sans_regular.ttf"), 33)
    draw.text((420, 158), "MindScale", font=title_font, fill=GOLD)
    draw.text((422, 266), "Your symptoms. Your record.", font=subtitle_font, fill=GOLD)
    return image


def main() -> None:
    vector("ic_launcher_background.xml", INK, background=True)
    vector("ic_launcher_foreground.xml", GOLD)
    vector("ic_launcher_monochrome.xml", "#FFFFFF")
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        content = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                   "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                   "    <background android:drawable=\"@drawable/ic_launcher_background\" />\n"
                   "    <foreground android:drawable=\"@drawable/ic_launcher_foreground\" />\n"
                   "    <monochrome android:drawable=\"@drawable/ic_launcher_monochrome\" />\n"
                   "</adaptive-icon>\n")
        (RES / "mipmap-anydpi" / name).write_text(content, encoding="utf-8")
    for density, size in SIZES.items():
        for round_icon in (False, True):
            name = "ic_launcher_round.webp" if round_icon else "ic_launcher.webp"
            icon(size, round_icon).save(RES / f"mipmap-{density}" / name,
                                        format="WEBP", lossless=True, method=6)
    icon(512).convert("RGB").save(OUT / "mindscale-store-icon-512.png", format="PNG")
    feature().save(OUT / "mindscale-feature-1024x500.png", format="PNG")


if __name__ == "__main__":
    main()
