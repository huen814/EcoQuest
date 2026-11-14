package com.codevibers.ecoquest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** XP, levels and the level-up attribute choices. */
public final class Progression {
    public static final int MAX_LEVEL = 30;

    public static final double XP_SORT = 4, XP_PLANT = 10, XP_FISH = 8, XP_MISSION = 30;
    /** A level you have already cleared gives only this share of the XP, so replaying one level cannot be farmed. */
    public static final double REPLAY_SCALE = 0.4;
    public static final double QUICK_LEARNER_PER_LEVEL = 0.10;

    public static final double PRIMARY_SHARE = 0.70;
    /** Attributes the Guardian does not have yet are this many times likelier to be offered than ones they could just level up. */
    public static final double NEW_ATTRIBUTE_WEIGHT = 2.0;
    public static final int OFFER_SIZE = 3;

    public record Gain(int level, List<Attribute> offer) { }

    public record Result(int xp, int levelBefore, int levelAfter, List<Gain> gains) {
        public boolean leveledUp() { return levelAfter > levelBefore; }
    }

    private Progression() { }

    public static int xpForNext(int level) { return 50 + 25 * (level - 1); }

    public static int clearBonus(int levelIndex, int stars) { return 40 + 8 * Math.max(0, levelIndex) + 10 * stars; }

    /** {@code guaranteeLevelUp}: the first clear of a story level always levels you up, even if the XP alone would not. */
    public static Result apply(Save save, int gained, boolean guaranteeLevelUp, Random rng) {
        int before = save.level;
        List<Gain> gains = new ArrayList<>();
        if (save.level < MAX_LEVEL) save.xp += Math.max(0, gained);
        while (save.level < MAX_LEVEL && save.xp >= xpForNext(save.level)) {
            save.xp -= xpForNext(save.level);
            save.level++;
            gains.add(new Gain(save.level, roll(save, rng)));
        }
        if (guaranteeLevelUp && gains.isEmpty() && save.level < MAX_LEVEL) {
            save.xp = 0;
            save.level++;
            gains.add(new Gain(save.level, roll(save, rng)));
        }
        if (save.level >= MAX_LEVEL) save.xp = 0;
        return new Result(Math.max(0, gained), before, save.level, Collections.unmodifiableList(gains));
    }

    static List<Attribute> roll(Save save, Random rng) {
        List<Attribute> offer = new ArrayList<>();
        while (offer.size() < OFFER_SIZE) {
            Attribute a = pickOne(save, rng, offer);
            if (a == null) break;
            offer.add(a);
        }
        if (offer.isEmpty()) return offer;
        save.offers.add(offer);
        return offer;
    }

    private static Attribute pickOne(Save save, Random rng, List<Attribute> exclude) {
        boolean wantPrimary = rng.nextDouble() < PRIMARY_SHARE;
        List<Attribute> pool = candidates(save, wantPrimary, exclude);
        if (pool.isEmpty()) pool = candidates(save, !wantPrimary, exclude);
        if (pool.isEmpty()) return null;
        double total = 0;
        for (Attribute a : pool) total += weight(save, a);
        double pick = rng.nextDouble() * total;
        for (Attribute a : pool) {
            pick -= weight(save, a);
            if (pick < 0) return a;
        }
        return pool.get(pool.size() - 1);
    }

    private static double weight(Save save, Attribute a) { return save.up[a.slot] == 0 ? NEW_ATTRIBUTE_WEIGHT : 1.0; }

    private static List<Attribute> candidates(Save save, boolean primary, List<Attribute> exclude) {
        List<Attribute> out = new ArrayList<>();
        for (Attribute a : Attribute.values()) {
            if (a.isPrimary() == primary && save.up[a.slot] < a.max && !exclude.contains(a)) out.add(a);
        }
        return out;
    }

    /** The oldest waiting choice, minus attributes that have been maxed since it was rolled. Empty when none is waiting. */
    public static List<Attribute> currentOffer(Save save) {
        while (!save.offers.isEmpty()) {
            List<Attribute> live = new ArrayList<>();
            for (Attribute a : save.offers.get(0)) if (save.up[a.slot] < a.max) live.add(a);
            if (!live.isEmpty()) return live;
            save.offers.remove(0);
        }
        return List.of();
    }

    public static boolean hasOffer(Save save) { return !currentOffer(save).isEmpty(); }

    public static int pendingChoices(Save save) {
        int n = 0;
        for (List<Attribute> o : save.offers) {
            for (Attribute a : o) if (save.up[a.slot] < a.max) { n++; break; }
        }
        return n;
    }

    /** Takes the pick from the oldest waiting choice and saves. False if it was not on offer. */
    public static boolean choose(Save save, Attribute pick) {
        if (!currentOffer(save).contains(pick)) return false;
        save.up[pick.slot]++;
        save.offers.remove(0);
        save.store();
        return true;
    }

    public static List<String> summary(Save save, Result r) {
        List<String> lines = new ArrayList<>();
        lines.add("XP +" + r.xp());
        lines.add(r.levelAfter() >= MAX_LEVEL ? "LV " + r.levelAfter() + "  MAX"
                : "LV " + r.levelAfter() + "  " + save.xp + "/" + xpForNext(r.levelAfter()));
        int shown = 0;
        for (Gain g : r.gains()) {
            if (shown == 2) {
                lines.add("+" + (r.gains().size() - shown) + " MORE LEVEL-UPS");
                break;
            }
            lines.add("LV " + g.level() + ": " + (g.offer().isEmpty() ? "ALL MAXED" : "CHOICE EARNED"));
            shown++;
        }
        return lines;
    }
}
