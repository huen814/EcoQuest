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
                "Move with the arrow keys or WASD, and fire the Spirit Bottle with ENTER or Z. Dodge the sludge!"),
            List.of(
                "The Spirit Bottle's light washes over the Muck Mound. It wobbles, sighs, and collapses " +
                "into neatly sorted piles of cans, bottles and wrappers, its googly eyes bouncing away down the hill.",

                "Sprout: It was only trash that nobody sorted. But somebody is stirring up real trouble. " +
                "Keep sorting, Guardian. The next level is waiting!")),

    /** Boss 2: the Queen's experiment. It cannot be beaten yet, so this one is an escape down a city street. Fought after level 6. */
    EXPERIMENT("THE EXPERIMENT", 0, Kind.CHASE, "boss/experiment.png", "boss/experiment_rage.png", null, "RAAAH!",
            "scene:city", 144, 132, 30, 0, 0,
            List.of(
                "The beach is spotless, but the sky turns a bruised grey-purple. Far away in the city, a cage door slams open. " +
                "Queen Muck has noticed the Guardian's good deeds, and her latest experiment has broken loose to stop them. " +
                "A trail of stink drifts down the street, and something very tall is coming.",

                "Sprout: Run, Guardian! You can't beat that thing yet. Get to the manhole at the end of the street! " +
                "Move with the arrow keys or WASD. There's no fighting this one, so just keep running! " +
                "Use the junk cubes to throw it off your trail, stay out of its stink clouds, they slow you down, and watch out for the ghosts!"),
            List.of(
                "The Guardian dives into the manhole and slams the lid. Above, the experiment howls and its claws scrape the metal... " +
                "then the sound fades into the dark tunnel.",

                "Sprout: Phew, that was close! Queen Muck is getting desperate. The tunnel leads right to the old landfill. Onward, Guardian!")),

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
    public int hp()            { return hp; }
    public Kind kind()         { return kind; }
    public String normal()     { return normal; }
    public String rage()       { return rage; }
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
