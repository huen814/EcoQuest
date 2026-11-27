package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;
import java.util.List;
import java.util.Random;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * Story lines before or after a boss fight: the boss's arena, the Guardian, the boss, and a dialogue box.
 * The Muck Mound gets extra scenes. Before the fight the Guardian takes a break on a bench at golden hour; then, suddenly, trash
 * bursts out of the ground, the mound drags itself up and the world turns gloomy into its own landfill arena. After the fight the
 * light fades the mound out of existence and the world returns to normal.
 */
public final class BossStoryScreen extends Screen {

    private static final int FEET = 340;
    private static final int TREES_Y = 60;           // the park tree band (backdrop/park_trees.png) sits at the same height as in park_plaza.png
    private static final int ERUPT_PAGE = 2;           // the Muck Mound's page on which the ground suddenly erupts

    private final DialogBox box;
    private final List<String> pages;
    private final Runnable next;
    private final BossDef def;
    private final ImageView playerImg;
    private final ImageView bossImg;
    private final boolean before;                       // true before the fight, false after it
    private final boolean mound;
    private final Random rng = new Random();
    private final StoryFx.Particles fx = new StoryFx.Particles(Ui.W, Ui.H);
    private final Rectangle bossClip = new Rectangle();
    private Node gloomScene, vignette;
    private Rectangle dark, haze, warm, fade;
    private boolean erupted;
    private Circle vanishGlow;
    private Rectangle calmTint, calmWarm;                // after the chase: the storm's violet hangs over the street, then clears
    private Group calmRain;
    private final double[] calmV = new double[50];
    private double t, mt, lastBurst, gloom;

    /** @param showBoss true before the fight (the boss looms), false after it (only the arena and the Guardian). */
    public BossStoryScreen(EcoQuestApp app, BossDef def, List<String> pages, Runnable next, boolean showBoss) {
        super(app);
        this.pages = pages;
        this.next = next;
        this.def = def;
        this.before = showBoss;
        this.mound = def.kind() == BossDef.Kind.MOUND;
        this.erupted = !(mound && showBoss) || pages.size() <= ERUPT_PAGE;

        Node arena = def.backdrop().startsWith("scene:")
                ? StoryFx.scene(def.backdrop().substring("scene:".length()))
                : Sprites.view(def.backdrop());
        if (mound) {
            root.getChildren().add(StoryFx.scene("river", "river_story"));         // the clean world: sky and ground...
            ImageView trees = Sprites.view("backdrop/park_trees.png");              // ...with the very same trees as the park in the knight's cutscene
            trees.setLayoutY(TREES_Y);
            root.getChildren().add(trees);
            if (!erupted) {                                         // the break: a bench in the golden evening light, until the ground erupts
                ImageView bench = Sprites.view("world/rest_bench_story.png");
                bench.setLayoutX(140 - bench.getImage().getWidth() / 2);
                bench.setLayoutY(FEET - 3 - bench.getImage().getHeight());
                warm = new Rectangle(Ui.W, Ui.H, Color.web("#ff9f4a"));
                warm.setOpacity(0);
                root.getChildren().addAll(bench, warm);
            }
            gloomScene = arena;
            root.getChildren().add(gloomScene);                     // the trash-choked arena fades in over it
        } else {
            root.getChildren().add(arena);
        }
        dark = new Rectangle(Ui.W, Ui.H, Color.web("#0d1a0f", mound ? 0.38 : showBoss ? 0.35 : 0.1));
        root.getChildren().add(dark);
        if (mound) {
            haze = new Rectangle(Ui.W, Ui.H, Color.web("#2a3a22", 0.3));
            vignette = StoryFx.vignette(Color.web("#050a05", 0.75));
            root.getChildren().addAll(haze, vignette);
            gloom = showBoss ? 0 : 1;
            applyGloom();
        }

        Sibling me = app.save().sibling();
        playerImg = Sprites.view(me.sheet(false));      // plain sheet: cutscenes never show shop cosmetics
        StoryFx.stand(playerImg, 140, FEET, 2);

        bossImg = Sprites.view(def.story());
        bossImg.setFitWidth(def.width() * 1.4);
        bossImg.setFitHeight(def.height() * 1.4);
        bossImg.setLayoutX(Ui.W - bossImg.getFitWidth() - 40);
        bossImg.setLayoutY(FEET - bossImg.getFitHeight() + 10);
        bossImg.setVisible(mound ? !showBoss || erupted : showBoss);      // the mound is still there at the start of its farewell scene
        if (mound && showBoss) bossImg.setClip(bossClip);
        root.getChildren().add(bossImg);

        if (mound) {
            vanishGlow = StoryFx.glow(150, Color.web("#d8ffe8", 0.8));
            vanishGlow.setLayoutX(bossImg.getLayoutX() + bossImg.getFitWidth() / 2);
            vanishGlow.setLayoutY(FEET + 10 - bossImg.getFitHeight() / 2);
            vanishGlow.setOpacity(0);
            root.getChildren().add(vanishGlow);
        }
        root.getChildren().addAll(playerImg, fx);
        if (def.kind() == BossDef.Kind.CHASE && !showBoss) {      // the storm of the chase scene clears into a normal evening street
            calmTint = new Rectangle(Ui.W, Ui.H, Color.web("#2a0c4a", 0.5));
            calmWarm = new Rectangle(Ui.W, Ui.H, Color.web("#ffe9a8"));
            calmWarm.setOpacity(0);
            calmRain = new Group();
            for (int i = 0; i < calmV.length; i++) {
                double len = 14 + rng.nextDouble() * 12;
                Line l = new Line(0, 0, -0.28 * len, len);
                l.setStroke(Color.web("#d4c8ff", 0.3 + rng.nextDouble() * 0.2));
                l.setStrokeWidth(1.2);
                l.setTranslateX(rng.nextDouble() * (Ui.W + 160));
                l.setTranslateY(rng.nextDouble() * Ui.H);
                calmV[i] = 620 + rng.nextDouble() * 320;
                calmRain.getChildren().add(l);
            }
            for (Node n : new Node[] {calmTint, calmWarm, calmRain}) n.setMouseTransparent(true);
            root.getChildren().addAll(calmTint, calmWarm, calmRain);
        }
        if (!erupted) {
            fade = new Rectangle(Ui.W, Ui.H, Color.BLACK);          // fades in from black after the villager's thank-you, then flashes when the ground erupts
            fade.setMouseTransparent(true);
            root.getChildren().add(fade);
        }

        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().add(box);
    }

