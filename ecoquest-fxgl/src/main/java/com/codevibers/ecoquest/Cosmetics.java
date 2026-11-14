package com.codevibers.ecoquest.model;

import java.util.ArrayList;
import java.util.List;

import com.codevibers.ecoquest.model.Cosmetic.Slot;

/** The cosmetics catalogue. Ids are what the save file stores, so never rename one that has shipped. */
public final class Cosmetics {
    public static final List<Cosmetic> ALL = List.of(
            // Outfit: recolours the hoodie (and the sister's skirt/shorts, which share its colour).
            def(Slot.OUTFIT, "Original", "The soft off-white outfit you started with."),
            new Cosmetic(Slot.OUTFIT, "outfit_mint", "Mint Outfit", 120, 0x9fe0cf, "A fresh mint colour, like new sprouts."),
            new Cosmetic(Slot.OUTFIT, "outfit_sky", "Sky Outfit", 120, 0xa9d4f5, "Clear-sky blue for a clean-air day."),
            new Cosmetic(Slot.OUTFIT, "outfit_peach", "Peach Outfit", 150, 0xf8c9a8, "Warm peach, like a sunny afternoon."),
            new Cosmetic(Slot.OUTFIT, "outfit_lilac", "Lilac Outfit", 150, 0xcbb8ee, "Soft lilac, the same shade as the menus."),
            new Cosmetic(Slot.OUTFIT, "outfit_sun", "Sunny Outfit", 200, 0xf8e48a, "Bright and cheerful yellow."),
            // Drawn designs: own sprite sheets, one sibling each (see tools/gen_sprites.py, OUTFITS).
            Cosmetic.design(Slot.OUTFIT, "outfit_witch", "Witch Outfit", 350, 0x9b3fe8,
                    "A pointed hat and a flared black dress with vivid purple trim.", "witch", Sibling.SISTER),
            Cosmetic.design(Slot.OUTFIT, "outfit_idol", "Twin-Tail Idol", 350, 0xcf2c45,
                    "Twin tails, a red strapped vest, white puff sleeves and a black skirt.", "idol", Sibling.SISTER),
            Cosmetic.design(Slot.OUTFIT, "outfit_reaper", "Grim Reaper", 350, 0x343a58,
                    "A big dark hood, a skull face with glowing eyes and a ragged cloak.", "reaper", Sibling.BROTHER),
            Cosmetic.design(Slot.OUTFIT, "outfit_stage", "Stage Idol", 350, 0x7fc4ee,
                    "A sky-blue stage jacket with gold trim, a pink bow tie, a star clip and a headset mic.", "stage", Sibling.BROTHER),
            // Hair
            def(Slot.HAIR, "Original", "Near-black hair, the way it started."),
            new Cosmetic(Slot.HAIR, "hair_chestnut", "Chestnut", 100, 0x6b4430, "Warm brown hair."),
            new Cosmetic(Slot.HAIR, "hair_plum", "Plum", 150, 0x6a3d7a, "Deep plum purple."),
            new Cosmetic(Slot.HAIR, "hair_teal", "Teal", 150, 0x2f8f93, "Teal, to match the park trees."),
            new Cosmetic(Slot.HAIR, "hair_rose", "Rose", 200, 0xc9708a, "Pink rose hair."),
            new Cosmetic(Slot.HAIR, "hair_silver", "Silver", 250, 0xb8b8cc, "Shiny silver."),
            // Boat paint
            def(Slot.BOAT, "Original", "Plain wooden hull."),
            new Cosmetic(Slot.BOAT, "boat_mint", "Mint Boat", 100, 0x8fd9c8, "Mint-painted hull."),
            new Cosmetic(Slot.BOAT, "boat_sky", "Sky Boat", 100, 0x8ec6ee, "Sky-blue hull."),
            new Cosmetic(Slot.BOAT, "boat_coral", "Coral Boat", 150, 0xf09a8a, "Coral-red hull."),
            new Cosmetic(Slot.BOAT, "boat_lilac", "Lilac Boat", 150, 0xb9a6e8, "Lilac hull."),
            new Cosmetic(Slot.BOAT, "boat_sun", "Sunshine Boat", 200, 0xf4d675, "Golden-yellow hull."));

    private Cosmetics() { }

    private static Cosmetic def(Slot slot, String name, String blurb) {
        return new Cosmetic(slot, Cosmetic.DEFAULT_ID, name, 0, 0, blurb);
    }

    /** What a sibling can see and wear in a slot, in shop order. */
    public static List<Cosmetic> forSlot(Slot slot, Sibling who) {
        List<Cosmetic> out = new ArrayList<>();
        for (Cosmetic c : ALL) if (c.slot() == slot && c.fits(who)) out.add(c);
        return out;
    }

    public static Cosmetic defaultFor(Slot slot) {
        for (Cosmetic c : ALL) if (c.slot() == slot && c.isDefault()) return c;
        throw new IllegalStateException("no default for " + slot);
    }

    /** Looks an id up in a slot; null if the id is unknown (e.g. from a newer or edited save). */
    public static Cosmetic find(Slot slot, String id) {
        for (Cosmetic c : ALL) if (c.slot() == slot && c.id().equals(id)) return c;
        return null;
    }
}
