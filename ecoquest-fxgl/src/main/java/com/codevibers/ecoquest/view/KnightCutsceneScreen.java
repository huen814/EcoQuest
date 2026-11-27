package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

/**
 * Story before boss 2, after level 6: the Queen's Knight steps out of a purple portal in the park and takes the Guardian
 * prisoner, the bars wither and a green portal carries the Guardian to a grassfield, the knight finds the empty cell, follows
 * through his own portal into the streets and drops the trash maze from a violet sky. {@code next} then starts the chase.
 */
public final class KnightCutsceneScreen extends Screen {
    private static final double FEET = 318;                    // feet stand on the footpath
    private static final double SCALE = 1;                      // characters are drawn at their in-game size so they fit the scenery
    private static final double PORTAL_SCALE = 0.7;
    private static final String KNIGHT_MUSIC = "knights_dialogue.wav";       // his dialogue theme: from the portal until the chase begins
    private static final String NATURE_MUSIC = "title.wav";                   // Mother Nature's theme, the same one the grove scene plays
    private static final double PURPLE_HUE = 0.63;
    private static final Color PURPLE = Color.web("#b070ff", 0.85);
    private static final double KNIGHT_SIZE = 100, KNIGHT_FEET_PAD = 98;
    private static final double CELL_KNIGHT_FEET = 342;          // in the corridor, in front of the bars
    private static final double CELL_PORTAL_X = 360, CELL_PORTAL_Y = FEET - 55;
    private static final Color BAR = Color.web("#8a86a8"), SHINE = Color.web("#b5b1d4");
    private static final Color LEAF_DARK = Color.web("#4fbf6e"), LEAF_LIGHT = Color.web("#a8f0b8");

    private enum Beat { PARK_PORTAL, CELL, FIELD, CELL_EMPTY, STREET }

    private static final List<List<String>> PAGES = List.of(
        List.of(
            "You stroll through the park, humming, looking for the next area to clean. The birds are singing, families are picnicking, " +
            "and for once nothing smells like garbage.",

            "Then the air above the path crackles and tears open. A ring of violet light spins wide, and a knight in dark armor strides " +
            "out of a purple portal. The whole park screams and scatters!",

            "Knight: Guardian. I am the Queen's knight, sent by Queen Muck. Your work has been causing Her Majesty great discomfort. It ends today.",

            "Knight: You will come with me. Resistance would be unwise.",

            "Before you can run, violet light swallows the park, and the ground drops out from under your feet."),
        List.of(
            "You wake up in a cold stone cell, lit by one flickering purple lamp. Iron bars stand between you and a long, dark hall.",

            "Knight: Remain here. Her Majesty will decide your fate. I will return within the hour.",

            "An hour crawls by. You rattle the bars, shove the lock and search every crack, but there is no way out. " +
            "Then something warm brushes your mind.",

            "???: Do not be afraid, little Guardian. I shall lend you my assistance.",

            "A soft green light seeps through the stone. The lock clicks, the bars wither into leaves, and a swirling green portal opens " +
            "in the middle of the cell. You do not hesitate. You step through!"),
        List.of(
            "An hour after the knight locked you away, a kind and unseen hand has carried you out. You land softly in a wide grassfield " +
            "full of flowers, with butterflies drifting all around you.",

            "You spot a bus stop down the road. That knight will be back soon, so you hurry to catch the next bus into town."),
        List.of(
            "Meanwhile, back in the dungeon, the knight returns to check on his prisoner.",

            "Knight: ...The cell is empty. The lock is covered in flowers. This was no ordinary escape.",

            "Knight: Queen Muck will nag me for days over this. No matter. The Guardian will be found."),
        List.of(
            "You found the nearest bus stop and rode all the way back to town. Now you hurry down the street toward the next area, " +
            "when the air behind you tears open again, and the knight's own portal spits him out right in your way!",

            "Knight: Found you. A few flowers will not save you from the Queen's will.",

            "Knight: Rise, maze of rubbish. Close every path.",

            "Junk walls rise on every side! Run for the end of the maze! Move with the arrow keys or WASD. " +
            "You cannot fight him, so use the junk cubes to throw him off your trail, stay out of his sludge, it slows you down, " +
            "and dodge the sad ghosts and his sludge bombs!"));

    private static final class Villager {
        final ImageView node;
        final Node shadow;
        final Ui.Label bang;
        final double homeX;
        double dx, speed;
        Villager(ImageView node, Node shadow, Ui.Label bang, double homeX) { this.node = node; this.shadow = shadow; this.bang = bang; this.homeX = homeX; }
    }

    private static final class Cube {
        final ImageView node;
        final double targetY;
        double y = -60, vy, delay;
        boolean landed;
        Cube(ImageView node, double targetY) { this.node = node; this.targetY = targetY; }
    }

    private static final class Butterfly {
        final Group node;
        final double baseX, baseY, ax, ay, w1, w2, phase;
        Butterfly(Group node, double baseX, double baseY, double ax, double ay, double w1, double w2, double phase) {
            this.node = node; this.baseX = baseX; this.baseY = baseY; this.ax = ax; this.ay = ay; this.w1 = w1; this.w2 = w2; this.phase = phase;
        }
    }

