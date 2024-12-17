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
	// The initial value are the defaults

	// These fields require special handling and consideration
	public int port = 25565;
	@SerializedName(value = "enableHostCheat", alternate = {"AllowCommands"})
	public boolean enableHostCheat = false; // Host cheats

	// These fields are read, synced, and save as normal
	public int maxPlayers = 8;
	public GameType GameMode = GameType.SURVIVAL;
	public String motd = Component.translatable("lanServer.title").getString();
	public boolean AllPlayersCheats = false;
	public boolean Whitelist = false;
	public boolean UseUPnP = true;
	public boolean OnlineMode = true;
	public boolean EnableUUIDFixer = false;
	public List<String> ForceOfflinePlayers = Collections.emptyList();
	public boolean PvP = true;
	public boolean CopyToClipboard = true;

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

		this.AllPlayersCheats = playerList.isAllowCommandsForAllPlayers();
		this.GameMode = server.getDefaultGameType();

		this.maxPlayers = playerList.getMaxPlayers();
		this.OnlineMode = server.usesAuthentication();
		this.PvP = server.isPvpAllowed();
		this.Whitelist = server.isEnforceWhitelist();

		this.motd = server.getMotd();
		this.EnableUUIDFixer = UUIDFixer.EnableUUIDFixer;
		this.ForceOfflinePlayers = UUIDFixer.ForceOfflinePlayers;
	}

	public void applyTo(IntegratedServer server) {
		PlayerList playerList = server.getPlayerList();
		server.setDefaultGameType(this.GameMode);
		playerList.setAllowCommandsForAllPlayers(this.AllPlayersCheats);

		((PlayerListAccessor) playerList).setMaxPlayers(this.maxPlayers);
		server.setUsesAuthentication(this.OnlineMode);
		server.setPvpAllowed(this.PvP);
		server.setEnforceWhitelist(this.Whitelist);
		playerList.setUsingWhiteList(this.Whitelist);

		server.setMotd(this.motd);
		UUIDFixer.EnableUUIDFixer = this.EnableUUIDFixer;
		UUIDFixer.ForceOfflinePlayers = this.ForceOfflinePlayers;
	}
}
