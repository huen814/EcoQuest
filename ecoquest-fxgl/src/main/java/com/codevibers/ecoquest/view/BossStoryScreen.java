package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;
import java.util.List;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Story lines before or after a boss fight: the boss's arena, the Guardian, the boss, and a dialogue box. */
public final class BossStoryScreen extends Screen {

    private static final int FEET = 340;

    private final DialogBox box;
    private final List<String> pages;
    private final Runnable next;
    private final BossDef def;
    private final ImageView playerImg;
    private final ImageView bossImg;
    private double t;

    /** @param showBoss true before the fight (the boss looms), false after it (only the arena and the Guardian). */
    public BossStoryScreen(EcoQuestApp app, BossDef def, List<String> pages, Runnable next, boolean showBoss) {
        super(app);
        this.pages = pages;
        this.next = next;
        this.def = def;

        root.getChildren().add(def.backdrop().startsWith("scene:")
                ? StoryFx.scene(def.backdrop().substring("scene:".length()))
                : Sprites.view(def.backdrop()));
        root.getChildren().add(new Rectangle(Ui.W, Ui.H, Color.web("#0d1a0f", showBoss ? 0.35 : 0.1)));

        Sibling me = app.save().sibling();
        playerImg = Sprites.view(me.sheet(false));      // plain sheet: cutscenes never show shop cosmetics
        StoryFx.stand(playerImg, 140, FEET, 2);

        bossImg = Sprites.view(def.normal());
        bossImg.setFitWidth(def.width() * 1.4);
        bossImg.setFitHeight(def.height() * 1.4);
        bossImg.setLayoutX(Ui.W - bossImg.getFitWidth() - 40);
        bossImg.setLayoutY(FEET - bossImg.getFitHeight() + 10);
        bossImg.setVisible(showBoss);
        root.getChildren().addAll(bossImg, playerImg);

        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().add(box);
    }

    @Override
    public void enter() {
        t = 0;
        box.show(pages);
        app.sfx(bossImg.isVisible() ? Sfx.FULL : Sfx.MISSION);
        if (bossImg.isVisible()) {            // before the fight: its battle music starts now and carries on into the fight
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
        bossImg.setTranslateY(Math.round(Math.sin(t * 2.2) * 3));
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
