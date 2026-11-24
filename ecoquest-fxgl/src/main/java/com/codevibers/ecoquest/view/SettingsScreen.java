package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Direction;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Settings opened from the main menu: the title backdrop with the shared {@link SettingsPanel} on top. */
public final class SettingsScreen extends Screen {
    private final SettingsPanel panel;

    public SettingsScreen(EcoQuestApp app) {
        super(app);
        root.getChildren().add(Sprites.view("backdrop/title.png"));
        root.getChildren().add(new Rectangle(Ui.W, Ui.H, Color.rgb(30, 20, 60, 0.35)));
        panel = new SettingsPanel(app, false, app::showMainMenu);
        panel.setLayoutX((Ui.W - SettingsPanel.WIDTH) / 2.0);
        panel.setLayoutY((Ui.H - SettingsPanel.HEIGHT) / 2.0);
        root.getChildren().add(panel);
    }

    @Override public void update(double tpf) { panel.update(tpf); }
    @Override public void onDirection(Direction d) { panel.onDirection(d); }
    @Override public void onConfirm() { panel.onConfirm(); }
    @Override public void onCancel() { panel.onCancel(); }

    /** The title song keeps playing on this screen. */
    @Override public boolean titleMusic() { return true; }
}
