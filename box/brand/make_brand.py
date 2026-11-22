"""Renders the box build's "Movies" brand into app/src/box/res.

Run: python3 -m venv /tmp/brandvenv && /tmp/brandvenv/bin/pip install fonttools
     /tmp/brandvenv/bin/python box/brand/make_brand.py   (needs rsvg-convert on PATH)
"""
import math
import subprocess
import tempfile
from pathlib import Path

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/box/res"
FONT = ROOT / "app/src/main/res/font/google_sans_flex.ttf"
NAME = "Movies"
SURFACE = "#141218"
ON_SURFACE = "#E6E0E9"
PLAY = "#1D1B20"
ACCENTS = {
    "": "#CFBDFE",
    "_arctic_blue": "#A8C8FF",
    "_emerald": "#8BD7AE",
    "_rose_gold": "#F2B8C6",
    "_copper": "#FFB68A",
    "_graphite": "#C9C5CC",
}
WORDMARK_ACCENTS = {**ACCENTS, "_gold": "#EFC46B", "_jade": "#86D5C4"}
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
COOKIE_LOBES = 9
COOKIE_DEPTH = 0.08
LOCKUP_MAX_WIDTH = 0.8
ICON_MARK_RADIUS = 0.36
SPLASH_MARK_RADIUS = 0.305
BANNER_MARK_RADIUS = 0.46
BANNER_MARK_CENTER_X = 0.83
BANNER_TEXT_LEFT = 0.075
BANNER_TEXT_GAP = 0.04
BANNER_TEXT_MAX_HEIGHT = 0.42
BANNER_WEIGHT = 600


def cookie_path(cx: float, cy: float, radius: float) -> str:
    points = []
    for index in range(360):
        angle = 2 * math.pi * index / 360
        r = radius * (1 + COOKIE_DEPTH * math.cos(COOKIE_LOBES * angle))
        points.append(f"{cx + r * math.cos(angle):.2f},{cy + r * math.sin(angle):.2f}")
    return "M" + " L".join(points) + " Z"


def play_triangle(cx: float, cy: float, radius: float) -> str:
    size = radius * 0.46
    corner = size * 0.32
    nudge = size * 0.12
    left = cx - size * 0.5 + nudge
    points = [(left, cy - size * 0.62), (left, cy + size * 0.62), (cx + size * 0.62 + nudge, cy)]
    joined = " ".join(f"{x:.2f},{y:.2f}" for x, y in points)
    return (f'<polygon points="{joined}" fill="{PLAY}" stroke="{PLAY}" '
            f'stroke-width="{corner:.2f}" stroke-linejoin="round"/>')


def mark(cx: float, cy: float, radius: float, accent: str) -> str:
    cookie = f'<path d="{cookie_path(cx, cy, radius)}" fill="{accent}"/>'
    return cookie + play_triangle(cx, cy, radius)


def load_font(weight: int = 500) -> TTFont:
    font = TTFont(FONT)
    return instantiateVariableFont(font, {"wght": weight, "ROND": 100, "opsz": 57})


def text_path(font: TTFont, x: float, baseline: float, height: float) -> tuple[str, float]:
    glyphs = font.getGlyphSet()
    cmap = font.getBestCmap()
    scale = height / font["head"].unitsPerEm
    pen = SVGPathPen(glyphs)
    cursor = 0.0
    for char in NAME:
        name = cmap[ord(char)]
        transform = (scale, 0, 0, -scale, x + cursor * scale, baseline)
        glyphs[name].draw(TransformPen(pen, transform))
        cursor += font["hmtx"][name][0]
    return pen.getCommands(), cursor * scale


def glow(width: float, height: float, accent: str) -> str:
    return (f'<defs><radialGradient id="g" cx="30%" cy="40%" r="75%">'
            f'<stop offset="0" stop-color="{accent}" stop-opacity="0.22"/>'
            f'<stop offset="1" stop-color="{accent}" stop-opacity="0"/></radialGradient></defs>'
            f'<rect width="{width}" height="{height}" fill="{SURFACE}"/>'
            f'<rect width="{width}" height="{height}" fill="url(#g)"/>')


