package io.github.nicolasgoyon.cartofold.server;

import io.github.nicolasgoyon.cartofold.Cartofold;
import io.github.nicolasgoyon.cartofold.CartofoldServerConfig;
import io.github.nicolasgoyon.cartofold.network.MapWorldPayload;
import io.github.nicolasgoyon.cartofold.network.ServerRulesPayload;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Sends the rules of the server for the maps to the players: when a player joins, and after a change of the rules.
 * When a player joins, the server also sends the identifier of the world (see {@link MapWorldPayload}). The file
 * {@code data/cartofold/world.id} of the world keeps this identifier.
 */
public final class ServerRules {
    @Nullable
    private static UUID world;

    private ServerRules() {
    }

    /** The identifier of the world, from its file, or a new identifier for a new world. */
    static UUID readWorld(Path file) {
        try {
            if (Files.isRegularFile(file)) {
                return UUID.fromString(Files.readString(file, StandardCharsets.UTF_8).strip());
            }
        } catch (IOException | IllegalArgumentException e) {
            Cartofold.LOGGER.warn("The identifier of the world {} is not valid: the world gets a new one", file, e);
        }
        UUID created = UUID.randomUUID();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, created.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Cartofold.LOGGER.warn("The identifier of the world {} cannot be written", file, e);
        }
        return created;
    }

    public static void onServerStarted(ServerStartedEvent event) {
        world = readWorld(event.getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve(Cartofold.MOD_ID)
                .resolve("world.id"));
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        world = null;
    }

    /** The rules of the server now. */
    public static ServerRulesPayload rules() {
        return new ServerRulesPayload(CartofoldServerConfig.entities(), CartofoldServerConfig.SAME_TEAM_ONLY.get());
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, rules());
            if (world != null) {
                PacketDistributor.sendToPlayer(player, new MapWorldPayload(world));
            }
        }
    }

    /** After a change of the file of the settings of the server, all the players get the new rules. */
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CartofoldServerConfig.SPEC
                && ServerLifecycleHooks.getCurrentServer() != null) {
            PacketDistributor.sendToAllPlayers(rules());
        }
    }
}
