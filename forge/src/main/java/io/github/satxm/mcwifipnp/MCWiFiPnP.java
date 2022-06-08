package io.github.satxm.mcwifipnp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.HTTPUtil;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.GameType;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod(MCWiFiPnP.MODID)
public class MCWiFiPnP {
	public static final String MODID = "mcwifipnp";
	private static final Map<MinecraftServer, Config> configMap = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Gson gson = new GsonBuilder().create();
	private static final Logger LOGGER = LogManager.getLogger(MCWiFiPnP.class);

	public MCWiFiPnP() {
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.addListener(this::ChangeButton);
	}

	@SubscribeEvent
	public void ChangeButton(GuiScreenEvent.InitGuiEvent.Post event) {
		Minecraft client = Minecraft.getInstance();
		Screen screen = event.getGui();
		if (screen instanceof IngameMenuScreen && event.getWidgetList().size() != 0) {
			for (int k = 0; k < event.getWidgetList().size(); k++) {
				Button ShareToLanOld = (Button) event.getWidgetList().get(k);
				if (ShareToLanOld.getMessage().getString()
						.equals(new TranslationTextComponent("menu.shareToLan").getString())) {
					int x = ShareToLanOld.x;
					int y = ShareToLanOld.y;
					int w = ShareToLanOld.getWidth();
					int h = ShareToLanOld.getHeight();
					Button ShareToLanNew = new Button(x, y, w, h, new TranslationTextComponent("menu.shareToLan"),
							(button) -> client.setScreen(new ShareToLanScreen(screen)));
					ShareToLanNew.active = ShareToLanOld.active;
					event.removeWidget(ShareToLanOld);
					event.addWidget(ShareToLanNew);
				}
			}
		}
	}

	
	
	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event) {
		Path location = event.getServer().getWorldPath(FolderName.ROOT).resolve("mcwifipnp.json");
		Config cfg;
		try {
			cfg = gson.fromJson(new String(Files.readAllBytes(location)), Config.class);
			cfg.location = location;
		} catch (IOException | JsonParseException e) {
			try {
				Files.deleteIfExists(location);
			} catch (IOException ioException) {
		}
			cfg = new Config();
			cfg.location = location;
			cfg.needsDefaults = true;
		}
		configMap.put(event.getServer(), cfg);
	}

	@SubscribeEvent
	public void onServerStopping(FMLServerStoppingEvent event) {
		Config cfg = configMap.get(event.getServer());
		if (event.getServer().isPublished() && cfg.UseUPnP) {
			UPnP.closePortTCP(cfg.port);
			LOGGER.info("Stopped forwarded port " + cfg.port + ".");
		}
	}

	public static Config getConfig(MinecraftServer server) {
		return Objects.requireNonNull(configMap.get(server), "no config for server???");
	}

	public static void openToLan() {
		Minecraft client = Minecraft.getInstance();
		IntegratedServer server = client.getSingleplayerServer();
		MCWiFiPnP.Config cfg = getConfig(server);

		server.setMotd(cfg.motd);
		server.getStatus().setDescription(new StringTextComponent(cfg.motd));
		server.publishServer(GameType.byName(cfg.GameMode), cfg.AllowCommands, cfg.port);
		server.getPlayerList().maxPlayers = cfg.maxPlayers;
		server.setUsesAuthentication(cfg.OnlineMode);
		server.setPvpAllowed(cfg.EnablePvP);
		client.gui.getChat().addMessage(new TranslationTextComponent("commands.publish.started", cfg.port));
		new Thread(() -> {
			if (cfg.UseUPnP) {
				if (UPnP.isUPnPAvailable()) {
					if (UPnP.isMappedTCP(cfg.port)) {
						client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.failed.mapped", cfg.port));
					} else if (UPnP.openPortTCP(cfg.port, cfg.motd)) {
						client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.success", cfg.port));
						LOGGER.info("Started forwarded port " + cfg.port + ".");
					} else {
						client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.failed", cfg.port));
					}
				} else {
					client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.failed.disabled", cfg.port));
				}
			}

			if (cfg.CopyToClipboard) {
				ArrayList<ITextComponent> IPComponentList = new ArrayList<ITextComponent>();
				ArrayList<String> IPList = new ArrayList<String>();
				for (int i = 0; i < IPAddressList().size(); i++) {
					Map<String, String> NewMap = IPAddressList().get(i);
					if (NewMap.get("Type") == "IPv4") {
						IPComponentList.add(IPComponent(
								new TranslationTextComponent(NewMap.get("Local")).getString() + " " + NewMap.get("Type"),
								NewMap.get("IP") + ":" + cfg.port));
					} else {
						IPComponentList.add(IPComponent(
								new TranslationTextComponent(NewMap.get("Local")).getString() + " " + NewMap.get("Type"),
								"[" + NewMap.get("IP") + "]:" + cfg.port));
					}
					IPList.add(NewMap.get("IP"));
				}
				if (!GetGlobalIPv4().isEmpty() && !IPList.contains(GetGlobalIPv4().get("IP"))) {
					IPComponentList.add(IPComponent(
							new TranslationTextComponent(GetGlobalIPv4().get("Local")).getString() + " "
									+ GetGlobalIPv4().get("Type"),
							GetGlobalIPv4().get("IP") + ":" + cfg.port));
					IPList.add(GetGlobalIPv4().get("IP"));
				}
				if (!GetGlobalIPv6().isEmpty() && !IPList.contains(GetGlobalIPv6().get("IP"))) {
					IPComponentList.add(IPComponent(
							new TranslationTextComponent(GetGlobalIPv6().get("Local")).getString() + " "
									+ GetGlobalIPv6().get("Type"),
							"[" + GetGlobalIPv6().get("IP") + "]:" + cfg.port));
					IPList.add(GetGlobalIPv4().get("IP"));
				}
				if (cfg.UseUPnP && UPnP.getExternalIP() != null && !IPList.contains(GetGlobalIPv6().get("IP"))) {
					IPComponentList.add(IPComponent("UPnP IPv4", UPnP.getExternalIP() + ":" + cfg.port));
					IPList.add(UPnP.getExternalIP());
				}
				if (IPList.isEmpty()) {
					client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.cantgetip"));
				} else {
					IFormattableTextComponent component = null;
					for (int i = 0; i < IPComponentList.size(); i++) {
						if (component == null) {
							component = IPComponentList.get(i).copy();
						} else {
							component.append(IPComponentList.get(i));
						}
					}
					client.gui.getChat().addMessage(new TranslationTextComponent("mcwifipnp.upnp.clipboard",
							new Object[] { component }));
				}
			}
	}, "MCWiFiPnP").start();
	}

	static void saveConfig(Config cfg) {
		if (!cfg.needsDefaults) {
			try {
				Files.write(cfg.location, toPrettyFormat(cfg).getBytes(), StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.CREATE);
			} catch (IOException e) {
				LOGGER.warn("Unable to write config file!", e);
			}
		}
	}

	public static class Config {
		public int port = HTTPUtil.getAvailablePort();
		public int maxPlayers = 10;
		public String GameMode = "survival";
		public String motd = "A Minecraft LAN World";
		public boolean UseUPnP = true;
		public boolean AllowCommands = false;
		public boolean OnlineMode = true;
		public boolean EnablePvP = true;
		public boolean CopyToClipboard = true;
		public transient Path location;
		public transient boolean needsDefaults = false;
	}

	private static String toPrettyFormat(Object src) {
		String json = gson.toJson(src);
		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(jsonObject);
	}

	private static ITextComponent IPComponent(String Type, String IP) {
		return TextComponentUtils.wrapInSquareBrackets((new StringTextComponent(Type)).withStyle((style) -> {
			return style.withColor(TextFormatting.GREEN)
					.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, IP))
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							new TranslationTextComponent("chat.copy.click").append("\n").append(IP)))
					.withInsertion(IP);
		}));
	}

    public static Map<String, String> GetGlobalIPv4() {
        String ipv4 = null;
        try {
            URL url = new URL("https://api-ipv4.ip.sb/ip");
            URLConnection URLconnection = url.openConnection();
            InputStreamReader isr = new InputStreamReader(URLconnection.getInputStream());
            BufferedReader bufr = new BufferedReader(isr);
            String str;
            while ((str = bufr.readLine()) != null) {
                ipv4 = str;
            }
            bufr.close();
        } catch (Exception e) {
        }
        Map<String, String> Gl4Map = new HashMap<String, String>();
        if (ipv4 != null) {
            // Gl4Map.put("Iface", "Global IPv4");
            Gl4Map.put("Type", "IPv4");
            Gl4Map.put("Local", "mcwifipnp.gui.Global");
            Gl4Map.put("IP", ipv4);
        }
        return Gl4Map;
    }

    public static Map<String, String> GetGlobalIPv6() {
        String ipv6 = null;
        try {
            URL url = new URL("https://api-ipv6.ip.sb/ip");
            URLConnection URLconnection = url.openConnection();
            InputStreamReader isr = new InputStreamReader(URLconnection.getInputStream());
            BufferedReader bufr = new BufferedReader(isr);
            String str;
            while ((str = bufr.readLine()) != null) {
                ipv6 = str;
            }
            bufr.close();
        } catch (Exception e) {
        }
        Map<String, String> Gl6Map = new HashMap<String, String>();
        if (ipv6 != null) {
            // Gl6Map.put("Iface", "Global IPv6");
            Gl6Map.put("Type", "IPv6");
            Gl6Map.put("Local", "mcwifipnp.gui.Global");
            Gl6Map.put("IP", ipv6);
        }
        return Gl6Map;
    }

    public static ArrayList<Map<String, String>> IPAddressList() {
        ArrayList<Map<String, String>> out = new ArrayList<Map<String, String>>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                try {
                    NetworkInterface iface = ifaces.nextElement();
                    if (!iface.isUp() || iface.isLoopback() || iface.isVirtual() || iface.isPointToPoint()) {
                        continue;
                    }
                    if (iface.getDisplayName().contains("Virtual")
                            || iface.getDisplayName().contains("VMware")
                            || iface.getDisplayName().contains("VirtualBox")
                            || iface.getDisplayName().contains("Bluetooth")
                            || iface.getDisplayName().contains("Hyper-V")) {
                        continue;
                    }
                    Enumeration<InetAddress> addrs = iface.getInetAddresses();
                    if (addrs == null) {
                        continue;
                    }
                    while (addrs.hasMoreElements()) {
                        Map<String, String> NetMap = new HashMap<String, String>();
                        // NetMap.put("Iface", iface.getDisplayName());
                        InetAddress addr = addrs.nextElement();
                        if (addr instanceof Inet4Address) {
                            NetMap.put("Type", "IPv4");
                        }
                        if (addr instanceof Inet6Address) {
                            NetMap.put("Type", "IPv6");
                        }
                        if (addr.isLinkLocalAddress()) {
                            continue;
                        }
                        if (addr.isSiteLocalAddress()) {
                            NetMap.put("Local", "mcwifipnp.gui.Local");
                        } else {
                            NetMap.put("Local", "mcwifipnp.gui.Global");
                        }
                        NetMap.put("IP", addr.getHostAddress());
                        out.add(NetMap);
                    }
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
		}
		return out;
	}
}
