package io.github.satxm.mcwifipnp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.WhitelistCommand;

import java.util.List;

public class MCWiFiPnP implements ModInitializer {
  public static final String MODID = "mcwifipnp";

  @Override
  public void onInitialize() {
    ServerLifecycleEvents.SERVER_STARTING.register(this::onServerLoad);
    ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
    ScreenEvents.AFTER_INIT.register(MCWiFiPnP::afterScreenInit);

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

  public static void afterScreenInit(Minecraft client, Screen screen, int i, int j) {
    if (screen instanceof PauseScreen) {
      final List<AbstractWidget> buttons = Screens.getButtons(screen);
      for (int k = 0; k < buttons.size(); k++) {
        AbstractWidget ShareToLanOld = buttons.get(k);
        if (buttons.size() != 0 && ShareToLanOld.getMessage().getString()
            .equals(Component.translatable("menu.shareToLan").getString())) {
          AbstractWidget ShareToLanNew = new Button(ShareToLanOld.x, ShareToLanOld.y, ShareToLanOld.getWidth(),
              ShareToLanOld.getHeight(), Component.translatable("menu.shareToLan"),
              (button) -> client.setScreen(new ShareToLanScreenNew(screen)));
          ShareToLanNew.active = ShareToLanOld.active;
          buttons.remove(ShareToLanOld);
          buttons.add(ShareToLanNew);
        }
      }
    }
  }

  private void onServerLoad(MinecraftServer server) {
    MCWiFiPnPUnit.ReadingConfig(server);
  }

  private void onServerStop(MinecraftServer server) {
    MCWiFiPnPUnit.CloseUPnPPort(server);
  }

}
