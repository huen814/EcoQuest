package com.codevibers.ecoquest.model;

/** Sound effects, synthesised at runtime (wave: 0 sine, 1 triangle, 2 square, 3 saw). */
public enum Sfx {
    CURSOR(660, .05, 2, .03), SELECT(880, .08, 1, .05), PICKUP(520, .08, 1, .05), CORRECT(620, .12, 1, .05),
    WRONG(130, .25, 3, .05), FULL(150, .20, 2, .05), EMPTY(300, .10, 1, .05), PLANT(660, .15, 0, .05),
    SWAP(220, .20, 2, .05), DUMP(110, .40, 3, .05), MISSION(880, .40, 0, .05), WIN(784, .50, 0, .05),
    BUY(700, .20, 0, .05), DENY(140, .20, 2, .05), BOOST(990, .15, 1, .05), BOAT(400, .10, 0, .04),
    HONK(250, .35, 2, .05);

    public final double hz;
    public final double seconds;
    public final int wave;
    public final double volume;

    Sfx(double hz, double seconds, int wave, double volume) {
        this.hz = hz;
        this.seconds = seconds;
        this.wave = wave;
        this.volume = volume;
    }
}
