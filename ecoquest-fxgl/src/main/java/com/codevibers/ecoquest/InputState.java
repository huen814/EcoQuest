package com.codevibers.ecoquest;

import java.util.EnumSet;
import java.util.Set;

import com.codevibers.ecoquest.model.Direction;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Keyboard: held direction for walking, plus one-shot button events forwarded to the active screen. */
public final class InputState {
    private final EcoQuestApp app;
    private final Set<KeyCode> down = EnumSet.noneOf(KeyCode.class);

    public InputState(EcoQuestApp app) { this.app = app; }

    /** Current combined movement direction. Opposing keys cancel each other. */
    public Direction current() {
        boolean left = down.contains(KeyCode.LEFT) || down.contains(KeyCode.A);
        boolean right = down.contains(KeyCode.RIGHT) || down.contains(KeyCode.D);
        boolean up = down.contains(KeyCode.UP) || down.contains(KeyCode.W);
        boolean downKey = down.contains(KeyCode.DOWN) || down.contains(KeyCode.S);
        int x = (right ? 1 : 0) - (left ? 1 : 0);
        int y = (downKey ? 1 : 0) - (up ? 1 : 0);
        if (x == 0 && y == 0) return null;
        if (x < 0 && y < 0) return Direction.UP_LEFT;
        if (x > 0 && y < 0) return Direction.UP_RIGHT;
        if (x < 0 && y > 0) return Direction.DOWN_LEFT;
        if (x > 0 && y > 0) return Direction.DOWN_RIGHT;
        if (x < 0) return Direction.LEFT;
        if (x > 0) return Direction.RIGHT;
        return y < 0 ? Direction.UP : Direction.DOWN;
    }

    public void clear() { down.clear(); }

    /** True while a boss-fight fire key is held. */
    public boolean confirmHeld() {
        return down.contains(KeyCode.ENTER) || down.contains(KeyCode.Z);
    }

    public void pressed(KeyEvent e) {
        KeyCode k = e.getCode();
        boolean fresh = down.add(k);
        Direction d = dir(k);
        if (d != null) {
            if (fresh) app.dispatchDirection(d);
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
