package io.github.satxm.mcwifipnp;

import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(MCWiFiPnP.MODID)
public class MCWiFiPnP {
  public static final String MODID = "mcwifipnp";

  public MCWiFiPnP(IEventBus modEventBus) {
    NeoForge.EVENT_BUS.register(this);
    NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
  }

  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {
    MCWiFiPnPUnit.ReadingConfig(event.getServer());
  }

  @SubscribeEvent
  public void onRegisterCommands(RegisterCommandsEvent event) {
    DeOpCommands.register(event.getDispatcher());
    OpCommand.register(event.getDispatcher());
    WhitelistCommand.register(event.getDispatcher());
    BanIpCommands.register(event.getDispatcher());
    BanListCommands.register(event.getDispatcher());
    BanPlayerCommands.register(event.getDispatcher());
    PardonCommand.register(event.getDispatcher());
    PardonIpCommand.register(event.getDispatcher());
    ForceOfflineCommand.register(event.getDispatcher());
  }

  @SubscribeEvent
  public void onServerStopping(ServerStoppingEvent event) {
    MCWiFiPnPUnit.CloseUPnPPort(event.getServer());
  }

}
