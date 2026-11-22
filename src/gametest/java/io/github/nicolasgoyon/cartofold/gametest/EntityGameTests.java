package io.github.nicolasgoyon.cartofold.gametest;

import com.mojang.authlib.GameProfile;
import io.github.nicolasgoyon.cartofold.Cartofold;
import io.github.nicolasgoyon.cartofold.CartofoldServerConfig;
import io.github.nicolasgoyon.cartofold.api.client.EntityMarkerKinds;
import io.github.nicolasgoyon.cartofold.server.ServerRules;
import io.github.nicolasgoyon.cartofold.view.EntityKind;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Cartofold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EntityGameTests {
    private EntityGameTests() {
    }

    /** Each entity has its kind on the maps. An arrow is not on the maps. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void entitiesHaveTheirKinds(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.assertTrue(EntityKind.of(helper.spawnWithNoFreeWill(EntityType.ZOMBIE, pos)) == EntityKind.HOSTILE,
                "A zombie is hostile");
        helper.assertTrue(EntityKind.of(helper.spawnWithNoFreeWill(EntityType.SLIME, pos)) == EntityKind.HOSTILE,
                "A slime is hostile");
        helper.assertTrue(EntityKind.of(helper.spawnWithNoFreeWill(EntityType.COW, pos)) == EntityKind.PASSIVE,
                "A cow is passive");
        helper.assertTrue(EntityKind.of(helper.spawnWithNoFreeWill(EntityType.VILLAGER, pos)) == EntityKind.VILLAGERS,
                "A villager is a villager");
        helper.assertTrue(EntityKind.of(helper.spawnWithNoFreeWill(EntityType.WANDERING_TRADER, pos))
                == EntityKind.VILLAGERS, "A wandering trader is a villager");
        helper.assertTrue(EntityKind.of(helper.spawn(EntityType.ARMOR_STAND, pos)) == EntityKind.OBJECTS,
                "An armor stand is an object");
        helper.assertTrue(EntityKind.of(helper.spawn(EntityType.BOAT, pos)) == EntityKind.OBJECTS,
                "A boat is an object");
        ItemEntity item = helper.spawnItem(Items.APPLE, 1.5f, 2, 1.5f);
        helper.assertTrue(EntityKind.of(item) == EntityKind.OBJECTS, "An item is an object");
        helper.assertTrue(EntityKind.of(helper.spawn(EntityType.ARROW, pos)) == null, "An arrow is not on the maps");
        helper.succeed();
    }

    /** The rules of the server follow its settings. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void rulesFollowTheSettingsOfTheServer(GameTestHelper helper) {
        helper.assertTrue(ServerRules.rules().entities() == EntityKind.all(), "By default, all the kinds show");
        helper.assertFalse(ServerRules.rules().sameTeamOnly(), "By default, all the players show");
        CartofoldServerConfig.ENTITIES.get(EntityKind.HOSTILE).set(false);
        try {
            int entities = ServerRules.rules().entities();
            helper.assertFalse(EntityKind.HOSTILE.in(entities), "The server stops the hostile mobs");
            helper.assertTrue(EntityKind.PLAYERS.in(entities), "The players still show");
        } finally {
            CartofoldServerConfig.ENTITIES.get(EntityKind.HOSTILE).set(true);
        }
        helper.succeed();
    }

    /** With the rule of the teams, a player sees only the players of its team. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void sameTeamOnlyShowsTheTeam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer viewer = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "cartofold_team_a"));
        ServerPlayer friend = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "cartofold_team_b"));
        ServerPlayer other = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "cartofold_team_c"));
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam("cartofold_test_team");
        try {
            helper.assertTrue(EntityKind.showsPlayer(viewer, other, false), "With no rule, all the players show");
            helper.assertFalse(EntityKind.showsPlayer(viewer, friend, true), "A player with no team sees no player");
            scoreboard.addPlayerToTeam(viewer.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(friend.getScoreboardName(), team);
            helper.assertTrue(EntityKind.showsPlayer(viewer, friend, true), "A player of the team shows");
            helper.assertFalse(EntityKind.showsPlayer(viewer, other, true), "A player out of the team does not show");
        } finally {
            scoreboard.removePlayerTeam(team);
        }
        helper.succeed();
    }

    /** A kind of a mod has the entities of its test, and the first kind that matches counts. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void kindsOfTheModsFindTheirEntities(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        ResourceLocation cows = ResourceLocation.fromNamespaceAndPath("cartofold_test", "cows");
        ResourceLocation animals = ResourceLocation.fromNamespaceAndPath("cartofold_test", "animals");
        EntityMarkerKinds.register(new EntityMarkerKinds.Kind(cows, Component.literal("Cows"), 0x804020,
                entity -> entity.getType() == EntityType.COW));
        EntityMarkerKinds.register(new EntityMarkerKinds.Kind(animals, Component.literal("Animals"), 0x208040,
                entity -> entity instanceof net.minecraft.world.entity.animal.Animal));
        try {
            helper.assertTrue(EntityMarkerKinds.of(helper.spawnWithNoFreeWill(EntityType.COW, pos))
                    .map(kind -> kind.id().equals(cows)).orElse(false), "A cow has the first kind");
            helper.assertTrue(EntityMarkerKinds.of(helper.spawnWithNoFreeWill(EntityType.PIG, pos))
                    .map(kind -> kind.id().equals(animals)).orElse(false), "A pig has the second kind");
            helper.assertTrue(EntityMarkerKinds.of(helper.spawnWithNoFreeWill(EntityType.ZOMBIE, pos)).isEmpty(),
                    "A zombie has no kind of a mod");
        } finally {
            EntityMarkerKinds.unregister(cows);
            EntityMarkerKinds.unregister(animals);
        }
        helper.succeed();
    }

    /**
     * A mock player with no Cartofold channel (the players of the GameTests of the other mods) joins with no error: the
     * login sends it no payload of Cartofold.
     */
    // The GameTests of the other mods use this deprecated method: the test does the same.
    @SuppressWarnings("removal")
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void playerWithNoChannelJoinsWithNoError(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        helper.assertFalse(player.connection.hasChannel(io.github.nicolasgoyon.cartofold.network.ServerRulesPayload.TYPE),
                "The mock player has no channel of Cartofold");
        net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent login =
                new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player);
        ServerRules.onPlayerLoggedIn(login);
        ServerPlayer fake = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "cartofold_login_fake"));
        ServerRules.onPlayerLoggedIn(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(fake));
        helper.succeed();
    }
}
