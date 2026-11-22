package io.github.nicolasgoyon.cartofold.server;

import io.github.nicolasgoyon.cartofold.data.RegionPos;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;

/** The rules of the grey parts of the areas, with no access to the game. */
final class GreyAreas {
    private GreyAreas() {
    }

    /** True if a set of chunks contains one of the new chunks. Both sets have chunks as {@link ChunkPos#toLong()}. */
    static boolean touches(LongSet area, @Nullable LongSet newChunks) {
        if (newChunks == null || newChunks.isEmpty()) {
            return false;
        }
        LongSet small = newChunks.size() < area.size() ? newChunks : area;
        LongSet large = small == newChunks ? area : newChunks;
        for (long chunk : small) {
            if (large.contains(chunk)) {
                return true;
            }
        }
        return false;
    }

    /** The chunks of a set, grouped by region. */
    static Map<RegionPos, LongSet> byRegion(LongSet chunks) {
        Map<RegionPos, LongSet> regions = new HashMap<>();
        for (long chunk : chunks) {
            regions.computeIfAbsent(RegionPos.ofChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)),
                    key -> new LongOpenHashSet()).add(chunk);
        }
        return regions;
    }
}
