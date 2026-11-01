#!/usr/bin/env python3
"""Generate the UI theme stylesheet from a theme file.

    scripts/theme.py indigo              # a name in themes/
    scripts/theme.py path/to/theme.json  # or a path
    scripts/theme.py --list

A theme defines every token in TOKENS: colors and shadows for light and dark,
shared shape and font values, and logo colors. The script writes the CSS
variables to src/main/resources/static/css/theme.css; generate_icons.py draws
the logo from the same theme. Exits 1 on a theme with missing or unknown
tokens, or with a color pairing below WCAG AA contrast (4.5:1 for text, 3:1
for icons).
"""

import argparse
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
THEMES = ROOT / "themes"
OUTPUT = ROOT / "src/main/resources/static/css/theme.css"

EXIT_OK = 0
EXIT_INVALID = 1

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
# Logo colors, read by generate_icons.py: tile, accent bars, page, page fold, text lines,
# and the outline drawn around the tile on dark backgrounds.
ICON_TOKENS = ["base", "bar", "paper", "fold", "line", "ring"]
TOKENS = {
    "light": [token for _, tokens in MODE_GROUPS for token in tokens],
    "dark": [token for _, tokens in MODE_GROUPS for token in tokens],
    "shared": [token for _, tokens in SHARED_GROUPS for token in tokens],
    "icon": ICON_TOKENS,
}

# Tints, hovers, and the focus ring follow the roles above, so themes do not set them.
DERIVED = """\
/* Derived tokens: computed from the roles above. */
:root {
    --primary-soft: color-mix(in oklch, var(--primary) 14%, var(--surface));
    --primary-hover: color-mix(in oklch, var(--primary) 85%, var(--text));
    --secondary-soft: color-mix(in oklch, var(--secondary) 14%, var(--surface));
    --accent-soft: color-mix(in oklch, var(--accent) 14%, var(--surface));
    --success-soft: color-mix(in oklch, var(--success) 14%, var(--surface));
    --warning-soft: color-mix(in oklch, var(--warning) 16%, var(--surface));
    --danger-soft: color-mix(in oklch, var(--danger) 14%, var(--surface));
    --danger-hover: color-mix(in oklch, var(--danger) 85%, var(--text));
    --info-soft: color-mix(in oklch, var(--info) 14%, var(--surface));
    --focus-ring: 0 0 0 3px color-mix(in oklch, var(--primary) 35%, transparent);
}
"""


class InvalidTheme(Exception):
    """A theme file that does not define exactly the expected tokens."""


# --- Theme files -------------------------------------------------------------


def resolve(theme):
    """Accept a theme name in themes/ or a path to a JSON file."""
    path = pathlib.Path(theme)
    return path if path.suffix == ".json" else THEMES / f"{theme}.json"


def load_theme(path):
    try:
        theme = json.loads(path.read_text())
    except FileNotFoundError as error:
        raise InvalidTheme(f"{path}: not found; see --list") from error
    validate(theme, path)
    check_contrast(theme, path)
    return theme


def validate(theme, path):
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


# --- Contrast ----------------------------------------------------------------

TEXT = 4.5  # WCAG AA for normal-size text
GRAPHIC = 3.0  # WCAG AA for icons and other non-text marks

# Tint amounts from the derived block, so the check sees the colors the CSS draws.
SOFT = {"primary": 0.14, "secondary": 0.14, "success": 0.14, "warning": 0.16, "danger": 0.14,
        "info": 0.14}
HOVER = 0.85


def rgb(color):
    value = color.removeprefix("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))


def mix(color, base, amount):
    """Approximates color-mix(in oklch, ...) with an sRGB blend; close enough to gate on."""
    return "#" + "".join(
        f"{round(c * amount + b * (1 - amount)):02x}" for c, b in zip(rgb(color), rgb(base))
    )


def luminance(color):
    channels = [value / 255 for value in rgb(color)]
    linear = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]


def contrast(first, second):
    lighter, darker = sorted((luminance(first), luminance(second)), reverse=True)
    return (lighter + 0.05) / (darker + 0.05)


def contrast_pairs(colors):
    """(label, foreground, background, minimum) for every pairing the UI draws."""
    surface = colors["surface"]
    soft = {token: mix(colors[token], surface, amount) for token, amount in SOFT.items()}
    hover = {token: mix(colors[token], colors["text"], HOVER) for token in ("primary", "danger")}
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


def check_contrast(theme, path):
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


def available_themes():
    return sorted(THEMES.glob("*.json"))


# --- CSS ---------------------------------------------------------------------


def declarations(values, groups, indent):
    lines = []
    for title, tokens in groups:
        lines.append(f"{indent}/* {title} */")
        lines.extend(f"{indent}--{token}: {values[token]};" for token in tokens)
        lines.append("")
    return lines[:-1]


def root_block(scheme, values, groups, indent="    "):
    body = [f"{indent}color-scheme: {scheme};", "", *declarations(values, groups, indent)]
    return [":root {", *body, "}"]


def render_css(theme, source):
    light = root_block("light", theme["light"] | theme["shared"], MODE_GROUPS + SHARED_GROUPS)
    dark = root_block("dark", theme["dark"], MODE_GROUPS)
    lines = [
        "/*",
        f" * Generated by scripts/theme.py from {source}. Do not edit by hand:",
        " * change the theme file and run make theme.",
        f" * Theme: {theme.get('name', source)}. {theme.get('description', '')}".rstrip(),
        " */",
        "",
        *light,
        "",
        "@media (prefers-color-scheme: dark) {",
        *("    " + line if line else line for line in dark),
        "}",
        "",
        DERIVED,
    ]
    return "\n".join(lines)


# --- Command line ------------------------------------------------------------


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("theme", nargs="?", help="theme name in themes/ or a JSON path")
    parser.add_argument("--list", action="store_true", help="list the themes in themes/")
    parser.add_argument("--output", type=pathlib.Path, default=OUTPUT, help="stylesheet to write")
    args = parser.parse_args(argv)
    if not args.list and not args.theme:
        parser.error("a theme or --list is required")
    return args


def list_themes():
    for path in available_themes():
        theme = json.loads(path.read_text())
        print(f"{path.stem:<10} {theme.get('description', '')}")
    return EXIT_OK


def write_theme(theme_arg, output):
    path = resolve(theme_arg)
    theme = load_theme(path)
    source = path.resolve().relative_to(ROOT) if path.resolve().is_relative_to(ROOT) else path
    css = render_css(theme, source)
    shown = output.relative_to(ROOT) if output.is_relative_to(ROOT) else output
    if output.exists() and output.read_text() == css:
        print(f"Unchanged {shown}")
        return EXIT_OK
    output.write_text(css)
    print(f"Wrote {shown} from {source}")
    return EXIT_OK


def main(argv=None):
    args = parse_args(argv)
    if args.list:
        return list_themes()
    try:
        return write_theme(args.theme, args.output.resolve())
    except InvalidTheme as error:
        print(error, file=sys.stderr)
        return EXIT_INVALID


if __name__ == "__main__":
    sys.exit(main())
