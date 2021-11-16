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

import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.NetworkUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

public class MCWiFiPnP implements ModInitializer {
	public static final String MODID = "mcwifipnp";
	private static final Map<MinecraftServer, Config> configMap = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Gson gson = new GsonBuilder().create();
	private static final Logger LOGGER = LogManager.getLogger(MCWiFiPnP.class);

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerLoad);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
	}

	@NotNull
	public static Config getConfig(MinecraftServer server) {
		return Objects.requireNonNull(configMap.get(server), "no config for server???");
	}

	public static void openToLan(MinecraftServer server) {
		MinecraftClient client = MinecraftClient.getInstance();

		Config cfg = configMap.get(server);
		saveConfig(cfg);

		server.setMotd(cfg.motd);
		server.getServerMetadata().setDescription(new LiteralText(cfg.motd));
		server.openToLan(GameMode.byName(cfg.GameMode), cfg.AllowCommands, cfg.port);
		server.setOnlineMode(cfg.OnlineMode);
		server.setPvpEnabled(cfg.EnablePvP);
		client.inGameHud.getChatHud().addMessage(new TranslatableText("commands.publish.started", cfg.port));

		new Thread(() -> {

			if (cfg.UseUPnP) {
				UPnPUtil.UPnPResult result = UPnPUtil.init(cfg.port, "Minecraft LAN Server");
				switch (result) {
					case SUCCESS:
						client.inGameHud.getChatHud()
								.addMessage(new TranslatableText("mcwifipnp.upnp.success", cfg.port));
						LOGGER.info("Started forwarded port " + cfg.port + ".");
						break;
					case FAILED_GENERIC:
						client.inGameHud.getChatHud()
								.addMessage(new TranslatableText("mcwifipnp.upnp.failed", cfg.port));
						break;
					case FAILED_MAPPED:
						client.inGameHud.getChatHud()
								.addMessage(new TranslatableText("mcwifipnp.upnp.failed.mapped", cfg.port));
						break;
					case FAILED_DISABLED:
						client.inGameHud.getChatHud()
								.addMessage(new TranslatableText("mcwifipnp.upnp.failed.disabled", cfg.port));
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
					client.inGameHud.getChatHud().addMessage(new TranslatableText("mcwifipnp.upnp.success.cantgetip"));
				}
			}
		}, "MCWiFiPnP").start();
	}

	private void onServerLoad(MinecraftServer server) {
		File cfgfile = server.getLevelStorage().resolveFile(server.getLevelName(), "mcwifipnp.json");
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

	private void onServerStop(MinecraftServer server) {
		Config cfg = configMap.get(server);
		if (server.isRemote() && cfg.UseUPnP) {
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
		public int port = NetworkUtils.findLocalPort();
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

	private static Text IPComponent(String Type, String IP) {
		MinecraftClient client = MinecraftClient.getInstance();
		Text component = Texts.bracketed((new LiteralText(Type)).styled((style) -> {
			style.setColor(Formatting.GREEN)
					.setClickEvent(
							new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(IP)))
					.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							new TranslatableText("chat.copy.click", new Object[0])))
					.setInsertion(String.valueOf(IP));
		}));
		client.inGameHud.getChatHud()
				.addMessage(new TranslatableText("mcwifipnp.upnp.success.clipboard", component));
		return component;
	}
}