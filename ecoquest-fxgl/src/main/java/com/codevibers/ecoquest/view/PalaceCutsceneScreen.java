package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.List;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;

public final class PalaceCutsceneScreen extends Screen {
    private static final double CX = Ui.W / 2.0;
    private static final double PORTAL_Y = 396;
    private static final double GATE_START = 462, GATE_END = 322;
    private static final double GATE_TIME = 3.0;
    private static final double HALL_END = 342;                  // where the Guardian stops: low on screen so Queen Muck stays visible above
    private static final double HALL_OFFSET = 190;
    private static final double WALK_SPEED = 90;
    private static final double QUEEN_FEET = 236, QUEEN_SCALE = 1.7;      // the hall is long now, so she stands far away on her stage

    private static final List<String> INTRO_PAGES = List.of(
        "Mother Nature's light opens a spinning portal. It drops the Guardian onto the pink carpet in front of " +
        "Queen Muck's palace, where the smog hangs thick and the great gate towers overhead.",

        "Sprout: {sibling} is somewhere inside, Guardian! Get through the gate, cross the great hall, " +
        "and stop Queen Muck before she buries the whole country!");

    private static final List<String> TALK_PAGES = List.of(
        "The gate groans open. Beyond it is a long hall of cardboard walls and bottle-glass windows, " +
        "and at the very end, on a throne heaped from garbage, sits Queen Muck.",

        "Queen Muck: HO HO HO! So the little Guardian finally walked all the way here! Do you like my palace? " +
        "Everything in it is something your village threw away!",

        "Queen Muck: {sibling} is safe in my tower, with plenty of rubbish for company. " +
        "You want them back? Then sort THIS!");

    private enum Phase { INTRO, WALK_GATE, FLASH, WALK_HALL, TALK }

    private final Runnable next;
    private final DialogBox box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
    private final StoryFx.Particles fx = new StoryFx.Particles(Ui.W, Ui.H);
    private final Group gateWorld = new Group();
    private final Group hallWorld = new Group();
    private final Rectangle flash = new Rectangle(Ui.W, Ui.H, Color.WHITE);
    private final Group portal = new Group();
    private final PortalView portalSprite = new PortalView();
    private final ImageView gatePlayer;
    private final ImageView hallPlayer;
    private final ImageView queen;
    private final Ellipse gateShadow = StoryFx.shadow(CX, GATE_START, 90);
    private final List<ImageView> imps = new ArrayList<>();

    private Phase phase = Phase.INTRO;
    private double t;
    private double walkT;
    private double flashT;
    private boolean swapped;
    private double hallOffset = HALL_OFFSET;

    public PalaceCutsceneScreen(EcoQuestApp app, Runnable next) {
        super(app);
        this.next = next;
        String sheet = app.save().sibling().sheet(false);

        gateWorld.getChildren().add(Sprites.view("backdrop/palace.png"));
        portal.getChildren().add(portalSprite);
        portal.setTranslateX(CX);
        portal.setTranslateY(PORTAL_Y);
        gatePlayer = Sprites.view(sheet);
        place(gatePlayer, GATE_START, 2, 0);
        gateWorld.getChildren().addAll(portal, gateShadow, gatePlayer);

        hallWorld.getChildren().add(Sprites.view("backdrop/palace_hall.png"));
        // the hall's minions (the four sorting-room guards) are painted into the backdrop itself
        queen = Sprites.view("boss/queen_muck.png");
        StoryFx.stand(queen, CX, QUEEN_FEET, QUEEN_SCALE);
        hallPlayer = Sprites.view(sheet);
        place(hallPlayer, HALL_END, 2, 0);
        hallPlayer.setTranslateY(HALL_OFFSET);
        hallWorld.getChildren().addAll(StoryFx.shadow(CX, QUEEN_FEET, 76), queen, hallPlayer);
        hallWorld.setVisible(false);

        fx.drift(14, () -> StoryFx.dot(1.5, Color.web("#d8c8d8", 0.6)), 0, -8, 14);
        flash.setOpacity(0);
        flash.setMouseTransparent(true);
        root.getChildren().addAll(gateWorld, hallWorld, fx, flash, box);
    }

