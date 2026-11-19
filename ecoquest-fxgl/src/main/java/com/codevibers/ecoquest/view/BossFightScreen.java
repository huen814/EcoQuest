package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Soul Knight-style boss fight.
 *
 * Controls: Arrow keys = move player  ENTER/Z = shoot  X = special (not used here)
 *
 * Player shoots drops of "Spirit Bottle" liquid.
 * Boss throws garbage projectiles in wave patterns.
 * Three player HP; six boss HP bars.
 * On boss death: victory cutscene.  On player death: retry prompt.
 */
public final class BossFightScreen extends Screen {

    // -- layout constants ------------------------------------------------------
    private static final int PX   = Sprites.PX;
    private static final int SH   = 72;
    private static final int BOSS_W = 144, BOSS_H = 216;   // her 48x72 sprite at 3x
    private static final double ARENA_X1 = 32, ARENA_X2 = Ui.W - 32;
    private static final double ARENA_Y1 = 80, ARENA_Y2 = Ui.H - 100;
    private static final double PLAYER_SPEED = 180;
    private static final double BULLET_SPEED  = 320;
    private static final double TRASH_SPEED_N = 180;
    private static final double TRASH_SPEED_R = 260;   // rage phase
    private static final double SHOOT_CD = 0.30;
    private static final int MAX_PLAYER_HP = 3;
    private static final int MAX_BOSS_HP   = 50;
    private static final double INVINCIBLE_TIME = 1.4;

    // -- boss state ------------------------------------------------------------
    private enum Phase { NORMAL, RAGE, DEAD }
    private Phase phase = Phase.NORMAL;

    // -- game objects ----------------------------------------------------------
    private final Pane arena = new Pane();

    private final ImageView playerImg;
    private final ImageView bossNormal;
    private final ImageView bossRage;

    private double playerX, playerY;
    private double bossCenterX, bossCenterY;
    private double bossShakeX, bossShakeY;

    private int playerHP = MAX_PLAYER_HP;
    private int bossHP   = MAX_BOSS_HP;
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

