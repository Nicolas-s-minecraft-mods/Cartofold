package io.github.nicolasgoyon.cartofold.gametest;

import io.github.nicolasgoyon.cartofold.Cartofold;
import io.github.nicolasgoyon.cartofold.CartofoldServerConfig;
import io.github.nicolasgoyon.cartofold.server.ServerRules;
import io.github.nicolasgoyon.cartofold.view.EntityKind;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
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
        helper.assertTrue(EntityKind.of(helper.spawnWithNoFreeWill(EntityType.VILLAGER, pos)) == EntityKind.PASSIVE,
                "A villager is passive");
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
}
