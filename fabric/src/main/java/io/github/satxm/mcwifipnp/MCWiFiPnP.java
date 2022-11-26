package io.github.satxm.mcwifipnp;

import io.github.satxm.mcwifipnp.mixin.PlayerListAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;

public class MCWiFiPnP implements ModInitializer {
	public static final String MODID = "mcwifipnp";

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerLoad);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
		ScreenEvents.AFTER_INIT.register(MCWiFiPnP::afterScreenInit);
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

	public static void openToLan() {
		Minecraft client = Minecraft.getInstance();
		IntegratedServer server = client.getSingleplayerServer();
		PlayerList playerList = server.getPlayerList();
		MCWiFiPnPUnit.Config cfg = MCWiFiPnPUnit.getConfig(server);

		server.setMotd(cfg.motd);
		server.getStatus().setDescription(Component.literal(cfg.motd));
		server.publishServer(GameType.byName(cfg.GameMode), cfg.AllowCommands, cfg.port);
		((PlayerListAccessor) playerList).setMaxPlayers(cfg.maxPlayers);
		server.setUsesAuthentication(cfg.OnlineMode);
		server.setPvpAllowed(cfg.EnablePvP);
		client.gui.getChat().addMessage(Component.translatable("commands.publish.started", cfg.port));

		new Thread(() -> {
			MCWiFiPnPUnit.UseUPnP(cfg, client);
			MCWiFiPnPUnit.CopyToClipboard(cfg, client);
		}, "MCWiFiPnP").start();
	}
}
