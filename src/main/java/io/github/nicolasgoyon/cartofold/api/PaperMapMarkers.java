package io.github.nicolasgoyon.cartofold.api;

import io.github.nicolasgoyon.cartofold.Cartofold;
import io.github.nicolasgoyon.cartofold.paper.PaperTargets;
import io.github.nicolasgoyon.cartofold.paper.PaperWaypoints;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

/**
 * The entry point of the waypoints of blocks on the survey maps (the paper maps of Cartofold).
 *
 * <pre>{@code
 * PaperMapMarkers.register(PaperMarkerSource.of(
 *         ResourceLocation.fromNamespaceAndPath("mymod", "village_heart"),
 *         state -> state.is(MyBlocks.VILLAGE_HEART.get()),
 *         MyMapDecorations.VILLAGE,
 *         (level, pos) -> villageAt(level, pos).map(village -> Component.literal(village.name()))));
 * }</pre>
 *
 * <p>Cartofold has a source for the banners (the icons and the names of the vanilla banners) and for the lodestone
 * (the decoration type {@code cartofold:waypoint}). Other mods can use this type for their own sources.
 */
public final class PaperMapMarkers {
    /**
     * The items of the survey maps. A block of a mod with its own use action does not need this tag: Cartofold gives the
     * use of a survey map on a block of a source to the map, before the block (see {@link #register}).
     */
    public static final TagKey<Item> SURVEY_MAPS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Cartofold.MOD_ID, "survey_maps"));

    private PaperMapMarkers() {
    }

    /** True if an item is a survey map (the tag {@link #SURVEY_MAPS}). */
    public static boolean isSurveyMap(ItemStack stack) {
        return stack.is(SURVEY_MAPS);
    }

    /**
     * Adds a source of waypoints. Call this method once, for example in the common setup. A source with the identifier
     * of another source replaces it. A survey map used on a block of a source marks the map, also when the block has
     * its own use action (for example a block that opens a screen): the block does not get this use.
     */
    public static void register(PaperMarkerSource source) {
        PaperWaypoints.register(source);
    }

    /**
     * A new survey map that points to one target, for example a treasure map that a cartographer sells.
     *
     * <ul>
     *   <li>The target is at the center of the map, with a marker: its icon and its name. The use of the map on a block
     *       does not remove this marker, and a move of the map at the table keeps it when the target stays in the area
     *       of the map.</li>
     *   <li>The item has the name "Map to" and the name of the target, and the tooltip shows the name of the target.</li>
     *   <li>The map has no land: the player sees the land when he explores it with the map in his hand, as with the
     *       other survey maps.</li>
     * </ul>
     *
     * @param level  the level of the target
     * @param target the position of the target
     * @param icon   the icon of the marker, a map decoration type with an id
     * @param name   the name of the target
     * @param scale  the scale of the map, from 0 (1 block for each pixel) to 4
     * @return the item of the map
     */
    public static ItemStack targetMap(ServerLevel level, BlockPos target, Holder<MapDecorationType> icon,
            Component name, int scale) {
        return PaperTargets.create(level, target, icon, name, scale);
    }

    /** The target of a survey map, or empty for a map with no target. Use it on the server. */
    public static Optional<BlockPos> target(MinecraftServer server, ItemStack stack) {
        return PaperTargets.target(server, stack);
    }

    /**
     * Removes the target of a survey map, for example when the target is not valid any more. The map keeps its land and
     * its other markers. Use it on the server.
     *
     * @return true if the map had a target
     */
    public static boolean clearTarget(MinecraftServer server, ItemStack stack) {
        return PaperTargets.clear(server, stack);
    }
}
