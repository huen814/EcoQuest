package com.codevibers.ecoquest.model;

/**
 * One purchasable look. Cosmetics are visual only: nothing in GameSession, scoring or the attributes
 * ever reads them. Each one is a colour swap applied to the sibling the player is using.
 *
 * @param rgb the colour to swap in (0xRRGGBB); ignored for the free default of each slot and for designs
 * @param art null for a plain colour swap; otherwise the name of a drawn design (e.g. "witch") that has its own
 *            sprite sheets, player_<sibling>_<art>.png and ..._boat.png
 * @param only the sibling this design was drawn for, or null if either sibling can wear it
 */
public record Cosmetic(Slot slot, String id, String name, int cost, int rgb, String blurb, String art, Sibling only) {

    /** A plain colour-swap cosmetic. */
    public Cosmetic(Slot slot, String id, String name, int cost, int rgb, String blurb) {
        this(slot, id, name, cost, rgb, blurb, null, null);
    }

    /** A drawn design with its own sprite sheets, made for one sibling, or for both when {@code only} is null. */
    public static Cosmetic design(Slot slot, String id, String name, int cost, int rgb, String blurb, String art, Sibling only) {
        return new Cosmetic(slot, id, name, cost, rgb, blurb, art, only);
    }

    /** True if this sibling can wear it (colour swaps fit both; a design fits the sibling it was drawn for). */
    public boolean fits(Sibling who) { return only == null || only == who; }

    /** True for a drawn design with its own sprite sheets, false for a colour swap. */
    public boolean hasArt() { return art != null; }

    /** The parts of the look that can be customised. */
    public enum Slot {
        OUTFIT("OUTFIT"), HAIR("HAIR"), BOAT("BOAT");

        public final String label;

        Slot(String label) { this.label = label; }
    }

    public static final String DEFAULT_ID = "default";

    /** The free, original look of a slot. Always owned, and what an unequipped slot falls back to. */
    public boolean isDefault() { return DEFAULT_ID.equals(id); }
}
