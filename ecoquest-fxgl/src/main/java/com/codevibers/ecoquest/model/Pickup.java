package com.codevibers.ecoquest.model;

/** Anything lying on a tile that the Guardian can pick up: waste, seeds, boosts. */
public abstract class Pickup {
    private static int nextId = 1;

    private final int id = nextId++;
    int col;
    int row;
    double drift;
    boolean river;

    protected Pickup(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int id() { return id; }
    public int col() { return col; }
    public int row() { return row; }
    /** True until first picked up out of the river (bonus is paid once). */
    public boolean river() { return river; }
    /** Texture stem including folder, e.g. items/tin_can. */
    public abstract String sprite();
    public abstract String displayName();
    public boolean isGarbage() { return this instanceof WasteItem; }
}
