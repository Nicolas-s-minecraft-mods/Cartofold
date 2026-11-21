package io.github.nicolasgoyon.cartofold.waypoint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A waypoint of a player: a named and colored position in a dimension.
 *
 * @param id        the identifier of the waypoint
 * @param name      the name, {@link Waypoints#MAX_NAME} characters at most. The waypoint of a death has no name: the
 *                  views show a translated name.
 * @param color     the color, in RGB
 * @param dimension the dimension of the position
 * @param pos       the position
 * @param death     true for the waypoint of a death of the player
 * @param minDistance the markers in the world hide this waypoint when it is nearer than this distance, in blocks, or
 *                    {@link #DEFAULT} for the distance of the settings of the client
 * @param maxDistance the markers in the world hide this waypoint when it is farther than this distance, in blocks, 0
 *                    for no limit, or {@link #DEFAULT} for the distance of the settings of the client
 */
public record Waypoint(UUID id, String name, int color, ResourceKey<Level> dimension, BlockPos pos, boolean death,
        int minDistance, int maxDistance) {
    /** The value of a distance that follows the settings of the client. */
    public static final int DEFAULT = -1;
    /** The largest distance of a waypoint, in blocks. */
    public static final int MAX_DISTANCE = 100_000;

    public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Waypoint::id),
            Codec.STRING.fieldOf("name").forGetter(Waypoint::name),
            Codec.INT.fieldOf("color").forGetter(Waypoint::color),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Waypoint::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(Waypoint::pos),
            Codec.BOOL.optionalFieldOf("death", false).forGetter(Waypoint::death),
            Codec.INT.optionalFieldOf("minDistance", DEFAULT).forGetter(Waypoint::minDistance),
            Codec.INT.optionalFieldOf("maxDistance", DEFAULT).forGetter(Waypoint::maxDistance))
            .apply(instance, Waypoint::new));

    public static final StreamCodec<ByteBuf, Waypoint> STREAM_CODEC = StreamCodec.of(Waypoint::encode,
            Waypoint::decode);

    public Waypoint {
        name = name.strip();
        if (name.length() > Waypoints.MAX_NAME) {
            name = name.substring(0, Waypoints.MAX_NAME);
        }
        color &= 0xFFFFFF;
        minDistance = distance(minDistance);
        maxDistance = distance(maxDistance);
    }

    /** A waypoint with the distances of the settings of the client. */
    public Waypoint(UUID id, String name, int color, ResourceKey<Level> dimension, BlockPos pos, boolean death) {
        this(id, name, color, dimension, pos, death, DEFAULT, DEFAULT);
    }

    /** This waypoint with other distances. */
    public Waypoint withDistances(int min, int max) {
        return new Waypoint(id, name, color, dimension, pos, death, min, max);
    }

    /** The smallest distance of the markers in the world, with the distance of the settings for {@link #DEFAULT}. */
    public int minDistance(int settings) {
        return minDistance == DEFAULT ? settings : minDistance;
    }

    /** The largest distance of the markers in the world, with the distance of the settings for {@link #DEFAULT}. */
    public int maxDistance(int settings) {
        return maxDistance == DEFAULT ? settings : maxDistance;
    }

    /** A valid distance: {@link #DEFAULT}, or a distance from 0 to {@link #MAX_DISTANCE}. */
    private static int distance(int value) {
        return value < 0 ? DEFAULT : Math.min(value, MAX_DISTANCE);
    }

    private static void encode(ByteBuf buffer, Waypoint waypoint) {
        UUIDUtil.STREAM_CODEC.encode(buffer, waypoint.id);
        NAME_CODEC.encode(buffer, waypoint.name);
        ByteBufCodecs.INT.encode(buffer, waypoint.color);
        DIMENSION_CODEC.encode(buffer, waypoint.dimension);
        BlockPos.STREAM_CODEC.encode(buffer, waypoint.pos);
        ByteBufCodecs.BOOL.encode(buffer, waypoint.death);
        ByteBufCodecs.VAR_INT.encode(buffer, waypoint.minDistance);
        ByteBufCodecs.VAR_INT.encode(buffer, waypoint.maxDistance);
    }

    private static Waypoint decode(ByteBuf buffer) {
        return new Waypoint(UUIDUtil.STREAM_CODEC.decode(buffer), NAME_CODEC.decode(buffer),
                ByteBufCodecs.INT.decode(buffer), DIMENSION_CODEC.decode(buffer), BlockPos.STREAM_CODEC.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer));
    }

    private static final StreamCodec<ByteBuf, String> NAME_CODEC = ByteBufCodecs.stringUtf8(Waypoints.MAX_NAME);
    private static final StreamCodec<ByteBuf, ResourceKey<Level>> DIMENSION_CODEC =
            ResourceKey.streamCodec(Registries.DIMENSION);
}
