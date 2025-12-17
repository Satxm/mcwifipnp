package io.github.satxm.mcwifipnp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.github.satxm.mcwifipnp.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class OnlineModeCommand {
	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> cmdBuilder = Commands.literal("onlinemode")
			.requires(Commands.hasPermission(Commands.LEVEL_OWNERS));

		cmdBuilder = cmdBuilder.then(
			Commands.argument("enabled", BoolArgumentType.bool()).executes(commandContext -> {
				return setEnabled(commandContext.getSource(), BoolArgumentType.getBool(commandContext, "enabled"));
			})
		).executes(commandContext -> {
			return showEnabled(commandContext.getSource());
		});

		commandDispatcher.register(cmdBuilder);
	}

	private static int setEnabled(CommandSourceStack commandSourceStack, boolean enabled) {
		MinecraftServer server = commandSourceStack.getServer();
		Config cfg = Config.readFromPublishedServer(server);
		cfg.onlineMode = enabled;
		cfg.saveAndApply(server);

		return showEnabled(commandSourceStack);
	}

	private static int showEnabled(CommandSourceStack commandSourceStack) {
		MinecraftServer server = commandSourceStack.getServer();
		Config cfg = Config.readFromPublishedServer(server);

		Component status = Component.translatable("mcwifipnp.gui.OnlineMode")
			.append(": ").append(cfg.onlineMode ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
		commandSourceStack.sendSuccess(()->status, false);
		return 1;
	}
}
