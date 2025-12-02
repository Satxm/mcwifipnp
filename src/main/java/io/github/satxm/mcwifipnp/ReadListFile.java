package io.github.satxm.mcwifipnp;

import java.io.BufferedReader;
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

	public static void ReadListFile(MinecraftServer server) {
		PlayerList playerList = server.getPlayerList();
		try {
			BufferedReader bufferedReader = Files.newReader(PlayerList.OPLIST_FILE, StandardCharsets.UTF_8);
			JsonArray jsonArray = GSON.fromJson(bufferedReader, JsonArray.class);

			for (JsonElement jsonElement : jsonArray) {
				JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "entry");
				ServerOpListEntry entry = new ServerOpListEntry(jsonObject);
				if (entry.getUser() != null) {
					playerList.getOps().add(entry);
				}
			}
		} catch (IOException | JsonParseException | NullPointerException e) {
		}
		try {
			BufferedReader bufferedReader = Files.newReader(PlayerList.WHITELIST_FILE, StandardCharsets.UTF_8);
			JsonArray jsonArray = GSON.fromJson(bufferedReader, JsonArray.class);

			for (JsonElement jsonElement : jsonArray) {
				JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "entry");
				UserWhiteListEntry entry = new UserWhiteListEntry(jsonObject);
				if (entry.getUser() != null) {
					playerList.getWhiteList().add(entry);
				}
			}
		} catch (IOException | JsonParseException | NullPointerException e) {
		}
		try {
			BufferedReader bufferedReader = Files.newReader(PlayerList.IPBANLIST_FILE, StandardCharsets.UTF_8);
			JsonArray jsonArray = GSON.fromJson(bufferedReader, JsonArray.class);

			for (JsonElement jsonElement : jsonArray) {
				JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "entry");
				IpBanListEntry entry = new IpBanListEntry(jsonObject);
				if (entry.getUser() != null) {
					playerList.getIpBans().add(entry);
				}
			}
		} catch (IOException | JsonParseException | NullPointerException e) {
		}
		try {
			BufferedReader bufferedReader = Files.newReader(PlayerList.USERBANLIST_FILE, StandardCharsets.UTF_8);
			JsonArray jsonArray = GSON.fromJson(bufferedReader, JsonArray.class);

			for (JsonElement jsonElement : jsonArray) {
				JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "entry");
				UserBanListEntry entry = new UserBanListEntry(jsonObject);
				if (entry.getUser() != null) {
					playerList.getBans().add(entry);
				}
			}
		} catch (IOException | JsonParseException | NullPointerException e) {
		}
	}
}