package io.github.nicolasgoyon.cartofold.network;

import io.github.nicolasgoyon.cartofold.Cartofold;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the identifier of the world of the server. The client keeps a cache of the map for each world: a
 * new world at the same address of a server has a new identifier, so it does not use the cache of the old world. The
 * server sends it when the player joins.
 *
 * @param world the identifier of the world
 */
public record MapWorldPayload(UUID world) implements CustomPacketPayload {
    public static final Type<MapWorldPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Cartofold.MOD_ID, "map_world"));

    public static final StreamCodec<ByteBuf, MapWorldPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, MapWorldPayload::world, MapWorldPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
