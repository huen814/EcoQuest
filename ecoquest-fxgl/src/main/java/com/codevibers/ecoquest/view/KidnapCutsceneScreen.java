package com.codevibers.ecoquest.view;

import java.util.List;
import java.util.Random;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;
import com.codevibers.ecoquest.story.StoryState;

import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;

/**
 * Cutscene after the player picks their hero (and the intro):
 *   page 1 - the tidy sibling (you) finds the messy sibling's room buried in garbage
 *   page 2 - the garbage is sucked together, the screen shakes, flash! QUEEN MUCK bursts out laughing "HO HO HO!"
 *   page 3 - she scoops up the messy sibling and you are left with a quest
 * Advances on confirm; skips on cancel. Either way it ends by running the callback it was given.
 */
public final class KidnapCutsceneScreen extends Screen {

    private static final double FEET = 318;
    private static final double SCALE = 2;
    private static final double YOU_X = 150, FRIEND_X = 350;
    private static final double QUEEN_X = 560, QUEEN_FEET = 338, QUEEN_W = 144, QUEEN_H = 216;   // her 48x72 sprite at 3x
    private static final double QUEEN_MID_Y = QUEEN_FEET - QUEEN_H / 2;
    private static final double REVEAL_AT = 1.25;   // seconds of rumbling before she appears

    private final Runnable next;
    private final DialogBox box;
    private final Random rng = new Random();

    private final Group world = new Group();          // everything that shakes
    private final StoryFx.Particles fx = new StoryFx.Particles(Ui.W, Ui.H);
    private final ImageView playerImg;
    private final ImageView friendImg;                // the messy sibling
    private final Ellipse friendShadow;
    private final ImageView queenImg;
    private final ImageView holeImg;                  // the room after she smashed out through the wall
    private final Circle aura = StoryFx.glow(210, Color.web("#ff8fc0", 0.85));
    private final Group queenPile;                    // the heap she rises from
    private final Rectangle dark = new Rectangle(Ui.W, Ui.H, Color.web("#1e0f30"));
    private final Rectangle flash = new Rectangle(Ui.W, Ui.H, Color.WHITE);
    private final Ui.Label laughLabel = new Ui.Label("HO HO HO!", 26, Color.web("#ff8fb8"));
    private final Ui.Label exclaim = new Ui.Label("!", 22, Ui.BAD);

    private double t;
    private double queenT = -1;       // seconds since the rumble began (-1 = not started)
    private boolean revealed;
    private double revealT;
    private double shake;
    private double flashAlpha;
    private double laughT = -1;
    private double holeT = -1;        // fade-in of the smashed wall on page three
    private double exitT = -99;       // Queen Muck prancing off through the hole
    private double heartTimer;
    private double grabT = -1;
    private boolean grabDone;
    private double trailTimer;

