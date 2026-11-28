package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.Sfx;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/** Shared layout for level select, game modes and the shop: titled list on top, description box below. */
abstract class ListScreen extends Screen {
    private static final double MAX_ROW = 31;
    private static final double ROWS_Y = 52;                   // where the first row starts on screen
    private static final double PANEL_Y = 10;
    private static final double BOX_Y = Ui.H - 98;             // top of the description box
    private static final double PANEL_MAX_BOTTOM = BOX_Y - 6;  // keep a small gap above the description box
    private static final double PANEL_PAD_BOTTOM = 12;

    private final Group rows = new Group();
    private final Ui.Label detail = new Ui.Label("", 10, Ui.INK);
    private final Ui.Label heading;
    private final double ROW;   // row height: 31, or smaller when there are too many rows to fit above the description box
    protected int index;

    ListScreen(EcoQuestApp app, String title) {
        super(app);
        root.getChildren().add(Sprites.view("backdrop/title.png"));
        Rectangle dim = new Rectangle(Ui.W, Ui.H, Color.rgb(0, 0, 0, 0.35));
        int n = Math.max(1, count0());
        ROW = Math.min(MAX_ROW, Math.floor((PANEL_MAX_BOTTOM - PANEL_PAD_BOTTOM - ROWS_Y) / n));
        // The panel reaches from the top edge down past the last row, so rows never spill over the frame.
        Group list = Ui.panel(Ui.W - 24, ROWS_Y - PANEL_Y + n * ROW + PANEL_PAD_BOTTOM - 2);
        list.setLayoutX(12);
        list.setLayoutY(PANEL_Y);
        heading = new Ui.Label(title, 13, Ui.FRAME_OUT);
        heading.setLayoutX(36);
        heading.setLayoutY(24);
        rows.setLayoutX(12);
        rows.setLayoutY(ROWS_Y);
        Group box = Ui.panel(Ui.W - 12, 92);
        box.setLayoutX(6);
        box.setLayoutY(Ui.H - 98);
        detail.setLayoutX(26);
        detail.setLayoutY(Ui.H - 82);
        detail.setWrap(Ui.W - 56);
        root.getChildren().addAll(dim, list, heading, rows, box, detail);
    }

    private int count0() { return rowCount(); }

    abstract int rowCount();
    abstract String left(int i);
    abstract String right(int i);
    abstract boolean enabled(int i);
    abstract String describe(int i);
    abstract void pick(int i);

    protected final void refresh() {
        rows.getChildren().clear();
        for (int i = 0; i < rowCount(); i++) {
            double y = i * ROW;
            if (i == index) {
                Rectangle hi = new Rectangle(8, y, Ui.W - 48, ROW - 3);
                hi.setFill(Color.web("#d8f0d8"));
                Polygon cur = new Polygon(0, 0, 10, 6, 0, 12);
                cur.setFill(Ui.FRAME_OUT);
                cur.setLayoutX(18);
                cur.setLayoutY(y + 9);
                rows.getChildren().addAll(hi, cur);
            }
            Color c = enabled(i) ? Ui.INK : Color.web("#9a9a98");
            Ui.Label l = new Ui.Label(left(i), 11, c);
            l.setLayoutX(40);
            l.setLayoutY(y + 8);
            Ui.Label r = new Ui.Label(right(i), 11, enabled(i) ? Ui.FRAME_IN : c);
            r.setLayoutX(Ui.W - 48 - r.textWidth());
            r.setLayoutY(y + 8);
            rows.getChildren().addAll(l, r);
        }
        detail.setText(describe(index));
    }

    @Override
    public void enter() { refresh(); }

    @Override
    public void onDirection(Direction d) {
        if (d == Direction.UP) index = Math.floorMod(index - 1, rowCount());
        else if (d == Direction.DOWN) index = Math.floorMod(index + 1, rowCount());
        else return;
        app.sfx(Sfx.CURSOR);
        refresh();
    }

    @Override
    public void onConfirm() {
        pick(index);
        refresh();
    }

    @Override
    public void onCancel() { app.showMainMenu(); }

    /** The title song keeps playing on this screen. */
    @Override
    public boolean titleMusic() { return true; }
}