    /** A park-like street scene with a portal, the knight, the Guardian, a violet sky and the falling maze. */
    private final class Stage {
        final Group world = new Group();
        final Group mid = new Group();
        final StoryFx.Particles fx = new StoryFx.Particles(Ui.W, Ui.H);
        final PortalView portal = new PortalView(PURPLE, PURPLE_HUE);
        final Group portalHolder;
        final ImageView knight = Sprites.view("boss/knight_story.png");
        final Node knightShadow;
        final ImageView player;
        final Group cubeLayer = new Group();
        final List<Cube> cubes = new ArrayList<>();
        final Group violet = new Group();
        final Rectangle storm = skyGradient(Ui.W, 330, Color.web("#3a0f66", 0.75));
        final Rectangle skyFlash = skyGradient(Ui.W, 330, Color.web("#e6d0ff", 0.8));
        final double playerX, knightX;
        boolean portalOn;
        double portalT, violetTarget, violetCur, stormTarget, stormCur, shakeT, flashLeft;
        int landed;

        // The street's build-up to the chase: rain, mist, lightning, and every cube that lands cracks the ground and scatters litter.
        boolean stormy;
        final Group ground = new Group();
        final Group crackGlow = new Group();
        final Group rain = new Group();
        final double[] dropV = new double[70];
        final Group mist = new Group();
        final double[] mistBase = new double[6];
        final Group bolt = new Group();
        final Rectangle dimRect = new Rectangle(Ui.W, Ui.H, Color.web("#1a0830", 0.3));
        double strikeT = -1, rumbleT = -1, strikeCd = 1.2;

        Stage(Node scene, Node decor, double playerX, double knightX, double portalX, double portalY) {
            this.playerX = playerX;
            this.knightX = knightX;
            Rectangle sky = skyGradient(Ui.W, 320, Color.web("#9b4dff", 0.6));
            Circle skyGlow = StoryFx.glow(230, Color.web("#b070ff", 0.45));
            skyGlow.setTranslateX(portalX);
            skyGlow.setTranslateY(90);
            Circle poolA = StoryFx.glow(170, Color.web("#a35cff", 0.55));
            poolA.setTranslateX(portalX);
            poolA.setTranslateY(FEET - 6);
            poolA.setScaleY(0.28);
            Circle poolB = StoryFx.glow(140, Color.web("#a35cff", 0.4));
            poolB.setTranslateX(playerX);
            poolB.setTranslateY(FEET - 2);
            poolB.setScaleY(0.28);
            violet.getChildren().addAll(sky, skyGlow, storm, poolA, poolB);
            violet.setOpacity(0);
            storm.setOpacity(0);
            skyFlash.setOpacity(0);

            portalHolder = holder(portal, portalX, portalY);
            portal.setVisible(false);
            placeKnight(knight, knightX, FEET);
            knight.setOpacity(0);
            knightShadow = StoryFx.shadow(knightX, FEET, 56);
            knightShadow.setOpacity(0);
            player = Sprites.view(app.save().sibling().sheet(false));
            StoryFx.stand(player, playerX, FEET, SCALE);
            for (int i = 0; i < dropV.length; i++) {
                double len = 14 + rng.nextDouble() * 12;
                Line l = new Line(0, 0, -0.28 * len, len);
                l.setStroke(Color.web("#d4c8ff", 0.3 + rng.nextDouble() * 0.2));
                l.setStrokeWidth(1.2);
                l.setTranslateX(rng.nextDouble() * (Ui.W + 160));
                l.setTranslateY(rng.nextDouble() * Ui.H);
                dropV[i] = 620 + rng.nextDouble() * 320;
                rain.getChildren().add(l);
            }
            for (int i = 0; i < mistBase.length; i++) {
                Circle m = StoryFx.glow(120 + rng.nextDouble() * 60, Color.web("#cdb8ff", 0.17));
                mistBase[i] = 60 + i * (Ui.W - 120) / (mistBase.length - 1.0);
                m.setTranslateY(300 + rng.nextDouble() * 150);
                m.setScaleY(0.28);
                mist.getChildren().add(m);
            }
            dimRect.setOpacity(0); rain.setOpacity(0); mist.setOpacity(0); bolt.setVisible(false);
            for (Node n : new Node[] {dimRect, rain, mist, bolt, ground, crackGlow}) n.setMouseTransparent(true);
            world.getChildren().addAll(scene, decor, mid, violet, dimRect, mist, ground, crackGlow, portalHolder, StoryFx.shadow(playerX, FEET, 36), knightShadow, knight, player, cubeLayer, bolt, rain, fx, skyFlash);
            world.setVisible(false);
        }

        void reset() {
            portalOn = false;
            portalT = 0;
            portal.setVisible(false);
            knight.setOpacity(0);
            knight.setTranslateX(0);
            knightShadow.setOpacity(0);
            violetTarget = 0;
            violetCur = 0;
            stormTarget = 0;
            stormCur = 0;
            violet.setOpacity(0);
            storm.setOpacity(0);
            skyFlash.setOpacity(0);
            flashLeft = 0;
            shakeT = 0;
            world.setTranslateX(0);
            world.setTranslateY(0);
            cubeLayer.getChildren().clear();
            cubes.clear();
            landed = 0;
            ground.getChildren().clear();
            crackGlow.getChildren().clear();
            bolt.setVisible(false);
            dimRect.setOpacity(0); rain.setOpacity(0); mist.setOpacity(0);
            strikeT = -1; rumbleT = -1; strikeCd = 1.2;
        }

        void openPortal(double violetLevel) {
            portal.setVisible(true);
            portalOn = true;
            portalT = 0;
            violetTarget = violetLevel;
            app.sfx(Sfx.MISSION);
        }

