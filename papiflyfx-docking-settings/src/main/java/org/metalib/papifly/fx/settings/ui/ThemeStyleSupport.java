package org.metalib.papifly.fx.settings.ui;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

final class ThemeStyleSupport {

    private ThemeStyleSupport() {
    }

    static String toCss(Paint paint) {
        if (paint instanceof Color color) {
            int red = (int) Math.round(color.getRed() * 255.0);
            int green = (int) Math.round(color.getGreen() * 255.0);
            int blue = (int) Math.round(color.getBlue() * 255.0);
            double opacity = color.getOpacity();
            if (opacity < 1.0) {
                return String.format("rgba(%d,%d,%d,%.3f)", red, green, blue, opacity);
            }
            return String.format("#%02x%02x%02x", red, green, blue);
        }
        return paint == null ? "#000000" : paint.toString();
    }
}
