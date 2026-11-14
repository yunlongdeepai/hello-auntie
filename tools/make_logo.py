# -*- coding: utf-8 -*-
"""Generate the minimal 'hello auntie' typographic logo and launcher icons."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "assets"
RES = ROOT / "android" / "app" / "src" / "main" / "res"
FONT = r"C:\Windows\Fonts\NotoSansSC-VF.ttf"

RED = (166, 43, 42, 255)
CREAM = (247, 232, 207, 255)
GOLD = (216, 162, 74, 255)
SS = 4


def font(size, weight):
    f = ImageFont.truetype(FONT, size)
    f.set_variation_by_axes([weight])
    return f


def rounded_icon(size, text="ha"):
    """Red squircle with a cream monogram and one gold full stop."""
    k = size / 1024 * SS
    canvas = Image.new("RGBA", (int(size * SS), int(size * SS)), (0, 0, 0, 0))
    d = ImageDraw.Draw(canvas)

    d.rounded_rectangle(
        [0, 0, int(size * SS), int(size * SS)],
        radius=228 * k,
        fill=RED,
    )

    # monogram, centered slightly above the optical centre
    f = font(int(420 * k), 900)
    d.text((512 * k, 500 * k), text, font=f, fill=CREAM, anchor="mm")

    # one small gold full stop: the only decorative element
    r = 15 * k
    cx, cy = 790 * k, 672 * k
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=GOLD)

    return canvas.resize((size, size), Image.LANCZOS)


def icon_foreground(size=432):
    k = size / 432 * SS
    canvas = Image.new("RGBA", (int(size * SS), int(size * SS)), (0, 0, 0, 0))
    d = ImageDraw.Draw(canvas)

    f = font(int(188 * k), 900)
    d.text((196 * k, 204 * k), "ha", font=f, fill=CREAM, anchor="mm")

    r = 8 * k
    cx, cy = 326 * k, 308 * k
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=GOLD)

    return canvas.resize((size, size), Image.LANCZOS)


def legacy_icon(size, round_mask=False):
    big = size * SS
    radius = int(big * 0.223)
    icon = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(icon)
    if round_mask:
        d.ellipse((0, 0, big, big), fill=RED)
    else:
        d.rounded_rectangle((0, 0, big, big), radius=radius, fill=RED)

    fg = icon_foreground(432).resize((big, big), Image.LANCZOS)
    icon.alpha_composite(fg)

    mask = Image.new("L", icon.size, 0)
    md = ImageDraw.Draw(mask)
    if round_mask:
        md.ellipse((0, 0, big, big), fill=255)
    else:
        md.rounded_rectangle((0, 0, big, big), radius=radius, fill=255)
    icon.putalpha(mask)
    return icon.resize((size, size), Image.LANCZOS)


def wordmark():
    W, H = 1400, 360
    k = SS
    canvas = Image.new("RGBA", (W * k, H * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(canvas)

    light = font(int(190 * k), 300)
    bold = font(int(190 * k), 900)
    text_y = 245 * k

    hello = "hello"
    auntie = "auntie"
    hello_w = d.textlength(hello, font=light)
    auntie_w = d.textlength(auntie, font=bold)
    gap = 42 * k
    dot_gap = 58 * k
    dot_r = 14 * k
    total = hello_w + gap + auntie_w + dot_gap + 2 * dot_r
    x = (W * k - total) / 2

    d.text((x, text_y), hello, font=light, fill=RED, anchor="ls")
    x += hello_w + gap
    d.text((x, text_y), auntie, font=bold, fill=RED, anchor="ls")
    x += auntie_w + dot_gap
    d.ellipse((x, text_y - 2 * dot_r - 8 * k, x + 2 * dot_r, text_y - 8 * k), fill=GOLD)

    return canvas.resize((W, H), Image.LANCZOS)


def main():
    ASSETS.mkdir(parents=True, exist_ok=True)
    wordmark().save(ASSETS / "logo_wordmark.png")
    rounded_icon(1024).save(ASSETS / "logo_mark.png")

    icon_dir = RES / "mipmap-anydpi-v26"
    icon_dir.mkdir(parents=True, exist_ok=True)
    adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""
    (icon_dir / "ic_launcher.xml").write_text(adaptive, encoding="utf-8")
    (icon_dir / "ic_launcher_round.xml").write_text(adaptive, encoding="utf-8")

    drawable = RES / "drawable-nodpi"
    drawable.mkdir(parents=True, exist_ok=True)
    icon_foreground(432).save(drawable / "ic_launcher_foreground.png")

    for density, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        legacy_icon(px).save(folder / "ic_launcher.png")
        legacy_icon(px, round_mask=True).save(folder / "ic_launcher_round.png")

    (RES / "values" / "colors.xml").write_text(
        """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#A62B2A</color>
</resources>
""",
        encoding="utf-8",
    )
    print("logo assets written to", ASSETS)
    print("launcher icons written to", RES)


if __name__ == "__main__":
    main()
