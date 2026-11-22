package io.github.nicolasgoyon.cartofold.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EntityKindTest {
    /** The bits go through the network: they must not change. */
    @Test
    void bitsDoNotChange() {
        assertEquals(1, EntityKind.PLAYERS.bit());
        assertEquals(2, EntityKind.HOSTILE.bit());
        assertEquals(4, EntityKind.PASSIVE.bit());
        assertEquals(8, EntityKind.OBJECTS.bit());
        assertEquals(16, EntityKind.VILLAGERS.bit());
        assertEquals(31, EntityKind.all());
    }

    @Test
    void setOfKindsTellsItsKinds() {
        int mask = EntityKind.PLAYERS.bit() | EntityKind.OBJECTS.bit();

        assertTrue(EntityKind.PLAYERS.in(mask));
        assertFalse(EntityKind.HOSTILE.in(mask));
        assertTrue(EntityKind.OBJECTS.in(mask));
    }
}
