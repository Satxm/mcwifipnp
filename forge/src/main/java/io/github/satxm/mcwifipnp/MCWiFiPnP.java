package io.github.satxm.mcwifipnp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;

import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HTTPUtil;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod(MCWiFiPnP.MODID)
public class MCWiFiPnP {
	public static final String MODID = "mcwifipnp";
	private static final Map<MinecraftServer, Config> configMap = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Gson gson = new GsonBuilder().create();
	private static final Logger LOGGER = LogManager.getLogger(MCWiFiPnP.class);

	public MCWiFiPnP() {
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.addListener(this::ChangeButton);
	}

	@Nonnull
	public static Config getConfig(MinecraftServer server) {
		return Objects.requireNonNull(configMap.get(server), "no config for server???");
	}

	@SubscribeEvent
	public void ChangeButton(GuiScreenEvent.InitGuiEvent.Post event) {
		Minecraft client = Minecraft.getInstance();
		Screen gui = event.getGui();
		if (gui instanceof IngameMenuScreen) {
			Button ShareToLanNew = new Button(client.screen.width / 2 + 4, client.screen.height / 4 + 96 + -16, 98, 20, new TranslationTextComponent("menu.shareToLan"), (p_96321_) -> {
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
		MinecraftServer server = event.getServer();
		Path location = server.getWorldPath(FolderName.ROOT).resolve("mcwifipnp.json");

		Config cfg;

		try {
			cfg = gson.fromJson(new String(Files.readAllBytes(location)), Config.class);
			cfg.location = location;
		} catch (IOException | JsonParseException e) {
			try {
				Files.deleteIfExists(location);
			} catch (IOException ioException) {
				//
			}

			cfg = new Config();
			cfg.location = location;
			cfg.needsDefaults = true;
		}

		configMap.put(server, cfg);
	}

	@SubscribeEvent
	public void onServerStopping(FMLServerStoppingEvent event){
		MinecraftServer server = event.getServer();
		Config cfg = configMap.get(server);
		if (server.isPublished() && cfg.UseUPnP) {
			UPnP.closePortTCP(cfg.port);
			LOGGER.info("Stopped forwarded port " + cfg.port +".");
		}
	}

	public static void openToLan(MinecraftServer server) {
		Minecraft client = Minecraft.getInstance();

		Config cfg = configMap.get(server);
		saveConfig(cfg);

		client.getSingleplayerServer().publishServer(GameType.byName(cfg.GameMode), cfg.AllowCommands, cfg.port);
		client.getSingleplayerServer().setUsesAuthentication(cfg.OnlineMode);
		client.gui.getChat().addMessage(new TranslationTextComponent("commands.publish.started", cfg.port));
		client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.allowcommands." + cfg.AllowCommands));
		client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.onlinemode." + cfg.OnlineMode));

		new Thread(() -> {

			if (cfg.UseUPnP) {
				UPnPUtil.UPnPResult result = UPnPUtil.init(cfg.port, "Minecraft LAN World");
				switch (result) {
					case SUCCESS:
						client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.success", cfg.port));
						LOGGER.info("Started forwarded port " + cfg.port + ".");
						break;
					case FAILED_GENERIC:
						client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.failed", cfg.port));
						break;
					case FAILED_MAPPED:
						client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.failed.mapped", cfg.port));
						break;
					case FAILED_DISABLED:
						client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.failed.disabled", cfg.port));
						break;
				}
			}

			if (cfg.CopyToClipboard) {
				String ip = UPnP.getExternalIP();
				if (ip == null || ip.equals("0.0.0.0")) {
					client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.success.cantgetip"));
				} else {
					client.keyboardHandler.setClipboard(ip + ":" + cfg.port);
					client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.success.clipboard", ip + ":" + cfg.port));
				}
			}
		},"MCWiFiPnP").start();
	}

	private static void saveConfig(Config cfg) {
		if (!cfg.needsDefaults) {
			try {
				Files.write(cfg.location, toPrettyFormat(cfg).getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			} catch (IOException e) {
				LOGGER.warn("Unable to write config file!", e);
			}
		}
	}

	public static class Config {
		public int version = 2;
		public int port = HTTPUtil.getAvailablePort();
		public String GameMode = "survival";
		public boolean UseUPnP = true;
		public boolean AllowCommands = false;
		public boolean OnlineMode = true;
		public boolean CopyToClipboard = true;
		public transient Path location;
		public transient boolean needsDefaults = false;
	}

	private static String toPrettyFormat(Object src) {
		String json = gson.toJson(src);
		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(jsonObject);
	}

}
