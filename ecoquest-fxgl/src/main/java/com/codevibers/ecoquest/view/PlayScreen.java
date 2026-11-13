package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.almasb.fxgl.texture.AnimationChannel;
import com.codevibers.ecoquest.EcoQuestApp;
import com.codevibers.ecoquest.model.Bin;
import com.codevibers.ecoquest.model.BinType;
import com.codevibers.ecoquest.model.Direction;
import com.codevibers.ecoquest.model.GameSession;
import com.codevibers.ecoquest.model.LevelConfig;
import com.codevibers.ecoquest.model.Levels;
import com.codevibers.ecoquest.model.Pickup;
import com.codevibers.ecoquest.model.PlantingSpot;
import com.codevibers.ecoquest.model.Player;
import com.codevibers.ecoquest.model.SessionListener;
import com.codevibers.ecoquest.model.Sfx;
import com.codevibers.ecoquest.model.Sibling;
import com.codevibers.ecoquest.model.WasteItem;

import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * The gameplay screen. The rules live in {@link GameSession}; this class turns that state into
 * FXGL entities (tiles, items, bins, trees, the Guardian), handles grid walking with smooth
 * tile-to-tile movement, and shows the story / offer / result overlays in the Emerald dialog style.
 */
public final class PlayScreen extends Screen implements SessionListener {
    private enum State { BRIEF, PLAY, PAUSED, OFFER, RESULT }

    private static final int PX = Sprites.PX;
    private static final int PLAYER_H = 72;
    private static final double TURN_PAUSE = 0.09;

    private final LevelConfig cfg;
    private final int levelIndex;
    private final GameSession session;
    private final Random rng = new Random();
    private State state = State.BRIEF;

    // world
    private MapView mapView;
    private final Map<Integer, ItemView> itemViews = new HashMap<>();
    private final List<BinView> binViews = new ArrayList<>();
    private final List<SpotView> spotViews = new ArrayList<>();
    private Entity truckEntity;

    // the Guardian
    private Entity playerEntity;
    private AnimatedTexture ptex;
    private final AnimationChannel[][][] channels = new AnimationChannel[2][4][2];
    private final Group carryGroup = new Group();
    private String carrySignature = "";
    private double px;
    private double py;
    private double fromX;
    private double fromY;
    private double toX;
    private double toY;
    private double moveT;
    private double moveDur = 0.2;
    private boolean moving;
    private double turnPause;
    private double time;

    // overlays (UI layer)
    private final Rectangle wash = new Rectangle(Ui.W, Ui.H, Color.web("#8c8aa0"));
    private final Rectangle smog = new Rectangle(Ui.W, Ui.H, Color.web("#463214"));
    private final Rectangle tint;
    private final Group rain = new Group();
    private final Group petals = new Group();
    private final Group fxLayer = new Group();
    private final List<Spark> sparks = new ArrayList<>();
    private final Rectangle vignette = new Rectangle(Ui.W, Ui.H);
    private String shownSound = "";
    private final Group tag = new Group();
    private String tagText = "";
    private final Hud hud;
    private final DialogBox toastBox = new DialogBox(6, 98, Ui.W - 12, 64); // slim banner under the HUD so it never hides the bins
    private final DialogBox dialog = new DialogBox(6, Ui.H - 114, Ui.W - 12, 108);
    private final Group overlay = new Group();
    private MenuBox menu;

    private double endTimer = -1;
    private double shake;

