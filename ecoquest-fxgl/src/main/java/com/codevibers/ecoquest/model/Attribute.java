package com.codevibers.ecoquest.model;

/**
 * Everything a level-up roll can hand out. Each attribute owns one slot of {@link Save#up} (the first six slots are
 * the old shop upgrades, so old saves keep working) and has its own maximum level so builds cannot stack forever.
 * Primary attributes change how the Guardian plays; secondary ones are small perks.
 */
public enum Attribute {
    BOOTS(0, Tier.PRIMARY, 3, "Swift Boots", "Walk faster on land."),
    GLOVES(1, Tier.PRIMARY, 3, "Strong Gloves", "Carry one more item."),
    SCANNER(2, Tier.PRIMARY, 1, "Eco Scanner", "Reach one tile further and see which bin fits."),
    BOAT(3, Tier.PRIMARY, 3, "Sturdy Boat", "Row the boat faster."),
    DRONE(4, Tier.PRIMARY, 3, "Helper Drone", "A drone collects litter for you, faster each level."),
    CLOVER(5, Tier.PRIMARY, 3, "Lucky Clover", "Longer Eco Boosts and more boost drops."),
    COIN_PURSE(6, Tier.SECONDARY, 2, "Coin Purse", "+1 coin for every correct sort."),
    FRESH_AIR(7, Tier.SECONDARY, 2, "Fresh Air", "Correct sorts cut 1 more pollution."),
    STEADY_HANDS(8, Tier.SECONDARY, 2, "Steady Hands", "Wrong bins cost 1 less score."),
    GREEN_THUMB(9, Tier.SECONDARY, 2, "Green Thumb", "Planted trees give +5 score."),
    QUICK_LEARNER(10, Tier.SECONDARY, 2, "Quick Learner", "+10% XP from everything.");

    public enum Tier { PRIMARY, SECONDARY }

    public static final int COUNT = values().length;

    public final int slot;
    public final Tier tier;
    public final int max;
    public final String label;
    public final String blurb;

    Attribute(int slot, Tier tier, int max, String label, String blurb) {
        this.slot = slot;
        this.tier = tier;
        this.max = max;
        this.label = label;
        this.blurb = blurb;
    }

    public boolean isPrimary() { return tier == Tier.PRIMARY; }
}
