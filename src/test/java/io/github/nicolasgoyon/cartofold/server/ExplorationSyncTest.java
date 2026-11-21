package io.github.nicolasgoyon.cartofold.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.nicolasgoyon.cartofold.network.ExplorationRequestPayload.Known;
import java.util.List;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class ExplorationSyncTest {
    private static final List<ChunkPos> CHUNKS = List.of(new ChunkPos(0, 0), new ChunkPos(1, 0), new ChunkPos(2, 0));
    private static final long[] TIMES = {100, 200, 300};

    @Test
    void clientWithNoDataGetsAllTheChunks() {
        assertEquals(CHUNKS, ExplorationSync.toSend(CHUNKS, TIMES, Known.NONE));
    }

    /** The client has the chunks until the time 200: it gets only the chunk of the time 300. */
    @Test
    void clientGetsOnlyTheNewerChunks() {
        assertEquals(List.of(new ChunkPos(2, 0)), ExplorationSync.toSend(CHUNKS, TIMES, new Known(200, 2)));
    }

    @Test
    void clientWithAllTheDataGetsNothing() {
        assertEquals(List.of(), ExplorationSync.toSend(CHUNKS, TIMES, new Known(300, 3)));
    }

    /**
     * The client has only one chunk until the time 200, but the server has two: the client misses a chunk (for example
     * a chunk of a shared map). It gets all the chunks.
     */
    @Test
    void clientThatMissesAnOldChunkGetsAllTheChunks() {
        assertEquals(CHUNKS, ExplorationSync.toSend(CHUNKS, TIMES, new Known(200, 1)));
    }

    /** A new sample of a chunk that the client has: the client gets only this chunk. */
    @Test
    void newSampleOfAKnownChunkGoesAlone() {
        long[] times = {100, 200, 900};

        assertEquals(List.of(new ChunkPos(2, 0)), ExplorationSync.toSend(CHUNKS, times, new Known(300, 3)));
    }
}
