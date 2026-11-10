package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.LevelConfig;
import com.codevibers.ecoquest.model.LevelConfig.Mode;
import com.codevibers.ecoquest.model.Levels;
import com.codevibers.ecoquest.model.Sfx;

public final class ModesScreen extends ListScreen {
    public ModesScreen(EcoQuestApp app) { super(app, "GAME MODES"); }

    @Override int rowCount() { return Levels.MODES.size(); }
    @Override boolean enabled(int i) { return true; }
    @Override String left(int i) { return Levels.MODES.get(i).name.toUpperCase(); }

    @Override String right(int i) {
        LevelConfig m = Levels.MODES.get(i);
        int best = app.save().high[m.highSlot];
        if (best == 0) return "NO RECORD";
        return "BEST " + best + (m.mode == Mode.TIME || m.mode == Mode.ZEN ? " PTS" : "S");
    }

    @Override String describe(int i) {
        LevelConfig m = Levels.MODES.get(i);
        return m.difficulty.toUpperCase() + "\n" + m.story[1];
    }

    @Override void pick(int i) {
        app.sfx(Sfx.SELECT);
        app.startLevel(Levels.MODES.get(i), -1);
    }
}
