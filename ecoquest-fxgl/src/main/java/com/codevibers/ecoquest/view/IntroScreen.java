package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.List;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;
import com.codevibers.ecoquest.story.StoryState;

import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Story intro, shown right AFTER the player picks a sibling.
 * Two yards side by side: the tidy sibling (the one you picked) on the left in a clean, sunny village,
 * the messy sibling on the right among piles of litter, flies and stink. The pile grows on the second page.
 */
public final class IntroScreen extends Screen {

    private static final double FEET = 318;
    private static final double SCALE = 2;
    private static final double YOU_X = 175, OTHER_X = 545;

    private final DialogBox box = new DialogBox(6, Ui.H - 134, Ui.W - 12, 128);
    private final Runnable next;
    private final StoryFx.Particles fx = new StoryFx.Particles(Ui.W, Ui.H);
    private final ImageView you;
    private final ImageView other;
    private final Group pileMore;
    private final List<javafx.scene.Node> messyYard = new ArrayList<>();   // page one is a clean village; these fade in on page two
    private double t;
    private double grow = -1;   // -1 = not started, otherwise seconds since the second page began

    private static final List<String> PAGES = List.of(
        "In a small, sunny village lived two siblings, {tidy} and {messy}. {tidy} kept everything neat, " +
        "sorted and shining. {messy} left things... everywhere.",

        "Their yards said it all: one spotless, the other buried under a mountain of " +
        "bottles, boxes and banana peels. Nobody suspected that a mountain " +
        "like that could ever wake up."
    );
    private List<String> pages = PAGES;

    public IntroScreen(EcoQuestApp app, Runnable next) {
        super(app);
        this.next = next;
        Sibling me = app.save().sibling();
        pages = PAGES.stream().map(p -> p.replace("{tidy}", me.name).replace("{messy}", me.other().name)).toList();

        // Two yards: a clean village on the left, a dump on the right.
        root.getChildren().add(StoryFx.scene("house"));
        Group dump = StoryFx.scene("dump");
        dump.setClip(new Rectangle(Ui.W / 2.0, 0, Ui.W / 2.0, Ui.H));
        root.getChildren().add(dump);
        Rectangle grime = new Rectangle(Ui.W / 2.0, 0, Ui.W / 2.0, Ui.H);
        grime.setFill(Color.web("#5a5a2a", 0.22));
        Rectangle fence = new Rectangle(Ui.W / 2.0 - 3, 0, 6, Ui.H);
        fence.setFill(Color.web("#2c2748", 0.55));
        var sun = StoryFx.glow(190, Color.web("#fff3b0", 0.65));
        sun.setTranslateX(110);
        sun.setTranslateY(40);
        root.getChildren().addAll(grime, fence, sun);
        messyYard.addAll(List.of(dump, grime, fence));

        // Back pile (behind the messy sibling); the bigger one fades in on page two.
        Group backPile = StoryFx.litter(5, 440, 680, 215, 300, 12, 26, 46);
        root.getChildren().add(backPile);
        messyYard.add(backPile);
        pileMore = StoryFx.litter(11, 420, 700, 150, 300, 30, 30, 56);
        pileMore.setOpacity(0);
        root.getChildren().add(pileMore);

        you = Sprites.view(me.sheet(false));   // plain sheet: the story never shows shop cosmetics
        other = Sprites.view(StoryState.messySheet(me));
        StoryFx.stand(you, YOU_X, FEET, SCALE);
        StoryFx.stand(other, OTHER_X, FEET, SCALE);
        root.getChildren().addAll(StoryFx.shadow(YOU_X, FEET, 90), StoryFx.shadow(OTHER_X, FEET, 90), you, other);

        // Litter lying around the front of the messy yard.
        Group frontLitter = StoryFx.litter(7, 400, 700, 322, 345, 10, 24, 40);
        root.getChildren().add(frontLitter);
        messyYard.add(frontLitter);
        messyYard.forEach(n -> n.setOpacity(0));

        // Ambient life: sparkles on the tidy side; flies and stink on the messy side.
        fx.driftIn(14, () -> StoryFx.sparkle(3, Color.web("#fff6a0", 0.9)), 20, 150, 340, 330, -10, 10);
        fx.driftIn(10, () -> StoryFx.dot(1.6, Color.web("#2a2a30")), 420, 190, 700, 330, -2, 46);
        fx.driftIn(10, () -> StoryFx.dot(5, Color.web("#86a04a", 0.35)), 450, 170, 680, 320, -16, 10);
        root.getChildren().add(fx);

        Ui.Label title = new Ui.Label("ECOQUEST", 28, Ui.FRAME_OUT);
        title.setLayoutX((Ui.W - title.textWidth()) / 2.0);
        title.setLayoutY(26);
        title.shadow(Color.web("#ffffff", 0.7));
        Ui.Label sub = new Ui.Label("GUARDIANS OF THE GREEN CITY", 10, Ui.FRAME_OUT);
        sub.setLayoutX((Ui.W - sub.textWidth()) / 2.0);
        sub.setLayoutY(72);
        sub.shadow(Color.web("#ffffff", 0.7));
        root.getChildren().addAll(title, sub);

        tag(me.name.toUpperCase() + " (YOU)", YOU_X, FEET - 72 * SCALE - 18, Ui.GOOD);
        tag(me.other().name.toUpperCase(), OTHER_X, FEET - 72 * SCALE - 18, Ui.BAD);
        root.getChildren().add(box);
    }

    private void tag(String text, double centerX, double y, Color col) {
        Ui.Label l = new Ui.Label(text, 9, col);
        l.setLayoutX(centerX - l.textWidth() / 2.0);
        l.setLayoutY(y);
        l.shadow(Color.web("#ffffff", 0.85));
        root.getChildren().add(l);
    }

    @Override
    public void enter() {
        box.show(pages);
        Audio.playMusic("intro.wav", 0.4);      // carries on into the kidnap cutscene until Queen Muck appears
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        box.update(tpf);
        fx.update(tpf);
        you.setTranslateY(Math.round(Math.sin(t * 2) * 2));
        other.setTranslateX(Math.round(Math.sin(t * 5) * 1));
        if (grow >= 0) {
            grow += tpf;
            pileMore.setOpacity(Math.min(1, grow / 1.2));
            messyYard.forEach(n -> n.setOpacity(Math.min(1, grow / 1.2)));
        }
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        int before = box.page();
        boolean done = box.advance();
        if (box.page() != before && box.page() == 1) {
            grow = 0;                 // the pile rises
            app.sfx(Sfx.DUMP);
        }
        if (done) next.run();
    }

    @Override
    public void onCancel() { app.showMainMenu(); }
}
