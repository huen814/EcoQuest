package com.codevibers.ecoquest.view;

import java.util.List;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Cosmetic;
import com.codevibers.ecoquest.model.Cosmetic.Slot;
import com.codevibers.ecoquest.model.Cosmetics;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.Save;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * Cosmetics shop. LEFT/RIGHT switch the tab (outfit, hair, boat), UP/DOWN browse, the confirm key buys or wears.
 * The character on the right previews the highlighted item on top of what is already worn.
 * Everything here is cosmetic: coins buy looks, never gameplay.
 */
public final class ShopScreen extends Screen {
    private static final double ROW = 30;
    private static final double LIST_X = 12, LIST_W = 412, PANEL_Y = 10, PANEL_H = 360;
    private static final double PREV_X = 432, PREV_W = Ui.W - 12 - PREV_X;
    private static final int FRAME_W = Sprites.PX, FRAME_H = 72;   // one 16x24 frame, drawn x3

    private final Group tabs = new Group();
    private final Group rows = new Group();
    private final Ui.Label coins = new Ui.Label("", 11, Ui.INK);
    private final Ui.Label detail = new Ui.Label("", 10, Ui.INK);
    private final Ui.Label previewName = new Ui.Label("", 10, Ui.FRAME_OUT);
    private final ImageView preview = new ImageView();
    private final Sibling who;
    private int tab;
    private int index;
    private double t;

    public ShopScreen(EcoQuestApp app) {
        super(app);
        who = app.save().sibling();
        root.getChildren().add(Sprites.view("backdrop/title.png"));
        Rectangle dim = new Rectangle(Ui.W, Ui.H, Color.rgb(0, 0, 0, 0.35));

        Group list = Ui.panel(LIST_W, PANEL_H);
        list.setLayoutX(LIST_X);
        list.setLayoutY(PANEL_Y);
        Group side = Ui.panel(PREV_W, PANEL_H);
        side.setLayoutX(PREV_X);
        side.setLayoutY(PANEL_Y);

        Ui.Label title = new Ui.Label("COSMETICS SHOP", 13, Ui.FRAME_OUT);
        title.setLayoutX(36);
        title.setLayoutY(24);
        coins.setLayoutY(26);
        tabs.setLayoutX(12);
        tabs.setLayoutY(52);
        rows.setLayoutX(12);
        rows.setLayoutY(92);

        preview.setSmooth(false);
        preview.setFitWidth(FRAME_W * 3);
        preview.setFitHeight(FRAME_H * 3);
        preview.setLayoutX(PREV_X + (PREV_W - FRAME_W * 3) / 2);
        preview.setLayoutY(PANEL_Y + 34);
        previewName.setLayoutY(PANEL_Y + 34 + FRAME_H * 3 + 14);

        Group box = Ui.panel(Ui.W - 12, 92);
        box.setLayoutX(6);
        box.setLayoutY(Ui.H - 98);
        detail.setLayoutX(26);
        detail.setLayoutY(Ui.H - 82);
        detail.setWrap(Ui.W - 56);

        root.getChildren().addAll(dim, list, side, title, coins, tabs, rows, preview, previewName, box, detail);
    }

    private Slot slot() { return Slot.values()[tab]; }

    private List<Cosmetic> items() { return Cosmetics.forSlot(slot(), who); }

    private Cosmetic selected() { return items().get(index); }

    private String status(Cosmetic c) {
        Save s = app.save();
        if (s.isWorn(c)) return "WORN";
        return s.owns(c) ? "OWNED" : c.cost() + " COINS";
    }

