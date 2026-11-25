package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * Story before boss 2, after level 6: the Queen's Knight steps out of a purple portal in the park and takes the
 * Guardian prisoner, Mother Nature's voice frees the Guardian, the knight finds the empty cell, follows through his own
 * portal and drops the trash maze from the sky. {@code next} then starts the chase.
 */
public final class KnightCutsceneScreen extends Screen {
    private static final double FEET = 340;
    private static final double SCALE = 2;
    private static final double PURPLE_HUE = 0.63;
    private static final Color PURPLE = Color.web("#b070ff", 0.85);
    private static final double KNIGHT_SIZE = 200, KNIGHT_FEET_PAD = 196;

    private enum Beat { PARK_PORTAL, CELL, PARK_FREE, CELL_EMPTY, PARK_MAZE }

    private static final List<List<String>> PAGES = List.of(
        List.of(
            "You stroll through the park, humming, looking for the next area to clean. The birds are singing, families are picnicking, " +
            "and for once nothing smells like garbage.",

            "Then the air above the path crackles and tears open. A ring of violet light spins wide, and a knight in dark armor strides " +
            "out of a purple portal. The whole park screams and scatters!",

            "Knight: Halt, Guardian! I am the Queen's knight, sent by Queen Muck herself. Her Majesty is tired of you cleaning up her kingdom.",

            "Knight: You will come with me. Quietly, if you know what is good for you. Loudly, if you do not!",

            "Before you can run, violet light swallows the park, and the ground drops out from under your feet."),
        List.of(
            "You wake up in a cold stone cell, lit by one flickering purple lamp. Iron bars stand between you and a long, dark hall.",

            "Knight: Stay put and stay quiet. Her Majesty will decide what becomes of a meddling little Guardian. I will be back to check on you!",

            "His footsteps fade down the hall. You rattle the bars, shove the lock and search every crack, but there is no way out. " +
            "Then something warm brushes your mind.",

            "???: Do not be afraid, little Guardian. I shall lend you my assistance.",

            "A soft green light seeps through the stone. The lock clicks, the bars melt into leaves, and the whole cell dissolves " +
            "into drifting petals. Who was that voice?"),
        List.of(
            "You blink. You are back in the park, standing among the trees and picnic blankets. The people have fled, the portal is gone, " +
            "and your heart is pounding.",

            "Sprout: Guardian, you are free! I do not know who helped you, but do not stand around. That knight will come looking for you!"),
        List.of(
            "Back in the dungeon, the knight returns to check on his prisoner.",

            "Knight: Prisoner, rise and... what? The cell is EMPTY? The lock is covered in flowers! How?!",

            "Knight: Her Majesty will have my head if the Guardian gets away. You cannot hide from me, wherever you run!"),
        List.of(
            "You race down the park path, searching for the next area. Behind you the air tears open again, and the knight's own portal " +
            "spits him out right in your way!",

            "Knight: Found you! Did you think a few flowers could save you from the Queen's will?",

            "Knight: Rise, maze of rubbish! Fall from the sky and trap the Guardian!",

            "Sprout: He is walling you in with junk! Run, Guardian, and get to the exit at the end of the maze! Move with the arrow keys or WASD. " +
            "You cannot fight him, so use the junk cubes to throw him off your trail, stay out of his sludge, it slows you down, " +
            "and dodge the sad ghosts and his sludge bombs!"));

    private static final class Villager {
        final ImageView node;
        final Ui.Label bang;
        final double homeX;
        double dx, speed;
        Villager(ImageView node, Ui.Label bang, double homeX) { this.node = node; this.bang = bang; this.homeX = homeX; }
    }

    private static final class Cube {
        final ImageView node;
        final double targetY;
        double y = -60, vy, delay;
        boolean landed;
        Cube(ImageView node, double targetY) { this.node = node; this.targetY = targetY; }
    }

    private final Runnable next;
    private final DialogBox box;
    private final Random rng = new Random();
    private final Rectangle flash = new Rectangle(Ui.W, Ui.H, Color.WHITE);

