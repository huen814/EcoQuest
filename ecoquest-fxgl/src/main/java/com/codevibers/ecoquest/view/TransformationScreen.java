package com.codevibers.ecoquest.view;

import java.util.List;
import java.util.Random;
import java.util.Set;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public final class TransformationScreen extends Screen {
    private static final double CX = Ui.W / 2.0;
    private static final double QUEEN_FEET = 236, QUEEN_SCALE = 1.7;      // far away on her stage at the end of the long hall
    private static final double GUARDIAN_FEET = 342;                 // low on screen so Queen Muck stays visible above
    private static final int CHANGE_PAGE = 6;                 // the green-light transformation page (unchanged)
    private static final Set<Integer> LAUGH_PAGES = Set.of(5);    // her last "OHOHOHO-" before the change
    private static final double CHANGE_TIME = 3.2;

    private static final List<String> PAGES = List.of(
        "The last drop of the Spirit Bottle's light hits Queen Bijou. Her gown of bags sags and her can-ring bows droop " +
        "as she collapses onto the floor, surrounded by scattered bottle caps and tinsel. " +
        "Her arrogant tone suddenly drops as she pieces together what happened.",

        "Queen Bijou: I... I lost?! Impossible! My impeccable aura, ruined by a mere glowing bottle! " +
        "Sir Filament actually had you, didn't he?!",

        "Queen Bijou: He marched all the way to the village, summoned his ghosts and lightning, and followed my ridiculous " +
        "orders to the letter... yet the only reason you slipped away was that Mother Nature's power overpowered him!",

        "She stares down at the bent tin crown in her hands, the Spirit Bottle's glow still flickering across her gown.",

        "Queen Bijou: Even the earth itself is trying to stop me... and all I did was sit up here hoarding garbage, " +
        "plotting to smother the world in smog, and sending my loyal knight to terrorize the village. " +
        "Well! If I am to rule, I suppose I must clean up this whole mess first!",

        "Queen Bijou: Farewell! OHOHOHO\u2014",

        "The Spirit Bottle's light, still soaking into her since that last drop, swells into a soft green glow and wraps around her. The bags, cans and wrappers crumble " +
        "into sparkles and the wind sorts them neatly away. The smog lifts, the walls shine, and the carpet blooms green.",

        "Queen Bijou: My apologies for the noise. It seems all that smog was clouding my heart as much as the sky. " +
        "Go on now\u2014someone very important is waiting for you. Leave the cleanup to me.");

    private final Runnable next;
    private final DialogBox box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
    private final StoryFx.Particles fx = new StoryFx.Particles(Ui.W, Ui.H);
    private final Random rng = new Random();
    private final ImageView cleanHall;
    private final ImageView queen;
    private final ImageView cleanQueen;
    private final Circle glow = StoryFx.glow(190, Color.web("#d8ffe0", 0.9));
    private final Rectangle flash = new Rectangle(Ui.W, Ui.H, Color.WHITE);
    private double t;
    private double changeT = -1;
    private final Ui.Label laughLabel = new Ui.Label("OHOHOHO!", 26, Color.web("#ff8fb8"));
    private double laughT = -1;
    private int lastPage = -1;

    public TransformationScreen(EcoQuestApp app, Runnable next) {
        super(app);
        this.next = next;

        root.getChildren().add(Sprites.view("backdrop/palace_hall.png"));
        cleanHall = Sprites.view("backdrop/palace_hall_clean.png");
        cleanHall.setOpacity(0);
        root.getChildren().add(cleanHall);

        glow.setTranslateX(CX);
        glow.setTranslateY(140);
        glow.setOpacity(0);
        root.getChildren().add(glow);

        queen = Sprites.view("boss/queen_bijou.png");
        StoryFx.stand(queen, CX, QUEEN_FEET, QUEEN_SCALE);
        cleanQueen = Sprites.view("boss/queen_bijou_clean.png");
        StoryFx.stand(cleanQueen, CX, QUEEN_FEET, QUEEN_SCALE);
        cleanQueen.setOpacity(0);

        ImageView guardian = Sprites.view(app.save().sibling().sheet(false));
        guardian.setSmooth(false);
        guardian.setViewport(new Rectangle2D(0, 3 * 72, Sprites.PX, 72));
        guardian.setFitWidth(Sprites.PX * 2);
        guardian.setFitHeight(72 * 2);
        guardian.setLayoutX(CX - Sprites.PX);
        guardian.setLayoutY(GUARDIAN_FEET - 144);
        root.getChildren().addAll(StoryFx.shadow(CX, QUEEN_FEET, 76), queen, cleanQueen,
                StoryFx.shadow(CX, GUARDIAN_FEET, 90), guardian, fx);

        laughLabel.shadow(Color.web("#2c2748"));
        laughLabel.setLayoutX(CX + 70);
        laughLabel.setLayoutY(60);
        laughLabel.setRotate(-8);
        laughLabel.setOpacity(0);
        laughLabel.setMouseTransparent(true);

        flash.setOpacity(0);
        flash.setMouseTransparent(true);
        root.getChildren().addAll(laughLabel, flash, box);
    }


    /** Fills in the kidnapped sibling's name (Lena or Adam, whichever was not picked). */
    private List<String> named(List<String> pages) {
        String n = app.save().sibling().other().name;
        return pages.stream().map(p -> p.replace("{sibling}", n)).toList();
    }

    /** Her last laugh, the same as in the kidnap cutscene: laughing sprite, "OHOHOHO!" bubble, sound and hearts. */
    private void startLaugh() {
        laughT = 0;
        queen.setImage(Sprites.get("boss/queen_bijou_laugh.png"));
        app.laugh();
        fx.burst(CX, 150, 22, () -> StoryFx.heart(5 + rng.nextInt(5), rng.nextBoolean() ? Color.web("#ff8fb8") : Color.web("#ffd0e4")), 260, 1.3, 120);
        fx.burst(CX, 150, 18, () -> StoryFx.sparkle(4 + rng.nextInt(3), rng.nextBoolean() ? Color.web("#fff6a0") : Color.WHITE), 320, 1.0, 0);
    }

    private void stopLaugh() {
        laughT = -1;
        laughLabel.setOpacity(0);
        queen.setImage(Sprites.get("boss/queen_bijou.png"));
    }

    private static double easeOutBack(double x) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }

    @Override
    public void enter() {
        t = 0;
        changeT = -1;
        laughT = -1;
        lastPage = -1;
        box.show(named(PAGES));
        app.sfx(Sfx.MISSION);
        Audio.playMusic("queen_dialogue.wav", 0.4);
    }

    @Override
    public void exit() { Audio.stopMusic(); }

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        fx.update(tpf);

        if (changeT < 0 && box.page() != lastPage) {
            lastPage = box.page();
            if (LAUGH_PAGES.contains(lastPage)) startLaugh();
        }
        if (laughT >= 0) {
            laughT += tpf;
            double pt = Math.min(1, laughT / 0.4);
            double s = pt >= 1 ? 1 : easeOutBack(pt);
            laughLabel.setScaleX(s);
            laughLabel.setScaleY(s);
            laughLabel.setOpacity(laughT < 2.0 ? 1 : Math.max(0, 1 - (laughT - 2.0) * 2));
            laughLabel.setTranslateY(Math.round(Math.sin(laughT * 14) * 2));      // giggle
            if (laughT > 2.5) stopLaugh();
        }

        if (changeT < 0) {
            queen.setTranslateX(Math.round(Math.sin(t * 22) * 1.5));
            if (box.page() >= CHANGE_PAGE) startChange();
            return;
        }
        changeT += tpf;
        double k = Math.min(1, changeT / CHANGE_TIME);
        double ease = k * k * (3 - 2 * k);
        cleanHall.setOpacity(ease);
        cleanQueen.setOpacity(ease);
        queen.setOpacity(1 - ease);
        queen.setTranslateX(0);
        glow.setOpacity(Math.sin(k * Math.PI) * 0.9);
        glow.setScaleX(1 + 0.1 * Math.sin(t * 4));
        glow.setScaleY(1 + 0.1 * Math.sin(t * 4));
        double peak = changeT < 0.5 ? changeT / 0.5 : Math.max(0, 1 - (changeT - 0.5) / 0.9);
        flash.setOpacity(0.8 * peak);
    }

    private void startChange() {
        changeT = 0;
        if (laughT >= 0) stopLaugh();
        app.sfx(Sfx.WIN);
        fx.converge(36, () -> StoryFx.sparkle(4, Color.web("#d8ffe0")), CX, 150, 1.4, 80);
        fx.burst(CX, 150, 50, () -> StoryFx.sparkle(3 + rng.nextInt(3), Color.web("#fff6a0")), 280, 1.6, 20);
        fx.drift(30, () -> StoryFx.confetti(4, 3, Color.web(rng.nextBoolean() ? "#f4a6c8" : "#fdfcf6")), 0, 16, 18);
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        if (box.advance()) next.run();
    }

    @Override
    public void onCancel() { next.run(); }
}
