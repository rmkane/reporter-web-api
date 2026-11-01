#!/usr/bin/env python3
"""Draw a specimen card for each theme, like a font specimen for colors.

    scripts/theme/generate_previews.py            # every theme in themes/
    scripts/theme/generate_previews.py ember rose # only these

Each card shows light and dark mode side by side: the logo, brand swatches,
status pills, a sample card with buttons, and the neutrals. Cards go to
themes/previews/<name>.png and a gallery to themes/README.md. Files are only
rewritten when their pixels or text change. Requires Pillow.
"""

import argparse
import io
import sys
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from generate_icons import MASTER, rasterize, report, ring
from themes import (
    SOFT, THEMES, InvalidTheme, available_themes, display, load_theme, mix, resolve, rgb, status,
    write_text,
)

PREVIEWS = THEMES / "previews"
GALLERY = THEMES / "README.md"

PANEL_WIDTH = 500
HEIGHT = 400
PAD = 28
GAP = 12

BRAND = (("primary", "Primary"), ("secondary", "Secondary"), ("accent", "Accent"))
STATUS = (("success", "Success"), ("warning", "Warning"), ("danger", "Danger"), ("info", "Info"))
NEUTRALS = ("background", "surface", "surface-muted", "border", "text", "text-muted")


@dataclass(frozen=True)
class Fonts:
    title: ImageFont.FreeTypeFont
    body: ImageFont.FreeTypeFont
    small: ImageFont.FreeTypeFont

    @classmethod
    def load(cls) -> "Fonts":
        # Pillow's bundled font keeps the cards identical across machines.
        return cls(*(ImageFont.load_default(size) for size in (22, 15, 12)))


# --- Drawing -----------------------------------------------------------------


def draw_logo(card: Image.Image, icon: dict, x: int, y: int, dark: bool, size: int = 40) -> None:
    """The dark half adds the ring, as the SVG does on dark pages."""
    shapes = report(icon) + ((ring(icon),) if dark else ())
    logo = rasterize(shapes, MASTER).resize((size, size), Image.Resampling.LANCZOS)
    card.paste(logo, (x, y), logo)


def draw_header(draw: ImageDraw.ImageDraw, card: Image.Image, theme: dict, mode: str,
                colors: dict, x: int, fonts: Fonts) -> None:
    draw_logo(card, theme["icon"], x + PAD, 24, dark=mode == "dark")
    draw.text((x + PAD + 52, 24), theme["name"], font=fonts.title, fill=rgb(colors["text"]))
    draw.text((x + PAD + 52, 52), mode.capitalize(), font=fonts.small, fill=rgb(colors["text-muted"]))


def draw_brand(draw: ImageDraw.ImageDraw, colors: dict, x: int, fonts: Fonts) -> None:
    width = (PANEL_WIDTH - 2 * PAD - 2 * GAP) // 3
    for index, (token, label) in enumerate(BRAND):
        left = x + PAD + index * (width + GAP)
        box = (left, 92, left + width, 156)
        draw.rounded_rectangle(box, radius=10, fill=rgb(colors[token]))
        ink = rgb(colors[f"on-{token}"])
        draw.text((left + 12, 104), label, font=fonts.body, fill=ink)
        draw.text((left + 12, 128), colors[token], font=fonts.small, fill=ink)


def draw_status(draw: ImageDraw.ImageDraw, colors: dict, x: int, fonts: Fonts) -> None:
    left = x + PAD
    for token, label in STATUS:
        width = round(draw.textlength(label, font=fonts.small)) + 24
        soft = rgb(mix(colors[token], colors["surface"], SOFT[token]))
        draw.rounded_rectangle((left, 172, left + width, 198), radius=13, fill=soft)
        draw.text((left + 12, 185), label, font=fonts.small, fill=rgb(colors[token]), anchor="lm")
        left += width + 8


