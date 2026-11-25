package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;

import javafx.animation.ScaleTransition;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Boss Replay (Main Menu -> Game Modes -> Boss Replay): pick a character, then replay a boss already beaten in the story.
 * Three cards in an HBox: Muck Mound, The Discarded Knight and Queen Muck. A card is locked until that boss is beaten in the story.
 * Works with the keyboard (LEFT/RIGHT pick a card, UP/DOWN or SPACE switch character, ENTER replays, X goes back) and the mouse.
 * The title-screen backdrop stays behind it; text and cards are styled to read clearly over the bright sky.
 *
 * Hook-up points: {@link #onReplayBossSelected(String, String)} and {@link #onBackToGameModes()}.
 */
public final class BossReplayScreen extends Screen {
    // palette: dark navy on the light sky, lime accent, slate cards
    private static final Color NAVY = Color.web("#1E293B");
    private static final Color NAVY_HOVER = Color.web("#334155");
    private static final Color LIME = Color.web("#A3E635");
    private static final Color LIME_HOT = Color.web("#BEF264");
    private static final Color CARD = Color.rgb(25, 38, 46, 0.72);
    private static final Color NAME = Color.web("#F8FAFC");
    private static final Color ATTACKS = Color.web("#E2E8F0");
    private static final Color MUTED = Color.web("#94A3B8");
    private static final Color LOCK_BTN = Color.web("#64748B");
    private static final Color SOFT_SHADOW = Color.rgb(255, 255, 255, 0.55);
    private static final Color DARK_SHADOW = Color.web("#0b1220");

    private static final double CARD_W = 210, CARD_H = 290, GAP = 15, ARC = 20;
    private static final double SELECTED_SCALE = 1.06;

    /** What differs from card to card. */
    private record CardSpec(BossDef boss, String badge, Color badgeColor, String attacks, String recordLabel, String button) { }

    private static final List<CardSpec> SPECS = List.of(
            new CardSpec(BossDef.MUCK_MOUND, "BOSS FIGHT", Color.web("#EF4444"),
                    "Gunk Shot \u2022 Sludge Bomb \u2022 Slime Rain \u2022 Sludge Ring", "Best Clear", "REPLAY FIGHT"),
            new CardSpec(BossDef.EXPERIMENT, "ESCAPE CHASE", Color.web("#F59E0B"),
                    "Junk Cubes \u2022 Sludge Bombs \u2022 Sad Ghosts", "Fastest Escape", "REPLAY CHASE"),
            new CardSpec(BossDef.QUEEN_MUCK, "FINAL BOSS", Color.web("#9333EA"),
                    "Ricochet Caps \u2022 Rusted Rebar \u2022 Rolling Tires \u2022 Leaky Batteries", "Best Clear", "REPLAY FIGHT"));

    private final List<Card> cards = new ArrayList<>();
    private final List<Pill> pills = new ArrayList<>();
    private Sibling character;
    private int index;

    /** m:ss.t, e.g. 0:45.3 */
    public static String time(double seconds) {
        int m = (int) (seconds / 60);
        return String.format(Locale.ROOT, "%d:%04.1f", m, seconds - m * 60);
    }

    public BossReplayScreen(EcoQuestApp app) {
        super(app);
        character = app.replayCharacter();

        root.getChildren().add(Sprites.view("backdrop/title.png"));     // keep the title-screen background

        // ---- header: title and character toggle
        Ui.Label title = label("BOSS REPLAY", 22, NAVY, SOFT_SHADOW);
        title.setLayoutX((Ui.W - title.textWidth()) / 2);
        title.setLayoutY(14);
        Ui.Label caption = label("PLAY AS", 8, NAVY, SOFT_SHADOW);
        caption.setLayoutX((Ui.W - caption.textWidth()) / 2);
        caption.setLayoutY(46);
        root.getChildren().addAll(title, caption);

        double pillW = 124, pillH = 28, pad = 3;
        double barW = 2 * pillW + 3 * pad, barH = pillH + 2 * pad;
        Rectangle bar = new Rectangle(barW, barH, Color.rgb(255, 255, 255, 0.55));
        bar.setArcWidth(barH);
        bar.setArcHeight(barH);
        bar.setStroke(NAVY);
        bar.setStrokeWidth(2);
        bar.setLayoutX((Ui.W - barW) / 2);
        bar.setLayoutY(60);
        root.getChildren().add(bar);
        double px = bar.getLayoutX() + pad;
        for (Sibling s : Sibling.values()) {
            Pill pill = new Pill(s, pillW, pillH);
            pill.setLayoutX(px);
            pill.setLayoutY(60 + pad);
            px += pillW + pad;
            pills.add(pill);
            root.getChildren().add(pill);
        }
        refreshPills();

        // ---- centre: three equal cards side by side
        HBox row = new HBox(GAP);
        row.setLayoutX((Ui.W - (3 * CARD_W + 2 * GAP)) / 2);
        row.setLayoutY(114);
        for (int i = 0; i < SPECS.size(); i++) {
            CardSpec spec = SPECS.get(i);
            Card card = new Card(spec, app.save().hasBeatenBoss(spec.boss().id()), app.save().bestBossTime(spec.boss().id()));
            final int at = i;
            card.setOnMouseEntered(e -> { if (index != at) { app.sfx(Sfx.CURSOR); select(at); } });
            card.setOnMouseClicked(e -> { select(at); activate(at); });
            cards.add(card);
            row.getChildren().add(card);
        }
        root.getChildren().add(row);

        // ---- footer: back button, last reward, key hints
        Pane back = button("< BACK TO GAME MODES", 236, 30, NAVY, Color.WHITE);
        back.setLayoutX(30);
        back.setLayoutY(426);
        back.setCursor(Cursor.HAND);
        back.setOnMouseEntered(e -> ((Rectangle) back.getChildren().get(0)).setFill(NAVY_HOVER));
        back.setOnMouseExited(e -> ((Rectangle) back.getChildren().get(0)).setFill(NAVY));
        back.setOnMouseClicked(e -> { app.sfx(Sfx.SELECT); onBackToGameModes(); });
        root.getChildren().add(back);

        String notice = app.takeReplayNotice();
        if (!notice.isEmpty()) {
            Ui.Label n = label(notice, 8, LIME, DARK_SHADOW);
            double w = n.textWidth() + 24;
            Rectangle chip = new Rectangle(w, 24, Color.rgb(30, 41, 59, 0.9));
            chip.setArcWidth(24);
            chip.setArcHeight(24);
            chip.setStroke(LIME);
            chip.setStrokeWidth(2);
            chip.setLayoutX(Ui.W - 30 - w);
            chip.setLayoutY(426);
            n.setLayoutX(chip.getLayoutX() + 12);
            n.setLayoutY(434);
            root.getChildren().addAll(chip, n);
        }
        Ui.Label hint = label("LEFT/RIGHT: PICK   UP/DOWN: CHARACTER   ENTER: REPLAY", 8, Color.WHITE, DARK_SHADOW);
        hint.setLayoutX(Ui.W - 30 - hint.textWidth());
        hint.setLayoutY(458);
        root.getChildren().add(hint);

        int first = 0;
        for (int i = 0; i < cards.size(); i++) if (cards.get(i).unlocked) { first = i; break; }
        select(first);
    }

    // ------------------------------------------------------------------ callbacks (hook these into your scene flow)

    /** A replay was chosen. {@code bossId} is {@link BossDef#id()}; {@code selectedCharacter} is "Lena" or "Adam". */
    public void onReplayBossSelected(String bossId, String selectedCharacter) {
        app.startBossReplay(bossId, selectedCharacter);
    }

    /** The back button: returns to the Game Modes menu (not Level Select). */
    public void onBackToGameModes() { app.showModes(); }

    // ------------------------------------------------------------------ interaction

    private void select(int i) {
        index = i;
        for (int j = 0; j < cards.size(); j++) cards.get(j).setSelected(j == i);
    }

    private void activate(int i) {
        Card c = cards.get(i);
        if (!c.unlocked) { app.sfx(Sfx.DENY); return; }
        app.sfx(Sfx.SELECT);
        onReplayBossSelected(c.spec.boss().id(), character.name);
    }

    private void setCharacter(Sibling s) {
        character = s;
        app.setReplayCharacter(s);
        refreshPills();
    }

    private void refreshPills() { for (Pill p : pills) p.setOn(p.who == character); }

    @Override
    public void onDirection(Direction d) {
        switch (d) {
            case LEFT -> select(Math.floorMod(index - 1, cards.size()));
            case RIGHT -> select(Math.floorMod(index + 1, cards.size()));
            case UP, DOWN -> setCharacter(character.other());
            default -> { return; }
        }
        app.sfx(Sfx.CURSOR);
    }

    @Override
    public void onConfirm() { activate(index); }

    @Override
    public void onAction2() {
        app.sfx(Sfx.CURSOR);
        setCharacter(character.other());
    }

    @Override
    public void onCancel() { onBackToGameModes(); }

    /** The title song keeps playing on this screen. */
    @Override
    public boolean titleMusic() { return true; }

    // ------------------------------------------------------------------ pieces

    private static Ui.Label label(String text, double size, Color color, Color shadow) {
        return new Ui.Label(text, size, color).shadow(shadow);
    }

    /** Solid rounded button: a Rectangle (child 0) and centred text. */
    private static Pane button(String text, double w, double h, Color fill, Color textColor) {
        Pane p = new Pane();
        Rectangle r = new Rectangle(w, h, fill);
        r.setArcWidth(h);
        r.setArcHeight(h);
        Ui.Label l = label(text, 9, textColor, DARK_SHADOW);
        l.setLayoutX((w - l.textWidth()) / 2);
        l.setLayoutY((h - 9) / 2);
        p.getChildren().addAll(r, l);
        return p;
    }

    /** Padlock drawn from shapes, centred on (cx, cy). */
    private static Group padlock(double cx, double cy) {
        Color metal = Color.web("#CBD5E1"), dark = Color.web("#0F172A");
        Arc shackle = new Arc(cx, cy - 4, 9, 11, 0, 180);
        shackle.setType(ArcType.OPEN);
        shackle.setFill(null);
        shackle.setStroke(metal);
        shackle.setStrokeWidth(4);
        Rectangle body = new Rectangle(cx - 13, cy - 4, 26, 20);
        body.setArcWidth(6);
        body.setArcHeight(6);
        body.setFill(metal);
        body.setStroke(dark);
        body.setStrokeWidth(2);
        Circle hole = new Circle(cx, cy + 4, 3, dark);
        Rectangle slot = new Rectangle(cx - 1.5, cy + 5, 3, 7);
        slot.setFill(dark);
        return new Group(shackle, body, hole, slot);
    }

    /** One of the two segments of the character pill bar. Lime when active, transparent otherwise. */
    private final class Pill extends Pane {
        final Sibling who;
        private final Rectangle bg;
        private final Ui.Label text;

        Pill(Sibling who, double w, double h) {
            this.who = who;
            bg = new Rectangle(w, h);
            bg.setArcWidth(h);
            bg.setArcHeight(h);
            text = label(who.name.toUpperCase(), 10, NAVY, SOFT_SHADOW);
            text.setLayoutX((w - text.textWidth()) / 2);
            text.setLayoutY((h - 10) / 2);
            getChildren().addAll(bg, text);
            setCursor(Cursor.HAND);
            setOnMouseClicked(e -> { app.sfx(Sfx.CURSOR); setCharacter(who); });
        }

        void setOn(boolean on) {
            bg.setFill(on ? LIME : Color.TRANSPARENT);
            text.setEffect(null);
            text.setFill(NAVY);
        }
    }

    /** One boss card, in its unlocked or locked state. */
    private final class Card extends Pane {
        final CardSpec spec;
        final boolean unlocked;
        private final Rectangle face;
        private final Rectangle action;
        private final ScaleTransition zoom = new ScaleTransition(Duration.millis(130), this);

        Card(CardSpec spec, boolean unlocked, double best) {
            this.spec = spec;
            this.unlocked = unlocked;
            setPrefSize(CARD_W, CARD_H);
            setMinSize(CARD_W, CARD_H);
            setMaxSize(CARD_W, CARD_H);

            face = new Rectangle(CARD_W, CARD_H, CARD);
            face.setArcWidth(ARC);
            face.setArcHeight(ARC);
            face.setStroke(NAVY);
            face.setStrokeWidth(3);
            getChildren().add(face);

            // portrait (the boss sprite): full colour when unlocked, a dark silhouette with a padlock when locked
            ImageView portrait = Sprites.view(spec.boss().normal());
            portrait.setPreserveRatio(true);
            portrait.setFitWidth(150);
            portrait.setFitHeight(100);
            portrait.setLayoutX((CARD_W - portrait.getLayoutBounds().getWidth()) / 2);
            portrait.setLayoutY(32);
            if (!unlocked) {
                ColorAdjust shade = new ColorAdjust();
                shade.setBrightness(-1.0);
                portrait.setEffect(shade);
                portrait.setOpacity(0.8);
            }
            getChildren().add(portrait);
            if (!unlocked) getChildren().add(padlock(CARD_W / 2, 84));

            Ui.Label name = label(spec.boss().label(), 11, unlocked ? NAME : MUTED, DARK_SHADOW);
            name.setLayoutX((CARD_W - name.textWidth()) / 2);
            name.setLayoutY(142);
            getChildren().add(name);

            if (unlocked) {
                Ui.Label attacks = label(spec.attacks(), 8, ATTACKS, DARK_SHADOW);
                attacks.setWrap(CARD_W - 24);
                attacks.setTextAlignment(TextAlignment.CENTER);
                attacks.setLayoutX(12);
                attacks.setLayoutY(166);
                Ui.Label rec = label(spec.recordLabel() + ": " + (best > 0 ? time(best) : "--:--"), 8, LIME, DARK_SHADOW);
                rec.setLayoutX((CARD_W - rec.textWidth()) / 2);
                rec.setLayoutY(230);
                action = new Rectangle(170, 30, LIME);
                action.setArcWidth(30);
                action.setArcHeight(30);
                action.setLayoutX(20);
                action.setLayoutY(CARD_H - 42);
                Ui.Label go = label(spec.button(), 9, NAVY, Color.rgb(255, 255, 255, 0.35));
                go.setLayoutX((CARD_W - go.textWidth()) / 2);
                go.setLayoutY(CARD_H - 42 + 11);
                getChildren().addAll(attacks, rec, action, go);
                setCursor(Cursor.HAND);
            } else {
                action = null;
                Ui.Label lock = label("Defeat in Story Mode to Unlock", 8, MUTED, DARK_SHADOW);
                lock.setWrap(CARD_W - 40);
                lock.setTextAlignment(TextAlignment.CENTER);
                lock.setLayoutX(20);
                lock.setLayoutY(170);
                Rectangle btn = new Rectangle(170, 30, LOCK_BTN);
                btn.setArcWidth(30);
                btn.setArcHeight(30);
                btn.setLayoutX(20);
                btn.setLayoutY(CARD_H - 42);
                Ui.Label locked = label("LOCKED", 9, Color.web("#CBD5E1"), DARK_SHADOW);
                locked.setLayoutX((CARD_W - locked.textWidth()) / 2);
                locked.setLayoutY(CARD_H - 42 + 11);
                getChildren().addAll(lock, btn, locked);
            }

            // mode badge: a pill centred along the top border
            Ui.Label badgeText = label(spec.badge(), 8, Color.WHITE, DARK_SHADOW);
            double bw = badgeText.textWidth() + 22;
            Rectangle badge = new Rectangle(bw, 20, spec.badgeColor());
            badge.setArcWidth(20);
            badge.setArcHeight(20);
            badge.setStroke(NAVY);
            badge.setStrokeWidth(2);
            badge.setLayoutX((CARD_W - bw) / 2);
            badge.setLayoutY(-10);
            badgeText.setLayoutX(badge.getLayoutX() + 11);
            badgeText.setLayoutY(-5);
            if (!unlocked) { badge.setOpacity(0.55); badgeText.setOpacity(0.7); }
            getChildren().addAll(badge, badgeText);
        }

        /** Highlights the card; an unlocked one also scales up. */
        void setSelected(boolean on) {
            face.setStroke(on ? LIME : NAVY);
            if (action != null) action.setFill(on ? LIME_HOT : LIME);
            double target = on && unlocked ? SELECTED_SCALE : 1.0;
            zoom.stop();
            zoom.setToX(target);
            zoom.setToY(target);
            zoom.playFromStart();
        }
    }
}
