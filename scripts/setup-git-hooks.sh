#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

if ! git rev-parse --git-dir >/dev/null 2>&1; then
    echo "This directory is not a git repository." >&2
    exit 1
fi

git config core.hooksPath .githooks
chmod +x .githooks/pre-commit
echo "Git hooks installed from .githooks (pre-commit checks Java format and class annotation order)."
