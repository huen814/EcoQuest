package com.codevibers.ecoquest.model;

public final class Bin {
    private BinType type;
    private final int col;
    private final int row;
    private int fill;

    Bin(BinType type, int col, int row) {
        this.type = type;
        this.col = col;
        this.row = row;
    }

    public BinType type() { return type; }
    public int col() { return col; }
    public int row() { return row; }
    public int fill() { return fill; }
    void setType(BinType t) { type = t; }
    void setFill(int f) { fill = f; }
}
