package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.model.Attribute;
import com.codevibers.ecoquest.model.Progression;
import com.codevibers.ecoquest.model.Save;

import javafx.scene.Group;
import javafx.scene.paint.Color;

final class AttributesPanel extends Group {
    static final double WIDTH = 540;
    private static final double ROW = 24;

    private final double height = 78 + Attribute.COUNT * ROW;

    AttributesPanel(Save save) {
        getChildren().add(Ui.panel(WIDTH, height));
        Ui.Label title = new Ui.Label("ATTRIBUTES  LV " + save.level, 13, Ui.FRAME_OUT);
        title.setLayoutX(26);
        title.setLayoutY(20);
        String xp = save.level >= Progression.MAX_LEVEL ? "MAX LEVEL" : "XP " + save.xp + "/" + Progression.xpForNext(save.level);
        Ui.Label xpLabel = new Ui.Label(xp, 11, Ui.FRAME_IN);
        xpLabel.setLayoutX(WIDTH - 26 - xpLabel.textWidth());
        xpLabel.setLayoutY(22);
        getChildren().addAll(title, xpLabel);
        for (Attribute a : Attribute.values()) {
            int rank = save.attr(a);
            double y = 54 + a.ordinal() * ROW;
            Color c = rank > 0 ? Ui.INK : Color.web("#9a9a98");
            Ui.Label name = new Ui.Label(a.label.toUpperCase(), 11, c);
            name.setLayoutX(26);
            name.setLayoutY(y);
            Ui.Label pips = new Ui.Label("*".repeat(rank) + "-".repeat(a.max - rank), 11, rank > 0 ? Ui.FRAME_IN : c);
            pips.setLayoutX(WIDTH - 26 - pips.textWidth());
            pips.setLayoutY(y);
            getChildren().addAll(name, pips);
        }
        Ui.Label hint = new Ui.Label("ENTER OR ESC: BACK", 10, Ui.FRAME_IN);
        hint.setLayoutX(26);
        hint.setLayoutY(height - 28);
        getChildren().add(hint);
    }

    double height() { return height; }
}