    @Override
    public void enter() {
        t = 0;
        mt = 0;
        lastBurst = 0;
        box.show(pages);
        if (erupted) app.sfx(before ? Sfx.FULL : Sfx.MISSION);       // the Muck Mound's break is quiet; its eruption plays the sound
        if (before) {                         // before the fight: its battle music starts now and carries on into the fight
            switch (def.kind()) {
                case MOUND -> { if (erupted) Audio.playMusic("boss_mound.wav", 0.5); }
                case CHASE -> Audio.playMusic("chase.wav", 0.55);
                default -> { }
            }
        }
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        fx.update(tpf);
        if (mound) updateMound(tpf);
        else bossImg.setTranslateY(Math.round(Math.sin(t * 2.2) * 3));
        if (calmTint != null) updateCalm(tpf);
    }

    /** The song of the title screen plays after the chase and carries on until the next level starts. */
    @Override
    public boolean titleMusic() { return def == BossDef.EXPERIMENT && !before; }

    /** Storm to clear sky: the violet tint and rain fade away, a soft golden glow comes up and a few glints drift by. */
    private void updateCalm(double tpf) {
        double k = Math.max(0, Math.min(1, (t - 0.3) / 3.0));
        double e = k * k * (3 - 2 * k);
        calmTint.setOpacity(1 - e);
        calmRain.setOpacity((1 - e) * 0.9);
        calmWarm.setOpacity(0.14 * Math.sin(Math.PI * e));
        if (e < 1) {
            for (int i = 0; i < calmV.length; i++) {
                Node l = calmRain.getChildren().get(i);
                double nx = l.getTranslateX() - calmV[i] * 0.28 * tpf, ny = l.getTranslateY() + calmV[i] * tpf;
                if (ny > Ui.H + 20 || nx < -30) { nx = rng.nextDouble() * (Ui.W + 160); ny = -30 - rng.nextDouble() * 40; }
                l.setTranslateX(nx); l.setTranslateY(ny);
            }
        }
        if (e > 0.3 && t - lastBurst > 0.35) {
            lastBurst = t;
            fx.burst(80 + rng.nextDouble() * 560, 150 + rng.nextDouble() * 180, 1, () -> StoryFx.sparkle(3, Color.web("#fff3b0")), 10, 2.2, -16);
        }
    }

