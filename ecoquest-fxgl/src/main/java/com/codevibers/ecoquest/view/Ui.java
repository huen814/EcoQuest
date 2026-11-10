package com.codevibers.ecoquest.view;

import java.io.InputStream;

import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/** Shared look: soft pastel dialog panels with a lavender frame and hard-shadowed pixel text. */
public final class Ui {
    public static final int W = 720;
    public static final int H = 480;

    public static final Color INK = Color.web("#3a3552");
    public static final Color SHADOW = Color.web("#d6d0ea");
    public static final Color PANEL = Color.web("#fcfaff");
    public static final Color FRAME_OUT = Color.web("#2c2748");
    public static final Color FRAME_IN = Color.web("#9a8fd6");
    public static final Color GOOD = Color.web("#25a08a");
    public static final Color BAD = Color.web("#d9566f");
    public static final Color ACCENT = Color.web("#d98a4a");
    public static final Color WHITE = Color.WHITE;

    private static final String FONT_RESOURCE = "/assets/ui/fonts/PressStart2P.ttf";
    private static final java.util.Map<Double, Font> FONTS = new java.util.HashMap<>();
    private static String family;
    private static boolean fontTried;

    private Ui() { }

    /** Pixel font if assets/ui/fonts/PressStart2P.ttf exists (drop it in), bold monospace otherwise. */
    public static Font font(double size) {
        if (!fontTried) {
            fontTried = true;
            try (InputStream in = Ui.class.getResourceAsStream(FONT_RESOURCE)) {
                if (in != null) {
                    Font f = Font.loadFont(in, 12);
                    if (f != null) family = f.getFamily();
                }
            } catch (Exception ignored) { }
        }
        return FONTS.computeIfAbsent(size, sz -> family != null ? Font.font(family, sz) : Font.font("Monospaced", FontWeight.BOLD, sz));
    }

    /** Rounded dialog panel: dark outer edge, green band, white body. */
    public static Group panel(double w, double h) {
        Rectangle shadow = new Rectangle(w, h, Color.rgb(0, 0, 0, 0.25));
        shadow.setArcWidth(16);
        shadow.setArcHeight(16);
        shadow.setTranslateY(3);
        Rectangle outer = new Rectangle(w, h, FRAME_OUT);
        outer.setArcWidth(14);
        outer.setArcHeight(14);
        Rectangle mid = new Rectangle(3, 3, w - 6, h - 6);
        mid.setFill(FRAME_IN);
        mid.setArcWidth(10);
        mid.setArcHeight(10);
        Rectangle body = new Rectangle(6, 6, w - 12, h - 12);
        body.setFill(PANEL);
        body.setArcWidth(6);
        body.setArcHeight(6);
        return new Group(shadow, outer, mid, body);
    }

    /** Pixel text with a hard drop shadow; layout origin is the top-left of the text. */
    public static final class Label extends Text {
        public Label(String text, double size, Color color) {
            super(text);
            setFont(font(size));
            setFill(color);
            setTextOrigin(VPos.TOP);
            setLineSpacing(Math.round(size * 0.6));
            shadow(color.getBrightness() > 0.8 ? Color.web("#2c2748") : SHADOW);
        }

        public Label shadow(Color c) {
            setEffect(new DropShadow(0, 1.5, 1.5, c));
            return this;
        }

        public void setWrap(double width) { setWrappingWidth(width); }
        public double textWidth() { return getLayoutBounds().getWidth(); }
    }
}
