package io.github.satxm.mcwifipnp;
import java.util.List;

import io.github.satxm.mcwifipnp.mixin.PlayerListAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.players.PlayerList;

public class MCWiFiPnP implements ModInitializer {
    public static final String MODID = "mcwifipnp";

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerLoad);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
        ScreenEvents.AFTER_INIT.register(MCWiFiPnP::afterScreenInit);

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
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
			final List<AbstractWidget> buttons = Screens.getButtons(screen);
			for (int k = 0; k < buttons.size(); k++) {
				AbstractWidget ShareToLanOld = buttons.get(k);
				if (buttons.size() != 0 && ShareToLanOld.getMessage().getString()
						.equals(new TranslatableComponent("menu.shareToLan").getString())) {
					AbstractWidget ShareToLanNew = new Button(ShareToLanOld.x, ShareToLanOld.y, ShareToLanOld.getWidth(), ShareToLanOld.getHeight(),new TranslatableComponent("menu.shareToLan"),
							(button) -> client.setScreen(new ShareToLanScreenNew(screen)));
					ShareToLanNew.active = ShareToLanOld.active;
					buttons.remove(ShareToLanOld);
					buttons.add(ShareToLanNew);
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
