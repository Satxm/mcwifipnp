package io.github.satxm.mcwifipnp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
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

	@SubscribeEvent
	public void ChangeButton(GuiScreenEvent.InitGuiEvent.Post event) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = event.getGui();
		if (screen instanceof IngameMenuScreen && event.getWidgetList().size() != 0) {
			for (int k = 0; k < event.getWidgetList().size(); k++) {
				Button ShareToLanOld = (Button) event.getWidgetList().get(k);
				if (ShareToLanOld.getMessage().getString()
						.equals(new TranslationTextComponent("menu.shareToLan").getString())) {
					int x = ShareToLanOld.x;
					int y = ShareToLanOld.y;
					int w = ShareToLanOld.getWidth();
					int h = ShareToLanOld.getHeight();
					Button ShareToLanNew = new Button(x, y, w, h, new TranslationTextComponent("menu.shareToLan"),
							(button) -> client.setScreen(new ShareToLanScreen(screen)));
					ShareToLanNew.active = ShareToLanOld.active;
					event.removeWidget(ShareToLanOld);
					event.addWidget(ShareToLanNew);
				}
			}
		}
	}

	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event) {
		Path location = event.getServer().getWorldPath(FolderName.ROOT).resolve("mcwifipnp.json");

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
		configMap.put(event.getServer(), cfg);
	}

	@SubscribeEvent
	public void onServerStopping(FMLServerStoppingEvent event) {
		Config cfg = configMap.get(event.getServer());
		if (event.getServer().isPublished() && cfg.UseUPnP) {
			UPnP.closePortTCP(cfg.port);
			LOGGER.info("Stopped forwarded port " + cfg.port + ".");
		}
	}

	public static Config getConfig(MinecraftServer server) {
		return Objects.requireNonNull(configMap.get(server), "no config for server???");
	}

	public static void openToLan(MinecraftServer server) {
		Minecraft client = Minecraft.getInstance();
		Config cfg = configMap.get(server);
		saveConfig(cfg);
		server.setMotd(cfg.motd);
		server.getStatus().setDescription(new StringTextComponent(cfg.motd));
		server.publishServer(GameType.byName(cfg.GameMode), cfg.AllowCommands, cfg.port);
		server.setUsesAuthentication(cfg.OnlineMode);
		server.setPvpAllowed(cfg.EnablePvP);
		client.gui.getChat().addMessage(new TranslationTextComponent("commands.publish.started", cfg.port));
		new Thread(() -> {
			if (cfg.UseUPnP) {
				UPnPUtil.UPnPResult result = UPnPUtil.init(cfg.port, "Minecraft LAN Server");
				switch (result) {
					case SUCCESS:
						client.gui.getChat()
								.addMessage(new TranslationTextComponent("mcwifipnp.upnp.success", cfg.port));
						LOGGER.info("Started forwarded port " + cfg.port + ".");
						break;
					case FAILED_GENERIC:
						client.gui.getChat()
								.addMessage(new TranslationTextComponent("mcwifipnp.upnp.failed", cfg.port));
						break;
					case FAILED_MAPPED:
						client.gui.getChat()
								.addMessage(new TranslationTextComponent("mcwifipnp.upnp.failed.mapped", cfg.port));
						break;
					case FAILED_DISABLED:
						client.gui.getChat()
								.addMessage(new TranslationTextComponent("mcwifipnp.upnp.failed.disabled", cfg.port));
						break;
				}
			}
			
			if (cfg.CopyToClipboard) {
				Boolean NoneIPv4 = false;
				Boolean NoneIPv6 = false;
				if (GetIP.IPv4AddressList().size() > 0 || GetIP.GetGlobalIPv4() != null
						|| UPnP.getExternalIP() != null) {
					for (int i = 0; i < GetIP.IPv4AddressList().size(); i++) {
						String IP = GetIP.IPv4AddressList().get(i) + ":" + cfg.port;
						IPComponent("IPv4", IP);
					}
					if (GetIP.GetGlobalIPv4() != null & !GetIP.IPv4AddressList().contains(GetIP.GetGlobalIPv4())) {
						String IP = GetIP.GetGlobalIPv4() + ":" + cfg.port;
						IPComponent("IPv4", IP);
					}
					if (UPnP.getExternalIP() != null & !GetIP.IPv4AddressList().contains(UPnP.getExternalIP())) {
						String IP = UPnP.getExternalIP() + ":" + cfg.port;
						IPComponent("IPv4", IP);
					}
				} else {
					NoneIPv4 = true;
				}
				if (GetIP.IPv6AddressList().size() > 0 || GetIP.GetGlobalIPv6() != null) {
					for (int i = 0; i < GetIP.IPv6AddressList().size(); i++) {
						String IP = "[" + GetIP.IPv6AddressList().get(i) + "]:" + cfg.port;
						IPComponent("IPv6", IP);
					}
					if (GetIP.GetGlobalIPv6() != null & !GetIP.IPv6AddressList().contains(GetIP.GetGlobalIPv6())) {
						String IP = "[" + GetIP.GetGlobalIPv6() + "]:" + cfg.port;
						IPComponent("IPv6", IP);
					}
				} else {
					NoneIPv6 = true;
				}
				if (NoneIPv4 == true && NoneIPv6 == true) {
					client.gui.getChat()
								.addMessage(new TranslationTextComponent("mcwifipnp.upnp.success.cantgetip"));
				}
			}
		}, "MCWiFiPnP").start();
	}

	private static void saveConfig(Config cfg) {
		if (!cfg.needsDefaults) {
			try {
				Files.write(cfg.location, toPrettyFormat(cfg).getBytes(), StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.CREATE);
			} catch (IOException e) {
				LOGGER.warn("Unable to write config file!", e);
			}
		}
	}

	public static class Config {
		public int port = HTTPUtil.getAvailablePort();
		public String GameMode = "survival";
		public String motd = "A Minecraft LAN Server";
		public boolean UseUPnP = true;
		public boolean AllowCommands = false;
		public boolean OnlineMode = true;
		public boolean EnablePvP = true;
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

	private static ITextComponent IPComponent(String Type, String IP) {
		Minecraft client = Minecraft.getInstance();
		ITextComponent component = TextComponentUtils
		.wrapInSquareBrackets((new StringTextComponent(Type)).withStyle((style) -> {
			return style.withColor(TextFormatting.GREEN)
					.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, IP))
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							new TranslationTextComponent("chat.copy.click")))
					.withInsertion(IP);
		}));
		client.gui.getChat()
								.addMessage(new TranslationTextComponent("mcwifipnp.upnp.success.clipboard", component));
		return component;
	}
}