package io.github.satxm.mcwifipnp;

import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;

public class MCWiFiPnP implements ModInitializer {
	public static final String MODID = "mcwifipnp";
	
	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerLoad);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
		ScreenEvents.AFTER_INIT.register(MCWiFiPnP::afterScreenInit);
	}

	public static void afterScreenInit(Minecraft client, Screen screen, int i, int j) {
		if (screen instanceof PauseScreen) {
			final List<AbstractWidget> buttons = Screens.getButtons(screen);
			for (int k = 0;  k < buttons.size() ; k++ ){
				AbstractWidget ShareToLanOld = buttons.get(k);
				if (buttons.size() != 0 && ShareToLanOld.getMessage().getString().equals(new TranslatableComponent("menu.shareToLan").getString())) {
					int x = ShareToLanOld.x;
					int y = ShareToLanOld.y;
					int w = ShareToLanOld.getWidth();
					int h = ShareToLanOld.getHeight();
					AbstractWidget ShareToLanNew = new Button(x, y, w, h, new TranslatableComponent("menu.shareToLan"), (button) -> client.setScreen(new ShareToLanScreen(screen)));
					ShareToLanNew.active = ShareToLanOld.active;
					buttons.remove(ShareToLanOld);
					buttons.add(ShareToLanNew);
				}
			}
		}
	}

	private void onServerLoad(MinecraftServer server) {
		MCWiFiPnPUnit.serverSatrting(server);
	}

	private void onServerStop(MinecraftServer server) {
		MCWiFiPnPUnit.serverStopping(server);
	}
}