    private final Group parkWorld = new Group();
    private final StoryFx.Particles parkFx = new StoryFx.Particles(Ui.W, Ui.H);
    private final PortalView portal = new PortalView(PURPLE, PURPLE_HUE);
    private final ImageView parkKnight = Sprites.view("boss/knight_story.png");
    private final ImageView parkPlayer;
    private final Node parkKnightShadow = StoryFx.shadow(500, FEET, 110);
    private final Node cellKnightShadow = StoryFx.shadow(520, FEET, 110);
    private final List<Villager> villagers = new ArrayList<>();
    private final Group cubeLayer = new Group();
    private final List<Cube> cubes = new ArrayList<>();

    private final Group cellWorld = new Group();
    private final StoryFx.Particles cellFx = new StoryFx.Particles(Ui.W, Ui.H);
    private ImageView cellPlayer;
    private final ImageView cellKnight = Sprites.view("boss/knight_story.png");
    private final Group bars = new Group();
    private final Circle lampGlow = StoryFx.glow(170, Color.web("#b070ff", 0.55));
    private final Circle greenGlow = StoryFx.glow(230, Color.web("#9fffc0", 0.8));
    private final Rectangle greenTint = new Rectangle(Ui.W, Ui.H, Color.web("#8affb0"));
    private final Ui.Label shock = new Ui.Label("!?", 28, Color.web("#ff4d6d"));

    private Beat beat = Beat.PARK_PORTAL;
    private double t, beatT;
    private int lastPage = -1;

    private int trans;
    private double transT;
    private Beat transTo;

    private boolean portalOn;
    private double portalT;
    private boolean panic;
    private double panicT;
    private double shakeT;
    private int landedCubes;

    private boolean cellKnightIn, cellKnightLeaving, greenOn, melting, leavesStarted;
    private double cellKnightAlpha, cellKnightSlide, greenT, meltT;

    public KnightCutsceneScreen(EcoQuestApp app, Runnable next) {
        super(app);
        this.next = next;

        parkWorld.getChildren().add(StoryFx.scene("park"));
        parkWorld.getChildren().add(StoryFx.vignette(Color.web("#1e0f30", 0.3)));
        portal.setVisible(false);
        parkWorld.getChildren().add(portal);

        double[] xs = {90, 270, 345, 430, 640};
        for (int i = 0; i < xs.length; i++) {
            double feet = 300 + (i % 3) * 14;
            ImageView v = Sprites.view("npc/npc_" + i + ".png");
            StoryFx.stand(v, xs[i], feet, 1.6);
            Ui.Label bang = new Ui.Label("!", 20, Color.web("#ff4d6d"));
            bang.setLayoutX(xs[i] - 6);
            bang.setLayoutY(feet - 72 * 1.6 - 30);
            bang.setVisible(false);
            villagers.add(new Villager(v, bang, xs[i]));
            parkWorld.getChildren().addAll(StoryFx.shadow(xs[i], feet, 50), v, bang);
        }

        placeKnight(parkKnight, 500);
        parkKnight.setOpacity(0);
        parkKnightShadow.setOpacity(0);
        parkPlayer = Sprites.view(app.save().sibling().sheet(false));
        StoryFx.stand(parkPlayer, 190, FEET, SCALE);
        parkWorld.getChildren().addAll(StoryFx.shadow(190, FEET, 90), parkKnightShadow, parkKnight, parkPlayer, cubeLayer, parkFx);

        buildCell(app);

        flash.setOpacity(0);
        flash.setMouseTransparent(true);
        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().addAll(parkWorld, cellWorld, flash, box);
    }

    private static void placeKnight(ImageView iv, double centerX) {
        iv.setFitWidth(KNIGHT_SIZE);
        iv.setFitHeight(KNIGHT_SIZE);
        iv.setLayoutX(centerX - KNIGHT_SIZE / 2);
        iv.setLayoutY(FEET - KNIGHT_FEET_PAD);
    }

