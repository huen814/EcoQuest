package com.codevibers.ecoquest.model;

/** Eight-way movement directions, expressed as one-tile grid offsets. */
public enum Direction {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0),
    UP_LEFT(-1, -1), UP_RIGHT(1, -1), DOWN_LEFT(-1, 1), DOWN_RIGHT(1, 1);

    public final int dc;
    public final int dr;

    Direction(int dc, int dr) {
        this.dc = dc;
        this.dr = dr;
    }

    public boolean diagonal() { return dc != 0 && dr != 0; }
}
