package io.github.nicolasgoyon.cartofold.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.nicolasgoyon.cartofold.view.EntityKind;
import io.github.nicolasgoyon.cartofold.view.MapProjection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.joml.Matrix4f;

/**
 * Draws the loaded entities on the minimap and on the map of the world: a dot with the color of the kind of the entity
 * (see {@link EntityKind}). The settings of the client select the kinds, and the server can stop each kind (see
 * {@link ClientRules}). The client knows only the entities near the player, so the dots show only near the player.
 */
public final class EntityMarkers {
    /** The half side of a dot, in units of the screen, before the scale of the markers. */
    private static final float HALF = 1.5f;
    /** A dot of a player is larger by this factor. */
    private static final float PLAYER = 1.4f;
    private static final int OUTLINE = 0xFF000000;

    private EntityMarkers() {
    }

    /** The kinds of entities that the maps show now: the kinds of the settings that the server lets show. */
    public static int shown() {
        int mask = 0;
        for (Map.Entry<EntityKind, ModConfigSpec.BooleanValue> entry : CartofoldClientConfig.ENTITIES.entrySet()) {
            if (entry.getValue().get()) {
                mask |= entry.getKey().bit();
            }
        }
        return mask & ClientRules.entities();
    }

    /**
     * Draws the entities of a dimension in the rectangle of a projection.
     *
     * @param labels the names of the markers: the players add their names to it, or null to show no name
     */
    public static void render(GuiGraphics graphics, ResourceKey<Level> dimension, MapProjection projection,
            float partialTick, @Nullable MapLabels labels) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        int shown = shown();
        if (level == null || player == null || shown == 0 || !level.dimension().equals(dimension)) {
            return;
        }
        float half = (float) (HALF * CartofoldClientConfig.MARKER_SCALE.get());
        int height = CartofoldClientConfig.ENTITIES_HEIGHT.get();
        double playerY = player.getY();
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = MapShapes.begin(VertexFormat.Mode.TRIANGLES);
        for (Entity entity : level.entitiesForRendering()) {
            EntityKind kind = EntityKind.of(entity);
            // The player has its arrow, and the vehicle of the player is under the arrow.
            if (kind == null || !kind.in(shown) || entity == player || entity.hasIndirectPassenger(player)
                    || entity.isInvisibleTo(player)) {
                continue;
            }
            Vec3 position = entity.getPosition(partialTick);
            // A mob in a cave under the player is not on the surface map, and the reverse.
            if (height > 0 && Math.abs(position.y - playerY) > height) {
                continue;
            }
            double[] point = projection.toScreen(position.x, position.z);
            if (!projection.contains(point[0], point[1])) {
                continue;
            }
            float x = (float) point[0];
            float y = (float) point[1];
            float size = kind == EntityKind.PLAYERS ? half * PLAYER : half;
            MapShapes.rectangle(buffer, matrix, x - size - 1, y - size - 1, x + size + 1, y + size + 1, OUTLINE);
            MapShapes.rectangle(buffer, matrix, x - size, y - size, x + size, y + size, 0xFF000000 | kind.color());
            // A player that sneaks keeps its name hidden, as in the world.
            if (labels != null && entity instanceof Player other && !other.isDiscrete()) {
                labels.add(other.getDisplayName(), x, y + size + CartofoldClientConfig.markerSizes().nameOffset(),
                        false);
            }
        }
        MapShapes.draw(buffer);
    }
}
