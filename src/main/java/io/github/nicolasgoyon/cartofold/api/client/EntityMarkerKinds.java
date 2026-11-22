package io.github.nicolasgoyon.cartofold.api.client;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * The kinds of entities of the other mods on the minimap and on the map of the world, for example the caravans of a
 * mod. An entity of a kind of a mod shows with the color of this kind, in place of the color of its kind of Cartofold
 * (players, hostile, passive, villagers, objects).
 *
 * <pre>{@code
 * EntityMarkerKinds.register(new EntityMarkerKinds.Kind(
 *         ResourceLocation.fromNamespaceAndPath("mymod", "caravans"),
 *         Component.literal("Caravans"), 0xC08040,
 *         entity -> entity instanceof CaravanEntity));
 * }</pre>
 *
 * <p>The entity keeps the rules of its kind of Cartofold: the settings of the player and of the server for this kind
 * select if the maps show it. The player can also hide a kind of a mod with the setting {@code entities.hiddenKinds}:
 * the maps then do not show its entities.
 * Call {@link #register} on the client, for example in the setup of the client.
 */
public final class EntityMarkerKinds {
    private static final List<Kind> KINDS = new CopyOnWriteArrayList<>();

    /**
     * A kind of entities of a mod.
     *
     * @param id       the identifier of the kind, in the namespace of its mod
     * @param name     the name of the kind
     * @param color    the color of the dots of the kind, in RGB
     * @param entities the entities of the kind. The test runs on the client, for each loaded entity, at each draw: keep
     *                 it fast.
     */
    public record Kind(ResourceLocation id, Component name, int color, Predicate<Entity> entities) {
    }

    private EntityMarkerKinds() {
    }

    /** Adds a kind. A kind with the identifier of another kind replaces it. The first kind that matches counts. */
    public static void register(Kind kind) {
        KINDS.removeIf(other -> other.id().equals(kind.id()));
        KINDS.add(kind);
    }

    /** Removes a kind. */
    public static void unregister(ResourceLocation id) {
        KINDS.removeIf(kind -> kind.id().equals(id));
    }

    /** The kinds of the mods, in the order of their registration. */
    public static List<Kind> kinds() {
        return List.copyOf(KINDS);
    }

    /** The first kind of a mod that has an entity, or empty. */
    public static Optional<Kind> of(Entity entity) {
        for (Kind kind : KINDS) {
            if (kind.entities().test(entity)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
