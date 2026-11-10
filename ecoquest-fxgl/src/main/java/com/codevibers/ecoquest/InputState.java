package com.codevibers.ecoquest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;

import com.codevibers.ecoquest.model.Direction;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Keyboard: held direction for walking, plus one-shot button events forwarded to the active screen. */
public final class InputState {
    private final EcoQuestApp app;
    private final Deque<Direction> held = new ArrayDeque<>();
    private final Set<KeyCode> down = EnumSet.noneOf(KeyCode.class);

    public InputState(EcoQuestApp app) { this.app = app; }

    /** The most recently pressed direction that is still held, or null. */
    public Direction current() { return held.peekFirst(); }

    public void clear() {
        held.clear();
        down.clear();
    }

    public void pressed(KeyEvent e) {
        KeyCode k = e.getCode();
        boolean fresh = down.add(k);
        Direction d = dir(k);
        if (d != null) {
            if (fresh) {
                held.remove(d);
                held.addFirst(d);
                app.dispatchDirection(d);
            }
            e.consume();
            return;
        }
        if (!fresh) return;
        switch (k) {
            case E, Z, ENTER -> app.dispatchConfirm();
            case SPACE -> app.dispatchAction2();
            case B -> app.dispatchBoat();
            case P -> app.dispatchPause();
            case ESCAPE, X, BACK_SPACE -> app.dispatchCancel();
            case M -> app.toggleMute();
            default -> { return; }
        }
        e.consume();
    }

    public void released(KeyEvent e) {
        down.remove(e.getCode());
        Direction d = dir(e.getCode());
        if (d != null) held.remove(d);
    }

    private static Direction dir(KeyCode k) {
        return switch (k) {
            case UP, W -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, A -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };
    }
}