    private static Rectangle rect(double x, double y, double w, double h, Color fill) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setFill(fill);
        return r;
    }

    private void buildCell(EcoQuestApp app) {
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
        greenGlow.setTranslateY(FEET - 70);
        greenGlow.setOpacity(0);
        cellPlayer = Sprites.view(app.save().sibling().sheet(false));
        StoryFx.stand(cellPlayer, 190, FEET, SCALE);
        cellWorld.getChildren().addAll(greenGlow, StoryFx.shadow(190, FEET, 90), cellPlayer);

        for (double x = 30; x < 700; x += 46) {
            bars.getChildren().add(rect(x, 0, 10, 330, Color.web("#8a86a8")));
            bars.getChildren().add(rect(x, 0, 3, 330, Color.web("#b5b1d4")));
        }
        bars.getChildren().add(rect(0, 60, Ui.W, 8, Color.web("#6e6a8c")));
        bars.getChildren().add(rect(0, 290, Ui.W, 8, Color.web("#6e6a8c")));
        cellWorld.getChildren().add(bars);

        placeKnight(cellKnight, 520);
        cellKnight.setOpacity(0);
        cellKnightShadow.setOpacity(0);
        cellWorld.getChildren().addAll(cellKnightShadow, cellKnight);

        shock.setLayoutX(420);
        shock.setLayoutY(FEET - KNIGHT_FEET_PAD - 30);
        shock.setVisible(false);
        greenTint.setOpacity(0);
        greenTint.setMouseTransparent(true);
        cellWorld.getChildren().addAll(StoryFx.vignette(Color.web("#05020c", 0.65)), greenTint, shock, cellFx);
        cellWorld.setVisible(false);
    }

    @Override
    public void enter() {
        enterBeat(Beat.PARK_PORTAL);
    }

    private void enterBeat(Beat b) {
        beat = b;
        beatT = 0;
        boolean cell = b == Beat.CELL || b == Beat.CELL_EMPTY;
        parkWorld.setVisible(!cell);
        cellWorld.setVisible(cell);

        switch (b) {
            case PARK_PORTAL -> resetPark(true);
            case PARK_FREE -> {
                resetPark(false);
                parkFx.burst(190, FEET - 70, 12, () -> StoryFx.sparkle(3, Color.web("#c8ffd8", 0.9)), 80, 1.4, -30);
            }
            case PARK_MAZE -> {
                resetPark(false);
                openPortal(560, 215);
            }
            case CELL -> {
                cellPlayer.setVisible(true);
                bars.setVisible(true);
                bars.setOpacity(1);
            }
            case CELL_EMPTY -> {
                cellPlayer.setVisible(false);
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
            default -> { }
        }
        box.show(PAGES.get(b.ordinal()));
        lastPage = -1;
    }

    private void resetPark(boolean withVillagers) {
        portalOn = false;
        portalT = 0;
        portal.setVisible(false);
        panic = false;
        panicT = 0;
        shakeT = 0;
        parkWorld.setTranslateX(0);
        parkWorld.setTranslateY(0);
        parkKnight.setOpacity(0);
        parkKnightShadow.setOpacity(0);
        parkKnight.setTranslateX(0);
        cubeLayer.getChildren().clear();
        cubes.clear();
        landedCubes = 0;
        for (Villager v : villagers) {
            v.dx = 0;
            v.node.setTranslateX(0);
            v.node.setVisible(withVillagers);
            v.bang.setVisible(false);
            v.bang.setTranslateX(0);
        }
    }

    private void openPortal(double x, double y) {
        portal.setTranslateX(x);
        portal.setTranslateY(y);
        portal.setVisible(true);
        portalOn = true;
        portalT = 0;
        app.sfx(Sfx.MISSION);
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
        cellFx.drift(14, () -> {
            Polygon leaf = new Polygon(0, -4, 3, 0, 0, 4, -3, 0);
            leaf.setFill(rng.nextBoolean() ? Color.web("#7cc79a", 0.9) : Color.web("#ff9fc4", 0.9));
            return leaf;
        }, 6, 20, 26);
    }

    private void dropMaze() {
        for (int i = 0; i < 18; i++) {
            double x;
            do { x = rng.nextDouble() * (Ui.W - 48); } while (Math.abs(x + 24 - 190) < 90 || Math.abs(x + 24 - 520) < 100);
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

    @Override
    public void update(double tpf) {
        t += tpf;
        beatT += tpf;
        box.update(tpf);
        parkFx.update(tpf);
        cellFx.update(tpf);

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

        if (parkWorld.isVisible()) animatePark(tpf);
        if (cellWorld.isVisible()) animateCell(tpf);
    }

    private void onPage(int pg) {
        switch (beat) {
            case PARK_PORTAL -> {
                if (pg == 1) {
                    openPortal(540, 215);
                    startPanic();
                    Audio.playMusic("chase.wav", 0.55);
                } else if (pg == 4) {
                    app.sfx(Sfx.BOOST);
                }
            }
            case CELL -> {
                if (pg == 1) cellKnightIn = true;
                else if (pg == 2) cellKnightLeaving = true;
                else if (pg == 3) {
                    greenOn = true;
                    app.sfx(Sfx.MISSION);
                } else if (pg == 4) {
                    melting = true;
                    startLeaves();
                    cellFx.burst(190, FEET - 70, 14, () -> StoryFx.sparkle(3, Color.web("#c8ffd8", 0.9)), 90, 1.4, -30);
                    app.sfx(Sfx.BOOST);
                }
            }
            case CELL_EMPTY -> {
                if (pg == 1) {
                    shock.setVisible(true);
                    app.sfx(Sfx.WRONG);
                } else if (pg == 2) {
                    shock.setVisible(false);
                }
            }
            case PARK_MAZE -> {
                if (pg == 2) dropMaze();
            }
            default -> { }
        }
    }

    private void animatePark(double tpf) {
        if (portalOn) {
            portalT += tpf;
            portal.update(portalT);
        }
        boolean knightOut = portalOn && portalT > 0.8;
        double op = parkKnight.getOpacity();
        if (knightOut && op < 1) {
            op = Math.min(1, op + tpf / 0.6);
            parkKnight.setOpacity(op);
            parkKnightShadow.setOpacity(op);
        }
        parkKnight.setTranslateX(Math.round((1 - op) * 40));
        parkKnight.setTranslateY(Math.round(Math.sin(t * 2.2) * 3));

        for (Villager v : villagers) {
            if (!v.node.isVisible()) continue;
            if (panic) {
                v.dx += v.speed * tpf;
                v.node.setTranslateX(v.dx);
                v.node.setTranslateY(-Math.abs(Math.sin((panicT + v.homeX) * 14)) * 10);
                v.bang.setTranslateX(v.dx);
                v.bang.setTranslateY(-Math.abs(Math.sin((panicT + v.homeX) * 14)) * 10);
                double x = v.homeX + v.dx;
                if (x < -60 || x > Ui.W + 60) {
                    v.node.setVisible(false);
                    v.bang.setVisible(false);
                }
            } else {
                v.node.setTranslateY(Math.round(Math.sin(t * 2 + v.homeX) * 2));
            }
        }
        if (panic) panicT += tpf;

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
                parkFx.burst(c.node.getLayoutX() + 24, c.targetY + 48, 4, () -> StoryFx.dot(3, Color.web("#8a7a6a", 0.8)), 60, 0.6, 40);
                if (landedCubes++ % 5 == 0) app.sfx(Sfx.DUMP);
            }
            c.node.setTranslateY(c.y);
        }
        if (shakeT > 0) {
            shakeT -= tpf;
            parkWorld.setTranslateX(shakeT > 0 ? (rng.nextDouble() - 0.5) * 8 : 0);
            parkWorld.setTranslateY(shakeT > 0 ? (rng.nextDouble() - 0.5) * 8 : 0);
        }
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
            cellKnight.setTranslateX(-80 + Math.round(Math.sin(t * 3) * 3 + (lastPage == 1 ? Math.sin(t * 40) * 3 : 0)));
        }
        cellKnight.setTranslateY(Math.round(Math.sin(t * 2.2) * 3));
        shock.setTranslateY(Math.round(Math.sin(t * 9) * 3));

        if (greenOn) {
            greenT = Math.min(1, greenT + tpf / 1.5);
            greenGlow.setOpacity(greenT * 0.8);
            greenTint.setOpacity(greenT * 0.22);
        }
        if (melting) {
            meltT = Math.min(1, meltT + tpf / 1.2);
            bars.setOpacity(1 - meltT);
            bars.setTranslateY(-meltT * 20);
        }
    }

    @Override
    public void onConfirm() {
        if (trans == 1) return;
        app.sfx(Sfx.SELECT);
        if (box.advance()) endBeat();
    }

    private void endBeat() {
        if (beat == Beat.PARK_MAZE) {
            next.run();
            return;
        }
        Color c = switch (beat) {
            case CELL -> Color.web("#baf5c8");
            case PARK_FREE -> Color.web("#0a0612");
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
