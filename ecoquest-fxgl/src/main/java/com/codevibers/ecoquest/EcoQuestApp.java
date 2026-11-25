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
import com.codevibers.ecoquest.model.Sibling;

import com.codevibers.ecoquest.view.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

public class EcoQuestApp extends GameApplication {
    public static final String VERSION = "0.4.0";
    private boolean fullscreenPending;

    private Save save;
    private InputState input;
    private Screen current;
    private Screen pending;
    /** Boss Replay state: true while a replayed fight is up; the character last picked on the replay screen. */
    private boolean replaying;
    private Sibling replayCharacter;
    /** Reward line from the last replayed fight, shown once on the Boss Replay screen. */
    private String replayNotice = "";

    public static void main(String[] args) { launch(args); }

    @Override
    protected void initSettings(GameSettings s) {
        s.setWidth(Ui.W);
        s.setHeight(Ui.H);
        s.setTitle("EcoQuest: Guardians of the Green City");
        s.setVersion(VERSION);
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
        Audio.setMusicScale(save.musicVol / 10.0);
        Audio.setSfxScale(save.sfxVol / 10.0);
        fullscreenPending = save.fullscreen;
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
            if (current.titleMusic()) Audio.playMusic("title.wav", 0.45);
            else Audio.stopMusicIf("title.wav");
            current.enter();
        }
        if (fullscreenPending && current != null && current.root().getScene() != null) {
            fullscreenPending = false;
            applyFullscreen();
        }
        if (current != null) current.update(tpf);
    }

    /** Settings: switches fullscreen on or off and remembers it. */
    public void setFullscreen(boolean on) {
        save.fullscreen = on;
        save.store();
        applyFullscreen();
    }

    private void applyFullscreen() {
        if (current == null || current.root().getScene() == null) return;
        if (current.root().getScene().getWindow() instanceof javafx.stage.Stage stage) {
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);   // ESC is the pause key
            stage.setFullScreenExitHint("");
            stage.setFullScreen(save.fullscreen);
        }
    }

    public void showSettings() { pending = new SettingsScreen(this); }

    // ------------------------------------------------------------------ services for screens

    public Save save() { return save; }
    public InputState input() { return input; }
    public void sfx(Sfx s) { Audio.play(s); }
    /** Queen Muck's "ho ho ho". */
    public void laugh() { Audio.laugh(); }

    /** Boss fights: switches between auto aim and aiming with the cursor. */
    public void toggleAutoAim() {
        save.autoAim = !save.autoAim;
        save.store();
    }

    /** The reward line of the last replay, once (empty after it has been read). */
    public String takeReplayNotice() {
        String n = replayNotice;
        replayNotice = "";
        return n;
    }

    /**
     * A boss was beaten (story or replay). Always keeps the fastest time. A replay also pays 15-25 coins (more for
     * keeping your hearts) plus a one-time 30 coin bonus the first time it beats the saved record. No XP.
     */
    public void reportBossClear(BossDef def, double seconds, int hpLeft, int maxHp) {
        boolean record = save.recordBossTime(def.id(), seconds);
        if (replaying) {
            int coins = 15 + (int) Math.round(10.0 * Math.max(0, hpLeft - 1) / Math.max(1, maxHp - 1));
            StringBuilder note = new StringBuilder("+" + coins + " COINS");
            if (record) {
                note.append("   NEW RECORD ").append(BossReplayScreen.time(seconds));
                if (save.claimBossRecordBonus(def.id())) { coins += 30; note.append("  +30 BONUS"); }
            }
            save.wallet += coins;
            replayNotice = note.toString();
        }
        save.store();
    }

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
    public void showMainMenu() { endBossReplay(); pending = new MainMenuScreen(this); }
    public void showLevelSelect() {
        if (Progression.hasOffer(save)) showAttributeChoice(this::showLevelSelect);
        else pending = new LevelSelectScreen(this);
    }
    public void showAttributeChoice(Runnable next) { pending = new AttributeChoiceScreen(this, next); }
    public void showModes() { endBossReplay(); pending = new ModesScreen(this); }

    // ------------------------------------------------------------------ boss replay (Main Menu -> Game Modes -> Boss Replay)

    /** The Boss Replay selection screen. */
    public void showBossReplay() { endBossReplay(); pending = new BossReplayScreen(this); }

    /** The character shown on the replay screen: the last one picked there, otherwise the story's sibling. */
    public Sibling replayCharacter() { return replayCharacter != null ? replayCharacter : save.sibling(); }
    public void setReplayCharacter(Sibling s) { replayCharacter = s; }

    /**
     * Replays a beaten boss as {@code character} ("Lena", "Adam", or SISTER / BROTHER). No intro or outro story, no XP,
     * no coins and no save changes: when the fight ends the player lands back on the Boss Replay screen.
     */
    public void startBossReplay(String bossId, String character) {
        BossDef def = BossDef.byId(bossId);
        if (def == null || !save.hasBeatenBoss(def.id())) { showBossReplay(); return; }
        Sibling who = save.sibling();
        for (Sibling s : Sibling.values()) {
            if (s.name.equalsIgnoreCase(character) || s.label.equalsIgnoreCase(character) || s.id.equalsIgnoreCase(character)) who = s;
        }
        replayCharacter = who;
        save.setReplaySibling(who);     // only for this fight; never written to the save file
        replaying = true;
        Runnable done = this::showBossReplay;
        pending = def.kind() == BossDef.Kind.CHASE ? new ChaseScreen(this, def, done) : new BossFightScreen(this, def, done);
    }

    /** X during a fight: back to the replay screen when replaying, otherwise to the main menu. */
    public void quitBossFight() { if (replaying) showBossReplay(); else showMainMenu(); }

    private void endBossReplay() {
        replaying = false;
        save.clearReplaySibling();
    }
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
            save.markBossBeaten(def.id());                        // unlocks it in Boss Replay
            save.store();
            if (def.outro().isEmpty()) after.run();
            else pending = new BossStoryScreen(this, def, def.outro(), after, false);
        };
        Runnable fight = () -> pending = def.kind() == BossDef.Kind.CHASE
                ? new ChaseScreen(this, def, won)
                : new BossFightScreen(this, def, won);
        if (def == BossDef.EXPERIMENT) pending = new KnightCutsceneScreen(this, fight);
        else if (def.intro().isEmpty()) fight.run();
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
