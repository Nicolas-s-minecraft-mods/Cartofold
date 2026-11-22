package io.github.nicolasgoyon.cartofold.client;

import io.github.nicolasgoyon.cartofold.Cartofold;
import io.github.nicolasgoyon.cartofold.view.MinimapLayout;
import io.github.nicolasgoyon.cartofold.view.SpeedZoom;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The speed of the player for the zoom of the minimap (see {@link SpeedZoom}). The speed is the horizontal speed of the
 * vehicle of the player, or of the player with no vehicle: a boat zooms out with its own speed.
 */
@EventBusSubscriber(modid = Cartofold.MOD_ID, value = Dist.CLIENT)
public final class MinimapSpeed {
    /** The ticks in one second. */
    private static final double TICKS = 20;
    /** The part of the new speed in the mean speed at each tick: the mean follows the speed in about 3 ticks. */
    private static final double WEIGHT = 0.3;
    /** A faster move in one tick is a teleport: it does not count. In blocks each second. */
    private static final double TELEPORT = 200;

    @Nullable
    private static Entity last;
    @Nullable
    private static Vec3 lastPos;
    private static double speed;
    private static int level;
    private static double factor = 1;
    private static long lastDraw = -1;

    private MinimapSpeed() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            last = null;
            lastPos = null;
            speed = 0;
            level = 0;
            return;
        }
        Entity entity = minecraft.player.getRootVehicle();
        Vec3 pos = entity.position();
        if (entity == last && lastPos != null) {
            double sample = Math.hypot(pos.x - lastPos.x, pos.z - lastPos.z) * TICKS;
            if (sample < TELEPORT) {
                speed += (sample - speed) * WEIGHT;
            }
        }
        last = entity;
        lastPos = pos;
        level = SpeedZoom.level(speed, level, CartofoldClientConfig.speedZoom());
    }

    /**
     * The scale of the minimap for this frame: the scale of the zoom level of the player with the factor of the speed,
     * but not farther than the setting {@code speedZoomLimit}.
     */
    public static double scale() {
        double base = MinimapLayout.scale(CartofoldClientConfig.MINIMAP_ZOOM.get());
        return SpeedZoom.scale(base, factor(), MinimapLayout.scale(CartofoldClientConfig.MINIMAP_SPEED_ZOOM_LIMIT.get()));
    }

    /**
     * The factor of the scale of the minimap for this frame. The factor goes to the factor of the speed with no jump.
     * It is 1 when the setting is off.
     */
    public static double factor() {
        long now = Util.getMillis();
        double target = CartofoldClientConfig.MINIMAP_SPEED_ZOOM.get()
                ? SpeedZoom.factor(level, CartofoldClientConfig.speedZoom()) : 1;
        factor = lastDraw < 0 ? target : SpeedZoom.approach(factor, target, (now - lastDraw) / 1000.0);
        lastDraw = now;
        return factor;
    }
}
