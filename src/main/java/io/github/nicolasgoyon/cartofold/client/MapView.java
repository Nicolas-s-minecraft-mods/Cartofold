package io.github.nicolasgoyon.cartofold.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import io.github.nicolasgoyon.cartofold.api.MapViewKind;
import io.github.nicolasgoyon.cartofold.data.ExplorationKey;
import io.github.nicolasgoyon.cartofold.data.MapRegion;
import io.github.nicolasgoyon.cartofold.data.RegionPos;
import io.github.nicolasgoyon.cartofold.view.MapProjection;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Draws the map of a dimension in the rectangle of a projection. The minimap, the map of the world and the map frames
 * use this view. The view draws only the explored chunks: the other pixels keep what is under the view. The layers
 * draw above the chunks (see {@link LayerRenderer}).
 */
public final class MapView {
    private MapView() {
    }

    /**
     * Draws the tiles of a dimension in the rectangle of a projection, then the visible layers.
     *
     * @param graphics   the graphics of the screen
     * @param dimension  the dimension
     * @param projection the projection of the view
     * @param kind       the kind of the view, for the visibility of the layers
     */
    public static void render(GuiGraphics graphics, ResourceKey<Level> dimension, MapProjection projection,
            MapViewKind kind) {
        render(graphics, dimension, projection, kind, layer -> true);
    }

    /**
     * Draws the tiles of a dimension in the rectangle of a projection, then a selection of the visible layers.
     *
     * @param graphics   the graphics of the screen
     * @param dimension  the dimension
     * @param projection the projection of the view
     * @param kind       the kind of the view, for the visibility of the layers
     * @param layers     the layers to draw, among the visible layers
     */
    public static void render(GuiGraphics graphics, ResourceKey<Level> dimension, MapProjection projection,
            MapViewKind kind, Predicate<ResourceLocation> layers) {
        render(graphics, ExplorationKey.surface(dimension), projection, kind, layers, null);
    }

    /**
     * Draws the tiles of a slice of a dimension (the surface or a band of caves) in the rectangle of a projection,
     * then a selection of the visible layers.
     *
     * @param graphics   the graphics of the screen
     * @param slice      the dimension and the slice
     * @param projection the projection of the view
     * @param kind       the kind of the view, for the visibility of the layers
     * @param layers     the layers to draw, among the visible layers
     * @param labels     the names of the markers, or null for a view with no names. The view adds the names to it, and
     *                   the caller draws them above its own draws.
     */
    public static void render(GuiGraphics graphics, ExplorationKey slice, MapProjection projection,
            MapViewKind kind, Predicate<ResourceLocation> layers, @Nullable MapLabels labels) {
        ResourceKey<Level> dimension = slice.dimension();
        int lod = projection.lod(Minecraft.getInstance().getWindow().getGuiScale());
        graphics.enableScissor((int) Math.floor(projection.x()), (int) Math.floor(projection.y()),
                (int) Math.ceil(projection.x() + projection.width()),
                (int) Math.ceil(projection.y() + projection.height()));
        RenderSystem.enableBlend();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(projection.screenCenterX(), projection.screenCenterY(), 0);
        pose.mulPose(Axis.ZP.rotation((float) projection.rotation()));
        pose.scale((float) projection.scale(), (float) projection.scale(), 1);
        Matrix4f matrix = pose.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        for (RegionPos pos : projection.regions()) {
            Optional<ResourceLocation> tile = MapTiles.get().tile(slice, pos, lod);
            if (tile.isEmpty()) {
                continue;
            }
            // Each corner comes from its block in doubles, then goes to floats: two tiles next to each other get the
            // same floats on their common side, so no line of pixels stays between them. The offsets are small: the
            // floats keep the precision far from the origin of the world.
            float minX = (float) (pos.minBlockX() - projection.centerX());
            float maxX = (float) (pos.minBlockX() + MapRegion.SIZE - projection.centerX());
            float minZ = (float) (pos.minBlockZ() - projection.centerZ());
            float maxZ = (float) (pos.minBlockZ() + MapRegion.SIZE - projection.centerZ());
            RenderSystem.setShaderTexture(0, tile.get());
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION_TEX);
            buffer.addVertex(matrix, minX, minZ, 0).setUv(0, 0);
            buffer.addVertex(matrix, minX, maxZ, 0).setUv(0, 1);
            buffer.addVertex(matrix, maxX, maxZ, 0).setUv(1, 1);
            buffer.addVertex(matrix, maxX, minZ, 0).setUv(1, 0);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
        pose.popPose();
        graphics.flush();
        LayerRenderer.render(graphics, dimension, projection, kind, layers, labels);
        graphics.flush();
        RenderSystem.disableBlend();
        graphics.disableScissor();
    }
}
