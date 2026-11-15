package com.codevibers.ecoquest.view;

import java.util.HashMap;
import java.util.Map;

import com.codevibers.ecoquest.model.Cosmetic;

/**
 * Pure colour-swap maths for the cosmetics (no JavaFX in here). The sheets are drawn with flat colours at an exact
 * integer scale, so swapping a few exact ARGB values recolours the hair or hull and nothing else.
 * The source colours below are the ones tools/gen_sprites.py draws; change them together.
 */
public final class PlayerPalette {
    private static final int HAIR = argb(43, 40, 56);
    private static final int HULL = argb(224, 176, 136);
    private static final int HULL_HI = argb(240, 204, 166);

    private PlayerPalette() { }

    private static int argb(int r, int g, int b) { return 0xff000000 | r << 16 | g << 8 | b; }

    private static int lighten(int c, double t) {
        int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
        return argb((int) (r + (255 - r) * t), (int) (g + (255 - g) * t), (int) (b + (255 - b) * t));
    }

    /** Old colour to new colour for one sheet. Defaults add nothing. */
    public static Map<Integer, Integer> swaps(boolean boat, Cosmetic hair, Cosmetic paint) {
        Map<Integer, Integer> m = new HashMap<>();
        if (!hair.isDefault()) m.put(HAIR, 0xff000000 | hair.rgb());
        if (boat && !paint.isDefault()) {
            int c = 0xff000000 | paint.rgb();
            m.put(HULL, c);
            m.put(HULL_HI, lighten(c, 0.35));
        }
        return m;
    }

    /** Applies the swaps to ARGB pixels in place. */
    public static void apply(int[] pixels, Map<Integer, Integer> swaps) {
        if (swaps.isEmpty()) return;
        for (int i = 0; i < pixels.length; i++) {
            Integer to = swaps.get(pixels[i]);
            if (to != null) pixels[i] = to;
        }
    }
}
