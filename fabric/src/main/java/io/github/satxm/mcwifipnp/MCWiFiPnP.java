package io.github.satxm.mcwifipnp;

import com.dosse.upnp.UPnP;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

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
import net.minecraft.client.Minecraft;
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
	}

	@NotNull
	public static Config getConfig(MinecraftServer server) {
		return Objects.requireNonNull(configMap.get(server), "no config for server???");
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
			LOGGER.info("Stopped forwarded port " + cfg.port +".");
		}
	}

	public static void openToLan(MinecraftServer server) {
		Minecraft client = Minecraft.getInstance();
		
		Config cfg = configMap.get(server);
		saveConfig(cfg);

		client.getSingleplayerServer().setMotd(cfg.motd);
		client.getSingleplayerServer().getStatus().setDescription(new TextComponent(cfg.motd));
		client.getSingleplayerServer().publishServer(GameType.byName(cfg.GameMode), cfg.AllowCommands, cfg.port);
		client.getSingleplayerServer().setUsesAuthentication(cfg.OnlineMode);
		client.getSingleplayerServer().setPvpAllowed(cfg.EnablePvP);
		client.gui.getChat().addMessage(new TranslatableComponent("commands.publish.started", cfg.port));
		client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.allowcommands." + cfg.AllowCommands));
		client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.onlinemode." + cfg.OnlineMode));
		client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.enablepvp." + cfg.EnablePvP));

		new Thread(() -> {

			if (cfg.UseUPnP) {
				UPnPUtil.UPnPResult result = UPnPUtil.init(cfg.port, "Minecraft LAN World");
				switch (result) {
					case SUCCESS:
						client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.success", cfg.port));
						LOGGER.info("Started forwarded port " + cfg.port + ".");
						break;
					case FAILED_GENERIC:
						client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.failed", cfg.port));
						break;
					case FAILED_MAPPED:
						client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.failed.mapped", cfg.port));
						break;
					case FAILED_DISABLED:
						client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.failed.disabled", cfg.port));
						break;
				}
			}

			if (cfg.CopyToClipboard) {
				String ip = UPnP.getExternalIP();
				if (ip == null || ip.equals("0.0.0.0")) {
					client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.success.cantgetip"));
				} else {
					client.keyboardHandler.setClipboard(ip + ":" + cfg.port);
					client.gui.getChat().addMessage(new TranslatableComponent("mcwifipnp.upnp.success.clipboard", ip + ":" + cfg.port));
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
		public int port = HttpUtil.getAvailablePort();
		public String GameMode = "survival";
		public String motd = "A New Minecraft LAN Server!";
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
