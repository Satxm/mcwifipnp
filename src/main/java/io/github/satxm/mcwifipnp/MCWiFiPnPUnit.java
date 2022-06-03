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
import net.minecraft.util.HttpUtil;
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
            Boolean NoneIPv4 = false;
            Boolean NoneIPv6 = false;
            if (IPv4AddressList().size() > 0 || GetGlobalIPv4() != null
                    || UPnP.getExternalIP() != null) {
                for (int i = 0; i < IPv4AddressList().size(); i++) {
                    String IP = IPv4AddressList().get(i) + ":" + cfg.port;
                    IPComponentList.add(IPComponent("IPv4", IP));
                }
                if (GetGlobalIPv4() != null & !IPv4AddressList().contains(GetGlobalIPv4())) {
                    String IP = GetGlobalIPv4() + ":" + cfg.port;
                    IPComponentList.add(IPComponent("IPv4", IP));
                }
                if (UPnP.getExternalIP() != null & !IPv4AddressList().contains(UPnP.getExternalIP())) {
                    String IP = UPnP.getExternalIP() + ":" + cfg.port;
                    IPComponentList.add(IPComponent("IPv4", IP));
                }
            } else {
                NoneIPv4 = true;
            }
            if (IPv6AddressList().size() > 0 || GetGlobalIPv6() != null) {
                for (int i = 0; i < IPv6AddressList().size(); i++) {
                    String IP = "[" + IPv6AddressList().get(i) + "]:" + cfg.port;
                    IPComponentList.add(IPComponent("IPv6", IP));
                }
                if (GetGlobalIPv6() != null & !IPv6AddressList().contains(GetGlobalIPv6())) {
                    String IP = "[" + GetGlobalIPv6() + "]:" + cfg.port;
                    IPComponentList.add(IPComponent("IPv6", IP));
                }
            } else {
                NoneIPv6 = true;
            }
            if (NoneIPv4 == true && NoneIPv6 == true) {
                client.gui.getChat().addMessage(Component.translatable("mcwifipnp.upnp.success.cantgetip"));
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
                        Component.translatable("mcwifipnp.upnp.success.clipboard", new Object[] { component }));
            }
        }
    }

    public static void serverSatrting(MinecraftServer server) {
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

    public static void serverStopping(MinecraftServer server) {
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
        public int port = HttpUtil.getAvailablePort();
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

    public static String GetGlobalIPv4() {
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
        return ipv4;
    }

    public static String GetGlobalIPv6() {
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
        return ipv6;
    }

    public static ArrayList<String> IPv4AddressList() {
        ArrayList<String> ret = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                try {
                    NetworkInterface iface = ifaces.nextElement();
                    if (!iface.isUp() || iface.isLoopback() || iface.isVirtual() || iface.isPointToPoint()) {
                        continue;
                    }
                    Enumeration<InetAddress> addrs = iface.getInetAddresses();
                    if (addrs == null) {
                        continue;
                    }
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (addr instanceof Inet4Address) {
                            String ipstart = addr.getHostAddress().substring(0, addr.getHostAddress().indexOf(":"));
                            if (!ipstart.equals("10") && !ipstart.equals("172") && !ipstart.equals("192")) {
                                ret.add(addr.getHostAddress());
                            }
                        }
                    }
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
        }
        return ret;
    }

    public static ArrayList<String> IPv6AddressList() {
        ArrayList<String> ret = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                try {
                    NetworkInterface iface = ifaces.nextElement();
                    if (!iface.isUp() || iface.isLoopback() || iface.isVirtual() || iface.isPointToPoint()) {
                        continue;
                    }
                    Enumeration<InetAddress> addrs = iface.getInetAddresses();
                    if (addrs == null) {
                        continue;
                    }
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (addr instanceof Inet6Address) {
                            String ipstart = addr.getHostAddress().substring(0, addr.getHostAddress().indexOf(":"));
                            if (!ipstart.equals("fe80")) {
                                ret.add(addr.getHostAddress());
                            }
                        }
                    }
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
        }
        return ret;
    }

}