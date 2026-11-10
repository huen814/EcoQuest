package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Direction;

import javafx.scene.Group;

/** One full-window screen. The app adds root to the UI layer between enter() and exit(). */
public abstract class Screen {
    protected final EcoQuestApp app;
    protected final Group root = new Group();

    protected Screen(EcoQuestApp app) { this.app = app; }

    public Group root() { return root; }

    public void enter() { }
    public void exit() { }
    public void update(double tpf) { }

    /** Arrow keys / WASD (fires once per key press). */
    public void onDirection(Direction d) { }
    /** A button: E, Z or Enter. */
    public void onConfirm() { }
    /** Space. */
    public void onAction2() { }
    /** B. */
    public void onBoat() { }
    /** P: pause / resume. */
    public void onPause() { }
    /** Esc, X or Backspace. */
    public void onCancel() { }
}
