package io.github.satxm.mcwifipnp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
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
import net.minecraft.network.chat.MutableComponent;
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
				if (UPnP.isUPnPAvailable()) {
					if (UPnP.isMappedTCP(cfg.port)) {
						client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.failed.mapped", cfg.port));
					} else if (UPnP.openPortTCP(cfg.port, cfg.motd)) {
						client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.success", cfg.port));
						LOGGER.info("Started forwarded port " + cfg.port + ".");
					} else {
						client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.failed", cfg.port));
					}
				} else {
					client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.failed.disabled", cfg.port));
				}
			}

			if (cfg.CopyToClipboard) {
				ArrayList<Component> IPComponentList = new ArrayList<Component>();
				Boolean NoneIPv4 = false;
				Boolean NoneIPv6 = false;
				if (GetIP.IPv4AddressList().size() > 0 || GetIP.GetGlobalIPv4() != null
						|| UPnP.getExternalIP() != null) {
					for (int i = 0; i < GetIP.IPv4AddressList().size(); i++) {
						String IP = GetIP.IPv4AddressList().get(i) + ":" + cfg.port;
						IPComponentList.add(IPComponent("IPv4", IP));
					}
					if (GetIP.GetGlobalIPv4() != null & !GetIP.IPv4AddressList().contains(GetIP.GetGlobalIPv4())) {
						String IP = GetIP.GetGlobalIPv4() + ":" + cfg.port;
						IPComponentList.add(IPComponent("IPv4", IP));
					}
					if (UPnP.getExternalIP() != null & !GetIP.IPv4AddressList().contains(UPnP.getExternalIP())) {
						String IP = UPnP.getExternalIP() + ":" + cfg.port;
						IPComponentList.add(IPComponent("IPv4", IP));
					}
				} else {
					NoneIPv4 = true;
				}
				if (GetIP.IPv6AddressList().size() > 0 || GetIP.GetGlobalIPv6() != null) {
					for (int i = 0; i < GetIP.IPv6AddressList().size(); i++) {
						String IP = "[" + GetIP.IPv6AddressList().get(i) + "]:" + cfg.port;
						IPComponentList.add(IPComponent("IPv6", IP));
					}
					if (GetIP.GetGlobalIPv6() != null & !GetIP.IPv6AddressList().contains(GetIP.GetGlobalIPv6())) {
						String IP = "[" + GetIP.GetGlobalIPv6() + "]:" + cfg.port;
						IPComponentList.add(IPComponent("IPv6", IP));
					}
				} else {
					NoneIPv6 = true;
				}
				if (NoneIPv4 == true && NoneIPv6 == true) {
					client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.success.cantgetip"));
				} else {
					MutableComponent component = null;
					for (int i = 0; i < IPComponentList.size(); i++) {
						if (component == null) {
							component = IPComponentList.get(i).copy();
						} else {
							component.append(IPComponentList.get(i));
						}
					}
					client.gui.getChat().addMessage(
							new TranslatableComponent("mcwifipnp.upnp.success.clipboard", new Object[] { component }));
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
		public String motd = "A Minecraft LAN World";
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

	private static Component IPComponent(String Type, String IP) {
		return ComponentUtils.wrapInSquareBrackets((new TextComponent(Type)).withStyle((style) -> {
			return style.withColor(ChatFormatting.GREEN)
					.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, IP))
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							new TranslatableComponent("chat.copy.click").append("\n").append(IP)))
					.withInsertion(IP);
		}));
	}
}
