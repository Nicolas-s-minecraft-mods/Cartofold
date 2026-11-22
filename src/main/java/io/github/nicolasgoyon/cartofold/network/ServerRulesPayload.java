package io.github.nicolasgoyon.cartofold.network;

import io.github.nicolasgoyon.cartofold.Cartofold;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the rules of the server for the maps. The server sends them when the player joins, and after a
 * change of the settings of the server.
 *
 * @param entities     the kinds of entities that the maps can show, as a set of bits (see
 *                     {@link io.github.nicolasgoyon.cartofold.view.EntityKind#bit})
 * @param sameTeamOnly true if the maps show only the other players of the team of the player
 */
public record ServerRulesPayload(int entities, boolean sameTeamOnly) implements CustomPacketPayload {
    public static final Type<ServerRulesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Cartofold.MOD_ID, "server_rules"));

    public static final StreamCodec<ByteBuf, ServerRulesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ServerRulesPayload::entities,
            ByteBufCodecs.BOOL, ServerRulesPayload::sameTeamOnly,
            ServerRulesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
