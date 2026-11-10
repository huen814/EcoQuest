package com.codevibers.ecoquest.model;

public final class LevelConfig {
    public enum Mode { CAMPAIGN, TIME, ENDLESS, SPRINT, ZEN }

    public final String name;
    public final String difficulty;
    public final String[] story;
    public String tip = "";
    public String scene = "house";
    public Mode mode = Mode.CAMPAIGN;
    /** Slot in Save.high for non-campaign modes. */
    public int highSlot;
    public double spawn;
    public double pp;
    public int maxItems;
    public int target;
    public int missionSort, missionPlant, missionClean;
    /** Bin capacity (0 = bins auto-empty at 10), seconds between dumps, factory offers on/off, seeds on/off, river on/off. */
    public int cap, dump, factory, seeds, river;
    public int shuffle;
    public double current;
    public int tintR, tintG, tintB;
    public double tintA;
    public boolean rain;

    LevelConfig(String name, String difficulty, String s1, String s2) {
        this.name = name;
        this.difficulty = difficulty;
        this.story = new String[]{s1, s2};
    }

    LevelConfig rate(double spawn, int max, double pp, int target) { this.spawn = spawn; this.maxItems = max; this.pp = pp; this.target = target; return this; }
    LevelConfig m(int sort, int plant, int clean) { missionSort = sort; missionPlant = plant; missionClean = clean; return this; }
    LevelConfig hz(int cap, int dump, int factory, int seeds, int river) { this.cap = cap; this.dump = dump; this.factory = factory; this.seeds = seeds; this.river = river; return this; }
    LevelConfig scene(String s) { scene = s; return this; }
    LevelConfig tint(int r, int g, int b, double a) { tintR = r; tintG = g; tintB = b; tintA = a; return this; }
    LevelConfig cur(double c) { current = c; return this; }
    LevelConfig shuf(int s) { shuffle = s; return this; }
    LevelConfig rain() { rain = true; return this; }
    LevelConfig tip(String t) { tip = t; return this; }
    LevelConfig mode(Mode m, int slot) { mode = m; highSlot = slot; return this; }

    public boolean isCampaign() { return mode == Mode.CAMPAIGN; }
    public boolean hasRiver() { return river > 0; }
}
