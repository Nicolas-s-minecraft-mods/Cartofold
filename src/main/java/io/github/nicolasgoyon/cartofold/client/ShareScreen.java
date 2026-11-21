package io.github.nicolasgoyon.cartofold.client;

import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/**
 * Selects a player to share the map with. A click on a player sends the command {@code /cartofold share <player>}:
 * the other player then accepts or refuses (see {@link io.github.nicolasgoyon.cartofold.server.MapShares}).
 */
public final class ShareScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int ROW = 24;
    /** The number of players on a page. */
    private static final int PAGE = 8;

    @Nullable
    private final Screen parent;
    private int page;

    public ShareScreen(@Nullable Screen parent) {
        super(Component.translatable("cartofold.share.title"));
        this.parent = parent;
    }

    /** The names of the other players on the server, in the order of the alphabet. */
    private List<String> players() {
        ClientPacketListener connection = minecraft == null ? null : minecraft.getConnection();
        if (connection == null || minecraft.player == null) {
            return List.of();
        }
        return connection.getOnlinePlayers().stream()
                .filter(info -> !info.getProfile().getId().equals(minecraft.player.getUUID()))
                .map(PlayerInfo::getProfile).map(profile -> profile.getName())
                .sorted(Comparator.comparing(name -> name.toLowerCase(java.util.Locale.ROOT))).toList();
    }

    @Override
    protected void init() {
        List<String> players = players();
        int pages = Math.max(1, (players.size() + PAGE - 1) / PAGE);
        page = Math.clamp(page, 0, pages - 1);
        int left = width / 2 - BUTTON_WIDTH / 2;
        int top = height / 2 - ROW * (PAGE / 2 + 1);
        List<String> shown = players.subList(page * PAGE, Math.min(players.size(), (page + 1) * PAGE));
        for (int i = 0; i < shown.size(); i++) {
            String name = shown.get(i);
            addRenderableWidget(Button.builder(Component.literal(name), button -> share(name))
                    .bounds(left, top + ROW * i, BUTTON_WIDTH, 20).build());
        }
        int bottom = top + ROW * PAGE + 8;
        int third = (BUTTON_WIDTH - 8) / 3;
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> turn(-1))
                .bounds(left, bottom, third, 20).build());
        previous.active = page > 0;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + third + 4, bottom, third, 20).build());
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> turn(1))
                .bounds(left + (third + 4) * 2, bottom, third, 20).build());
        next.active = page < pages - 1;
    }

    private void turn(int step) {
        page += step;
        rebuildWidgets();
    }

    private void share(String name) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.connection.sendCommand("cartofold share " + name);
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = height / 2 - ROW * (PAGE / 2 + 1);
        graphics.drawCenteredString(font, title, width / 2, top - 30, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("cartofold.share.help"), width / 2, top - 16,
                0xFFA0A0A0);
        if (players().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("cartofold.share.nobody"), width / 2, top + 6,
                    0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
