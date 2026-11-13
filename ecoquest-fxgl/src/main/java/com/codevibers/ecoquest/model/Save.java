package com.codevibers.ecoquest.model;

import java.io.*;
import java.util.Properties;

/** Progress, wallet, upgrades and best scores, stored in ~/.ecoquest.properties. */
public final class Save {
    private static final File FILE = new File(System.getProperty("user.home"), ".ecoquest.properties");

    /** Highest unlocked campaign level index. */
    public int unlocked;
    /** Index of the last campaign level the player has beaten (-1 = none yet). */
    public int completed = -1;
    /** True once a campaign playthrough has begun (this is what makes CONTINUE available). */
    public boolean started;
    /** Chosen sibling (index into Sibling.values()), -1 until the player picks one for this playthrough. */
    public int sibling = -1;
    public int wallet;
    public final int[] stars = new int[10];
    public final int[] high = new int[5];
    /** Upgrade levels (0-3): boots, gloves, scanner, boat, drone, clover. */
    public final int[] up = new int[6];
    public boolean muted;

    public static Save load() {
        Save s = new Save();
        try (FileInputStream in = new FileInputStream(FILE)) {
            Properties p = new Properties();
            p.load(in);
            s.unlocked = Integer.parseInt(p.getProperty("un", "0"));
            s.wallet = Integer.parseInt(p.getProperty("w", "0"));
            parse(p.getProperty("st"), s.stars);
            parse(p.getProperty("hs"), s.high);
            parse(p.getProperty("u"), s.up);
            s.muted = "1".equals(p.getProperty("muted"));
            // Newer keys. Saves from older versions don't have them, so work them out from the old data.
            int highestStar = -1;
            for (int i = 0; i < s.stars.length; i++) if (s.stars[i] > 0) highestStar = i;
            s.completed = Integer.parseInt(p.getProperty("lc", "" + Math.max(s.unlocked - 1, highestStar)));
            s.started = p.containsKey("started") ? "1".equals(p.getProperty("started")) : s.completed >= 0;
            // Saves from before character select: anyone mid-playthrough keeps the original (sister) look.
            s.sibling = Integer.parseInt(p.getProperty("sib", s.hasPlaythrough() ? "0" : "-1"));
            if (s.sibling < 0 || s.sibling >= Sibling.values().length) s.sibling = -1;
        } catch (Exception ignored) { }
        return s;
    }

    public void store() {
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            Properties p = new Properties();
            p.setProperty("un", "" + unlocked);
            p.setProperty("w", "" + wallet);
            p.setProperty("st", csv(stars));
            p.setProperty("hs", csv(high));
            p.setProperty("u", csv(up));
            p.setProperty("muted", muted ? "1" : "0");
            p.setProperty("lc", "" + completed);
            p.setProperty("started", started ? "1" : "0");
            p.setProperty("sib", "" + sibling);
            p.store(out, "EcoQuest save");
        } catch (Exception ignored) { }
    }

    public boolean hasChosenSibling() { return sibling >= 0; }

    /** The sibling to draw; the sister until a choice is made (e.g. in the game modes). */
    public Sibling sibling() { return sibling >= 0 ? Sibling.values()[sibling] : Sibling.SISTER; }

    public void chooseSibling(Sibling s) {
        sibling = s.ordinal();
        store();
    }

    /** True when a previous campaign playthrough exists to continue. */
    public boolean hasPlaythrough() { return started || completed >= 0; }

    /**
     * Wipes the story progress and the attributes (upgrades) for a fresh playthrough.
     * Wallet, mode high scores and the sound setting are kept (the wallet is the coin balance for cosmetics later).
     */
    public void newGame() {
        unlocked = 0;
        completed = -1;
        started = false;
        sibling = -1;   // the sibling is part of the story, so it is picked again on a new game
        java.util.Arrays.fill(stars, 0);
        java.util.Arrays.fill(up, 0);
        store();
    }

    public static int upgradeCost(int level) { return 80 + level * 80; }

    private static String csv(int[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) sb.append(i > 0 ? "," : "").append(a[i]);
        return sb.toString();
    }

    private static void parse(String s, int[] a) {
        if (s == null) return;
        String[] p = s.split(",");
        for (int i = 0; i < a.length && i < p.length; i++) {
            try { a[i] = Integer.parseInt(p[i].trim()); } catch (Exception ignored) { }
        }
    }
}
