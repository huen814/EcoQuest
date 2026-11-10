package com.codevibers.ecoquest.model;

public enum WasteType {
    PLASTIC_BOTTLE("Plastic bottle", BinType.PLASTIC),
    PLASTIC_BAG("Plastic bag", BinType.PLASTIC),
    FOOD_CONTAINER("Food container", BinType.PLASTIC),
    NEWSPAPER("Newspaper", BinType.PAPER),
    CARDBOARD_BOX("Cardboard box", BinType.PAPER),
    NOTEBOOK_PAPER("Notebook paper", BinType.PAPER),
    GLASS_BOTTLE("Glass bottle", BinType.GLASS),
    GLASS_JAR("Glass jar", BinType.GLASS),
    TIN_CAN("Tin can", BinType.METAL),
    ALUMINUM_CAN("Aluminum can", BinType.METAL),
    BOTTLE_CAP("Bottle cap", BinType.METAL),
    BANANA_PEEL("Banana peel", BinType.ORGANIC),
    FOOD_SCRAPS("Food scraps", BinType.ORGANIC),
    DRY_LEAVES("Dry leaves", BinType.ORGANIC);

    public final String label;
    public final BinType bin;

    WasteType(String label, BinType bin) {
        this.label = label;
        this.bin = bin;
    }

    public String sprite() { return "items/" + name().toLowerCase(); }
}
