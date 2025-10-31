package io.github.satxm.mcwifipnp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.CommandDispatcher;

import io.github.satxm.mcwifipnp.commands.*;
import io.github.satxm.mcwifipnp.network.UPnPModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.players.OldUsersConverter;

// This is the common entry which should not import any side-specific class
public class MCWiFiPnPUnit {
	public static final String MODID = "mcwifipnp";

	/**
	 * The logger that should be used throughout this mod and its plugins.
	 */
	public static final Logger LOGGER = LogManager.getLogger(MCWiFiPnP.class);

	/**
	 * Register commands.
	 * Should be called from platform-specific entries.
	 */
	public static void registerCommands(CommandDispatcher<CommandSourceStack> cmdDispatcher, boolean isDedicatedServer) {
		// Register our new commands on both client and dedicated server
		UUIDFixerCommand.register(cmdDispatcher);

		if (isDedicatedServer) {
			enableUUIDFixerOnDedicatedServer();
			return;
		}

		// Register our client-only commands
		IpCommand.register(cmdDispatcher);
		OnlineModeCommand.register(cmdDispatcher);
		UPnPCommand.register(cmdDispatcher);

		// Register missing vanilla server commands on the client-side
		DeOpCommands.register(cmdDispatcher);
		OpCommand.register(cmdDispatcher);
		WhitelistCommand.register(cmdDispatcher);
		BanIpCommands.register(cmdDispatcher);
		BanListCommands.register(cmdDispatcher);
		BanPlayerCommands.register(cmdDispatcher);
		PardonCommand.register(cmdDispatcher);
		PardonIpCommand.register(cmdDispatcher);
	}

	/**
	 * Called by platform-specific hooks just before the server stops.
	 * Only runs on client-side
	 */
	public static void onServerStops(MinecraftServer server) {
		if (!server.isDedicatedServer()) {
			UPnPModule.stop(server);
		}
	}

	public static void enableUUIDFixerOnDedicatedServer() {
		UUIDFixer.enabled = true;
		LOGGER.info("UUID Fixer has been enabled on the dedicated server."
			+ "To disable, delete mod McWifiPnP. Config file is \"uuid_fixer.json\".");
	}

	/**
	 * Copied from vanilla DedicatedServer
	 * @param server
	 * @return
	 */
	public static boolean convertOldUsers(MinecraftServer server) {
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