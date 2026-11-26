package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;
import java.util.List;
import java.util.Random;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Story lines before or after a boss fight: the boss's arena, the Guardian, the boss, and a dialogue box.
 * The Muck Mound gets extra effects: before the fight the trash erupts from the ground, the mound drags itself up and the
 * world turns gloomy; after it, the light fades the mound out of existence and the world returns to normal.
 */
public final class BossStoryScreen extends Screen {

    private static final int FEET = 340;

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
    private Rectangle dark, haze;
    private Circle vanishGlow;
    private double t, mt, lastBurst, gloom;

    /** @param showBoss true before the fight (the boss looms), false after it (only the arena and the Guardian). */
    public BossStoryScreen(EcoQuestApp app, BossDef def, List<String> pages, Runnable next, boolean showBoss) {
        super(app);
        this.pages = pages;
        this.next = next;
        this.def = def;
        this.before = showBoss;
        this.mound = def.kind() == BossDef.Kind.MOUND;

        Node arena = def.backdrop().startsWith("scene:")
                ? StoryFx.scene(def.backdrop().substring("scene:".length()))
                : Sprites.view(def.backdrop());
        if (mound) {
            root.getChildren().add(StoryFx.scene("river"));         // the clean world
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
        bossImg.setVisible(showBoss || mound);          // the mound is still there at the start of its farewell scene
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

        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().add(box);
    }

    @Override
    public void enter() {
        t = 0;
        mt = 0;
        lastBurst = 0;
        box.show(pages);
        app.sfx(before ? Sfx.FULL : Sfx.MISSION);
        if (before) {                         // before the fight: its battle music starts now and carries on into the fight
            switch (def.kind()) {
                case MOUND -> Audio.playMusic("boss_mound.wav", 0.5);
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
    }

    private void updateMound(double tpf) {
        mt += tpf;
        double bossX = bossImg.getLayoutX() + bossImg.getFitWidth() / 2, ground = FEET + 10, h = bossImg.getFitHeight();
        if (before) {
            gloom = Math.min(1, Math.max(0, (mt - 0.3) / 2.6));
            double k = Math.max(0, Math.min(1, (mt - 0.2) / 2.6)), e = 1 - Math.pow(1 - k, 3);
            double sink = (1 - e) * (h + 24);
            bossImg.setTranslateY(sink + (k >= 1 ? Math.round(Math.sin(t * 2.2) * 3) : 0));
            bossClip.setX(0);
            bossClip.setY(0);
            bossClip.setWidth(bossImg.getFitWidth());
            bossClip.setHeight(Math.max(0, h - sink + 12));
            if (k < 1) {
                if (mt - lastBurst > 0.09) {
                    lastBurst = mt;
                    fx.burst(bossX + (rng.nextDouble() - 0.5) * 120, ground, 3, () -> StoryFx.randomLitter(rng, 20), 150, 0.9, 520);
                    if (rng.nextDouble() < 0.4) app.sfx(Sfx.DUMP);
                }
                root.setTranslateX((rng.nextDouble() - 0.5) * 6);
                root.setTranslateY((rng.nextDouble() - 0.5) * 4);
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
