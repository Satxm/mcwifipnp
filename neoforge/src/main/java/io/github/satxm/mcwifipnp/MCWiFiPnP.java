package io.github.satxm.mcwifipnp;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(MCWiFiPnPUnit.MODID)
public class MCWiFiPnP {
  public MCWiFiPnP(IEventBus modEventBus) {
    NeoForge.EVENT_BUS.register(this);
  }

  @SubscribeEvent
  public void onRegisterCommands(RegisterCommandsEvent event) {
    MCWiFiPnPUnit.registerCommands(event.getDispatcher());
  }

  @SubscribeEvent
  public void onServerStopping(ServerStoppingEvent event) {
    UPnPModule.stop(event.getServer());
  }
}
