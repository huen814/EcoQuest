package com.codevibers.ecoquest.view;

import java.util.List;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.BinType;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.Sfx;

import javafx.scene.image.ImageView;

/** Title screen: sky, skyline, the five bins bobbing at the bottom, and the start menu. */
public final class MainMenuScreen extends Screen {
    private static final int CONTINUE = 2;
    private static final int SOUND = 5;

    private final MenuBox menu;
    private final ImageView[] bins = new ImageView[5];
    private final Ui.Label wallet;
    private double t;

    public MainMenuScreen(EcoQuestApp app) {
        super(app);
        root.getChildren().add(Sprites.view("backdrop/title.png"));
        BinType[] types = BinType.values();
        for (int i = 0; i < 5; i++) {
            bins[i] = Sprites.view("bins/" + types[i].sprite() + ".png");
            bins[i].setLayoutX(54 + i * 132);
            bins[i].setLayoutY(Ui.H - 58);
            root.getChildren().add(bins[i]);
        }
        Ui.Label title = new Ui.Label("ECOQUEST", 32, Ui.WHITE);
        title.setLayoutX((Ui.W - title.textWidth()) / 2);
        title.setLayoutY(26);
        Ui.Label sub = new Ui.Label("GUARDIANS OF THE GREEN CITY", 11, Ui.WHITE);
        sub.setLayoutX((Ui.W - sub.textWidth()) / 2);
        sub.setLayoutY(74);
        wallet = new Ui.Label("PHP " + app.save().wallet, 11, Ui.INK);
        wallet.setLayoutX(26);
        wallet.setLayoutY(Ui.H - 100);

        menu = new MenuBox(null, List.of(
                new MenuBox.Entry("START GAME", app::showLevelSelect),
                new MenuBox.Entry("NEW GAME", app::newGame),
                new MenuBox.Entry("CONTINUE", app::continueGame),
                new MenuBox.Entry("HOW TO PLAY", app::showHowTo),
                new MenuBox.Entry("UPGRADE SHOP", app::showShop),
                new MenuBox.Entry("SOUND: " + (app.save().muted ? "OFF" : "ON"), this::toggleSound),
                new MenuBox.Entry("QUIT", () -> System.exit(0))), 300);
        boolean canContinue = app.save().hasPlaythrough();
        menu.setEnabled(CONTINUE, canContinue);
        menu.setIndex(canContinue ? CONTINUE : 0);
        menu.setLayoutX((Ui.W - 300) / 2.0);
        menu.setLayoutY(118);
        root.getChildren().addAll(title, sub, menu);
    }

    private void toggleSound() {
        app.toggleMute();
        menu.setLabel(SOUND, "SOUND: " + (app.save().muted ? "OFF" : "ON"));
    }

    @Override
    public void update(double tpf) {
        t += tpf;
        menu.update(tpf);
        for (int i = 0; i < 5; i++) bins[i].setTranslateY(Math.round(Math.sin(t * 2 + i) * 3));
    }

    @Override
    public void onDirection(Direction d) {
        if (d == Direction.UP) menu.move(-1);
        else if (d == Direction.DOWN) menu.move(1);
        else return;
        app.sfx(Sfx.CURSOR);
    }

    @Override
    public void onConfirm() {
        app.sfx(menu.isEnabled(menu.index()) ? Sfx.SELECT : Sfx.DENY);
        menu.confirm();
    }
}
