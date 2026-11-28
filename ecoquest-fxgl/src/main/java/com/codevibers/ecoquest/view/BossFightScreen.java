package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Direction;

import javafx.geometry.Rectangle2D;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Soul Knight-style boss fight.
 *
 * Controls: WASD / arrows = move  mouse = aim  left-click or ENTER/Z = shoot
 *
 * Player shoots drops of "Spirit Bottle" liquid.
 * Muck Mound: Gunk Shot (aimed, fast sludge), Sludge Bomb (lobbed, splashes into a puddle and droplets), slime rain, puddles, sludge ring.
 * Queen Muck: Bottle Caps (ricochet), Rusted Rebar & Pipes (hover, then fly at you), Rolling Tires (lane rollers),
 * Leaky Car Batteries (lobbed, leave spark puddles) and Tied Trash Bags (lobbed, pop into a stink cloud).
 * Queen Bijou, second phase: when her blue bar empties she flashes white and comes back as the Tarnished Queen with a purple tint, a purple aura and a ring of
 * orbiting trash. Attacks: Banana Peel Combo, Soda Can Burst, Wrapper Tornado, Crushed Can volley and the Takeout Meteor.
 * Three player HP; six boss HP bars.
 * On boss death: victory cutscene.  On player death: retry prompt.
 */
public final class BossFightScreen extends Screen {

    // -- layout constants ------------------------------------------------------
    private static final int PX   = Sprites.PX;
    private static final int SH   = 72;
    // Use almost the full game window for the boss arena. The HUD remains in its
    // original position above the arena, while the player can roam across the
    // full width and most of the height instead of feeling boxed into one side.
    private static final double ARENA_X1 = 20, ARENA_X2 = Ui.W - 20;
    private static final double ARENA_Y1 = 96, ARENA_Y2 = Ui.H - 20;
    private static final double PLAYER_SPEED = 180;
    private static final double BULLET_SPEED  = 320;
    private static final double TRASH_SPEED_N = 180;
    private static final double TRASH_SPEED_R = 260;   // rage phase
    private static final double SHOOT_CD = 0.30;
    private static final int MAX_PLAYER_HP = 3;
    private static final double INVINCIBLE_TIME = 1.4;

    // -- boss state ------------------------------------------------------------
    private enum Phase { NORMAL, RAGE, DEAD }
    private Phase phase = Phase.NORMAL;

    // -- game objects ----------------------------------------------------------
    private final Pane arena = new Pane();

    private final ImageView playerImg;
    private final ImageView bossNormal;
    private final ImageView bossRage;
    private final BossDef def;           // which boss this is
    private final Runnable onWin;        // what happens after it is beaten
    private int maxBossHp;
    private final int bossW, bossH;

    private double playerX, playerY;
    private double bossCenterX, bossCenterY;
    private double aimX, aimY;
    private double bossShakeX, bossShakeY;

    private int playerHP = MAX_PLAYER_HP;
    private int bossHP;
    private double shootCooldown = 0;
    private double fightTime;               // seconds since the fight (re)started, for the best-clear record
    private boolean reported;
    private double bossFireTimer = 0;
    private double bossFireInterval = 2.0;
    private int bossPattern = 0;
    private double phaseFlash = 0;
    private double invincible = 0;
    private double bossDeathTimer = -1;
    private double retryAlpha = 0;
    private boolean retryActive = false;
    private PauseOverlay pause;
    private final Random rng = new Random(99);

    // Mouse fire is held locally; keyboard movement/fire are read from the shared input state.
    private boolean shooting;

    // Projectile lists
    private final List<Bullet> playerBullets = new ArrayList<>();
    private final List<TrashBullet> trashBullets = new ArrayList<>();
    private final List<HitFx> hitFx = new ArrayList<>();
    private final List<Puff> puffs = new ArrayList<>();         // trails, sparks, droplets, ground warnings (never hurt)
    private final List<Delayed> delayed = new ArrayList<>();    // things that happen a moment from now
    private final List<Lob> lobs = new ArrayList<>();           // things flying through the air towards a spot
    private final List<Rod> rods = new ArrayList<>();           // Queen Muck's hovering rebar and pipes
    private final Ui.Label attackLabel;
    private double attackT;

    // -- HUD elements ---------------------------------------------------------
    private final ImageView[] heartFull  = new ImageView[MAX_PLAYER_HP];
    private final ImageView[] heartEmpty = new ImageView[MAX_PLAYER_HP];
    private Rectangle[] bossBar;
    private final Group barGroup = new Group();
    private final Group barFrame;
    private final Ui.Label bossLabel;
    private final Ui.Label laughLabel;
    private double laughTimer;
    private final Ui.Label retryLabel;
    private final Ui.Label retryPrompt;
    private final Rectangle flashRect = new Rectangle(Ui.W, Ui.H, Color.WHITE);

    // -- Queen Bijou, second phase ---------------------------------------------
    private static final int PHASE2_HP = 80;
    private static final double AWAKEN_TIME = 2.6, ORBIT_R = 95, SLIDE_TIME = 0.9;
    private static final Color AURA = Color.web("#b455ff");
    private boolean unhinged;
    private double awakenT, whiteFlash;

