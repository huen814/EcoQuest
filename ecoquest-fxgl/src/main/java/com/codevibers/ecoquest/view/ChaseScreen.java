package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.Sfx;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

/**
 * Boss 2, "the escape": the Queen's Knight hunts the Guardian down a long, scrolling junkyard maze.
 * Reach the drain at the far end. Cubes of crushed junk form a maze (they block him too), sad ghosts patrol across the maze,
 * he leaves sludge puddles that slow you down, now and then he lunges (he warns you first), and he throws sludge bombs at the
 * spot you are running towards (a dashed ring shows where it lands; the splash hurts and leaves a puddle). Halfway there he
 * enrages (red eye, faster, more bombs); when you reach the seed at the end of the street, Mother Nature grows a wall of living wood that stops him.
 * There is no Spirit Bottle here: you can only run. Looping chase music plays while the screen is up.
 */
public final class ChaseScreen extends Screen {

    private static final int PX = Sprites.PX, SH = 72;
    private static final double WORLD_W = 2600;
    private static final double Y1 = 80, Y2 = Ui.H + 4;      // feet can reach the very bottom of the screen (the ground runs all the way down)
    private static final double PLAYER_SPEED = 170, STINK_SLOW = 0.5;
    private static final int MAX_HP = 3;
    private static final double INVINCIBLE = 1.4;
    private static final double EXIT_X = WORLD_W - 120, EXIT_Y = 290;
    private static final int KNIGHT_FEET = 11;                   // the knight's sprites (transparent, drawn 1:1, ~103 px tall with the staff, he stands ~83 px tall, the Guardian 72) end at his boots
    private static final String STUN = "boss/knight_stun.png";

    /** The street as a grid of junk cubes (# = a cube), 7 rows high (down to the bottom edge), 48 px per cell: start and exit areas are clear. */
    private static final int CELL = 48, MAZE_TOP = 134;     // the cubes cover every height the Guardian can walk at, right down to the bottom of the screen
    private static final String[] MAZE = {
        "..............#.....#.....#...##...##..#.....#.........",
        ".......#......#..#..#..##.....#..#..#..#..#..#.#.......",
        ".......#......#..#..#..##..##....#.....#..#..#.#.#.....",
        "..........#...#..#..#......##.#..#..#..#..#..#...#.....",
        "..........#......#.......#....##...##.....#............",
        "...........#..#.....#..#..#...#....#....#..##..#.......",
        "...........#..#.....#.....#.......##....#.....#........"
    };

    private enum Mode { WAIT, CHASE, TELEGRAPH, LUNGE, STUN }

    private static final class Block {
        final double x, y, w, h;
        Block(double x, double y, double w, double h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    }

    private static final class Ghost {
        final double x, baseY, amp, spd, phase;
        double y;
        final ImageView node = Sprites.view("boss/ghost.png");
        Ghost(double x, double baseY, double amp, double spd, double phase) {
            this.x = x; this.baseY = baseY; this.amp = amp; this.spd = spd; this.phase = phase;
            node.setFitWidth(48); node.setFitHeight(60);
        }
    }

    private static final class Cloud {
        final double x, y;
        double life = 5.0;
        final Circle node;
        Cloud(double x, double y) {
            this.x = x; this.y = y;
            node = new Circle(x, y, 36, Color.web("#7a3fa0", 0.38));
            node.setViewOrder(-1);
        }
    }

    private static final class Ring {
        double life = 0.4;
        final Circle node;
        Ring(double x, double y, Color col) {
            node = new Circle(x, y, 8, Color.TRANSPARENT);
            node.setStroke(col); node.setStrokeWidth(2.5);
            node.setViewOrder(-900);
        }
    }

    /** A glob of gunk the creature has spat: it flies in an arc to a marked spot, then splashes. */
    private static final class Spit {
        final double sx, sy, tx, ty; double t;
        final Circle glob, mark;
        Spit(double sx, double sy, double tx, double ty, Circle glob, Circle mark) {
            this.sx = sx; this.sy = sy; this.tx = tx; this.ty = ty; this.glob = glob; this.mark = mark;
        }
    }
    private static final double SPIT_TIME = 0.95, SPIT_ARC = 120, SPIT_SPLASH_R = 36;

    /** The creature finds its way round the cubes with a grid of 16 px cells (it fits through any gap you can). */
    private static final int G = 16, GY0 = 130, GH = 22;
    private static final int GW = (int) (WORLD_W / G) + 1;
    private final boolean[][] free = new boolean[GW][GH];
    private final int[][] flow = new int[GW][GH];
    private double flowT;

    private final Runnable onWin;
    private final Pane world = new Pane();
    private final ImageView playerImg;
    private final ImageView chaserImg;
    private final Circle chaserGlow = StoryFx.glow(60, Color.web("#d01818", 0.45));
    private final List<Block> blocks = new ArrayList<>();
    private final List<Ghost> ghosts = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    private final List<Spit> spits = new ArrayList<>();
    private double spitCd;

    private final ImageView[] heartFull = new ImageView[MAX_HP];
    private final ImageView[] heartEmpty = new ImageView[MAX_HP];
    private final Rectangle barFill = new Rectangle(0, 12, Color.web("#4a9a5a"));
    private final Rectangle chaserMark = new Rectangle(6, 18, Color.web("#e03030"));
    private final Ui.Label shout;
    private final Ui.Label retryLabel;
    private final Ui.Label retryPrompt;
    private final Rectangle flashRect = new Rectangle(Ui.W, Ui.H, Color.WHITE);
    private static final double BAR_X = 180, BAR_W = 360;

    // state
    private double pfx, pfy;                 // the Guardian's feet (world coordinates)
    private double cfx, cfy;                 // the creature's feet
    private double faceX = 1, faceY = 0;
    private Mode mode = Mode.WAIT;
    private double modeT, lungeCd, lungeDx, lungeDy, cloudT;
    private double cameraX, clock, invincible, flash, shoutT, wonT = -1, retryAlpha;
    private int hp = MAX_HP;
    private boolean retryActive, enraged;
    private PauseOverlay pause;
    private boolean up, down, left, right;

    private final BossDef def;
    private Group violet;

    private static final int PS = 3;                           // screen pixels per art pixel, like the rest of the game's art

    /** One pixel-art trunk of the wall of living wood Mother Nature grows from the seed: rows of bark climb out of the ground, then the crown opens. */
    private static final class Trunk {
        final Group box;
        final ImageView body, crown;
        final int rows, crownRows;
        final double delay;
        int up = -1, open = -1;
        boolean started;
        Trunk(Group box, ImageView body, ImageView crown, double delay) {
            this.box = box; this.body = body; this.crown = crown; this.delay = delay;
            this.rows = (int) (body.getImage().getHeight() / PS);
            this.crownRows = (int) (crown.getImage().getHeight() / PS);
        }
    }

