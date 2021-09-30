package io.github.satxm.mcwifipnp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
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

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

public class MCWiFiPnP implements ModInitializer {
	public static final String MODID = "mcwifipnp";
	private static final Map<MinecraftServer, Config> configMap = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Gson gson = new GsonBuilder().create();
	private static final Logger LOGGER = LogManager.getLogger(MCWiFiPnP.class);

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerLoad);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
		ScreenEvents.AFTER_INIT.register(MCWiFiPnP::afterScreenInit);
	}

	public static void afterScreenInit(Minecraft client, Screen screen, int i, int j) {
		if (screen instanceof PauseScreen) {
			final List<AbstractWidget> buttons = Screens.getButtons(screen);
			for (int k = 0; k < buttons.size(); k++) {
				AbstractWidget ShareToLanOld = buttons.get(k);
				if (buttons.size() != 0 && ShareToLanOld.getMessage().getString()
						.equals(new TranslatableComponent("menu.shareToLan").getString())) {
					int x = ShareToLanOld.x;
					int y = ShareToLanOld.y;
					int w = ShareToLanOld.getWidth();
					int h = ShareToLanOld.getHeight();
					AbstractWidget ShareToLanNew = new Button(x, y, w, h, new TranslatableComponent("menu.shareToLan"),
							(button) -> client.setScreen(new ShareToLanScreen(screen)));
					ShareToLanNew.active = ShareToLanOld.active;
					buttons.remove(ShareToLanOld);
					buttons.add(ShareToLanNew);
				}
			}
		}
	}

	private void onServerLoad(MinecraftServer server) {
		Path location = server.getWorldPath(LevelResource.ROOT).resolve("mcwifipnp.json");
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

	private void onServerStop(MinecraftServer server) {
		Config cfg = configMap.get(server);
		if (server.isPublished() && cfg.UseUPnP) {
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
		server.getStatus().setDescription(new TextComponent(cfg.motd));
		server.publishServer(GameType.byName(cfg.GameMode), cfg.AllowCommands, cfg.port);
		server.setUsesAuthentication(cfg.OnlineMode);
		server.setPvpAllowed(cfg.EnablePvP);
		client.gui.getChat().addMessage(new TranslatableComponent("commands.publish.started", cfg.port));
		new Thread(() -> {
			if (cfg.UseUPnP) {
				UPnPUtil.UPnPResult result = UPnPUtil.init(cfg.port, "Minecraft LAN Server");
				switch (result) {
					case SUCCESS:
						client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.success", cfg.port));
						LOGGER.info("Started forwarded port " + cfg.port + ".");
						break;
					case FAILED_GENERIC:
						client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.failed", cfg.port));
						break;
					case FAILED_MAPPED:
						client.gui.getChat()
								.addMessage(new TranslatableComponent("mcwifipnp.upnp.failed.mapped", cfg.port));
						break;
					case FAILED_DISABLED:
						client.gui.getChat()
								.addMessage(new TranslatableComponent("mcwifipnp.upnp.failed.disabled", cfg.port));
						break;
				}
			}
			if (cfg.CopyToClipboard) {
				if (IPv4() != null || IPv6() != null) {
					if (IPv4() != null) {
						String ipv4 = IPv4() + ":" + cfg.port;
						Component component = ComponentUtils
								.wrapInSquareBrackets((new TextComponent("IPv4")).withStyle((style) -> {
									return style.withColor(ChatFormatting.GREEN)
											.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ipv4))
											.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
													new TranslatableComponent("chat.copy.click")))
											.withInsertion(ipv4);
								}));
						client.gui.getChat()
								.addMessage(new TranslatableComponent("mcwifipnp.upnp.success.clipboard", component));
					}
					if (IPv6() != null) {
						String ipv6 = "[" + IPv6() + "]:" + cfg.port;
						Component component = ComponentUtils
								.wrapInSquareBrackets((new TextComponent("IPv6")).withStyle((style) -> {
									return style.withColor(ChatFormatting.GREEN)
											.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ipv6))
											.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
													new TranslatableComponent("chat.copy.click")))
											.withInsertion(ipv6);
								}));
						client.gui.getChat()
								.addMessage(new TranslatableComponent("mcwifipnp.upnp.success.clipboard", component));
					}
				} else {
					client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.success.cantgetip"));
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
		public int port = HttpUtil.getAvailablePort();
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