    // -- Queen Bijou, the pause before her second phase: silence, a trembling sad queen, then BOOM and a title card --
    private static final double WINDUP_TIME = 2.0, TITLE_TIME = 3.2, BARS_H = 56;
    private double windupT, titleT, sparkT, quakeT;
    private boolean windupThud, windupMusic;
    private final Rectangle dimRect = new Rectangle(Ui.W, Ui.H, Color.BLACK);
    private final Rectangle barTop = new Rectangle(Ui.W, BARS_H, Color.BLACK), barBottom = new Rectangle(Ui.W, BARS_H, Color.BLACK);
    private Ui.Label titleCard, titleSub;
    private Rectangle arenaDim, arenaBorder;                 // the arena's shade and border: light magenta in her second phase
    private Color dimBase, borderBase;
    private double orbitSpin, orbitR, orbitTarget = ORBIT_R, orbitSpeed = 1.7;
    private int hiddenType = -1;
    private double slowT, stinkT, slideT, slideDx, slideDy, stinkBubbleT;
    private Group meteorBall;
    private Circle meteorReticle;
    private double meteorT, meteorX, meteorY;
    private final List<Orbiter> orbiters = new ArrayList<>();
    private final List<Peel> peels = new ArrayList<>();
    private final Circle auraGlow = new Circle(0, 0, 135, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#c060ff", 0.55)), new Stop(0.6, Color.web("#8a3fe0", 0.28)), new Stop(1, Color.web("#6a1fc0", 0))));
    private final Circle stinkNode = new Circle(0, 0, 34, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#c060ff", 0.55)), new Stop(1, Color.web("#7a2fc0", 0))));
    private final Rectangle tint = new Rectangle(Ui.W, Ui.H, Color.web("#7a22d0", 0.24));
    private final ColorAdjust heartPurple = new ColorAdjust(-0.32, 0.1, -0.4, 0);
    private final DropShadow bossAura = new DropShadow(BlurType.GAUSSIAN, Color.web("#c060ff"), 28, 0.55, 0, 0);

    // -- help for the Guardian: Spirit Bottle overcharge and Mother Nature's blessing ----
    private static final double OVERCHARGE_FILL = 20, OVERCHARGE_TIME = 4, OVERCHARGE_CD = 0.21;
    private final Rectangle chargeBack = new Rectangle(100, 6, Color.web("#000000", 0.45)), chargeFill = new Rectangle(0, 6, Color.web("#7fe8d0"));
    private double charge, chargeT, blessingT, seedLife, seedGrow;
    private int blessingsLeft;
    private Group seed;

    // -- inner data types ------------------------------------------------------
    private static class Bullet {
        double x, y, vx, vy;
        final Circle node = new Circle(5, Color.web("#d8f6ff"));
        Bullet(double x, double y, double vx, double vy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            node.setStroke(Color.web("#3aa5ae")); node.setStrokeWidth(1.5);
        }
    }

    private static class TrashBullet {
        double x, y, vx, vy;
        double life = Double.MAX_VALUE;   // seconds left (puddles, clouds and fuses run out)
        double warn = 0;                  // seconds before it can hurt (puddles are telegraphed)
        double hitR = 18;                 // how close the Guardian has to be to get hit
        int bounces = 0;                  // wall ricochets left (only when ricochet is on)
        boolean ricochet, persist, harmless, flicker;   // persist: stays after hurting; harmless: just sitting there (a fuse)
        double spin, breathe, trailT;     // degrees per second; pulse size; trail timer
        Color trail;                      // leaves a fading trail of this colour
        Consumer<TrashBullet> onExpire;   // what happens when its life runs out
        final Node node;
        TrashBullet(double x, double y, double vx, double vy, Color col) { this(x, y, vx, vy, blob(7, col)); }
        TrashBullet(double x, double y, double vx, double vy, Node node) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.node = node;
            node.setMouseTransparent(true);
        }
    }

    /** A glossy ball, lit from the top left, so every projectile reads as a blob and not a flat dot. */
    private static Circle blob(double r, Color col) {
        Circle c = new Circle(r, new RadialGradient(0, 0, 0.35, 0.3, 0.85, true, CycleMethod.NO_CYCLE,
                new Stop(0, col.brighter().brighter()), new Stop(1, col.darker())));
        c.setStroke(col.darker().darker()); c.setStrokeWidth(1.5);
        return c;
    }

    private static class Puff {
        final Node node; final double vx, vy, max; double life; final boolean shrink;
        Puff(Node node, double vx, double vy, double life, boolean shrink) {
            this.node = node; this.vx = vx; this.vy = vy; this.life = life; this.max = life; this.shrink = shrink;
        }
    }

    private static class Delayed {
        double t; final Runnable run;
        Delayed(double t, Runnable run) { this.t = t; this.run = run; }
    }

    private static class Lob {
        final double sx, sy, tx, ty, dur, arc; double t;
        final Node node, shadow; final Runnable onLand;
        Lob(double sx, double sy, double tx, double ty, double dur, double arc, Node node, Node shadow, Runnable onLand) {
            this.sx = sx; this.sy = sy; this.tx = tx; this.ty = ty; this.dur = dur; this.arc = arc;
            this.node = node; this.shadow = shadow; this.onLand = onLand;
        }
    }

    /** A rusty rod or pipe wrapped in a magenta telekinetic glow. */
    private static class Rod {
        double x, y, ang, t, vx, vy; final double hover; final int slot, count; boolean flying;
        final Group node = new Group(); final DropShadow glow = new DropShadow(BlurType.GAUSSIAN, Color.web("#ff2fd0"), 10, 0.6, 0, 0);
        Rod(int slot, int count, double hover, boolean pipe) {
            this.slot = slot; this.count = count; this.hover = hover;
            double len = pipe ? 62 : 72, h = pipe ? 10 : 5;
            Rectangle body = new Rectangle(-len / 2, -h / 2, len, h);
            body.setArcWidth(4); body.setArcHeight(4);
            body.setFill(Color.web(pipe ? "#b5501f" : "#c9622a")); body.setStroke(Color.web("#7a2f12")); body.setStrokeWidth(1.2);
            node.getChildren().add(body);
            for (int i = 0; i < 3; i++) {                                  // rust bands (pipe) or ribs (rebar)
                Rectangle band = new Rectangle(-len / 2 + 10 + i * (len - 22) / 2.0, -h / 2 - (pipe ? 1.5 : 0), pipe ? 4 : 2, h + (pipe ? 3 : 0));
                band.setFill(Color.web(pipe ? "#7a2f12" : "#8e3d16"));
                node.getChildren().add(band);
            }
            node.setEffect(glow);
            node.setMouseTransparent(true);
        }
    }

    private static class HitFx {
        double x, y, life = 0.35;
        final Circle node;
        HitFx(double x, double y, Color col) {
            this.x = x; this.y = y;
            node = new Circle(10, Color.TRANSPARENT);
            node.setCenterX(x); node.setCenterY(y);     // was left at (0,0): every ring flashed in the top-left corner
            node.setStroke(col); node.setStrokeWidth(2.5);
        }
    }

    private static class Orbiter {
        final int type; final double offset; final Node node;
        Orbiter(int type, double offset, Node node) { this.type = type; this.offset = offset; this.node = node; }
    }

    private static class Peel {
        final double x, y; double life; final Node node;
        Peel(double x, double y, double life, Node node) { this.x = x; this.y = y; this.life = life; this.node = node; }
    }

    // -------------------------------------------------------------------------
    public BossFightScreen(EcoQuestApp app, BossDef def, Runnable onWin) {
        super(app);
        this.def = def;
        this.onWin = onWin;
        maxBossHp = def.hp();
        bossW = def.width();
        bossH = def.height();
        bossBar = new Rectangle[maxBossHp];

        // Arena background
        boolean queenArena = def.kind() == BossDef.Kind.QUEEN;                 // Queen Muck fights in a purple hall, the others in the green arena
        Rectangle bg = new Rectangle(Ui.W, Ui.H, Color.web(queenArena ? "#160a26" : "#0d1a0f"));
        root.getChildren().add(bg);
        root.getChildren().add(def.backdrop().startsWith("scene:")        // the boss's arena, dimmed
                ? StoryFx.scene(def.backdrop().substring("scene:".length()))
                : Sprites.view(def.backdrop()));
        Rectangle dim = new Rectangle(Ui.W, Ui.H, Color.web(queenArena ? "#160a26" : "#0d1a0f", 0.62));
        root.getChildren().add(dim);
        arenaDim = dim; dimBase = (Color) dim.getFill();

        // Floor grid pattern
        for (int gx = (int) ARENA_X1 + 32; gx < ARENA_X2; gx += 32) {
            Rectangle line = new Rectangle(1, ARENA_Y2 - ARENA_Y1, Color.web("#ffffff", 0.04));
            line.setLayoutX(gx); line.setLayoutY(ARENA_Y1);
            root.getChildren().add(line);
        }
        for (int gy = (int) ARENA_Y1 + 32; gy < ARENA_Y2; gy += 32) {
            Rectangle hline = new Rectangle(ARENA_X2 - ARENA_X1, 1, Color.web("#ffffff", 0.04));
            hline.setLayoutX(ARENA_X1); hline.setLayoutY(gy);
            root.getChildren().add(hline);
        }

        // Arena border
        Rectangle border = new Rectangle(ARENA_X2 - ARENA_X1, ARENA_Y2 - ARENA_Y1);
        border.setLayoutX(ARENA_X1); border.setLayoutY(ARENA_Y1);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.web(queenArena ? "#a070e0" : "#4a9a5a", 0.6)); border.setStrokeWidth(2);
        root.getChildren().add(border);
        arenaBorder = border; borderBase = (Color) border.getStroke();

        // Sprites
        playerImg  = new ImageView(Wardrobe.sheet(app.save(), app.save().sibling(), false));
        bossNormal = Sprites.view(def.normal());
        bossRage   = Sprites.view(def.rage());

        playerImg.setViewport(new Rectangle2D(0, 0, PX, SH));
        bossNormal.setFitWidth(bossW); bossNormal.setFitHeight(bossH);
        bossRage.setFitWidth(bossW);   bossRage.setFitHeight(bossH);

        arena.getChildren().addAll(bossNormal, bossRage, playerImg);
        arena.setMouseTransparent(true);
        root.getChildren().add(arena);
        tint.setVisible(false); tint.setMouseTransparent(true);
        root.getChildren().add(tint);

        // Her second phase entrance: dim, letterbox bars and a title card (all under the white flash)
        dimRect.setOpacity(0); dimRect.setMouseTransparent(true);
        barTop.setLayoutY(-BARS_H); barBottom.setLayoutY(Ui.H);
        barTop.setMouseTransparent(true); barBottom.setMouseTransparent(true);
        titleCard = new Ui.Label("TARNISHED QUEEN BIJOU", 24, Color.web("#e9b8ff"));
        titleCard.shadow(Color.web("#4a1a7a"));
        titleCard.setLayoutX((Ui.W - titleCard.textWidth()) / 2.0); titleCard.setLayoutY(Ui.H - BARS_H - 96);
        titleSub = new Ui.Label("SHE IS NOT DONE YET", 10, Color.web("#c9a0f0"));
        titleSub.shadow(Color.web("#4a1a7a"));
        titleSub.setLayoutX((Ui.W - titleSub.textWidth()) / 2.0); titleSub.setLayoutY(Ui.H - BARS_H - 54);
        for (Node entrance : new Node[]{titleCard, titleSub}) { entrance.setOpacity(0); entrance.setMouseTransparent(true); }
        root.getChildren().addAll(dimRect, barTop, barBottom, titleCard, titleSub);

        // Flash overlay
        flashRect.setOpacity(0); flashRect.setMouseTransparent(true);
        root.getChildren().add(flashRect);

        // -- HUD --------------------------------------------------------------
        // Player hearts
        for (int i = 0; i < MAX_PLAYER_HP; i++) {
            heartFull[i]  = Sprites.view("weapon/heart.png");
            heartEmpty[i] = Sprites.view("weapon/heart_empty.png");
            for (ImageView iv : new ImageView[]{heartFull[i], heartEmpty[i]}) {
                iv.setFitWidth(28); iv.setFitHeight(28); iv.setPreserveRatio(true);
                iv.setLayoutX(14 + i * 34); iv.setLayoutY(14);
                root.getChildren().add(iv);
            }
        }

        // Boss HP bar
        bossLabel = new Ui.Label(def.label(), 10, Ui.WHITE);
        bossLabel.setLayoutX((Ui.W - bossLabel.textWidth()) / 2.0);
        bossLabel.setLayoutY(14);
        root.getChildren().add(bossLabel);

        laughLabel = new Ui.Label(def.shout(), 14, Ui.BAD);
        laughLabel.setLayoutX((Ui.W - laughLabel.textWidth()) / 2.0);
        laughLabel.setLayoutY(52);
        laughLabel.setOpacity(0);
        root.getChildren().add(laughLabel);

        attackLabel = new Ui.Label("", 11, Ui.ACCENT);                   // names the attack that is coming
        attackLabel.setLayoutY(78);
        attackLabel.setOpacity(0);
        root.getChildren().add(attackLabel);

        root.getChildren().add(barGroup);
        buildBar(maxBossHp);
        barFrame = buildBarFrame();
        barFrame.setVisible(false);
        root.getChildren().add(barFrame);
        root.getChildren().remove(bossLabel);
        root.getChildren().add(root.getChildren().indexOf(barFrame) + 1, bossLabel);
        for (Rectangle r : new Rectangle[]{chargeBack, chargeFill}) { r.setLayoutX(14); r.setLayoutY(48); root.getChildren().add(r); }
        auraGlow.setMouseTransparent(true); stinkNode.setMouseTransparent(true);

        // Retry overlay
        retryLabel  = new Ui.Label("GAME OVER", 24, Ui.WHITE);
        retryLabel.setLayoutX((Ui.W - retryLabel.textWidth()) / 2.0);
        retryLabel.setLayoutY(Ui.H / 2.0 - 50);
        retryPrompt = new Ui.Label("ENTER  Try Again     X  Quit to Menu", 11, Ui.ACCENT);
        retryPrompt.setLayoutX((Ui.W - retryPrompt.textWidth()) / 2.0);
        retryPrompt.setLayoutY(Ui.H / 2.0);
        retryLabel.setOpacity(0); retryPrompt.setOpacity(0);
        root.getChildren().addAll(retryLabel, retryPrompt);
        pause = new PauseOverlay(app, () -> { }, app::quitBossFight);
        root.getChildren().add(pause);

        // Set up mouse aiming and firing; keyboard state comes from the shared InputState.
        root.setFocusTraversable(true);
        // Mouse aiming is independent from WASD movement; the arena artwork and layout stay unchanged.
        root.setOnMouseMoved(e -> updateAim(e));
        root.setOnMouseDragged(e -> updateAim(e));
        root.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                updateAim(e);
                shooting = true;
                e.consume();
            }
        });
        root.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                shooting = false;
                e.consume();
            }
        });
        // Keyboard state is read through InputState so movement works alongside the app-wide input handler.
    }

    // -------------------------------------------------------------------------
    @Override
    public void enter() {
        app.input().clear();
        resetUnhinged();
        phase = Phase.NORMAL;
        playerHP = MAX_PLAYER_HP;
        bossHP   = maxBossHp;
        bossFireTimer = 0;
        bossPattern   = 0;
        bossDeathTimer = -1;
        fightTime = 0;
        reported = false;
        retryActive = false;
        retryAlpha = 0;
        retryLabel.setOpacity(0); retryPrompt.setOpacity(0);
        playerBullets.clear(); trashBullets.clear(); hitFx.clear();
        arena.getChildren().removeIf(n -> n != bossNormal && n != bossRage && n != playerImg);
        lobs.clear(); rods.clear(); puffs.clear(); delayed.clear(); attackT = 0;

        playerX = ARENA_X1 + (ARENA_X2 - ARENA_X1) * 0.12;
        playerY = (ARENA_Y1 + ARENA_Y2) / 2.0 - SH / 2.0;
        bossCenterX = ARENA_X2 - bossW / 2.0 - 20;
        bossCenterY = (ARENA_Y1 + ARENA_Y2) / 2.0;
        aimX = bossCenterX;
        aimY = bossCenterY;

        bossNormal.setVisible(true);
        bossRage.setVisible(false);
        shooting = false;
        refreshHud();
        root.requestFocus();
        showLaugh();
        String track = switch (def.kind()) {
            case QUEEN -> "boss_queen.wav";
            case MOUND -> "boss_mound.wav";
            default -> null;
        };
        if (track != null) Audio.playMusic(track, 0.5);
        if (def.kind() == BossDef.Kind.QUEEN) Audio.preloadLoop("boss_queen_p2_intro.wav", "boss_queen_p2.wav");     // so her second theme is ready the instant it is needed
    }

    @Override
    public void exit() { Audio.stopMusic(); }

    /** The boss shouts: floating text plus its sound (Queen Muck's laugh, a gross groan for the others). */
    private void showLaugh() {
        laughTimer = unhinged ? 2.2 : 1.6;
        if (unhinged) {
            laughLabel.setText("HO HO HO HO HO!");
            laughLabel.setLayoutX((Ui.W - laughLabel.textWidth()) / 2.0);
        }
        if (def.kind() == BossDef.Kind.QUEEN) app.laugh();
        else app.sfx(Sfx.FULL);
    }

    // -------------------------------------------------------------------------
    @Override
    public void update(double tpf) {
        if (pause.isOpen()) { pause.update(tpf); return; }
        if (laughTimer > 0) laughTimer -= tpf;
        if (attackT > 0) attackT -= tpf;
        attackLabel.setOpacity(Math.max(0, Math.min(1, attackT * 2)));
        if (def.laugh() != null) bossNormal.setImage(Sprites.get(windupT > 0 ? "boss/queen_bijou_sad.png" : laughTimer > 0 ? def.laugh() : def.normal()));
        if (unhinged) { bossNormal.setVisible(laughTimer > 0); bossRage.setVisible(laughTimer <= 0); }
        laughLabel.setOpacity(Math.max(0, Math.min(1, laughTimer * 2)));
        updateEntrance(tpf);                            // keeps running on the game-over screen so nothing is left half-drawn
        if (phase == Phase.DEAD) { updateDeathSequence(tpf); return; }
        if (retryActive) { blinkRetry(tpf); return; }

        fightTime += tpf;

        // -- player movement --
        double dx = 0, dy = 0, speed = PLAYER_SPEED;
        Direction movement = app.input().current();
        if (slideT > 0) { slideT -= tpf; dx = slideDx; dy = slideDy; speed *= 0.7 * (0.25 + 0.75 * slideT / SLIDE_TIME); }
        else if (movement != null) { dx = movement.dc; dy = movement.dr; }
        if (slowT > 0) { slowT -= tpf; if (slideT <= 0) speed *= 0.45; }
        if (stinkT > 0) stinkT -= tpf;
        if (dx != 0 || dy != 0) {
            double length = Math.hypot(dx, dy);
            dx = dx / length * speed * tpf;
            dy = dy / length * speed * tpf;
        }
        playerX = clamp(playerX + dx, ARENA_X1,  ARENA_X2 - PX);
        playerY = clamp(playerY + dy, ARENA_Y1,  ARENA_Y2 - SH);

        // -- player shoot --
        shootCooldown -= tpf;
        boolean autoAim = app.save().autoAim;           // auto aim: shots go at the boss; otherwise they go at the cursor
        if ((shooting || app.input().confirmHeld()) && shootCooldown <= 0) {   // click / ENTER / Z fires either way
            if (autoAim) { aimX = bossCenterX; aimY = bossCenterY; }
            fireBullet();
            shootCooldown = chargeT > 0 ? OVERCHARGE_CD : SHOOT_CD;
        }

        // -- boss drift --
        double bossSpeed = phase == Phase.RAGE ? 100 : 70;
        boolean mound = def.kind() == BossDef.Kind.MOUND;
        double targetX = (ARENA_X1 + ARENA_X2) / 2.0 + Math.sin(System.nanoTime() * 1e-9 * (mound ? 0.45 : 0.6)) * (mound ? 190 : 100);
        double targetY = mound ? ARENA_Y2 - bossH / 2.0 - 12
                               : (ARENA_Y1 + ARENA_Y2) / 2.0 + Math.cos(System.nanoTime() * 1e-9 * 0.4) * 30;
        bossCenterX += (targetX - bossCenterX) * bossSpeed * tpf * 0.025;
        bossCenterY += (targetY - bossCenterY) * bossSpeed * tpf * 0.025;
        bossShakeX   = phase == Phase.RAGE ? (rng.nextDouble() - 0.5) * 4 : 0;
        bossShakeY   = phase == Phase.RAGE ? (rng.nextDouble() - 0.5) * 4 : 0;
        if (unhinged && laughTimer > 0) bossShakeY += Math.sin(laughTimer * 36) * 6;

        // -- boss fire --
        if (windupT > 0) updateWindup(tpf);
        else if (awakenT > 0) updateAwaken(tpf);
        else {
            bossFireTimer -= tpf;
            if (bossFireTimer <= 0) {
                double wait = def.kind() == BossDef.Kind.MOUND ? fireMound() : unhinged ? fireUnhinged() : fireQueen();
                bossPattern = (bossPattern + 1) % 60;
                bossFireInterval = phase == Phase.RAGE ? def.fireRage() : def.fireNormal();
                bossFireTimer = bossFireInterval * wait;     // big attacks leave a longer breather
            }
        }
        runDelayed(tpf);
        updateLobs(tpf);
        updateRods(tpf);
        updatePeels(tpf);
        updateMeteor(tpf);
        updateOrbit(tpf);

        // -- move bullets --
        double trashSpeed = phase == Phase.RAGE ? TRASH_SPEED_R : TRASH_SPEED_N;
        moveProjectiles(tpf);
        updatePuffs(tpf);

        // -- hit detection --
        checkCollisions();

        // -- invincibility frames --
        if (invincible > 0) {
            invincible -= tpf;
            playerImg.setOpacity(Math.sin(invincible * 25) > 0 ? 1.0 : 0.3);
        } else {
            playerImg.setOpacity(1.0);
        }

        // -- flash decay --
        if (phaseFlash > 0) {
            phaseFlash = Math.max(0, phaseFlash - tpf * 3);
            flashRect.setOpacity(phaseFlash * 0.6);
        }
        if (whiteFlash > 0) {
            whiteFlash = Math.max(0, whiteFlash - tpf * 0.9);
            flashRect.setOpacity(whiteFlash);
        }

        // -- position sprites --
        playerImg.setLayoutX(playerX);
        playerImg.setLayoutY(playerY);
        bossNormal.setLayoutX(bossCenterX - bossW / 2.0 + bossShakeX);
        bossNormal.setLayoutY(bossCenterY - bossH / 2.0 + bossShakeY);
        bossRage.setLayoutX(bossCenterX - bossW / 2.0 + bossShakeX);
        bossRage.setLayoutY(bossCenterY - bossH / 2.0 + bossShakeY);
        if (unhinged) updateAura(tpf);
        updateStatus(tpf);
        updateHelp(tpf);

        // -- hit fx --
        Iterator<HitFx> fi = hitFx.iterator();
        while (fi.hasNext()) {
            HitFx f = fi.next();
            f.life -= tpf;
            if (f.life <= 0) { arena.getChildren().remove(f.node); fi.remove(); }
            else { double r = (0.35 - f.life) * 50; f.node.setRadius(r); f.node.setOpacity(f.life / 0.35); }
        }

        refreshHud();
    }

    // -------------------------------------------------------------------------
    private void updateAim(MouseEvent event) {
        Point2D point = root.sceneToLocal(event.getSceneX(), event.getSceneY());
        aimX = clamp(point.getX(), ARENA_X1, ARENA_X2);
        aimY = clamp(point.getY(), ARENA_Y1, ARENA_Y2);
    }

    private void fireBullet() {
        double cx = playerX + PX / 2.0;
        double cy = playerY + SH / 2.0;
        double tx = aimX, ty = aimY;
        double len = Math.hypot(tx - cx, ty - cy);
        if (len < 1) return;
        Bullet b = new Bullet(cx, cy, (tx - cx) / len * BULLET_SPEED, (ty - cy) / len * BULLET_SPEED);
        if (chargeT > 0) { b.node.setFill(Color.web("#fff0a8")); b.node.setStroke(Color.web("#e8b020")); }
        playerBullets.add(b);
        arena.getChildren().add(b.node);
    }

    // =========================================================================
    //  small helpers shared by every attack
    // =========================================================================

    /** Names the attack that is coming, in the middle of the screen. */
    private void callout(String text) {
        attackLabel.setText(text);
        attackLabel.setLayoutX((Ui.W - attackLabel.textWidth()) / 2.0);
        attackT = 1.1;
    }

    private void later(double seconds, Runnable r) { delayed.add(new Delayed(seconds, r)); }

    private void runDelayed(double tpf) {
        for (Delayed d : new ArrayList<>(delayed)) {
            d.t -= tpf;
            if (d.t <= 0) { delayed.remove(d); d.run.run(); }
        }
    }

    /** Puts a node on the floor of the arena, under the boss and the Guardian. */
    private void addGround(Node n) { arena.getChildren().add(0, n); }

    private void addBullet(TrashBullet tb) {
        tb.node.setTranslateX(tb.x); tb.node.setTranslateY(tb.y);
        trashBullets.add(tb);
        arena.getChildren().add(tb.node);
    }

    private Puff puff(Node n, double x, double y, double vx, double vy, double life, boolean shrink, boolean ground) {
        n.setTranslateX(x); n.setTranslateY(y);
        n.setMouseTransparent(true);
        if (ground) addGround(n); else arena.getChildren().add(n);
        Puff p = new Puff(n, vx, vy, life, shrink);
        puffs.add(p);
        return p;
    }

    /** A little burst of droplets / sparks. */
    private void spark(double x, double y, Color col, int n, double speed) {
        for (int i = 0; i < n; i++) {
            double a = rng.nextDouble() * Math.PI * 2, sp = speed * (0.4 + rng.nextDouble() * 0.8);
            puff(blob(2 + rng.nextDouble() * 2.5, col), x, y, Math.cos(a) * sp, Math.sin(a) * sp, 0.35 + rng.nextDouble() * 0.3, true, false);
        }
    }

    private void ringFx(double x, double y, Color col) {
        HitFx fx = new HitFx(x, y, col);
        hitFx.add(fx); arena.getChildren().add(fx.node);
    }

    /** Throws something through the air: it flies in an arc, a shadow marks where it will come down, then onLand runs. */
    private void lob(double sx, double sy, double tx, double ty, double dur, double arc, Node projectile, Color shadowCol, Runnable onLand) {
        Ellipse shadow = new Ellipse(0, 0, 18, 9);
        shadow.setFill(shadowCol); shadow.setStroke(shadowCol.brighter()); shadow.setStrokeWidth(1.5);
        shadow.setTranslateX(tx); shadow.setTranslateY(ty);
        shadow.setMouseTransparent(true); projectile.setMouseTransparent(true);
        addGround(shadow);
        projectile.setTranslateX(sx); projectile.setTranslateY(sy);
        arena.getChildren().add(projectile);
        lobs.add(new Lob(sx, sy, tx, ty, dur, arc, projectile, shadow, onLand));
    }

    private void updateLobs(double tpf) {
        for (Lob l : new ArrayList<>(lobs)) {
            l.t += tpf / l.dur;
            double p = Math.min(1, l.t);
            double h = 4 * l.arc * p * (1 - p);
            l.node.setTranslateX(l.sx + (l.tx - l.sx) * p);
            l.node.setTranslateY(l.sy + (l.ty - l.sy) * p - h);
            l.node.setRotate(l.node.getRotate() + 420 * tpf);
            double g = 0.6 + 0.9 * p;
            l.shadow.setScaleX(g); l.shadow.setScaleY(g);
            if (l.t >= 1) {
                lobs.remove(l);
                arena.getChildren().removeAll(l.node, l.shadow);
                l.onLand.run();
            }
        }
    }

    private void updatePuffs(double tpf) {
        for (Iterator<Puff> it = puffs.iterator(); it.hasNext(); ) {
            Puff p = it.next();
            p.life -= tpf;
            if (p.life <= 0) { arena.getChildren().remove(p.node); it.remove(); continue; }
            p.node.setTranslateX(p.node.getTranslateX() + p.vx * tpf);
            p.node.setTranslateY(p.node.getTranslateY() + p.vy * tpf);
            double f = p.life / p.max;
            p.node.setOpacity(f);
            if (p.shrink) { p.node.setScaleX(0.4 + 0.6 * f); p.node.setScaleY(0.4 + 0.6 * f); }
        }
    }

    /** A dashed line from the boss that shows where a fast shot is about to go. */
    private void telegraphAim(double ang, double seconds, Color col) {
        Line ln = new Line(0, 0, Math.cos(ang) * 520, Math.sin(ang) * 520);
        ln.setStroke(col); ln.setStrokeWidth(3);
        ln.getStrokeDashArray().addAll(10.0, 8.0);
        puff(ln, bossCenterX, bossCenterY, 0, 0, seconds, false, true);
    }

    private double playerCx() { return playerX + PX / 2.0; }
    private double playerCy() { return playerY + SH / 2.0; }

    // =========================================================================
    //  MUCK MOUND: Gunk Shot, Sludge Bomb, slime rain, puddles, sludge ring
    // =========================================================================

    private double fireMound() {
        boolean rageOn = phase == Phase.RAGE;
        double speed = rageOn ? TRASH_SPEED_R : TRASH_SPEED_N;
        Color slime = Color.web("#8ab04a");
        double pcx = playerCx(), pcy = playerCy();
        double wait = 1.0;
        switch (bossPattern % 5) {
            case 0 -> {                                              // GUNK SHOT: a big, fast glob, aimed with a warning line
                callout("GUNK SHOT!");
                double base = Math.atan2(pcy - bossCenterY, pcx - bossCenterX);
                int n = rageOn ? 3 : 1;
                for (int i = 0; i < n; i++) {
                    double ang = base + (i - (n - 1) / 2.0) * 0.3;
                    telegraphAim(ang, 0.5, Color.web("#b070e0", 0.6));
                    later(0.5, () -> gunkShot(ang, speed * 1.9));
                }
                wait = 1.15;
            }
            case 1 -> {                                              // SLUDGE BOMB: lobbed, splashes into a puddle plus droplets
                callout("SLUDGE BOMB!");
                int n = rageOn ? 3 : 2;
                for (int i = 0; i < n; i++) {
                    double jx = i == 0 ? 0 : (rng.nextDouble() - 0.5) * 220, jy = i == 0 ? 0 : (rng.nextDouble() - 0.5) * 140;
                    double tx = clamp(pcx + jx, ARENA_X1 + 24, ARENA_X2 - 24), ty = clamp(pcy + jy, ARENA_Y1 + 24, ARENA_Y2 - 24);
                    later(i * 0.4, () -> sludgeBomb(tx, ty));
                }
                wait = 1.5;
            }
            case 2 -> {                                              // slime falling from the ceiling
                callout("SLIME RAIN!");
                int n = rageOn ? 9 : 6;
                for (int i = 0; i < n; i++) {
                    double x = ARENA_X1 + 30 + rng.nextDouble() * (ARENA_X2 - ARENA_X1 - 60);
                    addBullet(new TrashBullet(x, ARENA_Y1 + 4, 0, speed * 0.8, slime));
                }
            }
            case 3 -> {                                              // puddles that appear under you, with a warning
                int n = rageOn ? 3 : 2;
                for (int i = 0; i < n; i++) {
                    double x = clamp(pcx + (i == 0 ? 0 : (rng.nextDouble() - 0.5) * 220), ARENA_X1 + 20, ARENA_X2 - 20);
                    double y = clamp(pcy + (i == 0 ? 0 : (rng.nextDouble() - 0.5) * 140), ARENA_Y1 + 20, ARENA_Y2 - 20);
                    addPuddle(x, y, Color.web("#7aa040"));
                }
            }
            default -> {                                             // a slow ring of sludge
                int n = rageOn ? 12 : 8;
                for (int i = 0; i < n; i++) {
                    double ang = i * 2 * Math.PI / n + bossFireTimer;
                    addBullet(new TrashBullet(bossCenterX, bossCenterY, Math.cos(ang) * speed * 0.7, Math.sin(ang) * speed * 0.7, slime));
                }
            }
        }
        return wait;
    }

    /** Gunk Shot: one big purple glob with a toxic-green rim that leaves a sludge trail. */
    private void gunkShot(double ang, double speed) {
        Circle body = blob(13, Color.web("#8a3fc0"));
        body.setStroke(Color.web("#a8e04a")); body.setStrokeWidth(2.5);
        Group g = new Group(body, new Circle(-4, -4, 3.5, Color.web("#d9b8ff", 0.85)), new Circle(5, 4, 2.2, Color.web("#a8e04a", 0.85)));
        TrashBullet tb = new TrashBullet(bossCenterX, bossCenterY, Math.cos(ang) * speed, Math.sin(ang) * speed, g);
        tb.hitR = 17;
        tb.trail = Color.web("#7a3fa0");
        addBullet(tb);
        app.sfx(Sfx.FULL);
    }

    /** Sludge Bomb: lobbed high, lands where you stood, then bursts into a puddle and a ring of flying droplets. */
    private void sludgeBomb(double tx, double ty) {
        Circle body = blob(12, Color.web("#7a46a8"));
        body.setStroke(Color.web("#a8e04a")); body.setStrokeWidth(2);
        Group bomb = new Group(body, new Circle(-4, -4, 3.5, Color.web("#d9b8ff", 0.8)), new Circle(5, 3, 2.2, Color.web("#a8e04a", 0.85)));
        app.sfx(Sfx.FULL);
        lob(bossCenterX, bossCenterY - bossH * 0.2, tx, ty, 1.0, 120, bomb, Color.web("#5a2a7a", 0.35), () -> sludgeSplash(tx, ty));
    }

    private void sludgeSplash(double x, double y) {
        app.sfx(Sfx.DUMP);
        ringFx(x, y, Color.web("#b070e0"));
        addPuddle(x, y, Color.web("#7a4a98"));
        int n = phase == Phase.RAGE ? 9 : 6;
        double off = rng.nextDouble() * Math.PI;
        for (int i = 0; i < n; i++) {
            double a = off + i * 2 * Math.PI / n;
            TrashBullet tb = new TrashBullet(x, y, Math.cos(a) * 150, Math.sin(a) * 150, blob(6, Color.web("#7a4a98")));
            tb.hitR = 12; tb.life = 0.9;                              // droplets fly out a short way, then splat
            tb.trail = Color.web("#8ab04a");
            addBullet(tb);
        }
        spark(x, y, Color.web("#a070d0"), 8, 120);
    }

    /** A puddle: faint for a moment (safe), then it hurts, and it dries up after a few seconds. */
    private void addPuddle(double x, double y, Color col) {
        Circle c = new Circle(22, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, col.darker()), new Stop(1, Color.color(col.getRed(), col.getGreen(), col.getBlue(), 0.55))));
        c.setStroke(col.darker().darker()); c.setStrokeWidth(1.5);
        TrashBullet tb = new TrashBullet(x, y, 0, 0, c);
        tb.hitR = 26; tb.life = 3.2; tb.warn = 0.9;
        addBullet(tb);
    }

    // =========================================================================
    //  QUEEN MUCK: bottle caps, rebar & pipes, tires, batteries, trash bags
    // =========================================================================

    private double fireQueen() {
        boolean rageOn = phase == Phase.RAGE;
        double pcx = playerCx(), pcy = playerCy();
        double wait = 1.0;
        switch (bossPattern % 5) {
            case 0 -> { callout("BOTTLE CAPS!");    bottleCaps(rageOn ? 5 : 3, rageOn ? 330 : 270); wait = 1.2; }
            case 1 -> { callout("REBAR & PIPES!");  rodVolley(rageOn ? 5 : 3);                       wait = 1.7; }
            case 2 -> { callout("TIRES!");          rollTires(rageOn ? 3 : 2, rageOn ? 300 : 235);   wait = 1.5; }
            case 3 -> { callout("CAR BATTERIES!");  batteries(rageOn ? 3 : 2, pcx, pcy);             wait = 1.6; }
            default -> { callout("TRASH BAGS!");    trashBags(rageOn ? 3 : 2, pcx, pcy);             wait = 1.7; }
        }
        return wait;
    }

    // ---- Jagged Bottle Caps: silver discs, crimson rims, hot-pink trails; 3-shot spreads that ricochet up to 3 times ----
    private void bottleCaps(int n, double speed) {
        double base = Math.atan2(playerCy() - bossCenterY, playerCx() - bossCenterX);
        for (int i = 0; i < n; i++) {
            double ang = base + (i - (n - 1) / 2.0) * 0.3;
            TrashBullet tb = new TrashBullet(bossCenterX, bossCenterY, Math.cos(ang) * speed, Math.sin(ang) * speed, capShape());
            tb.hitR = 11; tb.ricochet = true; tb.bounces = 3; tb.spin = 720;
            tb.trail = Color.web("#ff3fa0");
            tb.onExpire = t -> {                                     // shatters after its last bounce
                spark(t.x, t.y, Color.web("#d8dce8"), 7, 150);
                spark(t.x, t.y, Color.web("#c0153f"), 4, 110);
            };
            addBullet(tb);
        }
        app.sfx(Sfx.FULL);
    }

    private Polygon capShape() {
        Polygon p = new Polygon();
        for (int i = 0; i < 24; i++) {                               // a crown of jagged teeth
            double a = i * Math.PI / 12, r = (i % 2 == 0) ? 10 : 7.6;
            p.getPoints().addAll(Math.cos(a) * r, Math.sin(a) * r);
        }
        p.setFill(new RadialGradient(0, 0, 0.35, 0.3, 0.9, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f4f6fc")), new Stop(1, Color.web("#9097a8"))));
        p.setStroke(Color.web("#c0153f")); p.setStrokeWidth(2.2);
        return p;
    }

    /** Wall bounce for ricochet shots: reflect, spark, and shatter once the bounces are used up. */
    private void bounce(TrashBullet tb) {
        boolean hit = false;
        if (tb.x < ARENA_X1)      { tb.x = ARENA_X1 + (ARENA_X1 - tb.x); tb.vx = Math.abs(tb.vx);  hit = true; }
        else if (tb.x > ARENA_X2) { tb.x = ARENA_X2 - (tb.x - ARENA_X2); tb.vx = -Math.abs(tb.vx); hit = true; }
        if (tb.y < ARENA_Y1)      { tb.y = ARENA_Y1 + (ARENA_Y1 - tb.y); tb.vy = Math.abs(tb.vy);  hit = true; }
        else if (tb.y > ARENA_Y2) { tb.y = ARENA_Y2 - (tb.y - ARENA_Y2); tb.vy = -Math.abs(tb.vy); hit = true; }
        if (hit) {
            spark(tb.x, tb.y, Color.web("#ff8fd0"), 4, 90);
            if (--tb.bounces < 0) tb.life = 0;
        }
    }

    // ---- Rusted Rebar & Pipes: rust-orange rods in a magenta glow hover behind her, aiming, then launch straight at you ----
    private void rodVolley(int n) {
        for (int i = 0; i < n; i++) {
            Rod r = new Rod(i, n, 0.9 + i * 0.2, i % 2 == 0);
            r.x = bossCenterX; r.y = bossCenterY;
            rods.add(r);
            arena.getChildren().add(r.node);
        }
        app.sfx(Sfx.FULL);
    }

    private void updateRods(double tpf) {
        double pcx = playerCx(), pcy = playerCy();
        for (Rod r : new ArrayList<>(rods)) {
            r.t += tpf;
            if (!r.flying) {
                double toP = Math.atan2(pcy - bossCenterY, pcx - bossCenterX), back = toP + Math.PI;
                double off = (r.slot - (r.count - 1) / 2.0) * 34;
                double ax = bossCenterX + Math.cos(back) * bossW * 0.42 + Math.cos(toP + Math.PI / 2) * off;
                double ay = bossCenterY + Math.sin(back) * bossW * 0.42 + Math.sin(toP + Math.PI / 2) * off + Math.sin(r.t * 5 + r.slot) * 3;
                r.x += (ax - r.x) * Math.min(1, tpf * 8);
                r.y += (ay - r.y) * Math.min(1, tpf * 8);
                r.ang = Math.atan2(pcy - r.y, pcx - r.x);            // it keeps aiming at you while it hovers
                r.glow.setRadius(8 + 14 * Math.min(1, r.t / r.hover));
                if (r.t >= r.hover) {                                // ...then goes straight at where you are right now
                    double sp = phase == Phase.RAGE ? 520 : 430;
                    r.vx = Math.cos(r.ang) * sp; r.vy = Math.sin(r.ang) * sp;
                    r.flying = true;
                    app.sfx(Sfx.FULL);
                }
            } else {
                r.x += r.vx * tpf; r.y += r.vy * tpf;
                if (r.x < ARENA_X1 - 90 || r.x > ARENA_X2 + 90 || r.y < ARENA_Y1 - 90 || r.y > ARENA_Y2 + 90) {
                    rods.remove(r); arena.getChildren().remove(r.node);
                    continue;
                }
            }
            r.node.setTranslateX(r.x); r.node.setTranslateY(r.y);
            r.node.setRotate(Math.toDegrees(r.ang));
        }
    }

    private static double segDist(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay, len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
        return Math.hypot(px - (ax + dx * t), py - (ay + dy * t));
    }

    // ---- Rolling Tires: matte-black tires with ash-gray treads roll along lanes; a grey strip warns you first ----
    private void rollTires(int n, double speed) {
        List<Double> lanes = new ArrayList<>();
        lanes.add(clamp(playerCy(), ARENA_Y1 + 26, ARENA_Y2 - 26));
        for (int guard = 0; lanes.size() < n && guard < 40; guard++) {
            double y = ARENA_Y1 + 26 + rng.nextDouble() * (ARENA_Y2 - ARENA_Y1 - 52);
            boolean ok = true;
            for (double l : lanes) if (Math.abs(l - y) < 80) ok = false;
            if (ok) lanes.add(y);
        }
        for (double y : lanes) {
            Rectangle warn = new Rectangle(ARENA_X2 - ARENA_X1, 40, Color.web("#8a8a96", 0.28));
            puff(warn, ARENA_X1, y - 20, 0, 0, 0.85, false, true);
            later(0.85, () -> spawnTire(y, speed));
        }
    }

    private void spawnTire(double y, double speed) {
        Circle tire = new Circle(19, Color.web("#1a1a1e"));
        tire.setStroke(Color.web("#8e8e98")); tire.setStrokeWidth(4);
        tire.getStrokeDashArray().addAll(6.0, 5.0);                  // ash-gray tread blocks
        Circle rim = new Circle(10, Color.web("#2e2e36"));
        rim.setStroke(Color.web("#55555f")); rim.setStrokeWidth(1.5);
        TrashBullet tb = new TrashBullet(ARENA_X2 - 6, y, -speed, 0, new Group(tire, rim, new Circle(3, Color.web("#7a7a86"))));
        tb.hitR = 21; tb.spin = -speed * 2.4;
        addBullet(tb);
        app.sfx(Sfx.DUMP);
    }

    // ---- Leaky Car Batteries: charcoal-and-yellow, tossed in high arcs, crack open into electric-cyan spark puddles ----
    private void batteries(int n, double pcx, double pcy) {
        for (int i = 0; i < n; i++) {
            double jx = i == 0 ? 0 : (rng.nextDouble() - 0.5) * 240, jy = i == 0 ? 0 : (rng.nextDouble() - 0.5) * 150;
            double tx = clamp(pcx + jx, ARENA_X1 + 26, ARENA_X2 - 26), ty = clamp(pcy + jy, ARENA_Y1 + 26, ARENA_Y2 - 26);
            later(i * 0.45, () -> {
                app.sfx(Sfx.FULL);
                lob(bossCenterX, bossCenterY - 30, tx, ty, 1.1, 150, batteryNode(), Color.web("#22d8ff", 0.35), () -> sparkPuddle(tx, ty));
            });
        }
    }

    private Node batteryNode() {
        Rectangle body = new Rectangle(-10, -13, 20, 26);
        body.setArcWidth(3); body.setArcHeight(3);
        body.setFill(Color.web("#2c2e34")); body.setStroke(Color.web("#121316")); body.setStrokeWidth(1.5);
        Rectangle stripe = new Rectangle(-10, -3, 20, 7);
        stripe.setFill(Color.web("#f2c230"));
        Rectangle t1 = new Rectangle(-7, -17, 5, 4), t2 = new Rectangle(2, -17, 5, 4);
        t1.setFill(Color.web("#9a9aa6")); t2.setFill(Color.web("#f2c230"));
        return new Group(body, stripe, t1, t2, new Circle(6, 9, 2.2, Color.web("#44e8ff")));   // a leaking drip
    }

    private void sparkPuddle(double x, double y) {
        app.sfx(Sfx.DUMP);
        Circle c = new Circle(26, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#d8fbff", 0.7)), new Stop(1, Color.web("#1fd2ff", 0.3))));
        c.setStroke(Color.web("#aaf6ff")); c.setStrokeWidth(2);
        TrashBullet tb = new TrashBullet(x, y, 0, 0, c);
        tb.hitR = 26; tb.life = 3.6; tb.persist = true; tb.flicker = true;
        addBullet(tb);
        ringFx(x, y, Color.web("#7aeaff"));
        spark(x, y, Color.web("#7aeaff"), 10, 170);
    }

    // ---- Tied Trash Bags: bloated olive-black bags land, sit on a short fuse, then pop into a lingering neon-green stink cloud ----
    private void trashBags(int n, double pcx, double pcy) {
        for (int i = 0; i < n; i++) {
            double jx = i == 0 ? 0 : (rng.nextDouble() - 0.5) * 260, jy = i == 0 ? 0 : (rng.nextDouble() - 0.5) * 160;
            double tx = clamp(pcx + jx, ARENA_X1 + 40, ARENA_X2 - 40), ty = clamp(pcy + jy, ARENA_Y1 + 40, ARENA_Y2 - 40);
            later(i * 0.45, () -> {
                app.sfx(Sfx.FULL);
                lob(bossCenterX, bossCenterY - 30, tx, ty, 1.2, 130, bagNode(), Color.web("#6a7a30", 0.35), () -> {
                    TrashBullet bag = new TrashBullet(tx, ty, 0, 0, bagNode());     // sits there with a flashing fuse
                    bag.harmless = true; bag.life = 1.2; bag.flicker = true;
                    bag.onExpire = t -> stinkCloud(t.x, t.y);
                    addBullet(bag);
                });
            });
        }
    }

    private Node bagNode() {
        Ellipse bag = new Ellipse(0, 0, 15, 17);
        bag.setFill(new RadialGradient(0, 0, 0.35, 0.3, 0.9, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#58623a")), new Stop(1, Color.web("#1c2014"))));
        bag.setStroke(Color.web("#10130b")); bag.setStrokeWidth(1.5);
        Circle knot = new Circle(0, -17, 4.5, Color.web("#2a3018"));
        knot.setStroke(Color.web("#10130b"));
        Ellipse shine = new Ellipse(-5, -6, 3.5, 5);
        shine.setFill(Color.web("#8a9660", 0.5));
        return new Group(bag, knot, shine);
    }

    private void stinkCloud(double x, double y) {
        app.sfx(Sfx.DUMP);
        Circle c = new Circle(66, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8dff4a", 0.6)), new Stop(0.7, Color.web("#4fe02a", 0.35)), new Stop(1, Color.web("#2aa818", 0.0))));
        TrashBullet tb = new TrashBullet(x, y, 0, 0, c);
        tb.hitR = 50; tb.life = 4.8; tb.persist = true; tb.breathe = 1;
        addBullet(tb);
        ringFx(x, y, Color.web("#9dff5a"));
        for (int i = 0; i < 8; i++) {                                // bubbles rising out of the cloud
            puff(blob(3 + rng.nextDouble() * 3, Color.web("#9dff5a")), x + (rng.nextDouble() - 0.5) * 80, y + (rng.nextDouble() - 0.5) * 50,
                    0, -28 - rng.nextDouble() * 20, 1.0 + rng.nextDouble() * 0.8, true, false);
        }
    }


    // =========================================================================
    //  QUEEN BIJOU, SECOND PHASE: aura, orbiting trash, peels, soda, wrappers, meteor
    // =========================================================================

    private void buildBar(int count) {
        maxBossHp = count;
        bossBar = new Rectangle[count];
        barGroup.getChildren().clear();
        double total = 360, step = total / count, x0 = (Ui.W - total) / 2.0;
        for (int i = 0; i < count; i++) {
            Rectangle seg = new Rectangle(Math.max(2, step - 2), 12);
            seg.setArcWidth(4); seg.setArcHeight(4);
            seg.setLayoutX(x0 + i * step); seg.setLayoutY(30);
            bossBar[i] = seg;
            barGroup.getChildren().add(seg);
        }
    }

    /** A glowing magenta border around the bar. */
    private static Group buildBarFrame() {
        double x = (Ui.W - 360) / 2.0 - 5, y = 25, w = 370, h = 22;
        Rectangle edge = new Rectangle(x, y, w, h);
        edge.setArcWidth(6); edge.setArcHeight(6);
        edge.setFill(Color.TRANSPARENT); edge.setStroke(Color.web("#ff66ff")); edge.setStrokeWidth(2.5);
        edge.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.web("#ff00ff"), 10, 0.7, 0, 0));
        Group g = new Group(edge);
        return g;
    }

    private void resetUnhinged() {
        unhinged = false; awakenT = 0; whiteFlash = 0;
        windupT = 0; titleT = 0; quakeT = 0; windupThud = false; windupMusic = false;
        dimRect.setOpacity(0); barTop.setLayoutY(-BARS_H); barBottom.setLayoutY(Ui.H);
        titleCard.setOpacity(0); titleSub.setOpacity(0);
        arena.setTranslateX(0); arena.setTranslateY(0);
        if (arenaDim != null) { arenaDim.setFill(dimBase); arenaBorder.setStroke(borderBase); }
        tint.setFill(Color.web("#7a22d0", 0.24));
        orbiters.clear(); peels.clear();
        orbitSpin = 0; orbitR = 0; orbitTarget = ORBIT_R; orbitSpeed = 1.7; hiddenType = -1;
        slowT = 0; stinkT = 0; slideT = 0; stinkBubbleT = 0;
        meteorBall = null; meteorReticle = null;
        buildBar(def.hp());
        barFrame.setVisible(false);
        tint.setVisible(false);
        bossNormal.setEffect(null); bossRage.setEffect(null);
        bossLabel.setText(def.label());
        bossLabel.setFill(Ui.WHITE);
        bossLabel.setLayoutX((Ui.W - bossLabel.textWidth()) / 2.0);
        laughLabel.setText(def.shout());
        laughLabel.setLayoutX((Ui.W - laughLabel.textWidth()) / 2.0);
        playerImg.setRotate(0);
        charge = 0; chargeT = 0; blessingT = 5; blessingsLeft = 1; seed = null;
        boolean queen = def.kind() == BossDef.Kind.QUEEN;
        chargeBack.setVisible(queen); chargeFill.setVisible(queen);
    }

    private void clearAttacks() {
        for (TrashBullet tb : trashBullets) arena.getChildren().remove(tb.node);
        for (Rod r : rods) arena.getChildren().remove(r.node);
        for (Lob l : lobs) arena.getChildren().removeAll(l.node, l.shadow);
        for (Puff p : puffs) arena.getChildren().remove(p.node);
        trashBullets.clear(); rods.clear(); lobs.clear(); puffs.clear(); delayed.clear();
    }

    /**
     * Her blue bar is empty, but she does not go down yet: every attack is cleared, the music cuts to silence, the screen dims
     * with letterbox bars, and a sad, trembling Queen is drawn in by purple sparks (updateWindup). Then boom().
     */
    private void beginSecondPhase() {
        bossHP = 0;
        windupT = WINDUP_TIME; windupThud = false; windupMusic = false; sparkT = 0.4;
        clearAttacks();
        Audio.stopMusic();
        app.sfx(Sfx.DUMP);
    }

    private void updateWindup(double tpf) {
        windupT -= tpf;
        double t = WINDUP_TIME - windupT, k = clamp(t / WINDUP_TIME, 0, 1);
        bossNormal.setVisible(true); bossRage.setVisible(false);
        double tremble = 1.5 + 9 * k * k;                               // she shakes harder and harder
        bossShakeX = (rng.nextDouble() - 0.5) * tremble;
        bossShakeY = (rng.nextDouble() - 0.5) * tremble;
        dimRect.setOpacity(0.6 * clamp(t / 0.5, 0, 1));
        double slide = clamp(t / 0.6, 0, 1);                            // letterbox bars close in
        barTop.setLayoutY(-BARS_H + BARS_H * slide);
        barBottom.setLayoutY(Ui.H - BARS_H * slide);
        if (!windupThud && t >= 1.0) { windupThud = true; app.sfx(Sfx.DUMP); }   // a second heartbeat
        sparkT -= tpf;
        if (sparkT <= 0) {                                              // purple sparks fly in toward her
            sparkT = Math.max(0.015, 0.05 - 0.04 * k);
            double ang = rng.nextDouble() * Math.PI * 2, r = 150 + rng.nextDouble() * 90, life = 0.45;
            double sx = bossCenterX + Math.cos(ang) * r, sy = bossCenterY + Math.sin(ang) * r * 0.6;
            puff(blob(2.5 + rng.nextDouble() * 2, rng.nextBoolean() ? AURA : Color.web("#e08aff")), sx, sy,
                    (bossCenterX - sx) / life, (bossCenterY - sy) / life, life, true, false);
        }
        if (!windupMusic && windupT <= 0.12) {                          // the player takes a moment to start: lead it in a hair early
            windupMusic = true;
            Audio.playMusicLoop("boss_queen_p2_intro.wav", "boss_queen_p2.wav", 0.5);     // fades in quietly, then only the loop repeats
        }
        if (windupT <= 0) { windupT = 0; boom(); }
    }

    /** The entrance: white flash and thunder, the second theme slams in, the screen shakes, the title card appears. */
    private void boom() {
        unhinged = true;
        blessingsLeft = 1; blessingT = 3;
        awakenT = AWAKEN_TIME;
        titleT = TITLE_TIME; quakeT = 0.9;
        dimRect.setOpacity(0);
        arenaDim.setFill(Color.web("#ff9cff", 0.45));               // the hall turns light magenta
        arenaBorder.setStroke(Color.web("#ff66ff", 0.85));
        tint.setFill(Color.web("#ff66ff", 0.2));
        buildBar(PHASE2_HP);
        barFrame.setVisible(true);
        bossLabel.setText("TARNISHED QUEEN BIJOU");
        tint.setVisible(true);
        bossNormal.setEffect(bossAura); bossRage.setEffect(bossAura);
        bossLabel.setFill(Color.web("#e9b8ff"));
        bossLabel.setLayoutX((Ui.W - bossLabel.textWidth()) / 2.0);
        phaseFlash = 0; whiteFlash = 1;
        flashRect.setFill(Color.WHITE); flashRect.setOpacity(1);
        app.sfx(Sfx.MISSION);
        app.sfx(Sfx.THUNDER);
        Audio.playMusicLoop("boss_queen_p2_intro.wav", "boss_queen_p2.wav", 0.5);      // no-op if updateWindup already started it
        orbitR = 0; orbitTarget = ORBIT_R; orbitSpeed = 1.7; hiddenType = -1;
        addGround(auraGlow);
        for (int i = 0; i < 12; i++) {
            int type = i % 3;
            Node n = type == 0 ? canNode() : type == 1 ? wrapperNode() : coreNode();
            n.setMouseTransparent(true);
            orbiters.add(new Orbiter(type, i * Math.PI * 2 / 12, n));
            arena.getChildren().add(n);
        }
        later(0.5, this::hoho);                         // her laugh comes in just after the thunder
    }

    /** Screen shake after the boom, then the title card and the letterbox bars sliding away. */
    private void updateEntrance(double tpf) {
        if (quakeT > 0) {
            quakeT = Math.max(0, quakeT - tpf);
            double m = quakeT * 18;
            arena.setTranslateX((rng.nextDouble() - 0.5) * m);
            arena.setTranslateY((rng.nextDouble() - 0.5) * m);
            if (quakeT == 0) { arena.setTranslateX(0); arena.setTranslateY(0); }
        }
        if (titleT <= 0) return;
        titleT = Math.max(0, titleT - tpf);
        double t = TITLE_TIME - titleT, out = clamp(titleT / 0.6, 0, 1);
        double in = clamp((t - 0.25) / 0.35, 0, 1);                      // pops in as the white clears
        titleCard.setOpacity(Math.min(in, out));
        double scale = 1.25 - 0.25 * in;
        titleCard.setScaleX(scale); titleCard.setScaleY(scale);
        titleSub.setOpacity(clamp((t - 0.7) / 0.4, 0, 1) * out);
        barTop.setLayoutY(-BARS_H + BARS_H * out);
        barBottom.setLayoutY(Ui.H - BARS_H * out);
        if (titleT == 0) { titleCard.setOpacity(0); titleSub.setOpacity(0); }
    }

    private void updateAwaken(double tpf) {
        awakenT -= tpf;
        double t = AWAKEN_TIME - awakenT;
        bossHP = awakenT <= 0 ? maxBossHp : (int) (maxBossHp * clamp((t - 0.7) / 1.3, 0, 1));
        if (awakenT <= 0) { awakenT = 0; bossFireTimer = 0.8; }
    }

    private void updateAura(double tpf) {
        double pulse = 1 + 0.08 * Math.sin(System.nanoTime() * 1e-9 * 5);
        auraGlow.setTranslateX(bossCenterX); auraGlow.setTranslateY(bossCenterY);
        auraGlow.setScaleX(pulse); auraGlow.setScaleY(pulse);
        wispT -= tpf;
        if (wispT <= 0) {
            wispT = 0.06;
            Ellipse wisp = new Ellipse(0, 0, 7, 13);
            wisp.setFill(Color.web(rng.nextBoolean() ? "#b455ff" : "#e08aff", 0.6));
            puff(wisp, bossCenterX + (rng.nextDouble() - 0.5) * bossW * 0.8, bossCenterY + rng.nextDouble() * bossH * 0.4,
                    (rng.nextDouble() - 0.5) * 16, -60 - rng.nextDouble() * 30, 0.8, true, true);
        }
    }
    private double wispT;

    private void updateOrbit(double tpf) {
        if (!unhinged) return;
        orbitSpin += tpf * orbitSpeed;
        orbitR += (orbitTarget - orbitR) * Math.min(1, tpf * 4);
        double pull = 1 - orbitR / ORBIT_R;
        double cx = bossCenterX, cy = bossCenterY + 10 + (-140) * pull;
        for (Orbiter o : orbiters) {
            double a = orbitSpin + o.offset;
            o.node.setTranslateX(cx + Math.cos(a) * orbitR);
            o.node.setTranslateY(cy + Math.sin(a) * orbitR * 0.5);
            o.node.setRotate(o.node.getRotate() + 120 * tpf);
            o.node.setVisible(orbitR > 14 && o.type != hiddenType);
        }
    }

    private void updateStatus(double tpf) {
        double lean = slideT > 0 ? Math.sin((SLIDE_TIME - slideT) / SLIDE_TIME * Math.PI) : 0;
        playerImg.setRotate(-22 * lean);
        playerImg.setTranslateY(-7 * lean);
        if (stinkT > 0) {
            if (stinkNode.getParent() == null) arena.getChildren().add(stinkNode);
            stinkNode.setTranslateX(playerCx()); stinkNode.setTranslateY(playerCy());
            double pulse = 1 + 0.08 * Math.sin(stinkT * 9);
            stinkNode.setScaleX(pulse); stinkNode.setScaleY(pulse);
            stinkNode.setOpacity(Math.min(1, stinkT));
            stinkBubbleT -= tpf;
            if (stinkBubbleT <= 0) {
                stinkBubbleT = 0.14;
                puff(blob(2.5 + rng.nextDouble() * 2, AURA), playerCx() + (rng.nextDouble() - 0.5) * 40, playerCy() + (rng.nextDouble() - 0.5) * 30, 0, -34, 0.8, true, false);
            }
        } else if (stinkNode.getParent() != null) arena.getChildren().remove(stinkNode);
    }

    private void hoho() {
        showLaugh();
        for (int i = 0; i < 5; i++) { int k = i; later(i * 0.24, () -> floatHo(k)); }
    }

    private void floatHo(int i) {
        Ui.Label l = new Ui.Label("HO!", 14 + rng.nextInt(7), Color.web("#e9b8ff"));
        l.shadow(Color.web("#4a1a7a"));
        puff(l, bossCenterX - 90 + i * 40 + rng.nextDouble() * 16, bossCenterY - bossH * 0.5 - 6, (rng.nextDouble() - 0.5) * 20, -45, 1.1, false, false);
    }

    private double fireUnhinged() {
        double wait = 1.0;
        switch (bossPattern % 5) {
            case 0 -> { callout("BANANA PEEL COMBO!"); bananaPeels(3);    wait = 1.5; }
            case 1 -> { callout("SODA CAN BURST!");    sodaBurst();       wait = 1.6; }
            case 2 -> { callout("WRAPPER TORNADO!");   wrapperTornado();  wait = 3.8; }
            case 3 -> { callout("CRUSHED CANS!");      canVolley();       wait = 1.6; }
            default -> { callout("TAKEOUT METEOR!");   takeoutMeteor();   wait = 2.7; }
        }
        if (bossPattern % 5 != 0) hoho();
        return wait;
    }

    // ---- Banana Peel Combo: peels stay on the floor; slip on one and you slide, then are slowed in a stink cloud (hearts turn dark purple); the peel itself does no damage, but she laughs and fires 8 crushed cans at you ----
    private void bananaPeels(int n) {
        for (int i = 0; i < n; i++) {
            double tx = clamp(playerCx() + (i == 0 ? 0 : (rng.nextDouble() - 0.5) * 300), ARENA_X1 + 24, ARENA_X2 - 24);
            double ty = clamp(playerCy() + (i == 0 ? 0 : (rng.nextDouble() - 0.5) * 190), ARENA_Y1 + 24, ARENA_Y2 - 24);
            later(i * 0.3, () -> {
                app.sfx(Sfx.FULL);
                lob(bossCenterX, bossCenterY - 20, tx, ty, 0.9, 110, bananaPeelNode(), Color.web("#f5d92a", 0.35), () -> dropPeel(tx, ty));
            });
        }
    }

    private void dropPeel(double x, double y) {
        Node n = bananaPeelNode();
        n.setTranslateX(x); n.setTranslateY(y); n.setMouseTransparent(true);
        addGround(n);
        peels.add(new Peel(x, y, 7.0, n));
        if (peels.size() > 7) arena.getChildren().remove(peels.remove(0).node);
        ringFx(x, y, Color.web("#f5d92a"));
    }

    private void updatePeels(double tpf) {
        for (Iterator<Peel> it = peels.iterator(); it.hasNext(); ) {
            Peel p = it.next();
            p.life -= tpf;
            if (p.life < 1) p.node.setOpacity(Math.max(0, p.life));
            boolean stepped = slideT <= 0 && slowT <= 0 && Math.hypot(p.x - playerCx(), p.y - (playerY + SH - 8)) < 20;
            if (p.life <= 0 || stepped) {
                arena.getChildren().remove(p.node);
                it.remove();
                if (stepped) slip();
            }
        }
    }

    private void slip() {
        Direction d = app.input().current();
        double dx = d != null ? d.dc : 1, dy = d != null ? d.dr : 0, len = Math.hypot(dx, dy);
        if (len == 0) { dx = 1; dy = 0; len = 1; }
        slideDx = dx / len; slideDy = dy / len;
        slideT = SLIDE_TIME; slowT = 2.6; stinkT = 2.6;
        app.sfx(Sfx.DUMP);
        spark(playerCx(), playerCy(), AURA, 8, 110);
        callout("SLOWED!");
        hoho();
        for (int i = 0; i < 8; i++) later(0.35 + i * 0.14, () -> fan(1, 0, 330));
    }

    // ---- Soda Can Burst: she crushes a glowing purple can, spraying a cone of fizzy liquid and aluminum shrapnel ----
    private void sodaBurst() {
        double base = Math.atan2(playerCy() - bossCenterY, playerCx() - bossCenterX), half = 0.45;
        Polygon cone = new Polygon(0, 0, Math.cos(base - half) * 520, Math.sin(base - half) * 520, Math.cos(base + half) * 520, Math.sin(base + half) * 520);
        cone.setFill(Color.web("#b455ff", 0.2)); cone.setStroke(Color.web("#e0a0ff", 0.6)); cone.setStrokeWidth(2);
        puff(cone, bossCenterX, bossCenterY, 0, 0, 1.1, false, true);
        Node can = sodaCanNode();
        can.setTranslateX(bossCenterX); can.setTranslateY(bossCenterY - 40);
        can.setMouseTransparent(true);
        arena.getChildren().add(can);
        app.sfx(Sfx.FULL);
        later(1.1, () -> {
            arena.getChildren().remove(can);
            app.sfx(Sfx.DUMP);
            double x = bossCenterX, y = bossCenterY - 20;
            spark(x, y, AURA, 10, 160);
            puff(sprite("boss/soda_burst.png", 140), x, y, 0, 0, 0.4, false, false);
            for (int i = 0; i < 15; i++) {
                double ang = base + (rng.nextDouble() - 0.5) * 2 * half;
                boolean metal = i % 3 == 0;
                double sp = metal ? 280 + rng.nextDouble() * 100 : 160 + rng.nextDouble() * 120;
                Node n = metal ? shrapnelNode() : blob(5 + rng.nextDouble() * 3, AURA);
                TrashBullet tb = new TrashBullet(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp, n);
                tb.hitR = 9;
                if (metal) { tb.spin = 720; tb.node.setRotate(Math.toDegrees(ang)); } else tb.life = 1.5;
                addBullet(tb);
            }
        });
    }

    // ---- Fast-Food Wrapper Tornado: her orbiting wrappers are flung out in widening spiral arms ----
    private void wrapperTornado() {
        hiddenType = 1;
        orbitSpeed = 6;
        double step = 0.12, dir = rng.nextBoolean() ? 1 : -1, start = rng.nextDouble() * Math.PI * 2;
        int shots = 18;
        for (int i = 0; i < shots; i++) {
            int k = i;
            later(0.7 + i * step, () -> {
                for (int arm = 0; arm < 2; arm++) {
                    double ang = start + dir * k * step * 2.6 + arm * Math.PI, sp = 140 + k * 1.5;
                    TrashBullet tb = new TrashBullet(bossCenterX, bossCenterY, Math.cos(ang) * sp, Math.sin(ang) * sp, wrapperNode());
                    tb.hitR = 10; tb.spin = 300;
                    addBullet(tb);
                }
            });
        }
        later(0.7 + shots * step + 0.2, () -> { hiddenType = -1; orbitSpeed = 1.7; });
        app.sfx(Sfx.FULL);
    }

    // ---- Crushed cans in aimed fans, plus apple cores that splat into purple puddles ----
    private void canVolley() {
        for (int r = 0; r < 3; r++) later(r * 0.5, () -> fan(5, 0.26, 300));
        for (int i = 0; i < 2; i++) {
            double tx = clamp(playerCx() + (i == 0 ? 0 : (rng.nextDouble() - 0.5) * 240), ARENA_X1 + 24, ARENA_X2 - 24);
            double ty = clamp(playerCy() + (i == 0 ? 0 : (rng.nextDouble() - 0.5) * 160), ARENA_Y1 + 24, ARENA_Y2 - 24);
            later(0.3 + i * 0.5, () -> lob(bossCenterX, bossCenterY - 20, tx, ty, 1.0, 130, coreNode(), Color.web("#8a3fc0", 0.35), () -> {
                app.sfx(Sfx.DUMP);
                ringFx(tx, ty, Color.web("#c070ff"));
                addPuddle(tx, ty, Color.web("#8a3fc0"));
            }));
        }
    }

    private void fan(int n, double spread, double speed) {
        double base = Math.atan2(playerCy() - bossCenterY, playerCx() - bossCenterX);
        for (int i = 0; i < n; i++) {
            double ang = base + (i - (n - 1) / 2.0) * spread;
            TrashBullet tb = new TrashBullet(bossCenterX, bossCenterY, Math.cos(ang) * speed, Math.sin(ang) * speed, canNode());
            tb.hitR = 12; tb.spin = 540;
            addBullet(tb);
        }
    }

    // ---- Takeout Meteor: every orbiting piece is pulled into one giant garbage ball, which is slammed onto where you stood ----
    private void takeoutMeteor() {
        orbitTarget = 0;
        meteorBall = buildMeteorBall();
        meteorBall.setMouseTransparent(true);
        meteorBall.setScaleX(0.25); meteorBall.setScaleY(0.25);
        meteorBall.setTranslateX(bossCenterX); meteorBall.setTranslateY(bossCenterY - 130);
        arena.getChildren().add(meteorBall);
        meteorX = playerCx(); meteorY = playerCy(); meteorT = 0;
        Circle ret = new Circle(0, 0, 85, Color.web("#b455ff", 0.16));
        ret.setStroke(Color.web("#e0a0ff", 0.8)); ret.setStrokeWidth(3);
        ret.getStrokeDashArray().addAll(12.0, 8.0);
        ret.setMouseTransparent(true);
        meteorReticle = ret;
        addGround(ret);
        app.sfx(Sfx.FULL);
    }

    private void updateMeteor(double tpf) {
        if (meteorBall == null) return;
        meteorT += tpf;
        double s = 0.25 + 0.75 * Math.min(1, meteorT / 1.1);
        meteorBall.setScaleX(s); meteorBall.setScaleY(s);
        meteorBall.setRotate(meteorBall.getRotate() + 200 * tpf);
        meteorBall.setTranslateX(bossCenterX + (rng.nextDouble() - 0.5) * 3);
        meteorBall.setTranslateY(bossCenterY - 130 + (rng.nextDouble() - 0.5) * 3);
        if (meteorT < 1.1) {
            meteorX += (playerCx() - meteorX) * Math.min(1, tpf * 7);
            meteorY += (playerCy() - meteorY) * Math.min(1, tpf * 7);
        }
        meteorReticle.setTranslateX(meteorX); meteorReticle.setTranslateY(meteorY);
        double pulse = 1 + 0.07 * Math.sin(meteorT * (meteorT < 1.1 ? 8 : 26));
        meteorReticle.setScaleX(pulse); meteorReticle.setScaleY(pulse);
        if (meteorT >= 1.5) {
            Group ball = meteorBall;
            meteorBall = null;
            double tx = meteorX, ty = meteorY;
            arena.getChildren().remove(ball);
            lob(ball.getTranslateX(), ball.getTranslateY(), tx, ty, 0.8, 150, ball, Color.web("#8a3fc0", 0.4), () -> meteorSlam(tx, ty));
        }
    }

    private void meteorSlam(double x, double y) {
        app.sfx(Sfx.DUMP);
        arena.getChildren().remove(meteorReticle);
        meteorReticle = null;
        orbitTarget = ORBIT_R;
        ringFx(x, y, Color.web("#e0a0ff"));
        ringFx(x, y, AURA);
        spark(x, y, AURA, 16, 220);
        addPuddle(x, y, Color.web("#8a3fc0"));
        if (invincible <= 0 && Math.hypot(playerCx() - x, playerCy() - y) < 85) hitPlayer();
        for (int i = 0; i < 14; i++) {
            double ang = i * 2 * Math.PI / 14;
            Node n = i % 3 == 0 ? canNode() : i % 3 == 1 ? wrapperNode() : coreNode();
            TrashBullet tb = new TrashBullet(x, y, Math.cos(ang) * 210, Math.sin(ang) * 210, n);
            tb.hitR = 11; tb.life = 1.1; tb.spin = 360;
            addBullet(tb);
        }
    }

    private Group buildMeteorBall() {
        Group g = new Group(sprite("boss/takeout_meteor.png", 116));
        g.setEffect(new DropShadow(BlurType.GAUSSIAN, AURA, 16, 0.8, 0, 0));
        return g;
    }

    // ---- Spirit Bottle overcharge fills while she survives (a hit costs some), then fires faster for a few seconds ----
    // ---- Mother Nature's blessing: after the Guardian is hurt, a seed sprouts that heals one heart ----
    private void updateHelp(double tpf) {
        if (def.kind() != BossDef.Kind.QUEEN) return;
        if (chargeT > 0) {
            chargeT -= tpf;
            if (chargeT <= 0) { chargeT = 0; charge = 0; }
        } else if (awakenT <= 0) {
            charge = Math.min(OVERCHARGE_FILL, charge + tpf);
            if (charge >= OVERCHARGE_FILL) { chargeT = OVERCHARGE_TIME; callout("SPIRIT OVERCHARGE!"); app.sfx(Sfx.MISSION); }
        }
        chargeFill.setWidth(100 * (chargeT > 0 ? chargeT / OVERCHARGE_TIME : charge / OVERCHARGE_FILL));
        chargeFill.setFill(chargeT > 0 ? Color.web("#ffe27a") : Color.web("#7fe8d0"));

        if (seed == null) {
            if (playerHP > 0 && playerHP < MAX_PLAYER_HP && blessingsLeft > 0 && awakenT <= 0) {
                blessingT -= tpf;
                if (blessingT <= 0) spawnSeed();
            }
            return;
        }
        seedLife -= tpf;
        seedGrow = Math.min(1, seedGrow + tpf);
        double sc = 0.3 + 0.7 * seedGrow;
        seed.setScaleX(sc); seed.setScaleY(sc);
        seed.setOpacity(seedLife < 2 ? 0.4 + 0.6 * Math.abs(Math.sin(seedLife * 10)) : 1);
        double sx = seed.getTranslateX(), sy = seed.getTranslateY();
        if (seedGrow >= 1 && Math.hypot(sx - playerCx(), sy - playerCy()) < 28) {
            playerHP = Math.min(MAX_PLAYER_HP, playerHP + 1);
            blessingsLeft--;
            arena.getChildren().remove(seed);
            seed = null;
            spark(sx, sy, Color.web("#7fe05a"), 12, 130);
            ringFx(sx, sy, Color.web("#b8ffb0"));
            app.sfx(Sfx.SELECT);
            callout("MOTHER NATURE'S BLESSING!");
        } else if (seedLife <= 0) {
            arena.getChildren().remove(seed);
            seed = null;
            blessingT = 4;
        }
    }

    private void spawnSeed() {
        double x = 0, y = 0;
        for (int i = 0; i < 12; i++) {
            x = ARENA_X1 + 40 + rng.nextDouble() * (ARENA_X2 - ARENA_X1 - 80);
            y = ARENA_Y1 + 40 + rng.nextDouble() * (ARENA_Y2 - ARENA_Y1 - 80);
            if (Math.hypot(x - bossCenterX, y - bossCenterY) > 150 && Math.hypot(x - playerCx(), y - playerCy()) > 110) break;
        }
        Circle halo = new Circle(0, -2, 17, Color.web("#b8ffb0", 0.18));
        halo.setStroke(Color.web("#b8ffb0", 0.8)); halo.setStrokeWidth(1.5);
        Ellipse mound = new Ellipse(0, 3, 9, 4), l1 = new Ellipse(-7, -6, 7, 3.5), l2 = new Ellipse(7, -6, 7, 3.5);
        mound.setFill(Color.web("#6a4a2a"));
        l1.setRotate(-30); l2.setRotate(30);
        for (Ellipse leaf : new Ellipse[]{l1, l2}) { leaf.setFill(Color.web("#7fe05a")); leaf.setStroke(Color.web("#2f7a2a")); }
        Rectangle stem = new Rectangle(-1.5, -6, 3, 9);
        stem.setFill(Color.web("#4aa03a"));
        seed = new Group(halo, mound, stem, l1, l2);
        seed.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.web("#7fe05a"), 10, 0.7, 0, 0));
        seed.setTranslateX(x); seed.setTranslateY(y);
        seed.setScaleX(0.3); seed.setScaleY(0.3);
        seed.setMouseTransparent(true);
        addGround(seed);
        seedLife = 10; seedGrow = 0;
        ringFx(x, y, Color.web("#b8ffb0"));
    }

    // ---- trash shapes ----
    private static ImageView sprite(String name, double size) {
        ImageView v = Sprites.view(name);
        v.setFitWidth(size); v.setFitHeight(size);
        v.setX(-size / 2); v.setY(-size / 2);
        return v;
    }

    private static Node canNode() { return sprite("boss/trash_can.png", 36); }

    private static Node wrapperNode() { return sprite("boss/trash_wrapper.png", 36); }

    private static Node coreNode() { return sprite("boss/trash_core.png", 36); }

    private static Node bananaPeelNode() {
        ImageView v = sprite("boss/stink_peel.png", 52);
        v.setEffect(new DropShadow(BlurType.GAUSSIAN, AURA, 8, 0.6, 0, 0));
        return new Group(v);
    }

    private static Node sodaCanNode() {
        ImageView can = sprite("boss/trash_can.png", 60);
        can.setEffect(new DropShadow(BlurType.GAUSSIAN, AURA, 14, 0.9, 0, 0));
        return can;
    }

    private static Node shrapnelNode() { return sprite("boss/trash_shard.png", 28); }

    // =========================================================================
    private void moveProjectiles(double tpf) {
        for (Bullet b : playerBullets) {
            b.x += b.vx * tpf; b.y += b.vy * tpf;
            b.node.setCenterX(b.x); b.node.setCenterY(b.y);
        }
        playerBullets.removeIf(b -> {
            if (b.x < ARENA_X1 || b.x > ARENA_X2 || b.y < ARENA_Y1 || b.y > ARENA_Y2) {
                arena.getChildren().remove(b.node); return true;
            }
            return false;
        });
        List<TrashBullet> dead = new ArrayList<>();
        for (TrashBullet tb : trashBullets) {
            tb.x += tb.vx * tpf; tb.y += tb.vy * tpf;
            if (tb.ricochet) bounce(tb);
            tb.node.setTranslateX(tb.x); tb.node.setTranslateY(tb.y);
            tb.life -= tpf;
            if (tb.warn > 0) tb.warn -= tpf;
            double op = tb.warn > 0 ? 0.3 : 1;
            if (tb.flicker) op *= 0.55 + 0.45 * Math.abs(Math.sin(tb.life * 22));
            if (tb.persist && tb.life < 0.6) op *= Math.max(0, tb.life / 0.6);
            tb.node.setOpacity(op);
            if (tb.spin != 0) tb.node.setRotate(tb.node.getRotate() + tb.spin * tpf);
            if (tb.breathe > 0) { double sc = 1 + 0.06 * Math.sin(tb.life * 4); tb.node.setScaleX(sc); tb.node.setScaleY(sc); }
            if (tb.trail != null) {
                tb.trailT -= tpf;
                if (tb.trailT <= 0) { tb.trailT = 0.04; puff(blob(4, tb.trail), tb.x, tb.y, 0, 0, 0.35, true, true); }
            }
            boolean out = !tb.ricochet && (tb.x < ARENA_X1 || tb.x > ARENA_X2 || tb.y < ARENA_Y1 || tb.y > ARENA_Y2);
            if (tb.life <= 0 || out) dead.add(tb);
        }
        for (TrashBullet tb : dead) {
            trashBullets.remove(tb);
            arena.getChildren().remove(tb.node);
            if (tb.life <= 0 && tb.onExpire != null) tb.onExpire.accept(tb);
        }
    }

    private void checkCollisions() {
        double bossDist = def.hitRadius();
        Iterator<Bullet> bi = playerBullets.iterator();
        while (bi.hasNext()) {
            Bullet b = bi.next();
            if (Math.hypot(b.x - bossCenterX, b.y - bossCenterY) < bossDist) {
                bi.remove(); arena.getChildren().remove(b.node);
                hitBoss();
            }
        }

        if (invincible > 0) return;
        double pcx = playerCx(), pcy = playerCy();
        Iterator<TrashBullet> ti = trashBullets.iterator();
        while (ti.hasNext()) {
            TrashBullet tb = ti.next();
            if (tb.harmless || tb.warn > 0) continue;
            if (Math.hypot(tb.x - pcx, tb.y - pcy) < tb.hitR) {
                if (!tb.persist) { ti.remove(); arena.getChildren().remove(tb.node); }   // clouds and spark puddles stay put
                hitPlayer();
                return;
            }
        }
        for (Rod r : new ArrayList<>(rods)) {                         // rods hurt along their whole length, once they are flying
            if (!r.flying) continue;
            double hx = Math.cos(r.ang) * 31, hy = Math.sin(r.ang) * 31;
            if (segDist(pcx, pcy, r.x - hx, r.y - hy, r.x + hx, r.y + hy) < 13) {
                rods.remove(r); arena.getChildren().remove(r.node);
                hitPlayer();
                return;
            }
        }
    }

    private void hitBoss() {
        if (awakenT > 0 || windupT > 0) return;
        bossHP = Math.max(0, bossHP - 1);
        HitFx fx = new HitFx(bossCenterX, bossCenterY, Color.web("#9fe0cf"));
        hitFx.add(fx); arena.getChildren().add(fx.node);
        app.sfx(Sfx.DUMP);

        if (bossHP == maxBossHp / 2 && phase == Phase.NORMAL) {
            phase = Phase.RAGE;
            bossNormal.setVisible(false);
            bossRage.setVisible(true);
            phaseFlash = 1.0;
            flashRect.setFill(Color.web("#e03030"));
            app.sfx(Sfx.MISSION);
            showLaugh();
        }
        if (bossHP <= 0 && def.kind() == BossDef.Kind.QUEEN && !unhinged) { beginSecondPhase(); return; }
        if (bossHP <= 0) {
            phase = Phase.DEAD;
            Audio.stopMusic();
            bossDeathTimer = 2.5;
            phaseFlash = 1.0;
            flashRect.setFill(Color.WHITE);
        }
    }

    private void hitPlayer() {
        playerHP = Math.max(0, playerHP - 1);
        charge = Math.max(0, charge - 6);
        invincible = INVINCIBLE_TIME;
        HitFx fx = new HitFx(playerX + PX / 2.0, playerY + SH / 2.0, Color.web("#e06060"));
        hitFx.add(fx); arena.getChildren().add(fx.node);
        phaseFlash = 0.4;
        flashRect.setFill(Color.web("#e03030"));
        app.sfx(Sfx.DUMP);

        if (playerHP <= 0) {
            retryActive = true;
            retryAlpha  = 0;
        }
    }

    // -------------------------------------------------------------------------
    private void refreshHud() {
        for (int i = 0; i < MAX_PLAYER_HP; i++) {
            heartFull[i].setVisible(i < playerHP);
            heartFull[i].setEffect(slowT > 0 ? heartPurple : null);
            heartEmpty[i].setVisible(i >= playerHP);
        }
        for (int i = 0; i < maxBossHp; i++) {
            boolean alive = i < bossHP;
            boolean rage = phase == Phase.RAGE;
            Color c;
            if (unhinged)
                c = alive ? Color.web("#ff00ff") : Color.web("#3a0a3a");
            else if (def.kind() == BossDef.Kind.QUEEN)       // Queen Bijou: pink bar, light blue in rage
                c = rage ? (alive ? Color.web("#8fd8ff") : Color.web("#2a3a4a"))
                         : (alive ? Color.web("#ff7fb8") : Color.web("#4a2a3a"));
            else
                c = rage ? (alive ? Color.web("#e03030") : Color.web("#4a2a2a"))
                         : (alive ? Color.web("#4a9a5a") : Color.web("#2a4a2a"));
            bossBar[i].setFill(c);
            bossBar[i].setStroke(Color.web("#ffffff", 0.25)); bossBar[i].setStrokeWidth(1);
        }
    }

    private void updateDeathSequence(double tpf) {
        bossDeathTimer -= tpf;
        // Shake boss
        bossNormal.setTranslateX((rng.nextDouble() - 0.5) * 10);
        bossRage.setTranslateX((rng.nextDouble()   - 0.5) * 10);
        double alpha = Math.min(1.0, (2.5 - bossDeathTimer));
        flashRect.setOpacity(alpha * 0.7);
        if (bossDeathTimer <= 0) {
            if (!reported) { reported = true; app.reportBossClear(def, fightTime, playerHP, MAX_PLAYER_HP); }
            onWin.run();
        }
    }

    private void blinkRetry(double tpf) {
        retryAlpha = Math.min(1.0, retryAlpha + tpf * 2);
        retryLabel.setOpacity(retryAlpha);
        retryPrompt.setOpacity(retryAlpha > 0.9 ? Math.abs(Math.sin(retryAlpha * 5 + System.nanoTime() * 1e-9 * 3)) : 0);
    }

    private void retry() {
        enter();
        app.sfx(Sfx.SELECT);
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    // -------------------------------------------------------------------------
    @Override public void onDirection(Direction d) { if (pause.isOpen()) pause.onDirection(d); }

    @Override public void onConfirm() {
        if (pause.isOpen()) pause.onConfirm();
        else if (retryActive) retry();
    }

    /** ESC pauses the fight (RESUME / SETTINGS / QUIT); on the game-over or victory screen it still quits. */
    @Override public void onCancel() {
        if (pause.isOpen()) pause.onCancel();
        else if (retryActive || phase == Phase.DEAD) app.quitBossFight();
        else openPause();
    }

    @Override public void onPause() {
        if (pause.isOpen()) pause.close();
        else if (!retryActive && phase != Phase.DEAD) openPause();
    }

    private void openPause() {
        shooting = false;
        pause.open();
    }
}
