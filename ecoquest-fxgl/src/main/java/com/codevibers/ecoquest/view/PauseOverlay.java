package com.codevibers.ecoquest.view;

import java.util.List;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.Sfx;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Pause menu for the boss fights and the chase: RESUME, SETTINGS, QUIT TO MENU. The owning screen stops updating
 * the fight while {@link #isOpen()} and forwards update and input here.
 */
final class PauseOverlay extends Group {
    private final EcoQuestApp app;
    private final Runnable onQuit;
    private final Runnable onResume;
    private MenuBox menu;
    private SettingsPanel settings;
    private boolean open;

    PauseOverlay(EcoQuestApp app, Runnable onResume, Runnable onQuit) {
        this.app = app;
        this.onResume = onResume;
        this.onQuit = onQuit;
        setViewOrder(-100000);      // above everything else on the screen
        setVisible(false);
    }

    boolean isOpen() { return open; }

    void open() {
        open = true;
        app.input().clear();
        setVisible(true);
        showMenu();
    }

    void close() {
        open = false;
        settings = null;
        menu = null;
        getChildren().clear();
        setVisible(false);
        app.input().clear();
        onResume.run();
    }

    private void showMenu() {
        settings = null;
        menu = new MenuBox("PAUSED", List.of(
                new MenuBox.Entry("RESUME", this::close),
                new MenuBox.Entry("SETTINGS", this::showSettings),
                new MenuBox.Entry("QUIT TO MENU", onQuit)), 300);
        menu.setLayoutX((Ui.W - 300) / 2.0);
        menu.setLayoutY((Ui.H - menu.height()) / 2.0);
        getChildren().setAll(new Rectangle(Ui.W, Ui.H, Color.rgb(30, 20, 60, 0.55)), menu);
    }

    private void showSettings() {
        menu = null;
        settings = new SettingsPanel(app, true, this::showMenu);
        settings.setLayoutX((Ui.W - SettingsPanel.WIDTH) / 2.0);
        settings.setLayoutY((Ui.H - SettingsPanel.HEIGHT) / 2.0);
        getChildren().setAll(new Rectangle(Ui.W, Ui.H, Color.rgb(30, 20, 60, 0.55)), settings);
    }

    void update(double tpf) {
        if (menu != null) menu.update(tpf);
        if (settings != null) settings.update(tpf);
    }

    void onDirection(Direction d) {
        if (settings != null) { settings.onDirection(d); return; }
        if (menu == null) return;
        if (d == Direction.UP) menu.move(-1);
        else if (d == Direction.DOWN) menu.move(1);
        else return;
        app.sfx(Sfx.CURSOR);
    }

    void onConfirm() {
        if (settings != null) { settings.onConfirm(); return; }
        if (menu != null) { app.sfx(Sfx.SELECT); menu.confirm(); }
    }

    /** ESC: closes settings back to the menu, or the menu back to the fight. */
    void onCancel() {
        if (settings != null) settings.onCancel();
        else close();
    }
}
