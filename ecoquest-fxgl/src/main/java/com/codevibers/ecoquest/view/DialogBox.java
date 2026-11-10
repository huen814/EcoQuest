package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.shape.Polygon;

/**
 * Bottom text box in the Emerald style. Two uses: paged typewriter dialog (show/advance) and
 * a short timed message with a coloured title (toast).
 */
public final class DialogBox extends Group {
    private static final double CHARS_PER_SECOND = 55;

    private final double w;
    private final double h;
    private final Ui.Label title = new Ui.Label("", 12, Ui.GOOD);
    private final Ui.Label body = new Ui.Label("", 12, Ui.INK);
    private final Polygon arrow = new Polygon(0, 0, 14, 0, 7, 9);
    private List<String> pages = new ArrayList<>();
    private int page;
    private double shown;
    private String full = "";
    private boolean paged;
    private double toastLeft;
    private double toastTotal;
    private double blink;

    public DialogBox(double x, double y, double w, double h) {
        this.w = w;
        this.h = h;
        setLayoutX(x);
        setLayoutY(y);
        title.setLayoutX(22);
        title.setLayoutY(16);
        body.setLayoutX(22);
        body.setWrap(w - 56);
        arrow.setFill(Ui.FRAME_IN);
        arrow.setLayoutX(w - 38);
        arrow.setLayoutY(h - 28);
        getChildren().addAll(Ui.panel(w, h), title, body, arrow);
        setVisible(false);
    }

    /** Starts a paged dialog; call advance() on each confirm press. */
    public void show(List<String> newPages) {
        pages = new ArrayList<>(newPages);
        page = 0;
        paged = true;
        toastLeft = 0;
        title.setText("");
        body.setLayoutY(20);
        setOpacity(1);
        setVisible(true);
        startPage();
    }

    private void startPage() {
        full = pages.get(page);
        shown = 0;
        body.setText("");
    }

    /** Skips the typing, or flips the page. Returns true once the last page was dismissed. */
    public boolean advance() {
        if (!isVisible() || !paged) return true;
        if (shown < full.length()) {
            shown = full.length();
            body.setText(full);
            return false;
        }
        if (page + 1 < pages.size()) {
            page++;
            startPage();
            return false;
        }
        hide();
        return true;
    }

    public void hide() {
        setVisible(false);
        paged = false;
        toastLeft = 0;
    }

    /** Shows a title line and a detail line for a few seconds. */
    public void toast(String head, String detail, boolean good, double seconds) {
        paged = false;
        title.setFill(good ? Ui.GOOD : Ui.BAD);
        title.setText(head.toUpperCase());
        full = detail.toUpperCase();
        shown = full.length();
        body.setText(full);
        body.setFont(Ui.font(detail.length() <= 60 ? 10 : 8));
        body.setLayoutY(44);
        toastLeft = toastTotal = seconds;
        arrow.setVisible(false);
        setOpacity(1);
        setVisible(true);
    }

    public void update(double tpf) {
        if (!isVisible()) return;
        if (paged) {
            body.setFont(Ui.font(12));
            if (shown < full.length()) {
                shown = Math.min(full.length(), shown + CHARS_PER_SECOND * tpf);
                body.setText(full.substring(0, (int) shown));
            }
            blink += tpf;
            arrow.setVisible(shown >= full.length() && Math.sin(blink * 8) > -0.3);
        } else if (toastLeft > 0) {
            toastLeft -= tpf;
            if (toastLeft <= 0) hide();
            else setOpacity(Math.min(1, toastLeft / 0.3));
        }
    }
}
