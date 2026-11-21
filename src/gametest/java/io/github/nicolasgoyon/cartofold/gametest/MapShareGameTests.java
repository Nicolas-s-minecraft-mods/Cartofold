package io.github.nicolasgoyon.cartofold.gametest;

import com.mojang.authlib.GameProfile;
import io.github.nicolasgoyon.cartofold.Cartofold;
import io.github.nicolasgoyon.cartofold.data.ExplorationKey;
import io.github.nicolasgoyon.cartofold.data.ExploredMask;
import io.github.nicolasgoyon.cartofold.data.MapSlice;
import io.github.nicolasgoyon.cartofold.data.RegionPos;
import io.github.nicolasgoyon.cartofold.server.ExplorationService;
import io.github.nicolasgoyon.cartofold.server.MapShares;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-game tests for the shares of maps between players (see {@link MapShares}). */
@GameTestHolder(Cartofold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MapShareGameTests {
    private MapShareGameTests() {
    }

    private static ServerPlayer player(ServerLevel level, String name) {
        return FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), name));
    }

    private static boolean explored(ExplorationService service, ExplorationKey key, UUID player, ChunkPos chunk,
            long now) {
        ExploredMask mask = service.existingMask(key, player, RegionPos.ofChunk(chunk.x, chunk.z), now);
        return mask != null && mask.isChunkExplored(chunk.x, chunk.z);
    }

    /** An accepted offer gives the chunks of the surface and of the caves to the other player, once. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void acceptedOfferGivesTheChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ExplorationService service = ExplorationService.get().orElseThrow();
        ServerPlayer from = player(level, "cartofold_share_a");
        ServerPlayer to = player(level, "cartofold_share_b");
        ChunkPos chunk = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        ExplorationKey surface = ExplorationKey.surface(level.dimension());
        ExplorationKey cave = new ExplorationKey(level.dimension(), MapSlice.caveAt(-30));
        long now = level.getGameTime();
        RegionPos pos = RegionPos.ofChunk(chunk.x, chunk.z);
        service.mask(surface, from.getUUID(), pos, now).markChunk(chunk.x, chunk.z);
        service.mask(cave, from.getUUID(), pos, now).markChunk(chunk.x, chunk.z);

        helper.assertTrue(MapShares.offer(from, to) == 1, "The offer goes");
        helper.assertTrue(MapShares.accept(to, from) == 1, "The other player accepts");
        helper.assertTrue(MapShares.accept(to, from) == 0, "An offer serves only once");

        helper.succeedWhen(() -> {
            long time = level.getGameTime();
            helper.assertTrue(explored(service, surface, to.getUUID(), chunk, time), "The surface is shared");
            helper.assertTrue(explored(service, cave, to.getUUID(), chunk, time), "The caves are shared");
            helper.assertTrue(explored(service, surface, from.getUUID(), chunk, time), "The first map keeps its chunk");
        });
    }

    /** A refused offer gives nothing, and a player cannot share with itself. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void refusedOfferGivesNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer from = player(level, "cartofold_share_c");
        ServerPlayer to = player(level, "cartofold_share_d");

        helper.assertTrue(MapShares.offer(from, from) == 0, "A player cannot share with itself");
        helper.assertTrue(MapShares.accept(to, from) == 0, "No offer, nothing to accept");
        helper.assertTrue(MapShares.offer(from, to) == 1, "The offer goes");
        helper.assertTrue(MapShares.refuse(to, from) == 1, "The other player refuses");
        helper.assertTrue(MapShares.accept(to, from) == 0, "A refused offer cannot be accepted");
        helper.succeed();
    }
}
