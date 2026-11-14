package com.codevibers.ecoquest.model;

import java.io.*;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.codevibers.ecoquest.model.Cosmetic.Slot;

/** Progress, coins, owned cosmetics and best scores, stored in ~/.ecoquest.properties. */
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
    /** Coins. Earned by playing, spent in the cosmetics shop. Kept across New Game. */
    public int wallet;
    public final int[] stars = new int[10];
    public final int[] high = new int[5];
    /**
     * Attribute levels, one slot per {@link Attribute} (the first six are the old shop upgrades: boots, gloves, scanner,
     * boat, drone, clover). They are now earned by levelling up; old saves keep what they bought.
     */
    public final int[] up = new int[Attribute.COUNT];
    public int xp;
    public int level = 1;
    /** Attribute choices earned but not picked yet, oldest first. Saved so quitting cannot skip or re-roll them. */
    public final java.util.List<java.util.List<Attribute>> offers = new java.util.ArrayList<>();
    /** Ids (e.g. "outfit_mint") of the cosmetics bought so far. The free defaults are always owned. Kept across New Game. */
    private final Set<String> owned = new LinkedHashSet<>();
    /** The cosmetic worn in each slot; a missing entry means the slot's default. Kept across New Game. */
    private final Map<Slot, String> worn = new EnumMap<>(Slot.class);
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
            for (Attribute a : Attribute.values()) s.up[a.slot] = Math.max(0, Math.min(a.max, s.up[a.slot]));
            s.muted = "1".equals(p.getProperty("muted"));
            // Cosmetics (absent in older saves, which simply start with none). Unknown ids are dropped.
            String own = p.getProperty("own", "");
            for (String raw : own.split(",")) {
                final String id = raw.trim();
                if (!id.isEmpty() && Cosmetics.ALL.stream().anyMatch(c -> c.id().equals(id) && !c.isDefault())) s.owned.add(id);
            }
            for (Slot slot : Slot.values()) {
                Cosmetic c = Cosmetics.find(slot, p.getProperty("eq_" + slot.name().toLowerCase(), Cosmetic.DEFAULT_ID));
                if (c != null && s.owns(c)) s.worn.put(slot, c.id());   // (not filtered by sibling here: sibling is read later)
            }
            // Newer keys. Saves from older versions don't have them, so work them out from the old data.
            int highestStar = -1;
            for (int i = 0; i < s.stars.length; i++) if (s.stars[i] > 0) highestStar = i;
            s.completed = Integer.parseInt(p.getProperty("lc", "" + Math.max(s.unlocked - 1, highestStar)));
            s.started = p.containsKey("started") ? "1".equals(p.getProperty("started")) : s.completed >= 0;
            // Saves from before character select: anyone mid-playthrough keeps the original (sister) look.
            s.sibling = Integer.parseInt(p.getProperty("sib", s.hasPlaythrough() ? "0" : "-1"));
            // Progression. Saves from before XP existed start at a level that matches how far they got.
            s.xp = Math.max(0, Integer.parseInt(p.getProperty("xp", "0")));
            s.level = Integer.parseInt(p.getProperty("lv", "" + (1 + Math.max(0, s.completed + 1))));
            s.level = Math.max(1, Math.min(Progression.MAX_LEVEL, s.level));
            parseOffers(p.getProperty("pend", ""), s.offers);
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
            p.setProperty("own", String.join(",", owned));
            for (Slot slot : Slot.values()) p.setProperty("eq_" + slot.name().toLowerCase(), worn.getOrDefault(slot, Cosmetic.DEFAULT_ID));
            p.setProperty("lc", "" + completed);
            p.setProperty("started", started ? "1" : "0");
            p.setProperty("sib", "" + sibling);
            p.setProperty("xp", "" + xp);
            p.setProperty("lv", "" + level);
            p.setProperty("pend", offersToString());
            p.store(out, "EcoQuest save");
        } catch (Exception ignored) { }
    }

    private String offersToString() {
        StringBuilder sb = new StringBuilder();
        for (java.util.List<Attribute> o : offers) {
            if (sb.length() > 0) sb.append(';');
            for (int i = 0; i < o.size(); i++) sb.append(i > 0 ? "." : "").append(o.get(i).slot);
        }
        return sb.toString();
    }

    private static void parseOffers(String csv, java.util.List<java.util.List<Attribute>> out) {
        if (csv == null || csv.isBlank()) return;
        for (String group : csv.split(";")) {
            java.util.List<Attribute> offer = new java.util.ArrayList<>();
            for (String tok : group.split("\\.")) {
                try {
                    int slot = Integer.parseInt(tok.trim());
                    for (Attribute a : Attribute.values()) if (a.slot == slot && !offer.contains(a)) offer.add(a);
                } catch (NumberFormatException ignored) { }
            }
            if (!offer.isEmpty()) out.add(offer);
        }
    }

    public int attr(Attribute a) { return up[a.slot]; }

    public boolean hasChosenSibling() { return sibling >= 0; }

    /** The sibling to draw; the sister until a choice is made (e.g. in the game modes). */
    public Sibling sibling() { return sibling >= 0 ? Sibling.values()[sibling] : Sibling.SISTER; }

    public void chooseSibling(Sibling s) {
        sibling = s.ordinal();
        store();
    }

    /** True when a previous campaign playthrough exists to continue. */
    public boolean hasPlaythrough() { return started || completed >= 0; }

    // ------------------------------------------------------------------ cosmetics

    public boolean owns(Cosmetic c) { return c.isDefault() || owned.contains(c.id()); }

    /** What is worn in a slot right now (the slot's default when nothing is equipped). */
    public Cosmetic worn(Slot slot) {
        Cosmetic c = Cosmetics.find(slot, worn.getOrDefault(slot, Cosmetic.DEFAULT_ID));
        // A design made for the other sibling is simply not shown on this one (it stays remembered in the save).
        return c != null && c.fits(sibling()) ? c : Cosmetics.defaultFor(slot);
    }

    public boolean isWorn(Cosmetic c) { return worn(c.slot()).equals(c); }

    /** Buys a cosmetic with coins. False (nothing changes) if it is already owned or the player cannot afford it. */
    public boolean buy(Cosmetic c) {
        if (owns(c) || wallet < c.cost()) return false;
        wallet -= c.cost();
        owned.add(c.id());
        store();
        return true;
    }

    /** Wears an owned cosmetic. False if it is not owned. */
    public boolean wear(Cosmetic c) {
        if (!owns(c) || !c.fits(sibling())) return false;
        worn.put(c.slot(), c.id());
        store();
        return true;
    }

    /**
     * Wipes the story progress and the attributes (upgrades) for a fresh playthrough.
     * Coins, owned and worn cosmetics, mode high scores and the sound setting are kept.
     */
    public void newGame() {
        unlocked = 0;
        completed = -1;
        started = false;
        sibling = -1;   // the sibling is part of the story, so it is picked again on a new game
        java.util.Arrays.fill(stars, 0);
        java.util.Arrays.fill(up, 0);
        xp = 0;
        level = 1;
        offers.clear();
        store();
    }

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
