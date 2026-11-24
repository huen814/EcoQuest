package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.Save;
import com.codevibers.ecoquest.model.Sfx;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * The settings panel, shared by the main menu (inside {@link SettingsScreen}) and the pause menus (level play, boss fights
 * and the chase). Five tabs: AUDIO, DISPLAY, CONTROLS, GAMEPLAY and DATA.
 * Keys: UP/DOWN pick a row (the top row is the tab bar), LEFT/RIGHT change a tab or a value, ENTER toggles, ESC closes.
 */
final class SettingsPanel extends Group {
    static final double WIDTH = 580, HEIGHT = 340;
    private static final double PAD = 26, TOP = 96, ROW = 30;
    private static final String[] TABS = {"AUDIO", "DISPLAY", "CONTROLS", "GAMEPLAY", "DATA"};
    private static final Color GREY = Color.web("#9a9a98");

    /** One line of a tab. {@code change} is null for plain information; it gets -1, +1 (LEFT/RIGHT) or 0 (ENTER). */
    private record Row(String name, Supplier<String> value, IntConsumer change, boolean enabled) { }

    private final EcoQuestApp app;
    private final Save save;
    private final boolean inGame;
    private final Runnable onClose;

    private final Group body = new Group();
    private final Ui.Label[] tabLabels = new Ui.Label[TABS.length];
    private final Polygon cursor = new Polygon(0, 0, 10, 6, 0, 12);
    private final List<Row> rows = new ArrayList<>();
    private final List<Ui.Label> valueLabels = new ArrayList<>();
    private int tab;
    private int index;          // 0 = the tab bar, 1.. = the selectable rows of the tab, counted in selectables()
    private double t;

    SettingsPanel(EcoQuestApp app, boolean inGame, Runnable onClose) {
        this.app = app;
        this.save = app.save();
        this.inGame = inGame;
        this.onClose = onClose;

        getChildren().add(Ui.panel(WIDTH, HEIGHT));
        Ui.Label title = new Ui.Label("SETTINGS", 14, Ui.FRAME_OUT);
        title.setLayoutX(PAD);
        title.setLayoutY(18);
        getChildren().add(title);
        double x = PAD;
        for (int i = 0; i < TABS.length; i++) {
            Ui.Label l = new Ui.Label(TABS[i], 11, Ui.INK);
            l.setLayoutX(x);
            l.setLayoutY(56);
            x += l.textWidth() + 26;
            tabLabels[i] = l;
            final int at = i;
            l.setOnMouseClicked(e -> { index = 0; tab = at; app.sfx(Sfx.CURSOR); build(); });
            getChildren().add(l);
        }
        Ui.Label hint = new Ui.Label("UP/DOWN: PICK   LEFT/RIGHT: CHANGE   ESC: BACK", 9, Ui.FRAME_IN);
        hint.setLayoutX(PAD);
        hint.setLayoutY(HEIGHT - 28);
        cursor.setFill(Ui.FRAME_OUT);
        getChildren().addAll(body, hint, cursor);
        build();
    }

    // ------------------------------------------------------------------ tabs

