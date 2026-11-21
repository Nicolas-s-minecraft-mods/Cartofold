package io.github.nicolasgoyon.cartofold.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegionPosTest {
    @Test
    void fileNameGivesTheRegionBack() {
        RegionPos pos = new RegionPos(-3, 12);

        assertEquals(Optional.of(pos), RegionPos.fromFileName(pos.fileName()));
    }

    @Test
    void otherNamesGiveNoRegion() {
        assertEquals(Optional.empty(), RegionPos.fromFileName("r.1.2.dat.tmp"));
        assertEquals(Optional.empty(), RegionPos.fromFileName("r.a.2.dat"));
        assertEquals(Optional.empty(), RegionPos.fromFileName("r.99999999999.2.dat"));
        assertEquals(Optional.empty(), RegionPos.fromFileName("notes.txt"));
    }
}
