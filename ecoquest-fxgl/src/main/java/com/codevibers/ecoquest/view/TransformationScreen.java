package com.codevibers.ecoquest.view;

import java.util.List;
import java.util.Random;

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
    private static final int CHANGE_PAGE = 2;
    private static final double CHANGE_TIME = 3.2;

    private static final List<String> PAGES = List.of(
        "The last drop of the Spirit Bottle hits Queen Muck. Her gown of bags sags, her can-ring bows droop, " +
        "and she sinks onto her throne with a long, quiet sigh.",

        "Queen Muck: Why did I think burying everything in garbage would make anyone happy? I hated cleaning, " +
        "so I made the whole world my dump... and now I can't even walk through my own palace. How illogical I have been!",

        "A soft green light spills from the Spirit Bottle and wraps around her. The bags, cans and wrappers crumble " +
        "into sparkles and the wind sorts them neatly away. The smog lifts, the walls shine, and the carpet blooms green.",

        "Queen Muck: I feel... light. Clean! Guardian, I am so sorry. {sibling} is safe in the tower. " +
        "Come, I will open the door myself, and help the village put everything right.");

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

        queen = Sprites.view("boss/queen_muck.png");
        StoryFx.stand(queen, CX, QUEEN_FEET, QUEEN_SCALE);
        cleanQueen = Sprites.view("boss/queen_muck_clean.png");
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

        flash.setOpacity(0);
        flash.setMouseTransparent(true);
        root.getChildren().addAll(flash, box);
    }


    /** Fills in the kidnapped sibling's name (Lena or Adam, whichever was not picked). */
    private List<String> named(List<String> pages) {
        String n = app.save().sibling().other().name;
        return pages.stream().map(p -> p.replace("{sibling}", n)).toList();
    }

    @Override
    public void enter() {
        t = 0;
        changeT = -1;
        box.show(named(PAGES));
        app.sfx(Sfx.MISSION);
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        fx.update(tpf);

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
