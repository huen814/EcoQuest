package com.codevibers.ecoquest.model;

/**
 * The two playable siblings. They differ in appearance and story only: nothing in the game rules
 * (GameSession, upgrades, scoring, speeds) ever reads which one was picked.
 * <p>
 * Both start out clean. The sibling the player picks stays clean and is the one they play as;
 * the other sibling is the dirty one (see {@link #other()} and {@link #dirtySheet()}), used by the story.
 */
public enum Sibling {
    SISTER("sister", "SISTER"),
    BROTHER("brother", "BROTHER");

    /** Used in texture names, e.g. player_sister.png. */
    public final String id;
    public final String label;

    Sibling(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** The sibling the player did not pick. */
    public Sibling other() { return this == SISTER ? BROTHER : SISTER; }

    /** Clean playable sheet (walking, or rowing the boat). */
    public String sheet(boolean boat) { return "player_" + id + (boat ? "_boat" : "") + ".png"; }

    /** Dirty walking sheet, for when this sibling is the one that was not picked (cutscenes). */
    public String dirtySheet() { return "player_" + id + "_dirty.png"; }
}