    public PlayScreen(EcoQuestApp app, LevelConfig cfg, int levelIndex) {
        super(app);
        this.cfg = cfg;
        this.levelIndex = levelIndex;
        this.session = new GameSession(cfg, levelIndex, app.save(), rng);
        this.session.setListener(this);
        this.hud = new Hud(cfg);
        this.hud.setOnPause(this::togglePause);

        wash.setOpacity(0);
        wash.setMouseTransparent(true);
        smog.setOpacity(0);
        smog.setMouseTransparent(true);
        tint = new Rectangle(Ui.W, Ui.H, Color.rgb(cfg.tintR, cfg.tintG, cfg.tintB, Math.max(0, Math.min(1, cfg.tintA))));
        tint.setMouseTransparent(true);
        tint.setVisible(cfg.tintA > 0);
        for (int i = 0; i < 40; i++) {
            Line l = new Line(0, 0, -4, 10);
            l.setStroke(Color.rgb(220, 235, 255, 0.55));
            l.setStrokeWidth(2);
            l.setLayoutX(rng.nextDouble() * Ui.W);
            l.setLayoutY(rng.nextDouble() * Ui.H);
            rain.getChildren().add(l);
        }
        rain.setVisible(cfg.rain);
        rain.setMouseTransparent(true);
        tag.setVisible(false);

        // dreamy look: soft violet vignette + drifting pastel petals
        vignette.setFill(new RadialGradient(0, 0, Ui.W / 2.0, Ui.H / 2.0, Ui.W * 0.62, false, CycleMethod.NO_CYCLE,
                new Stop(0.55, Color.rgb(80, 60, 130, 0)), new Stop(1, Color.rgb(80, 60, 130, 0.36))));
        vignette.setMouseTransparent(true);
        vignette.setCache(true);
        vignette.setCacheHint(CacheHint.SPEED);
        Color[] petalColors = {Color.web("#ffffff"), Color.web("#ffd6ec"), Color.web("#c9f3f0"), Color.web("#e2d8ff")};
        for (int i = 0; i < 16; i++) {
            Rectangle pt = new Rectangle(3, 3, petalColors[i % petalColors.length]);
            pt.setOpacity(0.85);
            pt.setLayoutX(rng.nextDouble() * Ui.W);
            pt.setLayoutY(rng.nextDouble() * Ui.H);
            pt.setUserData(rng.nextDouble() * 6.28);
            petals.getChildren().add(pt);
        }
        petals.setVisible(!cfg.rain);
        petals.setMouseTransparent(true);
        root.getChildren().addAll(wash, smog, tint, vignette, petals, rain, fxLayer, tag, hud, toastBox, dialog, overlay);
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void enter() {
        mapView = new MapView(session.map(), cfg.scene);
        FXGL.entityBuilder().view(mapView.root()).zIndex(-10).buildAndAttach();
        for (Bin b : session.bins()) binViews.add(new BinView(b));
        for (PlantingSpot s : session.spots()) spotViews.add(new SpotView(s));
        buildPlayer();
        dialog.show(briefPages());
        app.input().clear();
    }

    @Override
    public void exit() {
        new ArrayList<>(FXGL.getGameWorld().getEntities()).forEach(Entity::removeFromWorld);
        FXGL.getGameScene().getViewport().setX(0);
        FXGL.getGameScene().getViewport().setY(0);
        app.input().clear();
    }

    private List<String> briefPages() {
        List<String> pages = new ArrayList<>();
        pages.add(cfg.story[0]);
        pages.add(cfg.story[1]);
        if (cfg.isCampaign()) {
            pages.add("MISSION: " + missionSentence() + ". Then reach " + cfg.target
                    + " points, and keep pollution under 100%!");
        } else {
            pages.add(cfg.difficulty + ".");
        }
        pages.add("TIP: " + cfg.tip + " (A: E/Z/Enter, Space: empty bin, B: boat, Arrows/WASD: walk)");
        return pages;
    }

    private String missionSentence() {
        List<String> p = new ArrayList<>();
        if (cfg.missionSort > 0) p.add("sort " + cfg.missionSort + " items");
        if (cfg.missionPlant > 0) p.add("plant " + cfg.missionPlant + " trees");
        if (cfg.missionClean > 0) p.add("fish " + cfg.missionClean + " items out of the river (B: boat)");
        return String.join(", ", p);
    }

    // ------------------------------------------------------------------ the Guardian

    private static int rowOf(Direction d) {
        return switch (d) {
            case DOWN -> 0;
            case LEFT -> 1;
            case RIGHT -> 2;
            case UP -> 3;
        };
    }

    private void buildPlayer() {
        Sibling sibling = app.save().sibling();
        Image[] sheets = {Sprites.get(sibling.sheet(false)), Sprites.get(sibling.sheet(true))};
        for (int b = 0; b < 2; b++) {
            for (int r = 0; r < 4; r++) {
                channels[b][r][0] = new AnimationChannel(sheets[b], 4, PX, PLAYER_H, Duration.seconds(1), r * 4, r * 4);
                channels[b][r][1] = new AnimationChannel(sheets[b], 4, PX, PLAYER_H, Duration.seconds(0.36), r * 4, r * 4 + 3);
            }
        }
        ptex = new AnimatedTexture(channels[0][0][0]);
        ptex.loop();
        Player p = session.player();
        px = p.col() * PX;
        py = p.row() * PX - (PLAYER_H - PX);
        playerEntity = FXGL.entityBuilder().at(px, py).view(ptex).zIndex((int) (py + PLAYER_H)).buildAndAttach();
        playerEntity.getViewComponent().addChild(carryGroup);
    }

    /** Smooth tile-to-tile walking; the model moves first, the view glides to catch up. */
    private void updateMovement(double tpf, boolean allowInput) {
        Player p = session.player();
        if (moving) {
            moveT += tpf;
            double f = Math.min(1, moveT / moveDur);
            px = fromX + (toX - fromX) * f;
            py = fromY + (toY - fromY) * f;
            if (f >= 1) {
                moving = false;
                px = toX;
                py = toY;
            }
        }
        if (!moving && allowInput) {
            Direction d = app.input().current();
            if (d == null) {
                turnPause = 0;
            } else if (d != p.facing()) {
                session.turn(d);
                turnPause = TURN_PAUSE;
            } else if (turnPause > 0) {
                turnPause -= tpf;
            } else {
                session.tryMove(d);
            }
        }
        if (!moving) {
            double tx = p.col() * PX;
            double ty = p.row() * PX - (PLAYER_H - PX);
            if (tx != px || ty != py) {
                fromX = px;
                fromY = py;
                toX = tx;
                toY = ty;
                moveT = 0;
                moveDur = session.moveSeconds();
                moving = true;
            }
        }
    }

    private void syncPlayer() {
        Player p = session.player();
        ptex.loopNoOverride(channels[p.onBoat() ? 1 : 0][rowOf(p.facing())][moving ? 1 : 0]);
        double bob = p.onBoat() && !moving ? Math.round(Math.sin(time * 3) * 1.5) : 0;
        playerEntity.setPosition(px, py + bob);
        playerEntity.setZIndex((int) (py + PLAYER_H));

        StringBuilder sig = new StringBuilder();
        for (Pickup c : p.carried()) sig.append(c.id()).append(',');
        if (!sig.toString().equals(carrySignature)) {
            carrySignature = sig.toString();
            carryGroup.getChildren().clear();
            int i = 0;
            for (Pickup c : p.carried()) {
                ImageView iv = Sprites.view(c.sprite() + ".png");
                iv.setFitWidth(34);
                iv.setFitHeight(34);
                iv.setLayoutX(7);
                iv.setLayoutY(-24 - i * 26);
                carryGroup.getChildren().add(iv);
                i++;
            }
        }
    }

    // ------------------------------------------------------------------ frame update

    @Override
    public void update(double rawTpf) {
        double tpf = Math.min(rawTpf, 0.1);
        time += tpf;
        mapView.update(tpf);
        hud.update(session);
        toastBox.update(tpf);
        dialog.update(tpf);
        if (menu != null) menu.update(tpf);

        updateMovement(tpf, state == State.PLAY && !session.ended());
        if (state == State.PLAY) {
            session.tick(tpf);
            if (session.ended() && endTimer < 0) endTimer = 1.1;
            if (endTimer >= 0) {
                endTimer -= tpf;
                if (endTimer < 0) showResult();
            }
        }
        syncPlayer();
        syncItems(tpf);
        for (BinView b : binViews) b.update(tpf);
        for (SpotView s : spotViews) s.update();
        syncTruck();
        updateSparks(tpf);
        updateOverlays(tpf);
        if (state == State.PAUSED && menu != null) {
            String sl = soundLabel();
            if (!sl.equals(shownSound)) {
                shownSound = sl;
                menu.setLabel(2, sl);
            }
        }
    }

    private void syncItems(double tpf) {
        Set<Integer> alive = new HashSet<>();
        for (Pickup p : session.items()) {
            alive.add(p.id());
            itemViews.computeIfAbsent(p.id(), id -> new ItemView(p)).update(p, tpf);
        }
        itemViews.entrySet().removeIf(e -> {
            if (alive.contains(e.getKey())) return false;
            e.getValue().entity.removeFromWorld();
            return true;
        });
    }

    private void syncTruck() {
        if (session.truckActive()) {
            if (truckEntity == null) {
                truckEntity = FXGL.entityBuilder().view(Sprites.view("world/truck.png")).buildAndAttach();
            }
            double y = session.truckRow() * PX;
            truckEntity.setPosition(session.truckCol() * PX, y);
            truckEntity.setZIndex((int) (y + 2 * PX + 2));   // 2 tiles tall: draw above the player on both road rows
        } else if (truckEntity != null) {
            truckEntity.removeFromWorld();
            truckEntity = null;
        }
    }

    private void updateOverlays(double tpf) {
        double pol = Math.min(100, session.pollution()) / 100.0;
        smog.setOpacity(pol * 0.42);
        wash.setOpacity(pol * 0.25);   // pollution drains the colour (cheap overlay; the old whole-map effect caused the lag)

        if (petals.isVisible()) {
            for (Node n : petals.getChildren()) {
                n.setLayoutY(n.getLayoutY() + 16 * tpf);
                n.setLayoutX(n.getLayoutX() + Math.sin(time * 1.3 + (Double) n.getUserData()) * 16 * tpf);
                if (n.getLayoutY() > Ui.H) {
                    n.setLayoutY(-6);
                    n.setLayoutX(rng.nextDouble() * Ui.W);
                }
            }
        }
        if (rain.isVisible()) {
            for (Node n : rain.getChildren()) {
                n.setLayoutY(n.getLayoutY() + 420 * tpf);
                n.setLayoutX(n.getLayoutX() - 110 * tpf);
                if (n.getLayoutY() > Ui.H) {
                    n.setLayoutY(-12);
                    n.setLayoutX(rng.nextDouble() * (Ui.W + 120));
                }
            }
        }

        if (shake > 0) {
            shake -= tpf;
            FXGL.getGameScene().getViewport().setX((rng.nextDouble() - 0.5) * 10);
            FXGL.getGameScene().getViewport().setY((rng.nextDouble() - 0.5) * 10);
            if (shake <= 0) {
                FXGL.getGameScene().getViewport().setX(0);
                FXGL.getGameScene().getViewport().setY(0);
            }
        }
        updateTag();
    }

    /** Name tag above whatever the Guardian is facing (bins, litter, planting spots). */
    private void updateTag() {
        String text = null;
        int col = 0;
        int row = 0;
        if (state == State.PLAY && !session.ended() && !moving) {
            Pickup tp = session.targetPickup();
            Bin tb = session.targetBin();
            PlantingSpot ts = session.targetSpot();
            if (tp != null) {
                text = tp.displayName();
                col = tp.col();
                row = tp.row();
            } else if (tb != null) {
                text = tb.type().label.toUpperCase() + " BIN " + tb.fill() + "/" + session.binCapacity();
                col = tb.col();
                row = tb.row();
            } else if (ts != null) {
                text = ts.planted() ? "TREE" : "PLANTING SPOT";
                col = ts.col();
                row = ts.row();
            }
        }
        if (text == null) {
            tag.setVisible(false);
            tagText = "";
            return;
        }
        if (!text.equals(tagText)) {
            tagText = text;
            Ui.Label l = new Ui.Label(text, 11, Ui.INK);
            double w = l.textWidth() + 34;
            l.setLayoutX(17);
            l.setLayoutY(9);
            tag.getChildren().setAll(Ui.panel(w, 34), l);
            tag.setUserData(w);
        }
        double w = (Double) tag.getUserData();
        double x = Math.max(4, Math.min(Ui.W - w - 4, col * PX + PX / 2.0 - w / 2));
        double y = row * PX - 38;
        if (y < 96) y = row * PX + PX + 4;
        tag.setLayoutX(x);
        tag.setLayoutY(y);
        tag.setVisible(true);
    }

    // ------------------------------------------------------------------ input

    @Override
    public void onDirection(Direction d) {
        if ((state == State.OFFER || state == State.RESULT || state == State.PAUSED) && menu != null) {
            if (d == Direction.UP) menu.move(-1);
            else if (d == Direction.DOWN) menu.move(1);
            else return;
            app.sfx(Sfx.CURSOR);
        }
    }

    @Override
    public void onConfirm() {
        switch (state) {
            case BRIEF -> {
                app.sfx(Sfx.SELECT);
                if (dialog.advance()) state = State.PLAY;
            }
            case PLAY -> {
                if (!moving) session.interact();
            }
            case OFFER, RESULT, PAUSED -> {
                if (menu != null) {
                    app.sfx(Sfx.SELECT);
                    menu.confirm();
                }
            }
        }
    }

    @Override
    public void onAction2() {
        if (state == State.PLAY) session.emptyBin();
    }

    @Override
    public void onBoat() {
        if (state == State.PLAY && !moving) session.toggleBoat();
    }

    @Override
    public void onCancel() {
        if (state == State.PLAY) openPause();
        else if (state == State.PAUSED) closePause();
        else if (state == State.BRIEF) app.showMainMenu();
    }

    @Override
    public void onPause() { togglePause(); }

    // ------------------------------------------------------------------ pause

    private void togglePause() {
        if (state == State.PLAY && !session.ended()) openPause();
        else if (state == State.PAUSED) closePause();
    }

    private String soundLabel() { return app.save().muted ? "SOUND: OFF" : "SOUND: ON"; }

    private void openPause() {
        state = State.PAUSED;
        app.input().clear();
        shownSound = soundLabel();
        List<MenuBox.Entry> entries = new ArrayList<>();
        entries.add(new MenuBox.Entry("RESUME", this::closePause));
        entries.add(new MenuBox.Entry("RESTART LEVEL", () -> app.startLevel(cfg, levelIndex)));
        entries.add(new MenuBox.Entry(shownSound, app::toggleMute));   // index 2, label refreshed in update()
        if (cfg.mode == LevelConfig.Mode.ZEN) {
            entries.add(new MenuBox.Entry("FINISH RUN", () -> {
                closePause();
                session.finishZen();
            }));
        }
        entries.add(new MenuBox.Entry("QUIT TO MENU", app::showMainMenu));
        menu = new MenuBox("PAUSED", entries, 300);
        menu.setLayoutX((Ui.W - 300) / 2.0);
        menu.setLayoutY((Ui.H - menu.height()) / 2.0);
        Rectangle dim = new Rectangle(Ui.W, Ui.H, Color.rgb(30, 20, 60, 0.55));
        overlay.getChildren().setAll(dim, menu);
    }

    private void closePause() {
        overlay.getChildren().clear();
        menu = null;
        state = State.PLAY;
        app.input().clear();
    }

    // ------------------------------------------------------------------ session events

    @Override
    public void onToast(String title, String detail, boolean good, double seconds) {
        if (state == State.PLAY || state == State.OFFER) toastBox.toast(title, detail, good, seconds);
    }

    @Override
    public void onSound(Sfx sfx) { app.sfx(sfx); }

    @Override
    public void onFloatText(int col, int row, String text, int rgb) {
        // two plain Text nodes (dark one offset behind) instead of an OUTSIDE-stroked Text, which is very slow in JavaFX
        Text back = new Text(text);
        Text front = new Text(text);
        for (Text t : new Text[]{back, front}) {
            t.setFont(Ui.font(12));
            t.setTextOrigin(VPos.TOP);
        }
        back.setFill(Color.web("#2c2748"));
        back.setTranslateX(2);
        back.setTranslateY(2);
        front.setFill(Color.web(String.format("#%06x", rgb)));
        addSpark(new Spark(new Group(back, front), col * PX + 4, row * PX - 6, 0, -30, 0, 1.1, true));
    }

    @Override
    public void onBurst(int col, int row, int rgb, int count) {
        Color c = Color.web(String.format("#%06x", rgb));
        for (int i = 0; i < Math.min(count, 8); i++) {
            Rectangle r = new Rectangle(6, 6, c);
            addSpark(new Spark(r, col * PX + PX / 2.0, row * PX + PX / 2.0, (rng.nextDouble() - 0.5) * 240,
                    -80 - rng.nextDouble() * 150, 420, 0.5 + rng.nextDouble() * 0.4, false));
        }
    }

    // ------------------------------------------------------------------ light-weight effects (no FXGL entities)

    private static final class Spark {
        final Node node;
        final double life;
        final double gravity;
        final boolean text;
        double x;
        double y;
        double vx;
        double vy;
        double age;

        Spark(Node node, double x, double y, double vx, double vy, double gravity, double life, boolean text) {
            this.node = node;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.gravity = gravity;
            this.life = life;
            this.text = text;
            node.setLayoutX(x);
            node.setLayoutY(y);
            node.setMouseTransparent(true);
        }
    }

    private void addSpark(Spark sp) {
        if (sparks.size() >= 60) fxLayer.getChildren().remove(sparks.remove(0).node);
        sparks.add(sp);
        fxLayer.getChildren().add(sp.node);
    }

    private void updateSparks(double tpf) {
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark sp = sparks.get(i);
            sp.age += tpf;
            if (sp.age >= sp.life) {
                fxLayer.getChildren().remove(sp.node);
                sparks.remove(i);
                continue;
            }
            sp.vy += sp.gravity * tpf;
            sp.x += sp.vx * tpf;
            sp.y += sp.vy * tpf;
            sp.node.setLayoutX(sp.x);
            sp.node.setLayoutY(sp.y);
            sp.node.setOpacity(sp.text ? Math.max(0, 1 - Math.max(0, sp.age - sp.life * 0.6) / (sp.life * 0.4))
                    : Math.max(0, 1 - sp.age / sp.life));
        }
    }