    private void build() {
        rows.clear();
        valueLabels.clear();
        body.getChildren().clear();
        switch (tab) {
            case 0 -> {
                rows.add(new Row("MUSIC VOLUME", () -> bar(save.musicVol), d -> {
                    save.musicVol = step(save.musicVol, d);
                    Audio.setMusicScale(save.musicVol / 10.0);
                }, true));
                rows.add(new Row("SFX VOLUME", () -> bar(save.sfxVol), d -> {
                    save.sfxVol = step(save.sfxVol, d);
                    Audio.setSfxScale(save.sfxVol / 10.0);
                }, true));
                rows.add(new Row("SOUND", () -> save.muted ? "OFF" : "ON", d -> app.toggleMute(), true));
            }
            case 1 -> {
                rows.add(new Row("FULLSCREEN", () -> save.fullscreen ? "ON" : "OFF", d -> app.setFullscreen(!save.fullscreen), true));
                rows.add(new Row("SCREEN SHAKE", () -> save.screenShake ? "ON" : "OFF", d -> save.screenShake = !save.screenShake, true));
            }
            case 2 -> {
                rows.add(new Row("AIM MODE", () -> save.autoAim ? "AUTO AIM" : "CURSOR AIM", d -> app.toggleAutoAim(), true));
                rows.add(new Row("MOVE", () -> "WASD / ARROWS", null, false));
                rows.add(new Row("INTERACT", () -> "E / Z / ENTER", null, false));
                rows.add(new Row("EMPTY BIN", () -> "SPACE", null, false));
                rows.add(new Row("BOAT", () -> "B", null, false));
                rows.add(new Row("FIRE", () -> "CLICK / ENTER / Z", null, false));
                rows.add(new Row("SHOTS GO", () -> save.autoAim ? "AT THE BOSS" : "AT THE CURSOR", null, false));
                rows.add(new Row("PAUSE", () -> "P / ESC", null, false));
            }
            case 3 -> rows.add(new Row("LEVEL BRIEFING", () -> save.levelBriefing ? "ON" : "OFF",
                    d -> save.levelBriefing = !save.levelBriefing, true));
            default -> {
                rows.add(new Row("VERSION", () -> EcoQuestApp.VERSION, null, false));
                rows.add(new Row(inGame ? "RESET PROGRESS (MAIN MENU ONLY)" : "RESET PROGRESS",
                        () -> "", d -> { if (!inGame) app.showConfirmNewGame(); }, !inGame));
            }
        }
        int sel = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            boolean live = r.change() != null && r.enabled();
            Color c = r.change() == null ? Ui.FRAME_IN : (live ? Ui.INK : GREY);
            Ui.Label name = new Ui.Label(r.name(), 11, c);
            name.setLayoutX(PAD + 26);
            name.setLayoutY(TOP + i * ROW);
            Ui.Label value = new Ui.Label(r.value().get(), 11, r.change() == null ? Ui.INK : c);
            valueLabels.add(value);
            body.getChildren().addAll(name, value);
            if (r.change() != null) {       // mouse: hover picks the row, click on the left half lowers, right half raises (toggles just flip)
                final int at = ++sel;
                Rectangle hit = new Rectangle(WIDTH - 2 * PAD + 20, ROW - 4, Color.rgb(0, 0, 0, 0.01));
                hit.setLayoutX(PAD - 6);
                hit.setLayoutY(TOP + i * ROW - 6);
                hit.setOnMouseEntered(e -> { index = at; t = 0; place(); });
                hit.setOnMouseClicked(e -> { index = at; place(); apply(e.getX() < hit.getWidth() / 2 ? -1 : 1); });
                body.getChildren().add(hit);
            }
        }
        refreshValues();
        refreshTabs();
        place();
    }

    private void refreshValues() {
        for (int i = 0; i < rows.size(); i++) {
            Ui.Label v = valueLabels.get(i);
            v.setText(rows.get(i).value().get());
            v.setLayoutX(WIDTH - PAD - v.textWidth());
            v.setLayoutY(TOP + i * ROW);
        }
    }

    private void refreshTabs() {
        for (int i = 0; i < TABS.length; i++) {
            tabLabels[i].setText(i == tab ? "[" + TABS[i] + "]" : TABS[i]);
            tabLabels[i].setFill(i == tab ? Ui.FRAME_OUT : Ui.INK);
        }
    }

    /** Row numbers (into {@code rows}) that the cursor can stop on. */
    private List<Integer> selectables() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).change() != null) out.add(i);
        return out;
    }

    private void place() {
        if (index == 0) {
            cursor.setLayoutX(PAD - 20);
            cursor.setLayoutY(56);
        } else {
            int row = selectables().get(index - 1);
            cursor.setLayoutX(PAD + 4);
            cursor.setLayoutY(TOP + row * ROW);
        }
    }

    private static String bar(int v) { return "*".repeat(v) + "-".repeat(10 - v) + " " + v; }

    private static int step(int v, int d) { return d == 0 ? (v + 1) % 11 : Math.max(0, Math.min(10, v + d)); }

    // ------------------------------------------------------------------ input

    void update(double tpf) {
        t += tpf;
        cursor.setVisible(Math.sin(t * 9) > -0.6);
    }

    void onDirection(Direction d) {
        int n = selectables().size();
        switch (d) {
            case UP -> { index = Math.floorMod(index - 1, n + 1); t = 0; app.sfx(Sfx.CURSOR); place(); }
            case DOWN -> { index = Math.floorMod(index + 1, n + 1); t = 0; app.sfx(Sfx.CURSOR); place(); }
            case LEFT -> side(-1);
            case RIGHT -> side(1);
            default -> { }
        }
    }

    private void side(int delta) {
        if (index == 0) {
            tab = Math.floorMod(tab + delta, TABS.length);
            app.sfx(Sfx.CURSOR);
            build();
        } else {
            apply(delta);
        }
    }

    void onConfirm() {
        if (index == 0) side(1);
        else apply(0);
    }

    private void apply(int delta) {
        Row r = rows.get(selectables().get(index - 1));
        if (!r.enabled()) { app.sfx(Sfx.DENY); return; }
        r.change().accept(delta);
        save.store();
        app.sfx(Sfx.SELECT);
        refreshValues();
    }

    void onCancel() {
        save.store();
        onClose.run();
    }
}