    private final ImageView seed = Sprites.view("world/seed.png");
    private final ImageView seedGlow = Sprites.view("world/seed_glow.png");
    private final Group barrier = new Group();
    private final List<Trunk> trunks = new ArrayList<>();
    private final StoryFx.Particles growFx = new StoryFx.Particles(WORLD_W, Ui.H);
    private double barrierT, barX;

    // --- the storm-wrecked street (backdrop only: none of it blocks or hurts you)
    private final Group ground = new Group();                  // litter, puddles and the dark side of the cracks
    private final Group crackGlow = new Group();               // the violet light seeping out of the cracks
    private final Group mist = new Group();
    private final double[] mistX = new double[11];
    private final Group bolt = new Group();
    private final Group rain = new Group();
    private final double[] dropV = new double[90];
    private final Rectangle thunderRect = new Rectangle(Ui.W, Ui.H, Color.web("#e4d8ff"));
    private final StoryFx.Particles debris = new StoryFx.Particles(WORLD_W, Ui.H);   // bags and paper blown along by the wind
    private final StoryFx.Particles popFx = new StoryFx.Particles(WORLD_W, Ui.H);    // scraps shed by the ghosts, sparks of lightning
    private final java.util.Random fxRng = new java.util.Random(42);
    private double thunderCd = 2.5, strikeT = -1, rumbleT = -1;
    private Rectangle streetDim;

