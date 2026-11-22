package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public final class GroveCutsceneScreen extends Screen {
    private static final double FEET = 318;
    private static final double SCALE = 2;
    private static final double END_FEET = 335;
    private static final double START_OFFSET = 160;
    private static final double WALK_SPEED = 90;
    private static final double MN_X = 360, MN_FEET = 262, MN_SCALE = 2.2;
    private static final double FALL_H = 288;

    private static final List<String> PORTAL_PAGES = List.of(
        "The last street of Mega City sparkles clean. Then the air over the old fountain begins to hum, " +
        "and a ring of green light spins wide open. It is a portal!",

        "Sprout: News of your good deeds has travelled to every town, river and rooftop, Guardian. " +
        "Someone very beautiful and very kind wants to meet you. Step through!");

    private static final List<String> GROVE_PAGES = List.of(
        "There is no city on the other side. A hidden grove opens around you: waterfalls pour down mossy cliffs, " +
        "tall trees lean together into arches, and a long hall of living moss leads to a throne woven from roots and flowers.",

        "Mother Nature rises from her throne. She is young and radiant, so beautiful that the flowers turn to follow her, " +
        "with platinum hair that flows all the way down past her knees.",

        "Mother Nature: Welcome, Guardian! I felt every can you sorted, every sapling you planted and every river you cleared. " +
        "The people and the whole community are so thankful for you, and so am I.",

        "Mother Nature: For so many good deeds, I name you a trusted Guardian of the Green City.",

        "Mother Nature: Queen Muck's power has grown wild, and your sibling is locked inside her palace. " +
        "But the palace hides in the smog now, and no road leads there anymore.",

        "Mother Nature: I will open a portal to her gate. Go, Guardian, and bring your sibling home!");

    private enum Phase { PORTAL, FLASH_IN, WALK, TALK }

    private final Runnable next;
    private final DialogBox box;
    private final Random rng = new Random();

    private final Group portalWorld = new Group();
    private final Group groveWorld = new Group();
    private final StoryFx.Particles portalFx = new StoryFx.Particles(Ui.W, Ui.H);
    private final StoryFx.Particles groveFx = new StoryFx.Particles(Ui.W, Ui.H);
    private final Rectangle flash = new Rectangle(Ui.W, Ui.H, Color.WHITE);

    private final Group portal = new Group();
    private final PortalView portalSprite = new PortalView();
    private final ImageView portalPlayer;
    private final ImageView growPlayer;
    private final ImageView mother;
    private final List<Rectangle> streaks = new ArrayList<>();
    private final List<Double> streakSpeeds = new ArrayList<>();

    private Phase phase = Phase.PORTAL;
    private double t;
    private double flashT;
    private boolean swapped;
    private double walkT;
    private double walkOffset = START_OFFSET;

    public GroveCutsceneScreen(EcoQuestApp app, Runnable next) {
        super(app);
        this.next = next;

        portalWorld.getChildren().add(StoryFx.scene("city"));
        portalWorld.getChildren().add(StoryFx.vignette(Color.web("#1e0f30", 0.35)));
        portal.getChildren().add(portalSprite);
        portal.setTranslateX(540);
        portal.setTranslateY(215);
        portalPlayer = Sprites.view(app.save().sibling().sheet(false));
        StoryFx.stand(portalPlayer, 190, FEET, SCALE);
        portalFx.driftIn(14, () -> StoryFx.sparkle(3, Color.web("#c8fff0", 0.9)), 440, 100, 640, 330, -26, 20);
        portalWorld.getChildren().addAll(portal, StoryFx.shadow(190, FEET, 90), portalPlayer, portalFx);

        groveWorld.getChildren().add(Sprites.view("backdrop/grove.png"));

        var mistL = StoryFx.glow(70, Color.web("#ffffff", 0.4));
        mistL.setTranslateX(90);
        mistL.setTranslateY(296);
        var mistR = StoryFx.glow(70, Color.web("#ffffff", 0.4));
        mistR.setTranslateX(630);
        mistR.setTranslateY(296);
        groveWorld.getChildren().addAll(mistL, mistR);
        addFall(54, 72);
        addFall(594, 72);

        for (double gx : new double[] {222, 498}) {
            ImageView guard = Sprites.view("npc/grove_guard.png");
            StoryFx.stand(guard, gx, 338, 1.7);
            groveWorld.getChildren().addAll(StoryFx.shadow(gx, 338, 70), guard);
        }

        var halo = StoryFx.glow(120, Color.web("#fff6c0", 0.5));
        halo.setTranslateX(MN_X);
        halo.setTranslateY(MN_FEET - 80);
        mother = Sprites.view("npc/mother_nature.png");
        StoryFx.stand(mother, MN_X, MN_FEET, MN_SCALE);
        growPlayer = Sprites.view(app.save().sibling().sheet(false));
        StoryFx.stand(growPlayer, MN_X, END_FEET, SCALE);
        growPlayer.setViewport(new Rectangle2D(0, 3 * 72, Sprites.PX, 72));
        growPlayer.setTranslateY(START_OFFSET);
        groveWorld.getChildren().addAll(halo, StoryFx.shadow(MN_X, MN_FEET, 100), mother, growPlayer);

        groveFx.drift(18, () -> StoryFx.sparkle(2.5, Color.web("#fff6a0", 0.85)), 0, -7, 24);
        groveFx.drift(9, () -> {
            Polygon leaf = new Polygon(0, -4, 3, 0, 0, 4, -3, 0);
            leaf.setFill(Color.web("#7cc79a", 0.9));
            return leaf;
        }, 6, 20, 26);
        groveWorld.getChildren().add(groveFx);
        groveWorld.setVisible(false);

        flash.setOpacity(0);
        flash.setMouseTransparent(true);
        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().addAll(portalWorld, groveWorld, flash, box);
    }

    private void addFall(double x, double w) {
        Group g = new Group();
        g.setClip(new Rectangle(x, 0, w, FALL_H));
        for (int i = 0; i < 10; i++) {
            Rectangle r = new Rectangle(x + 4 + rng.nextDouble() * (w - 10), rng.nextDouble() * FALL_H, 2.5, 18 + rng.nextDouble() * 20);
            r.setFill(Color.web("#ffffff", 0.55));
            g.getChildren().add(r);
            streaks.add(r);
            streakSpeeds.add(150 + rng.nextDouble() * 90);
        }
        groveWorld.getChildren().add(g);
    }

    @Override
    public void enter() {
        t = 0;
        box.show(PORTAL_PAGES);
        app.sfx(Sfx.MISSION);
        portalFx.converge(12, () -> StoryFx.sparkle(3, Color.web("#9fe0cf")), 540, 215, 1.1, 60);
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        portalFx.update(tpf);
        portalSprite.update(t);

        if (phase == Phase.FLASH_IN) {
            flashT += tpf;
            flash.setOpacity(Math.min(1, flashT / 0.6));
            if (flashT >= 0.6) {
                portalWorld.setVisible(false);
                groveWorld.setVisible(true);
                swapped = true;
                flashT = 0;
                phase = Phase.WALK;
                app.sfx(Sfx.MISSION);
            }
        } else if (swapped && flash.getOpacity() > 0) {
            flashT += tpf;
            flash.setOpacity(Math.max(0, 1 - flashT / 0.9));
        }

        if (!swapped) return;
        groveFx.update(tpf);
        for (int i = 0; i < streaks.size(); i++) {
            Rectangle r = streaks.get(i);
            r.setY(r.getY() + streakSpeeds.get(i) * tpf);
            if (r.getY() > FALL_H) r.setY(-40);
        }
        mother.setTranslateY(Math.round(Math.sin(t * 2) * 3));

        if (phase == Phase.WALK) {
            walkT += tpf;
            walkOffset = Math.max(0, walkOffset - WALK_SPEED * tpf);
            growPlayer.setTranslateY(Math.round(walkOffset));
            int step = (int) (walkT / 0.16) % 4;
            growPlayer.setViewport(new Rectangle2D(step * Sprites.PX, 3 * 72, Sprites.PX, 72));
            if (walkOffset <= 0) arrive();
        }
    }

    private void arrive() {
        walkOffset = 0;
        growPlayer.setTranslateY(0);
        growPlayer.setViewport(new Rectangle2D(0, 3 * 72, Sprites.PX, 72));
        phase = Phase.TALK;
        box.show(GROVE_PAGES);
        groveFx.burst(MN_X, MN_FEET - 110, 10, () -> StoryFx.heart(5, Color.web("#ff8fb8")), 90, 1.4, -40);
    }

    @Override
    public void onConfirm() {
        switch (phase) {
            case PORTAL -> {
                app.sfx(Sfx.SELECT);
                if (box.advance()) {
                    phase = Phase.FLASH_IN;
                    flashT = 0;
                    app.sfx(Sfx.BOOST);
                }
            }
            case WALK -> walkOffset = 0;
            case TALK -> {
                app.sfx(Sfx.SELECT);
                if (box.advance()) next.run();
            }
            default -> { }
        }
    }

    @Override
    public void onCancel() {
        next.run();
    }
}
