package com.codevibers.ecoquest.model;

public final class WasteItem extends Pickup {
    private final WasteType type;

    WasteItem(WasteType type, int col, int row) {
        super(col, row);
        this.type = type;
    }

    public WasteType type() { return type; }
    public BinType bin() { return type.bin; }
    @Override public String sprite() { return type.sprite(); }
    @Override public String displayName() { return type.label.toUpperCase(); }
}