    @Override
    public void onShake(double seconds) { shake = Math.max(shake, seconds); }

    @Override
    public void onBinLid(Bin bin) {
        for (BinView v : binViews) if (v.bin == bin) v.lid = 0.25;
    }

    @Override
    public void onFactoryOffer() {
        state = State.OFFER;
        dialog.show(List.of("A shady factory boss offers you PHP 100 to look the other way. "
                + "It would pollute the city. What do you do?"));
        menu = new MenuBox(null, List.of(
                new MenuBox.Entry("ACCEPT THE MONEY", () -> answerOffer(true)),
                new MenuBox.Entry("REJECT THE BRIBE", () -> answerOffer(false))), 340);
        menu.setLayoutX(Ui.W - 346);
        menu.setLayoutY(Ui.H - 120 - menu.height());
        overlay.getChildren().setAll(menu);
    }

    private void answerOffer(boolean accept) {
        dialog.hide();
        overlay.getChildren().clear();
        menu = null;
        state = State.PLAY;
        session.answerFactoryOffer(accept);
    }

    // ------------------------------------------------------------------ result overlay

    private void showResult() {
        state = State.RESULT;
        toastBox.hide();
        dialog.hide();
        boolean win = session.won();
        String headline;
        if (cfg.isCampaign()) headline = win ? "LEVEL CLEAR!" : "THE CITY CHOKED...";
        else headline = win ? "RUN COMPLETE!" : "NOT ENOUGH SORTED";

        Rectangle dim = new Rectangle(Ui.W, Ui.H, Color.rgb(0, 0, 0, 0.5));
        Group panel = Ui.panel(620, 262);
        panel.setLayoutX(50);
        panel.setLayoutY(22);
        Ui.Label h = new Ui.Label(headline, 18, win ? Ui.GOOD : Ui.BAD);
        h.setLayoutX(78);
        h.setLayoutY(46);
        overlay.getChildren().setAll(dim, panel, h);

        double y = 82;
        if (win) {
            Ui.Label st = new Ui.Label("STARS  " + "*".repeat(session.stars()) + "-".repeat(3 - session.stars()), 14, Ui.ACCENT);
            st.setLayoutX(78);
            st.setLayoutY(y);
            overlay.getChildren().add(st);
            y += 32;
        }
        String extra = switch (cfg.mode) {
            case SPRINT -> "TIME " + (int) session.elapsed() + "s";
            case ENDLESS -> "SURVIVED " + (int) session.elapsed() + "s";
            default -> "POLLUTION " + (int) Math.min(100, session.pollution()) + "%";
        };
        Ui.Label stats = new Ui.Label("SCORE " + session.score() + "   SORTED " + session.sorted()
                + "\nBEST COMBO " + session.bestCombo() + "   PHP EARNED " + session.money()
                + "\n" + extra, 12, Ui.INK);
        stats.setLayoutX(78);
        stats.setLayoutY(y);
        overlay.getChildren().add(stats);
        y += 82;

        String lesson = !session.mistakes().isEmpty()
                ? "REMEMBER: " + session.mistakes().get(session.mistakes().size() - 1) + "."
                : win ? "NO SORTING MISTAKES. WELL DONE, GUARDIAN!" : "SORT FASTER AND PLANT TREES TO CUT POLLUTION.";
        Ui.Label tip = new Ui.Label(lesson, 10, Ui.FRAME_IN);
        tip.setWrap(540);
        tip.setLayoutX(78);
        tip.setLayoutY(y);
        overlay.getChildren().add(tip);

        List<MenuBox.Entry> entries = new ArrayList<>();
        if (cfg.isCampaign() && win && levelIndex < Levels.CAMPAIGN.size() - 1) {
            entries.add(new MenuBox.Entry("NEXT LEVEL", () -> app.startLevel(Levels.CAMPAIGN.get(levelIndex + 1), levelIndex + 1)));
        }
        entries.add(new MenuBox.Entry("RETRY", () -> app.startLevel(cfg, levelIndex)));
        entries.add(new MenuBox.Entry("UPGRADE SHOP", app::showShop));
        entries.add(new MenuBox.Entry(cfg.isCampaign() ? "LEVEL SELECT" : "GAME MODES",
                cfg.isCampaign() ? app::showLevelSelect : app::showModes));
        entries.add(new MenuBox.Entry("MAIN MENU", app::showMainMenu));
        menu = new MenuBox(null, entries, 300);
        menu.setLayoutX((Ui.W - 300) / 2.0);
        menu.setLayoutY(Math.min(Ui.H - menu.height() - 6, 292));
        overlay.getChildren().add(menu);
    }

