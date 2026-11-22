package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
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
 * Controls: Arrow keys = move player  ENTER/Z = shoot  X = special (not used here)
 *
 * Player shoots drops of "Spirit Bottle" liquid.
 * Muck Mound: Gunk Shot (aimed, fast sludge), Sludge Bomb (lobbed, splashes into a puddle and droplets), slime rain, puddles, sludge ring.
 * Queen Muck: Bottle Caps (ricochet), Rusted Rebar & Pipes (hover, then fly at you), Rolling Tires (lane rollers),
 * Leaky Car Batteries (lobbed, leave spark puddles) and Tied Trash Bags (lobbed, pop into a stink cloud).
 * Three player HP; six boss HP bars.
 * On boss death: victory cutscene.  On player death: retry prompt.
 */
public final class BossFightScreen extends Screen {

    // -- layout constants ------------------------------------------------------
    private static final int PX   = Sprites.PX;
    private static final int SH   = 72;
    private static final double ARENA_X1 = 32, ARENA_X2 = Ui.W - 32;
    private static final double ARENA_Y1 = 80, ARENA_Y2 = Ui.H - 100;
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
    private final int maxBossHp, bossW, bossH;

    private double playerX, playerY;
    private double bossCenterX, bossCenterY;
    private double bossShakeX, bossShakeY;

    private int playerHP = MAX_PLAYER_HP;
    private int bossHP;
    private double shootCooldown = 0;
    private double bossFireTimer = 0;
    private double bossFireInterval = 2.0;
    private int bossPattern = 0;
    private double phaseFlash = 0;
    private double invincible = 0;
    private double bossDeathTimer = -1;
    private double retryAlpha = 0;
    private boolean retryActive = false;
    private final Random rng = new Random(99);

