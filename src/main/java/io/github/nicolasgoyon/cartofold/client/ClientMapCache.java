package io.github.nicolasgoyon.cartofold.client;

import io.github.nicolasgoyon.cartofold.Cartofold;
import io.github.nicolasgoyon.cartofold.data.ExplorationKey;
import io.github.nicolasgoyon.cartofold.data.MapRegion;
import io.github.nicolasgoyon.cartofold.data.RegionPos;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The cache of the map on the disk of the client, for each world (see
 * {@link io.github.nicolasgoyon.cartofold.network.MapWorldPayload}). The rules:
 *
 * <ul>
 *   <li>The folder is {@code cartofold/cache/<world>} in the folder of the game. A region of a slice is one file,
 *       with the format of the regions of the server.</li>
 *   <li>A view that needs a region reads it first from the cache, on an IO thread. The request to the server waits for
 *       this read (see {@link ExplorationRequests}), and then tells the server the data that the client has: the
 *       server sends only the newer chunks.</li>
 *   <li>The changed regions go to the disk every {@link #FLUSH_TICKS} ticks, when they go out of the memory, and when
 *       the player leaves the world.</li>
 *   <li>Before the identifier of the world, the client waits {@link #WAIT_TICKS} ticks at most. With no identifier,
 *       the client has no cache: the server sends all the chunks, as with an empty cache.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Cartofold.MOD_ID, value = Dist.CLIENT)
public final class ClientMapCache {
    /** The time between two writes of the changed regions, in ticks. */
    public static final int FLUSH_TICKS = 20 * 30;
    /** The longest wait for the identifier of the world, in ticks. */
    public static final int WAIT_TICKS = 60;

    @Nullable
    private static Path folder;
    private static boolean closed;
    private static int ticks;
    /** The regions in the cache, for each slice: the files of the folder of the slice, read once. */
    private static final Map<ExplorationKey, Set<RegionPos>> FILES = new HashMap<>();
    /** The regions that an IO thread reads now. */
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    /** The last write of each file: the writes of one file run in their order. */
    private static final Map<Path, CompletableFuture<Void>> WRITES = new ConcurrentHashMap<>();

    private ClientMapCache() {
    }

    /** Opens the cache of a world. The server sends the identifier when the player joins. */
    public static void open(UUID world) {
        Path root = Minecraft.getInstance().gameDirectory.toPath().resolve(Cartofold.MOD_ID).resolve("cache");
        folder = root.resolve(world.toString());
        closed = false;
        FILES.clear();
        ClientExploration.get().onEvictions(ClientMapCache::write);
    }

    /** True while the client waits for the identifier of the world: the requests wait too. */
    public static boolean waiting() {
        return folder == null && !closed && ticks < WAIT_TICKS;
    }

    /**
     * Starts the read of a region from the cache, if the cache has it and the memory does not have it.
     *
     * @return true if the region is ready: in the memory, or not in the cache; false while the read runs
     */
    public static boolean ensure(ExplorationKey key, RegionPos pos) {
        Path base = folder;
        ClientExploration store = ClientExploration.get();
        if (base == null || store.has(key, pos) || !files(key).contains(pos)) {
            return true;
        }
        String id = key + "/" + pos;
        if (LOADING.add(id)) {
            Path file = file(base, key, pos);
            // A write of this file that runs now ends before the read. The read reports its end in the store.
            var unused = WRITES.getOrDefault(file, CompletableFuture.completedFuture(null))
                    .thenApplyAsync(ignored -> read(file, pos), Util.ioPool())
                    .whenComplete((region, error) -> Minecraft.getInstance().execute(() -> {
                        LOADING.remove(id);
                        // A region that the cache cannot read goes out of the list: the server sends it.
                        files(key).remove(pos);
                        if (region != null && region.isPresent() && base.equals(folder)) {
                            store.putCached(key, region.get());
                        }
                    }));
        }
        return false;
    }

    /** The regions of a slice in the cache. */
    private static Set<RegionPos> files(ExplorationKey key) {
        return FILES.computeIfAbsent(key, ignored -> {
            Set<RegionPos> regions = new HashSet<>();
            Path base = folder;
            if (base == null) {
                return regions;
            }
            Path slice = slice(base, key);
            if (Files.isDirectory(slice)) {
                try (Stream<Path> files = Files.list(slice)) {
                    files.forEach(path -> RegionPos.fromFileName(path.getFileName().toString())
                            .ifPresent(regions::add));
                } catch (IOException e) {
                    Cartofold.LOGGER.warn("The map cache {} cannot be listed", slice, e);
                }
            }
            return regions;
        });
    }

    private static Optional<MapRegion> read(Path file, RegionPos pos) {
        try (InputStream in = Files.newInputStream(file)) {
            return Optional.of(MapRegion.read(pos, in));
        } catch (IOException e) {
            Cartofold.LOGGER.warn("The map cache {} is not valid: the server sends the region again", file, e);
            return Optional.empty();
        }
    }

    /** The folder of a slice. A dimension with ".." in its identifier cannot reach a folder out of the cache. */
    private static Path slice(Path base, ExplorationKey key) {
        ResourceLocation id = key.dimension().location();
        Path slice = base.resolve(id.getNamespace()).resolve(id.getPath())
                .resolve(key.slice().surface() ? "surface" : key.slice().folder());
        if (!slice.normalize().startsWith(base.normalize())) {
            throw new IllegalArgumentException("The exploration key " + key + " is not a valid folder");
        }
        return slice;
    }

    private static Path file(Path base, ExplorationKey key, RegionPos pos) {
        return slice(base, key).resolve(pos.fileName());
    }

    /** Writes a region on an IO thread. The region is a copy, or a region out of the store. */
    private static void write(ExplorationKey key, MapRegion region) {
        Path base = folder;
        if (base == null) {
            return;
        }
        Path file = file(base, key, region.pos());
        files(key).add(region.pos());
        WRITES.compute(file, (path, previous) -> (previous == null ? CompletableFuture.<Void>completedFuture(null)
                : previous).thenRunAsync(() -> {
                    try {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        region.write(bytes);
                        Files.createDirectories(path.getParent());
                        Path temporary = path.resolveSibling(path.getFileName() + "." + UUID.randomUUID() + ".tmp");
                        Files.write(temporary, bytes.toByteArray());
                        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException e) {
                        Cartofold.LOGGER.warn("The map cache {} cannot be written", path, e);
                    }
                }, Util.ioPool()));
    }

    /** Writes the changed regions. */
    public static void flush() {
        if (folder != null) {
            ClientExploration.get().takeUnsaved(ClientMapCache::write);
        }
    }

    /** Writes the changed regions, then closes the cache: the player leaves the world. */
    public static void close() {
        flush();
        folder = null;
        closed = false;
        ticks = 0;
        FILES.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        ticks++;
        if (folder == null && ticks >= WAIT_TICKS) {
            // No identifier of the world: no cache for this game.
            closed = true;
        }
        if (ticks % FLUSH_TICKS == 0) {
            flush();
        }
    }
}
