package com.codevibers.ecoquest.view;

import java.util.List;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.story.StoryState;

import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Short "thank you" cutscene shown after each campaign level.
 * A villager hands over a piece of the Spirit Bottle (or the full weapon on the final level).
 */
public final class VillagerRewardScreen extends Screen {

    private static final int PX = Sprites.PX;
    private static final int SH = 72;

    private final int levelIndex;
    private final boolean isFinal;

    private final DialogBox box;
    private final ImageView npcImg;
    private final ImageView weaponImg;
    private double t;

    public VillagerRewardScreen(EcoQuestApp app, int levelIndex) {
        super(app);
        this.levelIndex = levelIndex;
        this.isFinal = (levelIndex >= 9);

        root.getChildren().add(Sprites.view("backdrop/" + sceneFor(levelIndex) + ".png"));
        Rectangle dim = new Rectangle(Ui.W, Ui.H, Color.rgb(0, 0, 0, 0.4));
        root.getChildren().add(dim);

        npcImg    = Sprites.view(StoryState.npcSprite(levelIndex) + ".png");
        weaponImg = Sprites.view("weapon/bottle.png");

        // NPC - single 48x48 frame (npc sprites are saved as single frames)
        npcImg.setFitWidth(96); npcImg.setFitHeight(96);
        npcImg.setPreserveRatio(true);
        npcImg.setLayoutX(100);
        npcImg.setLayoutY(180);
        root.getChildren().add(npcImg);

        // Weapon icon - shown after all 10 levels or growing with each level
        weaponImg.setOpacity(isFinal ? 1.0 : 0.3 + 0.07 * levelIndex);
        weaponImg.setFitWidth(96); weaponImg.setFitHeight(96);
        weaponImg.setPreserveRatio(true);
        weaponImg.setLayoutX(Ui.W - 180);
        weaponImg.setLayoutY(180);
        root.getChildren().add(weaponImg);

        // Weapon charge label
        Ui.Label charge;
        if (isFinal) {
            charge = new Ui.Label("SPIRIT BOTTLE: FULLY CHARGED!", 11, Ui.GOOD);
        } else {
            charge = new Ui.Label("SPIRIT BOTTLE: " + (levelIndex + 1) + " / 10 CHARGES", 11, Ui.ACCENT);
        }
        charge.setLayoutX((Ui.W - charge.textWidth()) / 2.0);
        charge.setLayoutY(300);
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

        if (isFinal) {
            return List.of(
                npc + ": \"You have sorted trash across all our lands! " +
                "The Spirit Bottle is fully charged with the pure energy of recycling! " +
                "Now go - Queen Muck's palace is just ahead!\"",
                "The bottle glows a brilliant green and blue. " +
                "You raise it high and run toward the final level, " +
                "determined to rescue your sibling once and for all!"
            );
        }
        return List.of(
            npc + ": \"Thank you! You kept our " +
            sceneFor(levelIndex).toUpperCase() + " clean! " +
            "Here is a charge of the Spirit Bottle to help you on your quest. " +
            "Your sibling is counting on you - keep going!\"",
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
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        npcImg.setTranslateY(Math.round(Math.sin(t * 2.2) * 2));
        weaponImg.setTranslateY(Math.round(Math.sin(t * 2.5 + 1) * 3));
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        boolean done = box.advance();
        if (done) {
            if (isFinal) {
                app.showBossFight();
            } else {
                // Continue to next level
                int next = levelIndex + 1;
                app.startLevel(com.codevibers.ecoquest.model.Levels.CAMPAIGN.get(next), next);
            }
        }
    }

    @Override
    public void onCancel() {
        // Skip reward screen
        if (isFinal) {
            app.showBossFight();
        } else {
            int next = levelIndex + 1;
            app.startLevel(com.codevibers.ecoquest.model.Levels.CAMPAIGN.get(next), next);
        }
    }
}
