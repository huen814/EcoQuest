package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.codevibers.ecoquest.model.TileMap;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * Shared dressing for the story cutscenes: full-screen scenery (the backdrop strips are only 144 px tall, so the rest
 * is filled with the same ground tiles the play map uses), glows, vignettes, scattered litter and a tiny particle system.
 */
public final class StoryFx {
    private static final int PX = Sprites.PX;
    private static final String[] LITTER = {
        "aluminum_can", "banana_peel", "bottle_cap", "cardboard_box", "food_container", "food_scraps",
        "glass_bottle", "glass_jar", "newspaper", "notebook_paper", "plastic_bag", "plastic_bottle", "tin_can"
    };

    private StoryFx() { }

    // ------------------------------------------------------------------ scenery

    /** A whole 720x480 scene: skyline strip on top, ground tiles below with a road along row 6. */
    public static Group scene(String name) {
        Group g = new Group();
        g.getChildren().add(Sprites.view("backdrop/" + name + ".png"));
        for (int c = 0; c < TileMap.COLS; c++) {
            for (int r = 3; r < TileMap.ROWS; r++) {
                String tex = r == 6 ? "tiles/road_" + name + ".png"
                        : "tiles/ground_" + name + "_" + (((c * 7 + r * 13) % 5 == 0) ? "b" : "a") + ".png";
                ImageView v = Sprites.view(tex);
                v.setLayoutX(c * PX);
                v.setLayoutY(r * PX);
                g.getChildren().add(v);
            }
        }
        return g;
    }

    /** The violet storm sky of the Queen's Knight: a purple wash over the sky and pools of violet light on the street, {@code width} px wide. */
    public static Group violetSky(double width) {
        Group g = new Group();
        Rectangle sky = new Rectangle(width, 320);
        sky.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#9b4dff", 0.6)), new Stop(1, Color.web("#9b4dff", 0))));
        Rectangle storm = new Rectangle(width, 330);
        storm.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#3a0f66", 0.7)), new Stop(1, Color.web("#3a0f66", 0))));
        g.getChildren().addAll(sky, storm);
        for (double x = 180; x < width; x += 360) {
            Circle skyGlow = glow(230, Color.web("#b070ff", 0.4));
            skyGlow.setTranslateX(x);
            skyGlow.setTranslateY(90);
            Circle pool = glow(170, Color.web("#a35cff", 0.5));
            pool.setTranslateX(x);
            pool.setTranslateY(334);
            pool.setScaleY(0.28);
            g.getChildren().addAll(skyGlow, pool);
        }
        g.setMouseTransparent(true);
        return g;
    }

