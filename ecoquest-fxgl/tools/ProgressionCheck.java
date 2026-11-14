// Headless check of the XP / attribute system (no JavaFX needed). Not part of the Maven build.
// From ecoquest-fxgl/:
//   mkdir -p /tmp/eq-out && javac -d /tmp/eq-out $(find src/main/java/com/codevibers/ecoquest/model -name "*.java") src/main/java/com/codevibers/ecoquest/Cosmetics.java tools/ProgressionCheck.java
//   java -Duser.home=/tmp/eq-home -cp /tmp/eq-out com.codevibers.ecoquest.model.ProgressionCheck
// (user.home is redirected so the check never touches your real ~/.ecoquest.properties)
package com.codevibers.ecoquest.model;

import java.io.*;
import java.util.*;

public class ProgressionCheck {
    static int fails = 0; static double cool = 0;
    static void ok(boolean c, String msg) { if (!c) { fails++; System.out.println("FAIL: " + msg); } else System.out.println("ok:   " + msg); }

    public static void main(String[] a) throws Exception {
        File f = new File(System.getProperty("user.home"), ".ecoquest.properties");
        try (FileWriter w = new FileWriter(f)) {
            w.write("un=3\nw=120\nst=3,2,1,0,0,0,0,0,0,0\nu=2,3,3,1,2,3\nlc=2\nstarted=1\nsib=0\n");
        }
        Save old = Save.load();
        ok(old.up.length == Attribute.COUNT, "up array has " + Attribute.COUNT + " slots");
        ok(old.up[0] == 2 && old.up[1] == 3 && old.up[2] == 1 && old.up[5] == 3, "old upgrades kept, scanner clamped to its cap: " + Arrays.toString(old.up));
        ok(old.level == 4 && old.xp == 0, "old save (3 levels cleared) starts at level " + old.level);
        old.xp = 17; old.level = 6; old.pity = 1; old.store();
        Save back = Save.load();
        ok(back.xp == 17 && back.level == 6 && back.pity == 1, "xp/level/pity round-trip");
        back.newGame();
        ok(back.xp == 0 && back.level == 1 && back.pity == 0 && Arrays.stream(back.up).sum() == 0, "New Game resets xp, level, attributes");
        ok(back.wallet == 120, "coins survive New Game");
        Random r = new Random(1);
        int N = 3000, maxStreak = 0, capBreaks = 0, levelups = 0, attrs = 0;
        Set<String> builds = new HashSet<>();
        int[] primaryRank = new int[Attribute.COUNT];
        for (int n = 0; n < N; n++) {
            Save s = new Save();
            int streak = 0;
            for (int run = 0; run < 10; run++) {
                Progression.Result res = Progression.apply(s, 140 + 20 * run, r);
                for (Progression.Gain g : res.gains()) {
                    levelups++;
                    if (g.attribute() == null) { streak++; maxStreak = Math.max(maxStreak, streak); }
                    else { streak = 0; attrs++; }
                }
            }
            for (Attribute at : Attribute.values()) if (s.up[at.slot] > at.max) capBreaks++;
            builds.add(Arrays.toString(s.up));
        }
        ok(capBreaks == 0, "no attribute ever above its max");
        ok(maxStreak <= Progression.PITY_LIMIT, "pity: longest dry streak = " + maxStreak);
        System.out.printf("info: avg level-ups per 10-level campaign %.1f, attributes gained %.1f, distinct builds %d / %d%n",
                levelups / (double) N, attrs / (double) N, builds.size(), N);

        Save m = new Save();
        for (Attribute at : Attribute.values()) m.up[at.slot] = at.max;
        Progression.Result mr = Progression.apply(m, 5000, r);
        ok(mr.gains().stream().allMatch(g -> g.attribute() == null), "maxed build gets no further attributes");
        Save big = new Save();
        Progression.apply(big, 10_000_000, r);
        ok(big.level == Progression.MAX_LEVEL && big.xp == 0, "level caps at " + Progression.MAX_LEVEL);
        for (int lvl = 0; lvl < 3; lvl++) {
            Save s = new Save();
            s.sibling = 0;
            GameSession g = new GameSession(Levels.CAMPAIGN.get(lvl), lvl, s, new Random(7));
            double t = 0;
            while (!g.ended() && t < 900) { bot(g); g.tick(0.05); t += 0.05; }
            Progression.Result pr = g.progression();
            if (pr == null) { System.out.println("info: level " + (lvl + 1) + " bot could not finish (needs planting/boat) - skipped"); continue; }
            System.out.printf("info: level %d bot: won=%b score=%d sorted=%d t=%.0fs xp=%d level %d->%d gains=%s%n",
                    lvl + 1, g.won(), g.score(), g.sorted(), t, g.xpEarned(), pr.levelBefore(), pr.levelAfter(), pr.gains());
            ok(pr != null && g.xpEarned() > 0, "campaign run earns XP");
            if (g.won()) {
                GameSession g2 = new GameSession(Levels.CAMPAIGN.get(lvl), lvl, s, new Random(7));
                double t2 = 0;
                while (!g2.ended() && t2 < 900) { bot(g2); g2.tick(0.05); t2 += 0.05; }
                System.out.printf("info: level %d replay xp=%d (first run %d)%n", lvl + 1, g2.xpEarned(), g.xpEarned());
                ok(g2.xpEarned() < g.xpEarned(), "replay earns less XP than the first clear");
            }
        }
        Save s = new Save();
        GameSession z = new GameSession(Levels.MODES.get(3), -1, s, new Random(3));
        double t = 0;
        while (!z.ended() && t < 300) { bot(z); z.tick(0.05); t += 0.05; }
        ok(z.xpEarned() == 0 && z.progression() == null && s.xp == 0 && s.level == 1, "game mode run gives no XP (sorted " + z.sorted() + ")");
        Save e = new Save();
        e.up[Attribute.STEADY_HANDS.slot] = 2; e.up[Attribute.COIN_PURSE.slot] = 2; e.up[Attribute.FRESH_AIR.slot] = 2;
        GameSession ge = new GameSession(Levels.CAMPAIGN.get(0), 0, e, new Random(7));
        double te = 0;
        while (!ge.ended() && te < 120 && ge.sorted() < 3) { bot(ge); ge.tick(0.05); te += 0.05; }
        ok(ge.sorted() >= 3 && ge.money() == ge.sorted() * 5, "Coin Purse 2: " + ge.sorted() + " sorts = " + ge.money() + " coins");

        System.out.println(fails == 0 ? "ALL CHECKS PASSED" : fails + " CHECK(S) FAILED");
    }
    static void bot(GameSession g) {
        Player p = g.player();
        if (g.ended()) return;
        if (cool > 0) { cool -= 0.05; return; }
        Pickup top = p.carried().isEmpty() ? null : p.carried().get(p.carried().size() - 1);
        int tc, tr; Direction face = null;
        if (top instanceof WasteItem w) {
            Bin b = null;
            for (Bin x : g.bins()) if (x.type() == w.bin()) b = x;
            tc = b.col(); tr = b.row() + 1; face = Direction.UP;
        } else if (top != null) {
            g.interact(); return;
        } else {
            Pickup best = null; double bd = 1e9;
            for (Pickup it : g.items()) {
                if (!it.isGarbage() || it.river()) continue;
                if (g.map().isBoatWater(it.col(), it.row())) continue;
                double d = Math.abs(it.col() - p.col()) + Math.abs(it.row() - p.row());
                if (d < bd) { bd = d; best = it; }
            }
            if (best == null) return;
            tc = best.col(); tr = best.row();
        }
        if (p.col() == tc && p.row() == tr) {
            if (face != null) g.turn(face);
            g.interact();
            return;
        }
        Direction d;
        if (p.row() != tr && (p.col() == tc || Math.random() < 0.5 || p.row() == TileMap.BIN_ROW + 1)) d = tr > p.row() ? Direction.DOWN : Direction.UP;
        else d = tc > p.col() ? Direction.RIGHT : Direction.LEFT;
        if (p.col() == tc && p.row() != tr) d = tr > p.row() ? Direction.DOWN : Direction.UP;
        if (!g.tryMove(d)) {
            Direction alt = (d == Direction.UP || d == Direction.DOWN) ? (p.col() < 14 ? Direction.RIGHT : Direction.LEFT) : (p.row() > 0 ? Direction.DOWN : Direction.UP);
            g.tryMove(alt);
        }
        cool = 0.12;
    }
}
