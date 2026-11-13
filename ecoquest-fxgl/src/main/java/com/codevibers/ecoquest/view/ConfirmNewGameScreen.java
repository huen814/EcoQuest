package com.codevibers.ecoquest.view;

import java.util.List;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.Sfx;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** "Start over?" prompt shown by NEW GAME when a previous playthrough exists. Defaults to the safe answer. */
public final class ConfirmNewGameScreen extends Screen {
    private final MenuBox menu;

    public ConfirmNewGameScreen(EcoQuestApp app) {
        super(app);
        root.getChildren().add(Sprites.view("backdrop/title.png"));
        Rectangle dim = new Rectangle(Ui.W, Ui.H, Color.rgb(0, 0, 0, 0.45));

        Group panel = Ui.panel(Ui.W - 80, 150);
        panel.setLayoutX(40);
        panel.setLayoutY(60);
        Ui.Label head = new Ui.Label("START A NEW GAME?", 13, Ui.FRAME_OUT);
        head.setLayoutX(70);
        head.setLayoutY(82);
        Ui.Label body = new Ui.Label("YOUR STORY PROGRESS AND ATTRIBUTES WILL BE RESET.\nCOINS AND COSMETICS ARE KEPT.", 10, Ui.INK);
        body.setLayoutX(70);
        body.setLayoutY(118);
        body.setWrap(Ui.W - 140);

        menu = new MenuBox(null, List.of(
                new MenuBox.Entry("NO, GO BACK", app::showMainMenu),
                new MenuBox.Entry("YES, NEW GAME", app::startNewGame)), 300);
        menu.setLayoutX((Ui.W - 300) / 2.0);
        menu.setLayoutY(240);
        root.getChildren().addAll(dim, panel, head, body, menu);
    }

    @Override
    public void update(double tpf) { menu.update(tpf); }

    @Override
    public void onDirection(Direction d) {
        if (d == Direction.UP) menu.move(-1);
        else if (d == Direction.DOWN) menu.move(1);
        else return;
        app.sfx(Sfx.CURSOR);
    }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        menu.confirm();
    }

    @Override
    public void onCancel() { app.showMainMenu(); }
}
