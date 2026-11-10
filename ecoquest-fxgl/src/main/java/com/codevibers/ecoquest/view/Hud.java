package com.codevibers.ecoquest.view;

import com.codevibers.ecoquest.model.GameSession;
import com.codevibers.ecoquest.model.LevelConfig;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Two Emerald-style panels across the top: level / mission / score, and the pollution meter. */
public final class Hud extends Group {
    private static final double LEFT_W = 440;
    private static final double RIGHT_W = Ui.W - LEFT_W - 18;

    private final LevelConfig cfg;
    private final Ui.Label name;
    private final Ui.Label score = new Ui.Label("", 11, Ui.INK);
    private final Ui.Label mission = new Ui.Label("", 10, Ui.FRAME_IN);
    private final Ui.Label status = new Ui.Label("", 10, Ui.INK);
    private final Ui.Label polLabel = new Ui.Label("POLLUTION", 10, Ui.INK);
    private final Ui.Label polValue = new Ui.Label("", 10, Ui.INK);
    private final Ui.Label clock = new Ui.Label("", 10, Ui.INK);
    private final Rectangle polFill = new Rectangle(0, 14);
    private final String[] last = new String[5];
    private Runnable onPause = () -> { };

    public Hud(LevelConfig cfg) {
        this.cfg = cfg;
        name = new Ui.Label(cfg.name.toUpperCase(), 11, Ui.FRAME_OUT);

        Group left = Ui.panel(LEFT_W, 84);
        left.setLayoutX(6);
        left.setLayoutY(6);
        Group right = Ui.panel(RIGHT_W, 84);
        right.setLayoutX(Ui.W - RIGHT_W - 6);
        right.setLayoutY(6);

        place(name, 24, 18);
        place(score, 24, 18);
        place(mission, 24, 42);
        place(status, 24, 64);

        double rx = Ui.W - RIGHT_W - 6;
        place(polLabel, rx + 18, 18);
        place(polValue, rx + 18, 18);
        Rectangle bg = new Rectangle(rx + 18, 42, RIGHT_W - 36, 14);
        bg.setFill(Color.web("#404858"));
        bg.setArcWidth(6);
        bg.setArcHeight(6);
        polFill.setX(rx + 18);
        polFill.setY(42);
        polFill.setArcWidth(6);
        polFill.setArcHeight(6);
        place(clock, rx + 18, 64);

        getChildren().addAll(left, right, name, score, mission, status, polLabel, polValue, bg, polFill, clock);

        // clickable pause button (P / Esc do the same)
        Rectangle btnBg = new Rectangle(88, 22, Ui.FRAME_IN);
        btnBg.setArcWidth(8);
        btnBg.setArcHeight(8);
        btnBg.setStroke(Ui.FRAME_OUT);
        btnBg.setStrokeWidth(2);
        Ui.Label btnText = new Ui.Label("II PAUSE", 9, Color.WHITE);
        btnText.setLayoutX(10);
        btnText.setLayoutY(7);
        Group btn = new Group(btnBg, btnText);
        btn.setLayoutX(Ui.W - 6 - 18 - 88);
        btn.setLayoutY(60);
        btn.setCursor(Cursor.HAND);
        btn.setOnMouseEntered(e -> btnBg.setFill(Ui.FRAME_OUT));
        btn.setOnMouseExited(e -> btnBg.setFill(Ui.FRAME_IN));
        btn.setOnMouseClicked(e -> {
            onPause.run();
            e.consume();
        });
        getChildren().add(btn);
    }

    public void setOnPause(Runnable r) { onPause = r; }

    private static void place(Ui.Label l, double x, double y) {
        l.setLayoutX(x);
        l.setLayoutY(y);
    }

    private void set(int slot, Ui.Label l, String text) {
        if (!text.equals(last[slot])) {
            last[slot] = text;
            l.setText(text);
        }
    }

    public void update(GameSession s) {
        int pol = (int) Math.min(100, s.pollution());
        String scoreText = cfg.isCampaign() ? "SCORE " + s.score() + "/" + cfg.target : "SCORE " + s.score();
        set(0, score, scoreText);
        score.setLayoutX(6 + LEFT_W - 18 - score.textWidth());

        set(1, mission, missionText(s));

        StringBuilder st = new StringBuilder();
        if (s.combo() > 0) st.append("COMBO x").append(s.combo()).append("  ");
        st.append("CARRY ").append(s.player().carried().size()).append('/').append(s.carryCapacity());
        if (s.boostSeconds() > 0) st.append("  BOOST ").append((int) Math.ceil(s.boostSeconds())).append('S');
        st.append("  PHP ").append(s.money());
        set(2, status, st.toString());

        set(3, polValue, pol + "%");
        polValue.setLayoutX(Ui.W - 6 - 18 - polValue.textWidth());
        polFill.setWidth((RIGHT_W - 36) * pol / 100.0);
        polFill.setFill(pol < 40 ? Color.web("#40c060") : pol < 75 ? Color.web("#f0c030") : Color.web("#e04838"));

        String c;
        switch (cfg.mode) {
            case TIME -> c = "TIME " + (int) Math.ceil(s.timeLeft()) + "S";
            case ZEN -> c = "P: PAUSE";
            default -> c = "TIME " + (int) s.elapsed() / 60 + ":" + String.format("%02d", (int) s.elapsed() % 60);
        }
        set(4, clock, c);
    }

    private String missionText(GameSession s) {
        switch (cfg.mode) {
            case TIME: return "SORT AS MUCH AS YOU CAN";
            case ENDLESS: return "SURVIVE: " + (int) s.elapsed() + "S";
            case SPRINT: return "SORTED " + s.sorted() + "/25";
            case ZEN: return "SORTED " + s.sorted() + "  NO RUSH";
            default: break;
        }
        if (s.missionDone()) return "MISSION DONE! REACH " + cfg.target + " PTS";
        StringBuilder sb = new StringBuilder();
        if (cfg.missionSort > 0) sb.append("SORT ").append(s.sorted()).append('/').append(cfg.missionSort).append("  ");
        if (cfg.missionPlant > 0) sb.append("PLANT ").append(s.planted()).append('/').append(cfg.missionPlant).append("  ");
        if (cfg.missionClean > 0) sb.append("FISH ").append(s.cleaned()).append('/').append(cfg.missionClean);
        return sb.toString().trim();
    }
}
