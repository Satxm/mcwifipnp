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

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

public class Config {
  // The initial value are the defaults
  public int port = 25565;
  public int maxPlayers = 8;
  public GameType GameMode = GameType.SURVIVAL;
  public String motd = Component.translatable("lanServer.title").getString();
  public boolean AllPlayersCheats = false;
  public boolean Whitelist = false;
  public boolean UseUPnP = true;
  public boolean AllowCommands = false;
  public boolean OnlineMode = true;
  public boolean EnableUUIDFixer = false;
  public List<String> ForceOfflinePlayers = Collections.emptyList();
  public boolean PvP = true;
  public boolean CopyToClipboard = true;

  // These fields will not be serialized
  private transient Path location;
  public transient final boolean usingDefaults;

  public static transient final Logger LOGGER = MCWiFiPnPUnit.LOGGER;
  public static transient final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(GameType.class, new EnumLowerCaseAdapter<>())
      .setPrettyPrinting()
      .create();

  /*
   * This constructor exists to be used by GSON
   */
  private Config() {
    this.usingDefaults = false;
  }

  private Config(boolean usingDefaults) {
    this.usingDefaults = usingDefaults;
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
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      return Enum.valueOf((Class<T>) typeOfT, json.getAsString().toUpperCase());
    }
  }
}
