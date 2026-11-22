package io.github.nicolasgoyon.cartofold.view;

/**
 * The zoom of the minimap that follows the speed of the player, or of the vehicle of the player. The minimap shows
 * more land when the player goes faster. The rules:
 *
 * <ul>
 *   <li>The speed has 5 levels: stopped, walk, sprint, a little faster than a sprint (a horse, a boat on water) and
 *       very fast (an elytra, a boat on ice). {@link #LIMITS} gives the lowest speed of each level.</li>
 *   <li>Each level multiplies the scale of the zoom level of the player by its factor (see {@link #FACTORS}).</li>
 *   <li>A speed must go 10% under the limit of a level to go to the level below: a speed near a limit does not change
 *       the zoom at each tick.</li>
 *   <li>The factor of the draw goes to the factor of the level in about one second, with no jump.</li>
 * </ul>
 */
public final class SpeedZoom {
    /** The number of levels of speed. */
    public static final int LEVELS = 5;
    /** The lowest speed of the levels 1 to 4, in blocks each second. */
    private static final double[] LIMITS = {0.5, 4.8, 7.5, 15};
    /** The factors of the scale of the levels 0 to 4. */
    private static final double[] FACTORS = {1, 0.8, 0.6, 0.4, 0.2};
    /** The part of a limit that a speed must go under to go to the level below. */
    private static final double DOWN = 0.9;
    /** The time for the factor of the draw to go about two thirds of the way to its target, in seconds. */
    private static final double SMOOTH_SECONDS = 0.35;

    private SpeedZoom() {
    }

    /**
     * The level of a speed.
     *
     * @param speed    the speed, in blocks each second
     * @param previous the level before, from 0 to 4
     * @return the level, from 0 (stopped) to 4 (very fast)
     */
    public static int level(double speed, int previous) {
        int level = 0;
        while (level < LIMITS.length && speed >= LIMITS[level]) {
            level++;
        }
        // The level goes down only when the speed is clearly under the limit of the level before.
        if (previous > level && previous <= LIMITS.length && speed >= LIMITS[previous - 1] * DOWN) {
            return previous;
        }
        return level;
    }

    /** The factor of the scale of a level. A level out of the range gives the nearest level. */
    public static double factor(int level) {
        return FACTORS[Math.clamp(level, 0, FACTORS.length - 1)];
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
