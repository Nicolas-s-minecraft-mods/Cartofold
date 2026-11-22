package io.github.nicolasgoyon.cartofold.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpeedZoomTest {
    @Test
    void eachSpeedHasItsLevel() {
        assertEquals(0, SpeedZoom.level(0, 0));
        assertEquals(1, SpeedZoom.level(4.3, 0));
        assertEquals(2, SpeedZoom.level(5.6, 0));
        assertEquals(3, SpeedZoom.level(10, 0));
        assertEquals(4, SpeedZoom.level(30, 0));
    }

    /** A speed a little under a limit keeps the level: the zoom does not change at each tick. */
    @Test
    void levelGoesDownOnlyUnderTheMargin() {
        assertEquals(2, SpeedZoom.level(7.2, 2));
        assertEquals(2, SpeedZoom.level(4.5, 2));
        assertEquals(1, SpeedZoom.level(4.0, 2));
        assertEquals(0, SpeedZoom.level(0, 4));
    }

    @Test
    void fasterLevelsShowMoreLand() {
        for (int level = 1; level < SpeedZoom.LEVELS; level++) {
            assertTrue(SpeedZoom.factor(level) < SpeedZoom.factor(level - 1));
        }
        assertEquals(1, SpeedZoom.factor(0));
        assertEquals(SpeedZoom.factor(4), SpeedZoom.factor(9));
    }

    @Test
    void factorGoesToTheTargetWithNoJump() {
        double half = SpeedZoom.approach(1, 0.25, 0.05);

        assertTrue(half < 1 && half > 0.8);
        assertEquals(0.25, SpeedZoom.approach(1, 0.25, 10), 1e-6);
        assertEquals(1, SpeedZoom.approach(1, 0.25, 0), 1e-9);
    }

    @Test
    void defaultSettingsKeepTheLimitsAndTheFactors() {
        SpeedZoom.Settings settings = SpeedZoom.Settings.DEFAULT;

        assertEquals(1, SpeedZoom.level(0.5, 0, settings));
        assertEquals(2, SpeedZoom.level(4.8, 0, settings));
        assertEquals(3, SpeedZoom.level(7.5, 0, settings));
        assertEquals(4, SpeedZoom.level(15, 0, settings));
        double[] factors = {1, 0.8, 0.6, 0.4, 0.2};
        for (int level = 0; level < SpeedZoom.LEVELS; level++) {
            assertEquals(factors[level], SpeedZoom.factor(level, settings), 1e-9);
        }
    }

    @Test
    void settingsMoveTheLimitsAndTheStrongestZoomOut() {
        SpeedZoom.Settings settings = new SpeedZoom.Settings(2, 30, 10);

        assertEquals(0, SpeedZoom.level(1.9, 0, settings));
        assertEquals(1, SpeedZoom.level(2, 0, settings));
        assertEquals(3, SpeedZoom.level(20, 0, settings));
        assertEquals(4, SpeedZoom.level(30, 0, settings));
        assertEquals(0.1, SpeedZoom.factor(4, settings), 1e-9);
        assertEquals(1, SpeedZoom.factor(4, new SpeedZoom.Settings(2, 30, 1)), 1e-9);
    }

    @Test
    void settingsStayValid() {
        SpeedZoom.Settings settings = new SpeedZoom.Settings(10, 5, 0.2);

        assertEquals(10, settings.fullSpeed());
        assertEquals(1, settings.zoomOut());
        assertEquals(4, SpeedZoom.level(10, 0, settings));
    }

    @Test
    void scaleStopsAtTheFarthestZoomLevel() {
        assertEquals(0.2, SpeedZoom.scale(1, 0.2, 1.0 / 16), 1e-9);
        assertEquals(0.25, SpeedZoom.scale(1, 0.2, 0.25), 1e-9);
        // A zoom level of the player farther than the limit stays.
        assertEquals(1.0 / 16, SpeedZoom.scale(1.0 / 16, 0.2, 0.25), 1e-9);
    }
}
