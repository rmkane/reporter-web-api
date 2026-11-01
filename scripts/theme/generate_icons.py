#!/usr/bin/env python3
"""Create the report favicon from one list of shapes, in a theme's colors.

Shapes draw themselves as SVG and as pixels. The SVG is written directly.
The pixel image is drawn at 256 pixels and downscaled with Lanczos into
favicon.ico at 16, 32, and 48. Colors come from the theme's "icon" section.

The ICO needs Pillow (brew install pillow on macOS); without it, only the SVG
is written. Each file is rewritten only when its content changes.

    scripts/theme/generate_icons.py --theme ember
"""

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Protocol

from themes import ROOT, InvalidTheme, display, load_theme, resolve, status, write_text

SVG_PATH = ROOT / "src/main/resources/static/favicon.svg"
ICO_PATH = ROOT / "src/main/resources/static/favicon.ico"
VIEW = 64
MASTER = 256
SIZES = (16, 32, 48)

DEFAULT_THEME = "ember"


class Shape(Protocol):
    def svg(self) -> str: ...

    def draw(self, canvas, scale: float) -> None: ...


@dataclass(frozen=True)
class Rect:
    x: float
    y: float
    width: float
    height: float
    radius: float
    fill: str

    def svg(self) -> str:
        return (
            f'<rect x="{num(self.x)}" y="{num(self.y)}" '
            f'width="{num(self.width)}" height="{num(self.height)}" '
            f'rx="{num(self.radius)}" fill="{self.fill}"/>'
        )

    def draw(self, canvas, scale: float) -> None:
        box = (
            self.x * scale,
            self.y * scale,
            (self.x + self.width) * scale,
            (self.y + self.height) * scale,
        )
        canvas.rounded_rectangle(box, radius=self.radius * scale, fill=rgba(self.fill))


@dataclass(frozen=True)
class Polygon:
    points: tuple[tuple[float, float], ...]
    fill: str

    def svg(self) -> str:
        pairs = " ".join(f"{num(x)},{num(y)}" for x, y in self.points)
        return f'<polygon points="{pairs}" fill="{self.fill}"/>'

    def draw(self, canvas, scale: float) -> None:
        scaled = [(x * scale, y * scale) for x, y in self.points]
        canvas.polygon(scaled, fill=rgba(self.fill))


@dataclass(frozen=True)
class Ring:
    """An outline around the tile, so it stays visible on dark backgrounds."""

    fill: str
    width: float = 2

    def svg(self) -> str:
        inset = self.width / 2
        return (
            f'<rect class="ring" x="{num(inset)}" y="{num(inset)}" '
            f'width="{num(VIEW - self.width)}" height="{num(VIEW - self.width)}" '
            f'rx="{num(12 - inset)}" fill="none" stroke="{self.fill}" '
            f'stroke-width="{num(self.width)}"/>'
        )

    def draw(self, canvas, scale: float) -> None:
        canvas.rounded_rectangle(
            (0, 0, VIEW * scale - 1, VIEW * scale - 1),
            radius=12 * scale,
            outline=rgba(self.fill),
            width=round(self.width * scale),
        )


# Shows the ring only when the viewer prefers dark mode, in the page header and browser tabs.
DARK_RING_STYLE = (
    "<style>.ring{display:none}"
    "@media (prefers-color-scheme:dark){.ring{display:inline}}</style>"
)


def ring(colors: dict[str, str]) -> Ring:
    return Ring(colors["ring"])


def report(colors: dict[str, str]) -> tuple[Shape, ...]:
    base, bar, paper, fold, line = (colors[key] for key in ("base", "bar", "paper", "fold", "line"))
    return (
        Rect(0, 0, VIEW, VIEW, 12, base),
        Rect(14, 8, 36, 48, 4, paper),
        Polygon(((40, 8), (50, 8), (50, 18)), fold),
        Rect(20, 26, 16, 3.2, 1.6, base),
        Rect(20, 32, 24, 2.4, 1.2, line),
        Rect(20, 37, 18, 2.4, 1.2, line),
        Rect(20, 46, 4, 6, 1, bar),
        Rect(26, 42, 4, 10, 1, base),
        Rect(32, 44, 4, 8, 1, bar),
    )


