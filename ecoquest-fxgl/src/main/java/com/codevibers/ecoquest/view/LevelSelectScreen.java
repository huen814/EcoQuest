package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.LevelConfig;
import com.codevibers.ecoquest.model.Levels;
import com.codevibers.ecoquest.model.Sfx;

public final class LevelSelectScreen extends ListScreen {
    public LevelSelectScreen(EcoQuestApp app) {
        super(app, "SELECT LEVEL");
        index = Math.min(app.save().unlocked, Levels.CAMPAIGN.size() - 1);
    }

    @Override int rowCount() { return Levels.CAMPAIGN.size(); }
    @Override boolean enabled(int i) { return i <= app.save().unlocked; }

    @Override String left(int i) { return (i + 1) + " " + Levels.CAMPAIGN.get(i).name.toUpperCase(); }

    @Override String right(int i) {
        if (!enabled(i)) return "LOCKED";
        int s = app.save().stars[i];
        return Levels.CAMPAIGN.get(i).difficulty.toUpperCase() + " " + "*".repeat(s) + "-".repeat(3 - s);
    }

    @Override String describe(int i) {
        LevelConfig c = Levels.CAMPAIGN.get(i);
        return enabled(i) ? c.story[0] + "\nTARGET " + c.target + " POINTS. " + c.tip.toUpperCase() : "CLEAR THE PREVIOUS LEVEL TO UNLOCK.";
    }

    @Override void pick(int i) {
        if (!enabled(i)) { app.sfx(Sfx.DENY); return; }
        app.sfx(Sfx.SELECT);
        app.startLevel(Levels.CAMPAIGN.get(i), i);
    }
}
