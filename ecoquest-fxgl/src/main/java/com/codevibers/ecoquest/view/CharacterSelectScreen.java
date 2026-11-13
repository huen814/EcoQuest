package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * Pick the sister or the brother. Shown at the start of a playthrough (first campaign level after a new game).
 * Both are shown clean. The one you pick is the one you play (and stays clean); the other sibling is the dirty one.
 * Both play exactly the same: only the look and the story presentation differ.
 */
public final class CharacterSelectScreen extends Screen {
    private static final double CARD_W = 250;
    private static final double CARD_H = 270;
    private static final double CARD_Y = 78;
    private static final double SPRITE_W = 144;   // one 48x72 frame shown at x3
    private static final double SPRITE_H = 216;

    private final Runnable afterChoice;
    private final Sibling[] siblings = Sibling.values();
    private final Group[] cards = new Group[2];
    private final ImageView[] sprites = new ImageView[2];
    private final Polygon cursor = new Polygon(0, 0, 18, 0, 9, 12);
    private final Ui.Label info = new Ui.Label("", 10, Ui.INK);
    private int index;
    private double t;

    public CharacterSelectScreen(EcoQuestApp app, Runnable afterChoice) {
        super(app);
        this.afterChoice = afterChoice;
        root.getChildren().add(Sprites.view("backdrop/title.png"));
        root.getChildren().add(new Rectangle(Ui.W, Ui.H, Color.rgb(0, 0, 0, 0.4)));

        Ui.Label title = new Ui.Label("CHOOSE YOUR GUARDIAN", 16, Ui.WHITE);
        title.setLayoutX((Ui.W - title.textWidth()) / 2);
        title.setLayoutY(20);
        root.getChildren().add(title);

        double gap = 40;
        double x0 = (Ui.W - (2 * CARD_W + gap)) / 2;
        for (int i = 0; i < 2; i++) {
            Sibling s = siblings[i];
            Group card = new Group(Ui.panel(CARD_W, CARD_H));
            card.setLayoutX(x0 + i * (CARD_W + gap));
            card.setLayoutY(CARD_Y);

            ImageView v = Sprites.view(s.sheet(false));
            v.setViewport(new Rectangle2D(0, 0, 48, 72));   // first frame, facing down
            v.setFitWidth(SPRITE_W);
            v.setFitHeight(SPRITE_H);
            v.setLayoutX((CARD_W - SPRITE_W) / 2);
            v.setLayoutY(14);
            sprites[i] = v;

            Ui.Label name = new Ui.Label(s.label, 13, Ui.FRAME_OUT);
            name.setLayoutX((CARD_W - name.textWidth()) / 2);
            name.setLayoutY(CARD_H - 40);
            card.getChildren().addAll(v, name);
            cards[i] = card;
            root.getChildren().add(card);
        }

        cursor.setFill(Color.WHITE);
        cursor.setStroke(Ui.FRAME_OUT);
        root.getChildren().add(cursor);

        Group box = Ui.panel(Ui.W - 12, 84);
        box.setLayoutX(6);
        box.setLayoutY(Ui.H - 90);
        info.setLayoutX(26);
        info.setLayoutY(Ui.H - 76);
        info.setWrap(Ui.W - 56);
        root.getChildren().addAll(box, info);
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < 2; i++) cards[i].setOpacity(i == index ? 1.0 : 0.6);
        double cx = cards[index].getLayoutX() + CARD_W / 2 - 9;
        cursor.setLayoutX(cx);
        Sibling pick = siblings[index];
        info.setText("YOU PLAY AS THE " + pick.label + ". THE " + pick.other().label + " ENDS UP DIRTY IN THE STORY.\n"
                + "BOTH PLAY EXACTLY THE SAME.\n"
                + "ARROWS: CHOOSE    E / ENTER: CONFIRM    X: BACK");
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        cursor.setLayoutY(CARD_Y - 20 + Math.round(Math.sin(t * 5) * 3));
        for (int i = 0; i < 2; i++) sprites[i].setTranslateY(i == index ? Math.round(Math.sin(t * 4) * 2) : 0);
    }

    @Override
    public void onDirection(Direction d) {
        if (d == Direction.LEFT) index = 0;
        else if (d == Direction.RIGHT) index = 1;
        else return;
        app.sfx(Sfx.CURSOR);
        refresh();
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        app.save().chooseSibling(siblings[index]);
        afterChoice.run();
    }

    @Override
    public void onCancel() { app.showMainMenu(); }
}