        void dropMaze() {
            stormTarget = 1;
            violetTarget = 1;
            for (int i = 0; i < 18; i++) {
                double x;
                do { x = rng.nextDouble() * (Ui.W - 48); } while (Math.abs(x + 24 - playerX) < 90 || Math.abs(x + 24 - knightX) < 100);
                ImageView iv = Sprites.view("boss/junk_cube" + (i % 3) + ".png");
                iv.setFitWidth(48);
                iv.setFitHeight(48);
                iv.setLayoutX(x);
                iv.setTranslateY(-60);
                Cube c = new Cube(iv, 130 + rng.nextDouble() * 170);
                c.delay = i * 0.12 + rng.nextDouble() * 0.1;
                cubes.add(c);
            }
            cubes.sort(Comparator.comparingDouble(c -> c.targetY));
            for (Cube c : cubes) cubeLayer.getChildren().add(c.node);
        }

        void update(double tpf) {
            fx.update(tpf);
            if (portalOn) {
                portalT += tpf;
                portal.update(portalT);
            }
            double op = knight.getOpacity();
            if (portalOn && portalT > 0.8 && op < 1) {
                op = Math.min(1, op + tpf / 0.6);
                knight.setOpacity(op);
                knightShadow.setOpacity(op);
            }
            knight.setTranslateX(Math.round((1 - op) * 40));
            knight.setTranslateY(Math.round(Math.sin(t * 2.2) * 3));

            violetCur = approach(violetCur, violetTarget, tpf * 1.2);
            stormCur = approach(stormCur, stormTarget, tpf * 0.9);
            violet.setOpacity(violetCur * (0.85 + 0.15 * Math.sin(t * 3)));
            storm.setOpacity(stormCur);
            if (flashLeft > 0) {
                flashLeft -= tpf;
                skyFlash.setOpacity(Math.max(0, flashLeft / 0.15) * 0.7);
            }

            for (Cube c : cubes) {
                if (c.landed) continue;
                c.delay -= tpf;
                if (c.delay > 0) continue;
                c.vy += 1400 * tpf;
                c.y += c.vy * tpf;
                if (c.y >= c.targetY) {
                    c.y = c.targetY;
                    c.landed = true;
                    shakeT = 0.25;
                    fx.burst(c.node.getLayoutX() + 24, c.targetY + 48, 4, () -> StoryFx.dot(3, Color.web("#8a7a6a", 0.8)), 60, 0.6, 40);
                    landed++;
                    if (stormy) landFx(c.node.getLayoutX() + 24, c.targetY + 48);
                    if (landed % 4 == 1) {
                        app.sfx(Sfx.DUMP);
                        flashLeft = 0.15;
                    }
                }
                c.node.setTranslateY(c.y);
            }
            if (shakeT > 0) {
                shakeT -= tpf;
                world.setTranslateX(shakeT > 0 ? (rng.nextDouble() - 0.5) * 8 : 0);
                world.setTranslateY(shakeT > 0 ? (rng.nextDouble() - 0.5) * 8 : 0);
            }
            if (stormy) updateStorm(tpf);
        }

        private final String[] litterNames = {"aluminum_can", "banana_peel", "bottle_cap", "cardboard_box", "food_container", "glass_bottle", "newspaper", "plastic_bag", "plastic_bag", "plastic_bottle", "tin_can"};

        /** Where a cube lands the street splits, and a little trash is thrown out of the crack. */
        void landFx(double x, double y) {
            int arms = 3 + rng.nextInt(2);
            for (int k = 0; k < arms; k++) {
                double a = rng.nextDouble() * Math.PI * 2, cx = x, cy = y;
                Polyline dark = new Polyline(), lit = new Polyline();
                int segs = 3 + rng.nextInt(3);
                for (int i = 0; i <= segs; i++) {
                    dark.getPoints().addAll(cx, cy);
                    lit.getPoints().addAll(cx, cy);
                    a += (rng.nextDouble() - 0.5) * 0.7;
                    double len = 12 + rng.nextDouble() * 16;
                    cx = Math.round((cx + Math.cos(a) * len) / 3) * 3;
                    cy = Math.round((cy + Math.sin(a) * len * 0.55) / 3) * 3;
                }
                dark.setFill(null); lit.setFill(null);
                dark.setStroke(Color.web("#0b0514", 0.9)); dark.setStrokeWidth(3);
                lit.setStroke(Color.web("#b070ff", 0.7)); lit.setStrokeWidth(1); lit.setBlendMode(BlendMode.ADD);
                ground.getChildren().add(dark);
                crackGlow.getChildren().add(lit);
            }
            for (int k = 0; k < 2; k++) {
                double size = 18 + rng.nextDouble() * 14;
                ImageView v = StoryFx.item(litterNames[rng.nextInt(litterNames.length)], size);
                v.setLayoutX(x + (rng.nextDouble() - 0.5) * 90 - size / 2);
                v.setLayoutY(y + (rng.nextDouble() - 0.3) * 30 - size / 2);
                v.setRotate(rng.nextDouble() * 360);
                ground.getChildren().add(v);
            }
        }

