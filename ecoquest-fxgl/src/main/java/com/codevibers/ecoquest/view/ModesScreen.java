package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.LevelConfig;
import com.codevibers.ecoquest.model.LevelConfig.Mode;
import com.codevibers.ecoquest.model.Levels;
import com.codevibers.ecoquest.model.Sfx;

/** Game Modes menu (opened from the main menu): the five score modes, then Boss Replay. Back returns to the main menu. */
public final class ModesScreen extends ListScreen {
    public ModesScreen(EcoQuestApp app) { super(app, "GAME MODES"); }

    /** The extra row after the modes opens Boss Replay. */
    private boolean isBossReplayRow(int i) { return i == Levels.MODES.size(); }

    private int bossesBeaten() {
        int n = 0;
        for (BossDef d : BossDef.values()) if (app.save().hasBeatenBoss(d.id())) n++;
        return n;
    }

    @Override int rowCount() { return Levels.MODES.size() + 1; }
    @Override boolean enabled(int i) { return true; }

    @Override String left(int i) { return isBossReplayRow(i) ? "BOSS REPLAY" : Levels.MODES.get(i).name.toUpperCase(); }

    @Override String right(int i) {
        if (isBossReplayRow(i)) return bossesBeaten() + "/" + BossDef.values().length + " >";
        LevelConfig m = Levels.MODES.get(i);
        int best = app.save().high[m.highSlot];
        if (best == 0) return "NO RECORD";
        return "BEST " + best + (m.mode == Mode.TIME || m.mode == Mode.ZEN ? " PTS" : "S");
    }

    @Override String describe(int i) {
        if (isBossReplayRow(i)) return "REPLAY THE BOSSES YOU HAVE BEATEN IN THE STORY.\n15-25 COINS PER CLEAR, NO XP. YOUR STORY IS NOT CHANGED.";
        LevelConfig m = Levels.MODES.get(i);
        return m.difficulty.toUpperCase() + "\n" + m.story[1];
    }

    @Override void pick(int i) {
        app.sfx(Sfx.SELECT);
        if (isBossReplayRow(i)) { app.showBossReplay(); return; }
        app.startLevel(Levels.MODES.get(i), -1);
    }
}
