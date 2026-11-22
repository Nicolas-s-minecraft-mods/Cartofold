package io.github.nicolasgoyon.cartofold.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nicolasgoyon.cartofold.data.RegionPos;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class GreyAreasTest {
    private static LongOpenHashSet chunks(int... xz) {
        LongOpenHashSet set = new LongOpenHashSet();
        for (int i = 0; i < xz.length; i += 2) {
            set.add(ChunkPos.asLong(xz[i], xz[i + 1]));
        }
        return set;
    }

    @Test
    void anAreaTouchesANewChunkInIt() {
        LongOpenHashSet area = chunks(0, 0, 1, 0, 2, 0);

        assertTrue(GreyAreas.touches(area, chunks(9, 9, 2, 0)));
        assertFalse(GreyAreas.touches(area, chunks(9, 9)));
        assertFalse(GreyAreas.touches(area, null));
        assertFalse(GreyAreas.touches(area, new LongOpenHashSet()));
    }

    @Test
    void chunksGoToTheirRegion() {
        Map<RegionPos, LongSet> regions = GreyAreas.byRegion(chunks(0, 0, 31, 31, 32, 0, -1, -1));

        assertEquals(3, regions.size());
        assertEquals(2, regions.get(new RegionPos(0, 0)).size());
        assertEquals(1, regions.get(new RegionPos(1, 0)).size());
        assertEquals(1, regions.get(new RegionPos(-1, -1)).size());
    }
}