    public KidnapCutsceneScreen(EcoQuestApp app, Runnable next) {
        super(app);
        this.next = next;
        Sibling me = app.save().sibling();

        // The messy room, litter all over, a big heap where the queen will rise; page three swaps in the smashed wall.
        world.getChildren().add(Sprites.view("backdrop/room.png"));
        holeImg = Sprites.view("backdrop/room_hole.png");
        holeImg.setOpacity(0);
        world.getChildren().add(holeImg);
        world.getChildren().add(StoryFx.litter(3, 20, 700, 262, 332, 46, 22, 44));
        aura.setTranslateX(QUEEN_X);
        aura.setTranslateY(QUEEN_MID_Y);
        aura.setOpacity(0);
        world.getChildren().add(aura);

        queenImg = Sprites.view("boss/queen_muck.png");
        queenImg.setSmooth(false);
        queenImg.setFitWidth(QUEEN_W);
        queenImg.setFitHeight(QUEEN_H);
        queenImg.setLayoutX(QUEEN_X - QUEEN_W / 2);
        queenImg.setLayoutY(QUEEN_FEET - QUEEN_H);
        queenImg.setVisible(false);
        queenPile = StoryFx.litter(9, 450, 670, 304, 346, 14, 26, 44);
        world.getChildren().addAll(queenImg, queenPile);

        playerImg = Sprites.view(me.sheet(false));   // plain sheet: cutscenes never show shop cosmetics
        friendImg = Sprites.view(StoryState.messySheet(me));
        StoryFx.stand(playerImg, YOU_X, FEET, SCALE);
        StoryFx.stand(friendImg, FRIEND_X, FEET, SCALE);
        friendShadow = StoryFx.shadow(FRIEND_X, FEET, 90);
        world.getChildren().addAll(StoryFx.shadow(YOU_X, FEET, 90), friendShadow, playerImg, friendImg);

        // Ambient life in the room: flies, stink, dust.
        fx.driftIn(14, () -> StoryFx.dot(1.6, Color.web("#2a2a30")), 20, 180, 700, 335, -2, 50);
        fx.driftIn(12, () -> StoryFx.dot(5, Color.web("#86a04a", 0.33)), 30, 170, 690, 330, -16, 10);
        fx.driftIn(12, () -> StoryFx.sparkle(2.5, Color.web("#e8e0c0", 0.6)), 30, 150, 690, 330, -6, 14);
        world.getChildren().add(fx);

        dark.setOpacity(0.22);
        dark.setMouseTransparent(true);
        flash.setOpacity(0);
        flash.setMouseTransparent(true);

        laughLabel.shadow(Color.web("#2c2748"));
        laughLabel.setLayoutX(QUEEN_X - 330);
        laughLabel.setLayoutY(70);
        laughLabel.setRotate(-8);
        laughLabel.setOpacity(0);
        exclaim.setLayoutX(YOU_X - 6);
        exclaim.setLayoutY(FEET - 72 * SCALE - 30);
        exclaim.setOpacity(0);

        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().addAll(world, dark, flash, laughLabel, exclaim, box);

        Ui.Label skip = new Ui.Label("X : SKIP", 9, Ui.FRAME_IN);
        skip.setLayoutX(Ui.W - skip.textWidth() - 14);
        skip.setLayoutY(Ui.H - 148);
        root.getChildren().add(skip);
    }

    @Override
    public void enter() {
        t = 0;
        queenT = -1;
        revealed = false;
        grabT = -1;
        grabDone = false;
        holeT = -1;
        exitT = -99;
        holeImg.setOpacity(0);
        shake = 0;
        box.show(List.of(getPage(0), getPage(1), getPage(2)));
        Audio.playMusic("queen_dialogue.wav", 0.4);
    }

    @Override
    public void exit() { Audio.stopMusic(); }

    private String getPage(int i) {
        String tidy = app.save().sibling().name, messy = app.save().sibling().other().name;
        return switch (i) {
            case 0 -> tidy + " knocks on " + messy + "'s door. No answer. " +
                      "Pushing it open, they gasp - the room is ankle-deep in rotting garbage. " +
                      "Empty bottles, old takeaway boxes and banana peels are piled to the ceiling!";
            case 1 -> "The rubbish TREMBLES. Cans clatter, bags inflate, and from the heap " +
                      "rises a glittering TRASH QUEEN - QUEEN MUCK! A crown of bottle caps, a gown of " +
                      "garbage bags, and a hand over her grinning mouth. \"Ho ho ho ho~! You simply MUST " +
                      "stay with me, darling!\" She scoops up " + messy + " in a bouquet of tangled plastic!";
            default -> "\"" + messy.toUpperCase() + "! NOOO!\" " + tidy + " shouts, but Queen Muck has already " +
                       "pranced through the wall, laughing \"ho ho ho!\", toward her palace in the polluted outskirts. " +
                       "Sort trash, earn the SPIRIT BOTTLE, and go rescue " + messy + "!";
        };
    }

    // ------------------------------------------------------------------ the entrance

