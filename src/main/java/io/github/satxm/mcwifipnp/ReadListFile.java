package io.github.satxm.mcwifipnp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.GsonHelper;

public class ReadListFile {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static void readListFiles(MinecraftServer server) {
		PlayerList playerList = server.getPlayerList();

		loadList(PlayerList.OPLIST_FILE, jsonArray -> {
			for (JsonElement element : jsonArray) {
				JsonObject obj = GsonHelper.convertToJsonObject(element, "entry");
				ServerOpListEntry entry = new ServerOpListEntry(obj);
				if (entry.getUser() != null) {
					playerList.getOps().add(entry);
				}
			}
		});

		loadList(PlayerList.WHITELIST_FILE, jsonArray -> {
			for (JsonElement element : jsonArray) {
				JsonObject obj = GsonHelper.convertToJsonObject(element, "entry");
				UserWhiteListEntry entry = new UserWhiteListEntry(obj);
				if (entry.getUser() != null) {
					playerList.getWhiteList().add(entry);
				}
			}
		});

		loadList(PlayerList.IPBANLIST_FILE, jsonArray -> {
			for (JsonElement element : jsonArray) {
				JsonObject obj = GsonHelper.convertToJsonObject(element, "entry");
				IpBanListEntry entry = new IpBanListEntry(obj);
				if (entry.getUser() != null) {
					playerList.getIpBans().add(entry);
				}
			}
		});

		loadList(PlayerList.USERBANLIST_FILE, jsonArray -> {
			for (JsonElement element : jsonArray) {
				JsonObject obj = GsonHelper.convertToJsonObject(element, "entry");
				UserBanListEntry entry = new UserBanListEntry(obj);
				if (entry.getUser() != null) {
					playerList.getBans().add(entry);
				}
			}
		});
	}

	private static void loadList(File file, java.util.function.Consumer<JsonArray> processor) {
		if (!file.exists()) {
			return;
		}

		try (BufferedReader reader = Files.newReader(file, StandardCharsets.UTF_8)) {
			JsonArray jsonArray = GSON.fromJson(reader, JsonArray.class);
			if (jsonArray != null) {
				processor.accept(jsonArray);
			}
		} catch (IOException | JsonParseException e) {
			e.printStackTrace();
		}
	}
}
