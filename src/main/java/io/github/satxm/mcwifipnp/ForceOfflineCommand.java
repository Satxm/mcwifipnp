package io.github.satxm.mcwifipnp;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.GameProfileArgument.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

public class ForceOfflineCommand {
	private static final SimpleCommandExceptionType ERROR_ALREADY_IN = new SimpleCommandExceptionType(
		Component.translatable("mcwifipnp.commands.forceoffline.add.failed"));
	private static final SimpleCommandExceptionType ERROR_NOT_IN = new SimpleCommandExceptionType(
		Component.translatable("mcwifipnp.commands.forceoffline.remove.failed"));

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {

		LiteralArgumentBuilder<CommandSourceStack> cmdBuilder = Commands.literal("forceoffline")
			.requires((cmdStack) -> cmdStack.hasPermission(3));

		cmdBuilder = cmdBuilder.then(Commands.literal("list").executes((commandContext) -> {
			return showList((CommandSourceStack) commandContext.getSource());
		}));

		RequiredArgumentBuilder<CommandSourceStack, Result> addCmdTargets =
			Commands.argument("targets", GameProfileArgument.gameProfile())
			.suggests((commandContext, suggestionsBuilder) -> {
				MinecraftServer server = ((CommandSourceStack) commandContext.getSource()).getServer();
				PlayerList playerList = server.getPlayerList();
				Config cfg = Config.read(server);
				return SharedSuggestionProvider.suggest(playerList.getPlayers().stream()
					.filter(
						(serverPlayer) -> !cfg.forcedOfflinePlayers.contains(serverPlayer.getGameProfile().getName())
					).map(
						(serverPlayer) -> serverPlayer.getGameProfile().getName()
					), suggestionsBuilder);
			}).executes((commandContext) -> {
				return addPlayers((CommandSourceStack) commandContext.getSource(),
					GameProfileArgument.getGameProfiles(commandContext, "targets"));
			});
		cmdBuilder = cmdBuilder.then(Commands.literal("add").then(addCmdTargets));

		RequiredArgumentBuilder<CommandSourceStack, Result> removeCmdTargets =
			Commands.argument("targets", GameProfileArgument.gameProfile())
			.suggests((commandContext, suggestionsBuilder) -> {
				MinecraftServer server = ((CommandSourceStack) commandContext.getSource()).getServer();
				Config cfg = Config.read(server);
				return SharedSuggestionProvider.suggest(cfg.forcedOfflinePlayers.stream(), suggestionsBuilder);
			}).executes((commandContext) -> {
				return removePlayers((CommandSourceStack) commandContext.getSource(),
					GameProfileArgument.getGameProfiles(commandContext, "targets"));
			});
		cmdBuilder = cmdBuilder.then(Commands.literal("remove").then(removeCmdTargets));

		commandDispatcher.register(cmdBuilder);
	}

	private static int addPlayers(CommandSourceStack commandSourceStack, Collection<GameProfile> collection)
		throws CommandSyntaxException {

		MinecraftServer server = commandSourceStack.getServer();
		Config cfg = Config.read(server);

		int added = 0;
		for (GameProfile gameProfile : collection) {
			if (!cfg.forcedOfflinePlayers.contains(gameProfile.getName())) {
				cfg.forcedOfflinePlayers.add(gameProfile.getName());
				commandSourceStack.sendSuccess(() -> {
					return Component.translatable("mcwifipnp.commands.forceoffline.add.success",
						Component.literal(gameProfile.getName()));
				}, true);
				added++;
			}
		}

		if (added == 0) {
			throw ERROR_ALREADY_IN.create();
		} else {
			cfg.save();
			return added;
		}
	}

	private static int removePlayers(CommandSourceStack commandSourceStack, Collection<GameProfile> collection)
		throws CommandSyntaxException {

		MinecraftServer server = commandSourceStack.getServer();
		Config cfg = Config.read(server);

		int removed = 0;
		for (GameProfile gameProfile: collection) {
			if (cfg.forcedOfflinePlayers.contains(gameProfile.getName())) {
				cfg.forcedOfflinePlayers.remove(gameProfile.getName());
				commandSourceStack.sendSuccess(() -> {
					return Component.translatable("mcwifipnp.commands.forceoffline.remove.success",
						Component.literal(gameProfile.getName()));
				}, true);
				removed++;
			}
		}

		if (removed == 0) {
			throw ERROR_NOT_IN.create();
		} else {
			cfg.save();
			commandSourceStack.getServer().kickUnlistedPlayers(commandSourceStack);
			return removed;
		}
	}

	private static int showList(CommandSourceStack commandSourceStack) {
		MinecraftServer server = commandSourceStack.getServer();
		Config cfg = Config.read(server);

		if (cfg.forcedOfflinePlayers.size() == 0) {
			commandSourceStack.sendSuccess(() -> {
				return Component.translatable("mcwifipnp.commands.forceoffline.none");
			}, false);
		} else {
			commandSourceStack.sendSuccess(() -> {
				return Component.translatable("mcwifipnp.commands.forceoffline.list",
					cfg.forcedOfflinePlayers.size(), StringUtils.join(cfg.forcedOfflinePlayers, ", "));
			}, false);
		}

		return cfg.forcedOfflinePlayers.size();
	}
}