    private void updateMound(double tpf) {
        mt += tpf;
        double bossX = bossImg.getLayoutX() + bossImg.getFitWidth() / 2, ground = FEET + 10, h = bossImg.getFitHeight();
        if (before) {
            if (!erupted && box.page() >= ERUPT_PAGE) erupt();
            if (!erupted) {
                updateBreak();
                return;
            }
            gloom = Math.min(1, Math.max(0, (mt - 0.9) / 2.6));
            double k = Math.max(0, Math.min(1, (mt - 1.1) / 2.6)), e = 1 - Math.pow(1 - k, 3);
            if (fade != null) {
                fade.setOpacity(Math.max(0, 1 - mt / 0.35) * 0.6);          // a flash as the ground erupts
                warm.setOpacity(0.16 * Math.max(0, 1 - mt / 0.8));
                playerImg.setTranslateY(-Math.sin(Math.PI * Math.min(1, mt / 0.5)) * 26);     // the Guardian leaps up, startled
            }
            double sink = (1 - e) * (h + 24);
            bossImg.setTranslateY(sink + (k >= 1 ? Math.round(Math.sin(t * 2.2) * 3) : 0));
            bossClip.setX(0);
            bossClip.setY(0);
            bossClip.setWidth(bossImg.getFitWidth());
            bossClip.setHeight(Math.max(0, h - sink + 12));
            if (k < 1) {
                if (mt - lastBurst > 0.07) {
                    lastBurst = mt;
                    boolean surge = mt < 1.6;         // first trash bursts out of the ground all over the clearing, then it piles up around the mound
                    double x = surge ? 30 + rng.nextDouble() * (Ui.W - 60) : bossX + (rng.nextDouble() - 0.5) * 120;
                    fx.burst(x, ground + (surge ? rng.nextDouble() * 60 - 20 : 0), surge ? 5 : 3, () -> StoryFx.randomLitter(rng, 20), surge ? 260 : 150, 0.9, 520);
                    if (rng.nextDouble() < 0.4) app.sfx(Sfx.DUMP);
                }
                double shake = mt < 0.8 ? 10 : 6;
                root.setTranslateX((rng.nextDouble() - 0.5) * shake);
                root.setTranslateY((rng.nextDouble() - 0.5) * shake * 0.65);
            } else {
                root.setTranslateX(0);
                root.setTranslateY(0);
            }
        } else {
            double k = Math.max(0, Math.min(1, (mt - 0.5) / 2.2));
            gloom = 1 - Math.max(0, Math.min(1, (mt - 0.6) / 3.0));
            bossImg.setOpacity(1 - k);
            bossImg.setTranslateY(Math.round(Math.sin(t * 2.2) * 3) - k * 10);
            vanishGlow.setOpacity(Math.sin(Math.PI * k) * 0.9);
            if (k > 0 && k < 1 && mt - lastBurst > 0.06) {
                lastBurst = mt;
                fx.burst(bossX + (rng.nextDouble() - 0.5) * 150, ground - rng.nextDouble() * h * 0.9, 2,
                        () -> StoryFx.sparkle(3, Color.web("#d8ffe8")), 70, 1.4, -60);
            }
            if (k >= 1) bossImg.setVisible(false);
        }
        applyGloom();
    }

    /** The calm before the eruption: a fade in from black, a warm evening glow, the Guardian breathing slowly and a few drifting glints. */
    private void updateBreak() {
        fade.setOpacity(Math.max(0, 1 - t / 1.0));
        warm.setOpacity(0.16 * Math.min(1, t / 1.5));
        playerImg.setTranslateY(Math.round(Math.sin(t * 1.6)));
        if (t - lastBurst > 0.8) {
            lastBurst = t;
            fx.burst(300 + rng.nextDouble() * 380, 170 + rng.nextDouble() * 120, 1, () -> StoryFx.sparkle(3, Color.web("#fff3b0")), 10, 2.2, -16);
        }
    }

    /** The ground suddenly bursts: a flash, a rumble, trash flying out of the earth and the battle music kicking in. */
    private void erupt() {
        erupted = true;
        mt = 0;
        lastBurst = 0;
        bossImg.setVisible(true);
        fade.setFill(Color.web("#fff6d8"));
        app.sfx(Sfx.FULL);
        Audio.playMusic("boss_mound.wav", 0.5);
        for (int i = 0; i < 6; i++) {
            fx.burst(60 + i * 120, FEET + 10, 8, () -> StoryFx.randomLitter(rng, 20), 300, 1.0, 520);
        }
    }

    private void applyGloom() {
        gloomScene.setOpacity(gloom);
        dark.setOpacity(0.2 + 0.8 * gloom);
        haze.setOpacity(gloom);
        vignette.setOpacity(gloom);
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        if (box.advance()) next.run();
    }

    @Override
    public void onCancel() {
        next.run();
    }
}
