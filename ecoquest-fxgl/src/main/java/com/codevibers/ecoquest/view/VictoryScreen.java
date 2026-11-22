package com.codevibers.ecoquest.view;

import java.util.List;
import java.util.Random;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;

import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * Ending cutscene after the boss is defeated: a sunny village in bloom, confetti, fireworks,
 * and both siblings reunited (and the rescued one clean again) under the glowing Spirit Bottle.
 */
public final class VictoryScreen extends Screen {

    private static final double FEET = 318;
    private static final String[] CONFETTI = {"#9fe0cf", "#f4a6c8", "#f7d88a", "#7fb2f2", "#d9566f", "#86dba6"};

    private final DialogBox box;
    private final ImageView playerImg;
    private final ImageView friendImg;
    private final ImageView weaponImg;
    private final javafx.scene.shape.Circle glow = StoryFx.glow(150, Color.web("#9fe0cf", 0.8));
    private final StoryFx.Particles fx = new StoryFx.Particles(Ui.W, Ui.H);
    private final Random rng = new Random();
    private double t;
    private double fireworkTimer;
    private double bloomT = -1;                               // the outskirts bloom on the last page
    private final javafx.scene.Group bloom = new javafx.scene.Group();

    public VictoryScreen(EcoQuestApp app) {
        super(app);

        root.getChildren().add(Sprites.view("backdrop/palace.png"));
        var sun = StoryFx.glow(240, Color.web("#fff3b0", 0.7));
        sun.setTranslateX(Ui.W - 110);
        sun.setTranslateY(40);
        bloom.getChildren().addAll(Sprites.view("backdrop/meadow.png"), sun);
        bloom.setOpacity(0);
        root.getChildren().add(bloom);

        // Both siblings reunited, both clean now.
        Sibling me = app.save().sibling();
        playerImg = Sprites.view(me.sheet(false));   // plain sheet: cutscenes never show shop cosmetics
        friendImg = Sprites.view(me.other().sheet(false));
        StoryFx.stand(playerImg, Ui.W / 2.0 - 70, FEET, 2);
        StoryFx.stand(friendImg, Ui.W / 2.0 + 70, FEET, 2);
        weaponImg = Sprites.view("weapon/bottle.png");
        weaponImg.setFitWidth(72);
        weaponImg.setFitHeight(72);
        weaponImg.setLayoutX((Ui.W - 72) / 2.0);
        weaponImg.setLayoutY(150);
        glow.setTranslateX(Ui.W / 2.0);
        glow.setTranslateY(186);
        root.getChildren().addAll(glow, StoryFx.shadow(Ui.W / 2.0 - 70, FEET, 90), StoryFx.shadow(Ui.W / 2.0 + 70, FEET, 90),
                playerImg, friendImg, weaponImg);

        // Confetti rains down, sparkles and hearts float up.
        fx.drift(70, () -> StoryFx.confetti(5 + rng.nextInt(4), 3 + rng.nextInt(3), Color.web(CONFETTI[rng.nextInt(CONFETTI.length)])), 0, 70, 34);
        fx.drift(16, () -> StoryFx.heart(4, Color.web("#ff8fb8", 0.9)), 0, -22, 16);
        fx.drift(16, () -> StoryFx.sparkle(3, Color.web("#fff6a0", 0.9)), 0, -12, 12);
        root.getChildren().add(fx);

        Ui.Label title = new Ui.Label("VICTORY!", 32, Ui.GOOD);
        title.setLayoutX((Ui.W - title.textWidth()) / 2.0);
        title.setLayoutY(26);
        title.shadow(Color.web("#ffffff", 0.85));
        root.getChildren().add(title);

        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().add(box);
    }

    private List<String> buildPages() {
        String tidy = app.save().sibling().name, messy = app.save().sibling().other().name;
        return List.of(
            "Queen Muck keeps her promise and throws open the tower door herself. " +
            "Outside, the smog is gone for good, and cans, bottles and wrappers lie sorted neatly " +
            "by colour all across the palace grounds!",

            "\"" + tidy.toUpperCase() + "!\" " + messy + " cried, racing out of the tower. " +
            "They threw their arms around " + tidy + " and laughed and laughed. " +
            "\"You sorted all that garbage... just for me?\"",

            "\"Every single piece,\" " + tidy + " grinned, holding up the now-empty bottle. " +
            "The village erupted in cheers. The polluted outskirts bloomed into meadows " +
            "overnight - because clean habits really do change everything. THE END."
        );
    }

    @Override
    public void enter() {
        t = 0;
        bloomT = -1;
        bloom.setOpacity(0);
        box.show(buildPages());
        app.sfx(Sfx.MISSION);
        firework(Ui.W / 2.0, 110);
    }

    private void firework(double x, double y) {
        Color c = Color.web(CONFETTI[rng.nextInt(CONFETTI.length)]);
        fx.burst(x, y, 22, () -> StoryFx.sparkle(3 + rng.nextInt(3), c), 230, 1.0, 60);
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        fx.update(tpf);
        if (bloomT >= 0) {
            bloomT += tpf;
            bloom.setOpacity(Math.min(1, bloomT / 1.6));
        }
        playerImg.setTranslateY(-Math.round(Math.abs(Math.sin(t * 2.6)) * 6));
        friendImg.setTranslateY(-Math.round(Math.abs(Math.sin(t * 2.6 + 1)) * 6));
        weaponImg.setTranslateY(Math.round(Math.sin(t * 3) * 4));
        weaponImg.setOpacity(0.7 + 0.3 * Math.abs(Math.sin(t * 2)));
        double pulse = 1 + 0.08 * Math.sin(t * 3);
        glow.setScaleX(pulse);
        glow.setScaleY(pulse);

        // fireworks over the village
        fireworkTimer += tpf;
        if (fireworkTimer > 1.1) {
            fireworkTimer = 0;
            firework(80 + rng.nextDouble() * (Ui.W - 160), 70 + rng.nextDouble() * 90);
        }
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        int before = box.page();
        boolean done = box.advance();
        if (box.page() != before) {
            // the reunion: a burst of hearts between the siblings
            if (box.page() == 1) fx.burst(Ui.W / 2.0, 210, 26, () -> StoryFx.heart(5 + rng.nextInt(4), Color.web("#ff8fb8")), 200, 1.4, -30);
            if (box.page() == 2) { bloomT = 0; firework(Ui.W / 2.0 - 150, 120); firework(Ui.W / 2.0 + 150, 120); }
        }
        if (done) app.showMainMenu();
    }

    @Override
    public void onCancel() { app.showMainMenu(); }
}
