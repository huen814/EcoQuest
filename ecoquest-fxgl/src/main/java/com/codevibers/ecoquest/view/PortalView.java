package com.codevibers.ecoquest.view;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public final class PortalView extends Group {
    private static final int FRAMES = 8, W = 120, H = 168;
    private static final double OPEN_TIME = 0.9;

    private final ImageView img = Sprites.view("portal.png");
    private final Circle glow = StoryFx.glow(170, Color.web("#9fe0cf", 0.85));

    public PortalView() {
        img.setViewport(new Rectangle2D(0, 0, W, H));
        img.setFitWidth(W);
        img.setFitHeight(H);
        img.setLayoutX(-W / 2.0);
        img.setLayoutY(-H / 2.0);
        getChildren().addAll(glow, img);
    }

    public void update(double t) {
        img.setViewport(new Rectangle2D(((int) (t * 12) % FRAMES) * W, 0, W, H));
        double k = Math.min(1, t / OPEN_TIME);
        double open = 1 + 2.2 * Math.pow(k - 1, 3) + 1.2 * Math.pow(k - 1, 2);
        double pulse = 1 + 0.04 * Math.sin(t * 4);
        setScaleX(Math.max(0.05, open) * pulse);
        setScaleY(Math.max(0.05, open) * pulse);
        glow.setOpacity(0.7 + 0.3 * Math.sin(t * 3));
    }
}
