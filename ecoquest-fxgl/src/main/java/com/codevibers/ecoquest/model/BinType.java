package com.codevibers.ecoquest.model;

public enum BinType {
    PLASTIC("Plastic", 0x7fb2f2, "Plastics go in the PLASTIC bin so they can be recycled."),
    PAPER("Paper", 0xf7d88a, "Paper and cardboard go in the PAPER bin."),
    GLASS("Glass", 0x86dba6, "Glass containers go in the GLASS bin."),
    METAL("Metal", 0xf29a9a, "Metal cans go in the METAL bin."),
    ORGANIC("Organic", 0xc29a7a, "Food and plant waste go in the ORGANIC bin.");

    public final String label;
    public final int rgb;
    public final String explanation;

    BinType(String label, int rgb, String explanation) {
        this.label = label;
        this.rgb = rgb;
        this.explanation = explanation;
    }

    public String sprite() { return name().toLowerCase(); }
}
