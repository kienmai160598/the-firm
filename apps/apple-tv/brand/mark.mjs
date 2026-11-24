// The Movies mark: nine geometric tiles on a 3 by 3 grid, Bauhaus style, with a play triangle in
// the centre. Each tile is one primitive (dot, half disc, quarter disc, rounded square, triangle)
// drawn in a unit cell. Lengths are in cells; the grid is 3 cells plus 2 gaps wide.

export const COLORS = {
  background: "#F4F3EF",
  orange: "#F26A21",
  yellow: "#FFC72C",
  navy: "#1F3B73",
  ink: "#2B2B30",
};

export const GAP = 0.1;
export const MARK_SIZE = 3 + 2 * GAP;

// Unit cell paths, (0, 0) to (1, 1).
const SHAPES = {
  dot: "M0 .5A.5 .5 0 1 1 1 .5A.5 .5 0 1 1 0 .5Z",
  halfDisc: "M0 0H.5A.5 .5 0 0 1 .5 1H0Z",
  quarterDisc: "M0 1V0A1 1 0 0 1 1 1Z",
  roundedSquare: "M.3 0H1V1H0V.3A.3 .3 0 0 1 .3 0Z",
  play: "M0 0L1 .5L0 1Z",
};

// column, row, shape, colour, rotation in degrees about the cell centre
export const TILES = [
  { col: 0, row: 0, shape: "dot", color: "yellow" },
  { col: 1, row: 0, shape: "halfDisc", color: "ink", rotation: 90 },
  { col: 2, row: 0, shape: "dot", color: "orange" },
  { col: 0, row: 1, shape: "roundedSquare", color: "navy" },
  { col: 1, row: 1, shape: "play", color: "orange", isPlay: true },
  { col: 2, row: 1, shape: "halfDisc", color: "yellow" },
  { col: 0, row: 2, shape: "quarterDisc", color: "ink" },
  { col: 1, row: 2, shape: "halfDisc", color: "navy", rotation: 270 },
  { col: 2, row: 2, shape: "dot", color: "ink" },
];

/**
 * SVG markup for the mark with its top-left corner at (x, y) and the given side in pixels.
 * `tiles` picks which tiles to draw, so the icon can split the play tile onto its own layer.
 */
export function markSvg(x, y, size, tiles = TILES) {
  const cell = size / MARK_SIZE;
  return tiles
    .map(({ col, row, shape, color, rotation = 0 }) => {
      const left = x + col * (1 + GAP) * cell;
      const top = y + row * (1 + GAP) * cell;
      return (
        `<g transform="translate(${left} ${top}) scale(${cell}) rotate(${rotation} .5 .5)">` +
        `<path d="${SHAPES[shape]}" fill="${COLORS[color]}"/></g>`
      );
    })
    .join("");
}

/** The tile shapes in unit cells, for the design file. */
export const SHAPE_PATHS = SHAPES;
