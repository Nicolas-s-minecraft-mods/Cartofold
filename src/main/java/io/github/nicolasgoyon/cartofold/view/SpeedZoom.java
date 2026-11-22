package io.github.nicolasgoyon.cartofold.view;

/**
 * The zoom of the minimap that follows the speed of the player, or of the vehicle of the player. The minimap shows
 * more land when the player goes faster. The rules:
 *
 * <ul>
 *   <li>The speed has 5 levels: stopped, walk, sprint, a little faster than a sprint (a horse, a boat on water) and
 *       very fast (an elytra, a boat on ice). The {@link Settings} give the lowest speed of the level 1 and of the
 *       level 4. The levels 2 and 3 are at the same places between these two speeds as with the default settings.</li>
 *   <li>Each level multiplies the scale of the zoom level of the player by its factor. The factor goes from 1 (level
 *       0) to {@code 1 / zoomOut} (level 4) in equal steps.</li>
 *   <li>A speed must go 10% under the limit of a level to go to the level below: a speed near a limit does not change
 *       the zoom at each tick.</li>
 *   <li>The factor of the draw goes to the factor of the level in about one second, with no jump.</li>
 *   <li>The scale never goes under the scale of a farthest zoom level (see {@link #scale}).</li>
 * </ul>
 */
public final class SpeedZoom {
    /** The number of levels of speed. */
    public static final int LEVELS = 5;
    /** The places of the limits of the levels 1 to 4 between the speed of the level 1 and the speed of the level 4. */
    private static final double[] PLACES = {0, 4.3 / 14.5, 7 / 14.5, 1};
    /** The part of a limit that a speed must go under to go to the level below. */
    private static final double DOWN = 0.9;
    /** The time for the factor of the draw to go about two thirds of the way to its target, in seconds. */
    private static final double SMOOTH_SECONDS = 0.35;

    /**
     * The settings of the zoom with the speed.
     *
     * @param startSpeed the speed of the first zoom out (level 1), in blocks each second
     * @param fullSpeed  the speed of the strongest zoom out (level 4), in blocks each second. A value under
     *                   {@code startSpeed} gives {@code startSpeed}.
     * @param zoomOut    the zoom out at the level 4: 5 shows 5 times more land on each side. A value under 1 gives 1.
     */
    public record Settings(double startSpeed, double fullSpeed, double zoomOut) {
        /** The default settings: a walk starts the zoom, an elytra gives the strongest zoom out, 5 times. */
        public static final Settings DEFAULT = new Settings(0.5, 15, 5);

        public Settings {
            startSpeed = Math.max(0, startSpeed);
            fullSpeed = Math.max(startSpeed, fullSpeed);
            zoomOut = Math.max(1, zoomOut);
        }

        /** The lowest speed of a level from 1 to 4. */
        double limit(int level) {
            return startSpeed + (fullSpeed - startSpeed) * PLACES[level - 1];
        }
    }

    private SpeedZoom() {
    }

    /** The level of a speed with the default settings. See {@link #level(double, int, Settings)}. */
    public static int level(double speed, int previous) {
        return level(speed, previous, Settings.DEFAULT);
    }

    /**
     * The level of a speed.
     *
     * @param speed    the speed, in blocks each second
     * @param previous the level before, from 0 to 4
     * @param settings the settings of the player
     * @return the level, from 0 (stopped) to 4 (very fast)
     */
    public static int level(double speed, int previous, Settings settings) {
        int level = 0;
        while (level < LEVELS - 1 && speed >= settings.limit(level + 1)) {
            level++;
        }
        // The level goes down only when the speed is clearly under the limit of the level before.
        if (previous > level && previous < LEVELS && speed >= settings.limit(previous) * DOWN) {
            return previous;
        }
        return level;
    }

    /** The factor of a level with the default settings. See {@link #factor(int, Settings)}. */
    public static double factor(int level) {
        return factor(level, Settings.DEFAULT);
    }

    /** The factor of the scale of a level. A level out of the range gives the nearest level. */
    public static double factor(int level, Settings settings) {
        int clamped = Math.clamp(level, 0, LEVELS - 1);
        return 1 - (1 - 1 / settings.zoomOut()) * clamped / (LEVELS - 1);
    }

    /**
     * The scale of the minimap with the factor of the speed.
     *
     * @param base     the scale of the zoom level of the player
     * @param factor   the factor of the speed
     * @param farthest the smallest scale of the zoom with the speed. A base scale under it does not change.
     * @return the scale of the draw
     */
    public static double scale(double base, double factor, double farthest) {
        return Math.max(base * factor, Math.min(base, farthest));
    }

    /**
     * The next factor of the draw, nearer to the target.
     *
     * @param current the factor of the last draw
     * @param target  the factor of the level now
     * @param seconds the time since the last draw, in seconds
     * @return the factor of this draw
     */
    public static double approach(double current, double target, double seconds) {
        // The factors multiply the scale: the change is even on a scale of logarithms.
        double keep = Math.exp(-Math.max(0, seconds) / SMOOTH_SECONDS);
        return Math.exp(Math.log(target) + (Math.log(current) - Math.log(target)) * keep);
    }
}
