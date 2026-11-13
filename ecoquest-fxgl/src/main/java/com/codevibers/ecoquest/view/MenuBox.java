package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.shape.Polygon;

/** Vertical choice list with a blinking arrow cursor (Emerald start-menu look). */
public final class MenuBox extends Group {
    public record Entry(String label, Runnable action) { }

    private static final double ROW = 34;
    private static final double PAD = 16;

    private final List<Entry> entries;
    private final List<Ui.Label> labels = new ArrayList<>();
    private final boolean[] disabled;
    private final Polygon cursor = new Polygon(0, 0, 10, 6, 0, 12);
    private final double height;
    private int index;
    private double t;

    /** title may be null. */
    public MenuBox(String title, List<Entry> entries, double width) {
        this.entries = entries;
        this.disabled = new boolean[entries.size()];
        double top = PAD;
        Ui.Label head = null;
        if (title != null) {
            head = new Ui.Label(title, 12, Ui.FRAME_IN);
            head.setLayoutX(PAD + 22);
            head.setLayoutY(PAD);
            top += 30;
        }
        height = top + entries.size() * ROW + PAD - 4;
        getChildren().add(Ui.panel(width, height));
        if (head != null) getChildren().add(head);
        for (int i = 0; i < entries.size(); i++) {
            Ui.Label l = new Ui.Label(entries.get(i).label(), 12, Ui.INK);
            l.setLayoutX(PAD + 26);
            l.setLayoutY(top + i * ROW + 8);
            labels.add(l);
            getChildren().add(l);
        }
        cursor.setFill(Ui.FRAME_OUT);
        cursor.setLayoutX(PAD + 6);
        getChildren().add(cursor);
        top0 = top;
        place();
    }

    private final double top0;

    public double height() { return height; }
    public int index() { return index; }

    public void setLabel(int i, String text) { labels.get(i).setText(text); }

    /** Greyed-out entries can still be highlighted, but confirm() does nothing for them. */
    public void setEnabled(int i, boolean on) {
        disabled[i] = !on;
        labels.get(i).setFill(on ? Ui.INK : javafx.scene.paint.Color.web("#9a9a98"));
    }

    public boolean isEnabled(int i) { return !disabled[i]; }

    public void setIndex(int i) {
        index = Math.floorMod(i, entries.size());
        t = 0;
        place();
    }

    public void move(int delta) {
        index = Math.floorMod(index + delta, entries.size());
        t = 0;
        place();
    }

    public void confirm() { if (!disabled[index]) entries.get(index).action().run(); }

    public void update(double tpf) {
        t += tpf;
        cursor.setVisible(Math.sin(t * 9) > -0.6);
    }

    private void place() { cursor.setLayoutY(top0 + index * ROW + 8); }
}
