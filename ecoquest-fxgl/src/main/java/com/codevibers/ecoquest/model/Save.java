package com.codevibers.ecoquest.model;

import java.io.*;
import java.util.Properties;

/** Progress, wallet, upgrades and best scores, stored in ~/.ecoquest.properties. */
public final class Save {
    private static final File FILE = new File(System.getProperty("user.home"), ".ecoquest.properties");

    /** Highest unlocked campaign level index. */
    public int unlocked;
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
            p.store(out, "EcoQuest save");
        } catch (Exception ignored) { }
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
