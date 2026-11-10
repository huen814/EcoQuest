package com.codevibers.ecoquest.model;

public final class BoostItem extends Pickup {
    BoostItem(int col, int row) { super(col, row); }
    @Override public String sprite() { return "items/boost"; }
    @Override public String displayName() { return "ECO BOOST"; }
}