    /** The room starts to tremble and every scrap of garbage is pulled toward the heap. */
    private void startEntrance() {
        if (queenT >= 0) return;
        queenT = 0;
        app.sfx(Sfx.DUMP);
        fx.converge(34, () -> StoryFx.randomLitter(rng, 26), QUEEN_X, QUEEN_MID_Y + 20, 1.3, 90);
        fx.converge(16, () -> StoryFx.sparkle(4, Color.web("#ff8fc0")), QUEEN_X, QUEEN_MID_Y, 1.2, 60);
    }

    /** Flash, shake, hearts and trash burst out, the queen pops in and laughs. */
    private void reveal() {
        if (revealed) return;
        startEntrance();
        revealed = true;
        revealT = 0;
        queenImg.setVisible(true);
        queenPile.setOpacity(0.6);
        flashAlpha = 1;
        shake = 9;
        exclaim.setOpacity(1);
        app.sfx(Sfx.DUMP);
        startLaugh();
        fx.burst(QUEEN_X, QUEEN_MID_Y, 30, () -> StoryFx.heart(5 + rng.nextInt(5), rng.nextBoolean() ? Color.web("#ff8fb8") : Color.web("#ffd0e4")), 280, 1.3, 120);
        fx.burst(QUEEN_X, QUEEN_MID_Y, 26, () -> StoryFx.sparkle(4 + rng.nextInt(3), rng.nextBoolean() ? Color.web("#fff6a0") : Color.WHITE), 340, 1.0, 0);
        fx.burst(QUEEN_X, QUEEN_MID_Y + 40, 18, () -> StoryFx.randomLitter(rng, 24), 320, 1.1, 420);
    }

    private void startLaugh() {
        laughT = 0;
        queenImg.setImage(Sprites.get("boss/queen_muck_laugh.png"));
        app.laugh();
    }

    /** She grabs the messy sibling, who floats up to her hand. */
    private void startGrab() {
        if (grabT >= 0) return;
        if (!revealed) reveal();
        grabT = 0;
        app.sfx(Sfx.MISSION);
    }

    // ------------------------------------------------------------------ frame update

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        fx.update(tpf);

        // rumble before the reveal
        if (queenT >= 0 && !revealed) {
            queenT += tpf;
            double k = Math.min(1, queenT / REVEAL_AT);
            shake = 1 + 4 * k;
            dark.setOpacity(0.22 + 0.3 * k);
            queenPile.setTranslateX(Math.round(Math.sin(queenT * 70) * 3 * k));
            queenPile.setTranslateY(-Math.round(Math.abs(Math.sin(queenT * 40)) * 4 * k));
            aura.setOpacity(0.35 * k);
            if (queenT >= REVEAL_AT) reveal();
        }

        // pop-in, bobbing and pulsing aura once she is out
        if (revealed) {
            revealT += tpf;
            double pt = Math.min(1, revealT / 0.55);
            double s = pt >= 1 ? 1 : easeOutBack(pt);
            queenImg.setScaleX(s);
            queenImg.setScaleY(s);
            queenImg.setTranslateY(QUEEN_H / 2 * (1 - s) + Math.round(Math.sin(t * 2.4) * 4));
            queenPile.setTranslateX(0);
            queenPile.setTranslateY(0);
            double pulse = 1 + 0.06 * Math.sin(t * 3);
            aura.setScaleX(pulse);
            aura.setScaleY(pulse);
            aura.setOpacity(Math.min(1, revealT * 3) * (0.8 + 0.2 * Math.sin(t * 3)));
            dark.setOpacity(Math.max(0.35, dark.getOpacity() - tpf * 0.4));
            // hearts keep floating up from her
            heartTimer += tpf;
            if (heartTimer > 0.22) {
                heartTimer = 0;
                fx.burst(QUEEN_X - 90 + rng.nextDouble() * 180, QUEEN_FEET - 120 - rng.nextDouble() * 120, 1,
                        () -> StoryFx.heart(4 + rng.nextInt(3), Color.web("#ff8fb8")), 25, 1.7, -60);
            }
            // the tidy sibling recoils, the "!" bobs
            playerImg.setTranslateX(-Math.round(8 * Math.min(1, revealT * 4)));
            exclaim.setTranslateY(Math.round(Math.sin(t * 12) * 2));
            exclaim.setOpacity(revealT < 2.2 ? 1 : Math.max(0, 1 - (revealT - 2.2) * 2));
        }

