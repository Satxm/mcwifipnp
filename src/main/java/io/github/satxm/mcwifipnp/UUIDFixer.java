package io.github.satxm.mcwifipnp;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class UUIDFixer {
	private static final Logger LOGGER = LogManager.getLogger(UUIDFixer.class);
	
	public static boolean enabled = false;

	public static class PolicyHolder {
		private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
		private static final String UUID_MAP_FILE = "uuid_fixer.json";
		private static final String POLICY_ONLINE = "online";
		private static final String POLICY_OFFLINE = "offline";

		public final Path location;

		private final JsonObject rootNode;

		public PolicyHolder() {
			this(Path.of(UUID_MAP_FILE));
		}

		public PolicyHolder(Path filePath) {
			this.location = filePath;

			JsonObject rootNode = null;
			try (FileReader reader = new FileReader(filePath.toFile())) {
				rootNode = JsonParser.parseReader(reader).getAsJsonObject();
			} catch (IOException e) {
				rootNode = new JsonObject();
			}
			this.rootNode = rootNode;
		}

		public boolean isOnlineByDefault() {
			return !POLICY_OFFLINE.equalsIgnoreCase(this.getOrNull("*"));
		}

		public void setDefaultPolicy(boolean defaultIsOnline) {
			this.set("*", defaultIsOnline ? POLICY_ONLINE : POLICY_OFFLINE);
		}

		public Set<String> getUsers() {
			Set<String> users = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

			for (Map.Entry<String, JsonElement> entry : this.rootNode.entrySet()) {
				String key = entry.getKey();
				if (!"*".equals(key))
					users.add(key);
			}

			return users;
		}

		public static boolean isUUID(String policy) {
			try {
				UUID.fromString(policy);
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		public static boolean isOnlinePolicy(String policy) {
			return POLICY_ONLINE.equalsIgnoreCase(policy);
		}

		public static boolean isOfflinePolicy(String policy) {
			return POLICY_OFFLINE.equalsIgnoreCase(policy);
		}

		public static boolean isValidPolicy(String policy) {
			if (isOnlinePolicy(policy))
				return true;
			if (isOfflinePolicy(policy))
				return true;

			return isUUID(policy);
		}

		public int count() {
			int count = 0;
			for (Map.Entry<String, JsonElement> entry : this.rootNode.entrySet()) {
				try {
					String policy = entry.getValue().getAsString();
					if (isValidPolicy(policy))
						count++;
				} catch (Exception e) {}
			}
			return count;
		}

		public void set(String key, String value) {
			this.remove(key);
			this.rootNode.addProperty(key, value);
		}

		/**
		 * @param parent the root Json node
		 * @param key a case-insensitive key, can be * or the player name
		 * @return policy string, uuid string, or null if not specified
		 */
		public String getOrNull(String key) {
			for (Map.Entry<String, JsonElement> entry : this.rootNode.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(key)) {
					try {
						return entry.getValue().getAsString();
					} catch (Exception e) {
						return null;
					}
				}
			}

			return null;
		}

		public boolean remove(String playerName) {
			boolean removed = false;

			Iterator<Map.Entry<String, JsonElement>> iterator = this.rootNode.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, JsonElement> entry = iterator.next();
				if (entry.getKey().equalsIgnoreCase(playerName)) {
					iterator.remove();
					removed = true;
				}
			}

			return removed;
		}

		@Nullable
		public UUID uuidOf(String playerName) {
			String policy = this.getOrNull(playerName);

			if (policy == null) {
				policy = this.isOnlineByDefault() ? POLICY_ONLINE : POLICY_OFFLINE;
			}

			LOGGER.info("Policy of " + playerName + " is: " + policy);

			if (POLICY_OFFLINE.equalsIgnoreCase(policy))
				return null; // The policy specify a user as offline

			try {
				// Use the specified UUID as override
				return UUID.fromString(policy);
			} catch (IllegalArgumentException e) {
				return getOfficialUUID(playerName);
			}
		}

		public void save() {
			try {
				String json = GSON.toJson(this.rootNode);
				Files.write(this.location, json.getBytes("utf-8"), StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);
			} catch (IOException e) {
				LOGGER.warn("Unable to write config file!", e);
			}
		}
	}

	/**
	 * Mixin/ Coremod callback
	 * Return non-null to override, null to use vanilla offline UUID.
	 */
	public static UUID hookEntry(String playerName) {
		if (!enabled)
			return null;

		return new PolicyHolder().uuidOf(playerName);
	}

	@Nullable
	public static UUID getOfficialUUID(String playerName) {
		String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
		try {
			String UUIDJson = IOUtils.toString(URI.create(url), Charset.defaultCharset());
			if (!UUIDJson.isEmpty()) {
				JsonObject root = JsonParser.parseString(UUIDJson).getAsJsonObject();
				String playerName2 = root.getAsJsonPrimitive("name").getAsString();
				String uuidString = root.getAsJsonPrimitive("id").getAsString();
				// com.mojang.util.UUIDTypeAdapter.fromString(String)
				long uuidMSB = Long.parseLong(uuidString.substring(0, 8), 16);
				uuidMSB <<= 32;
				uuidMSB |= Long.parseLong(uuidString.substring(8, 16), 16);
				long uuidLSB = Long.parseLong(uuidString.substring(16, 24), 16);
				uuidLSB <<= 32;
				uuidLSB |= Long.parseLong(uuidString.substring(24, 32), 16);
				UUID uuid = new UUID(uuidMSB, uuidLSB);

				if (playerName2.equalsIgnoreCase(playerName))
					return uuid;
			}
		} catch (IOException | JsonSyntaxException e) {
			e.printStackTrace();
		}

		return null;
	}
}