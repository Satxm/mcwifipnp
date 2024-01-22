package io.github.satxm.mcwifipnp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.players.PlayerList;
import io.github.satxm.mcwifipnp.mixin.PlayerListAccessor;

public class MCWiFiPnP implements ModInitializer {
    public static final String MODID = "mcwifipnp";

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerLoad);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
        ScreenEvents.AFTER_INIT.register(MCWiFiPnP::afterScreenInit);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            DeOpCommands.register(dispatcher);
            OpCommand.register(dispatcher);
            WhitelistCommand.register(dispatcher);
            BanIpCommands.register(dispatcher);
            BanListCommands.register(dispatcher);
            BanPlayerCommands.register(dispatcher);
        });
    }

    public static void afterScreenInit(Minecraft client, Screen screen, int i, int j) {
        if (screen instanceof PauseScreen) {
            for (AbstractWidget button :Screens.getButtons(screen)) {
                if (button.getMessage().equals(Component.translatable("menu.shareToLan"))) {
                    Button newButton = Button.builder(Component.translatable("menu.shareToLan"), $ -> client.setScreen(new ShareToLanScreenNew(screen))).bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight()).build();
                    newButton.active = button.active;
                    Screens.getButtons(screen).remove(button);
                    Screens.getButtons(screen).add(newButton);
                }
            }
        }
    }

    private void onServerLoad(MinecraftServer server) {
        MCWiFiPnPUnit.ReadingConfig(server);
    }

    private void onServerStop(MinecraftServer server) {
        MCWiFiPnPUnit.CloseUPnPPort(server);
    }

    public static void setMaxPlayers(IntegratedServer server, int num) {
        PlayerList playerList = server.getPlayerList();
        ((PlayerListAccessor)playerList).setMaxPlayers(num);
    }

}
