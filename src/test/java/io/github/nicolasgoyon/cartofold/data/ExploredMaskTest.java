package io.github.nicolasgoyon.cartofold.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class ExploredMaskTest {
    private static final RegionPos POS = new RegionPos(-1, -1);

    /** A chunk marks its 256 columns, and only them. A second mark changes nothing. */
    @Test
    void chunkMarksItsColumns() {
        ExploredMask mask = new ExploredMask(POS);
        assertTrue(mask.markChunk(-1, -1));
        assertEquals(256, mask.count());
        assertTrue(mask.isExplored(-16, -16));
        assertTrue(mask.isExplored(-1, -1));
        assertFalse(mask.isExplored(-17, -1));
        assertFalse(mask.isExplored(-1, -17));
        assertTrue(mask.isChunkExplored(-1, -1));
        assertFalse(mask.isChunkExplored(-2, -1));
        assertFalse(mask.markChunk(-1, -1));
        assertTrue(mask.isDirty());
    }

    @Test
    void allTheChunksOfARow() {
        ExploredMask mask = new ExploredMask(POS);
        for (int chunkX = -32; chunkX < 0; chunkX++) {
            mask.markChunk(chunkX, -32);
        }
        assertEquals(512 * 16, mask.count());
    }

    /** A change after the copy does not change the copy. */
    @Test
    void copyKeepsTheBitsOfItsTime() {
        ExploredMask mask = new ExploredMask(POS);
        mask.markChunk(-1, -1);

        ExploredMask copy = mask.copy();
        mask.markChunk(-2, -1);

        assertTrue(copy.isChunkExplored(-1, -1));
        assertFalse(copy.isChunkExplored(-2, -1));
        assertFalse(copy.isDirty());
    }

    @Test
    void chunkOutOfTheRegion() {
        ExploredMask mask = new ExploredMask(POS);
        assertThrows(IllegalArgumentException.class, () -> mask.markChunk(0, -1));
    }

    @Test
    void fileKeepsTheMask() throws IOException {
        ExploredMask mask = new ExploredMask(POS);
        mask.markChunk(-5, -9);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mask.write(out);
        ExploredMask read = ExploredMask.read(POS, new ByteArrayInputStream(out.toByteArray()));
        assertEquals(256, read.count());
        assertTrue(read.isChunkExplored(-5, -9));
        assertFalse(read.isDirty());
    }

    /** A merge adds the chunks of the other mask, keeps the chunks of this mask, and tells the new chunks. */
    @Test
    void mergeAddsTheNewChunks() {
        ExploredMask mine = new ExploredMask(POS);
        mine.markChunk(-1, -1);
        mine.setSaved();
        ExploredMask other = new ExploredMask(POS);
        other.markChunk(-1, -1);
        other.markChunk(-5, -9);

        List<ChunkPos> added = mine.merge(other);

        assertEquals(List.of(new ChunkPos(-5, -9)), added);
        assertTrue(mine.isChunkExplored(-1, -1));
        assertTrue(mine.isChunkExplored(-5, -9));
        assertTrue(mine.isDirty());
        assertEquals(List.of(), mine.merge(other));
    }

    @Test
    void mergeOfAnotherRegionFails() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExploredMask(POS).merge(new ExploredMask(new RegionPos(0, 0))));
    }
}
