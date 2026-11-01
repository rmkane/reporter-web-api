"""Theme model shared by the theme scripts: tokens, loading, validation, contrast.

A theme file in themes/ defines every token in TOKENS: colors and shadows for
light and dark, shared shape and font values, and logo colors. Loading a theme
checks that it defines exactly those tokens and that every color pairing the UI
draws meets WCAG AA contrast (4.5:1 for text, 3:1 for icons).
"""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
THEMES = ROOT / "themes"

TEXT = 4.5  # WCAG AA for normal-size text
GRAPHIC = 3.0  # WCAG AA for icons and other non-text marks

# Token groups, in output order. Mode tokens differ between light and dark.
MODE_GROUPS = [
    ("Brand", ["primary", "on-primary", "secondary", "on-secondary", "accent", "on-accent"]),
    ("Status", ["success", "warning", "danger", "on-danger", "info"]),
    ("Neutrals", ["background", "surface", "surface-muted", "border", "text", "text-muted"]),
    ("Depth", ["shadow-sm", "shadow"]),
]
SHARED_GROUPS = [
    ("Shape", ["radius-sm", "radius", "radius-lg"]),
    ("Type", ["font-sans", "font-mono"]),
]
# Logo colors: tile, accent bars, page, page fold, text lines, and the outline
# drawn around the tile on dark backgrounds.
ICON_TOKENS = ["base", "bar", "paper", "fold", "line", "ring"]
TOKENS = {
    "light": [token for _, tokens in MODE_GROUPS for token in tokens],
    "dark": [token for _, tokens in MODE_GROUPS for token in tokens],
    "shared": [token for _, tokens in SHARED_GROUPS for token in tokens],
    "icon": ICON_TOKENS,
}

# Derived tints and hovers. generate_css.py writes them as color-mix() rules and
# the contrast check blends the same amounts, so both see the same colors.
SOFT = {"primary": 0.14, "secondary": 0.14, "accent": 0.14, "success": 0.14, "warning": 0.16,
        "danger": 0.14, "info": 0.14}
HOVER = {"primary": 0.85, "danger": 0.85}
FOCUS_RING = 0.35


class InvalidTheme(Exception):
    """A theme with missing or unknown tokens, or a pairing below WCAG AA."""


# --- Loading -----------------------------------------------------------------


def resolve(theme: str) -> Path:
    """Accept a theme name in themes/ or a path to a JSON file."""
    path = Path(theme)
    return path if path.suffix == ".json" else THEMES / f"{theme}.json"


def available_themes() -> list[Path]:
    return sorted(THEMES.glob("*.json"))


def load_theme(path: Path) -> dict:
    try:
        theme = json.loads(path.read_text())
    except FileNotFoundError as error:
        raise InvalidTheme(f"{path}: not found; see make themes") from error
    check_tokens(theme, path)
    check_contrast(theme, path)
    return theme


def check_tokens(theme: dict, path: Path) -> None:
    """Report every section's missing and unknown tokens at once."""
    problems = []
    for section, expected in TOKENS.items():
        actual = set(theme.get(section, {}))
        missing = [token for token in expected if token not in actual]
        unknown = sorted(actual - set(expected))
        if missing:
            problems.append(f"{section}: missing {', '.join(missing)}")
        if unknown:
            problems.append(f"{section}: unknown {', '.join(unknown)}")
    if problems:
        raise InvalidTheme("\n".join(f"{path}: {problem}" for problem in problems))


# --- Colors and contrast -----------------------------------------------------


def rgb(color: str) -> tuple[int, int, int]:
    value = color.removeprefix("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))


def mix(color: str, base: str, amount: float) -> str:
    """Approximates color-mix(in oklch, ...) with an sRGB blend; close enough to gate on."""
    return "#" + "".join(
        f"{round(c * amount + b * (1 - amount)):02x}" for c, b in zip(rgb(color), rgb(base))
    )


def luminance(color: str) -> float:
    channels = [value / 255 for value in rgb(color)]
    linear = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]


def contrast(first: str, second: str) -> float:
    lighter, darker = sorted((luminance(first), luminance(second)), reverse=True)
    return (lighter + 0.05) / (darker + 0.05)


def contrast_pairs(colors: dict[str, str]) -> list[tuple[str, str, str, float]]:
    """(label, foreground, background, minimum) for every pairing the UI draws."""
    surface = colors["surface"]
    soft = {token: mix(colors[token], surface, amount) for token, amount in SOFT.items()}
    hover = {token: mix(colors[token], colors["text"], amount) for token, amount in HOVER.items()}
    pairs = [(f"on-{token} on {token}", colors[f"on-{token}"], colors[token], TEXT)
             for token in ("primary", "secondary", "accent", "danger")]
    pairs += [(f"on-{token} on {token} hover", colors[f"on-{token}"], hover[token], TEXT)
              for token in hover]
    pairs += [
        ("text on background", colors["text"], colors["background"], TEXT),
        ("text on surface", colors["text"], surface, TEXT),
        ("text-muted on surface", colors["text-muted"], surface, TEXT),
        ("text-muted on surface-muted", colors["text-muted"], colors["surface-muted"], TEXT),
        ("primary links on surface", colors["primary"], surface, TEXT),
        ("accent badges on surface", colors["accent"], surface, TEXT),
        ("secondary tags on their tint", colors["secondary"], soft["secondary"], TEXT),
        ("primary glyph on its tint", colors["primary"], soft["primary"], GRAPHIC),
    ]
    pairs += [(f"{token} badges on their tint", colors[token], soft[token], TEXT)
              for token in ("success", "warning", "danger", "info")]
    return pairs


def check_contrast(theme: dict, path: Path) -> None:
    problems = [
        f"{mode}: {label} is {ratio:.2f}:1, needs {minimum}:1"
        for mode in ("light", "dark")
        for label, fg, bg, minimum in contrast_pairs(theme[mode])
        if (ratio := contrast(fg, bg)) < minimum
    ]
    ring = contrast(theme["icon"]["ring"], theme["dark"]["background"])
    if ring < GRAPHIC:
        problems.append(f"icon: ring on the dark background is {ring:.2f}:1, needs {GRAPHIC}:1")
    if problems:
        raise InvalidTheme("\n".join(f"{path}: {problem}" for problem in problems))


# --- Output helpers ----------------------------------------------------------


def display(path: Path) -> str:
    """A path relative to the repository root when it is inside it."""
    resolved = path.resolve()
    return str(resolved.relative_to(ROOT)) if resolved.is_relative_to(ROOT) else str(path)


def write_text(path: Path, text: str) -> bool:
    """Write the file unless it already has this text. Returns whether it wrote."""
    if path.exists() and path.read_text(encoding="utf-8") == text:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    return True


def status(wrote: bool) -> str:
    return "Wrote" if wrote else "Unchanged"
