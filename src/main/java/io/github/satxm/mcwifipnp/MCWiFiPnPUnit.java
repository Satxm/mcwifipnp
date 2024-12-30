package io.github.satxm.mcwifipnp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.CommandDispatcher;

import io.github.satxm.mcwifipnp.commands.IpCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;

public class MCWiFiPnPUnit {
	public static final String MODID = "mcwifipnp";
	public static final Component MODIFY_LAN_OPTIONS = Component.translatable("mcwifipnp.gui.lanServerOptions");

	public static final Logger LOGGER = LogManager.getLogger(MCWiFiPnP.class);

	/**
	 * Register commands.
	 * Should be called from platform-specific entries.
	 */
	public static void registerCommands(CommandDispatcher<CommandSourceStack> cmdDispatcher) {
		DeOpCommands.register(cmdDispatcher);
		OpCommand.register(cmdDispatcher);
		WhitelistCommand.register(cmdDispatcher);
		BanIpCommands.register(cmdDispatcher);
		BanListCommands.register(cmdDispatcher);
		BanPlayerCommands.register(cmdDispatcher);
		PardonCommand.register(cmdDispatcher);
		PardonIpCommand.register(cmdDispatcher);
		ForceOfflineCommand.register(cmdDispatcher);
		IpCommand.register(cmdDispatcher);
	}

	public static void publishServer(Config cfg) {
		Minecraft client = Minecraft.getInstance();
		IntegratedServer server = client.getSingleplayerServer();
		PlayerList playerList = server.getPlayerList();

		MutableComponent component = server.publishServer(cfg.gameType, cfg.allowHostCheat, cfg.port)
			? PublishCommand.getSuccessMessage(cfg.port)
			: Component.translatable("commands.publish.failed");
		client.gui.getChat().addMessage(component);
		playerList.getOps().add(new ServerOpListEntry(
			server.getSingleplayerProfile(), 4, playerList.canBypassPlayerLimit(server.getSingleplayerProfile())));

		UPnPModule.startIfEnabled(server, cfg);
		if (cfg.getPublicIP) {
			new Thread(() -> {
				client.gui.getChat().addMessage(IpCommand.getBrief(server));
			}, "MCWiFiPnP").start();
		}
	}

	protected static boolean convertOldUsers(MinecraftServer server) {
		int i;
		boolean bl = false;
		for (i = 0; !bl && i <= 2; ++i) {
			if (i > 0) {
				LOGGER.warn("Encountered a problem while converting the user banlist, retrying in a few seconds");
				MCWiFiPnPUnit.waitForRetry();
			}
			bl = OldUsersConverter.convertUserBanlist(server);
		}
		boolean bl2 = false;
		for (i = 0; !bl2 && i <= 2; ++i) {
			if (i > 0) {
				LOGGER.warn("Encountered a problem while converting the ip banlist, retrying in a few seconds");
				MCWiFiPnPUnit.waitForRetry();
			}
			bl2 = OldUsersConverter.convertIpBanlist(server);
		}
		boolean bl3 = false;
		for (i = 0; !bl3 && i <= 2; ++i) {
			if (i > 0) {
				LOGGER.warn("Encountered a problem while converting the op list, retrying in a few seconds");
				MCWiFiPnPUnit.waitForRetry();
			}
			bl3 = OldUsersConverter.convertOpsList(server);
		}
		boolean bl4 = false;
		for (i = 0; !bl4 && i <= 2; ++i) {
			if (i > 0) {
				LOGGER.warn("Encountered a problem while converting the whitelist, retrying in a few seconds");
				MCWiFiPnPUnit.waitForRetry();
			}
			bl4 = OldUsersConverter.convertWhiteList(server);
		}
		return bl || bl2 || bl3 || bl4;
	}

	private static void waitForRetry() {
		try {
			Thread.sleep(5000L);
		} catch (InterruptedException interruptedException) {
			return;
		}
	}

}