        // the "HO HO HO!" bubble
        if (laughT >= 0) {
            laughT += tpf;
            double pt = Math.min(1, laughT / 0.4);
            double s = pt >= 1 ? 1 : easeOutBack(pt);
            laughLabel.setScaleX(s);
            laughLabel.setScaleY(s);
            double a = laughT < 2.0 ? 1 : Math.max(0, 1 - (laughT - 2.0) * 2);
            laughLabel.setOpacity(a);
            laughLabel.setTranslateY(Math.round(Math.sin(laughT * 14) * 2));   // giggle
            if (laughT > 2.5) {
                laughT = -1;
                queenImg.setImage(Sprites.get("boss/queen_muck.png"));
            }
        }

        // the messy sibling floating up into her hand
        if (grabT >= 0) {
            grabT += tpf;
            double k = Math.min(1, grabT / 1.1);
            double e = k * k * (3 - 2 * k);
            double cx = FRIEND_X, cy = FEET - 72;                 // start (center of the sprite)
            double tx = QUEEN_X - 60, ty = QUEEN_MID_Y + 10;      // her hand
            double x = cx + (tx - cx) * e;
            double y = cy + (ty - cy) * e - Math.sin(k * Math.PI) * 60;
            friendImg.setTranslateX(x - cx);
            friendImg.setTranslateY(y - cy);
            friendImg.setScaleX(1 - 0.6 * e);
            friendImg.setScaleY(1 - 0.6 * e);
            friendImg.setRotate(e * 25);
            friendImg.setOpacity(k > 0.85 ? (1 - k) / 0.15 : 1);
            friendShadow.setOpacity(1 - e);
            trailTimer += tpf;
            if (trailTimer > 0.05 && k < 1) {
                trailTimer = 0;
                fx.burst(x, y, 2, () -> StoryFx.sparkle(3, Color.web("#fff6a0")), 40, 0.6, 0);
            }
            if (k >= 1 && !grabDone) {
                grabDone = true;
                exitT = -1.2;                                 // she waits a moment, laughing, then prances off
                friendImg.setVisible(false);
                shake = 5;
                startLaugh();
                fx.burst(tx, ty, 16, () -> StoryFx.heart(5, Color.web("#ff8fb8")), 200, 1.2, 80);
            }
        }

        // the wall is smashed open and Queen Muck prances out through the hole
        if (holeT >= 0) {
            holeT += tpf;
            holeImg.setOpacity(Math.min(1, holeT / 0.9));
        }
        if (exitT > -99) {
            exitT += tpf;
            if (exitT > 0) {
                double k = Math.min(1, exitT / 1.4);
                queenImg.setTranslateX(k * k * 240);
                queenImg.setOpacity(1 - k);
                aura.setOpacity(aura.getOpacity() * (1 - k));
            }
        }

        // screen shake and flash
        if (revealed) shake = Math.max(0, shake - tpf * 12);
        world.setTranslateX(shake > 0.2 ? Math.round((rng.nextDouble() - 0.5) * 2 * shake) : 0);
        world.setTranslateY(shake > 0.2 ? Math.round((rng.nextDouble() - 0.5) * 2 * shake) : 0);
        if (flashAlpha > 0) {
            flashAlpha = Math.max(0, flashAlpha - tpf * 2.2);
            flash.setOpacity(flashAlpha);
        }
        playerImg.setTranslateY(Math.round(Math.sin(t * 2) * 1.5));
    }

    private static double easeOutBack(double x) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        int before = box.page();
        boolean done = box.advance();
        int now = box.page();
        if (now != before) {
            if (now == 1) startEntrance();
            else if (now == 2) {
                startGrab();
                holeT = 0;
            }
        }
        if (done) next.run();
    }

    @Override
    public void onCancel() {
        // Skip cutscene entirely
        next.run();
    }
}
