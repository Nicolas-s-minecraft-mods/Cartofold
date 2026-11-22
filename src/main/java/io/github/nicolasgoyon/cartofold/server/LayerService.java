package io.github.nicolasgoyon.cartofold.server;

import io.github.nicolasgoyon.cartofold.api.ChunkArea;
import io.github.nicolasgoyon.cartofold.api.LayerContent;
import io.github.nicolasgoyon.cartofold.api.LayerUpdate;
import io.github.nicolasgoyon.cartofold.api.MapMarker;
import io.github.nicolasgoyon.cartofold.api.MarkerClickEvent;
import io.github.nicolasgoyon.cartofold.network.LayerPayload;
import io.github.nicolasgoyon.cartofold.network.LayerRemovePayload;
import io.github.nicolasgoyon.cartofold.network.LayerUpdatePayload;
import io.github.nicolasgoyon.cartofold.network.MarkerClickPayload;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The layers of the other mods, on the server (see {@link io.github.nicolasgoyon.cartofold.api.CartofoldLayers}).
 *
 * <ul>
 *   <li>A layer for all the players goes to the players on the server, and to each player that joins later.</li>
 *   <li>A layer for one player goes to this player only. A layer for one player replaces a layer for all the
 *       players with the same identifier, for this player.</li>
 * </ul>
 *
 * <p>The service keeps the layers that each player has, to accept only the clicks on markers that the player has.
 *
 * <p>An area with a grey part (see {@link ChunkArea#withGreyUnexplored}) goes to the clients with the chunks that exist
 * in the world: the chunks with map data on the server. Every {@link #REFRESH_TICKS} ticks, the areas with a grey part
 * that contain a chunk with new map data go again to the players that have them.
 */
public final class LayerService {
    /** The largest number of clicks on markers of a player in one second. */
    public static final int CLICKS_PER_SECOND = 10;
    /** The ticks between two refreshes of the grey parts of the areas. */
    public static final int REFRESH_TICKS = 100;

    private final ActionLimiter clicks = new ActionLimiter(CLICKS_PER_SECOND, 20);
    @Nullable
    private static LayerService instance;

    private final MinecraftServer server;
    private final Map<ResourceLocation, LayerContent> global = new LinkedHashMap<>();
    private final Map<UUID, Map<ResourceLocation, LayerContent>> players = new HashMap<>();
    /** The chunks with new map data since the last refresh, for each dimension. */
    private final Map<ResourceKey<Level>, LongOpenHashSet> newChunks = new HashMap<>();

    LayerService(MinecraftServer server) {
        this.server = server;
    }

    /** The service of the running server, or empty with no server. */
    public static Optional<LayerService> get() {
        return Optional.ofNullable(instance);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        instance = new LayerService(event.getServer());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        instance = null;
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (instance != null && event.getEntity() instanceof ServerPlayer player) {
            instance.global.values().forEach(content -> instance.send(player, content));
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (instance != null) {
            instance.players.remove(event.getEntity().getUUID());
            instance.clicks.remove(event.getEntity().getUUID());
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (instance != null && event.getServer().getTickCount() % REFRESH_TICKS == 0) {
            instance.refresh();
        }
    }

    /** Receives a click on a marker from a client. */
    public static void onMarkerClick(MarkerClickPayload payload, IPayloadContext context) {
        if (instance != null && context.player() instanceof ServerPlayer player
                && instance.clicks.allow(player.getUUID(), player.server.overworld().getGameTime())) {
            instance.click(player, payload.layer(), payload.marker());
        }
    }

    /** Sets a layer for all the players. */
    public void setGlobal(LayerContent content) {
        LayerContent old = global.put(content.info().id(), content);
        LayerPayload payload = null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (hasGlobal(player, content.info().id(), old)) {
                if (payload == null) {
                    payload = new LayerPayload(resolve(content));
                }
                keep(player, content);
                distribute(player, payload);
            }
        }
    }

    /**
     * True if a player shows the layer for all the players, or no layer with this identifier. A layer of one player
     * replaces the layer for all the players: the changes of the layer for all the players do not reach it.
     */
    private boolean hasGlobal(ServerPlayer player, ResourceLocation layer, @Nullable LayerContent global) {
        Map<ResourceLocation, LayerContent> layers = players.get(player.getUUID());
        LayerContent current = layers == null ? null : layers.get(layer);
        return current == null || current == global;
    }

    /** Changes some elements of a layer for all the players that have this layer. */
    public void updateGlobal(ResourceLocation layer, LayerUpdate update) {
        LayerContent content = global.get(layer);
        if (content == null) {
            return;
        }
        LayerContent next = content.apply(update);
        global.put(layer, next);
        LayerUpdatePayload payload = null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Map<ResourceLocation, LayerContent> layers = players.get(player.getUUID());
            // A layer of one player replaces the layer for all the players: it does not get the change.
            if (layers != null && layers.get(layer) == content) {
                layers.put(layer, next);
                if (payload == null) {
                    payload = new LayerUpdatePayload(layer, resolve(update));
                }
                distribute(player, payload);
            }
        }
    }

    /** Removes a layer for all the players. */
    public void removeGlobal(ResourceLocation layer) {
        LayerContent old = global.remove(layer);
        if (old != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (hasGlobal(player, layer, old)) {
                    remove(player, layer);
                }
            }
        }
    }

    /** Sets a layer for one player. */
    public void send(ServerPlayer player, LayerContent content) {
        keep(player, content);
        distribute(player, new LayerPayload(resolve(content)));
    }

    private void keep(ServerPlayer player, LayerContent content) {
        players.computeIfAbsent(player.getUUID(), key -> new HashMap<>()).put(content.info().id(), content);
    }

    /** Changes some elements of a layer for one player. */
    public void update(ServerPlayer player, ResourceLocation layer, LayerUpdate update) {
        Map<ResourceLocation, LayerContent> layers = players.get(player.getUUID());
        LayerContent content = layers == null ? null : layers.get(layer);
        if (content != null) {
            layers.put(layer, content.apply(update));
            distribute(player, new LayerUpdatePayload(layer, resolve(update)));
        }
    }

    /** Removes a layer for one player. */
    public void remove(ServerPlayer player, ResourceLocation layer) {
        Map<ResourceLocation, LayerContent> layers = players.get(player.getUUID());
        if (layers != null && layers.remove(layer) != null) {
            distribute(player, new LayerRemovePayload(layer));
        }
    }

    /** The content of a layer that a player has, or empty. */
    public Optional<LayerContent> layer(UUID player, ResourceLocation layer) {
        return Optional.ofNullable(players.getOrDefault(player, Map.of()).get(layer));
    }

    /**
     * Posts a {@link MarkerClickEvent} if the player has a clickable marker with this identifier.
     *
     * @return true if the event was posted
     */
    public boolean click(ServerPlayer player, ResourceLocation layer, String markerId) {
        Optional<MapMarker> marker = layer(player.getUUID(), layer).flatMap(content -> content.marker(markerId))
                .filter(MapMarker::clickable);
        marker.ifPresent(found -> NeoForge.EVENT_BUS.post(new MarkerClickEvent(player, layer, found)));
        return marker.isPresent();
    }

    /** Records a chunk with new map data: the areas with a grey part that contain it go again to the players. */
    public void chunkKnown(ResourceKey<Level> dimension, ChunkPos chunk) {
        newChunks.computeIfAbsent(dimension, key -> new LongOpenHashSet()).add(chunk.toLong());
    }

    /** Sends again the areas with a grey part that contain a chunk with new map data. */
    void refresh() {
        if (newChunks.isEmpty()) {
            return;
        }
        Map<ResourceKey<Level>, LongOpenHashSet> chunks = new HashMap<>(newChunks);
        newChunks.clear();
        IdentityHashMap<ChunkArea, ChunkArea> resolved = new IdentityHashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Map<ResourceLocation, LayerContent> layers = players.get(player.getUUID());
            if (layers == null) {
                continue;
            }
            for (LayerContent content : layers.values()) {
                List<ChunkArea> changed = new ArrayList<>();
                for (ChunkArea area : content.areas()) {
                    if (area.greyUnexplored() && GreyAreas.touches(area.chunks(), chunks.get(area.dimension()))) {
                        changed.add(resolved.computeIfAbsent(area, this::resolve));
                    }
                }
                if (!changed.isEmpty()) {
                    distribute(player, new LayerUpdatePayload(content.info().id(),
                            new LayerUpdate(changed, List.of(), List.of(), List.of(), List.of(), List.of())));
                }
            }
        }
    }

    /** The content with the chunks that exist in the world in each area with a grey part. */
    private LayerContent resolve(LayerContent content) {
        if (content.areas().stream().noneMatch(ChunkArea::greyUnexplored)) {
            return content;
        }
        return new LayerContent(content.info(), content.areas().stream().map(this::resolve).toList(),
                content.lines(), content.markers());
    }

    private LayerUpdate resolve(LayerUpdate update) {
        if (update.areas().stream().noneMatch(ChunkArea::greyUnexplored)) {
            return update;
        }
        return new LayerUpdate(update.areas().stream().map(this::resolve).toList(), update.lines(),
                update.markers(), update.removedAreas(), update.removedLines(), update.removedMarkers());
    }

    private ChunkArea resolve(ChunkArea area) {
        if (!area.greyUnexplored()) {
            return area;
        }
        LongSet known = ExplorationService.get().map(service -> service.sampledChunks(area.dimension(),
                area.chunks())).orElse(LongSets.EMPTY_SET);
        return area.withKnownChunks(known);
    }

    private static void distribute(ServerPlayer player, CustomPacketPayload payload) {
        if (!(player instanceof FakePlayer) && player.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
