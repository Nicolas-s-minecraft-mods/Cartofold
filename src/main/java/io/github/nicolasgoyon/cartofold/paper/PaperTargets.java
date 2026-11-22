package io.github.nicolasgoyon.cartofold.paper;

import io.github.nicolasgoyon.cartofold.Cartofold;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

/**
 * The survey maps that point to one target, for the other mods (see
 * {@link io.github.nicolasgoyon.cartofold.api.PaperMapMarkers#targetMap}). The target is a marker of the map with the
 * source {@link #SOURCE}: the check of the markers does not remove it, and a move of the map keeps it when the target
 * stays in the area of the map.
 */
public final class PaperTargets {
    /** The source of the markers of the targets. No block has this source. */
    public static final ResourceLocation SOURCE = ResourceLocation.fromNamespaceAndPath(Cartofold.MOD_ID, "target");
    /** The key of the marker of the target in the map. */
    public static final String KEY = "target";

    private PaperTargets() {
    }

    /**
     * A new survey map with the target at its center.
     *
     * @param level  the level of the target
     * @param target the position of the target
     * @param icon   the icon of the marker of the target
     * @param name   the name of the target: the name of the marker, of the item and the line of the tooltip
     * @param scale  the scale of the map, from 0 to {@link SurveyMapData#MAX_SCALE}
     * @return the item of the map
     */
    public static ItemStack create(ServerLevel level, BlockPos target, Holder<MapDecorationType> icon, Component name,
            int scale) {
        ResourceLocation type = icon.unwrapKey().map(ResourceKey::location)
                .orElseThrow(() -> new IllegalArgumentException("The map decoration type of a target has no id"));
        SurveyMapData data = new SurveyMapData(level.dimension(), target.getX(), target.getZ(), scale, false,
                new byte[PaperMapGeometry.SIZE * PaperMapGeometry.SIZE]);
        data.putMarker(new SurveyMarker(KEY, target.immutable(), type, name.getString(), SOURCE));
        ItemStack stack = SurveyMapItem.create(level.getServer(), data);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.cartofold.survey_map.target", name));
        stack.set(DataComponents.LORE, new ItemLore(List.of(name.copy().withStyle(ChatFormatting.GRAY))));
        return stack;
    }

    /** The target of a survey map, or empty for a map with no target or for an item that is not a survey map. */
    public static Optional<BlockPos> target(MinecraftServer server, ItemStack stack) {
        SurveyMapData data = SurveyMapItem.data(server, stack);
        return data == null ? Optional.empty() : marker(data).map(SurveyMarker::pos);
    }

    /**
     * Removes the target of a survey map, for example when another player opened the treasure. The map keeps its land.
     *
     * @return true if the map had a target
     */
    public static boolean clear(MinecraftServer server, ItemStack stack) {
        SurveyMapData data = SurveyMapItem.data(server, stack);
        return data != null && data.removeMarker(KEY);
    }

    /** The marker of the target of a map, or empty. */
    static Optional<SurveyMarker> marker(SurveyMapData data) {
        return data.markers().stream().filter(PaperTargets::isTarget).findFirst();
    }

    /** True if a marker is the marker of a target. */
    public static boolean isTarget(@Nullable SurveyMarker marker) {
        return marker != null && SOURCE.equals(marker.source());
    }
}