    private static void place(ImageView iv, double feet, double scale, int frame) {
        iv.setSmooth(false);
        iv.setViewport(new Rectangle2D(frame * Sprites.PX, 3 * 72, Sprites.PX, 72));
        iv.setFitWidth(Sprites.PX * scale);
        iv.setFitHeight(72 * scale);
        iv.setLayoutX(CX - Sprites.PX * scale / 2);
        iv.setLayoutY(feet - 72 * scale);
    }


    /** Fills in the kidnapped sibling's name (Lena or Adam, whichever was not picked). */
    private List<String> named(List<String> pages) {
        String n = app.save().sibling().other().name;
        return pages.stream().map(p -> p.replace("{sibling}", n)).toList();
    }

    @Override
    public void enter() {
        t = 0;
        box.show(named(INTRO_PAGES));
        app.sfx(Sfx.MISSION);
        fx.converge(12, () -> StoryFx.sparkle(3, Color.web("#9fe0cf")), CX, PORTAL_Y, 1.1, 60);
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        fx.update(tpf);
        portalSprite.update(t);
        for (int i = 0; i < imps.size(); i++) imps.get(i).setTranslateY(-Math.round(Math.abs(Math.sin(t * 2 + i * 1.7)) * 4));

        switch (phase) {
            case WALK_GATE -> {
                walkT += tpf;
                double k = Math.min(1, walkT / GATE_TIME);
                double feet = GATE_START + (GATE_END - GATE_START) * k;
                double scale = 2.0 - 0.6 * k;
                place(gatePlayer, feet, scale, k >= 1 ? 0 : (int) (walkT / 0.16) % 4);
                gateShadow.setCenterY(feet - 2);
                gateShadow.setRadiusX(22.5 * scale);
                gateShadow.setRadiusY(6.4 * scale);
                portal.setOpacity(Math.max(0, 1 - k * 1.6));
                if (k >= 1) {
                    phase = Phase.FLASH;
                    flashT = 0;
                    app.sfx(Sfx.FULL);
                }
            }
            case FLASH -> {
                flashT += tpf;
                if (!swapped) {
                    flash.setOpacity(Math.min(1, flashT / 0.5));
                    if (flashT >= 0.5) {
                        gateWorld.setVisible(false);
                        hallWorld.setVisible(true);
                        swapped = true;
                        flashT = 0;
                        phase = Phase.WALK_HALL;
                    }
                }
            }
            case WALK_HALL -> {
                walkT += tpf;
                hallOffset = Math.max(0, hallOffset - WALK_SPEED * tpf);
                hallPlayer.setTranslateY(Math.round(hallOffset));
                place(hallPlayer, HALL_END, 2, hallOffset <= 0 ? 0 : (int) (walkT / 0.16) % 4);
                if (hallOffset <= 0) {
                    phase = Phase.TALK;
                    box.show(named(TALK_PAGES));
                    app.sfx(Sfx.FULL);
                }
            }
            default -> { }
        }
        if (swapped && flash.getOpacity() > 0 && phase != Phase.FLASH) {
            flashT += tpf;
            flash.setOpacity(Math.max(0, 1 - flashT / 0.7));
        }
    }

    @Override
    public void onConfirm() {
        switch (phase) {
            case INTRO -> {
                app.sfx(Sfx.SELECT);
                if (box.advance()) {
                    phase = Phase.WALK_GATE;
                    walkT = 0;
                }
            }
            case WALK_GATE -> walkT = GATE_TIME;
            case WALK_HALL -> hallOffset = 0;
            case TALK -> {
                app.sfx(Sfx.SELECT);
                if (box.advance()) next.run();
            }
            default -> { }
        }
    }

    @Override
    public void onCancel() { next.run(); }
}
