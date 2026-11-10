package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Levels;
import com.codevibers.ecoquest.model.Save;
import com.codevibers.ecoquest.model.Sfx;

public final class ShopScreen extends ListScreen {
    private static final int MAX_LEVEL = 3;

    public ShopScreen(EcoQuestApp app) { super(app, "UPGRADE SHOP"); }

    @Override int rowCount() { return Levels.UPGRADES.length; }
    @Override boolean enabled(int i) { return true; }
    @Override String left(int i) { return Levels.UPGRADES[i][0].toUpperCase(); }

    @Override String right(int i) {
        int lv = app.save().up[i];
        String pips = "*".repeat(lv) + "-".repeat(MAX_LEVEL - lv);
        return lv >= MAX_LEVEL ? pips + " MAX" : pips + " PHP " + Save.upgradeCost(lv);
    }

    @Override String describe(int i) {
        return Levels.UPGRADES[i][1].toUpperCase() + "\nWALLET: PHP " + app.save().wallet
                + "  (EARN PHP BY SORTING, MISSIONS AND FACTORY DEALS)";
    }

    @Override void pick(int i) {
        Save s = app.save();
        int lv = s.up[i];
        int cost = Save.upgradeCost(lv);
        if (lv >= MAX_LEVEL || s.wallet < cost) { app.sfx(Sfx.DENY); return; }
        s.wallet -= cost;
        s.up[i]++;
        s.store();
        app.sfx(Sfx.BUY);
    }
}
