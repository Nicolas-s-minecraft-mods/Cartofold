package io.github.nicolasgoyon.cartofold.server;

import io.github.nicolasgoyon.cartofold.CartofoldServerConfig;
import io.github.nicolasgoyon.cartofold.network.ServerRulesPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Sends the rules of the server for the maps to the players: when a player joins, and after a change of the rules. */
public final class ServerRules {
    private ServerRules() {
    }

    /** The rules of the server now. */
    public static ServerRulesPayload rules() {
        return new ServerRulesPayload(CartofoldServerConfig.entities());
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, rules());
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
