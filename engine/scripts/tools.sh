#!/usr/bin/env bash
# Sourced by the engine scripts: puts cmake 4 and ninja 1 on PATH, installed once into
# engine/.tools (gitignored), so no Homebrew install is needed.
engine_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
engine_tools="$engine_root/.tools"

if [[ ! -x "$engine_tools/bin/cmake" || ! -x "$engine_tools/bin/ninja" ]]; then
    python3 -m venv "$engine_tools"
    "$engine_tools/bin/pip" install --quiet "cmake==4.*" "ninja==1.*"
fi
export PATH="$engine_tools/bin:$PATH"