    // ------------------------------------------------------------------ entity views

    /** A piece of litter / seed / boost lying on a tile. */
    private final class ItemView {
        final Entity entity;
        double x;
        double y;

        ItemView(Pickup p) {
            ImageView img = Sprites.view(p.sprite() + ".png");
            Ellipse shadow = new Ellipse(24, 41, 14, 5);
            shadow.setFill(Color.rgb(0, 0, 0, 0.25));
            x = p.col() * PX;
            y = p.row() * PX;
            entity = FXGL.entityBuilder().at(x, y).view(new Group(shadow, img)).zIndex((int) (y + PX) - 2)
                    .with(new Fx.Bob(img, p.id() * 0.7)).buildAndAttach();
        }

        void update(Pickup p, double tpf) {
            double tx = p.col() * PX;
            double ty = p.row() * PX;
            if (Math.abs(tx - x) > 3 * PX) x = tx;
            else x += (tx - x) * Math.min(1, tpf * 12);
            y += (ty - y) * Math.min(1, tpf * 12);
            entity.setPosition(x, y);
            entity.setZIndex((int) (y + PX) - 2);   // -2: on a tie the Guardian / boat is drawn on top of litter
        }
    }

    /** A recycling bin: colour sprite, open lid flash, fill bar and the Eco Scanner hint. */
    private final class BinView {
        final Bin bin;
        final Entity entity;
        private final ImageView img = new ImageView();
        private final Rectangle barBg = new Rectangle(6, -6, 36, 8);
        private final Rectangle barFill = new Rectangle(8, -4, 0, 4);
        private final Ellipse hint = new Ellipse(24, 42, 23, 8);
        private final Rectangle pill = new Rectangle();
        private final Ui.Label labelText = new Ui.Label("", 9, Ui.INK);
        private BinType labelShown;
        private BinType shown;
        private boolean shownOpen;
        double lid;
        private double t;

