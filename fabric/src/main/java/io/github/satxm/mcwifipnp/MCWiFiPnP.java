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

				if (IPv4() != null || IPv6() != null) {
					if (IPv4() != null) {
						String ipv4 = IPv4() + ":" + cfg.port;
						Text component = Texts.bracketed((new LiteralText("IPv4")).styled((style) -> {
							style.setColor(Formatting.GREEN)
									.setClickEvent(
											new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(ipv4)))
									.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
											new TranslatableText("chat.copy.click", new Object[0])))
									.setInsertion(String.valueOf(ipv4));
						}));
						client.inGameHud.getChatHud()
								.addMessage(new TranslatableText("mcwifipnp.upnp.success.clipboard", component));
					}
					if (IPv6() != null) {
						String ipv6 = "[" + IPv6() + "]:" + cfg.port;
						Text component = Texts.bracketed((new LiteralText("IPv6")).styled((style) -> {
							style.setColor(Formatting.GREEN)
									.setClickEvent(
											new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(ipv6)))
									.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
											new TranslatableText("chat.copy.click", new Object[0])))
									.setInsertion(String.valueOf(ipv6));
						}));
						client.inGameHud.getChatHud()
								.addMessage(new TranslatableText("mcwifipnp.upnp.success.clipboard", component));
					}
				} else {
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
		public int version = 2;
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

	private static String IPv4() {
		if (UPnP.getExternalIP() != null && GetIP.GetGlobalIPv4() != null
				&& UPnP.getExternalIP().equals(GetIP.GetGlobalIPv4())) {
			return GetIP.GetGlobalIPv4();
		} else if (GetIP.GetGlobalIPv4() != null && GetIP.GetLocalIPv4() != null
				&& GetIP.GetLocalIPv4().equals(GetIP.GetGlobalIPv4())) {
			return GetIP.GetGlobalIPv4();
		}
		return null;
	}

	private static String IPv6() {
		if (GetIP.GetGlobalIPv6() != null && GetIP.GetLocalIPv6() != null
				&& GetIP.GetLocalIPv6().equals(GetIP.GetGlobalIPv6())) {
			return GetIP.GetGlobalIPv6();
		} else
			return null;
	}
}