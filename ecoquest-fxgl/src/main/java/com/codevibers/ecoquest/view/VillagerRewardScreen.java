package com.codevibers.ecoquest.view;

import java.util.List;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.story.StoryState;

import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * Short "thank you" cutscene shown after each campaign level.
 * A villager hands over a piece of the Spirit Bottle (or the full weapon on the final level).
 */
public final class VillagerRewardScreen extends Screen {

    private static final double FEET = 318;
    private static final double BOTTLE_X = 520, BOTTLE_Y = 210;

    private final int levelIndex;
    private final boolean isFinal;

    private final DialogBox box;
    private final ImageView npcImg;
    private final ImageView weaponImg;
    private final javafx.scene.shape.Circle glow;
    private final StoryFx.Particles fx = new StoryFx.Particles(Ui.W, Ui.H);
    private final java.util.Random rng = new java.util.Random();
    private double t;

    public VillagerRewardScreen(EcoQuestApp app, int levelIndex) {
        super(app);
        this.levelIndex = levelIndex;
        this.isFinal = (levelIndex >= 9);

        // Full scene for the level just cleaned (skyline + ground + road), warm light, soft vignette.
        root.getChildren().add(StoryFx.scene(sceneFor(levelIndex)));
        var sun = StoryFx.glow(220, Color.web("#fff3b0", 0.55));
        sun.setTranslateX(Ui.W - 110);
        sun.setTranslateY(36);
        root.getChildren().addAll(sun, StoryFx.vignette(Color.web("#1e0f30", 0.35)));

        npcImg    = Sprites.view(StoryState.npcSprite(levelIndex) + ".png");
        weaponImg = Sprites.view("weapon/bottle.png");

        // The villager stands on the road, big enough to read.
        StoryFx.stand(npcImg, 190, FEET, 2);
        root.getChildren().addAll(StoryFx.shadow(190, FEET, 90), npcImg);

        // Spirit Bottle: floats on the right inside a pulsing glow that grows with every charge.
        glow = StoryFx.glow(120 + levelIndex * 6, isFinal ? Color.web("#9fe0cf", 0.9) : Color.web("#9fe0cf", 0.55));
        glow.setTranslateX(BOTTLE_X);
        glow.setTranslateY(BOTTLE_Y);
        root.getChildren().add(glow);
        weaponImg.setOpacity(isFinal ? 1.0 : 0.45 + 0.055 * levelIndex);
        weaponImg.setFitWidth(110);
        weaponImg.setFitHeight(110);
        weaponImg.setPreserveRatio(true);
        weaponImg.setLayoutX(BOTTLE_X - 55);
        weaponImg.setLayoutY(BOTTLE_Y - 55);
        root.getChildren().add(weaponImg);

        // Sparkles rise around the bottle; more of them the more charged it is.
        fx.driftIn(8 + levelIndex * 2, () -> StoryFx.sparkle(3, Color.web("#fff6a0", 0.9)),
                BOTTLE_X - 120, 120, BOTTLE_X + 120, 300, -22, 14);
        fx.driftIn(10, () -> StoryFx.sparkle(2.5, Color.web("#ffffff", 0.7)), 20, 150, 700, 320, -8, 12);
        root.getChildren().add(fx);

        Ui.Label charge;
        if (isFinal) {
            charge = new Ui.Label("SPIRIT BOTTLE: FULLY CHARGED!", 11, Ui.GOOD);
        } else {
            charge = new Ui.Label("SPIRIT BOTTLE: " + (levelIndex + 1) + " / 10 CHARGES", 11, Ui.ACCENT);
        }
        charge.setLayoutX(BOTTLE_X - charge.textWidth() / 2.0);
        charge.setLayoutY(BOTTLE_Y + 78);
        charge.shadow(Color.web("#ffffff", 0.85));
        root.getChildren().add(charge);

        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().add(box);
    }

    private static String sceneFor(int idx) {
        String[] scenes = {"house","park","river","ind","market","beach","dump","arctic","jungle","city"};
        return scenes[Math.min(idx, scenes.length - 1)];
    }

    private List<String> buildPages() {
        String npcNames[] = {"Aling Nena","Mang Ben","Tatay Ramon","Ate Rosa","Lola Remy",
                             "Kuya Jun","Mang Dodoy","Dr. Frost","Ka Tomas","Mayor Cruz"};
        String npc = npcNames[Math.min(levelIndex, npcNames.length - 1)];
        String messy = app.save().sibling().other().name;

        if (isFinal) {
            return List.of(
                npc + ": \"You have sorted trash across all our lands! " +
                "The Spirit Bottle is fully charged with the pure energy of recycling! " +
                "Now go - Queen Muck's palace is just ahead!\"",
                "The bottle glows a brilliant green and blue. " +
                "You raise it high and run toward the final level, " +
                "determined to rescue " + messy + " once and for all!"
            );
        }
        return List.of(
            npc + ": \"Thank you! You kept our " +
            sceneFor(levelIndex).toUpperCase() + " clean! " +
            "Here is a charge of the Spirit Bottle to help you on your quest. " +
            messy + " is counting on you - keep going!\"",
            "The bottle absorbs another flash of recycled energy. " +
            "You nod and head to the next area. " +
            (levelIndex < 8 ? "There are still more people who need help!" : "Queen Muck's palace is almost in reach!")
        );
    }

    @Override
    public void enter() {
        t = 0;
        box.show(buildPages());
        app.sfx(Sfx.MISSION);
        Audio.playMusic("stage_clear_npc.wav", 0.4);
        // thank-you hearts over the villager and a flash of light into the bottle
        fx.burst(190, FEET - 150, 10, () -> StoryFx.heart(5, Color.web("#ff8fb8")), 90, 1.4, -40);
        fx.burst(BOTTLE_X, BOTTLE_Y, isFinal ? 40 : 16, () -> StoryFx.sparkle(4, Color.web("#9fe0cf")), isFinal ? 320 : 180, 1.1, 0);
        fx.converge(10, () -> StoryFx.sparkle(3, Color.web("#86dba6")), BOTTLE_X, BOTTLE_Y, 0.9, 50);
    }

    @Override
    public void exit() { Audio.stopMusic(); }

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        fx.update(tpf);
        npcImg.setTranslateY(-Math.round(Math.abs(Math.sin(t * 2.4)) * 5));   // happy little hops
        weaponImg.setTranslateY(Math.round(Math.sin(t * 2.5 + 1) * 4));
        double pulse = 1 + 0.08 * Math.sin(t * 3);
        glow.setScaleX(pulse);
        glow.setScaleY(pulse);
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        boolean done = box.advance();
        if (done) proceed();
    }

    /** After the final level comes the last boss; after some levels a boss stands in the way; otherwise the next level. */
    private void proceed() {
        if (isFinal) {
            app.showGroveCutscene(app::showBossFight);
            return;
        }
        int next = levelIndex + 1;
        Runnable nextLevel = () -> app.startLevel(com.codevibers.ecoquest.model.Levels.CAMPAIGN.get(next), next);
        BossDef boss = BossDef.after(levelIndex);
        if (boss != null) app.showBoss(boss, nextLevel);
        else nextLevel.run();
    }

    @Override
    public void onCancel() {
        // Skip reward screen
        proceed();
    }
}
