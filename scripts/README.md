# Scripts

Helper scripts, grouped by what they work on. Run them through `make` (or `.\make.ps1` on Windows) rather than directly; the targets below pass the right arguments. Each script's docstring or header describes its options.

| Directory | Purpose | Make targets |
| --- | --- | --- |
| [lint/](lint/) | Code checks beyond Spotless | `make lint`, `make format` |
| [theme/](theme/) | The UI theme: CSS variables, favicon, and preview cards | `make theme`, `make themes`, `make previews` |
| [scan/](scan/) | Trivy vulnerability scans of the jar and the Docker image | `make scan`, `make scan-jar`, `make scan-image` |
| [git/](git/) | Local git setup | `make hooks` |

## lint

- `annotation_order.py` checks (`--check`) or fixes (`--fix`) the order of class annotations. The tiers live in `annotation_order.json`; an annotation that no tier lists fails the check until it is added there.

## theme

The theme files in [themes/](../themes/) are the source of truth. These scripts read them and write generated files, and they only rewrite a file when its content changes.

- `themes.py` is the shared model, not a command: the token list, loading, validation, and the WCAG AA contrast check every theme must pass.
- `generate_css.py` writes `src/main/resources/static/css/theme.css` from a theme. `--list` lists the themes.
- `generate_icons.py` draws `favicon.svg` and `favicon.ico` in a theme's logo colors. The SVG outlines its tile on dark pages; the ICO needs Pillow (`brew install pillow`).
- `generate_previews.py` draws a light and dark preview card for each theme into `themes/previews/` and the gallery in `themes/README.md`. Needs Pillow.

## scan

- `scan.sh` (and `scan.ps1` for Windows) runs Trivy against a jar or an image and writes JSON, CSV, and HTML reports to `target/scan/`. It fails on findings at `TRIVY_FAIL_ON` (default HIGH and CRITICAL).

## git

- `setup-hooks.sh` points this clone at `.githooks/`, whose pre-commit hook runs the same checks as `make lint`.

## Conventions

- Python scripts use `#!/usr/bin/env python3`, `from pathlib import Path`, type hints, and a `main(argv=None) -> int` that returns the exit code. Longer files split into `# ---` sections.
- Paths are resolved from the repository root, so scripts work from any directory, and printed paths are relative to it.
- Generated files say which script made them and are rewritten only when their content changes.
- Scripts in one directory may import each other; scripts in different directories stay independent.
