package io.github.satxm.mcwifipnp;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;


public class OfflinePlayerCommand {
  private static final SimpleCommandExceptionType ERROR_ALREADY_IN = new SimpleCommandExceptionType(Component.translatable("mcwifipnp.commands.offlineplayers.add.failed"));
  private static final SimpleCommandExceptionType ERROR_NOT_IN = new SimpleCommandExceptionType(Component.translatable("mcwifipnp.commands.offlineplayers.remove.failed"));

  public OfflinePlayerCommand() {
  }

  public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
    commandDispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder) Commands.literal("offlineplayer").requires((commandSourceStack) -> {
      return commandSourceStack.hasPermission(3);
    })).then(Commands.literal("list").executes((commandContext) -> {
      return showList((CommandSourceStack)commandContext.getSource());
    }))).then(Commands.literal("add").then(Commands.argument("targets", GameProfileArgument.gameProfile()).suggests((commandContext, suggestionsBuilder) -> {
      MinecraftServer server = ((CommandSourceStack)commandContext.getSource()).getServer();
      PlayerList playerList = server.getPlayerList();
      MCWiFiPnPUnit.ReadingConfig(server);
      MCWiFiPnPUnit.Config cfg = MCWiFiPnPUnit.getConfig(server);
      List<String> alwaysOfflinePlayers  = cfg.alwaysOfflinePlayers;
      return SharedSuggestionProvider.suggest(playerList.getPlayers().stream().filter((serverPlayer) -> {
        return !alwaysOfflinePlayers.contains(serverPlayer.getGameProfile().getName());
      }).map((serverPlayer) -> {
        return serverPlayer.getGameProfile().getName();
      }), suggestionsBuilder);
    }).executes((commandContext) -> {
      return addPlayers((CommandSourceStack)commandContext.getSource(), GameProfileArgument.getGameProfiles(commandContext, "targets"));
    })))).then(Commands.literal("remove").then(Commands.argument("targets", GameProfileArgument.gameProfile()).suggests((commandContext, suggestionsBuilder) -> {
      MinecraftServer server = ((CommandSourceStack)commandContext.getSource()).getServer();
      PlayerList playerList = server.getPlayerList();
      MCWiFiPnPUnit.ReadingConfig(server);
      MCWiFiPnPUnit.Config cfg = MCWiFiPnPUnit.getConfig(server);
      List<String> alwaysOfflinePlayers  = cfg.alwaysOfflinePlayers;
      return SharedSuggestionProvider.suggest(alwaysOfflinePlayers.stream(), suggestionsBuilder);
    }).executes((commandContext) -> {
      return removePlayers((CommandSourceStack)commandContext.getSource(), GameProfileArgument.getGameProfiles(commandContext, "targets"));
    }))));
  }

  private static int addPlayers(CommandSourceStack commandSourceStack, Collection<GameProfile> collection) throws CommandSyntaxException {

    MinecraftServer server = commandSourceStack.getServer();
    MCWiFiPnPUnit.ReadingConfig(server);
    MCWiFiPnPUnit.Config cfg = MCWiFiPnPUnit.getConfig(server);
    List<String> alwaysOfflinePlayers  = cfg.alwaysOfflinePlayers;
    int i = 0;
    Iterator var4 = collection.iterator();

    while(var4.hasNext()) {
      GameProfile gameProfile = (GameProfile)var4.next();
      if (!alwaysOfflinePlayers.contains(gameProfile.getName())) {
        alwaysOfflinePlayers.add(gameProfile.getName());
        MCWiFiPnPUnit.saveConfig(cfg);
        commandSourceStack.sendSuccess(() -> {
          return Component.translatable("mcwifipnp.commands.offlineplayers.add.success", new Object[]{Component.literal(gameProfile.getName())});
        }, true);
        ++i;
      }
    }

    if (i == 0) {
      throw ERROR_ALREADY_IN.create();
    } else {
      return i;
    }
  }

  private static int removePlayers(CommandSourceStack commandSourceStack, Collection<GameProfile> collection) throws CommandSyntaxException {
    MinecraftServer server = commandSourceStack.getServer();
    MCWiFiPnPUnit.ReadingConfig(server);
    MCWiFiPnPUnit.Config cfg = MCWiFiPnPUnit.getConfig(server);
    List<String> alwaysOfflinePlayers  = cfg.alwaysOfflinePlayers;

    int i = 0;
    Iterator var4 = collection.iterator();

    while(var4.hasNext()) {
      GameProfile gameProfile = (GameProfile)var4.next();
      if (alwaysOfflinePlayers.contains(gameProfile.getName()))  {
        alwaysOfflinePlayers.remove(gameProfile.getName());
        MCWiFiPnPUnit.saveConfig(cfg);
        commandSourceStack.sendSuccess(() -> {
          return Component.translatable("mcwifipnp.commands.offlineplayers.remove.success", new Object[]{Component.literal(gameProfile.getName())});
        }, true);
        ++i;
      }
    }

    if (i == 0) {
      throw ERROR_NOT_IN.create();
    } else {
      commandSourceStack.getServer().kickUnlistedPlayers(commandSourceStack);
      return i;
    }
  }

  private static int showList(CommandSourceStack commandSourceStack) {
    MinecraftServer server = commandSourceStack.getServer();
    MCWiFiPnPUnit.ReadingConfig(server);
    MCWiFiPnPUnit.Config cfg = MCWiFiPnPUnit.getConfig(server);
    List<String> alwaysOfflinePlayers  = cfg.alwaysOfflinePlayers;

    if (alwaysOfflinePlayers.size() == 0) {
      commandSourceStack.sendSuccess(() -> {
        return Component.translatable("mcwifipnp.commands.offlineplayers.none");
      }, false);
    } else {
      commandSourceStack.sendSuccess(() -> {
        return Component.translatable("mcwifipnp.commands.offlineplayers.list", new Object[]{alwaysOfflinePlayers.size(), StringUtils.join(alwaysOfflinePlayers,", ")});
      }, false);
    }

    return alwaysOfflinePlayers.size();
  }
}
