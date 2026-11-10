package com.codevibers.ecoquest.model;

/** Events the session raises; the view turns them into toasts, particles and sounds. */
public interface SessionListener {
    default void onToast(String title, String detail, boolean good, double seconds) { }
    default void onSound(Sfx sfx) { }
    default void onFloatText(int col, int row, String text, int rgb) { }
    default void onBurst(int col, int row, int rgb, int count) { }
    default void onShake(double seconds) { }
    default void onBinLid(Bin bin) { }
    default void onFactoryOffer() { }
}
