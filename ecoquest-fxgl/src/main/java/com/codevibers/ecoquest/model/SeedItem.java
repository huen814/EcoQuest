package com.codevibers.ecoquest.model;

public final class SeedItem extends Pickup {
    SeedItem(int col, int row) { super(col, row); }
    @Override public String sprite() { return "items/seed"; }
    @Override public String displayName() { return "TREE SEED"; }
}
