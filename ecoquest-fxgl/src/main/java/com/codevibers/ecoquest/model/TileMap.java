package com.codevibers.ecoquest.model;

/**
 * The 15 x 10 world grid (48 px tiles = 720 x 480).
 * rows 0-1 sky/skyline (behind the HUD), 2 scenery, 3 grass + planting spots, 4-5 road,
 * 6-7 river (river levels only), 8 grass + bins, 9 grass.
 */
public final class TileMap {
    public enum Tile { SKY, SCENE, GRASS, ROAD, WATER, BRIDGE }

    public static final int COLS = 15;
    public static final int ROWS = 10;
    public static final int SPOT_ROW = 3;
    public static final int ROAD_TOP = 4;
    public static final int ROAD_BOTTOM = 5;
    public static final int RIVER_TOP = 6;
    public static final int RIVER_BOTTOM = 7;
    public static final int BIN_ROW = 8;
    public static final int[] BRIDGES = {2, 12};
    public static final int[] BIN_COLS = {1, 4, 7, 10, 13};
    public static final int BOAT_START_COL = 6;

    private final Tile[][] tiles = new Tile[COLS][ROWS];
    private final boolean river;
    /** The rowing boat; the view draws it while nobody is aboard. */
    public int boatCol = BOAT_START_COL;
    public int boatRow = RIVER_TOP;
    public boolean boatBoarded;

    TileMap(boolean river) {
        this.river = river;
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Tile t = r < 2 ? Tile.SKY : r == 2 ? Tile.SCENE
                        : (r == ROAD_TOP || r == ROAD_BOTTOM) ? Tile.ROAD : Tile.GRASS;
                if (river && (r == RIVER_TOP || r == RIVER_BOTTOM)) t = Tile.WATER;
                tiles[c][r] = t;
            }
        }
        if (river) for (int b : BRIDGES) for (int r = RIVER_TOP; r <= RIVER_BOTTOM; r++) tiles[b][r] = Tile.BRIDGE;
    }

    public boolean river() { return river; }

    /** The rowing boat can only use the stretch of river between the bridges that holds its start tile. */
    public int laneMin() {
        int min = 0;
        for (int b : BRIDGES) if (b < BOAT_START_COL) min = Math.max(min, b + 1);
        return min;
    }

    public int laneMax() {
        int max = COLS - 1;
        for (int b : BRIDGES) if (b > BOAT_START_COL) max = Math.min(max, b - 1);
        return max;
    }

    /** Open water (no bridge deck): the only tiles the boat may enter. */
    public boolean isBoatWater(int c, int r) { return river && inBounds(c, r) && tiles[c][r] == Tile.WATER; }
    public boolean inBounds(int c, int r) { return c >= 0 && c < COLS && r >= 0 && r < ROWS; }
    public Tile at(int c, int r) { return inBounds(c, r) ? tiles[c][r] : Tile.SKY; }
    /** Any tile in the river band, bridges included (items there float and need the boat). */
    public boolean isRiverTile(int c, int r) { return river && r >= RIVER_TOP && r <= RIVER_BOTTOM && inBounds(c, r); }
}
