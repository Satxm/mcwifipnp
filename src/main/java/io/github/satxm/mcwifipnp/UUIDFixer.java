package io.github.satxm.mcwifipnp;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class UUIDFixer {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type TYPE_STRING_MAP = new TypeToken<Map<String, String>>() {}.getType();
	private static final String UUID_MAP_FILE = "user_uuid.json";
	private static final String POLICY_ONLINE = "online";
	private static final String POLICY_OFFLINE = "offline";

	@Nullable
	public static final JsonObject readUUIDMap(String filePath) {
		try (FileReader reader = new FileReader(filePath)) {
			return new JsonParser().parse(reader).getAsJsonObject();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * @param parent the root Json node
	 * @param key a case-insensitive key, can be * or the player name
	 * @return policy string, uuid string, or null if not specified
	 */
	public static String getPolicyOrNull(JsonObject parent, String key) {
		for (Map.Entry<String, JsonElement> entry : parent.entrySet()) {
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

	/**
	 * Mixin/ Coremod callback
	 * Return non-null to override, null to use vanilla offline UUID.
	 */
	public static UUID hookEntry(String playerName) {
		JsonObject uuidMap = readUUIDMap(UUID_MAP_FILE);
		if (null == uuidMap)
			return null;

		String policy = getPolicyOrNull(uuidMap, playerName);

		if (policy == null) {
			if (POLICY_OFFLINE.equalsIgnoreCase(getPolicyOrNull(uuidMap, "*"))) {
				policy = POLICY_OFFLINE;
			} else {
				policy = POLICY_ONLINE;
			}
		}

		System.out.println("Policy of " + playerName + " is: " + policy);

		if (POLICY_OFFLINE.equalsIgnoreCase(policy))
			return null;

		try {
			// Use the specified UUID
			return UUID.fromString(policy);
		} catch (IllegalArgumentException e) {
			return getOfficialUUID(playerName);
		}
	}

	@Nullable
	public static UUID getOfficialUUID(String playerName) {
		String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
		try {
			String UUIDJson = IOUtils.toString(URI.create(url), Charset.defaultCharset());
			if (!UUIDJson.isEmpty()) {
				JsonObject root = new JsonParser().parse(UUIDJson).getAsJsonObject();
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