    public ChaseScreen(EcoQuestApp app, BossDef def, Runnable onWin) {
        super(app);
        this.onWin = onWin;
        this.def = def;

        // The street: the city scene repeated along the world, a little darker.
        for (int i = 0; i * Ui.W < WORLD_W; i++) {
            var part = StoryFx.scene(def.backdrop().substring("scene:".length()));
            part.setLayoutX(i * Ui.W);
            world.getChildren().add(part);
        }
        Rectangle dim = new Rectangle(WORLD_W, Ui.H, Color.web("#1a0830", 0.3));
        streetDim = dim;
        dim.setViewOrder(-0.5);
        world.getChildren().add(dim);
        violet = StoryFx.violetSky(WORLD_W);                      // the same violet storm sky as when the knight drops the maze
        violet.setViewOrder(-0.6);
        world.getChildren().add(violet);

        // Cubes of crushed junk: walls with doors, zigzags and pillars to dodge around, like the halls of a haunted house.
        // The creature runs straight at you and gets stuck on corners, so loop around the cubes to shake it off.
        for (int r = 0; r < MAZE.length; r++) {
            for (int c = 0; c < MAZE[r].length(); c++) {
                if (MAZE[r].charAt(c) == '#') addCube(c, r);
            }
        }

        buildStreetFx();

        for (int gx = 0; gx < GW; gx++) {                         // where the creature can stand
            for (int gy = 0; gy < GH; gy++) free[gx][gy] = !blocked(gx * G + G / 2.0, GY0 + gy * G + G / 2.0, 16, 8);
        }

        double[][] patrols = {{600, 300, 125, 1.4, 0}, {1080, 300, 130, 1.7, 1.6}, {1416, 300, 130, 1.5, 3.1},
                              {2088, 300, 125, 1.9, 4.4}, {2328, 300, 130, 1.6, 5.7}};
        for (double[] p : patrols) {
            Ghost g = new Ghost(p[0], p[1], p[2], p[3], p[4]);
            ghosts.add(g);
            world.getChildren().add(g.node);
        }

        seedGlow.setLayoutX(EXIT_X - seedGlow.getImage().getWidth() / 2); seedGlow.setLayoutY(EXIT_Y - seedGlow.getImage().getHeight() / 2);
        seedGlow.setViewOrder(-1);
        seed.setLayoutX(EXIT_X - seed.getImage().getWidth() / 2); seed.setLayoutY(EXIT_Y - 13 * PS);
        seed.setViewOrder(-1);
        Ui.Label exitLabel = new Ui.Label("A SEED", 12, Ui.GOOD);
        exitLabel.setLayoutX(EXIT_X - exitLabel.textWidth() / 2.0);
        exitLabel.setLayoutY(EXIT_Y - 70);
        exitLabel.setViewOrder(-2000);
        world.getChildren().addAll(seedGlow, seed, exitLabel);
        buildBarrier();

        playerImg = new ImageView(Wardrobe.sheet(app.save(), app.save().sibling(), false));
        playerImg.setViewport(new Rectangle2D(0, 0, PX, SH));
        chaserImg = Sprites.view(def.normal());
        world.getChildren().addAll(chaserGlow, chaserImg, playerImg);
        world.setMouseTransparent(true);
        root.getChildren().add(world);

        buildRain();
        thunderRect.setOpacity(0); thunderRect.setMouseTransparent(true);
        root.getChildren().addAll(rain, thunderRect);

        flashRect.setOpacity(0); flashRect.setMouseTransparent(true);
        root.getChildren().add(flashRect);

        // HUD: hearts, title, a progress bar with the creature's position, and the shout
        for (int i = 0; i < MAX_HP; i++) {
            heartFull[i] = Sprites.view("weapon/heart.png");
            heartEmpty[i] = Sprites.view("weapon/heart_empty.png");
            for (ImageView iv : new ImageView[]{heartFull[i], heartEmpty[i]}) {
                iv.setFitWidth(28); iv.setFitHeight(28); iv.setPreserveRatio(true);
                iv.setLayoutX(14 + i * 34); iv.setLayoutY(14);
                root.getChildren().add(iv);
            }
        }
        Ui.Label title = new Ui.Label("ESCAPE!", 10, Ui.WHITE);
        title.setLayoutX((Ui.W - title.textWidth()) / 2.0);
        title.setLayoutY(14);
        Rectangle track = new Rectangle(BAR_W, 12, Color.web("#2a4a2a"));
        track.setLayoutX(BAR_X); track.setLayoutY(30);
        track.setStroke(Color.web("#ffffff", 0.25));
        barFill.setLayoutX(BAR_X); barFill.setLayoutY(30);
        chaserMark.setLayoutY(27);
        root.getChildren().addAll(title, track, barFill, chaserMark);

        shout = new Ui.Label("RAAAH!", 14, Ui.BAD);
        shout.setLayoutX((Ui.W - shout.textWidth()) / 2.0);
        shout.setLayoutY(52);
        shout.setOpacity(0);
        root.getChildren().add(shout);

        retryLabel = new Ui.Label("CAUGHT!", 24, Ui.WHITE);
        retryLabel.setLayoutX((Ui.W - retryLabel.textWidth()) / 2.0);
        retryLabel.setLayoutY(Ui.H / 2.0 - 50);
        retryPrompt = new Ui.Label("ENTER  Try Again     X  Quit to Menu", 11, Ui.ACCENT);
        retryPrompt.setLayoutX((Ui.W - retryPrompt.textWidth()) / 2.0);
        retryPrompt.setLayoutY(Ui.H / 2.0);
        retryLabel.setOpacity(0); retryPrompt.setOpacity(0);
        root.getChildren().addAll(retryLabel, retryPrompt);
        pause = new PauseOverlay(app, () -> { }, app::quitBossFight);
        root.getChildren().add(pause);

        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP, W    -> up = true;
                case DOWN, S  -> down = true;
                case LEFT, A  -> left = true;
                case RIGHT, D -> right = true;
                case X        -> { if (retryActive) onCancel(); }
                default -> { }
            }
        });
        root.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case UP, W    -> up = false;
                case DOWN, S  -> down = false;
                case LEFT, A  -> left = false;
                case RIGHT, D -> right = false;
                case ENTER, Z -> { if (retryActive) retry(); }
                default -> { }
            }
        });
    }

    /** One cube of crushed junk standing in the street. It blocks the Guardian and the creature. */
    private void addCube(int col, int row) {
        double x = col * CELL, y = MAZE_TOP + row * CELL;
        blocks.add(new Block(x, y, CELL, CELL));
        ImageView cube = Sprites.view("boss/junk_cube" + ((col * 7 + row * 3) % 3) + ".png");
        cube.setFitWidth(CELL); cube.setFitHeight(CELL);
        cube.setLayoutX(x); cube.setLayoutY(y);
        cube.setViewOrder(-(y + CELL));
        world.getChildren().add(cube);
    }

    // ------------------------------------------------------------------------------------------------
    @Override
    public void enter() {
        hp = MAX_HP;
        pfx = 130; pfy = 270;
        cfx = 30; cfy = 270;
        cameraX = 0; world.setTranslateX(0);                     // the camera starts back at the beginning of the street, not wherever you were caught
        mode = Mode.WAIT; modeT = 1.0; lungeCd = 3.5; cloudT = 0; flowT = 0;
        invincible = 0; flash = 0; wonT = -1; retryActive = false; retryAlpha = 0; clock = 0;
        faceX = 1; faceY = 0;
        retryLabel.setOpacity(0); retryPrompt.setOpacity(0); flashRect.setOpacity(0);
        up = down = left = right = false;
        for (Cloud c : clouds) world.getChildren().remove(c.node);
        for (Ring r : rings) world.getChildren().remove(r.node);
        clouds.clear(); rings.clear();
        for (Spit sp : spits) world.getChildren().removeAll(sp.glob, sp.mark);
        spits.clear(); spitCd = 6.0;
        for (Ghost g : ghosts) if (!world.getChildren().contains(g.node)) world.getChildren().add(g.node);
        enraged = false;
        resetStorm();
        resetBarrier();
        chaserImg.setImage(Sprites.get(def.normal()));
        say("RUN!", 1.4);
        refreshHud();
        root.requestFocus();
        Audio.playMusic("chase.wav", 0.55);
    }

    @Override
    public void exit() { Audio.stopMusic(); }

    private void retry() { enter(); }

    @Override public void onDirection(Direction d) { if (pause.isOpen()) pause.onDirection(d); }

    @Override public void onConfirm() { if (pause.isOpen()) pause.onConfirm(); }

    /** ESC pauses the chase (RESUME / SETTINGS / QUIT); on the game-over screen it still quits. */
    @Override
    public void onCancel() {
        if (pause.isOpen()) pause.onCancel();
        else if (retryActive || wonT >= 0) app.quitBossFight();
        else openPause();
    }

    @Override public void onPause() {
        if (pause.isOpen()) pause.close();
        else if (!retryActive && wonT < 0) openPause();
    }

    private void openPause() {
        up = down = left = right = false;
        strikeT = -1; rumbleT = -1; bolt.setVisible(false); thunderRect.setOpacity(0);
        pause.open();
    }

    private void say(String text, double seconds) {
        shout.setText(text);
        shout.setLayoutX((Ui.W - shout.textWidth()) / 2.0);
        shoutT = seconds;
    }

    // ------------------------------------------------------------------------------------------------
    @Override
    public void update(double tpf) {
        if (pause.isOpen()) { pause.update(tpf); return; }
        clock += tpf;
        if (violet != null) violet.setOpacity(0.9 + 0.1 * Math.sin(clock * 3));
        if (shoutT > 0) shoutT -= tpf;
        shout.setOpacity(Math.max(0, Math.min(1, shoutT * 2)));
        if (flash > 0) {
            flash -= tpf;
            flashRect.setOpacity(Math.max(0, Math.min(0.55, flash)));
        }
        updateRings(tpf);
        updateStormFx(tpf);
        if (wonT >= 0) calmStorm();

        if (wonT >= 0) {                                         // the seed grows a wall of living wood between you and the knight
            wonT -= tpf;
            barrierT += tpf;
            growBarrier(tpf);
            if (wonT < 0.6) {
                flashRect.setFill(Color.web("#e8ffe8"));
                flashRect.setOpacity(Math.min(0.9, (0.6 - wonT) * 1.6));
            }
            if (wonT <= 0) onWin.run();
            return;
        }
        if (retryActive) {
            retryAlpha = Math.min(1.0, retryAlpha + tpf * 2);
            retryLabel.setOpacity(retryAlpha);
            retryPrompt.setOpacity(retryAlpha > 0.9 ? Math.abs(Math.sin(retryAlpha * 5 + System.nanoTime() * 1e-9 * 3)) : 0);
            return;
        }
        if (invincible > 0) invincible -= tpf;

        movePlayer(tpf);
        updateChaser(tpf);
        updateGhosts();
        updateClouds(tpf);
        updateSpits(tpf);
        checkContacts();

        if (Math.hypot(pfx - EXIT_X, pfy - EXIT_Y) < 42) {      // made it: the seed answers
            wonT = 3.6;
            barX = pfx - 78;                                      // the wall grows between the Guardian and the knight
            barrier.setLayoutX(barX);
            barrier.setVisible(true);
            barrierT = 0;
            if (barX - cfx > 360) cfx = barX - 360;               // keep him in view for the charge
            chaserImg.setImage(Sprites.get(def.rage()));
            growFx.burst(EXIT_X, EXIT_Y, 24, () -> StoryFx.sparkle(3, Color.web("#c8ffd8")), 140, 1.2, -20);
            app.reportBossClear(def, clock, hp, MAX_HP);
            app.sfx(Sfx.MISSION);
            say("THE FOREST ANSWERS...", 2.6);
        }
        placeNodes();
        refreshHud();
    }

    private boolean blocked(double cx, double cy, double hw, double hh) {
        for (Block b : blocks) {
            if (cx + hw > b.x && cx - hw < b.x + b.w && cy + hh > b.y && cy - hh < b.y + b.h) return true;
        }
        return false;
    }

    private void movePlayer(double tpf) {
        double dx = (right ? 1 : 0) - (left ? 1 : 0), dy = (down ? 1 : 0) - (up ? 1 : 0);
        if (dx != 0 || dy != 0) {
            double len = Math.hypot(dx, dy);
            faceX = dx / len; faceY = dy / len;
        }
        double speed = PLAYER_SPEED;
        for (Cloud c : clouds) if (Math.hypot(pfx - c.x, pfy - c.y) < 38) { speed *= STINK_SLOW; break; }
        stepPlayer(dx == 0 && dy == 0 ? 0 : dx / Math.hypot(dx, dy) * speed * tpf,
                   dx == 0 && dy == 0 ? 0 : dy / Math.hypot(dx, dy) * speed * tpf);
    }

    private void stepPlayer(double mx, double my) {
        double nx = Math.max(PX / 2.0, Math.min(WORLD_W - PX / 2.0, pfx + mx));
        if (!blocked(nx, pfy, 14, 8)) pfx = nx;
        double ny = Math.max(Y1 + SH - 10, Math.min(Y2 - 10, pfy + my));
        if (!blocked(pfx, ny, 14, 8)) pfy = ny;
    }

    private void stepChaser(double mx, double my) {
        double nx = Math.max(10, Math.min(WORLD_W - 10, cfx + mx));
        if (!blocked(nx, cfy, 16, 8)) cfx = nx;
        double ny = Math.max(Y1 + SH - 10, Math.min(Y2 - 10, cfy + my));
        if (!blocked(cfx, ny, 16, 8)) cfy = ny;
    }

    private void updateChaser(double tpf) {
        double progress = Math.min(1, pfx / EXIT_X);
        double dx = pfx - cfx, dy = pfy - cfy, dist = Math.max(1, Math.hypot(dx, dy));
        modeT -= tpf;
        switch (mode) {
            case WAIT -> {
                if (modeT <= 0) { mode = Mode.CHASE; say("HALT.", 1.6); app.sfx(Sfx.FULL); }
            }
            case CHASE -> {
                if (!enraged && progress >= 0.5) {                 // phase 2: he snaps
                    enraged = true;
                    chaserImg.setImage(Sprites.get(def.rage()));
                    app.sfx(Sfx.FULL);
                }
                double speed = 158 + 14 * progress + (enraged ? 12 : 0);                 // starts slower than you and ends a hair faster: loop round the cubes to keep your lead
                flowT -= tpf;
                if (flowT <= 0) { computeFlow(); flowT = 0.15; }
                double[] goal = chaseTarget();
                double gx = goal[0] - cfx, gy = goal[1] - cfy, glen = Math.max(1, Math.hypot(gx, gy));
                stepChaser(gx / glen * speed * tpf, gy / glen * speed * tpf);
                cloudT -= tpf;
                if (cloudT <= 0) { dropCloud(); cloudT = 0.7; }
                lungeCd -= tpf;
                if (lungeCd <= 0 && dist > 140) {                // warn first, then lunge
                    mode = Mode.TELEGRAPH; modeT = 0.55;
                    chaserImg.setImage(Sprites.get(def.rage()));
                    say("!!", 0.6);
                    app.sfx(Sfx.FULL);
                }
                spitCd -= tpf;
                if (spitCd <= 0 && dist > 220 && dist < 600) {   // spits ahead of you while you run
                    gunkSpit();
                    spitCd = (enraged ? 3.8 : 5.5) + (clock * 5 % 2);
                }
            }
            case TELEGRAPH -> {
                if (modeT <= 0) {
                    mode = Mode.LUNGE; modeT = 0.7;
                    lungeDx = dx / dist; lungeDy = dy / dist;
                }
            }
            case LUNGE -> {
                stepChaser(lungeDx * 300 * tpf, lungeDy * 300 * tpf);
                if (modeT <= 0) endLunge();
            }
            case STUN -> {
                if (modeT <= 0) {
                    mode = Mode.CHASE;
                    chaserImg.setImage(Sprites.get(enraged ? def.rage() : def.normal()));
                }
            }
        }
    }

    /** How many cells away from the Guardian every free cell is, so the creature can walk the shortest way round the cubes. */
    private void computeFlow() {
        for (int[] column : flow) java.util.Arrays.fill(column, -1);
        int sx = Math.max(0, Math.min(GW - 1, (int) (pfx / G)));
        int sy = Math.max(0, Math.min(GH - 1, (int) ((pfy - GY0) / G)));
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        flow[sx][sy] = 0;
        queue.add(new int[]{sx, sy});
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = cell[0] + dx, ny = cell[1] + dy;
                    if (nx < 0 || ny < 0 || nx >= GW || ny >= GH || flow[nx][ny] >= 0 || !free[nx][ny]) continue;
                    if (dx != 0 && dy != 0 && (!free[cell[0] + dx][cell[1]] || !free[cell[0]][cell[1] + dy])) continue;   // no cutting corners
                    flow[nx][ny] = flow[cell[0]][cell[1]] + 1;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
    }

    /** Straight at the Guardian when nothing is in the way, otherwise towards the next cell on the shortest way round. */
    private double[] chaseTarget() {
        if (lineClear(cfx, cfy, pfx, pfy)) return new double[]{pfx, pfy};
        int cx = Math.max(0, Math.min(GW - 1, (int) (cfx / G)));
        int cy = Math.max(0, Math.min(GH - 1, (int) ((cfy - GY0) / G)));
        int best = flow[cx][cy], bx = cx, by = cy;
        if (best < 0) return new double[]{pfx, pfy};
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = cx + dx, ny = cy + dy;
                if ((dx == 0 && dy == 0) || nx < 0 || ny < 0 || nx >= GW || ny >= GH || flow[nx][ny] < 0 || flow[nx][ny] >= best) continue;
                if (dx != 0 && dy != 0 && (!free[cx + dx][cy] || !free[cx][cy + dy])) continue;
                best = flow[nx][ny]; bx = nx; by = ny;
            }
        }
        if (bx == cx && by == cy) return new double[]{pfx, pfy};
        return new double[]{bx * G + G / 2.0, GY0 + by * G + G / 2.0};
    }

    private boolean lineClear(double x0, double y0, double x1, double y1) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        int steps = (int) (len / 8);
        for (int i = 1; i <= steps; i++) {
            double t = i * 8.0 / len;
            if (blocked(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, 16, 8)) return false;
        }
        return true;
    }

    private void endLunge() {
        mode = Mode.CHASE;
        lungeCd = 4.0 + (clock * 7 % 2);
        chaserImg.setImage(Sprites.get(enraged ? def.rage() : def.normal()));
    }

    private void dropCloud() {
        Cloud c = new Cloud(cfx, cfy);
        clouds.add(c);
        world.getChildren().add(c.node);
    }

    /** Sludge bomb: lobbed at the spot you are running towards. A dashed ring marks the landing; the splash hurts and leaves a sludge puddle. */
    private void gunkSpit() {
        double tx = Math.max(PX, Math.min(WORLD_W - PX, pfx + faceX * 70));
        double ty = Math.max(Y1 + SH, Math.min(Y2 - 10, pfy + faceY * 45));
        if (blocked(tx, ty, 14, 8)) { tx = pfx; ty = pfy; }        // never lands inside a cube
        Circle mark = new Circle(tx, ty, SPIT_SPLASH_R, Color.web("#a04ad0", 0.18));
        mark.setStroke(Color.web("#e0a0ff")); mark.setStrokeWidth(2);
        mark.getStrokeDashArray().addAll(6.0, 5.0);
        mark.setViewOrder(-1);
        Circle glob = new Circle(11, new RadialGradient(0, 0, 0.35, 0.3, 0.85, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#d9b8ff")), new Stop(1, Color.web("#5a2a80"))));
        glob.setStroke(Color.web("#e0a0ff")); glob.setStrokeWidth(2);
        glob.setViewOrder(-3000);
        world.getChildren().addAll(mark, glob);
        spits.add(new Spit(cfx, cfy - 55, tx, ty, glob, mark));
        say("BOMB!", 0.6);
        app.sfx(Sfx.FULL);
    }

    private void updateSpits(double tpf) {
        for (Spit sp : new ArrayList<>(spits)) {
            sp.t += tpf / SPIT_TIME;
            double p = Math.min(1, sp.t);
            sp.glob.setCenterX(sp.sx + (sp.tx - sp.sx) * p);
            sp.glob.setCenterY(sp.sy + (sp.ty - sp.sy) * p - 4 * SPIT_ARC * p * (1 - p));
            sp.mark.setOpacity(0.5 + 0.5 * Math.abs(Math.sin(clock * 14)));
            if (sp.t >= 1) {
                spits.remove(sp);
                world.getChildren().removeAll(sp.glob, sp.mark);
                ring(sp.tx, sp.ty, Color.web("#b070e0"));
                app.sfx(Sfx.DUMP);
                Cloud c = new Cloud(sp.tx, sp.ty);
                clouds.add(c);
                world.getChildren().add(c.node);
                if (invincible <= 0 && Math.hypot(pfx - sp.tx, pfy - sp.ty) < SPIT_SPLASH_R) hurt(sp.tx, sp.ty);
            }
        }
    }

    private void updateGhosts() {
        for (Ghost g : ghosts) {
            g.y = g.baseY + Math.sin(clock * g.spd + g.phase) * g.amp;
            if (g.node.getParent() != null && fxRng.nextDouble() < 0.03) {          // scraps of bag flake off as it drifts
                popFx.burst(g.x, g.y - 8, 1, () -> StoryFx.centeredItem("plastic_bag", 9), 28, 1.0, 45);
            }
        }
    }

    private void popGhost(Ghost g) {
        world.getChildren().remove(g.node);
        ring(g.x, g.y - 25, Color.web("#e4def0"));
        popFx.burst(g.x, g.y - 25, 12, () -> StoryFx.centeredItem("plastic_bag", 12), 130, 0.9, 220);
        app.sfx(Sfx.DUMP);
    }

    private void updateClouds(double tpf) {
        List<Cloud> gone = new ArrayList<>();
        for (Cloud c : clouds) {
            c.life -= tpf;
            c.node.setOpacity(Math.max(0, Math.min(1, c.life)));
            if (c.life <= 0) gone.add(c);
        }
        for (Cloud c : gone) { clouds.remove(c); world.getChildren().remove(c.node); }
    }

    private void checkContacts() {
        if (invincible > 0) return;
        if (mode != Mode.STUN && mode != Mode.WAIT && Math.hypot(pfx - cfx, pfy - cfy) < 30) {
            hurt(cfx, cfy);
            if (!retryActive) {
                mode = Mode.STUN; modeT = 0.9;                   // it staggers, giving you a moment to get away
                chaserImg.setImage(Sprites.get(STUN));
            }
            return;
        }
        for (Ghost g : ghosts) {
            if (g.node.getParent() != null && Math.hypot(pfx - g.x, pfy - g.y) < 30) {
                popGhost(g);
                hurt(g.x, g.y);
                return;
            }
        }
    }

    private void hurt(double fromX, double fromY) {
        hp = Math.max(0, hp - 1);
        invincible = INVINCIBLE;
        flash = 0.4;
        flashRect.setFill(Color.web("#e03030"));
        ring(pfx, pfy - 30, Color.web("#e06060"));
        app.sfx(Sfx.DUMP);
        double len = Math.max(1, Math.hypot(pfx - fromX, pfy - fromY));
        for (int i = 0; i < 6; i++) stepPlayer((pfx - fromX) / len * 12, (pfy - fromY) / len * 12);   // knocked back
        if (hp <= 0) { retryActive = true; retryAlpha = 0; }
    }

    private void ring(double x, double y, Color col) {
        Ring r = new Ring(x, y, col);
        rings.add(r);
        world.getChildren().add(r.node);
    }

    private void updateRings(double tpf) {
        List<Ring> gone = new ArrayList<>();
        for (Ring r : rings) {
            r.life -= tpf;
            r.node.setRadius(8 + (0.4 - r.life) * 90);
            r.node.setOpacity(Math.max(0, r.life * 2.5));
            if (r.life <= 0) gone.add(r);
        }
        for (Ring r : gone) { rings.remove(r); world.getChildren().remove(r.node); }
    }

    // ------------------------------------------------------------------------------------------------
    // The storm-wrecked street: litter, puddles, glowing cracks, mist, blowing bags, rain and lightning.
    // It stays clear of the seed at the end of the street.

    private static final String[] STREET_LITTER = {
        "aluminum_can", "banana_peel", "bottle_cap", "cardboard_box", "food_container", "food_scraps", "glass_bottle",
        "glass_jar", "newspaper", "notebook_paper", "plastic_bag", "plastic_bag", "plastic_bottle", "tin_can"
    };

    private void buildStreetFx() {
        java.util.Random r = new java.util.Random(7);
        double maxX = EXIT_X - 190;
        for (int i = 0; i < 16; i++) addCrack(60 + r.nextDouble() * (maxX - 60), 175 + r.nextDouble() * 285, r);
        for (int i = 0; i < 18; i++) {                            // puddles of sludge rainwater
            double x = 80 + r.nextDouble() * (maxX - 80), y = 190 + r.nextDouble() * 270;
            double rx = 18 + r.nextDouble() * 26, ry = 5 + r.nextDouble() * 6;
            Ellipse puddle = new Ellipse(x, y, rx, ry);
            puddle.setFill(Color.web("#150a28", 0.6));
            puddle.setStroke(Color.web("#a070e0", 0.4));
            puddle.setStrokeWidth(1.5);
            Ellipse sheen = new Ellipse(x - rx * 0.25, y - ry * 0.3, rx * 0.45, ry * 0.3);
            sheen.setFill(Color.web("#d8c0ff", 0.25));
            ground.getChildren().addAll(puddle, sheen);
        }
        for (int i = 0; i < 90; i++) {                            // litter all over the street
            double x = 30 + r.nextDouble() * (maxX - 30), y = 165 + r.nextDouble() * 295;
            double size = 18 + r.nextDouble() * 16;
            String name = STREET_LITTER[r.nextInt(STREET_LITTER.length)];
            double rot = r.nextDouble() * 360;
            if (blocked(x, y + size / 2, 12, 8)) continue;
            Ellipse shade = new Ellipse(x, y + size * 0.42, size * 0.42, size * 0.14);
            shade.setFill(Color.web("#0a0414", 0.35));
            ImageView v = StoryFx.item(name, size);
            v.setLayoutX(x - size / 2); v.setLayoutY(y - size / 2);
            v.setRotate(rot);
            ground.getChildren().addAll(shade, v);
        }
        ground.setViewOrder(-0.1);
        crackGlow.setViewOrder(-0.55);
        for (int i = 0; i < mistX.length; i++) {                  // low mist drifting over the street
            Circle m = StoryFx.glow(120 + r.nextDouble() * 60, Color.web("#cdb8ff", 0.17));
            mistX[i] = 100 + i * (WORLD_W - 200) / (mistX.length - 1) + (r.nextDouble() - 0.5) * 120;
            m.setTranslateY(300 + r.nextDouble() * 170);
            m.setScaleY(0.28);
            mist.getChildren().add(m);
        }
        mist.setViewOrder(-0.55);
        debris.drift(24, () -> StoryFx.centeredItem(fxRng.nextInt(3) == 0 ? "newspaper" : "plastic_bag", 14 + fxRng.nextInt(9)), -120, 10, 30);
        debris.setViewOrder(-1000);                               // in front of the cubes, the ghosts and the characters, behind the wall of wood
        bolt.setViewOrder(-0.8); bolt.setVisible(false); bolt.setMouseTransparent(true);
        popFx.setViewOrder(-3000);
        world.getChildren().addAll(ground, crackGlow, mist, debris, bolt, popFx);
    }

    /** A real-looking fissure: a long run that mostly crosses the street with sharp kinks, tapering to a hairline, with a few short forks. */
    private void addCrack(double x, double y, java.util.Random r) {
        double a = (r.nextBoolean() ? 0 : Math.PI) + (r.nextDouble() - 0.5) * 1.0;
        java.util.List<double[]> pts = crackPath(x, y, a, 8 + r.nextInt(5), 0.45, 14, 28, r);
        int n = pts.size() - 1;
        drawCrack(pts, 0, n / 2, 4, 0.7);
        drawCrack(pts, n / 2, n - 2, 3, 0.6);
        drawCrack(pts, n - 2, n, 2, 0.4);
        int forks = 2 + r.nextInt(2);
        for (int f = 0; f < forks; f++) {
            double[] p = pts.get(2 + r.nextInt(Math.max(1, n - 3)));
            double ba = a + (r.nextBoolean() ? 1 : -1) * (0.6 + r.nextDouble() * 0.5);
            java.util.List<double[]> br = crackPath(p[0], p[1], ba, 3 + r.nextInt(2), 0.5, 8, 16, r);
            drawCrack(br, 0, br.size() - 2, 2, 0.5);
        }
    }

    private static double snap(double v) { return Math.round(v / PS) * PS; }

    private java.util.List<double[]> crackPath(double x, double y, double a, int segs, double wobble, double minLen, double maxLen, java.util.Random r) {
        java.util.List<double[]> pts = new ArrayList<>();
        double cx = snap(x), cy = snap(y);
        for (int i = 0; i <= segs; i++) {
            pts.add(new double[]{cx, cy});
            a += (r.nextDouble() - 0.5) * wobble;
            double len = minLen + r.nextDouble() * (maxLen - minLen);
            cx = snap(cx + Math.cos(a) * len);
            cy = snap(cy + Math.sin(a) * len * 0.55);               // the street is seen from a slant, so cracks run flatter
        }
        return pts;
    }

    /** One stretch of a crack: a dark fissure, a lighter lip on its lower edge for depth, and a thin violet glow inside. */
    private void drawCrack(java.util.List<double[]> pts, int from, int to, double width, double glowAlpha) {
        Polyline dark = new Polyline(), lip = new Polyline(), lit = new Polyline();
        for (int i = from; i <= to; i++) {
            double[] p = pts.get(i);
            dark.getPoints().addAll(p[0], p[1]);
            lip.getPoints().addAll(p[0], p[1] + width);
            lit.getPoints().addAll(p[0], p[1]);
        }
        dark.setFill(null); lip.setFill(null); lit.setFill(null);
        dark.setStroke(Color.web("#0b0514", 0.9)); dark.setStrokeWidth(width);
        lip.setStroke(Color.web("#6a5a88", 0.35)); lip.setStrokeWidth(1);
        lit.setStroke(Color.web("#b070ff", glowAlpha)); lit.setStrokeWidth(1);
        lit.setBlendMode(BlendMode.ADD);
        ground.getChildren().addAll(lip, dark);
        crackGlow.getChildren().add(lit);
    }

    private void buildRain() {
        for (int i = 0; i < dropV.length; i++) {
            double len = 14 + fxRng.nextDouble() * 12;
            Line l = new Line(0, 0, -0.28 * len, len);
            l.setStroke(Color.web("#d4c8ff", 0.3 + fxRng.nextDouble() * 0.2));
            l.setStrokeWidth(1.2);
            l.setTranslateX(fxRng.nextDouble() * (Ui.W + 160));
            l.setTranslateY(fxRng.nextDouble() * Ui.H);
            dropV[i] = 620 + fxRng.nextDouble() * 320;
            rain.getChildren().add(l);
        }
        rain.setMouseTransparent(true);
    }

    private void resetStorm() {
        thunderCd = 2.5; strikeT = -1; rumbleT = -1;
        rain.setOpacity(1); mist.setOpacity(1); debris.setOpacity(1); streetDim.setOpacity(1);
        bolt.setVisible(false);
        thunderRect.setOpacity(0);
        popFx.clear();
    }

    /** Once the seed answers, the storm dies away with the wall of wood growing: the rain, mist, violet sky and cracks all fade out. */
    private void calmStorm() {
        double calm = Math.max(0, Math.min(1, (3.6 - wonT) / 2.8));
        double keep = 1 - calm * calm * (3 - 2 * calm);
        violet.setOpacity(violet.getOpacity() * keep);
        streetDim.setOpacity(keep);
        rain.setOpacity(keep); mist.setOpacity(keep); debris.setOpacity(keep);
        crackGlow.setOpacity(crackGlow.getOpacity() * keep);
    }

    private void updateStormFx(double tpf) {
        debris.update(tpf);
        popFx.update(tpf);
        for (int i = 0; i < mist.getChildren().size(); i++) {
            mist.getChildren().get(i).setTranslateX(mistX[i] + Math.sin(clock * 0.25 + i * 1.7) * 70);
        }
        for (int i = 0; i < dropV.length; i++) {
            Node l = rain.getChildren().get(i);
            double nx = l.getTranslateX() - dropV[i] * 0.28 * tpf, ny = l.getTranslateY() + dropV[i] * tpf;
            if (ny > Ui.H + 20 || nx < -30) { nx = fxRng.nextDouble() * (Ui.W + 160); ny = -30 - fxRng.nextDouble() * 40; }
            l.setTranslateX(nx); l.setTranslateY(ny);
        }
        crackGlow.setOpacity(0.5 + 0.25 * Math.sin(clock * 2.2));

        if (strikeT < 0 && wonT < 0) {
            thunderCd -= tpf;
            if (thunderCd <= 0) { strike(); thunderCd = 4 + fxRng.nextDouble() * 6; }
        }
        if (rumbleT >= 0) {
            rumbleT -= tpf;
            if (rumbleT < 0) app.sfx(Sfx.THUNDER);                 // the rumble arrives a moment after the flash
        }
        if (strikeT >= 0) {
            strikeT += tpf;
            double f = strikeT < 0.07 ? 1 : strikeT < 0.15 ? 0.2 : strikeT < 0.24 ? 0.85 : Math.max(0, 0.85 * (1 - (strikeT - 0.24) / 0.5));
            thunderRect.setOpacity(0.34 * f);
            bolt.setOpacity(strikeT < 0.35 ? (strikeT >= 0.07 && strikeT < 0.15 ? 0.3 : 1) : Math.max(0, 1 - (strikeT - 0.35) / 0.2));
            crackGlow.setOpacity(Math.min(1, crackGlow.getOpacity() + 0.5 * f));
            if (strikeT > 0.75) { strikeT = -1; bolt.setVisible(false); thunderRect.setOpacity(0); }
        }
    }

    /** A flash of lightning somewhere in view: a forked bolt, a white-violet flash over the screen and, a moment later, the thunder. */
    private void strike() {
        double x = cameraX + 90 + fxRng.nextDouble() * (Ui.W - 180);
        if (Math.abs(x - EXIT_X) < 150) x -= 220;                  // never right over the seed
        double yEnd = 210 + fxRng.nextDouble() * 220;
        java.util.List<Double> pts = new ArrayList<>();
        double cx = x;
        int segs = 9;
        for (int i = 0; i <= segs; i++) {
            pts.add(cx);
            pts.add(-10 + (yEnd + 10) * i / segs);
            cx += (fxRng.nextDouble() - 0.5) * 80;
        }
        double hitX = pts.get(segs * 2);
        int k = 3 + fxRng.nextInt(3);
        double fx = pts.get(k * 2), fy = pts.get(k * 2 + 1), dir = fxRng.nextBoolean() ? 1 : -1;
        Polyline core = new Polyline(), halo = new Polyline();
        Polyline fork = new Polyline(fx, fy, fx + dir * 30, fy + 45, fx + dir * 16, fy + 75, fx + dir * 50, fy + 115);
        core.getPoints().addAll(pts);
        halo.getPoints().addAll(pts);
        halo.setFill(null); core.setFill(null); fork.setFill(null);
        halo.setStroke(Color.web("#b080ff", 0.5)); halo.setStrokeWidth(10); halo.setBlendMode(BlendMode.ADD);
        core.setStroke(Color.web("#fbf8ff")); core.setStrokeWidth(3);
        fork.setStroke(Color.web("#e8dcff")); fork.setStrokeWidth(2);
        Circle skyGlow = StoryFx.glow(190, Color.web("#d0b8ff", 0.5));
        skyGlow.setTranslateX(x); skyGlow.setTranslateY(70);
        Circle hitGlow = StoryFx.glow(95, Color.web("#ffffff", 0.55));
        hitGlow.setTranslateX(hitX); hitGlow.setTranslateY(yEnd); hitGlow.setScaleY(0.35);
        bolt.getChildren().setAll(skyGlow, hitGlow, halo, fork, core);
        bolt.setOpacity(1);
        bolt.setVisible(true);
        strikeT = 0;
        rumbleT = 0.3 + fxRng.nextDouble() * 0.5;
        thunderRect.setOpacity(0.34);
        ring(hitX, yEnd, Color.web("#e8dcff"));
        popFx.burst(hitX, yEnd, 10, () -> StoryFx.sparkle(3, Color.web("#e0d0ff")), 170, 0.5, 320);
    }

    // ------------------------------------------------------------------------------------------------
    /** One square wood chip, so the dust the trunks kick up is made of pixels too. */
    private static Rectangle chip(Color c) { return new Rectangle(PS, PS, c); }

    /** The wall of living wood: seven pixel trunks standing one above the other, so together they close the whole street. */
    private void buildBarrier() {
        java.util.Random r = new java.util.Random(3);
        for (int i = 0; i < 7; i++) {
            double baseY = Math.min(Y2 - 2, 118 + i * 62);
            ImageView body = Sprites.view("world/wood_trunk_" + i + ".png");
            ImageView crown = Sprites.view("world/wood_crown_" + i + ".png");
            body.setVisible(false);
            crown.setVisible(false);
            Group box = new Group(body, crown);
            box.setLayoutX(Math.round(((i % 2) * 14 - 7 + r.nextDouble() * 8) / PS) * PS);
            box.setLayoutY(baseY);
            box.setViewOrder(-baseY);
            barrier.getChildren().add(box);
            trunks.add(new Trunk(box, body, crown, i * 0.16 + r.nextDouble() * 0.1));
        }
        barrier.setVisible(false);
        barrier.setViewOrder(-1500);
        growFx.setViewOrder(-3000);
        world.getChildren().addAll(barrier, growFx);
    }

    private void resetBarrier() {
        barrierT = 0;
        barrier.setVisible(false);
        seedGlow.setScaleX(1); seedGlow.setScaleY(1); seedGlow.setOpacity(1);
        for (Trunk t : trunks) {
            t.started = false;
            t.up = t.open = -1;
            t.body.setVisible(false);
            t.crown.setVisible(false);
        }
        growFx.getChildren().clear();
    }

    /** The seed sprouts: the trunks shoot up behind the Guardian while the knight charges at the wall and is stopped. */
    private void growBarrier(double tpf) {
        seedGlow.setScaleX(1 + barrierT * 1.2);
        seedGlow.setScaleY(seedGlow.getScaleX());
        for (Trunk t : trunks) {
            double u = Math.max(0, Math.min(1, (barrierT - 0.35 - t.delay) / 0.9));
            double g = u * u * (3 - 2 * u);
            if (g > 0 && !t.started) {
                t.started = true;
                growFx.burst(barX + t.box.getLayoutX(), t.box.getLayoutY(), 8, () -> chip(Color.web("#7a5232")), 90, 0.7, 260);
                app.sfx(Sfx.DUMP);
            }
            int up = Math.min(t.rows, ((int) (g * t.rows) + 1) / 2 * 2);                                    // the bark rises two pixel rows at a time
            int open = Math.min(t.crownRows, (int) Math.ceil(Math.max(0, g - 0.6) / 0.4 * t.crownRows / 4) * 4);   // then the crown opens from the bottom up
            if (up == t.up && open == t.open) continue;           // nothing changed this frame, so nothing to redraw
            t.up = up;
            t.open = open;
            double bw = t.body.getImage().getWidth(), bh = t.body.getImage().getHeight(), cw = t.crown.getImage().getWidth(), ch = t.crown.getImage().getHeight();
            t.body.setVisible(up > 0);
            if (up > 0) {
                t.body.setViewport(new Rectangle2D(0, bh - up * PS, bw, up * PS));
                t.body.setLayoutX(-bw / 2);
                t.body.setLayoutY(-up * PS);
            }
            t.crown.setVisible(open > 0);
            if (open > 0) {
                t.crown.setViewport(new Rectangle2D(0, ch - open * PS, cw, open * PS));
                t.crown.setLayoutX(-cw / 2);
                t.crown.setLayoutY((9 - up - open) * PS);
            }
        }
        growFx.update(tpf);
        double stop = barX - 34;
        if (cfx < stop) {
            cfx = Math.min(stop, cfx + 280 * tpf);
            chaserImg.setImage(Sprites.get(def.rage()));
        } else {
            chaserImg.setImage(Sprites.get(STUN));
        }
        placeNodes();
    }

    private void placeNodes() {
        playerImg.setLayoutX(pfx - PX / 2.0);
        playerImg.setLayoutY(pfy - SH + 10);
        playerImg.setViewOrder(-pfy);
        playerImg.setOpacity(invincible > 0 && ((int) (clock * 12) % 2 == 0) ? 0.35 : 1);
        double shake = mode == Mode.TELEGRAPH ? (((int) (clock * 40) % 2) - 0.5) * 6 : 0;
        double bob = mode == Mode.WAIT ? 0 : Math.sin(clock * 6) * 2;     // he stands perfectly still for the split second before the chase starts
        chaserImg.setLayoutX(cfx - chaserImg.getImage().getWidth() / 2 + shake);
        chaserImg.setLayoutY(cfy - chaserImg.getImage().getHeight() + KNIGHT_FEET + bob);   // his boots sit where the Guardian's do
        chaserImg.setViewOrder(-cfy);
        boolean angry = mode == Mode.TELEGRAPH || mode == Mode.LUNGE;
        chaserGlow.setLayoutX(cfx + shake);
        chaserGlow.setLayoutY(cfy - 40);
        chaserGlow.setViewOrder(-cfy + 1);
        chaserGlow.setScaleX(1 + 0.08 * Math.sin(clock * 5) + (angry ? 0.25 : 0));
        chaserGlow.setScaleY(chaserGlow.getScaleX());
        chaserGlow.setOpacity(mode == Mode.STUN ? 0.25 : angry ? 1.0 : 0.65 + 0.2 * Math.sin(clock * 3));
        for (Ghost g : ghosts) {
            g.node.setLayoutX(g.x - 24);
            g.node.setLayoutY(g.y - 52);
            g.node.setViewOrder(-g.y);
            double w = Math.sin(clock * 5 + g.phase);                       // the bags billow and rustle
            g.node.setScaleX(1 + 0.05 * w);
            g.node.setScaleY(1 - 0.04 * w);
            g.node.setRotate(Math.sin(clock * 2.2 + g.phase) * 5);
            g.node.setOpacity(0.88 + 0.1 * Math.sin(clock * 3 + g.phase * 2));
        }
        double target = Math.max(0, Math.min(WORLD_W - Ui.W, pfx - Ui.W * 0.38));
        cameraX += (target - cameraX) * 0.12;
        world.setTranslateX(-cameraX);
    }

    private void refreshHud() {
        for (int i = 0; i < MAX_HP; i++) {
            heartFull[i].setVisible(i < hp);
            heartEmpty[i].setVisible(i >= hp);
        }
        barFill.setWidth(BAR_W * Math.max(0, Math.min(1, pfx / EXIT_X)));
        chaserMark.setLayoutX(BAR_X + BAR_W * Math.max(0, Math.min(1, cfx / EXIT_X)) - 3);
    }
}
