package com.codevibers.ecoquest.view;

import java.util.HashMap;
import java.util.Map;

import com.almasb.fxgl.dsl.FXGL;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/** Texture cache. Names are relative to assets/textures, e.g. "bins/plastic.png". */
public final class Sprites {
    public static final int PX = 48;
    private static final Map<String, Image> CACHE = new HashMap<>();

    private Sprites() { }

    public static Image get(String name) {
        return CACHE.computeIfAbsent(name, n -> {
            try {
                return FXGL.getAssetLoader().loadImage(n);
            } catch (Exception e) {
                System.err.println("Missing texture: " + n + " (run tools/gen_sprites.py)");
                return placeholder();
            }
        });
    }

    public static ImageView view(String name) {
        ImageView v = new ImageView(get(name));
        v.setSmooth(false);
        return v;
    }

    private static Image placeholder() {
        WritableImage img = new WritableImage(PX, PX);
        for (int x = 0; x < PX; x++) {
            for (int y = 0; y < PX; y++) img.getPixelWriter().setColor(x, y, ((x / 8 + y / 8) % 2 == 0) ? Color.MAGENTA : Color.BLACK);
        }
        return img;
    }
}