        void updateStorm(double tpf) {
            double build = Math.min(1, 0.2 * violetCur + stormCur);          // a hint of rain from the portal on, the full storm as the maze falls
            dimRect.setOpacity(stormCur);
            mist.setOpacity(stormCur);
            rain.setOpacity(build);
            crackGlow.setOpacity(0.5 + 0.25 * Math.sin(t * 2.2));
            for (int i = 0; i < mist.getChildren().size(); i++) {
                mist.getChildren().get(i).setTranslateX(mistBase[i] + Math.sin(t * 0.25 + i * 1.7) * 50);
            }
            if (build > 0.03) {
                for (int i = 0; i < dropV.length; i++) {
                    Node l = rain.getChildren().get(i);
                    double nx = l.getTranslateX() - dropV[i] * 0.28 * tpf, ny = l.getTranslateY() + dropV[i] * tpf;
                    if (ny > Ui.H + 20 || nx < -30) { nx = rng.nextDouble() * (Ui.W + 160); ny = -30 - rng.nextDouble() * 40; }
                    l.setTranslateX(nx); l.setTranslateY(ny);
                }
            }
            if (stormCur > 0.5 && strikeT < 0) {
                strikeCd -= tpf;
                if (strikeCd <= 0) { strike(); strikeCd = 2.2 + rng.nextDouble() * 2.5; }
            }
            if (rumbleT >= 0) {
                rumbleT -= tpf;
                if (rumbleT < 0) app.sfx(Sfx.THUNDER);
            }
            if (strikeT >= 0) {
                strikeT += tpf;
                double f = strikeT < 0.07 ? 1 : strikeT < 0.15 ? 0.2 : strikeT < 0.24 ? 0.85 : Math.max(0, 0.85 * (1 - (strikeT - 0.24) / 0.5));
                skyFlash.setOpacity(0.8 * f);
                bolt.setOpacity(strikeT < 0.35 ? (strikeT >= 0.07 && strikeT < 0.15 ? 0.3 : 1) : Math.max(0, 1 - (strikeT - 0.35) / 0.2));
                crackGlow.setOpacity(Math.min(1, crackGlow.getOpacity() + 0.5 * f));
                if (strikeT > 0.75) { strikeT = -1; bolt.setVisible(false); skyFlash.setOpacity(0); }
            }
        }

        void strike() {
            double x = 60 + rng.nextDouble() * (Ui.W - 120);
            double yEnd = 210 + rng.nextDouble() * 120;
            List<Double> pts = new ArrayList<>();
            double cx = x;
            int segs = 8;
            for (int i = 0; i <= segs; i++) {
                pts.add(cx);
                pts.add(-10 + (yEnd + 10) * i / segs);
                cx += (rng.nextDouble() - 0.5) * 70;
            }
            Polyline core = new Polyline(), halo = new Polyline();
            core.getPoints().addAll(pts);
            halo.getPoints().addAll(pts);
            core.setFill(null); halo.setFill(null);
            halo.setStroke(Color.web("#b080ff", 0.5)); halo.setStrokeWidth(10); halo.setBlendMode(BlendMode.ADD);
            core.setStroke(Color.web("#fbf8ff")); core.setStrokeWidth(3);
            bolt.getChildren().setAll(halo, core);
            bolt.setOpacity(1);
            bolt.setVisible(true);
            strikeT = 0;
            rumbleT = 0.3 + rng.nextDouble() * 0.5;
        }
    }

    private final Runnable next;
    private final DialogBox box;
    private final Random rng = new Random();
    private final Rectangle flash = new Rectangle(Ui.W, Ui.H, Color.WHITE);

    private final Stage park;
    private final Stage street;
    private final List<Villager> villagers = new ArrayList<>();

    private final Group cellWorld = new Group();
    private final StoryFx.Particles cellFx = new StoryFx.Particles(Ui.W, Ui.H);
    private ImageView cellPlayer;
    private final ImageView cellKnight = Sprites.view("boss/knight_story.png");
    private final Node cellKnightShadow = StoryFx.shadow(520, CELL_KNIGHT_FEET, 56);
    private final Group bars = new Group();
    private final List<Rectangle> barRects = new ArrayList<>();
    private final List<Rectangle> shineRects = new ArrayList<>();
    private final List<Rectangle> crossRects = new ArrayList<>();
    private final PortalView cellPortal = new PortalView();
    private final Circle lampGlow = StoryFx.glow(170, Color.web("#b070ff", 0.55));
    private final Circle greenGlow = StoryFx.glow(150, Color.web("#9fffc0", 0.8));
    private final Rectangle greenTint = new Rectangle(Ui.W, Ui.H, Color.web("#8affb0"));
    private final Ui.Label shock = new Ui.Label("...", 28, Color.web("#c8c0d8"));

    private final Group fieldWorld = new Group();
    private final StoryFx.Particles fieldFx = new StoryFx.Particles(Ui.W, Ui.H);
    private final PortalView fieldPortal = new PortalView();
    private final List<Butterfly> butterflies = new ArrayList<>();
    private boolean fieldDrift;

    private Beat beat = Beat.PARK_PORTAL;
    private double t, beatT;
    private int lastPage = -1;

    private int trans;
    private double transT;
    private Beat transTo;

    private boolean panic;
    private double panicT;

    private boolean cellKnightIn, cellKnightLeaving, greenOn, melting, leavesStarted, cellPortalOn, suckDone;
    private double cellKnightAlpha, cellKnightSlide, greenT, meltT, cellPortalT, suckT;