def draw_sample(draw: ImageDraw.ImageDraw, colors: dict, x: int, fonts: Fonts) -> None:
    left, top, right = x + PAD, 214, x + PANEL_WIDTH - PAD
    draw.rounded_rectangle(
        (left, top, right, top + 116), radius=12, fill=rgb(colors["surface"]),
        outline=rgb(colors["border"]),
    )
    draw.text((left + 16, top + 14), "Login fails on Safari", font=fonts.body, fill=rgb(colors["text"]))
    draw.text((left + 16, top + 38), "RPT-42 · updated 2 hours ago", font=fonts.small,
              fill=rgb(colors["text-muted"]))
    button_top = top + 70
    draw.rounded_rectangle((left + 16, button_top, left + 96, button_top + 32), radius=6,
                           fill=rgb(colors["primary"]))
    draw.text((left + 56, button_top + 16), "Save", font=fonts.small, fill=rgb(colors["on-primary"]),
              anchor="mm")
    draw.rounded_rectangle((left + 106, button_top, left + 196, button_top + 32), radius=6,
                           fill=rgb(colors["surface"]), outline=rgb(colors["border"]))
    draw.text((left + 151, button_top + 16), "Cancel", font=fonts.small, fill=rgb(colors["text"]),
              anchor="mm")
    draw.text((left + 214, button_top + 16), "View report", font=fonts.small,
              fill=rgb(colors["primary"]), anchor="lm")


def draw_neutrals(draw: ImageDraw.ImageDraw, colors: dict, x: int, fonts: Fonts) -> None:
    draw.text((x + PAD, 360), "Neutrals", font=fonts.small, fill=rgb(colors["text-muted"]), anchor="lm")
    left = x + PAD + 72
    for token in NEUTRALS:
        draw.rounded_rectangle((left, 346, left + 28, 374), radius=6, fill=rgb(colors[token]),
                               outline=rgb(colors["border"]))
        left += 36


def draw_panel(card: Image.Image, theme: dict, mode: str, x: int, fonts: Fonts) -> None:
    colors = theme[mode]
    draw = ImageDraw.Draw(card)
    draw.rectangle((x, 0, x + PANEL_WIDTH, HEIGHT), fill=rgb(colors["background"]))
    draw_header(draw, card, theme, mode, colors, x, fonts)
    draw_brand(draw, colors, x, fonts)
    draw_status(draw, colors, x, fonts)
    draw_sample(draw, colors, x, fonts)
    draw_neutrals(draw, colors, x, fonts)


def render_card(theme: dict, fonts: Fonts) -> Image.Image:
    card = Image.new("RGB", (2 * PANEL_WIDTH, HEIGHT))
    draw_panel(card, theme, "light", 0, fonts)
    draw_panel(card, theme, "dark", PANEL_WIDTH, fonts)
    return card


# --- Files -------------------------------------------------------------------


def same_pixels(path: Path, image: Image.Image) -> bool:
    if not path.exists():
        return False
    with Image.open(path) as existing:
        return existing.size == image.size and existing.convert("RGB").tobytes() == image.tobytes()


def write_card(path: Path, image: Image.Image) -> bool:
    """Write the PNG unless it already has these pixels. Returns whether it wrote."""
    if same_pixels(path, image):
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    buffer = io.BytesIO()
    image.save(buffer, format="PNG", optimize=True)
    path.write_bytes(buffer.getvalue())
    return True


def gallery(themes: list[tuple[str, dict]]) -> str:
    lines = [
        "# Themes",
        "",
        "<!-- Generated by scripts/theme/generate_previews.py; run make previews after editing a"
        " theme. -->",
        "",
        "Apply one with `make theme THEME=<name>`. Each preview shows light and dark mode.",
        "",
    ]
    for name, theme in themes:
        lines += [f"## {name}", "", theme.get("description", ""), "",
                  f"![{name} theme preview](previews/{name}.png)", ""]
    return "\n".join(lines)


# --- Command line ------------------------------------------------------------


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("themes", nargs="*", help="theme names in themes/; default all")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    names = args.themes or [path.stem for path in available_themes()]
    fonts = Fonts.load()
    try:
        themes = [(name, load_theme(resolve(name))) for name in names]
    except InvalidTheme as error:
        print(error, file=sys.stderr)
        return 1
    for name, theme in themes:
        path = PREVIEWS / f"{name}.png"
        print(f"{status(write_card(path, render_card(theme, fonts)))} {display(path)}")
    if not args.themes:
        all_themes = [(path.stem, load_theme(path)) for path in available_themes()]
        print(f"{status(write_text(GALLERY, gallery(all_themes)))} {display(GALLERY)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
