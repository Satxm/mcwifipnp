package io.github.satxm.mcwifipnp;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class MCWiFiPnP implements ModInitializer {
  @Override
  public void onInitialize(ModContainer mod) {
    ServerLifecycleEvents.SERVER_STOPPING.register(UPnPModule::stop);

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      MCWiFiPnPUnit.registerCommands(dispatcher);
    });
  }
}
