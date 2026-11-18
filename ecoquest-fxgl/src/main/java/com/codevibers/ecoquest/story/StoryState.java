package com.codevibers.ecoquest.story;

import com.codevibers.ecoquest.model.Sibling;

/**
 * Story wording and art shared by the cutscenes and the boss fight.
 * Nobody has a name: the one you pick is "the tidy sibling" (you), the other is "the messy sibling".
 */
public final class StoryState {
    private StoryState() { }

    public static final String BOSS = "QUEEN MUCK";

    /** The sibling you play. */
    public static final String TIDY = "the tidy sibling";
    /** The sibling who was not picked (dirty sheet, kidnapped). */
    public static final String MESSY = "the messy sibling";

    /** Short role tag shown under a sprite. */
    public static String role(boolean player) { return player ? "TIDY SIBLING" : "MESSY SIBLING"; }

    /** The messy sibling's sheet: always the one that was not picked. */
    public static String messySheet(Sibling picked) { return picked.other().dirtySheet(); }

    /** npc_<index> sprite for the villager shown after each campaign level (0-9). */
    public static String npcSprite(int levelIndex) { return "npc/npc_" + Math.min(9, levelIndex); }
}
