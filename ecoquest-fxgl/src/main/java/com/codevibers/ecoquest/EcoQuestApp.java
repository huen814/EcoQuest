package com.codevibers.ecoquest;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.LevelConfig;
import com.codevibers.ecoquest.model.Levels;
import com.codevibers.ecoquest.model.Progression;
import com.codevibers.ecoquest.model.Save;
import com.codevibers.ecoquest.model.Sfx;

import com.codevibers.ecoquest.view.*;
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
    /** Queen Muck's "ho ho ho". */
    public void laugh() { Audio.laugh(); }

    public void toggleMute() {
        save.muted = !save.muted;
        Audio.setMuted(save.muted);
        save.store();
    }

    public void startLevel(LevelConfig cfg, int index) {
        if (cfg.isCampaign() && Progression.hasOffer(save)) {
            showAttributeChoice(() -> startLevel(cfg, index));
            return;
        }
        if (cfg.isCampaign() && !save.hasChosenSibling()) {
            // First campaign level of a playthrough: pick a sibling, story intro for that pick, kidnap cutscene, then this level.
            Runnable play = () -> startLevel(cfg, index);
            pending = new CharacterSelectScreen(this, () -> pending = new IntroScreen(this, () -> showKidnapCutscene(play)));
            return;
        }
        if (cfg.isCampaign() && !save.started) {
            save.started = true;
            save.store();
        }
        pending = new PlayScreen(this, cfg, index);
    }
    public void showMainMenu() { pending = new MainMenuScreen(this); }
    public void showLevelSelect() {
        if (Progression.hasOffer(save)) showAttributeChoice(this::showLevelSelect);
        else pending = new LevelSelectScreen(this);
    }
    public void showAttributeChoice(Runnable next) { pending = new AttributeChoiceScreen(this, next); }
    public void showModes() { pending = new ModesScreen(this); }
    public void showShop() { pending = new ShopScreen(this); }
    public void showHowTo() { pending = new HowToScreen(this); }
    public void showAttributes() { pending = new AttributesScreen(this, this::showLevelSelect); }
    public void showAttributesFromMenu() { pending = new AttributesScreen(this, this::showMainMenu); }

    // ------------------------------------------------------------------ story

    public void showKidnapCutscene(Runnable next) { pending = new KidnapCutsceneScreen(this, next); }
    /** After each campaign level: villager thank-you and Spirit Bottle charge (the last one leads to the boss fight). */
    public void showVillagerReward(int levelIndex) { pending = new VillagerRewardScreen(this, levelIndex); }
    /** After the last level: the portal and Mother Nature's grove, then {@code next} (the Queen Muck fight). */
    public void showGroveCutscene(Runnable next) { pending = new GroveCutsceneScreen(this, next); }
    public void showBossFight() {
        pending = new PalaceCutsceneScreen(this, () ->
                showBoss(BossDef.QUEEN_MUCK, () -> pending = new TransformationScreen(this, this::showVictory)));
    }

    /** Story lines, then the fight, then more story lines, then {@code after}. */
    public void showBoss(BossDef def, Runnable after) {
        Runnable won = () -> {
            save.bossAfter = -1;                                  // beaten: CONTINUE no longer sends the player back to it
            save.store();
            if (def.outro().isEmpty()) after.run();
            else pending = new BossStoryScreen(this, def, def.outro(), after, false);
        };
        Runnable fight = () -> pending = def.kind() == BossDef.Kind.CHASE
                ? new ChaseScreen(this, def, won)
                : new BossFightScreen(this, def, won);
        if (def.intro().isEmpty()) fight.run();
        else pending = new BossStoryScreen(this, def, def.intro(), fight, true);
    }
    public void showVictory() { pending = new VictoryScreen(this); }

    public void showConfirmNewGame() { pending = new ConfirmNewGameScreen(this); }

    /** CONTINUE: fight the boss that was still waiting, or resume the campaign right after the last completed level. */
    public void continueGame() {
        if (!save.hasPlaythrough()) return;
        BossDef waiting = save.bossAfter >= 0 ? BossDef.after(save.bossAfter) : null;
        if (waiting != null) {                                    // the player quit before beating this level's boss
            int next = Math.min(save.bossAfter + 1, Levels.CAMPAIGN.size() - 1);
            if (waiting == BossDef.QUEEN_MUCK) showBossFight();
            else showBoss(waiting, () -> startLevel(Levels.CAMPAIGN.get(next), next));
            return;
        }
        int i = Math.min(save.completed + 1, Levels.CAMPAIGN.size() - 1);
        startLevel(Levels.CAMPAIGN.get(i), i);
    }

    /** NEW GAME: asks first if there is a playthrough to lose, otherwise starts straight away. */
    public void newGame() {
        if (save.hasPlaythrough()) showConfirmNewGame();
        else startNewGame();
    }

    /** Resets story progress and attributes, then starts at level 1. */
    public void startNewGame() {
        save.newGame();
        startLevel(Levels.CAMPAIGN.get(0), 0);
    }

    // ------------------------------------------------------------------ key dispatch (from InputState)

    public void dispatchDirection(Direction d) { if (current != null) current.onDirection(d); }
    public void dispatchConfirm() { if (current != null) current.onConfirm(); }
    public void dispatchAction2() { if (current != null) current.onAction2(); }
    public void dispatchBoat() { if (current != null) current.onBoat(); }
    public void dispatchPause() { if (current != null) current.onPause(); }
    public void dispatchCancel() { if (current != null) current.onCancel(); }
}
