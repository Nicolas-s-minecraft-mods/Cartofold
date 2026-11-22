package io.github.nicolasgoyon.cartofold.view;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

/** The place, the zoom and the rotation of the minimap. The minimap and its tests use these rules. */
public final class MinimapLayout {
    /** The space between the minimap and the sides of the screen, in units of the screen. */
    public static final int MARGIN = 6;
    /** The farthest zoom level: one unit of the screen shows 16 x 16 blocks. */
    public static final int MIN_ZOOM = -2;
    /** The nearest zoom level: one block takes 4 x 4 units of the screen. */
    public static final int MAX_ZOOM = 4;
    /** The scales of the zoom levels from {@link #MIN_ZOOM}: the number of units of the screen for one block. */
    private static final double[] ZOOMS = {1.0 / 16, 1.0 / 8, 0.25, 0.5, 1, 2, 4};

    /** The corner of the screen of the minimap. */
    public enum Corner implements TranslatableEnum {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        @Override
        public Component getTranslatedName() {
            return Component.translatable("cartofold.configuration.minimap.corner." + name().toLowerCase(Locale.ROOT));
        }

        /** True for the two corners at the top of the screen. */
        public boolean top() {
            return this == TOP_LEFT || this == TOP_RIGHT;
        }

        /** True for the two corners at the left of the screen. */
        public boolean left() {
            return this == TOP_LEFT || this == BOTTOM_LEFT;
        }
    }

    private MinimapLayout() {
    }

    /**
     * The scale of a zoom level, from {@link #MIN_ZOOM} to {@link #MAX_ZOOM}. Each level doubles the scale of the level
     * before it. A level out of the range gives the nearest level.
     */
    public static double scale(int zoom) {
        return ZOOMS[Math.clamp(zoom, MIN_ZOOM, MAX_ZOOM) - MIN_ZOOM];
    }

    /**
     * The position of the minimap on the screen.
     *
     * @param corner       the corner of the screen
     * @param size         the size of the minimap, in units of the screen
     * @param screenWidth  the width of the screen
     * @param screenHeight the height of the screen
     * @param textHeight   the height of the text below or above the minimap (the coordinates), or 0
     * @return the X and the Y of the top left corner of the minimap
     */
    public static int[] position(Corner corner, int size, int screenWidth, int screenHeight, int textHeight) {
        int x = corner.left() ? MARGIN : screenWidth - MARGIN - size;
        int y = corner.top() ? MARGIN : screenHeight - MARGIN - size - textHeight;
        return new int[] {x, y};
    }

    /**
     * The rotation of the map for a projection (see {@link MapProjection}).
     *
     * @param rotate true if the direction of the player is at the top, false if the north is at the top
     * @param yaw    the yaw of the player, in degrees (0: south, 90: west)
     * @return the rotation, in radians
     */
    public static double mapRotation(boolean rotate, float yaw) {
        return rotate ? Math.PI - Math.toRadians(yaw) : 0;
    }

    /**
     * The rotation of the arrow of the player on the screen. With a rotation of 0, the arrow points to the top.
     *
     * @param rotate true if the map turns with the player
     * @param yaw    the yaw of the player, in degrees
     * @return the rotation, in radians, clockwise
     */
    public static double arrowRotation(boolean rotate, float yaw) {
        return rotate ? 0 : Math.toRadians(yaw) + Math.PI;
    }

    /**
     * Keeps a point in the minimap: a point out of the minimap goes to its side, in the same direction from the
     * center.
     *
     * @param dx    the X of the point from the center of the minimap
     * @param dy    the Y of the point from the center of the minimap
     * @param limit the largest distance from the center (the half of the size, less the size of the marker)
     * @param round true for a round minimap
     * @return the X and the Y of the point from the center, in the minimap
     */
    public static double[] keepInside(double dx, double dy, double limit, boolean round) {
        double distance = round ? Math.hypot(dx, dy) : Math.max(Math.abs(dx), Math.abs(dy));
        if (distance <= limit) {
            return new double[] {dx, dy};
        }
        double factor = limit / distance;
        return new double[] {dx * factor, dy * factor};
    }
}
