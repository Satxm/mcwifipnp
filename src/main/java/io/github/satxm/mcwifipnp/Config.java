package io.github.satxm.mcwifipnp;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

import io.github.satxm.mcwifipnp.mixin.PlayerListAccessor;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

public class Config {
	// These fields require special handling and consideration
	public int port = 25565;

	@SerializedName(value = "allow-host-cheat", alternate = {"AllowCommands"})
	public boolean allowHostCheat = false;

	// These fields are read, synced, and save as normal
	@SerializedName(value = "max-players", alternate = {"maxPlayers"})
	public int maxPlayers = 8;

	@SerializedName(value = "gamemode", alternate = {"GameMode"})
	public GameType gameType = GameType.SURVIVAL;

	public String motd = Component.translatable("lanServer.title").getString();

	@SerializedName(value = "allow-everyone-cheat", alternate = {"AllPlayersCheats"})
	public boolean allowEveryoneCheat = false;

	@SerializedName(value = "enforce-whitelist", alternate = {"Whitelist"})
	public boolean enforceWhitelist = false;

	@SerializedName(value = "enable-upnp", alternate = {"UseUPnP"})
	public boolean useUPnP = true;

	@SerializedName(value = "online-mode", alternate = {"OnlineMode"})
	public boolean onlineMode = true;

	@SerializedName(value = "enable-uuid-fixer", alternate = {"EnableUUIDFixer"})
	public boolean enableUUIDFixer = false;

	@SerializedName(value = "forced-offline-players", alternate = {"ForceOfflinePlayers"})
	public List<String> forcedOfflinePlayers = Collections.emptyList();

	@SerializedName(value = "pvp", alternate = {"PvP"})
	public boolean enablePvP = true;

	@SerializedName(value = "get-public-ip", alternate = {"CopyToClipboard"})
	public boolean getPublicIP = true;

	// These fields will not be serialized
	public transient Path location;
	public transient final boolean usingDefaults;

	public static transient final Logger LOGGER = MCWiFiPnPUnit.LOGGER;
	public static transient final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(GameType.class, new EnumLowerCaseAdapter<>()).setPrettyPrinting().create();

	/*
	 * This constructor exists to be used by GSON
	 */
	private Config() {
		this(false);
	}

	private Config(boolean usingDefaults) {
		this.usingDefaults = usingDefaults;
	}

	public static Path getConfigPath(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("mcwifipnp.json");
	}

	/**
	 * @param server the server instance used to get the root map path
	 * @return the latest config instance read from the path
	 */
	public static Config read(MinecraftServer server) {
		return read(getConfigPath(server));
	}

	public static Config read(Path location) {
		Config cfg;

		try {
			cfg = GSON.fromJson(new String(Files.readAllBytes(location), "utf-8"), Config.class);
			cfg.location = location;
		} catch (IOException | JsonParseException e) {
			try {
				Files.deleteIfExists(location);
			} catch (IOException ie) {
				LOGGER.warn("Unable to read config file!", ie);
			}
			cfg = new Config(true);
			cfg.location = location;
		}

		return cfg;
	}

	public void save() {
		try {
			Files.write(this.location, GSON.toJson(this).getBytes("utf-8"), StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);
		} catch (IOException e) {
			LOGGER.warn("Unable to write config file!", e);
		}
	}

	private static class EnumLowerCaseAdapter<T extends Enum<T>> implements JsonSerializer<T>, JsonDeserializer<T> {
		@Override
		public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.name().toLowerCase());
		}

		@SuppressWarnings("unchecked")
		@Override
		public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return Enum.valueOf((Class<T>) typeOfT, json.getAsString().toUpperCase());
		}
	}

	public void readFromRunningServer(IntegratedServer server) {
		PlayerList playerList = server.getPlayerList();

		this.port = server.getPort();

		this.allowEveryoneCheat = playerList.isAllowCommandsForAllPlayers();
		this.gameType = server.getDefaultGameType();

		this.maxPlayers = playerList.getMaxPlayers();
		this.onlineMode = server.usesAuthentication();
		this.enablePvP = server.isPvpAllowed();
		this.enforceWhitelist = server.isEnforceWhitelist();

		this.motd = server.getMotd();
		this.enableUUIDFixer = UUIDFixer.tryOnlineFirst;
		this.forcedOfflinePlayers = UUIDFixer.alwaysOfflinePlayers;
	}

	public void applyTo(IntegratedServer server) {
		PlayerList playerList = server.getPlayerList();
		server.setDefaultGameType(this.gameType);
		playerList.setAllowCommandsForAllPlayers(this.allowEveryoneCheat);

		((PlayerListAccessor) playerList).setMaxPlayers(this.maxPlayers);
		server.setUsesAuthentication(this.onlineMode);
		server.setPvpAllowed(this.enablePvP);
		server.setEnforceWhitelist(this.enforceWhitelist);
		playerList.setUsingWhiteList(this.enforceWhitelist);

		server.setMotd(this.motd);
		UUIDFixer.tryOnlineFirst = this.enableUUIDFixer;
		UUIDFixer.alwaysOfflinePlayers = this.forcedOfflinePlayers;
	}
}
