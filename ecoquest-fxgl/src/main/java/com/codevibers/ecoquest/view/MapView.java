package com.codevibers.ecoquest.view;

import java.util.ArrayList;
import java.util.List;

import com.codevibers.ecoquest.model.TileMap;
import com.codevibers.ecoquest.model.TileMap.Tile;

import javafx.scene.Group;
import javafx.scene.image.ImageView;

/**
 * Static tile art for one scene (grass, road, river, bridges) plus a backdrop strip for rows 0-2.
 */
public final class MapView {
    private static final int PX = Sprites.PX;

    private final TileMap map;
    private final Group root = new Group();
    private final List<ImageView> water = new ArrayList<>();
    private final ImageView boat = Sprites.view("world/boat.png");
    private final String scene;
    private double t;
    private int frame;

    public MapView(TileMap map, String scene) {
        this.map = map;
        this.scene = scene;
        root.getChildren().add(Sprites.view("backdrop/" + scene + ".png"));
        for (int c = 0; c < TileMap.COLS; c++) {
            for (int r = 3; r < TileMap.ROWS; r++) {
                ImageView v;
                Tile tile = map.at(c, r);
                switch (tile) {
                    case ROAD -> v = Sprites.view("tiles/road_" + scene + ".png");
                    case WATER -> {
                        v = Sprites.view("tiles/water_" + scene + "_0.png");
                        water.add(v);
                    }
                    case BRIDGE -> v = Sprites.view("tiles/bridge_" + scene + ".png");
                    default -> v = Sprites.view("tiles/ground_" + scene + "_" + (((c * 7 + r * 13) % 5 == 0) ? "b" : "a") + ".png");
                }
                v.setLayoutX(c * PX);
                v.setLayoutY(r * PX);
                root.getChildren().add(v);
            }
        }
        root.getChildren().add(boat);
        update(0);
    }

    public Group root() { return root; }

    public void update(double tpf) {
        t += tpf;
        int f = (int) (t * 2) % 2;
        if (f != frame || t == 0) {
            frame = f;
            for (ImageView v : water) v.setImage(Sprites.get("tiles/water_" + scene + "_" + f + ".png"));
        }
        boat.setVisible(map.river() && !map.boatBoarded);
        boat.setLayoutX(map.boatCol * PX);
        boat.setLayoutY(map.boatRow * PX + Math.round(Math.sin(t * 2.5) * 2));
    }
}
