package io.github.satxm.mcwifipnp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.github.satxm.mcwifipnp.Config;
import io.github.satxm.mcwifipnp.network.UPnPModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class UPnPCommand {
	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> cmdBuilder = Commands.literal("upnp")
			.requires((cmdStack) -> cmdStack.hasPermission(3));

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

		if (cfg.useUPnP ^ enabled) {
			cfg.useUPnP = enabled;
			cfg.saveAndApply(server);

			if (enabled) {
				if (server.isPublished())
					UPnPModule.startIfEnabled(server, cfg);
			} else {
				UPnPModule.stop(server);
			}
		}

		return showEnabled(commandSourceStack);
	}

	private static int showEnabled(CommandSourceStack commandSourceStack) {
		MinecraftServer server = commandSourceStack.getServer();
		Config cfg = Config.readFromPublishedServer(server);

		Component status = Component.literal("UPnP: ")
			.append(cfg.useUPnP ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
		commandSourceStack.sendSuccess(()->status, false);
		return 1;
	}
}
