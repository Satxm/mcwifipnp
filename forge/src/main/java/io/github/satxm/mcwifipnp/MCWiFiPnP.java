package io.github.satxm.mcwifipnp;

import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(MCWiFiPnP.MODID)
public class MCWiFiPnP {
  public static final String MODID = "mcwifipnp";

  public MCWiFiPnP() {
    MinecraftForge.EVENT_BUS.register(this);
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
