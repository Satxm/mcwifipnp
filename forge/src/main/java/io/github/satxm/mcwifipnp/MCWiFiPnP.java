package io.github.satxm.mcwifipnp;

import java.io.File;
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
import net.minecraft.client.resources.I18n;
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
import net.minecraft.world.dimension.DimensionType;
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
				if (ShareToLanOld.getMessage().equals(I18n.format("menu.shareToLan"))) {
					int x = ShareToLanOld.x;
					int y = ShareToLanOld.y;
					int w = ShareToLanOld.getWidth();
					int h = ShareToLanOld.getHeight();
					Button ShareToLanNew = new Button(x, y, w, h, I18n.format("menu.shareToLan"),
							(button) -> client.displayGuiScreen(new ShareToLanScreen(screen)));
					ShareToLanNew.active = ShareToLanOld.active;
					event.removeWidget(ShareToLanOld);
					event.addWidget(ShareToLanNew);
				}
			}
		}
	}

	@Nonnull
	public static Config getConfig(MinecraftServer server) {
		return Objects.requireNonNull(configMap.get(server), "no config for server???");
	}

	public static void openToLan(MinecraftServer server) {
		Minecraft client = Minecraft.getInstance();

		Config cfg = configMap.get(server);
		saveConfig(cfg);

		server.setMOTD(cfg.motd);
		server.getServerStatusResponse().setServerDescription(new StringTextComponent(cfg.motd));
		client.getIntegratedServer().shareToLAN(GameType.getByName(cfg.GameMode), cfg.AllowCommands, cfg.port);
		server.setOnlineMode(cfg.OnlineMode);
		server.setAllowPvp(cfg.EnablePvP);
		client.ingameGUI.getChatGUI()
				.printChatMessage(new TranslationTextComponent("commands.publish.started", cfg.port));

		new Thread(() -> {

			if (cfg.UseUPnP) {
				UPnPUtil.UPnPResult result = UPnPUtil.init(cfg.port, "Minecraft LAN Server");
				switch (result) {
					case SUCCESS:
						client.ingameGUI.getChatGUI()
								.printChatMessage(new TranslationTextComponent("mcwifipnp.upnp.success", cfg.port));
						LOGGER.info("Started forwarded port " + cfg.port + ".");
						break;
					case FAILED_GENERIC:
						client.ingameGUI.getChatGUI()
								.printChatMessage(new TranslationTextComponent("mcwifipnp.upnp.failed", cfg.port));
						break;
					case FAILED_MAPPED:
						client.ingameGUI.getChatGUI().printChatMessage(
								new TranslationTextComponent("mcwifipnp.upnp.failed.mapped", cfg.port));
						break;
					case FAILED_DISABLED:
						client.ingameGUI.getChatGUI().printChatMessage(
								new TranslationTextComponent("mcwifipnp.upnp.failed.disabled", cfg.port));
						break;
				}
			}

			if (cfg.CopyToClipboard) {
				if (IPv4() != null || IPv6() != null) {
					if (IPv4() != null) {
						String ipv4 = IPv4() + ":" + cfg.port;
						ITextComponent component = TextComponentUtils
								.wrapInSquareBrackets((new StringTextComponent("IPv4")).applyTextStyle((style) -> {
									style.setColor(TextFormatting.GREEN)
											.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ipv4))
											.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
													new TranslationTextComponent("chat.copy.click")))
											.setInsertion(ipv4);
								}));
						client.ingameGUI.getChatGUI().printChatMessage(
								new TranslationTextComponent("mcwifipnp.upnp.success.clipboard", component));
					}
					if (IPv6() != null) {
						String ipv6 = "[" + IPv6() + "]:" + cfg.port;
						ITextComponent component = TextComponentUtils
								.wrapInSquareBrackets((new StringTextComponent("IPv6")).applyTextStyle((style) -> {
									style.setColor(TextFormatting.GREEN)
											.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ipv6))
											.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
													new TranslationTextComponent("chat.copy.click")))
											.setInsertion(ipv6);
								}));
						client.ingameGUI.getChatGUI().printChatMessage(
								new TranslationTextComponent("mcwifipnp.upnp.success.clipboard", component));
					}
				} else {
					client.ingameGUI.getChatGUI()
							.printChatMessage(new TranslationTextComponent("mcwifipnp.upnp.success.cantgetip"));
				}
			}
		}, "MCWiFiPnP").start();
	}

	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event) {
		MinecraftServer server = event.getServer();
		File cfgfile = new File(server.getWorld(DimensionType.OVERWORLD).getSaveHandler().getWorldDirectory(),
				"mcwifipnp.json");
		Path location = cfgfile.toPath();

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
	public void onServerStopping(FMLServerStoppingEvent event) {
		MinecraftServer server = event.getServer();
		Config cfg = configMap.get(server);
		if (server.getPublic() && cfg.UseUPnP) {
			UPnP.closePortTCP(cfg.port);
			LOGGER.info("Stopped forwarded port " + cfg.port + ".");
		}
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
		public int port = HTTPUtil.getSuitableLanPort();
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

	private static String IPv4() {
		if (GetIP.IPv4AddressList().size() >= 1 || GetIP.GetGlobalIPv4() != null) {
			for (int i = 0; i < GetIP.IPv4AddressList().size(); i++) {
				if (GetIP.IPv4AddressList().get(i).getHostAddress().equals(GetIP.GetGlobalIPv4())) {
					return GetIP.IPv4AddressList().get(i).getHostAddress();
				};
			}
		} else if (UPnP.getExternalIP() != null && GetIP.GetGlobalIPv4() != null
				&& UPnP.getExternalIP().equals(GetIP.GetGlobalIPv4())) {
			return GetIP.GetGlobalIPv4();
		}
		return null;
	}

	private static String IPv6() {
		if (GetIP.IPv6AddressList().size() >= 1 || GetIP.GetGlobalIPv6() != null) {
			for (int i = 0; i < GetIP.IPv6AddressList().size(); i++) {
				if (GetIP.IPv6AddressList().get(i).getHostAddress().equals(GetIP.GetGlobalIPv6())) {
					return GetIP.IPv6AddressList().get(i).getHostAddress();
				};
			}
		}
		return null;
	}
}