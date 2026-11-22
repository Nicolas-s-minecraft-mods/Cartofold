package io.github.nicolasgoyon.cartofold.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class LayerContentTest {
    private static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));

    private static MapMarker marker(String id, int x) {
        return MapMarker.of(id, OVERWORLD, new BlockPos(x, 64, 0), 0xFF0000, Component.literal(id));
    }

    private static LayerContent layer(List<MapMarker> markers) {
        ChunkArea area = ChunkArea.of("area", OVERWORLD, List.of(new ChunkPos(0, 0)), 0x40FF0000, 0xFFFF0000,
                Component.literal("Area"));
        return new LayerContent(LayerInfo.of(ResourceLocation.fromNamespaceAndPath("test", "layer"),
                Component.literal("Layer"), 0, false), List.of(area), List.of(), markers);
    }

    @Test
    void markerKeepsAllTheLinesOfItsTooltip() {
        List<Component> lines = List.of(Component.literal("Red faction"), Component.literal("At war"),
                Component.literal("North"));

        MapMarker marker = marker("a", 0).withTooltip(lines).withClick(true);

        assertEquals(lines, marker.tooltipLines());
        assertEquals(java.util.Optional.of(lines.getFirst()), marker.tooltip());
        assertEquals(true, marker.clickable());
    }

    @Test
    void tooltipOverTheLimitFails() {
        List<Component> lines = java.util.Collections.nCopies(MapMarker.MAX_TOOLTIP_LINES + 1, Component.literal("x"));

        assertThrows(IllegalArgumentException.class, () -> marker("a", 0).withTooltip(lines));
    }

    @Test
    void areaKeepsItsChunksWithTheExploredOption() {
        ChunkArea area = ChunkArea.of("area", OVERWORLD, List.of(new ChunkPos(0, 0), new ChunkPos(1, 0)), 0x40FF0000,
                0xFFFF0000, Component.literal("Area"));

        ChunkArea fog = area.withOnlyExplored(true);

        assertEquals(false, area.onlyExplored());
        assertEquals(true, fog.onlyExplored());
        assertEquals(area.chunks(), fog.chunks());
        assertEquals(false, area.equals(fog));
    }

    @Test
    void changedMarkerKeepsItsPlace() {
        LayerContent content = layer(List.of(marker("a", 0), marker("b", 1)));

        LayerContent next = content.apply(LayerUpdate.marker(marker("a", 50)));

        assertEquals(List.of(marker("a", 50), marker("b", 1)), next.markers());
    }

    @Test
    void newMarkerGoesAfterTheOtherMarkers() {
        LayerContent content = layer(List.of(marker("a", 0)));

        LayerContent next = content.apply(LayerUpdate.marker(marker("b", 1)));

        assertEquals(List.of(marker("a", 0), marker("b", 1)), next.markers());
    }

    @Test
    void removeTakesOnlyTheElementOfTheIdentifier() {
        LayerContent content = layer(List.of(marker("a", 0), marker("b", 1)));

        LayerContent next = content.apply(LayerUpdate.removeMarker("a"));

        assertEquals(List.of(marker("b", 1)), next.markers());
    }

    @Test
    void updateKeepsTheOtherKindsOfElements() {
        LayerContent content = layer(List.of(marker("a", 0)));

        LayerContent next = content.apply(LayerUpdate.marker(marker("a", 50)));

        assertSame(content.areas(), next.areas());
    }

    @Test
    void removeAndChangeInOneUpdateKeepTheChange() {
        LayerContent content = layer(List.of(marker("a", 0)));

        LayerContent next = content.apply(new LayerUpdate(List.of(), List.of(), List.of(marker("a", 50)), List.of(),
                List.of(), List.of("a")));

        assertEquals(List.of(marker("a", 50)), next.markers());
    }

    @Test
    void removeAnUnknownElementChangesNothing() {
        LayerContent content = layer(List.of(marker("a", 0)));

        assertEquals(content.markers(), content.apply(LayerUpdate.removeMarker("b")).markers());
    }

    @Test
    void updateOverTheLimitFails() {
        List<MapMarker> markers = new ArrayList<>();
        for (int i = 0; i < LayerContent.MAX_ELEMENTS; i++) {
            markers.add(marker("m" + i, i));
        }
        LayerContent content = layer(markers);
        LayerUpdate update = LayerUpdate.marker(marker("new", 0));

        assertThrows(IllegalArgumentException.class, () -> content.apply(update));
    }

    @Test
    void greyAreaKeepsOnlyItsOwnKnownChunks() {
        ChunkArea area = ChunkArea.of("area", OVERWORLD, List.of(new ChunkPos(0, 0), new ChunkPos(1, 0)), 0x40FF0000,
                0xFFFF0000, Component.literal("Area")).withGreyUnexplored(true);
        it.unimi.dsi.fastutil.longs.LongOpenHashSet known = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        known.add(new ChunkPos(1, 0).toLong());
        known.add(new ChunkPos(5, 5).toLong());

        ChunkArea resolved = area.withKnownChunks(known);

        assertEquals(true, resolved.greyUnexplored());
        assertEquals(1, resolved.knownChunks().size());
        assertEquals(true, resolved.knownChunks().contains(new ChunkPos(1, 0).toLong()));
        // An area with no grey part keeps no known chunks.
        assertEquals(0, resolved.withGreyUnexplored(false).knownChunks().size());
        assertEquals(false, area.equals(resolved));
    }

    @Test
    void greyAreaGoesThroughTheNetwork() {
        ChunkArea area = ChunkArea.of("area", OVERWORLD, List.of(new ChunkPos(0, 0), new ChunkPos(1, 0)), 0x40FF0000,
                0xFFFF0000, Component.literal("Area")).withGreyUnexplored(true);
        it.unimi.dsi.fastutil.longs.LongOpenHashSet known = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        known.add(new ChunkPos(0, 0).toLong());
        ChunkArea resolved = area.withKnownChunks(known);
        net.minecraft.network.RegistryFriendlyByteBuf buffer = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY,
                net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);

        ChunkArea.STREAM_CODEC.encode(buffer, resolved);
        ChunkArea.STREAM_CODEC.encode(buffer, area.withGreyUnexplored(false).withOnlyExplored(true));

        assertEquals(resolved, ChunkArea.STREAM_CODEC.decode(buffer));
        ChunkArea plain = ChunkArea.STREAM_CODEC.decode(buffer);
        assertEquals(false, plain.greyUnexplored());
        assertEquals(true, plain.onlyExplored());
    }
}
