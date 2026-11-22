package io.github.nicolasgoyon.cartofold.view;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;

/** The kinds of entities that the maps can show. The settings of the client and of the server use them. */
public enum EntityKind {
    /** The other players. */
    PLAYERS(0, 0xFFFFFF),
    /** The hostile mobs: zombies, creepers, and the other enemies. */
    HOSTILE(1, 0xFF4040),
    /** The other mobs: animals, fish. */
    PASSIVE(2, 0x50E050),
    /** The villagers and the wandering traders (the mobs that are a {@link Npc}). */
    VILLAGERS(4, 0x50A0FF),
    /** The items on the ground and the vehicles: boats, minecarts. */
    OBJECTS(3, 0xFFD040);

    /** The bit of the kind in the network: it does not change with the order of the kinds. */
    private final int bit;
    private final int color;

    EntityKind(int bit, int color) {
        this.bit = 1 << bit;
        this.color = color;
    }

    /** The color of the dots of this kind, in RGB. */
    public int color() {
        return color;
    }

    /** The bit of this kind in a set of kinds (see {@link #all}). */
    public int bit() {
        return bit;
    }

    /** The name of this kind in the files of settings and in the keys of the translations. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** True if a set of kinds has this kind. */
    public boolean in(int mask) {
        return (mask & bit()) != 0;
    }

    /** The set of all the kinds. */
    public static int all() {
        int mask = 0;
        for (EntityKind kind : values()) {
            mask |= kind.bit();
        }
        return mask;
    }

    /** The kind of an entity, or null for an entity that the maps do not show (an arrow, an orb of experience). */
    @Nullable
    public static EntityKind of(Entity entity) {
        if (entity instanceof Player) {
            return PLAYERS;
        }
        if (entity instanceof Enemy) {
            return HOSTILE;
        }
        if (entity instanceof Npc) {
            return VILLAGERS;
        }
        if (entity instanceof Mob) {
            return PASSIVE;
        }
        // An armor stand is living, but it is not a mob: it is an object.
        if (entity instanceof LivingEntity || entity instanceof ItemEntity || entity instanceof VehicleEntity) {
            return OBJECTS;
        }
        return null;
    }

    /**
     * True if the maps of a player show another player. With the rule of the server for the teams, only the players of
     * the team of the player show, and a player with no team sees no other player.
     *
     * @param viewer       the player of the maps
     * @param other        the other player
     * @param sameTeamOnly the rule of the server
     */
    public static boolean showsPlayer(Player viewer, Player other, boolean sameTeamOnly) {
        return !sameTeamOnly || (viewer.getTeam() != null && viewer.isAlliedTo(other));
    }
}
