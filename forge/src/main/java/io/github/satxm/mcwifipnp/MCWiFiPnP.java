package io.github.satxm.mcwifipnp;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(MCWiFiPnPUnit.MODID)
public class MCWiFiPnP {
  public MCWiFiPnP() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {
    MCWiFiPnPUnit.ReadingConfig(event.getServer());
  }

  @SubscribeEvent
  public void onRegisterCommands(RegisterCommandsEvent event) {
    MCWiFiPnPUnit.registerCommands(event.getDispatcher());
  }

  @SubscribeEvent
  public void onServerStopping(ServerStoppingEvent event) {
    MCWiFiPnPUnit.CloseUPnPPort(event.getServer());
  }
}
