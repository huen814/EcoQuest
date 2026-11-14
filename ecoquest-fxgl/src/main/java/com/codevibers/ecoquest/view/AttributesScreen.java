package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Attribute;
import com.codevibers.ecoquest.model.Progression;
import com.codevibers.ecoquest.model.Save;
import com.codevibers.ecoquest.model.Sfx;

public final class AttributesScreen extends ListScreen {
    private final Runnable back;

    public AttributesScreen(EcoQuestApp app, Runnable back) {
        super(app, "ATTRIBUTES  LV " + app.save().level);
        this.back = back;
    }

    private static Attribute at(int i) { return Attribute.values()[i]; }

    @Override int rowCount() { return Attribute.COUNT; }

    @Override boolean enabled(int i) { return app.save().attr(at(i)) > 0; }

    @Override String left(int i) { return at(i).label.toUpperCase(); }

    @Override String right(int i) {
        Attribute a = at(i);
        int r = app.save().attr(a);
        return (a.isPrimary() ? "MAIN " : "MINOR ") + "*".repeat(r) + "-".repeat(a.max - r);
    }

    @Override String describe(int i) {
        Attribute a = at(i);
        Save s = app.save();
        String xp = s.level >= Progression.MAX_LEVEL ? "MAX LEVEL" : "XP " + s.xp + "/" + Progression.xpForNext(s.level);
        return a.label.toUpperCase() + "  " + s.attr(a) + "/" + a.max + "\n" + a.blurb.toUpperCase()
                + "\nLEVEL-UPS CAN OFFER THIS AS A CHOICE.   " + xp;
    }

    @Override void pick(int i) { app.sfx(Sfx.CURSOR); }

    @Override
    public void onCancel() { back.run(); }
}
