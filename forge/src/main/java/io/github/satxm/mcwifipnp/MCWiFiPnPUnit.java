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
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HTTPUtil;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.storage.FolderName;

public class MCWiFiPnPUnit {
	private static final Map<MinecraftServer, Config> configMap = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Gson gson = new GsonBuilder().create();
	private static final Logger LOGGER = LogManager.getLogger(MCWiFiPnPUnit.class);

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
				String ip = null;
				if (GetIP.GetLocalIPv6() != null && GetIP.GetGlobalIPv6() != null) {
					ip = "[" + GetIP.GetGlobalIPv6() + "]";
				} else if (UPnP.getExternalIP() != null && GetIP.GetGlobalIPv4() != null
						&& UPnP.getExternalIP().equals(GetIP.GetGlobalIPv4())) {
					ip = GetIP.GetGlobalIPv4();
				}
				if (ip == null) {
					client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.success.cantgetip"));
				} else {
					client.keyboardHandler.setClipboard(ip + ":" + cfg.port);
					client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.success.clipboard"));
				}
			}
		}, "MCWiFiPnP").start();
	}

	public static void serverStarting(MinecraftServer server) {
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

	public static void serverStopping(MinecraftServer server) {
		Config cfg = configMap.get(server);
		if (server.isPublished() && cfg.UseUPnP) {
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
		public int version = 2;
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

}
