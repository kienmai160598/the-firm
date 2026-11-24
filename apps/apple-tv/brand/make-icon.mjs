// Renders the layered tvOS app icon and the Top Shelf images from mark.mjs into the asset
// catalog. Needs rsvg-convert and ImageMagick (brew install librsvg imagemagick).
//
//   node apps/apple-tv/brand/make-icon.mjs
//
// The design source is the "the-firm-movies" project in the Things design hub (brand.pen).

import { execFileSync } from "node:child_process";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { COLORS, TILES, markSvg } from "./mark.mjs";

// The mark takes this share of the icon height, and of the Top Shelf height.
const ICON_MARK_HEIGHT = 0.62;
const SHELF_MARK_HEIGHT = 0.44;
// Wordmark size and gap, as shares of the mark side.
const SHELF_WORDMARK = { text: "Movies", size: 0.62, gap: 0.2, letterSpacing: -0.006 };

const INFO = { info: { author: "xcode", version: 1 } };
const here = dirname(fileURLToPath(import.meta.url));
const catalog = join(here, "../Sources/Assets.xcassets");
const brand = join(catalog, "App Icon & Top Shelf Image.brandassets");
const scratch = mkdtempSync(join(tmpdir(), "movies-icon-"));

const svg = (width, height, body) =>
  `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" ` +
  `viewBox="0 0 ${width} ${height}">${body}</svg>`;

const background = (width, height) =>
  `<rect width="${width}" height="${height}" fill="${COLORS.background}"/>`;

const frameTiles = TILES.filter((tile) => !tile.isPlay);
const playTiles = TILES.filter((tile) => tile.isPlay);

function render(name, markup, path) {
  const source = join(scratch, `${name}.svg`);
  writeFileSync(source, markup);
  mkdirSync(dirname(path), { recursive: true });
  execFileSync("rsvg-convert", [source, "-o", path]);
}

function writeJson(path, value) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, `${JSON.stringify(value, null, 2)}\n`);
}

/** The width of the wordmark at a font size, measured from a trimmed render. */
function wordmarkWidth(fontSize, letterSpacing) {
  const probe = join(scratch, "wordmark.png");
  const height = Math.ceil(fontSize * 2);
  const width = Math.ceil(fontSize * 6);
  render(
    "wordmark",
    svg(width, height, wordmarkText({ x: 0, y: fontSize * 1.4 }, fontSize, letterSpacing)),
    probe,
  );
  const trimmed = execFileSync("magick", [probe, "-trim", "-format", "%w", "info:"]);
  return Number(trimmed.toString());
}

function wordmarkText(origin, fontSize, letterSpacing) {
  return (
    `<text x="${origin.x}" y="${origin.y}" font-family="System Font" font-weight="600" ` +
    `font-size="${fontSize}" letter-spacing="${letterSpacing}" fill="${COLORS.ink}">` +
    `${SHELF_WORDMARK.text}</text>`
  );
}

/** Back (opaque), Middle (the eight frame tiles) and Front (the play tile) at one pixel size. */
function iconLayers(width, height) {
  const size = height * ICON_MARK_HEIGHT;
  const x = (width - size) / 2;
  const y = (height - size) / 2;
  return {
    Back: svg(width, height, background(width, height)),
    Middle: svg(width, height, markSvg(x, y, size, frameTiles)),
    Front: svg(width, height, markSvg(x, y, size, playTiles)),
  };
}

function writeImageStack(stackName, sizes) {
  const stack = join(brand, `${stackName}.imagestack`);
  writeJson(join(stack, "Contents.json"), {
    ...INFO,
    layers: ["Front", "Middle", "Back"].map((layer) => ({ filename: `${layer}.imagestacklayer` })),
  });
  for (const layer of ["Front", "Middle", "Back"]) {
    const layerDir = join(stack, `${layer}.imagestacklayer`);
    writeJson(join(layerDir, "Contents.json"), INFO);
    const images = sizes.map(({ width, height, scale }) => {
      const filename = `${layer.toLowerCase()}${scale && scale !== "1x" ? `@${scale}` : ""}.png`;
      render(
        `${stackName}-${layer}-${width}`,
        iconLayers(width, height)[layer],
        join(layerDir, "Content.imageset", filename),
      );
      return scale ? { filename, idiom: "tv", scale } : { filename, idiom: "tv" };
    });
    writeJson(join(layerDir, "Content.imageset", "Contents.json"), { images, ...INFO });
  }
}

function shelfImage(width, height) {
  const markSize = height * SHELF_MARK_HEIGHT;
  const fontSize = SHELF_WORDMARK.size * markSize;
  const letterSpacing = SHELF_WORDMARK.letterSpacing * markSize;
  const gap = SHELF_WORDMARK.gap * markSize;
  const lockupWidth = markSize + gap + wordmarkWidth(fontSize, letterSpacing);
  const left = (width - lockupWidth) / 2;
  // SF Pro cap height is 0.7 of the font size: centre the capitals on the mark.
  const baseline = height / 2 + fontSize * 0.35;
  return svg(
    width,
    height,
    background(width, height) +
      markSvg(left, (height - markSize) / 2, markSize) +
      wordmarkText({ x: left + markSize + gap, y: baseline }, fontSize, letterSpacing),
  );
}

function writeShelf(name, width, height) {
  const set = join(brand, `${name}.imageset`);
  const images = [1, 2].map((scale) => {
    const filename = `${name.toLowerCase().replaceAll(" ", "-")}${scale === 2 ? "@2x" : ""}.png`;
    render(`${name}-${scale}`, shelfImage(width * scale, height * scale), join(set, filename));
    return { filename, idiom: "tv", scale: `${scale}x` };
  });
  writeJson(join(set, "Contents.json"), { images, ...INFO });
}

try {
  rmSync(brand, { recursive: true, force: true });
  writeJson(join(catalog, "Contents.json"), INFO);
  writeJson(join(brand, "Contents.json"), {
    assets: [
      { filename: "App Icon - App Store.imagestack", idiom: "tv", role: "primary-app-icon", size: "1280x768" },
      { filename: "App Icon.imagestack", idiom: "tv", role: "primary-app-icon", size: "400x240" },
      { filename: "Top Shelf Image Wide.imageset", idiom: "tv", role: "top-shelf-image-wide", size: "2320x720" },
      { filename: "Top Shelf Image.imageset", idiom: "tv", role: "top-shelf-image", size: "1920x720" },
    ],
    ...INFO,
  });
  writeImageStack("App Icon", [
    { width: 400, height: 240, scale: "1x" },
    { width: 800, height: 480, scale: "2x" },
  ]);
  writeImageStack("App Icon - App Store", [{ width: 1280, height: 768 }]);
  writeShelf("Top Shelf Image", 1920, 720);
  writeShelf("Top Shelf Image Wide", 2320, 720);
  console.log(`Wrote ${brand}`);
} finally {
  rmSync(scratch, { recursive: true, force: true });
}
