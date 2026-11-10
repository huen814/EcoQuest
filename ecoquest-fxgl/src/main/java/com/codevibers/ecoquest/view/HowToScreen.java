package com.codevibers.ecoquest.view;

import java.util.List;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Sfx;

public final class HowToScreen extends Screen {
    private final DialogBox box = new DialogBox(6, Ui.H - 150, Ui.W - 12, 144);

    public HowToScreen(EcoQuestApp app) {
        super(app);
        root.getChildren().addAll(Sprites.view("backdrop/title.png"), box);
    }

    @Override
    public void enter() {
        box.show(List.of(
                "WALK WITH ARROWS OR WASD. PRESS E, Z OR ENTER (A) TO PICK UP LITTER OR DROP IT INTO THE BIN YOU FACE.",
                "RIGHT BIN: +10 SCORE AND POLLUTION -5. WRONG BIN: -5 AND YOUR COMBO RESETS. CHAIN SORTS FOR UP TO x4.",
                "SEEDS GO INTO EMPTY PLANTING SPOTS: +20 AND A GROWING TREE. RIVER ITEMS ARE WORTH +25: ROW THE BOAT WITH B.",
                "SPACE EMPTIES A FULL BIN. ECO BOOST STARS GIVE x2 SCORE. LEFT-OVER GARBAGE RAISES POLLUTION: AT 100% YOU LOSE!",
                "FINISH THE MISSION AND REACH THE TARGET SCORE. 3 STARS NEED POLLUTION UNDER 25% AND 2 OR FEWER MISTAKES. P OR ESC PAUSES. M MUTES."));
    }

    @Override
    public void update(double tpf) { box.update(tpf); }

    @Override
    public void onConfirm() {
        app.sfx(Sfx.SELECT);
        if (box.advance()) app.showMainMenu();
    }

    @Override
    public void onCancel() { app.showMainMenu(); }
}