    public KnightCutsceneScreen(EcoQuestApp app, Runnable next) {
        super(app);
        this.next = next;

        park = new Stage(Sprites.view("backdrop/park_plaza.png"), new Group(), 190, 500, 540, FEET - 62);
        park.fx.driftIn(12, () -> StoryFx.dot(1.8, Color.web("#bff3ff", 0.9)), 335, 195, 385, 250, 45, 8);

        double[] xs = {125, 270, 330, 385, 585};
        for (int i = 0; i < xs.length; i++) {
            double feet = 304 + (i % 3) * 11;
            ImageView v = Sprites.view("npc/park_" + i + ".png");
            StoryFx.stand(v, xs[i], feet, 1);
            Node shadow = StoryFx.shadow(xs[i], feet, 30);
            Ui.Label bang = new Ui.Label("!", 20, Color.web("#ff4d6d"));
            bang.setLayoutX(xs[i] - 6);
            bang.setLayoutY(feet - 72 - 26);
            bang.setVisible(false);
            villagers.add(new Villager(v, shadow, bang, xs[i]));
            park.mid.getChildren().addAll(shadow, v, bang);
        }
        for (String side : new String[] {"left", "right"}) {                             // the two big front trees, redrawn over the villagers so they run behind them
            ImageView tree = Sprites.view("backdrop/park_front_" + side + ".png");
            tree.setLayoutX(side.equals("left") ? 0 : 626);
            tree.setLayoutY(side.equals("left") ? 204 : 208);
            park.mid.getChildren().add(tree);
        }

        street = new Stage(StoryFx.scene("city"), busStop(60, 290), 260, 500, 560, FEET - 62);
        street.stormy = true;                                       // the street gets the chase scene's storm

        buildCell();
        buildField();

        flash.setOpacity(0);
        flash.setMouseTransparent(true);
        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().addAll(park.world, cellWorld, fieldWorld, street.world, flash, box);
    }

    // ------------------------------------------------------------------ scenery builders

