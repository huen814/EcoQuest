package com.codevibers.ecoquest.model;

import java.util.ArrayList;
import java.util.List;

public final class Player {
    int col;
    int row;
    Direction facing = Direction.DOWN;
    boolean onBoat;
    final List<Pickup> carried = new ArrayList<>();

    Player(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int col() { return col; }
    public int row() { return row; }
    public Direction facing() { return facing; }
    public boolean onBoat() { return onBoat; }
    /** Carried items, oldest first; the last one is the one used next. */
    public List<Pickup> carried() { return carried; }
}