        BinView(Bin b) {
            this.bin = b;
            img.setSmooth(false);
            barBg.setFill(Color.web("#404858"));
            barBg.setArcWidth(4);
            barBg.setArcHeight(4);
            hint.setFill(Color.web("#fff48a"));
            hint.setVisible(false);
            entity = FXGL.entityBuilder().at(b.col() * PX, b.row() * PX)
                    .view(new Group(hint, img, barBg, barFill, pill, labelText)).zIndex(b.row() * PX + PX).buildAndAttach();
            refresh(false);
        }

        private void refresh(boolean open) {
            shown = bin.type();
            shownOpen = open;
            img.setImage(Sprites.get("bins/" + bin.type().sprite() + (open ? "_open" : "") + ".png"));
            if (labelShown != bin.type()) {
                labelShown = bin.type();
                refreshLabel();
            }
        }

        /** Permanent name tag under the bin; follows the bin when the types get swapped. */
        private void refreshLabel() {
            labelText.setText(bin.type().label.toUpperCase());
            double w = labelText.textWidth() + 14;
            pill.setWidth(w);
            pill.setHeight(17);
            pill.setArcWidth(8);
            pill.setArcHeight(8);
            pill.setFill(Color.web(String.format("#%06x", bin.type().rgb)));
            pill.setStroke(Ui.FRAME_OUT);
            pill.setStrokeWidth(2);
            pill.setX(PX / 2.0 - w / 2);
            pill.setY(PX + 3);
            labelText.setLayoutX(PX / 2.0 - w / 2 + 7);
            labelText.setLayoutY(PX + 7);
        }