    private static Rectangle rect(double x, double y, double w, double h, Color fill) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setFill(fill);
        return r;
    }

    private static Rectangle skyGradient(double w, double h, Color top) {
        Rectangle r = new Rectangle(w, h);
        r.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, Color.color(top.getRed(), top.getGreen(), top.getBlue(), 0))));
        r.setMouseTransparent(true);
        return r;
    }

    private static double approach(double cur, double target, double step) {
        return cur < target ? Math.min(target, cur + step) : Math.max(target, cur - step);
    }

    /** The portal sprite is 168 px tall; wrap it so it is sized for the 72 px characters. */
    private static Group holder(PortalView p, double x, double y) {
        Group h = new Group(p);
        h.setTranslateX(x);
        h.setTranslateY(y);
        h.setScaleX(PORTAL_SCALE);
        h.setScaleY(PORTAL_SCALE);
        return h;
    }

    private static void placeKnight(ImageView iv, double centerX, double feetY) {
        iv.setFitWidth(KNIGHT_SIZE);
        iv.setFitHeight(KNIGHT_SIZE);
        iv.setLayoutX(centerX - KNIGHT_SIZE / 2);
        iv.setLayoutY(feetY - KNIGHT_FEET_PAD);
    }

    private static Group busStop(double x, double feetY) {
        Group g = new Group();
        g.getChildren().addAll(
            rect(x, feetY - 96, 120, 8, Color.web("#2c2748")),
            rect(x + 4, feetY - 88, 6, 88, Color.web("#4a4468")),
            rect(x + 110, feetY - 88, 6, 88, Color.web("#4a4468")),
            rect(x + 10, feetY - 86, 100, 62, Color.web("#8fd0ea", 0.35)),
            rect(x + 16, feetY - 30, 88, 8, Color.web("#8a5a3c")),
            rect(x + 20, feetY - 22, 6, 22, Color.web("#4a4468")),
            rect(x + 94, feetY - 22, 6, 22, Color.web("#4a4468")),
            rect(x + 132, feetY - 130, 5, 130, Color.web("#8e87aa")),
            rect(x + 118, feetY - 142, 34, 24, Color.web("#2f6fd0")));
        Ui.Label sign = new Ui.Label("BUS", 8, Color.WHITE);
        sign.setLayoutX(x + 124);
        sign.setLayoutY(feetY - 134);
        g.getChildren().add(sign);
        return g;
    }

    private Group butterflyNode(Color wing) {
        Group g = new Group();
        Polygon left = new Polygon(0, 0, -10, -8, -8, 5);
        Polygon right = new Polygon(0, 0, 10, -8, 8, 5);
        left.setFill(wing);
        right.setFill(wing);
        g.getChildren().addAll(left, right, rect(-1, -4, 2, 8, Color.web("#2c2748")));
        return g;
    }

    private Node leaf() {
        Polygon leaf = new Polygon(0, -4, 3, 0, 0, 4, -3, 0);
        leaf.setFill(rng.nextBoolean() ? Color.web("#7cc79a", 0.9) : Color.web("#ff9fc4", 0.9));
        return leaf;
    }

    private void buildCell() {
        Color mortar = Color.web("#241838");
        cellWorld.getChildren().add(new Rectangle(Ui.W, Ui.H, Color.web("#1a1128")));
        Group wall = new Group();
        for (int r = 0; r < 14; r++) {
            wall.getChildren().add(rect(0, r * 24, Ui.W, 2, mortar));
            for (int c = 0; c < 16; c++) wall.getChildren().add(rect(c * 48 + (r % 2) * 24, r * 24, 2, 24, mortar));
        }
        cellWorld.getChildren().add(wall);
        cellWorld.getChildren().add(rect(0, 300, Ui.W, Ui.H - 300, Color.web("#241a36")));
        cellWorld.getChildren().add(rect(0, 300, Ui.W, 3, Color.web("#33264a")));

        lampGlow.setTranslateX(360);
        lampGlow.setTranslateY(92);
        cellWorld.getChildren().addAll(rect(358, 0, 4, 72, Color.web("#0c0816")), rect(344, 72, 32, 20, Color.web("#3a2a50")), lampGlow);

        greenGlow.setTranslateX(190);
        greenGlow.setTranslateY(FEET - 36);
        greenGlow.setOpacity(0);
        cellPlayer = Sprites.view(app.save().sibling().sheet(false));
        StoryFx.stand(cellPlayer, 190, FEET, SCALE);
        cellPortal.setVisible(false);
        cellWorld.getChildren().addAll(greenGlow, StoryFx.shadow(190, FEET, 36), cellPlayer, holder(cellPortal, CELL_PORTAL_X, CELL_PORTAL_Y));

        for (double x = 30; x < 700; x += 46) {
            Rectangle bar = rect(x, 0, 10, 330, BAR);
            Rectangle shine = rect(x, 0, 3, 330, SHINE);
            barRects.add(bar);
            shineRects.add(shine);
            bars.getChildren().addAll(bar, shine);
        }
        for (double y : new double[] {60, 322}) {
            Rectangle cross = rect(0, y, Ui.W, 8, Color.web("#6e6a8c"));
            crossRects.add(cross);
            bars.getChildren().add(cross);
        }
        cellWorld.getChildren().add(bars);

        placeKnight(cellKnight, 520, CELL_KNIGHT_FEET);
        cellKnight.setOpacity(0);
        cellKnightShadow.setOpacity(0);
        cellWorld.getChildren().addAll(cellKnightShadow, cellKnight);

        shock.setLayoutX(420);
        shock.setLayoutY(CELL_KNIGHT_FEET - KNIGHT_FEET_PAD - 30);
        shock.setVisible(false);
        greenTint.setOpacity(0);
        greenTint.setMouseTransparent(true);
        cellWorld.getChildren().addAll(StoryFx.vignette(Color.web("#05020c", 0.65)), greenTint, shock, cellFx);
        cellWorld.setVisible(false);
    }

    private void buildField() {
        fieldWorld.getChildren().add(Sprites.view("backdrop/field.png"));

        Color[] wings = {Color.web("#ff7aa8"), Color.web("#ffd84d"), Color.web("#7fd0ff"), Color.web("#b98cff"), Color.web("#ff9a57"), Color.web("#ffffff")};
        for (int i = 0; i < 14; i++) {
            Group node = butterflyNode(wings[i % wings.length]);
            butterflies.add(new Butterfly(node, 40 + rng.nextDouble() * 640, 90 + rng.nextDouble() * 220,
                    30 + rng.nextDouble() * 60, 10 + rng.nextDouble() * 25, 0.4 + rng.nextDouble() * 0.5, 0.7 + rng.nextDouble() * 0.8, rng.nextDouble() * 6.28));
            fieldWorld.getChildren().add(node);
        }

        ImageView fieldPlayer = Sprites.view(app.save().sibling().sheet(false));
        StoryFx.stand(fieldPlayer, 190, FEET, SCALE);
        fieldWorld.getChildren().addAll(StoryFx.shadow(190, FEET, 36), fieldPlayer, holder(fieldPortal, 190, FEET - 55), fieldFx);
        fieldWorld.setVisible(false);
    }

    // ------------------------------------------------------------------ flow

    @Override
    public void enter() {
        enterBeat(Beat.PARK_PORTAL);
    }

    private void enterBeat(Beat b) {
        beat = b;
        beatT = 0;
        park.world.setVisible(b == Beat.PARK_PORTAL);
        cellWorld.setVisible(b == Beat.CELL || b == Beat.CELL_EMPTY);
        fieldWorld.setVisible(b == Beat.FIELD);
        street.world.setVisible(b == Beat.STREET);

        switch (b) {
            case PARK_PORTAL -> {
                park.reset();
                panic = false;
                panicT = 0;
                for (Villager v : villagers) {
                    v.dx = 0;
                    for (Node n : new Node[] {v.node, v.shadow}) {
                        n.setVisible(true);
                        n.setTranslateX(0);
                        n.setTranslateY(0);
                    }
                    v.bang.setVisible(false);
                    v.bang.setTranslateX(0);
                    v.bang.setTranslateY(0);
                }
            }
            case CELL -> resetCell();
            case FIELD -> {
                fieldFx.burst(190, FEET - 36, 14, () -> StoryFx.sparkle(3, Color.web("#c8ffd8", 0.9)), 90, 1.4, -30);
                if (!fieldDrift) {
                    fieldDrift = true;
                    fieldFx.drift(10, () -> StoryFx.sparkle(2.5, Color.web("#fff6a0", 0.85)), 0, -7, 24);
                }
                app.sfx(Sfx.MISSION);
            }
            case CELL_EMPTY -> {
                cellPlayer.setVisible(false);
                cellPortal.setVisible(false);
                bars.setVisible(false);
                cellKnightIn = true;
                cellKnightLeaving = false;
                cellKnightSlide = 0;
                cellKnightAlpha = 1;
                cellKnight.setOpacity(1);
                cellKnight.setTranslateX(-80);
                cellKnightShadow.setOpacity(1);
                cellKnightShadow.setTranslateX(-80);
                greenOn = false;
                greenGlow.setOpacity(0.25);
                greenTint.setOpacity(0.08);
                startLeaves();
            }
            case STREET -> {
                street.reset();
                street.openPortal(0.5);
            }
            default -> { }
        }
        box.show(PAGES.get(b.ordinal()));
        lastPage = -1;
    }

    private void resetCell() {
        cellKnightIn = false;
        cellKnightLeaving = false;
        greenOn = false;
        melting = false;
        cellPortalOn = false;
        suckDone = false;
        cellKnightAlpha = 0;
        cellKnightSlide = 0;
        greenT = 0;
        meltT = 0;
        cellPortalT = 0;
        suckT = 0;
        cellKnight.setOpacity(0);
        cellKnight.setTranslateX(0);
        cellKnightShadow.setOpacity(0);
        cellKnightShadow.setTranslateX(0);
        greenGlow.setOpacity(0);
        greenTint.setOpacity(0);
        cellPortal.setVisible(false);
        cellPlayer.setVisible(true);
        cellPlayer.setTranslateX(0);
        cellPlayer.setTranslateY(0);
        cellPlayer.setScaleX(1);
        cellPlayer.setScaleY(1);
        cellPlayer.setRotate(0);
        bars.setVisible(true);
        bars.setOpacity(1);
        for (int i = 0; i < barRects.size(); i++) {
            barRects.get(i).setHeight(330);
            barRects.get(i).setFill(BAR);
            shineRects.get(i).setHeight(330);
            shineRects.get(i).setFill(SHINE);
        }
        for (Rectangle c : crossRects) c.setOpacity(1);
    }

    private void startPanic() {
        panic = true;
        panicT = 0;
        for (Villager v : villagers) {
            v.speed = (v.homeX < 540 ? -1 : 1) * (130 + rng.nextDouble() * 70);
            v.bang.setVisible(true);
        }
    }

    private void startLeaves() {
        if (leavesStarted) return;
        leavesStarted = true;
        cellFx.drift(14, this::leaf, 6, 20, 26);
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        beatT += tpf;
        box.update(tpf);
        cellFx.update(tpf);
        fieldFx.update(tpf);

        if (trans == 1) {
            transT += tpf;
            flash.setOpacity(Math.min(1, transT / 0.45));
            if (transT >= 0.45) {
                enterBeat(transTo);
                trans = 2;
                transT = 0;
            }
        } else if (trans == 2) {
            transT += tpf;
            flash.setOpacity(Math.max(0, 1 - transT / 0.6));
            if (transT >= 0.6) {
                trans = 0;
                flash.setOpacity(0);
            }
        }

        int pg = box.page();
        if (pg != lastPage) {
            lastPage = pg;
            onPage(pg);
        }

        if (park.world.isVisible()) {
            park.update(tpf);
            animateVillagers(tpf);
        }
        if (street.world.isVisible()) street.update(tpf);
        if (cellWorld.isVisible()) animateCell(tpf);
        if (fieldWorld.isVisible()) animateField();
    }

    private void onPage(int pg) {
        switch (beat) {
            case PARK_PORTAL -> {
                if (pg == 1) {
                    park.openPortal(0.8);
                    startPanic();
                    Audio.playMusic(KNIGHT_MUSIC, 0.5);
                } else if (pg == 4) {
                    app.sfx(Sfx.BOOST);
                }
            }
            case CELL -> {
                if (pg == 1) cellKnightIn = true;
                else if (pg == 2) cellKnightLeaving = true;
                else if (pg == 3) {
                    greenOn = true;
                    Audio.playMusic(NATURE_MUSIC, 0.45);             // her theme starts with the whisper and carries on into the grassfield
                    app.sfx(Sfx.MISSION);
                } else if (pg == 4) {
                    melting = true;
                    startLeaves();
                    cellFx.burst(CELL_PORTAL_X, CELL_PORTAL_Y, 16, () -> StoryFx.sparkle(3, Color.web("#c8ffd8", 0.9)), 110, 1.4, -30);
                    app.sfx(Sfx.BOOST);
                }
            }
            case CELL_EMPTY -> {
                if (pg == 1) {
                    shock.setVisible(true);
                    Audio.playMusic(KNIGHT_MUSIC, 0.5);              // he finds the empty cell: back to his theme
                    app.sfx(Sfx.WRONG);
                } else if (pg == 2) {
                    shock.setVisible(false);
                }
            }
            case STREET -> {
                if (pg == 2) street.dropMaze();
            }
            default -> { }
        }
    }

    private void animateVillagers(double tpf) {
        for (Villager v : villagers) {
            if (!v.node.isVisible()) continue;
            if (panic) {
                v.dx += v.speed * tpf;
                double hop = -Math.abs(Math.sin((panicT + v.homeX) * 14)) * 10;
                v.node.setTranslateX(v.dx);
                v.node.setTranslateY(hop);
                v.shadow.setTranslateX(v.dx);
                v.bang.setTranslateX(v.dx);
                v.bang.setTranslateY(hop);
                double x = v.homeX + v.dx;
                if (x < -60 || x > Ui.W + 60) {
                    v.node.setVisible(false);
                    v.shadow.setVisible(false);
                    v.bang.setVisible(false);
                }
            } else {
                v.node.setTranslateY(Math.round(Math.sin(t * 2 + v.homeX) * 2));
            }
        }
        if (panic) panicT += tpf;
    }

    private void animateField() {
        for (Butterfly b : butterflies) {
            b.node.setTranslateX(b.baseX + b.ax * Math.sin(b.w1 * t + b.phase));
            b.node.setTranslateY(b.baseY + b.ay * Math.sin(b.w2 * t + b.phase * 1.7));
            b.node.setScaleX(0.45 + 0.55 * Math.abs(Math.sin(t * 12 + b.phase)));
            b.node.setRotate(14 * Math.cos(b.w1 * t + b.phase));
        }
        fieldPortal.update(1.0 + beatT);
        double k = Math.min(1, beatT / 1.6);
        fieldPortal.setScaleX(fieldPortal.getScaleX() * (1 - 0.85 * k));
        fieldPortal.setScaleY(fieldPortal.getScaleY() * (1 - 0.85 * k));
        fieldPortal.setOpacity(1 - k);
        fieldPortal.setVisible(k < 1);
    }

    private void animateCell(double tpf) {
        lampGlow.setOpacity(0.7 + 0.15 * Math.sin(t * 7) + 0.1 * Math.sin(t * 13));

        if (beat == Beat.CELL) {
            if (cellKnightLeaving) {
                cellKnightSlide += 180 * tpf;
                cellKnightAlpha = Math.max(0, cellKnightAlpha - tpf / 0.8);
            } else if (cellKnightIn) {
                cellKnightAlpha = Math.min(1, cellKnightAlpha + tpf / 0.5);
            }
            cellKnight.setOpacity(cellKnightAlpha);
            cellKnightShadow.setOpacity(cellKnightAlpha);
            cellKnight.setTranslateX(cellKnightSlide);
            cellKnightShadow.setTranslateX(cellKnightSlide);
        }
        if (beat == Beat.CELL_EMPTY) {
            cellKnight.setTranslateX(-80);
        }
        cellKnight.setTranslateY(Math.round(Math.sin(t * 2.2) * 3));
        shock.setTranslateY(Math.round(Math.sin(t * 9) * 3));

        if (greenOn) {
            greenT = Math.min(1, greenT + tpf / 1.5);
            greenGlow.setOpacity(greenT * 0.8);
            greenTint.setOpacity(greenT * 0.22);
        }

        if (melting) {
            meltT = Math.min(1, meltT + tpf / 1.6);
            for (int i = 0; i < barRects.size(); i++) {
                Rectangle bar = barRects.get(i);
                double p = Math.max(0, Math.min(1, meltT * 1.5 - (i % 5) * 0.08));
                bar.setHeight(330 * (1 - p));
                bar.setFill(BAR.interpolate(LEAF_DARK, p));
                shineRects.get(i).setHeight(330 * (1 - p));
                shineRects.get(i).setFill(SHINE.interpolate(LEAF_LIGHT, p));
                if (p > 0 && p < 1 && rng.nextDouble() < 0.07) {
                    cellFx.burst(bar.getX() + 5, bar.getHeight(), 2, this::leaf, 45, 1.0, 70);
                }
            }
            for (Rectangle c : crossRects) c.setOpacity(1 - meltT);

            if (meltT > 0.5 && !cellPortalOn) {
                cellPortalOn = true;
                cellPortalT = 0;
                cellPortal.setVisible(true);
                cellFx.converge(14, () -> StoryFx.sparkle(3, Color.web("#9fe0cf")), CELL_PORTAL_X, CELL_PORTAL_Y, 1.1, 60);
                app.sfx(Sfx.MISSION);
            }
        }

        if (cellPortalOn) {
            cellPortalT += tpf;
            cellPortal.update(cellPortalT);
            if (cellPortalT > 1.2 && !suckDone) {
                suckT = Math.min(1, suckT + tpf / 1.3);
                double e = suckT * suckT;
                cellPlayer.setTranslateX((CELL_PORTAL_X - 190) * e);
                cellPlayer.setTranslateY((CELL_PORTAL_Y - (FEET - 36)) * e);
                cellPlayer.setScaleX(1 - 0.92 * suckT);
                cellPlayer.setScaleY(1 - 0.92 * suckT);
                cellPlayer.setRotate(suckT * 720);
                if (suckT >= 1) {
                    suckDone = true;
                    cellPlayer.setVisible(false);
                    cellFx.burst(CELL_PORTAL_X, CELL_PORTAL_Y, 16, () -> StoryFx.sparkle(3, Color.web("#c8ffd8", 0.9)), 120, 1.0, 0);
                    app.sfx(Sfx.BOOST);
                }
            }
        }
    }

    @Override
    public void onConfirm() {
        if (trans == 1) return;
        app.sfx(Sfx.SELECT);
        if (box.advance()) endBeat();
    }

    private void endBeat() {
        if (beat == Beat.STREET) {
            next.run();
            return;
        }
        Color c = switch (beat) {
            case CELL -> Color.web("#baf5c8");
            case FIELD -> Color.web("#0a0612");
            default -> Color.web("#b070ff");
        };
        flash.setFill(c);
        transTo = Beat.values()[beat.ordinal() + 1];
        trans = 1;
        transT = 0;
        if (beat == Beat.PARK_PORTAL || beat == Beat.CELL_EMPTY) app.sfx(Sfx.BOOST);
    }

    @Override
    public void onCancel() {
        next.run();
    }
}
