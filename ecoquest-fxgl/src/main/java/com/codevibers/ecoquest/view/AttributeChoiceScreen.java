package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Attribute;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.Progression;
import com.codevibers.ecoquest.model.Save;
import com.codevibers.ecoquest.model.Sfx;

import java.util.List;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public final class AttributeChoiceScreen extends Screen {
    private static final double CARD_W = 200, CARD_H = 280, GAP = 22, TOP = 100, LIFT = 14;
    private static final Color MAIN_TINT = Ui.ACCENT;
    private static final Color MINOR_TINT = Ui.GOOD;
    private static final Color PIP_EMPTY = Color.web("#d6d0ea");

    private final Runnable next;
    private final List<Attribute> offer;
    private final Group[] cards;
    private final Rectangle[] glows;
    private final double[] lift;
    private final Ui.Label confirmText = new Ui.Label("", 11, Ui.INK);
    private int index;
    private boolean done;

    public AttributeChoiceScreen(EcoQuestApp app, Runnable next) {
        super(app);
        this.next = next;
        Save save = app.save();
        offer = Progression.currentOffer(save);
        cards = new Group[offer.size()];
        glows = new Rectangle[offer.size()];
        lift = new double[offer.size()];

        root.getChildren().add(Sprites.view("backdrop/title.png"));
        root.getChildren().add(new Rectangle(Ui.W, Ui.H, Color.rgb(10, 8, 24, 0.72)));

        Ui.Label title = new Ui.Label("LEVEL UP!  LV " + save.level, 20, Ui.WHITE);
        title.setLayoutX((Ui.W - title.textWidth()) / 2);
        title.setLayoutY(22);
        int waiting = Progression.pendingChoices(save);
        Ui.Label sub = new Ui.Label("CHOOSE ONE ATTRIBUTE" + (waiting > 1 ? "   " + waiting + " CHOICES LEFT" : ""), 11, Ui.ACCENT);
        sub.setLayoutX((Ui.W - sub.textWidth()) / 2);
        sub.setLayoutY(56);
        root.getChildren().addAll(title, sub);

        double total = cards.length * CARD_W + (cards.length - 1) * GAP;
        double x0 = (Ui.W - total) / 2;
        for (int i = 0; i < cards.length; i++) {
            cards[i] = buildCard(offer.get(i), i);
            cards[i].setLayoutX(x0 + i * (CARD_W + GAP));
            cards[i].setLayoutY(TOP);
            root.getChildren().add(cards[i]);
        }

        double bw = 300;
        Group button = Ui.panel(bw, 46);
        button.setLayoutX((Ui.W - bw) / 2);
        button.setLayoutY(Ui.H - 82);
        confirmText.setLayoutY(Ui.H - 82 + 17);
        Ui.Label hint = new Ui.Label("LEFT / RIGHT: BROWSE     ESC: MENU", 9, Ui.FRAME_IN);
        hint.setLayoutX((Ui.W - hint.textWidth()) / 2);
        hint.setLayoutY(Ui.H - 26);
        root.getChildren().addAll(button, confirmText, hint);

        if (!offer.isEmpty()) select(0, false);
        for (int i = 0; i < cards.length; i++) apply(i);
    }

    private Group buildCard(Attribute a, int slot) {
        Group g = new Group();
        Color tint = a.isPrimary() ? MAIN_TINT : MINOR_TINT;

        Rectangle glow = new Rectangle(-6, -6, CARD_W + 12, CARD_H + 12);
        glow.setArcWidth(16);
        glow.setArcHeight(16);
        glow.setFill(tint);
        glow.setOpacity(0);
        glows[slot] = glow;
        g.getChildren().addAll(glow, Ui.panel(CARD_W, CARD_H));

        Ui.Label tag = new Ui.Label(a.isPrimary() ? "MAIN" : "MINOR", 9, Ui.FRAME_IN);
        tag.setLayoutX(14);
        tag.setLayoutY(14);
        g.getChildren().add(tag);

        double cx = CARD_W / 2, cy = 82, r = 40;
        Polygon outer = diamond(cx, cy, r, tint, Ui.FRAME_OUT);
        Polygon inner = diamond(cx, cy, r - 12, Color.rgb(255, 255, 255, 0.35), null);
        Ui.Label letter = new Ui.Label(a.label.substring(0, 1).toUpperCase(), 20, Ui.INK);
        letter.setLayoutX(cx - letter.textWidth() / 2);
        letter.setLayoutY(cy - 10);
        g.getChildren().addAll(outer, inner, letter);

        Ui.Label name = new Ui.Label(a.label.toUpperCase(), 11, Ui.INK);
        name.setLayoutX((CARD_W - name.textWidth()) / 2);
        name.setLayoutY(138);
        g.getChildren().add(name);

        int rank = app.save().attr(a);
        Ui.Label step = new Ui.Label("LV " + rank + " > " + (rank + 1), 10, Ui.FRAME_IN);
        step.setLayoutX((CARD_W - step.textWidth()) / 2);
        step.setLayoutY(160);
        g.getChildren().add(step);

        double pip = 12, pipGap = 6;
        double px = (CARD_W - (a.max * pip + (a.max - 1) * pipGap)) / 2;
        for (int i = 0; i < a.max; i++) {
            Rectangle p = new Rectangle(px + i * (pip + pipGap), 182, pip, pip);
            p.setFill(i < rank ? tint : i == rank ? Ui.WHITE : PIP_EMPTY);
            p.setStroke(i == rank ? tint : Ui.FRAME_IN);
            p.setStrokeWidth(2);
            g.getChildren().add(p);
        }

        Rectangle rule = new Rectangle(18, 208, CARD_W - 36, 2);
        rule.setFill(PIP_EMPTY);
        Ui.Label desc = new Ui.Label(a.blurb.toUpperCase(), 10, Ui.INK);
        desc.setWrap(CARD_W - 32);
        desc.setLayoutX(16);
        desc.setLayoutY(220);
        g.getChildren().addAll(rule, desc);
        return g;
    }

    private static Polygon diamond(double cx, double cy, double r, Color fill, Color stroke) {
        Polygon p = new Polygon(cx, cy - r, cx + r, cy, cx, cy + r, cx - r, cy);
        p.setFill(fill);
        if (stroke != null) {
            p.setStroke(stroke);
            p.setStrokeWidth(3);
        }
        return p;
    }

    private void select(int i, boolean sound) {
        index = i;
        confirmText.setText("ENTER: TAKE " + offer.get(i).label.toUpperCase());
        confirmText.setLayoutX((Ui.W - confirmText.textWidth()) / 2);
        if (sound) app.sfx(Sfx.CURSOR);
    }

    private void apply(int i) {
        cards[i].setTranslateY(-LIFT * lift[i]);
        cards[i].setScaleX(1 + 0.05 * lift[i]);
        cards[i].setScaleY(1 + 0.05 * lift[i]);
        cards[i].setOpacity(0.72 + 0.28 * lift[i]);
        glows[i].setOpacity(lift[i]);
    }

    @Override
    public void update(double tpf) {
        for (int i = 0; i < cards.length; i++) {
            double target = i == index ? 1 : 0;
            lift[i] += (target - lift[i]) * Math.min(1, tpf * 14);
            apply(i);
        }
    }

    @Override
    public void onDirection(Direction d) {
        if (done || cards.length < 2) return;
        if (d == Direction.LEFT) select(Math.max(0, index - 1), index > 0);
        else if (d == Direction.RIGHT) select(Math.min(cards.length - 1, index + 1), index < cards.length - 1);
    }

    @Override
    public void onConfirm() {
        if (done || offer.isEmpty()) return;
        done = true;
        if (!Progression.choose(app.save(), offer.get(index))) {
            done = false;
            return;
        }
        app.sfx(Sfx.SELECT);
        if (Progression.hasOffer(app.save())) app.showAttributeChoice(next);
        else next.run();
    }

    @Override
    public void onCancel() {
        if (!done) app.showMainMenu();
    }
}
