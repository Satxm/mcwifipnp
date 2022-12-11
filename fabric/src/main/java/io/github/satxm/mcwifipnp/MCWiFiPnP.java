package io.github.satxm.mcwifipnp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.WhitelistCommand;

public class MCWiFiPnP implements ModInitializer {
	public static final String MODID = "mcwifipnp";

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerLoad);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
		ScreenEvents.AFTER_INIT.register(MCWiFiPnP::afterScreenInit);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
		{
			DeOpCommands.register(dispatcher);
			OpCommand.register(dispatcher);
			WhitelistCommand.register(dispatcher);
			BanIpCommands.register(dispatcher);
			BanListCommands.register(dispatcher);
			BanPlayerCommands.register(dispatcher);
		});
	}

	public static void afterScreenInit(Minecraft client, Screen screen, int i, int j) {
		if (screen instanceof ShareToLanScreen) {
			client.setScreen(new ShareToLanScreenNew(screen));
		}
	}

	private void onServerLoad(MinecraftServer server) {
		MCWiFiPnPUnit.ReadingConfig(server);
	}

	private void onServerStop(MinecraftServer server) {
		MCWiFiPnPUnit.ClosePortUPnP(server);
	}

}