    private void refresh() {
        Save s = app.save();
        coins.setText("COINS " + s.wallet);
        coins.setLayoutX(LIST_X + LIST_W - 24 - coins.textWidth());

        tabs.getChildren().clear();
        double x = 0;
        for (int i = 0; i < Slot.values().length; i++) {
            Ui.Label l = new Ui.Label(Slot.values()[i].label, 11, i == tab ? Ui.WHITE : Ui.INK);
            double w = l.textWidth() + 20;
            Rectangle bg = new Rectangle(x, 0, w, 26);
            bg.setArcWidth(8);
            bg.setArcHeight(8);
            bg.setFill(i == tab ? Ui.FRAME_IN : Color.web("#e8e3f6"));
            l.setLayoutX(x + 10);
            l.setLayoutY(7);
            tabs.getChildren().addAll(bg, l);
            x += w + 8;
        }

        rows.getChildren().clear();
        List<Cosmetic> items = items();
        for (int i = 0; i < items.size(); i++) {
            Cosmetic c = items.get(i);
            double y = i * ROW;
            if (i == index) {
                Rectangle hi = new Rectangle(0, y, LIST_W - 24, ROW - 3);
                hi.setFill(Color.web("#d8f0d8"));
                Polygon cur = new Polygon(0, 0, 10, 6, 0, 12);
                cur.setFill(Ui.FRAME_OUT);
                cur.setLayoutX(8);
                cur.setLayoutY(y + 8);
                rows.getChildren().addAll(hi, cur);
            }
            if (!c.isDefault()) {   // colour chip (for a drawn design, its main colour)
                Rectangle sw = new Rectangle(28, y + 6, 14, 14);
                sw.setFill(Color.web(String.format("#%06x", c.rgb())));
                sw.setStroke(Ui.FRAME_OUT);
                rows.getChildren().add(sw);
            }
            boolean owned = s.owns(c);
            Ui.Label name = new Ui.Label(c.name().toUpperCase(), 11, Ui.INK);
            name.setLayoutX(52);
            name.setLayoutY(y + 8);
            Ui.Label right = new Ui.Label(status(c), 11, s.isWorn(c) ? Ui.GOOD : owned ? Ui.FRAME_IN : s.wallet >= c.cost() ? Ui.INK : Ui.BAD);
            right.setLayoutX(LIST_W - 24 - 12 - right.textWidth());
            right.setLayoutY(y + 8);
            rows.getChildren().addAll(name, right);
        }

        Cosmetic c = selected();
        detail.setText((c.blurb().isEmpty() ? "" : c.blurb().toUpperCase() + "\n") + "LOOKS ONLY: NEVER CHANGES GAMEPLAY.  CONFIRM: "
                + (s.owns(c) ? "WEAR" : "BUY") + "   LEFT/RIGHT: TAB");
        updatePreview();
    }

    /** The highlighted item shown on top of everything already worn. */
    private void updatePreview() {
        Save s = app.save();
        Cosmetic sel = selected();
        Cosmetic outfit = slot() == Slot.OUTFIT ? sel : s.worn(Slot.OUTFIT);
        Cosmetic hair = slot() == Slot.HAIR ? sel : s.worn(Slot.HAIR);
        Cosmetic paint = slot() == Slot.BOAT ? sel : s.worn(Slot.BOAT);
        preview.setImage(Wardrobe.sheet(who, slot() == Slot.BOAT, outfit, hair, paint));
        showFrame();
        previewName.setText(sel.name().toUpperCase());
        previewName.setLayoutX(PREV_X + (PREV_W - previewName.textWidth()) / 2);
    }

    /** Facing down; the character gently walks on the spot (or rows the boat). */
    private void showFrame() {
        int col = (int) (t / 0.25) % 4;
        preview.setViewport(new Rectangle2D(col * FRAME_W, 0, FRAME_W, FRAME_H));
    }

    @Override
    public void enter() { refresh(); }

    @Override
    public void update(double tpf) {
        t += tpf;
        showFrame();
    }

    @Override
    public void onDirection(Direction d) {
        if (d == Direction.UP || d == Direction.DOWN) {
            index = Math.floorMod(index + (d == Direction.UP ? -1 : 1), items().size());
        } else if (d == Direction.LEFT || d == Direction.RIGHT) {
            tab = Math.floorMod(tab + (d == Direction.LEFT ? -1 : 1), Slot.values().length);
            index = 0;
        } else {
            return;
        }
        app.sfx(Sfx.CURSOR);
        refresh();
    }

    @Override
    public void onConfirm() {
        Save s = app.save();
        Cosmetic c = selected();
        if (s.owns(c)) {
            if (s.isWorn(c)) { app.sfx(Sfx.DENY); return; }
            s.wear(c);
            app.sfx(Sfx.SELECT);
        } else if (s.buy(c)) {
            s.wear(c);              // wear it straight away; the player bought it to use it
            app.sfx(Sfx.BUY);
        } else {
            app.sfx(Sfx.DENY);
        }
        refresh();
    }

    @Override
    public void onCancel() { app.showMainMenu(); }
}