    /** Soft round light: bright in the middle, fading to nothing at the edge. Centered on (0, 0). */
    public static Circle glow(double radius, Color color) {
        Circle c = new Circle(radius);
        c.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, color), new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0))));
        c.setMouseTransparent(true);
        return c;
    }

    /** Full-screen darkening toward the edges (or a flat tint when strength is high). */
    public static Rectangle vignette(Color edge) {
        Rectangle r = new Rectangle(Ui.W, Ui.H);
        r.setFill(new RadialGradient(0, 0, 0.5, 0.45, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0.45, Color.color(edge.getRed(), edge.getGreen(), edge.getBlue(), 0)), new Stop(1, edge)));
        r.setMouseTransparent(true);
        return r;
    }

    public static Ellipse shadow(double centerX, double feetY, double w) {
        Ellipse e = new Ellipse(centerX, feetY - 2, w / 2, w / 7);
        e.setFill(Color.rgb(20, 10, 40, 0.28));
        return e;
    }

    /** Puts a 48x72 sprite frame (facing down) on the ground at (centerX, feetY), scaled up. */
    public static void stand(ImageView iv, double centerX, double feetY, double scale) {
        iv.setSmooth(false);
        iv.setViewport(new Rectangle2D(0, 0, PX, 72));
        iv.setFitWidth(PX * scale);
        iv.setFitHeight(72 * scale);
        iv.setLayoutX(centerX - PX * scale / 2);
        iv.setLayoutY(feetY - 72 * scale);
    }

    /** Scatters item sprites over a ground area; lower ones are drawn bigger and on top. */
    public static Group litter(long seed, double x0, double x1, double y0, double y1, int count, double minSize, double maxSize) {
        Random rng = new Random(seed);
        List<double[]> spots = new ArrayList<>();
        for (int i = 0; i < count; i++) spots.add(new double[] {x0 + rng.nextDouble() * (x1 - x0), y0 + rng.nextDouble() * (y1 - y0), rng.nextInt(LITTER.length), rng.nextDouble()});
        spots.sort((a, b) -> Double.compare(a[1], b[1]));
        Group g = new Group();
        for (double[] s : spots) {
            double depth = (s[1] - y0) / Math.max(1, y1 - y0);
            double size = minSize + (maxSize - minSize) * (0.4 + 0.6 * depth) * (0.7 + 0.3 * s[3]);
            ImageView v = item(LITTER[(int) s[2]], size);
            v.setLayoutX(s[0] - size / 2);
            v.setLayoutY(s[1] - size);
            v.setRotate((s[3] - 0.5) * 60);
            g.getChildren().add(v);
        }
        return g;
    }

    // ------------------------------------------------------------------ particle shapes (centered on 0,0)

    public static ImageView item(String name, double size) {
        ImageView v = Sprites.view("items/" + name + ".png");
        v.setFitWidth(size);
        v.setFitHeight(size);
        v.setPreserveRatio(true);
        return v;
    }

    /** Item that is centered on its translate position (for particles). */
    public static Node centeredItem(String name, double size) {
        ImageView v = item(name, size);
        v.setLayoutX(-size / 2);
        v.setLayoutY(-size / 2);
        return new Group(v);
    }

    public static Node randomLitter(Random rng, double size) {
        return centeredItem(LITTER[rng.nextInt(LITTER.length)], size);
    }

    public static Node dot(double radius, Color c) { return new Circle(radius, c); }

    public static Node sparkle(double s, Color c) {
        double k = s * 0.28;
        Polygon p = new Polygon(0, -s, k, -k, s, 0, k, k, 0, s, -k, k, -s, 0, -k, -k);
        p.setFill(c);
        return p;
    }

    public static Node heart(double s, Color c) {
        Polygon p = new Polygon(0, s * 0.95, -s, -s * 0.1, -s, -s * 0.55, -s * 0.5, -s * 0.95, 0, -s * 0.5,
                s * 0.5, -s * 0.95, s, -s * 0.55, s, -s * 0.1);
        p.setFill(c);
        p.setStroke(c.darker());
        p.setStrokeWidth(0.8);
        return p;
    }

    public static Node confetti(double w, double h, Color c) {
        Rectangle r = new Rectangle(-w / 2, -h / 2, w, h);
        r.setFill(c);
        return r;
    }

    // ------------------------------------------------------------------ particles

    /** Lightweight particle layer: ambient drifters, one-shot bursts and things sucked toward a point. */
    public static final class Particles extends Group {
        private enum Mode { DRIFT, BURST, CONVERGE }

        private static final class P {
            Node node;
            Mode mode;
            double x, y, vx, vy, gravity, t, max, phase, sway, sx, sy, tx, ty, spin, baseOpacity = 1;
        }

        private final List<P> live = new ArrayList<>();
        private final Random rng = new Random();
        private final double w, h;

        public Particles(double w, double h) {
            this.w = w;
            this.h = h;
            setMouseTransparent(true);
        }

        private void add(P p) {
            live.add(p);
            p.baseOpacity = p.node.getOpacity();
            p.node.setTranslateX(p.x);
            p.node.setTranslateY(p.y);
            getChildren().add(p.node);
        }

        /** Endless drifters spread over the area (vy &lt; 0 rises, vy &gt; 0 falls), swaying sideways, wrapping around. */
        public void drift(int n, Supplier<Node> maker, double vx, double vy, double sway) {
            for (int i = 0; i < n; i++) {
                P p = new P();
                p.mode = Mode.DRIFT;
                p.node = maker.get();
                p.x = rng.nextDouble() * w;
                p.y = rng.nextDouble() * h;
                p.vx = vx * (0.6 + rng.nextDouble() * 0.8);
                p.vy = vy * (0.6 + rng.nextDouble() * 0.8);
                p.sway = sway;
                p.phase = rng.nextDouble() * Math.PI * 2;
                p.spin = (rng.nextDouble() - 0.5) * 40;
                add(p);
            }
        }

        /** Drifters confined to a box (used for stink and flies around one pile). */
        public void driftIn(int n, Supplier<Node> maker, double x0, double y0, double x1, double y1, double vy, double sway) {
            for (int i = 0; i < n; i++) {
                P p = new P();
                p.mode = Mode.DRIFT;
                p.node = maker.get();
                p.x = x0 + rng.nextDouble() * (x1 - x0);
                p.y = y0 + rng.nextDouble() * (y1 - y0);
                p.vy = vy * (0.6 + rng.nextDouble() * 0.8);
                p.sway = sway;
                p.phase = rng.nextDouble() * Math.PI * 2;
                p.sx = y0; p.sy = y1;          // vertical wrap range
                p.tx = x0; p.ty = x1;          // horizontal wrap range
                add(p);
            }
        }

        /** Flies outward from a point and fades out. */
        public void burst(double x, double y, int n, Supplier<Node> maker, double speed, double life, double gravity) {
            for (int i = 0; i < n; i++) {
                P p = new P();
                p.mode = Mode.BURST;
                p.node = maker.get();
                double a = rng.nextDouble() * Math.PI * 2;
                double sp = speed * (0.35 + rng.nextDouble() * 0.9);
                p.x = x; p.y = y;
                p.vx = Math.cos(a) * sp;
                p.vy = Math.sin(a) * sp;
                p.gravity = gravity;
                p.max = life * (0.6 + rng.nextDouble() * 0.6);
                p.spin = (rng.nextDouble() - 0.5) * 420;
                add(p);
            }
        }

        /** Things from the screen edges spiral in to (tx, ty) and vanish. */
        public void converge(int n, Supplier<Node> maker, double tx, double ty, double duration, double orbit) {
            for (int i = 0; i < n; i++) {
                P p = new P();
                p.mode = Mode.CONVERGE;
                p.node = maker.get();
                int side = rng.nextInt(4);
                p.sx = side == 0 ? -20 : side == 1 ? w + 20 : rng.nextDouble() * w;
                p.sy = side == 2 ? -20 : side == 3 ? h * 0.8 : rng.nextDouble() * h * 0.8;
                p.tx = tx; p.ty = ty;
                p.max = duration * (0.7 + rng.nextDouble() * 0.5);
                p.t = -rng.nextDouble() * duration * 0.5;   // start delay
                p.sway = (rng.nextBoolean() ? 1 : -1) * orbit * (0.5 + rng.nextDouble());
                p.spin = (rng.nextDouble() - 0.5) * 540;
                p.x = p.sx; p.y = p.sy;
                add(p);
                p.node.setVisible(false);
            }
        }

        public void clear() {
            live.clear();
            getChildren().clear();
        }

        public void update(double dt) {
            for (Iterator<P> it = live.iterator(); it.hasNext(); ) {
                P p = it.next();
                p.t += dt;
                if (p.mode == Mode.DRIFT) {
                    p.x += (p.vx + Math.sin(p.t * 2 + p.phase) * p.sway) * dt;
                    p.y += p.vy * dt;
                    boolean boxed = p.sy != 0 || p.tx != 0 || p.ty != 0;
                    double yLo = boxed ? p.sx : -30, yHi = boxed ? p.sy : h + 30;
                    double xLo = boxed ? p.tx : -30, xHi = boxed ? p.ty : w + 30;
                    if (p.y < yLo) p.y = yHi; else if (p.y > yHi) p.y = yLo;
                    if (p.x < xLo) p.x = xHi; else if (p.x > xHi) p.x = xLo;
                    p.node.setRotate(p.node.getRotate() + p.spin * dt);
                } else if (p.mode == Mode.BURST) {
                    p.vy += p.gravity * dt;
                    p.x += p.vx * dt;
                    p.y += p.vy * dt;
                    double k = Math.min(1, p.t / p.max);
                    p.node.setOpacity(p.baseOpacity * (1 - k));
                    p.node.setRotate(p.node.getRotate() + p.spin * dt);
                    if (p.t >= p.max) {
                        getChildren().remove(p.node);
                        it.remove();
                        continue;
                    }
                } else {
                    if (p.t < 0) continue;
                    p.node.setVisible(true);
                    double k = Math.min(1, p.t / p.max);
                    double e = k * k;
                    double dx = p.tx - p.sx, dy = p.ty - p.sy;
                    double len = Math.max(1, Math.hypot(dx, dy));
                    double off = Math.sin(k * Math.PI) * p.sway;
                    p.x = p.sx + dx * e + (-dy / len) * off;
                    p.y = p.sy + dy * e + (dx / len) * off;
                    p.node.setRotate(p.node.getRotate() + p.spin * dt);
                    p.node.setScaleX(1 - 0.7 * k);
                    p.node.setScaleY(1 - 0.7 * k);
                    p.node.setOpacity(k > 0.85 ? p.baseOpacity * (1 - k) / 0.15 : p.baseOpacity);
                    if (k >= 1) {
                        getChildren().remove(p.node);
                        it.remove();
                        continue;
                    }
                }
                p.node.setTranslateX(p.x);
                p.node.setTranslateY(p.y);
            }
        }
    }
}