def svg_document(shapes: tuple[Shape, ...], outline: Ring | None = None) -> str:
    parts = [DARK_RING_STYLE] if outline else []
    parts += [shape.svg() for shape in shapes]
    parts += [outline.svg()] if outline else []
    body = "\n".join(f"  {part}" for part in parts)
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {VIEW} {VIEW}" '
        f'role="img" aria-label="Report">\n{body}\n</svg>\n'
    )


def write_svg(path: Path, shapes: tuple[Shape, ...], outline: Ring | None = None) -> bool:
    """Write the SVG unless it already has this content. Returns whether it wrote."""
    return write_text(path, svg_document(shapes, outline))


def rasterize(shapes: tuple[Shape, ...], master: int):
    from PIL import Image, ImageDraw

    image = Image.new("RGBA", (master, master), (0, 0, 0, 0))
    canvas = ImageDraw.Draw(image)
    scale = master / VIEW
    for shape in shapes:
        shape.draw(canvas, scale)
    return image


def downscale(image, size: int):
    from PIL import Image

    return image.resize((size, size), Image.Resampling.LANCZOS)


def same_pixels(path: Path, frames) -> bool:
    """Whether the ICO at path holds exactly these frames, compared pixel by pixel.

    ICO bytes vary between Pillow versions, so comparing pixels keeps a
    regenerated icon from showing as changed when it looks the same.
    """
    from PIL import Image

    if not path.exists():
        return False
    with Image.open(path) as existing:
        if set(existing.info.get("sizes", ())) != {frame.size for frame in frames}:
            return False
        for frame in frames:
            existing.size = frame.size
            existing.load()
            if existing.convert("RGBA").tobytes() != frame.convert("RGBA").tobytes():
                return False
    return True


def write_ico(master, path: Path, sizes: tuple[int, ...]) -> bool:
    """Write the ICO unless it already has these pixels. Returns whether it wrote."""
    ordered = tuple(sorted(sizes, reverse=True))
    frames = [downscale(master, size) for size in ordered]
    if same_pixels(path, frames):
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    frames[0].save(
        path,
        format="ICO",
        sizes=[(size, size) for size in ordered],
        append_images=frames[1:],
    )
    return True


def num(value: float) -> str:
    return f"{value:g}"


def rgba(fill: str) -> tuple[int, int, int, int]:
    hex_color = fill.removeprefix("#")
    return (
        int(hex_color[0:2], 16),
        int(hex_color[2:4], 16),
        int(hex_color[4:6], 16),
        255,
    )


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--theme", default=DEFAULT_THEME, help="theme name in themes/ or a JSON path")
    parser.add_argument("--svg", type=Path, default=SVG_PATH)
    parser.add_argument("--ico", type=Path, default=ICO_PATH)
    parser.add_argument("--sizes", type=icon_sizes, default=SIZES)
    return parser.parse_args(argv)


def icon_sizes(value: str) -> tuple[int, ...]:
    return tuple(int(size) for size in value.split(",") if size.strip())


def has_pillow() -> bool:
    try:
        import PIL  # noqa: F401
    except ImportError:
        return False
    return True


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        colors = load_theme(resolve(args.theme))["icon"]
    except InvalidTheme as error:
        print(error, file=sys.stderr)
        return 1
    shapes = report(colors)
    # The SVG adds the dark-mode ring; the ICO is a fixed bitmap, so it goes without.
    print(f"{status(write_svg(args.svg, shapes, ring(colors)))} {display(args.svg)}")
    if not has_pillow():
        print(f"Skipped {display(args.ico)}: install Pillow to redraw it (brew install pillow)")
        return 0
    wrote = write_ico(rasterize(shapes, MASTER), args.ico, args.sizes)
    listed = ", ".join(str(size) for size in sorted(args.sizes))
    print(f"{status(wrote)} {display(args.ico)} ({listed})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
