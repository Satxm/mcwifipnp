package io.github.satxm.mcwifipnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(MCWiFiPnP.MODID)
public class MCWiFiPnP {
	public static final String MODID = "mcwifipnp";

	public MCWiFiPnP() {
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.addListener(this::ChangeButton);
	}

	@SubscribeEvent
	public void ChangeButton(ScreenEvent.InitScreenEvent.Post event) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = event.getScreen();
		if (screen instanceof PauseScreen && event.getListenersList().size() != 0) {
			for (int k = 0; k < event.getListenersList().size(); k++) {
				Button ShareToLanOld = (Button) event.getListenersList().get(k);
				if (ShareToLanOld.getMessage().getString()
						.equals(new TranslatableComponent("menu.shareToLan").getString())) {
					int x = ShareToLanOld.x;
					int y = ShareToLanOld.y;
					int w = ShareToLanOld.getWidth();
					int h = ShareToLanOld.getHeight();
					Button ShareToLanNew = new Button(x, y, w, h, new TranslatableComponent("menu.shareToLan"),
							(button) -> client.setScreen(new ShareToLanScreen(screen)));
					ShareToLanNew.active = ShareToLanOld.active;
					event.removeListener(ShareToLanOld);
					event.addListener(ShareToLanNew);
				}
			}
		}
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		MCWiFiPnPUnit.serverSatrting(event.getServer());
	}

	@SubscribeEvent
	public void onServerStopping(ServerStoppingEvent event) {
		MCWiFiPnPUnit.serverStopping(event.getServer());
	}
}
