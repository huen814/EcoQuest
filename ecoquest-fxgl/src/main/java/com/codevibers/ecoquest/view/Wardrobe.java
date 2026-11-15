package com.codevibers.ecoquest.view;

import java.util.HashMap;
import java.util.Map;

import com.codevibers.ecoquest.model.Cosmetic;
import com.codevibers.ecoquest.model.Cosmetic.Slot;
import com.codevibers.ecoquest.model.Save;
import com.codevibers.ecoquest.model.Sibling;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/** Builds the player sheet for a sibling wearing a set of cosmetics (cached). Cosmetics never touch gameplay. */
public final class Wardrobe {
    private static final Map<String, Image> CACHE = new HashMap<>();

    private Wardrobe() { }

    /** The sheet for what the save currently has equipped. */
    public static Image sheet(Save save, Sibling who, boolean boat) {
        return sheet(who, boat, save.worn(Slot.OUTFIT), save.worn(Slot.HAIR), save.worn(Slot.BOAT));
    }

    public static Image sheet(Sibling who, boolean boat, Cosmetic outfit, Cosmetic hair, Cosmetic paint) {
        Image base = Sprites.get(outfit.hasArt() ? who.artSheet(outfit.art(), boat) : who.sheet(boat));
        Map<Integer, Integer> swaps = PlayerPalette.swaps(boat, hair, paint);
        if (swaps.isEmpty()) return base;
        String key = who.id + boat + outfit.id() + hair.id() + (boat ? paint.id() : "");
        return CACHE.computeIfAbsent(key, k -> {
            int w = (int) base.getWidth(), h = (int) base.getHeight();
            int[] px = new int[w * h];
            base.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
            PlayerPalette.apply(px, swaps);
            WritableImage out = new WritableImage(w, h);
            out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
            return out;
        });
    }
}
