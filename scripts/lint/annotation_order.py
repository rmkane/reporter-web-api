#!/usr/bin/env python3
"""Check or fix the order of class annotations in Java sources.

Annotations directly above a top-level type are ordered by tier, keeping
source order within a tier. Usage:

    scripts/lint/annotation_order.py --check [paths...]
    scripts/lint/annotation_order.py --fix [paths...]

Paths default to src. The tiers live in annotation_order.json next to this
script. Exits 1 when --check finds a file out of order, and 2 on an annotation
that no tier lists, so new annotations get a deliberate home.
"""

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONFIG = Path(__file__).resolve().with_suffix(".json")

EXIT_OK = 0
EXIT_UNORDERED = 1
EXIT_UNKNOWN = 2

TYPE_DECLARATION = re.compile(
    r"(public |final |abstract |sealed |non-sealed )*(class|record|interface|enum)\b"
)
ANNOTATION_NAME = re.compile(r"@(\w+)")
COMMENT_STARTS = ("*", "/*", "//")
CONTINUATION_STARTS = (" ", "\t", ")", "}")


class UnknownAnnotation(Exception):
    """An annotation that no tier lists."""


# --- Tiers -------------------------------------------------------------------


@dataclass(frozen=True)
class Tier:
    name: str
    annotations: frozenset[str]
    prefixes: tuple[str, ...]

    def matches(self, annotation: str) -> bool:
        return annotation in self.annotations or annotation.startswith(self.prefixes)


def load_tiers(path: Path = CONFIG) -> list[Tier]:
    """Read the tiers, in order, from the JSON config."""
    return [
        Tier(
            name=tier["name"],
            annotations=frozenset(tier["annotations"]),
            prefixes=tuple(tier.get("prefixes", ())),
        )
        for tier in json.loads(path.read_text())["tiers"]
    ]


def tier_index(annotation: str, tiers: list[Tier]) -> int:
    for index, tier in enumerate(tiers):
        if tier.matches(annotation):
            return index
    raise UnknownAnnotation(annotation)


# --- Annotation block --------------------------------------------------------


def is_annotation(line: str) -> bool:
    return line.startswith("@")


def is_continuation(line: str) -> bool:
    """A wrapped annotation argument, as Google Java Format writes it, but not a comment."""
    stripped = line.strip()
    return (
        line.startswith(CONTINUATION_STARTS)
        and bool(stripped)
        and not stripped.startswith(COMMENT_STARTS)
    )


def find_declaration(lines: list[str]) -> int | None:
    """Index of the first top-level type declaration, or None."""
    return next((i for i, line in enumerate(lines) if TYPE_DECLARATION.match(line)), None)


def find_block_start(lines: list[str], end: int) -> int:
    """Index of the first annotation line in the block that ends at `end`."""
    start = end
    while start > 0 and (is_annotation(lines[start - 1]) or is_continuation(lines[start - 1])):
        start -= 1
    while start < end and not is_annotation(lines[start]):
        start += 1
    return start


def group_units(block: list[str]) -> list[list[str]]:
    """Group each annotation with its wrapped lines so it moves as one unit."""
    units = []
    for line in block:
        if is_annotation(line):
            units.append([line])
        else:
            units[-1].append(line)
    return units


def annotation_name(unit: list[str]) -> str:
    return ANNOTATION_NAME.match(unit[0]).group(1)


def order_units(units: list[list[str]], tiers: list[Tier]) -> list[list[str]]:
    """Sort by tier; enumerate keeps source order within a tier."""
    def rank(item: tuple[int, list[str]]) -> tuple[int, int]:
        position, unit = item
        return tier_index(annotation_name(unit), tiers), position

    return [unit for _, unit in sorted(enumerate(units), key=rank)]


def reorder(text: str, tiers: list[Tier]) -> str | None:
    """Return the text with its class annotations in tier order, or None if already ordered."""
    lines = text.split("\n")
    end = find_declaration(lines)
    if end is None:
        return None
    start = find_block_start(lines, end)
    units = group_units(lines[start:end])
    ordered = order_units(units, tiers)
    if ordered == units:
        return None
    lines[start:end] = [line for unit in ordered for line in unit]
    return "\n".join(lines)


# --- Command line ------------------------------------------------------------


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true", help="report files out of order")
    mode.add_argument("--fix", action="store_true", help="rewrite files out of order")
    parser.add_argument("paths", nargs="*", default=["src"], help="files or directories")
    return parser.parse_args(argv)


def java_files(paths: list[str]) -> list[Path]:
    files = []
    for path in map(Path, paths):
        files.extend(sorted(path.rglob("*.java")) if path.is_dir() else [path])
    return files


def process(files: list[Path], tiers: list[Tier], fix: bool) -> list[Path]:
    """Return the files that were out of order, rewriting them when `fix` is set."""
    unordered = []
    for file in files:
        try:
            fixed = reorder(file.read_text(), tiers)
        except UnknownAnnotation as error:
            raise UnknownAnnotation(f"{file}: @{error} is not in any tier") from error
        if fixed is None:
            continue
        unordered.append(file)
        if fix:
            file.write_text(fixed)
    return unordered


def report(unordered: list[Path], fix: bool) -> int:
    label = "Reordered" if fix else "Out of order"
    for file in unordered:
        print(f"{label}: {file}")
    if unordered and not fix:
        print("Run make format to fix the class annotation order.", file=sys.stderr)
        return EXIT_UNORDERED
    return EXIT_OK


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        unordered = process(java_files(args.paths), load_tiers(), args.fix)
    except UnknownAnnotation as error:
        print(f"{error}; add it to {CONFIG.relative_to(ROOT)}", file=sys.stderr)
        return EXIT_UNKNOWN
    return report(unordered, args.fix)


if __name__ == "__main__":
    sys.exit(main())
