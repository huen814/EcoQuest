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
 * Story intro, shown right AFTER the player picks a sibling.
 * The sibling you picked is the tidy one (you). The other sibling is the messy one.
 * Two pages of narration, then the callback (the kidnap cutscene).
 */
public final class IntroScreen extends Screen {

    private static final int PX = Sprites.PX;
    private static final int SH = 72;   // one 48x72 frame, facing down

    private final DialogBox box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
    private final Runnable next;

    private static final List<String> PAGES = List.of(
        "In a small, sunny village lived two siblings. One kept everything neat, " +
        "sorted and shining. The other left things... everywhere.",

        "Their rooms said it all: one spotless, the other buried under a mountain of " +
        "bottles, boxes and banana peels. Nobody suspected that a mountain " +
        "like that could ever wake up."
    );

    public IntroScreen(EcoQuestApp app, Runnable next) {
        super(app);
        this.next = next;
        Sibling me = app.save().sibling();

        root.getChildren().add(new Rectangle(Ui.W, Ui.H, Color.web("#10181c")));
        root.getChildren().add(new Rectangle(Ui.W, 200, Color.web("#1e2d4a")));

        Ui.Label title = new Ui.Label("ECOQUEST", 28, Ui.WHITE);
        title.setLayoutX((Ui.W - title.textWidth()) / 2.0);
        title.setLayoutY(32);
        Ui.Label sub = new Ui.Label("GUARDIANS OF THE GREEN CITY", 10, Ui.WHITE);
        sub.setLayoutX((Ui.W - sub.textWidth()) / 2.0);
        sub.setLayoutY(80);
        root.getChildren().addAll(title, sub);

        // You (clean, wearing your outfit) on the left, the messy sibling on the right.
        ImageView you = new ImageView(Wardrobe.sheet(app.save(), me, false));
        ImageView other = Sprites.view(StoryState.messySheet(me));
        place(you, 100, 190);
        place(other, Ui.W - 148, 190);
        tag(StoryState.role(true) + " (YOU)", 100 + PX / 2.0, 270, Ui.GOOD);
        tag(StoryState.role(false), Ui.W - 148 + PX / 2.0, 270, Ui.ACCENT);

        Ui.Label amp = new Ui.Label("&", 20, Ui.FRAME_IN);
        amp.setLayoutX((Ui.W - amp.textWidth()) / 2.0);
        amp.setLayoutY(230);
        root.getChildren().addAll(amp, box);
    }

    private void place(ImageView iv, double x, double y) {
        iv.setViewport(new Rectangle2D(0, 0, PX, SH));
        iv.setLayoutX(x);
        iv.setLayoutY(y);
        root.getChildren().add(iv);
    }

    private void tag(String text, double centerX, double y, Color col) {
        Ui.Label l = new Ui.Label(text, 9, col);
        l.setLayoutX(centerX - l.textWidth() / 2.0);
        l.setLayoutY(y);
        root.getChildren().add(l);
    }

    @Override
    public void enter() { box.show(PAGES); }

    @Override
    public void update(double tpf) { box.update(tpf); }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        if (box.advance()) next.run();
    }

    @Override
    public void onCancel() { app.showMainMenu(); }
}
