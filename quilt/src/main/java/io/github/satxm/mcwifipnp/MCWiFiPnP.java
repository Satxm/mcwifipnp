package io.github.satxm.mcwifipnp;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents;
import org.quiltmc.qsl.screen.api.client.ScreenEvents;

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
	public void onInitialize(ModContainer mod) {
		ServerLifecycleEvents.STARTING.register(this::onServerLoad);
		ServerLifecycleEvents.STOPPING.register(this::onServerStop);
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

	public static void afterScreenInit(Screen screen, Minecraft client, int i, int j) {
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
