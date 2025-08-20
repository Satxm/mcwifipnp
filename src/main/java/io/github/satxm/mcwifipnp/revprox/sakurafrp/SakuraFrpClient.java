package io.github.satxm.mcwifipnp.revprox.sakurafrp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.satxm.mcwifipnp.revprox.FetchedTunnel;
import io.github.satxm.mcwifipnp.revprox.InvalidAuthException;

public class SakuraFrpClient {
	private static final String TOKEN_PATH = "./mcwifipnp/token.txt";
	private static final String TUNNELS_PATH = "./mcwifipnp/tunnels.txt";
	private static final HttpClient CLIENT = HttpClient.newHttpClient();
	private static final Gson GSON = new Gson();

	public static CompletableFuture<JsonElement> fetchTunnelsFake() {
		System.out.println("fetchTunnelsFake");
		return CompletableFuture.supplyAsync(() -> {
			BufferedReader reader;
			try {
				reader = Files.newBufferedReader(Path.of(TUNNELS_PATH));
				return GSON.fromJson(reader, JsonElement.class);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}, CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS));
	}

	private static CompletableFuture<Map<Integer, SakuraFrpNode>> fetchNodes(String token) {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.natfrp.com/v4/nodes"))
				.header("Authorization", "Bearer " + token).GET().build();

		return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
			int status = response.statusCode();
			if (status == 200) {
				JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
				Map<Integer, SakuraFrpNode> result = new HashMap<>();
				for (Map.Entry<String, JsonElement> e : root.entrySet()) {
					int id = Integer.parseInt(e.getKey());
					SakuraFrpNode node = GSON.fromJson(e.getValue(), SakuraFrpNode.class);
					result.put(id, node);
				}
				return result;
			} else if (status == 401 || status == 403) {
				throw new SecurityException("Access denied: invalid or expired token.");
			} else {
				throw new RuntimeException("Node API failed with status code: " + status);
			}
		});
	}

	public static CompletableFuture<List<FetchedTunnel>> fetchTunnels() {
		try {
			// 读取 token
			String token = Files.readString(Path.of(TOKEN_PATH)).trim();

			// 构造请求
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.natfrp.com/v4/tunnels"))
					.header("Authorization", "Bearer " + token).GET().build();

			// 异步发送并返回 JSON body
			CompletableFuture<SakuraFrpTunnel[]> tunnelsFuture = CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
				int status = response.statusCode();
				if (status == 200) {
					SakuraFrpTunnel[] tunnels = GSON.fromJson(response.body(), SakuraFrpTunnel[].class);
					JsonArray jsonRoot = GSON.fromJson(response.body(), JsonArray.class);
					if (jsonRoot == null) {
						throw new RuntimeException("Response JSON is null.");
					}

					return tunnels;
				} else if (status == 401 || status == 403) {
					throw new InvalidAuthException("Access denied: invalid or expired token.");
				} else {
					throw new RuntimeException("HTTP request failed with status code: " + status);
				}
			});

			return tunnelsFuture.thenCompose(tunnels -> fetchNodes(token).thenApply(nodeMap -> {
				for (SakuraFrpTunnel t : tunnels) {
					SakuraFrpNode node = nodeMap.get(t.node);
					if (node != null) {
						t.desc = node.name;
						t.hostname = node.host;
					}
				}
				return List.of(tunnels);
			}));
		} catch (IOException e) {
			CompletableFuture<List<FetchedTunnel>> failed = new CompletableFuture<>();
			failed.completeExceptionally(e);
			return failed;
		}
	}

	public static Runnable pingNode(String host, int port, Consumer<Integer> onComplete) {
		return () -> {
			try (Socket socket = new Socket()) {
				long start = System.nanoTime();
				socket.connect(new InetSocketAddress(host, port), 5000);
				long elapsedMs = (System.nanoTime() - start) / 1_000_000;
				onComplete.accept((int) elapsedMs);
			} catch (IOException e) {
				onComplete.accept(-1);
			}
		};
	}
}
