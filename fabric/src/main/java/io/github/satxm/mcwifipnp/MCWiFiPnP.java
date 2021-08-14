package io.github.satxm.mcwifipnp;

import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.NetworkUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.GameMode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

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

		server.openToLan(GameMode.byName(cfg.GameMode), cfg.AllowCommands, cfg.port);
		client.getServer().setOnlineMode(cfg.OnlineMode);
		client.inGameHud.getChatHud().addMessage(new TranslatableText("commands.publish.started", cfg.port));
		client.inGameHud.getChatHud().addMessage(new TranslatableText("mcwifipnp.upnp.allowcommands." + cfg.AllowCommands));
		client.inGameHud.getChatHud().addMessage(new TranslatableText("mcwifipnp.upnp.onlinemode." + cfg.OnlineMode));

		new Thread(() -> {

			if (cfg.UseUPnP) {
				UPnPUtil.UPnPResult result = UPnPUtil.init(cfg.port, "Minecraft LAN World");
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
				String ip = UPnP.getExternalIP();
				if (ip == null) {
					client.inGameHud.getChatHud().addMessage(new TranslatableText("mcwifipnp.upnp.success.cantgetip"));
				} else {
					if (ip.equals("0.0.0.0")) {
						client.inGameHud.getChatHud()
								.addMessage(new TranslatableText("mcwifipnp.upnp.success.cantgetip"));
					} else {
						client.keyboard.setClipboard(ip + ":" + cfg.port);
						client.inGameHud.getChatHud().addMessage(
								new TranslatableText("mcwifipnp.upnp.success.clipboard", ip + ":" + cfg.port));
					}
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
			LOGGER.info("Stopped forwarded port " + cfg.port +".");
		}
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
		public int port = NetworkUtils.findLocalPort();
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