    // Input flags
    private boolean moveUp, moveDown, moveLeft, moveRight, shooting;

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
    private final Rectangle[] bossBar;
    private final Ui.Label bossLabel;
    private final Ui.Label laughLabel;
    private double laughTimer;
    private final Ui.Label retryLabel;
    private final Ui.Label retryPrompt;
    private final Rectangle flashRect = new Rectangle(Ui.W, Ui.H, Color.WHITE);

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
            node.setStroke(col); node.setStrokeWidth(2.5);
        }
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
        Rectangle bg = new Rectangle(Ui.W, Ui.H, Color.web("#0d1a0f"));
        root.getChildren().add(bg);
        root.getChildren().add(def.backdrop().startsWith("scene:")        // the boss's arena, dimmed
                ? StoryFx.scene(def.backdrop().substring("scene:".length()))
                : Sprites.view(def.backdrop()));
        Rectangle dim = new Rectangle(Ui.W, Ui.H, Color.web("#0d1a0f", 0.62));
        root.getChildren().add(dim);

        // Floor grid pattern
        for (int gx = 32; gx < Ui.W; gx += 32) {
            Rectangle line = new Rectangle(1, Ui.H - 160, Color.web("#ffffff", 0.04));
            line.setLayoutX(gx); line.setLayoutY(ARENA_Y1);
            root.getChildren().add(line);
        }
        for (int gy = 80; gy < Ui.H - 80; gy += 32) {
            Rectangle hline = new Rectangle(Ui.W - 64, 1, Color.web("#ffffff", 0.04));
            hline.setLayoutX(32); hline.setLayoutY(gy);
            root.getChildren().add(hline);
        }

        // Arena border
        Rectangle border = new Rectangle(ARENA_X2 - ARENA_X1, ARENA_Y2 - ARENA_Y1);
        border.setLayoutX(ARENA_X1); border.setLayoutY(ARENA_Y1);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.web("#4a9a5a", 0.6)); border.setStrokeWidth(2);
        root.getChildren().add(border);

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

        double barTotalW = 360;
        double segStep = barTotalW / maxBossHp;
        double barStartX = (Ui.W - barTotalW) / 2.0;
        for (int i = 0; i < maxBossHp; i++) {
            Rectangle seg = new Rectangle(Math.max(2, segStep - 2), 12);
            seg.setArcWidth(4); seg.setArcHeight(4);
            seg.setLayoutX(barStartX + i * segStep); seg.setLayoutY(30);
            bossBar[i] = seg;
            root.getChildren().add(seg);
        }

        // Retry overlay
        retryLabel  = new Ui.Label("GAME OVER", 24, Ui.WHITE);
        retryLabel.setLayoutX((Ui.W - retryLabel.textWidth()) / 2.0);
        retryLabel.setLayoutY(Ui.H / 2.0 - 50);
        retryPrompt = new Ui.Label("ENTER  Try Again     X  Quit to Menu", 11, Ui.ACCENT);
        retryPrompt.setLayoutX((Ui.W - retryPrompt.textWidth()) / 2.0);
        retryPrompt.setLayoutY(Ui.H / 2.0);
        retryLabel.setOpacity(0); retryPrompt.setOpacity(0);
        root.getChildren().addAll(retryLabel, retryPrompt);

        // Set up keyboard input - forward key states to boolean flags
        root.setFocusTraversable(true);
        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP, W    -> moveUp    = true;
                case DOWN, S  -> moveDown  = true;
                case LEFT, A  -> moveLeft  = true;
                case RIGHT, D -> moveRight = true;
                case ENTER, Z -> shooting  = true;
                case X        -> { if (retryActive) onCancel(); }
            }
        });
        root.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case UP, W    -> moveUp    = false;
                case DOWN, S  -> moveDown  = false;
                case LEFT, A  -> moveLeft  = false;
                case RIGHT, D -> moveRight = false;
                case ENTER, Z -> { shooting = false; if (retryActive) retry(); }
            }
        });
    }

    // -------------------------------------------------------------------------
    @Override
    public void enter() {
        phase = Phase.NORMAL;
        playerHP = MAX_PLAYER_HP;
        bossHP   = maxBossHp;
        bossFireTimer = 0;
        bossPattern   = 0;
        bossDeathTimer = -1;
        retryActive = false;
        retryAlpha = 0;
        retryLabel.setOpacity(0); retryPrompt.setOpacity(0);
        playerBullets.clear(); trashBullets.clear(); hitFx.clear();
        arena.getChildren().removeIf(n -> n != bossNormal && n != bossRage && n != playerImg);
        lobs.clear(); rods.clear(); puffs.clear(); delayed.clear(); attackT = 0;

        playerX = ARENA_X1 + 60;
        playerY = (ARENA_Y1 + ARENA_Y2) / 2.0 - SH / 2.0;
        bossCenterX = ARENA_X2 - bossW / 2.0 - 20;
        bossCenterY = (ARENA_Y1 + ARENA_Y2) / 2.0;

        bossNormal.setVisible(true);
        bossRage.setVisible(false);
        moveUp = moveDown = moveLeft = moveRight = shooting = false;
        refreshHud();
        root.requestFocus();
        showLaugh();
        String track = switch (def.kind()) {
            case QUEEN -> "boss_queen.wav";
            case MOUND -> "boss_mound.wav";
            default -> null;
        };
        if (track != null) Audio.playMusic(track, 0.5);
    }

    @Override
    public void exit() { Audio.stopMusic(); }

    /** The boss shouts: floating text plus its sound (Queen Muck's laugh, a gross groan for the others). */
    private void showLaugh() {
        laughTimer = 1.6;
        if (def.kind() == BossDef.Kind.QUEEN) app.laugh();
        else app.sfx(Sfx.FULL);
    }

    // -------------------------------------------------------------------------
    @Override
    public void update(double tpf) {
        if (laughTimer > 0) laughTimer -= tpf;
        if (attackT > 0) attackT -= tpf;
        attackLabel.setOpacity(Math.max(0, Math.min(1, attackT * 2)));
        if (def.laugh() != null) bossNormal.setImage(Sprites.get(laughTimer > 0 ? def.laugh() : def.normal()));
        laughLabel.setOpacity(Math.max(0, Math.min(1, laughTimer * 2)));
        if (phase == Phase.DEAD) { updateDeathSequence(tpf); return; }
        if (retryActive) { blinkRetry(tpf); return; }

        // -- player movement --
        double dx = 0, dy = 0;
        if (moveLeft)  dx -= PLAYER_SPEED * tpf;
        if (moveRight) dx += PLAYER_SPEED * tpf;
        if (moveUp)    dy -= PLAYER_SPEED * tpf;
        if (moveDown)  dy += PLAYER_SPEED * tpf;
        playerX = clamp(playerX + dx, ARENA_X1,  ARENA_X2 - PX);
        playerY = clamp(playerY + dy, ARENA_Y1,  ARENA_Y2 - SH);

        // -- player shoot --
        shootCooldown -= tpf;
        if (shooting && shootCooldown <= 0) {
            fireBullet();
            shootCooldown = SHOOT_CD;
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

        // -- boss fire --
        bossFireTimer -= tpf;
        if (bossFireTimer <= 0) {
            double wait = def.kind() == BossDef.Kind.MOUND ? fireMound() : fireQueen();
            bossPattern = (bossPattern + 1) % 60;
            bossFireInterval = phase == Phase.RAGE ? def.fireRage() : def.fireNormal();
            bossFireTimer = bossFireInterval * wait;     // big attacks leave a longer breather
        }
        runDelayed(tpf);
        updateLobs(tpf);
        updateRods(tpf);

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

        // -- position sprites --
        playerImg.setLayoutX(playerX);
        playerImg.setLayoutY(playerY);
        bossNormal.setLayoutX(bossCenterX - bossW / 2.0 + bossShakeX);
        bossNormal.setLayoutY(bossCenterY - bossH / 2.0 + bossShakeY);
        bossRage.setLayoutX(bossCenterX - bossW / 2.0 + bossShakeX);
        bossRage.setLayoutY(bossCenterY - bossH / 2.0 + bossShakeY);

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
    private void fireBullet() {
        double cx = playerX + PX / 2.0;
        double cy = playerY + SH / 2.0;
        double tx = bossCenterX, ty = bossCenterY;
        double len = Math.hypot(tx - cx, ty - cy);
        if (len < 1) return;
        Bullet b = new Bullet(cx, cy, (tx - cx) / len * BULLET_SPEED, (ty - cy) / len * BULLET_SPEED);
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
            heartEmpty[i].setVisible(i >= playerHP);
        }
        for (int i = 0; i < maxBossHp; i++) {
            boolean alive = i < bossHP;
            Color c = phase == Phase.RAGE
                ? (alive ? Color.web("#e03030") : Color.web("#4a2a2a"))
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
    @Override public void onConfirm() { if (retryActive) retry(); }
    @Override public void onCancel()  { app.showMainMenu(); }
}
