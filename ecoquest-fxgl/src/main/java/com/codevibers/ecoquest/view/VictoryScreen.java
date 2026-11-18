package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Ending cutscene after the boss is defeated.
 * Shows both characters reunited plus the final credit card.
 */
public final class VictoryScreen extends Screen {

    private static final int PX = Sprites.PX;
    private static final int SH = 72;

    private final DialogBox box;
    private final ImageView playerImg;
    private final ImageView friendImg;
    private final ImageView weaponImg;
    private double t;
    private final List<Confetti> confetti = new java.util.ArrayList<>();

    private record Confetti(double x, double speed, double phase, Color col) { }

    public VictoryScreen(EcoQuestApp app) {
        super(app);

        root.getChildren().add(Sprites.view("backdrop/house.png"));
        Rectangle overlay = new Rectangle(Ui.W, Ui.H, Color.rgb(10, 24, 10, 0.4));
        root.getChildren().add(overlay);

        // Seed confetti
        java.util.Random rng = new java.util.Random(42);
        String[] cols = {"#9fe0cf","#f4a6c8","#f7d88a","#7fb2f2","#d9566f","#86dba6"};
        for (int i = 0; i < 40; i++) {
            confetti.add(new Confetti(
                rng.nextDouble() * Ui.W,
                50 + rng.nextDouble() * 80,
                rng.nextDouble() * Math.PI * 2,
                Color.web(cols[rng.nextInt(cols.length)])
            ));
        }

        // Characters side by side, now both clean
        Sibling me = app.save().sibling();
        playerImg = new ImageView(Wardrobe.sheet(app.save(), me, false));
        friendImg = Sprites.view(me.other().sheet(false));   // rescued: clean again
        weaponImg = Sprites.view("weapon/bottle.png");

        setup(playerImg, Ui.W / 2.0 - 80, 170);
        setup(friendImg, Ui.W / 2.0 + 20,  170);

        weaponImg.setFitWidth(64); weaponImg.setFitHeight(64);
        weaponImg.setLayoutX((Ui.W - 64) / 2.0);
        weaponImg.setLayoutY(100);
        root.getChildren().add(weaponImg);

        Ui.Label title = new Ui.Label("VICTORY!", 32, Ui.GOOD);
        title.setLayoutX((Ui.W - title.textWidth()) / 2.0);
        title.setLayoutY(26);
        root.getChildren().add(title);

        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().add(box);
    }

    private void setup(ImageView iv, double x, double y) {
        iv.setViewport(new Rectangle2D(0, 0, PX, SH));
        iv.setLayoutX(x); iv.setLayoutY(y);
        root.getChildren().add(iv);
    }

    private List<String> buildPages() {
        return List.of(
            "The Spirit Bottle explodes with clean green light! Queen Muck " +
            "lets out a dramatic shriek as the recycled energy melts her gown of bags " +
            "piece by piece - cans, bottles and wrappers raining harmlessly " +
            "to the ground, sorted neatly by colour!",

            "\"SIBLING!\" the messy sibling cried, tumbling free as the last tendril " +
            "of plastic crumbled. They threw their arms around the tidy sibling and laughed and laughed. " +
            "\"You sorted all that garbage... just for me?\"",

            "\"Every single piece,\" the tidy sibling grinned, holding up the now-empty bottle. " +
            "The village erupted in cheers. The polluted outskirts bloomed into meadows " +
            "overnight - because clean habits really do change everything. THE END."
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
        playerImg.setTranslateY(Math.round(Math.sin(t * 2.5) * 3));
        friendImg.setTranslateY(Math.round(Math.sin(t * 2.5 + 1) * 3));
        weaponImg.setTranslateY(Math.round(Math.sin(t * 3) * 4));
        weaponImg.setOpacity(0.6 + 0.4 * Math.abs(Math.sin(t * 2)));

        // Draw confetti as simple rectangles via node reuse would be costly;
        // we cheat by moving existing coloured labels around off-screen.
        // (A proper particle system would use Canvas; we keep it light here.)
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        boolean done = box.advance();
        if (done) {
            app.showMainMenu();
        }
    }

    @Override
    public void onCancel() { app.showMainMenu(); }
}
