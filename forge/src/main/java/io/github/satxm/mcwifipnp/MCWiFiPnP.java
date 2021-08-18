package io.github.satxm.mcwifipnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
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
		Screen gui = event.getGui();
		if (gui instanceof PauseScreen) {
			Button ShareToLanNew = new Button(client.screen.width / 2 + 4, client.screen.height / 4 + 96 + -16, 98, 20, new TranslatableComponent("menu.shareToLan"), (button) -> {
				client.setScreen(new ShareToLanScreen(gui));
			});
			ShareToLanNew.active = client.hasSingleplayerServer() && !client.getSingleplayerServer().isPublished();
			Button ShareToLanOld = (Button) event.getWidgetList().get(6);
			Button SaveAndExit = (Button) event.getWidgetList().get(7);

			event.removeWidget(ShareToLanOld);
			event.addWidget(ShareToLanNew);
			event.addWidget(SaveAndExit);
        }
	}

	@SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
		MCWiFiPnPUnit.serverSatrting(event.getServer());
	}

	@SubscribeEvent
	public void onServerStopping(FMLServerStoppingEvent event){
		MCWiFiPnPUnit.serverStopping(event.getServer());
	}

}
