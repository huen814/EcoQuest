package com.codevibers.ecoquest.view;

import java.util.List;

/**
 * What makes one boss fight different from another: its look, health, attacks, arena and the story lines around it.
 * {@link BossFightScreen} runs any of them; {@link BossStoryScreen} plays the lines before and after.
 */
public enum BossDef {

    /** Boss 1: a heap of unsorted trash that came to life. Fought after level 3. */
    MUCK_MOUND("MUCK MOUND", 30, Kind.MOUND, "boss/mound.png", "boss/mound_rage.png", null, "GLORP!",
            "scene:dump", 160, 128, 56, 1.5, 0.85,
            List.of(
                "The river runs clear again, but the ground beneath the old landfill starts to shake. " +
                "Every bottle, can and wrapper that people never sorted properly has dragged itself " +
                "into one hulking, dripping heap with two googly eyes. The corruption of nature has taken shape!",

                "Sprout: That's the Muck Mound, Guardian! It was born from all the trash nobody sorted. " +
                "Move in eight directions with the arrow keys or WASD, aim with the mouse, and fire with left-click, ENTER or Z. Dodge the sludge!"),
            List.of(
                "The Spirit Bottle's light washes over the Muck Mound. It wobbles, sighs, and collapses " +
                "into neatly sorted piles of cans, bottles and wrappers, its googly eyes bouncing away down the hill.",

                "Sprout: It was only trash that nobody sorted. But somebody is stirring up real trouble. " +
                "Keep sorting, Guardian. The next level is waiting!")),

    /** Boss 2: the Queen's Knight, sent by Queen Muck to capture the Guardian. He cannot be beaten, so this one is an escape. Fought after level 6. */
    EXPERIMENT("THE QUEEN'S KNIGHT", 0, Kind.CHASE, "boss/knight.png", "boss/knight_rage.png", null, "FOR HER MAJESTY!",
            "scene:city", 144, 144, 30, 0, 0,
            List.of(),
            List.of(
                "The maze shudders and the junk cubes crumble to dust. The knight, chasing too hard, trips over his own sludge and crashes into a heap of scrap. " +
                "\"Her Majesty... will not be pleased...\" he groans, and he drags himself back through his portal.",

                "Sprout: He's gone, for now. So that is who Queen Muck sends after her troublemakers. Come on, Guardian, the next area is waiting!")),

    /** The final boss: Queen Muck, on her throne in the palace hall. Fought after the last level. */
    QUEEN_MUCK("QUEEN MUCK", 50, Kind.QUEEN, "boss/queen_muck.png", "boss/queen_muck_rage.png", "boss/queen_muck_laugh.png",
            "HO HO HO!", "backdrop/palace_hall.png", 144, 216, 65, 1.6, 0.9, List.of(), List.of());

    /** Decides which attack patterns the boss uses. */
    public enum Kind { QUEEN, MOUND, CHASE }

    private final String label;
    private final int hp;
    private final Kind kind;
    private final String normal, rage, laugh, shout, backdrop;
    private final int width, height, hitRadius;
    private final double fireNormal, fireRage;
    private final List<String> intro, outro;

    BossDef(String label, int hp, Kind kind, String normal, String rage, String laugh, String shout, String backdrop,
            int width, int height, int hitRadius, double fireNormal, double fireRage, List<String> intro, List<String> outro) {
        this.label = label; this.hp = hp; this.kind = kind;
        this.normal = normal; this.rage = rage; this.laugh = laugh; this.shout = shout; this.backdrop = backdrop;
        this.width = width; this.height = height; this.hitRadius = hitRadius;
        this.fireNormal = fireNormal; this.fireRage = fireRage;
        this.intro = intro; this.outro = outro;
    }

    public String label()      { return label; }
    /** Stable id used by Boss Replay and the save: "muck_mound", "experiment" or "queen_muck". */
    public String id()         { return name().toLowerCase(); }
    public int hp()            { return hp; }
    public Kind kind()         { return kind; }
    public String normal()     { return normal; }
    public String rage()       { return rage; }
    /** Sprite shown in the story scenes before and after the fight (the Queen's Knight has a larger portrait than his chase sprite). */
    public String story()      { return this == EXPERIMENT ? "boss/knight_story.png" : normal; }
    /** Sprite used while the boss laughs, or null if it has none. */
    public String laugh()      { return laugh; }
    public String shout()      { return shout; }
    /** "scene:name" for a story scene, otherwise a backdrop texture path. */
    public String backdrop()   { return backdrop; }
    public int width()         { return width; }
    public int height()        { return height; }
    public int hitRadius()     { return hitRadius; }
    public double fireNormal() { return fireNormal; }
    public double fireRage()   { return fireRage; }
    public List<String> intro() { return intro; }
    public List<String> outro() { return outro; }

    /** The boss with this id (see {@link #id()}), or null. */
    public static BossDef byId(String id) {
        for (BossDef d : values()) if (d.id().equalsIgnoreCase(id)) return d;
        return null;
    }

    /** The boss fought right after this level (0-based), or null when the next level follows directly. */
    public static BossDef after(int levelIndex) {
        return switch (levelIndex) {
            case 2 -> MUCK_MOUND;
            case 5 -> EXPERIMENT;
            case 9 -> QUEEN_MUCK;       // after the last level
            default -> null;
        };
    }
}
