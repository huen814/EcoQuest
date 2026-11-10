package com.codevibers.ecoquest;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.LevelConfig;
import com.codevibers.ecoquest.model.Levels;
import com.codevibers.ecoquest.model.Save;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.view.Audio;
import com.codevibers.ecoquest.view.HowToScreen;
import com.codevibers.ecoquest.view.LevelSelectScreen;
import com.codevibers.ecoquest.view.MainMenuScreen;
import com.codevibers.ecoquest.view.ModesScreen;
import com.codevibers.ecoquest.view.PlayScreen;
import com.codevibers.ecoquest.view.Screen;
import com.codevibers.ecoquest.view.ShopScreen;
import com.codevibers.ecoquest.view.Ui;

import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

public class EcoQuestApp extends GameApplication {
    private Save save;
    private InputState input;
    private Screen current;
    private Screen pending;

    public static void main(String[] args) { launch(args); }

    @Override
    protected void initSettings(GameSettings s) {
        s.setWidth(Ui.W);
        s.setHeight(Ui.H);
        s.setTitle("EcoQuest: Guardians of the Green City");
        s.setVersion("0.1");
        s.setIntroEnabled(false);
        s.setMainMenuEnabled(false);
        s.setGameMenuEnabled(false);
        s.setManualResizeEnabled(true);
        s.setPreserveResizeRatio(true);
        s.setFullScreenAllowed(true);
    }

    @Override
    protected void initInput() {
        input = new InputState(this);
        FXGL.getInput().addEventHandler(KeyEvent.KEY_PRESSED, input::pressed);
        FXGL.getInput().addEventHandler(KeyEvent.KEY_RELEASED, input::released);
    }

    @Override
    protected void initGame() {
        save = Save.load();
        Audio.setMuted(save.muted);
        FXGL.getGameScene().setBackgroundColor(Color.web("#10181c"));
        showMainMenu();
    }

    @Override
    protected void onUpdate(double tpf) {
        if (pending != null) {
            Screen next = pending;
            pending = null;
            if (current != null) {
                current.exit();
                FXGL.getGameScene().removeUINode(current.root());
            }
            current = next;
            FXGL.getGameScene().addUINode(current.root());
            current.enter();
        }
        if (current != null) current.update(tpf);
    }

    // ------------------------------------------------------------------ services for screens

    public Save save() { return save; }
    public InputState input() { return input; }
    public void sfx(Sfx s) { Audio.play(s); }

    public void toggleMute() {
        save.muted = !save.muted;
        Audio.setMuted(save.muted);
        save.store();
    }

    public void startLevel(LevelConfig cfg, int index) { pending = new PlayScreen(this, cfg, index); }
    public void showMainMenu() { pending = new MainMenuScreen(this); }
    public void showLevelSelect() { pending = new LevelSelectScreen(this); }
    public void showModes() { pending = new ModesScreen(this); }
    public void showShop() { pending = new ShopScreen(this); }
    public void showHowTo() { pending = new HowToScreen(this); }

    /** "Campaign" button: continue at the furthest unlocked level. */
    public void continueCampaign() {
        int i = Math.min(save.unlocked, Levels.CAMPAIGN.size() - 1);
        startLevel(Levels.CAMPAIGN.get(i), i);
    }

    // ------------------------------------------------------------------ key dispatch (from InputState)

    public void dispatchDirection(Direction d) { if (current != null) current.onDirection(d); }
    public void dispatchConfirm() { if (current != null) current.onConfirm(); }
    public void dispatchAction2() { if (current != null) current.onAction2(); }
    public void dispatchBoat() { if (current != null) current.onBoat(); }
    public void dispatchPause() { if (current != null) current.onPause(); }
    public void dispatchCancel() { if (current != null) current.onCancel(); }
}
