package io.github.nicolasgoyon.cartofold;

import io.github.nicolasgoyon.cartofold.view.EntityKind;
import java.util.EnumMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The settings of the server, in the folder {@code serverconfig} of the world. The server sends its rules to each
 * player (see {@link io.github.nicolasgoyon.cartofold.server.ServerRules}).
 */
public final class CartofoldServerConfig {
    public static final ModConfigSpec SPEC;
    /** For each kind of entity: true if the maps of the players can show it. */
    public static final Map<EntityKind, ModConfigSpec.BooleanValue> ENTITIES = new EnumMap<>(EntityKind.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("The entities that the minimaps and the maps of the world of the players can show. A server"
                + " can stop them for a fair game.").translation("cartofold.configuration.server.entities")
                .push("entities");
        for (EntityKind kind : EntityKind.values()) {
            ENTITIES.put(kind, builder.comment("Let the maps of the players show the " + kind.key() + ".")
                    .translation("cartofold.configuration.server.entities." + kind.key()).define(kind.key(), true));
        }
        builder.pop();
        SPEC = builder.build();
    }

    private CartofoldServerConfig() {
    }

    /** The kinds of entities that the maps of the players can show, as a set of bits (see {@link EntityKind#bit}). */
    public static int entities() {
        int mask = 0;
        for (Map.Entry<EntityKind, ModConfigSpec.BooleanValue> entry : ENTITIES.entrySet()) {
            if (entry.getValue().get()) {
                mask |= entry.getKey().bit();
            }
        }
        return mask;
    }
}