    // -- HUD elements ---------------------------------------------------------
    private final ImageView[] heartFull  = new ImageView[MAX_PLAYER_HP];
    private final ImageView[] heartEmpty = new ImageView[MAX_PLAYER_HP];
    private final Rectangle[] bossBar    = new Rectangle[MAX_BOSS_HP];
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
        final Circle node;
        TrashBullet(double x, double y, double vx, double vy, Color col) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            node = new Circle(7, col);
            node.setStroke(col.darker()); node.setStrokeWidth(1.5);
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
    public BossFightScreen(EcoQuestApp app) {
        super(app);

        // Arena background
        Rectangle bg = new Rectangle(Ui.W, Ui.H, Color.web("#0d1a0f"));
        root.getChildren().add(bg);
        root.getChildren().add(Sprites.view("backdrop/palace.png"));      // her palace in the polluted outskirts, dimmed
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
        bossNormal = Sprites.view("boss/queen_muck.png");
        bossRage   = Sprites.view("boss/queen_muck_rage.png");

        playerImg.setViewport(new Rectangle2D(0, 0, PX, SH));
        bossNormal.setFitWidth(BOSS_W); bossNormal.setFitHeight(BOSS_H);
        bossRage.setFitWidth(BOSS_W);   bossRage.setFitHeight(BOSS_H);

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
        bossLabel = new Ui.Label("QUEEN MUCK", 10, Ui.WHITE);
        bossLabel.setLayoutX((Ui.W - bossLabel.textWidth()) / 2.0);
        bossLabel.setLayoutY(14);
        root.getChildren().add(bossLabel);

        laughLabel = new Ui.Label("HO HO HO!", 14, Ui.BAD);
        laughLabel.setLayoutX((Ui.W - laughLabel.textWidth()) / 2.0);
        laughLabel.setLayoutY(52);
        laughLabel.setOpacity(0);
        root.getChildren().add(laughLabel);

        double barTotalW = MAX_BOSS_HP * 38.0;
        double barStartX = (Ui.W - barTotalW) / 2.0;
        for (int i = 0; i < MAX_BOSS_HP; i++) {
            Rectangle seg = new Rectangle(32, 12);
            seg.setArcWidth(4); seg.setArcHeight(4);
            seg.setLayoutX(barStartX + i * 38); seg.setLayoutY(30);
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
        bossHP   = MAX_BOSS_HP;
        bossFireTimer = 0;
        bossPattern   = 0;
        bossDeathTimer = -1;
        retryActive = false;
        retryAlpha = 0;
        retryLabel.setOpacity(0); retryPrompt.setOpacity(0);
        playerBullets.clear(); trashBullets.clear(); hitFx.clear();
        arena.getChildren().removeIf(n -> n instanceof Circle);

        playerX = ARENA_X1 + 60;
        playerY = (ARENA_Y1 + ARENA_Y2) / 2.0 - SH / 2.0;
        bossCenterX = ARENA_X2 - BOSS_W / 2.0 - 20;
        bossCenterY = (ARENA_Y1 + ARENA_Y2) / 2.0;

        bossNormal.setVisible(true);
        bossRage.setVisible(false);
        moveUp = moveDown = moveLeft = moveRight = shooting = false;
        refreshHud();
        root.requestFocus();
        showLaugh();
    }

    /** Queen Muck laughs: floating text plus the laugh sound. */
    private void showLaugh() {
        laughTimer = 1.6;
        app.laugh();
    }

    // -------------------------------------------------------------------------
    @Override
    public void update(double tpf) {
        if (laughTimer > 0) laughTimer -= tpf;
        bossNormal.setImage(Sprites.get(laughTimer > 0 ? "boss/queen_muck_laugh.png" : "boss/queen_muck.png"));
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
        double targetX = (ARENA_X1 + ARENA_X2) / 2.0 + Math.sin(System.nanoTime() * 1e-9 * 0.6) * 100;
        double targetY = (ARENA_Y1 + ARENA_Y2) / 2.0 + Math.cos(System.nanoTime() * 1e-9 * 0.4) * 30;
        bossCenterX += (targetX - bossCenterX) * bossSpeed * tpf * 0.025;
        bossCenterY += (targetY - bossCenterY) * bossSpeed * tpf * 0.025;
        bossShakeX   = phase == Phase.RAGE ? (rng.nextDouble() - 0.5) * 4 : 0;
        bossShakeY   = phase == Phase.RAGE ? (rng.nextDouble() - 0.5) * 4 : 0;

        // -- boss fire --
        bossFireTimer -= tpf;
        if (bossFireTimer <= 0) {
            fireTrash();
            bossPattern = (bossPattern + 1) % 4;
            bossFireInterval = phase == Phase.RAGE ? 0.9 : 1.6;
            bossFireTimer = bossFireInterval;
        }

        // -- move bullets --
        double trashSpeed = phase == Phase.RAGE ? TRASH_SPEED_R : TRASH_SPEED_N;
        moveProjectiles(tpf);

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
        bossNormal.setLayoutX(bossCenterX - BOSS_W / 2.0 + bossShakeX);
        bossNormal.setLayoutY(bossCenterY - BOSS_H / 2.0 + bossShakeY);
        bossRage.setLayoutX(bossCenterX - BOSS_W / 2.0 + bossShakeX);
        bossRage.setLayoutY(bossCenterY - BOSS_H / 2.0 + bossShakeY);

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

    private void fireTrash() {
        double bx = bossCenterX, by = bossCenterY;
        Color col = new Color[] {
            Color.web("#c8b050"), Color.web("#d9566f"), Color.web("#8abc84"), Color.web("#7fb2f2")
        }[bossPattern % 4];

        double speed = phase == Phase.RAGE ? TRASH_SPEED_R : TRASH_SPEED_N;

        switch (bossPattern) {
            case 0 -> {   // 4-way cross
                for (int i = 0; i < 4; i++) {
                    double ang = i * Math.PI / 2;
                    addTrash(bx, by, Math.cos(ang) * speed, Math.sin(ang) * speed, col);
                }
            }
            case 1 -> {   // aimed at player + spread
                double px = playerX + PX / 2.0, py = playerY + SH / 2.0;
                double len = Math.hypot(px - bx, py - by);
                double bvx = (px - bx) / len, bvy = (py - by) / len;
                for (int s = -1; s <= 1; s++) {
                    double ang = Math.atan2(bvy, bvx) + s * 0.3;
                    addTrash(bx, by, Math.cos(ang) * speed, Math.sin(ang) * speed, col);
                }
            }
            case 2 -> {   // 8-way burst
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4 + (phase == Phase.RAGE ? bossFireTimer : 0);
                    addTrash(bx, by, Math.cos(ang) * speed * 0.75, Math.sin(ang) * speed * 0.75, col);
                }
            }
            case 3 -> {   // spiral (rage only: doubled)
                int count = phase == Phase.RAGE ? 10 : 5;
                for (int i = 0; i < count; i++) {
                    double ang = i * (2 * Math.PI / count) + bossFireTimer * 2;
                    addTrash(bx, by, Math.cos(ang) * speed, Math.sin(ang) * speed, col);
                }
            }
        }
    }

    private void addTrash(double x, double y, double vx, double vy, Color col) {
        TrashBullet tb = new TrashBullet(x, y, vx, vy, col);
        trashBullets.add(tb);
        arena.getChildren().add(tb.node);
    }

    // -------------------------------------------------------------------------
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
        for (TrashBullet tb : trashBullets) {
            tb.x += tb.vx * tpf; tb.y += tb.vy * tpf;
            tb.node.setCenterX(tb.x); tb.node.setCenterY(tb.y);
        }
        trashBullets.removeIf(tb -> {
            if (tb.x < ARENA_X1 || tb.x > ARENA_X2 || tb.y < ARENA_Y1 || tb.y > ARENA_Y2) {
                arena.getChildren().remove(tb.node); return true;
            }
            return false;
        });
    }

    private void checkCollisions() {
        double bossDist = BOSS_H * 0.3;
        Iterator<Bullet> bi = playerBullets.iterator();
        while (bi.hasNext()) {
            Bullet b = bi.next();
            if (Math.hypot(b.x - bossCenterX, b.y - bossCenterY) < bossDist) {
                bi.remove(); arena.getChildren().remove(b.node);
                hitBoss();
            }
        }

        if (invincible > 0) return;
        double pcx = playerX + PX / 2.0, pcy = playerY + SH / 2.0;
        Iterator<TrashBullet> ti = trashBullets.iterator();
        while (ti.hasNext()) {
            TrashBullet tb = ti.next();
            if (Math.hypot(tb.x - pcx, tb.y - pcy) < 18) {
                ti.remove(); arena.getChildren().remove(tb.node);
                hitPlayer();
                break;
            }
        }
    }

    private void hitBoss() {
        bossHP = Math.max(0, bossHP - 1);
        HitFx fx = new HitFx(bossCenterX, bossCenterY, Color.web("#9fe0cf"));
        hitFx.add(fx); arena.getChildren().add(fx.node);
        app.sfx(Sfx.DUMP);

        if (bossHP == MAX_BOSS_HP / 2 && phase == Phase.NORMAL) {
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
        for (int i = 0; i < MAX_BOSS_HP; i++) {
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
            app.showVictory();
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