        void update(double tpf) {
            t += tpf;
            if (lid > 0) lid -= tpf;
            boolean open = lid > 0;
            if (shown != bin.type() || open != shownOpen) refresh(open);

            double frac = Math.min(1.0, bin.fill() / (double) session.binCapacity());
            boolean any = bin.fill() > 0;
            barBg.setVisible(any);
            barFill.setVisible(any);
            barFill.setWidth(32 * frac);
            barFill.setFill(frac < 0.6 ? Color.web("#40c060") : frac < 1 ? Color.web("#f0c030") : Color.web("#e04838"));

            Pickup top = session.player().carried().isEmpty() ? null
                    : session.player().carried().get(session.player().carried().size() - 1);
            boolean on = session.hasScanner() && top instanceof WasteItem w && w.bin() == bin.type();
            hint.setVisible(on);
            if (on) hint.setOpacity(0.35 + 0.3 * Math.sin(t * 6));
        }
    }

    /** Soil patch; once planted, a tree sprite that grows from a sapling. */
    private final class SpotView {
        final PlantingSpot spot;
        private Entity tree;
        private ImageView treeImg;
        private boolean grown;

        SpotView(PlantingSpot s) {
            this.spot = s;
            FXGL.entityBuilder().at(s.col() * PX, s.row() * PX).view(Sprites.view("world/spot.png"))
                    .zIndex(s.row() * PX + PX - 10).buildAndAttach();
        }

        void update() {
            if (!spot.planted()) return;
            if (tree == null) {
                treeImg = Sprites.view("world/sapling.png");
                tree = FXGL.entityBuilder().at(spot.col() * PX, spot.row() * PX).view(treeImg)
                        .zIndex(spot.row() * PX + PX + 5).buildAndAttach();
            }
            double g = spot.growth();
            if (!grown && g >= 0.6) {
                grown = true;
                treeImg.setImage(Sprites.get("world/tree.png"));
            }
            double sc = 0.55 + 0.45 * g;
            treeImg.setScaleX(sc);
            treeImg.setScaleY(sc);
            treeImg.setTranslateY((1 - sc) * PX / 2.0);
        }
    }
}
