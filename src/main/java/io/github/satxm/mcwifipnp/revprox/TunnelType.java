package io.github.satxm.mcwifipnp.revprox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import io.github.satxm.mcwifipnp.MCWiFiPnPUnit;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public class TunnelType {
	private final static Map<String, TunnelType> TUNNEL_TYPES = new HashMap<>();
	private static boolean ICON_UPLOADED = false;

	public final String name;
	private final String iconFileName;

	@Nullable
	private ResourceLocation icon;

	private TunnelType(String name, String iconFileName) {
		this.name = name;
		this.iconFileName = iconFileName;
	}

	public ResourceLocation getIcon() {
		return this.icon;
	}

	public static void uploadIcons(TextureManager textureMgr) {
		if (ICON_UPLOADED)
			return;

		ICON_UPLOADED = true;

		for (Map.Entry<String, TunnelType> entry: TUNNEL_TYPES.entrySet()) {
			String tunnelTypeName = entry.getKey();
			String iconFileName = entry.getValue().iconFileName;
			try {
				Path path = Paths.get("mcwifipnp", iconFileName + ".png");
				byte[] bytes = Files.readAllBytes(path);

				NativeImage nativeImage = NativeImage.read(bytes);
				DynamicTexture dynamicTexture = new DynamicTexture(
					() -> "Favicon " + iconFileName, nativeImage);

				ResourceLocation resLoc = ResourceLocation.fromNamespaceAndPath(
					MCWiFiPnPUnit.MODID, "tunnel_types/" + tunnelTypeName);
				textureMgr.register(resLoc, dynamicTexture);
				entry.getValue().icon = resLoc;
			} catch (Exception e) {
				entry.getValue().icon = ResourceLocation
					.withDefaultNamespace("textures/misc/unknown_server.png");
			}
		}
	}

	public static TunnelType register(String name) {
		if (TUNNEL_TYPES.containsKey(name))
			return TUNNEL_TYPES.get(name);

		TunnelType tunnelType = new TunnelType(name, name);
		TUNNEL_TYPES.put(name, tunnelType);
		return tunnelType;
	}

	public static TunnelType get(String name) {
		return TUNNEL_TYPES.get(name);
	}

	static {
		register("sakurafrp");
		register("openfrp");
		register("cloudflare");
	}
}
