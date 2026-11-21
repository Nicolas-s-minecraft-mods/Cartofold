package io.github.nicolasgoyon.cartofold.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nicolasgoyon.cartofold.data.RegionPos;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExplorationRequestPayloadTest {
    @Test
    void combineKeepsTheFirstRegionsAndRemovesTheDuplicates() {
        List<RegionPos> regions = ExplorationRequestPayload.combine(
                List.of(new RegionPos(0, 0), new RegionPos(1, 0)), List.of(new RegionPos(1, 0), new RegionPos(5, 5)));

        assertEquals(List.of(new RegionPos(0, 0), new RegionPos(1, 0), new RegionPos(5, 5)), regions);
    }

    @Test
    void combineKeepsTheRegionsNearTheCenterOfTheView() {
        List<RegionPos> view = new ArrayList<>();
        for (int z = -10; z <= 10; z++) {
            for (int x = -10; x <= 10; x++) {
                view.add(new RegionPos(x + 100, z));
            }
        }

        List<RegionPos> regions = ExplorationRequestPayload.combine(List.of(new RegionPos(0, 0)), view);

        assertEquals(ExplorationRequestPayload.MAX_REGIONS, regions.size());
        assertEquals(new RegionPos(0, 0), regions.get(0));
        assertTrue(regions.contains(new RegionPos(100, 0)));
        assertFalse(regions.contains(new RegionPos(90, -10)));
    }

    /** The data that the client has for each region goes through the network. */
    @Test
    void knownDataGoesThroughTheNetwork() {
        ExplorationRequestPayload payload = new ExplorationRequestPayload(net.minecraft.world.level.Level.OVERWORLD,
                List.of(new ExplorationRequestPayload.SliceRegions(io.github.nicolasgoyon.cartofold.data.MapSlice.SURFACE,
                        List.of(new RegionPos(1, 2), new RegionPos(-3, 4)),
                        List.of(ExplorationRequestPayload.Known.NONE, new ExplorationRequestPayload.Known(9_000, 40)))),
                false);
        net.minecraft.network.RegistryFriendlyByteBuf buffer = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY,
                net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);

        ExplorationRequestPayload.STREAM_CODEC.encode(buffer, payload);

        ExplorationRequestPayload read = ExplorationRequestPayload.STREAM_CODEC.decode(buffer);
        assertEquals(payload, read);
        assertEquals(new ExplorationRequestPayload.Known(9_000, 40), read.slices().get(0).known(new RegionPos(-3, 4)));
        assertEquals(ExplorationRequestPayload.Known.NONE, read.slices().get(0).known(new RegionPos(7, 7)));
    }

    @Test
    void knownDataNeedsOneValueForEachRegion() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ExplorationRequestPayload.SliceRegions(io.github.nicolasgoyon.cartofold.data.MapSlice.SURFACE,
                        List.of(new RegionPos(1, 2)), List.of()));
    }
}
