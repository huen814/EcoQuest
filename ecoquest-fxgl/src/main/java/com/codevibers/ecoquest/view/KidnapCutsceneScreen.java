package com.codevibers.ecoquest.view;

import java.util.List;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;
import com.codevibers.ecoquest.story.StoryState;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Short cutscene after the player picks their hero:
 *   - the tidy sibling (you) visits the messy sibling's room
 *   - Queen Muck emerges and kidnaps the messy sibling
 *   - the player is left alone with a quest
 * Advances on confirm; skips on cancel. Either way it ends by running the callback it was given.
 */
public final class KidnapCutsceneScreen extends Screen {

    private static final int PX = Sprites.PX;
    private static final int SH = 72;

    private final Runnable next;
    private final DialogBox box;
    private final ImageView playerImg;
    private final ImageView friendImg;   // messy version
    private final ImageView monsterImg;
    private final Rectangle flash = new Rectangle(Ui.W, Ui.H, Color.WHITE);

    private int page = 0;
    private double t;
    private double flashAlpha = 0;

    public KidnapCutsceneScreen(EcoQuestApp app, Runnable next) {
        super(app);
        this.next = next;
        Sibling me = app.save().sibling();

        // Background: messy room = dump scene palette
        root.getChildren().add(Sprites.view("backdrop/dump.png"));
        Rectangle dim = new Rectangle(Ui.W, Ui.H, Color.rgb(0, 0, 0, 0.5));
        root.getChildren().add(dim);

        // Sprites
        playerImg  = new ImageView(Wardrobe.sheet(app.save(), me, false));
        friendImg  = Sprites.view(StoryState.messySheet(me));
        monsterImg = Sprites.view("boss/queen_muck.png");

        setupSprite(playerImg,  80,  200);
        setupSprite(friendImg,  Ui.W / 2.0 - 24, 200);
        setupSprite(monsterImg, Ui.W - 240, 100, 160, 160);   // boss is 160x160

        monsterImg.setVisible(false);

        flash.setOpacity(0);
        flash.setMouseTransparent(true);

        box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
        root.getChildren().addAll(flash, box);

        Ui.Label skip = new Ui.Label("X : SKIP", 9, Ui.FRAME_IN);
        skip.setLayoutX(Ui.W - skip.textWidth() - 14);
        skip.setLayoutY(Ui.H - 148);
        root.getChildren().add(skip);
    }

    private void setupSprite(ImageView iv, double x, double y) {
        iv.setViewport(new Rectangle2D(0, 0, PX, SH));
        iv.setLayoutX(x);
        iv.setLayoutY(y);
        root.getChildren().add(iv);
    }

    private void setupSprite(ImageView iv, double x, double y, double w, double h) {
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);
        iv.setLayoutX(x);
        iv.setLayoutY(y);
        root.getChildren().add(iv);
    }

    @Override
    public void enter() {
        page = 0;
        t = 0;
        flashAlpha = 0;
        monsterImg.setVisible(false);
        box.show(List.of(getPage(0), getPage(1), getPage(2)));
    }

    private String getPage(int i) {
        return switch (i) {
            case 0 -> "The tidy sibling knocks on the messy sibling's door. No answer. " +
                      "Pushing it open, they gasp - the room is ankle-deep in rotting garbage. " +
                      "Empty bottles, old takeaway boxes and banana peels are piled to the ceiling!";
            case 1 -> "The rubbish TREMBLES. Cans clatter, bags inflate, and from the heap " +
                      "rises a glittering TRASH QUEEN - QUEEN MUCK! A crown of bottle caps, a gown of " +
                      "garbage bags, and a hand over her grinning mouth. \"Ho ho ho ho~! You simply MUST " +
                      "stay with me, darling!\" She scoops up the messy sibling in a bouquet of tangled plastic!";
            default -> "\"SIBLING! NOOO!\" the tidy sibling shouts, but Queen Muck has already " +
                       "pranced through the wall, laughing \"ho ho ho!\", toward her palace in the polluted outskirts. " +
                       "Sort trash, earn the SPIRIT BOTTLE, and go rescue your sibling!";
        };
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        if (flashAlpha > 0) {
            flashAlpha = Math.max(0, flashAlpha - tpf * 2.5);
            flash.setOpacity(flashAlpha);
        }
        // gentle player bob
        playerImg.setTranslateY(Math.round(Math.sin(t * 2) * 1.5));
        // monster shake when visible
        if (monsterImg.isVisible()) {
            monsterImg.setTranslateX(Math.round((Math.random() - 0.5) * 2));
        }
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        // Flash + monster reveal on page 1->2
        if (page == 1) {
            flashAlpha = 1.0;
            monsterImg.setVisible(true);
            friendImg.setOpacity(0.3);
            app.sfx(Sfx.DUMP);
            app.laugh();   // ho ho ho
        }
        boolean done = box.advance();
        page++;
        if (done) {
            next.run();
        }
    }

    @Override
    public void onCancel() {
        // Skip cutscene entirely
        next.run();
    }
}
