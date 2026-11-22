package io.github.nicolasgoyon.cartofold.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.nicolasgoyon.cartofold.api.MapViewKind;
import io.github.nicolasgoyon.cartofold.api.client.EntityMarkerKinds;
import io.github.nicolasgoyon.cartofold.view.EntityKind;
import io.github.nicolasgoyon.cartofold.view.MapProjection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * (see {@link EntityKind}), or with the color of a kind of another mod (see {@link EntityMarkerKinds}). The settings of
 * the client select the kinds for each view, and the server can stop each kind and show only the players of the same
 * team (see {@link ClientRules}). The client knows only the entities near the player, so the dots show only near the
 * player.
 */
public final class EntityMarkers {
    /** The half side of a dot, in units of the screen, before the scale of the markers. */
    private static final float HALF = 1.5f;
    /** A dot of a player is larger by this factor. */
    private static final float PLAYER = 1.4f;
    private static final int OUTLINE = 0xFF000000;

    private EntityMarkers() {
    }

    /**
     * The kinds of entities that a view shows now: the kinds of the settings of the view that the server lets show.
     *
     * @param kind the view: {@link MapViewKind#WORLD_MAP} for the map of the world, another kind for the minimap
     */
    public static int shown(MapViewKind kind) {
        Map<EntityKind, ModConfigSpec.BooleanValue> settings = kind == MapViewKind.WORLD_MAP
                ? CartofoldClientConfig.WORLD_MAP_ENTITIES : CartofoldClientConfig.ENTITIES;
        int mask = 0;
        for (Map.Entry<EntityKind, ModConfigSpec.BooleanValue> entry : settings.entrySet()) {
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
            MapViewKind view, float partialTick, @Nullable MapLabels labels) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        int shown = shown(view);
        if (level == null || player == null || shown == 0 || !level.dimension().equals(dimension)) {
            return;
        }
        float half = (float) (HALF * CartofoldClientConfig.MARKER_SCALE.get());
        int height = CartofoldClientConfig.ENTITIES_HEIGHT.get();
        double playerY = player.getY();
        boolean sameTeamOnly = ClientRules.sameTeamOnly();
        List<? extends String> hidden = CartofoldClientConfig.HIDDEN_ENTITY_KINDS.get();
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = MapShapes.begin(VertexFormat.Mode.TRIANGLES);
        for (Entity entity : level.entitiesForRendering()) {
            EntityKind kind = EntityKind.of(entity);
            // The player has its arrow, and the vehicle of the player is under the arrow.
            if (kind == null || !kind.in(shown) || entity == player || entity.hasIndirectPassenger(player)
                    || entity.isInvisibleTo(player)
                    || (entity instanceof Player other && !EntityKind.showsPlayer(player, other, sameTeamOnly))) {
                continue;
            }
            Optional<EntityMarkerKinds.Kind> modKind = EntityMarkerKinds.of(entity);
            if (modKind.isPresent() && hidden.contains(modKind.get().id().toString())) {
                continue;
            }
            int color = modKind.map(EntityMarkerKinds.Kind::color).orElse(kind.color());
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
            MapShapes.rectangle(buffer, matrix, x - size, y - size, x + size, y + size, 0xFF000000 | color);
            // A player that sneaks keeps its name hidden, as in the world.
            if (labels != null && entity instanceof Player other && !other.isDiscrete()) {
                labels.add(other.getDisplayName(), x, y + size + CartofoldClientConfig.markerSizes().nameOffset(),
                        false);
            }
        }
        MapShapes.draw(buffer);
    }
}
