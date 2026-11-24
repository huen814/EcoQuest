package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Boss 2, "the escape": a creature that cannot be beaten yet hunts the Guardian down a long, scrolling city street.
 * Reach the manhole at the far end. Cubes of crushed junk form a maze (it blocks the creature too), toothy ghosts patrol across
 * the street, the creature leaves stink clouds that slow you down, now and then it lunges (it warns you first), and it spits
 * lobbed gunk at the spot you are running towards (a dashed ring shows where it will land; the splash hurts and leaves a stink cloud).
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
    private static final int CW = 144, CH = 132;                 // the creature's sprite (wide, so its paper wings fit)
    private static final String STUN = "boss/experiment_stun.png";

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
            node = new Circle(x, y, 36, Color.web("#7aa040", 0.38));
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
    private final Circle chaserGlow = StoryFx.glow(95, Color.web("#d01818", 0.45));
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
    private boolean retryActive;
    private boolean up, down, left, right;

    private final BossDef def;

    public ChaseScreen(EcoQuestApp app, BossDef def, Runnable onWin) {
        super(app);
        this.onWin = onWin;
        this.def = def;

        // The street: the city scene repeated along the world, a little darker.
        for (int i = 0; i * Ui.W < WORLD_W; i++) {
            var part = StoryFx.scene("city");
            part.setLayoutX(i * Ui.W);
            world.getChildren().add(part);
        }
        Rectangle dim = new Rectangle(WORLD_W, Ui.H, Color.web("#0d1a0f", 0.35));
        dim.setViewOrder(-0.5);
        world.getChildren().add(dim);

        // Cubes of crushed junk: walls with doors, zigzags and pillars to dodge around, like the halls of a haunted house.
        // The creature runs straight at you and gets stuck on corners, so loop around the cubes to shake it off.
        for (int r = 0; r < MAZE.length; r++) {
            for (int c = 0; c < MAZE[r].length(); c++) {
                if (MAZE[r].charAt(c) == '#') addCube(c, r);
            }
        }

        for (int gx = 0; gx < GW; gx++) {                         // where the creature can stand
            for (int gy = 0; gy < GH; gy++) free[gx][gy] = !blocked(gx * G + G / 2.0, GY0 + gy * G + G / 2.0, 22, 8);
        }

        double[][] patrols = {{600, 300, 125, 1.4, 0}, {1080, 300, 130, 1.7, 1.6}, {1416, 300, 130, 1.5, 3.1},
                              {2088, 300, 125, 1.9, 4.4}, {2328, 300, 130, 1.6, 5.7}};
        for (double[] p : patrols) {
            Ghost g = new Ghost(p[0], p[1], p[2], p[3], p[4]);
            ghosts.add(g);
            world.getChildren().add(g.node);
        }

        Circle exitGlow = StoryFx.glow(70, Color.web("#9fe0cf", 0.7));
        exitGlow.setLayoutX(EXIT_X); exitGlow.setLayoutY(EXIT_Y);
        exitGlow.setViewOrder(-1);
        Circle manhole = new Circle(EXIT_X, EXIT_Y, 30, Color.web("#2a2a38"));
        manhole.setStroke(Color.web("#c9cdd4")); manhole.setStrokeWidth(3);
        manhole.setViewOrder(-1);
        Ui.Label exitLabel = new Ui.Label("EXIT", 12, Ui.GOOD);
        exitLabel.setLayoutX(EXIT_X - exitLabel.textWidth() / 2.0);
        exitLabel.setLayoutY(EXIT_Y - 70);
        exitLabel.setViewOrder(-2000);
        world.getChildren().addAll(exitGlow, manhole, exitLabel);

        playerImg = new ImageView(Wardrobe.sheet(app.save(), app.save().sibling(), false));
        playerImg.setViewport(new Rectangle2D(0, 0, PX, SH));
        chaserImg = Sprites.view(def.normal());
        chaserImg.setFitWidth(CW); chaserImg.setFitHeight(CH);
        world.getChildren().addAll(chaserGlow, chaserImg, playerImg);
        world.setMouseTransparent(true);
        root.getChildren().add(world);

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
        chaserImg.setImage(Sprites.get("boss/experiment.png"));
        say("RUN!", 1.4);
        refreshHud();
        root.requestFocus();
        Audio.playMusic("chase.wav", 0.55);
    }

    @Override
    public void exit() { Audio.stopMusic(); }

    private void retry() { enter(); }

    @Override
    public void onCancel() { app.quitBossFight(); }

    private void say(String text, double seconds) {
        shout.setText(text);
        shout.setLayoutX((Ui.W - shout.textWidth()) / 2.0);
        shoutT = seconds;
    }

    // ------------------------------------------------------------------------------------------------
    @Override
    public void update(double tpf) {
        clock += tpf;
        if (shoutT > 0) shoutT -= tpf;
        shout.setOpacity(Math.max(0, Math.min(1, shoutT * 2)));
        if (flash > 0) {
            flash -= tpf;
            flashRect.setOpacity(Math.max(0, Math.min(0.55, flash)));
        }
        updateRings(tpf);

        if (wonT >= 0) {                                         // diving into the manhole
            wonT -= tpf;
            flashRect.setFill(Color.WHITE);
            flashRect.setOpacity(Math.min(0.9, (1.4 - wonT) * 0.8));
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

        if (Math.hypot(pfx - EXIT_X, pfy - EXIT_Y) < 42) {      // made it
            wonT = 1.4;
            app.reportBossClear(def, clock, hp, MAX_HP);
            app.sfx(Sfx.MISSION);
            say("MADE IT!", 1.4);
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
        if (!blocked(nx, cfy, 22, 8)) cfx = nx;
        double ny = Math.max(Y1 + SH - 10, Math.min(Y2 - 10, cfy + my));
        if (!blocked(cfx, ny, 22, 8)) cfy = ny;
    }

    private void updateChaser(double tpf) {
        double progress = Math.min(1, pfx / EXIT_X);
        double dx = pfx - cfx, dy = pfy - cfy, dist = Math.max(1, Math.hypot(dx, dy));
        modeT -= tpf;
        switch (mode) {
            case WAIT -> {
                if (modeT <= 0) { mode = Mode.CHASE; say("RAAAH!", 1.2); app.sfx(Sfx.FULL); }
            }
            case CHASE -> {
                double speed = 158 + 14 * progress;                 // starts slower than you and ends a hair faster: loop round the cubes to keep your lead
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
                    chaserImg.setImage(Sprites.get("boss/experiment_rage.png"));
                    say("!!", 0.6);
                    app.sfx(Sfx.FULL);
                }
                spitCd -= tpf;
                if (spitCd <= 0 && dist > 220 && dist < 600) {   // spits ahead of you while you run
                    gunkSpit();
                    spitCd = 5.5 + (clock * 5 % 2);
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
                    chaserImg.setImage(Sprites.get("boss/experiment.png"));
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
            if (blocked(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, 22, 8)) return false;
        }
        return true;
    }

    private void endLunge() {
        mode = Mode.CHASE;
        lungeCd = 4.0 + (clock * 7 % 2);
        chaserImg.setImage(Sprites.get("boss/experiment.png"));
    }

    private void dropCloud() {
        Cloud c = new Cloud(cfx, cfy);
        clouds.add(c);
        world.getChildren().add(c.node);
    }

    /** Gunk Spit: lobs a glob at the spot you are running towards. A dashed ring marks the landing; the splash hurts and leaves a stink cloud. */
    private void gunkSpit() {
        double tx = Math.max(PX, Math.min(WORLD_W - PX, pfx + faceX * 70));
        double ty = Math.max(Y1 + SH, Math.min(Y2 - 10, pfy + faceY * 45));
        if (blocked(tx, ty, 14, 8)) { tx = pfx; ty = pfy; }        // never lands inside a cube
        Circle mark = new Circle(tx, ty, SPIT_SPLASH_R, Color.web("#8ab04a", 0.18));
        mark.setStroke(Color.web("#b9f06a")); mark.setStrokeWidth(2);
        mark.getStrokeDashArray().addAll(6.0, 5.0);
        mark.setViewOrder(-1);
        Circle glob = new Circle(11, new RadialGradient(0, 0, 0.35, 0.3, 0.85, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#d9b8ff")), new Stop(1, Color.web("#5a2a80"))));
        glob.setStroke(Color.web("#a8e04a")); glob.setStrokeWidth(2);
        glob.setViewOrder(-3000);
        world.getChildren().addAll(mark, glob);
        spits.add(new Spit(cfx, cfy - 70, tx, ty, glob, mark));
        say("SPIT!", 0.6);
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
                ring(sp.tx, sp.ty, Color.web("#9ad04a"));
                app.sfx(Sfx.DUMP);
                Cloud c = new Cloud(sp.tx, sp.ty);
                clouds.add(c);
                world.getChildren().add(c.node);
                if (invincible <= 0 && Math.hypot(pfx - sp.tx, pfy - sp.ty) < SPIT_SPLASH_R) hurt(sp.tx, sp.ty);
            }
        }
    }

    private void updateGhosts() {
        for (Ghost g : ghosts) g.y = g.baseY + Math.sin(clock * g.spd + g.phase) * g.amp;
    }

    private void popGhost(Ghost g) {
        world.getChildren().remove(g.node);
        ring(g.x, g.y - 25, Color.web("#e4def0"));
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
        if (mode != Mode.STUN && mode != Mode.WAIT && Math.hypot(pfx - cfx, pfy - cfy) < 36) {
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
    private void placeNodes() {
        playerImg.setLayoutX(pfx - PX / 2.0);
        playerImg.setLayoutY(pfy - SH + 10);
        playerImg.setViewOrder(-pfy);
        playerImg.setOpacity(invincible > 0 && ((int) (clock * 12) % 2 == 0) ? 0.35 : 1);
        double shake = mode == Mode.TELEGRAPH ? (((int) (clock * 40) % 2) - 0.5) * 6 : 0;
        chaserImg.setLayoutX(cfx - CW / 2.0 + shake);
        chaserImg.setLayoutY(cfy - CH + 10 + Math.sin(clock * 6) * 2);
        chaserImg.setViewOrder(-cfy);
        boolean angry = mode == Mode.TELEGRAPH || mode == Mode.LUNGE;
        chaserGlow.setLayoutX(cfx + shake);
        chaserGlow.setLayoutY(cfy - 62);
        chaserGlow.setViewOrder(-cfy + 1);
        chaserGlow.setScaleX(1 + 0.08 * Math.sin(clock * 5) + (angry ? 0.25 : 0));
        chaserGlow.setScaleY(chaserGlow.getScaleX());
        chaserGlow.setOpacity(mode == Mode.STUN ? 0.25 : angry ? 1.0 : 0.65 + 0.2 * Math.sin(clock * 3));
        for (Ghost g : ghosts) {
            g.node.setLayoutX(g.x - 24);
            g.node.setLayoutY(g.y - 52);
            g.node.setViewOrder(-g.y);
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
