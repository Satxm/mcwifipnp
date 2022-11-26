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

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public class MCWiFiPnPUnit {
    private static final Map<MinecraftServer, Config> configMap = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Gson gson = new GsonBuilder().create();

    private static final Logger LOGGER = LogManager.getLogger(MCWiFiPnP.class);

    public static Config getConfig(MinecraftServer server) {
        return Objects.requireNonNull(configMap.get(server), "no config for server???");
    }

    public static void UseUPnP(Config cfg, Minecraft client) {
        if (cfg.UseUPnP) {
            if (UPnP.isUPnPAvailable()) {
                if (UPnP.isMappedTCP(cfg.port)) {
                    client.gui.getChat().addMessage(Component.translatable("mcwifipnp.upnp.failed.mapped", cfg.port));
                } else if (UPnP.openPortTCP(cfg.port, cfg.motd)) {
                    client.gui.getChat().addMessage(Component.translatable("mcwifipnp.upnp.success", cfg.port));
                    LOGGER.info("Started forwarded port " + cfg.port + ".");
                } else {
                    client.gui.getChat().addMessage(Component.translatable("mcwifipnp.upnp.failed", cfg.port));
                }
            } else {
                client.gui.getChat().addMessage(Component.translatable("mcwifipnp.upnp.failed.disabled", cfg.port));
            }
        }
    }

    public static void CopyToClipboard(Config cfg, Minecraft client) {
        if (cfg.CopyToClipboard) {
            ArrayList<Component> IPComponentList = new ArrayList<Component>();
            ArrayList<String> IPList = new ArrayList<String>();
            for (int i = 0; i < IPAddressList().size(); i++) {
                Map<String, String> NewMap = IPAddressList().get(i);
                if (NewMap.get("Type") == "IPv4") {
                    IPComponentList.add(
                            IPComponent(
                                    Component.translatable(NewMap.get("Local")).getString() + " " + NewMap.get("Type"),
                                    NewMap.get("IP") + ":" + cfg.port));
                } else {
                    IPComponentList.add(IPComponent(
                            Component.translatable(NewMap.get("Local")).getString() + " " + NewMap.get("Type"),
                            "[" + NewMap.get("IP") + "]:" + cfg.port));
                }
                IPList.add(NewMap.get("IP"));
            }
            if (!GetGlobalIPv4().isEmpty() && !IPList.contains(GetGlobalIPv4().get("IP"))) {
                IPComponentList.add(IPComponent(
                        Component.translatable(GetGlobalIPv4().get("Local")).getString() + " "
                                + GetGlobalIPv4().get("Type"),
                        GetGlobalIPv4().get("IP") + ":" + cfg.port));
                IPList.add(GetGlobalIPv4().get("IP"));
            }
            if (!GetGlobalIPv6().isEmpty() && !IPList.contains(GetGlobalIPv6().get("IP"))) {
                IPComponentList.add(IPComponent(
                        Component.translatable(GetGlobalIPv6().get("Local")).getString() + " "
                                + GetGlobalIPv6().get("Type"),
                        "[" + GetGlobalIPv6().get("IP") + "]:" + cfg.port));
                IPList.add(GetGlobalIPv4().get("IP"));
            }
            if (cfg.UseUPnP && UPnP.getExternalIP() != null && !IPList.contains(GetGlobalIPv6().get("IP"))) {
                IPComponentList.add(IPComponent("UPnP IPv4", UPnP.getExternalIP() + ":" + cfg.port));
                IPList.add(UPnP.getExternalIP());
            }
            if (IPList.isEmpty()) {
                client.gui.getChat().addMessage(Component.translatable("mcwifipnp.upnp.cantgetip"));
            } else {
                MutableComponent component = null;
                for (int i = 0; i < IPComponentList.size(); i++) {
                    if (component == null) {
                        component = IPComponentList.get(i).copy();
                    } else {
                        component.append(IPComponentList.get(i));
                    }
                }
                client.gui.getChat().addMessage(
                        Component.translatable("mcwifipnp.upnp.clipboard", new Object[] { component }));
            }
        }
    }

    public static void ReadingConfig(MinecraftServer server) {
        Path location = server.getWorldPath(LevelResource.ROOT).resolve("mcwifipnp.json");
        MCWiFiPnPUnit.Config cfg;
        try {
            cfg = gson.fromJson(new String(Files.readAllBytes(location)), MCWiFiPnPUnit.Config.class);
            cfg.location = location;
        } catch (IOException | JsonParseException e) {
            try {
                Files.deleteIfExists(location);
            } catch (IOException ie) {
                LOGGER.warn("Unable to read config file!", ie);
            }
            cfg = new MCWiFiPnPUnit.Config();
            cfg.location = location;
            cfg.needsDefaults = true;
        }
        configMap.put(server, cfg);
    }

    public static void ClosePortUPnP(MinecraftServer server) {
        MCWiFiPnPUnit.Config cfg = configMap.get(server);
        if (server.isPublished() && cfg.UseUPnP) {
            UPnP.closePortTCP(cfg.port);
            LOGGER.info("Stopped forwarded port " + cfg.port + ".");
        }
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
        public int port = 25565;
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
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(jsonObject);
    }

    private static Component IPComponent(String Type, String IP) {
        return ComponentUtils.wrapInSquareBrackets(Component.literal(Type).withStyle((style) -> {
            return style.withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, IP))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("chat.copy.click").append("\n").append(IP)))
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