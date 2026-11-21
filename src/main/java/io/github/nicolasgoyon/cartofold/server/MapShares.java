package io.github.nicolasgoyon.cartofold.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * The shares of maps between players.
 *
 * <ul>
 *   <li>{@code /cartofold share <player>} offers the explored chunks of the player to another player. The button
 *       <b>Share</b> of the map of the world sends this command.</li>
 *   <li>The other player gets a message with two buttons: {@code /cartofold share accept <player>} and
 *       {@code /cartofold share refuse <player>}.</li>
 *   <li>An accepted offer adds the explored chunks of the first player to the chunks of the other player, in all the
 *       dimensions and all the bands of caves (see {@link ExplorationService#share}). It removes no chunk.</li>
 *   <li>An offer expires after {@link #OFFER_TICKS} ticks, or when one of the two players leaves. A player can send
 *       {@link #MAX_OFFERS} offers each minute.</li>
 * </ul>
 */
public final class MapShares {
    /** The time of an offer, in ticks: 5 minutes. */
    public static final long OFFER_TICKS = 20L * 60 * 5;
    /** The largest number of offers of a player each minute. */
    public static final int MAX_OFFERS = 5;
    private static final ActionLimiter LIMITER = new ActionLimiter(MAX_OFFERS, 20L * 60);
    /** The offers, by the player that gets them, then by the player that sends them, with their game time. */
    private static final Map<UUID, Map<UUID, Long>> OFFERS = new HashMap<>();

    private MapShares() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cartofold").then(Commands.literal("share")
                .then(Commands.literal("accept").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> accept(context.getSource().getPlayerOrException(), player(context)))))
                .then(Commands.literal("refuse").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> refuse(context.getSource().getPlayerOrException(), player(context)))))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> offer(context.getSource().getPlayerOrException(), player(context))))));
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return EntityArgument.getPlayer(context, "player");
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    /** Offers the explored chunks of a player to another player. The result is 1 if the offer goes. */
    public static int offer(ServerPlayer from, ServerPlayer to) {
        if (from.getUUID().equals(to.getUUID())) {
            from.sendSystemMessage(Component.translatable("cartofold.share.self").withStyle(ChatFormatting.RED));
            return 0;
        }
        long now = now(from);
        if (!LIMITER.allow(from.getUUID(), now)) {
            from.sendSystemMessage(Component.translatable("cartofold.share.limit").withStyle(ChatFormatting.RED));
            return 0;
        }
        OFFERS.computeIfAbsent(to.getUUID(), ignored -> new HashMap<>()).put(from.getUUID(), now);
        String name = from.getGameProfile().getName();
        MutableComponent message = Component.translatable("cartofold.share.offer", from.getDisplayName())
                .append(" ").append(button("cartofold.share.accept", "/cartofold share accept " + name,
                        ChatFormatting.GREEN))
                .append(" ").append(button("cartofold.share.refuse", "/cartofold share refuse " + name,
                        ChatFormatting.RED));
        to.sendSystemMessage(message);
        from.sendSystemMessage(Component.translatable("cartofold.share.sent", to.getDisplayName()));
        return 1;
    }

    private static Component button(String key, String command, ChatFormatting color) {
        return Component.literal("[").append(Component.translatable(key)).append("]").withStyle(style -> style
                .withColor(color).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

    /** True if an offer of a player to another player exists and did not expire. The offer then goes away. */
    private static boolean take(ServerPlayer to, ServerPlayer from) {
        Map<UUID, Long> offers = OFFERS.get(to.getUUID());
        Long time = offers == null ? null : offers.remove(from.getUUID());
        if (offers != null && offers.isEmpty()) {
            OFFERS.remove(to.getUUID());
        }
        if (time == null || now(to) - time > OFFER_TICKS) {
            to.sendSystemMessage(Component.translatable("cartofold.share.none", from.getDisplayName())
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    /** Accepts the offer of a player: its explored chunks go to the map of the player that accepts. */
    public static int accept(ServerPlayer to, ServerPlayer from) {
        if (!take(to, from)) {
            return 0;
        }
        ExplorationService service = ExplorationService.get().orElse(null);
        if (service == null) {
            return 0;
        }
        MinecraftServer server = to.server;
        UUID toId = to.getUUID();
        UUID fromId = from.getUUID();
        Component toName = to.getDisplayName();
        Component fromName = from.getDisplayName();
        to.sendSystemMessage(Component.translatable("cartofold.share.copying", fromName));
        service.share(fromId, toId, chunks -> {
            ServerPlayer receiver = server.getPlayerList().getPlayer(toId);
            if (receiver != null) {
                receiver.sendSystemMessage(Component.translatable("cartofold.share.done", chunks, fromName)
                        .withStyle(ChatFormatting.GREEN));
            }
            ServerPlayer sender = server.getPlayerList().getPlayer(fromId);
            if (sender != null) {
                sender.sendSystemMessage(Component.translatable("cartofold.share.accepted", toName));
            }
        });
        return 1;
    }

    /** Refuses the offer of a player. */
    public static int refuse(ServerPlayer to, ServerPlayer from) {
        if (!take(to, from)) {
            return 0;
        }
        from.sendSystemMessage(Component.translatable("cartofold.share.refused", to.getDisplayName()));
        return 1;
    }

    /** The offers of a player and to a player go away when it leaves. */
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID player = event.getEntity().getUUID();
        OFFERS.remove(player);
        OFFERS.values().forEach(offers -> offers.remove(player));
        OFFERS.values().removeIf(Map::isEmpty);
        LIMITER.remove(player);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        OFFERS.clear();
    }
}