def svg(width: float, height: float, body: str) -> str:
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
            f'viewBox="0 0 {width} {height}">{body}</svg>')


def icon_svg(accent: str) -> str:
    return svg(192, 192, glow(192, 192, accent) + mark(96, 96, 192 * ICON_MARK_RADIUS, accent))


def lockup(font: TTFont, width: float, height: float, accent: str) -> str:
    _, unit_text_width = text_path(font, 0, 0, 1)
    unit_total = 0.6 + 0.165 + unit_text_width * 0.5
    size = min(height, width * LOCKUP_MAX_WIDTH / unit_total)
    radius = size * 0.3
    text_height = size * 0.5
    gap = radius * 0.55
    left = (width - (radius * 2 + gap + unit_text_width * text_height)) / 2
    baseline = height / 2 + text_height * 0.36
    commands, _ = text_path(font, left + radius * 2 + gap, baseline, text_height)
    name = f'<path d="{commands}" fill="{ON_SURFACE}"/>'
    return mark(left + radius, height / 2, radius, accent) + name


def bleed_lockup(font: TTFont, width: float, height: float, accent: str) -> str:
    """TV banner: the mark large and cut by the right edge, the name as big as the rest allows."""
    radius = height * BANNER_MARK_RADIUS
    center_x = width * BANNER_MARK_CENTER_X
    mark_left = center_x - radius * (1 + COOKIE_DEPTH)
    text_left = width * BANNER_TEXT_LEFT
    _, unit_text_width = text_path(font, 0, 0, 1)
    available = mark_left - width * BANNER_TEXT_GAP - text_left
    text_height = min(height * BANNER_TEXT_MAX_HEIGHT, available / unit_text_width)
    baseline = height / 2 + text_height * 0.36
    commands, _ = text_path(font, text_left, baseline, text_height)
    name = f'<path d="{commands}" fill="{ON_SURFACE}"/>'
    return mark(center_x, height / 2, radius, accent) + name


def banner_svg(font: TTFont, width: int, height: int, accent: str) -> str:
    body = glow(width, height, accent) + bleed_lockup(font, width, height, accent)
    return svg(width, height, body)


def splash_mark_svg() -> str:
    return svg(1080, 1080, mark(540, 540, 1080 * SPLASH_MARK_RADIUS, ACCENTS[""]))


def render(source: str, target: Path, width: int, height: int) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", suffix=".svg", delete=False) as handle:
        handle.write(source)
    subprocess.run(["rsvg-convert", "-w", str(width), "-h", str(height), handle.name, "-o",
                    str(target)], check=True)
    Path(handle.name).unlink()


def main() -> None:
    font = load_font()
    banner_font = load_font(BANNER_WEIGHT)
    for suffix, accent in ACCENTS.items():
        for density, size in DENSITIES.items():
            icon = RES / f"mipmap-{density}/ic_launcher{suffix}.png"
            render(icon_svg(accent), icon, size, size)
        banner = banner_svg(banner_font, 320, 180, accent)
        render(banner, RES / f"mipmap-xhdpi/banner{suffix}.png", 320, 180)
    for suffix, accent in WORDMARK_ACCENTS.items():
        wordmark = svg(1085, 344, lockup(font, 1085, 344, accent))
        render(wordmark, RES / f"drawable-nodpi/app_logo_wordmark{suffix}.png", 1085, 344)
    render(splash_mark_svg(), RES / "drawable-nodpi/app_logo_mark.png", 1080, 1080)
    small_banner = banner_svg(banner_font, 1280, 720, ACCENTS[""])
    render(small_banner, RES / "drawable/tv_banner.png", 1280, 720)
    large_banner = banner_svg(banner_font, 1920, 1080, ACCENTS[""])
    render(large_banner, RES / "drawable-nodpi/tv_banner.png", 1920, 1080)


if __name__ == "__main__":
    main()
