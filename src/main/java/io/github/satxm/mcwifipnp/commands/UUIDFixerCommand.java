package io.github.satxm.mcwifipnp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import io.github.satxm.mcwifipnp.Config;
import io.github.satxm.mcwifipnp.UUIDFixer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class UUIDFixerCommand {
	private final static Component POLICY_ONLINE = Component.translatable("mcwifipnp.commands.uuidfixer.policy.online");
	private final static Component POLICY_OFFLINE = Component.translatable("mcwifipnp.commands.uuidfixer.policy.offline");
	private final static Component POLICY_INVALID = Component.translatable("mcwifipnp.commands.uuidfixer.policy.invalid");

	private static Component policyToComponent(String policy) {
		if (UUIDFixer.PolicyHolder.isUUID(policy)) {
			return Component.literal(policy);
		} else if (UUIDFixer.PolicyHolder.isOfflinePolicy(policy)) {
			return POLICY_OFFLINE;
		} else if (UUIDFixer.PolicyHolder.isOnlinePolicy(policy)) {
			return POLICY_ONLINE;
		} else {
			return POLICY_INVALID;
		}
	}

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {

		LiteralArgumentBuilder<CommandSourceStack> cmdBuilder = Commands.literal("uuidfixer")
			.requires((cmdStack) -> cmdStack.hasPermission(3));

		cmdBuilder = cmdBuilder.then(Commands.literal("list").executes((commandContext) -> {
			return showList((CommandSourceStack) commandContext.getSource());
		}));

		RequiredArgumentBuilder<CommandSourceStack, String> anyKnownPlayerNameArg =
			Commands.argument("playerName", StringArgumentType.string())
			.suggests((commandContext, suggestionsBuilder) -> {
				MinecraftServer server = ((CommandSourceStack) commandContext.getSource()).getServer();
				Set<String> players = server.getPlayerList().getPlayers().stream()
					.map(player -> player.getGameProfile().getName()).collect(Collectors.toSet());

				Set<String> hints = new LinkedHashSet<>();

				UUIDFixer.PolicyHolder policyHolder = new UUIDFixer.PolicyHolder();
				policyHolder.getUsers().stream().filter(playerName -> !players.contains(playerName)).forEach(hints::add);
				hints.addAll(players);

				return SharedSuggestionProvider.suggest(hints, suggestionsBuilder);
			});

		RequiredArgumentBuilder<CommandSourceStack, String> modeArg =
			Commands.argument("mode", StringArgumentType.string())
			.suggests((commandContext, suggestionsBuilder) -> {
				return SharedSuggestionProvider.suggest(List.of("online", "offline", "<UUID>"), suggestionsBuilder);
			});

		cmdBuilder = cmdBuilder.then(Commands.literal("force").then(
			modeArg.then(anyKnownPlayerNameArg.executes(
				(commandContext) -> {
					return setPolicy((CommandSourceStack) commandContext.getSource(),
						StringArgumentType.getString(commandContext, "playerName"),
						StringArgumentType.getString(commandContext, "mode"));
				}
			))
		));

		cmdBuilder = cmdBuilder.then(Commands.literal("default-online")
			.then(Commands.argument("mode", BoolArgumentType.bool())
				.executes(commandContext -> {
					return setDefaultPolicy(commandContext.getSource(), BoolArgumentType.getBool(commandContext, "mode"));
				})
			)
			.executes(commandContext -> {
					return showDefaultPolicy(commandContext.getSource());
			})
		);

		RequiredArgumentBuilder<CommandSourceStack, String> playerNameArg =
			Commands.argument("playerName", StringArgumentType.string())
			.suggests((commandContext, suggestionsBuilder) -> {
				UUIDFixer.PolicyHolder policyHolder = new UUIDFixer.PolicyHolder();
				return SharedSuggestionProvider.suggest(policyHolder.getUsers(), suggestionsBuilder);
			});

		cmdBuilder = cmdBuilder.then(Commands.literal("remove").then(playerNameArg.executes(
			(commandContext) -> {
				return removePolicy((CommandSourceStack) commandContext.getSource(),
					StringArgumentType.getString(commandContext, "playerName"));
			}
		)));


		cmdBuilder = cmdBuilder.then(Commands.literal("test").then(anyKnownPlayerNameArg.executes(
			(commandContext) -> {
				return testPolicy((CommandSourceStack) commandContext.getSource(),
					StringArgumentType.getString(commandContext, "playerName"));
			}
		)));

		cmdBuilder = cmdBuilder.then(Commands.literal("enabled").then(
			Commands.argument("enabled", BoolArgumentType.bool()).executes(commandContext -> {
				return setEnabled(commandContext.getSource(), BoolArgumentType.getBool(commandContext, "enabled"));
			})
		).executes(commandContext -> {
			return showEnabled(commandContext.getSource());
		}));

		commandDispatcher.register(cmdBuilder);
	}

	private static int setEnabled(CommandSourceStack commandSourceStack, boolean enabled) {
		MinecraftServer server = commandSourceStack.getServer();
		Config cfg = Config.readFromPublishedServer(server);
		cfg.enableUUIDFixer = enabled;
		cfg.saveAndApply(server);

		return showEnabled(commandSourceStack);
	}

	private static int showEnabled(CommandSourceStack commandSourceStack) {
		MinecraftServer server = commandSourceStack.getServer();
		Config cfg = Config.readFromPublishedServer(server);

		Component status = Component.translatable("mcwifipnp.commands.uuidfixer.name")
			.append(": ").append(cfg.enableUUIDFixer ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
		commandSourceStack.sendSuccess(()->status, false);
		return 1;
	}

	private static int setPolicy(CommandSourceStack commandSourceStack, String playerName,
		String policy) throws CommandSyntaxException {

		UUIDFixer.PolicyHolder policyHolder = new UUIDFixer.PolicyHolder();

		if (!UUIDFixer.PolicyHolder.isValidPolicy(policy)) {
			throw new SimpleCommandExceptionType(
				Component.translatable("mcwifipnp.commands.uuidfixer.set.bad-policy", policy)).create();
		}
		policyHolder.set(playerName, policy);

		if (UUIDFixer.PolicyHolder.isUUID(policy)) {
			commandSourceStack.sendSuccess(() -> {
				return Component.translatable("mcwifipnp.commands.uuidfixer.set.override",
					playerName, policy);
			}, true);
		} else {
			commandSourceStack.sendSuccess(() -> {
				return Component.translatable("mcwifipnp.commands.uuidfixer.set.success",
					playerName, policyToComponent(policy));
			}, true);
		}

		policyHolder.save();
		return 1;
	}

	private static int removePolicy(CommandSourceStack commandSourceStack, String playerName)
		throws CommandSyntaxException {

		UUIDFixer.PolicyHolder policyHolder = new UUIDFixer.PolicyHolder();

		if (policyHolder.getOrNull(playerName) == null) {
			throw new SimpleCommandExceptionType(
				Component.translatable("mcwifipnp.commands.uuidfixer.policy.not-exist", playerName)).create();
		} else {
			policyHolder.remove(playerName);
			commandSourceStack.sendSuccess(() -> {
				return Component.translatable("mcwifipnp.commands.uuidfixer.remove.success", playerName);
			}, true);

			policyHolder.save();
			commandSourceStack.getServer().kickUnlistedPlayers(commandSourceStack);
			return 1;
		}
	}

	private static int setDefaultPolicy(CommandSourceStack commandSourceStack, final boolean defaultIsOnline) {
		UUIDFixer.PolicyHolder policyHolder = new UUIDFixer.PolicyHolder();
		policyHolder.setDefaultPolicy(defaultIsOnline);
		policyHolder.save();

		commandSourceStack.sendSuccess(
			() -> Component.translatable("mcwifipnp.commands.uuidfixer.default.set",
				defaultIsOnline ? POLICY_ONLINE : POLICY_OFFLINE), false);

		return 1;
	}

	private static Supplier<Component> showDefaultPolicy(UUIDFixer.PolicyHolder policyHolder) {
		final boolean defaultIsOnline = policyHolder.isOnlineByDefault();
		return () -> Component.translatable("mcwifipnp.commands.uuidfixer.default.show",
			defaultIsOnline ? POLICY_ONLINE : POLICY_OFFLINE);
	}

	private static int showDefaultPolicy(CommandSourceStack commandSourceStack) {
		UUIDFixer.PolicyHolder policyHolder = new UUIDFixer.PolicyHolder();
		commandSourceStack.sendSuccess(showDefaultPolicy(policyHolder), false);
		return 1;
	}

	private static Component showPolicy(UUIDFixer.PolicyHolder policyHolder, String playerName, boolean simulate) {
		MutableComponent policyStr = Component.empty();

		policyStr.append(playerName + ": ");
		String policy = policyHolder.getOrNull(playerName);
		if (simulate && policy == null) {
			policyStr.append(policyHolder.isOnlineByDefault() ? POLICY_ONLINE : POLICY_OFFLINE);
		} else {
			policyStr.append(policyToComponent(policy));
		}

		return policyStr;
	}

	private static int showList(CommandSourceStack commandSourceStack) {
		UUIDFixer.PolicyHolder policyHolder = new UUIDFixer.PolicyHolder();
		int count = policyHolder.count();

		final MutableComponent list = (MutableComponent) showDefaultPolicy(policyHolder).get();
		if (count == 0) {
			list.append("\n");
			list.append(Component.translatable("mcwifipnp.commands.uuidfixer.none"));
		} else {
			list.append("\n");
			list.append(Component.translatable("mcwifipnp.commands.uuidfixer.list", policyHolder.count()));
			for (String playerName: policyHolder.getUsers()) {
				list.append("\n");
				list.append(showPolicy(policyHolder, playerName, false));
			}
		}

		commandSourceStack.sendSuccess(() -> list, false);

		return count;
	}

	private static int testPolicy(CommandSourceStack commandSourceStack, String playerName) {
		UUIDFixer.PolicyHolder policyHolder = new UUIDFixer.PolicyHolder();
		final Component component = showPolicy(policyHolder, playerName, true);
		commandSourceStack.sendSuccess(() -> component, false);
		return 1;
	}
}
