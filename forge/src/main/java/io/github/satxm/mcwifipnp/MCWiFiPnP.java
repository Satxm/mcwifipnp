package io.github.satxm.mcwifipnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

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
		Button ShareToLanNew = new Button(client.screen.width / 2 + 4, client.screen.height / 4 + 96 + -16, 98, 20, new TranslationTextComponent("menu.shareToLan"), (button) -> {
			client.setScreen(new ShareToLanScreen(gui));
		});
		ShareToLanNew.active = client.hasSingleplayerServer() && !client.getSingleplayerServer().isPublished();

		if (gui instanceof IngameMenuScreen) {
			if(event.getWidgetList().size() == 8) {
			event.removeWidget((Button) event.getWidgetList().get(6));
			event.addWidget(ShareToLanNew);
			event.addWidget((Button) event.getWidgetList().get(7));
			}
        }
	}

	@SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
		MCWiFiPnPUnit.serverStarting(event.getServer());
	}

	@SubscribeEvent
	public void onServerStopping(FMLServerStoppingEvent event){
		MCWiFiPnPUnit.serverStopping(event.getServer());
	}
}
