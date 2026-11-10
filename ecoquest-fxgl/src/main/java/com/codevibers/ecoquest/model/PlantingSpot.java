package com.codevibers.ecoquest.model;

public final class PlantingSpot {
    private final int col;
    private final int row;
    private boolean planted;
    private double growth;

    PlantingSpot(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int col() { return col; }
    public int row() { return row; }
    public boolean planted() { return planted; }
    /** 0 (fresh sapling) to 1 (full tree). */
    public double growth() { return growth; }
    void plant() { planted = true; growth = 0; }
    void grow(double dt) { if (planted) growth = Math.min(1, growth + dt / 3.0); }
}
