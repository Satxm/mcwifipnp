package io.github.satxm.mcwifipnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(MCWiFiPnP.MODID)
public class MCWiFiPnP {
	public static final String MODID = "mcwifipnp";

	public MCWiFiPnP(IEventBus modBus) {
		modBus.register(this);
		modBus.addListener(this::afterScreenInit);
		modBus.addListener(this::onRegisterCommands);
	}

	public void afterScreenInit(final ScreenEvent.Init.Post event) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = event.getScreen();
		if (screen instanceof ShareToLanScreen) {
			client.setScreen(new ShareToLanScreenNew(screen));
		}
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
    }

	@SubscribeEvent
	public void onServerStopping(ServerStoppingEvent event) {
		MCWiFiPnPUnit.CloseUPnPPort(event.getServer());
	}

}
