package io.github.nicolasgoyon.cartofold.client;

import io.github.nicolasgoyon.cartofold.Cartofold;
import io.github.nicolasgoyon.cartofold.network.ServerRulesPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** The rules of the server for the maps, on the client. Before the rules of the server, the maps show no entity. */
@EventBusSubscriber(modid = Cartofold.MOD_ID, value = Dist.CLIENT)
public final class ClientRules {
    private static volatile int entities;
    private static volatile boolean sameTeamOnly;

    private ClientRules() {
    }

    /** The kinds of entities that the server lets the maps show, as a set of bits. */
    public static int entities() {
        return entities;
    }

    /** True if the maps show only the other players of the team of the player. */
    public static boolean sameTeamOnly() {
        return sameTeamOnly;
    }

    public static void set(ServerRulesPayload payload) {
        entities = payload.entities();
        sameTeamOnly = payload.sameTeamOnly();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        entities = 0;
        sameTeamOnly = false;
    }
}
