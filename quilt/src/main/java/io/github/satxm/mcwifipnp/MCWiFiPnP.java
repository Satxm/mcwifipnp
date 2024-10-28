package io.github.satxm.mcwifipnp;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.WhitelistCommand;

public class MCWiFiPnP implements ModInitializer {
  public static final String MODID = "mcwifipnp";

  @Override
  public void onInitialize(ModContainer mod) {
    ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerLoad);
    ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      DeOpCommands.register(dispatcher);
      OpCommand.register(dispatcher);
      WhitelistCommand.register(dispatcher);
      BanIpCommands.register(dispatcher);
      BanListCommands.register(dispatcher);
      BanPlayerCommands.register(dispatcher);
      PardonCommand.register(dispatcher);
      PardonIpCommand.register(dispatcher);
      ForceOfflineCommand.register(dispatcher);
    });
  }

  private void onServerLoad(MinecraftServer server) {
    MCWiFiPnPUnit.ReadingConfig(server);
  }

  private void onServerStop(MinecraftServer server) {
    MCWiFiPnPUnit.CloseUPnPPort(server);
  }

}
