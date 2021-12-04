package com.won983212.servermod.utility;

public class Color {
    public final static Color WHITE = new Color(255, 255, 255);

    protected final int value;

    public Color(int r, int g, int b) {
        this(r, g, b, 0xff);
    }

    public Color(int r, int g, int b, int a) {
        value = ((a & 0xff) << 24) |
                ((r & 0xff) << 16) |
                ((g & 0xff) << 8) |
                (b & 0xff);
    }

    public Color(int rgba) {
        value = rgba;
    }

    public Color(int rgb, boolean hasAlpha) {
        if (hasAlpha) {
            value = rgb;
        } else {
            value = rgb | 0xff_000000;
        }
    }

    public int getRed() {
        return (getRGB() >> 16) & 0xff;
    }

    public int getGreen() {
        return (getRGB() >> 8) & 0xff;
    }

    public int getBlue() {
        return getRGB() & 0xff;
    }

    public int getAlpha() {
        return (getRGB() >> 24) & 0xff;
    }

    public float getRedAsFloat() {
        return getRed() / 255f;
    }

    public float getGreenAsFloat() {
        return getGreen() / 255f;
    }

    public float getBlueAsFloat() {
        return getBlue() / 255f;
    }

    public float getAlphaAsFloat() {
        return getAlpha() / 255f;
    }

    public int getRGB() {
        return value;
    }
}
