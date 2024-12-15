package io.github.satxm.mcwifipnp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

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
  public transient Path location;
  public transient boolean needsDefaults = false;

  public static transient final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  public static transient final Logger LOGGER = MCWiFiPnPUnit.LOGGER;

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
      cfg = new Config();
      cfg.location = location;
      cfg.needsDefaults = true;
    }

    return cfg;
  }

  public void save() {
    if (!this.needsDefaults) {
      try {
        Files.write(this.location, GSON.toJson(this).getBytes("utf-8"), StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE);
      } catch (IOException e) {
        LOGGER.warn("Unable to write config file!", e);
      }
    }
  }
}
