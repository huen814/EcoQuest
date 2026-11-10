package com.codevibers.ecoquest.view;

import com.almasb.fxgl.entity.component.Component;

import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/** Tiny entity components for juice: bobbing items, rising score text, burst particles. */
public final class Fx {
    private Fx() { }

    public static final class Bob extends Component {
        private final ImageView view;
        private double t;

        public Bob(ImageView view, double phase) {
            this.view = view;
            this.t = phase;
        }

        @Override
        public void onUpdate(double tpf) {
            t += tpf;
            view.setTranslateY(Math.round(Math.sin(t * 3) * 2));
        }
    }

    public static final class Rise extends Component {
        private final Node node;
        private final double life;
        private double age;

        public Rise(Text node, double life) {
            this.node = node;
            this.life = life;
        }

        @Override
        public void onUpdate(double tpf) {
            age += tpf;
            entity.translateY(-30 * tpf);
            node.setOpacity(Math.max(0, 1 - Math.max(0, age - life * 0.6) / (life * 0.4)));
            if (age >= life) entity.removeFromWorld();
        }
    }

    public static final class Particle extends Component {
        private final Rectangle rect;
        private final double life;
        private double vx;
        private double vy;
        private double age;

        public Particle(Rectangle rect, double vx, double vy, double life) {
            this.rect = rect;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
        }

        @Override
        public void onUpdate(double tpf) {
            age += tpf;
            vy += 420 * tpf;
            entity.translate(vx * tpf, vy * tpf);
            rect.setOpacity(Math.max(0, 1 - age / life));
            if (age >= life) entity.removeFromWorld();
        }
    }
}
