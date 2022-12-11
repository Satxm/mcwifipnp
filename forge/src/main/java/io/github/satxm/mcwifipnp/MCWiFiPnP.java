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
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmlserverevents.FMLServerStartingEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppingEvent;

@Mod(MCWiFiPnP.MODID)
public class MCWiFiPnP {
	public static final String MODID = "mcwifipnp";

	public MCWiFiPnP() {
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.addListener(this::ChangeButton);
	}

	@SubscribeEvent
	public void ChangeButton(GuiScreenEvent.InitGuiEvent.Post event) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = event.getGui();
		if (screen instanceof ShareToLanScreen){
			client.setScreen(new ShareToLanScreenNew(screen));
		}
	}

	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event) {
		MCWiFiPnPUnit.ReadingConfig(event.getServer());
		DeOpCommands.register(event.getServer().getCommands().getDispatcher());
		OpCommand.register(event.getServer().getCommands().getDispatcher());
		WhitelistCommand.register(event.getServer().getCommands().getDispatcher());
		BanIpCommands.register(event.getServer().getCommands().getDispatcher());
		BanListCommands.register(event.getServer().getCommands().getDispatcher());
		BanPlayerCommands.register(event.getServer().getCommands().getDispatcher());
	}

	@SubscribeEvent
	public void onServerStopping(FMLServerStoppingEvent event) {
		MCWiFiPnPUnit.CloseUPnPPort(event.getServer());
	}

}
