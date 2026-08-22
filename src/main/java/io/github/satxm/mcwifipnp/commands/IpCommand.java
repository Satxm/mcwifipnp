package io.github.satxm.mcwifipnp.commands;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.dosse.upnp.UPnP;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.github.satxm.mcwifipnp.network.GlobalIPs;
import io.github.satxm.mcwifipnp.network.UPnPModule;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

public class IpCommand {
	public static String LOCAL_IP_KEY = "mcwifipnp.gui.localIP";
	public static String GLOBAL_IP_KEY = "mcwifipnp.gui.globalIP";
	public static Component CANNOT_GET_IP = Component.translatable("mcwifipnp.upnp.cantgetip");

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> cmdBuilder = Commands.literal("ip")
				.requires(Commands.hasPermission(Commands.LEVEL_OWNERS));

		cmdBuilder = cmdBuilder.then(Commands.literal("get")
				.then(EnumArgument.IP_FAMILY.appendTo(Commands.literal("local").executes(IpCommand::showLocalIPsAll),
						(enumOption) -> enumOption.executes(IpCommand::showLocalIPs)))
				.then(EnumArgument.IP_FAMILY.appendTo(Commands.literal("global").executes(IpCommand::showGlobalIPAll),
						(enumOption) -> enumOption.executes(IpCommand::showGlobalIPs)))
				.then(Commands.literal("upnp").executes(IpCommand::showUPnPIP))
				.executes(IpCommand::showDetail));

		// The default action
		cmdBuilder = cmdBuilder.executes(IpCommand::showBrief);

		commandDispatcher.register(cmdBuilder);
	}

	private static int showLocalIPs(CommandContext<CommandSourceStack> context) {
		StandardProtocolFamily family = EnumArgument.IP_FAMILY.valueOf(context, 0);

		return getAndReply(context, (components) -> getIPComponentLocal(components, family));
	}

	private static void showLocalIPsAllImpl(List<MutableComponent> results) {
		int labelIndex = results.size();

		int count = getIPComponentLocal(results, StandardProtocolFamily.INET);
		count += getIPComponentLocal(results, StandardProtocolFamily.INET6);
		if (count > 0) {
			results.add(labelIndex, Component.translatable(LOCAL_IP_KEY, "IP:"));
		}
	}

	private static int showLocalIPsAll(CommandContext<CommandSourceStack> context) {
		return getAndReply(context, IpCommand::showLocalIPsAllImpl);
	}

	private static int showGlobalIPs(CommandContext<CommandSourceStack> context) {
		StandardProtocolFamily family = EnumArgument.IP_FAMILY.valueOf(context, 0);

		return getAndReply(context, (components) -> getIPComponentGlobal(components, family));
	}

	private static void showGlobalIPAllImpl(List<MutableComponent> results) {
		int labelIndex = results.size();

		int count = getIPComponentGlobal(results, StandardProtocolFamily.INET);
		count += getIPComponentGlobal(results, StandardProtocolFamily.INET6);
		if (count > 0) {
			results.add(labelIndex, Component.translatable(GLOBAL_IP_KEY, "IP:"));
		}
	}

	private static int showGlobalIPAll(CommandContext<CommandSourceStack> context) {
		return getAndReply(context, IpCommand::showGlobalIPAllImpl);
	}

	private static int showUPnPIP(CommandContext<CommandSourceStack> context) {
		return getAndReply(context, (components) -> {
			showUPnPIPAllImpl(components, context);
		});
	}

	private static void showUPnPIPAllImpl(List<MutableComponent> results, CommandContext<CommandSourceStack> context) {
		int labelIndex = results.size();

		if (getIPComponentUPnP(results, context) > 0) {
			results.add(labelIndex, Component.literal("UPnP IPv4:"));
		}
	}

	private static int showDetail(CommandContext<CommandSourceStack> context) {
		return getAndReply(context, (components) -> {
			showGlobalIPAllImpl(components);
			showLocalIPsAllImpl(components);
			showUPnPIPAllImpl(components, context);
		});
	}

	private static int showBrief(CommandContext<CommandSourceStack> context) {
		Component result = getBrief(context.getSource().getServer());
		if (result == CANNOT_GET_IP) {
			context.getSource().sendFailure(result);
		} else {
			context.getSource().sendSuccess(() -> result, false);
		}
		return result == CANNOT_GET_IP ? 0 : Command.SINGLE_SUCCESS;
	}

	private static int getAndReply(CommandContext<CommandSourceStack> context, Consumer<List<MutableComponent>> sources) {
		List<MutableComponent> components = new LinkedList<>();
		sources.accept(components);

		if (components.isEmpty()) {
			context.getSource().sendFailure(CANNOT_GET_IP);
			return 0;
		} else {
			components.forEach(component -> context.getSource().sendSuccess(() -> component, false));
			return components.size();
		}
	}

	private static int getIPComponentLocal(List<MutableComponent> results, StandardProtocolFamily family) {
		List<InetAddress> localIPs = getHostIPs();
		if (localIPs.isEmpty()) {
			return 0;
		}
		System.out.println(family.toString());
		System.out.println(localIPs.toString());

		for (InetAddress addr : localIPs) {
			StandardProtocolFamily addrFamily = (addr instanceof Inet6Address)
					? StandardProtocolFamily.INET6
					: StandardProtocolFamily.INET;
			if (family == addrFamily) {
				String addrString = addr.getHostAddress();
				if (addr instanceof Inet6Address) {
					addrString = formatIPv6(addrString);
					if (addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
						results.add(copyable(addrString));
					}
				} else {
					results.add(copyable(addrString));
				}
			}
		}
		return results.size();
	}

	private static int getIPComponentGlobal(List<MutableComponent> results, StandardProtocolFamily family) {
		List<InetAddress> localIPs = getHostIPs();
		String ip = GlobalIPs.fetchGlobalIP(family);

		System.out.println(family.toString());
		System.out.println(ip);
		if (ip != null) {
			try {
				InetAddress ipaddr = InetAddress.getByName(ip);
				if (!localIPs.contains(ipaddr))
					localIPs.add(ipaddr);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		if (localIPs.isEmpty()) {
			return 0;
		}
		System.out.println(localIPs.toString());

		for (InetAddress addr : localIPs) {
			StandardProtocolFamily addrFamily = (addr instanceof java.net.Inet6Address)
					? StandardProtocolFamily.INET6
					: StandardProtocolFamily.INET;
			if (family == addrFamily) {
				String addrString = addr.getHostAddress();
				if (!(addr.isLinkLocalAddress() || addr.isSiteLocalAddress())) {
					if (addr instanceof Inet6Address) {
						addrString = formatIPv6(addrString);
						results.add(copyable(addrString));
					} else {
						results.add(copyable(addrString));
					}
				}
			}
		}
		return results.size();
	}

	private static int getIPComponentUPnP(List<MutableComponent> results, CommandContext<CommandSourceStack> context) {
		String upnpIP = UPnP.getExternalIP();
		if (!UPnPModule.has(context.getSource().getServer()) || upnpIP == null) {
			return 0;
		}

		final MutableComponent component = copyable(upnpIP);
		results.add(component);
		return 1;
	}

	public static Component getBrief(MinecraftServer server) {
		int port = server.getPort();
		boolean useUPnP = UPnPModule.has(server);

		List<MutableComponent> IPComponentList = new LinkedList<>();
		ArrayList<String> IPList = new ArrayList<String>();

		List<InetAddress> localIPs = getHostIPs();
		for (InetAddress addr : localIPs) {
			String addrString = addr.getHostAddress();
			StandardProtocolFamily family = (addr instanceof Inet6Address)
					? StandardProtocolFamily.INET6
					: StandardProtocolFamily.INET;
			if (addr instanceof Inet6Address) {
				addrString = formatIPv6(addrString);
				if (!(addr.isLinkLocalAddress() || addr.isSiteLocalAddress())) {
					IPComponentList.add(copyable(GLOBAL_IP_KEY, addrString, family, port));
				} else {
					IPComponentList.add(copyable(LOCAL_IP_KEY, addrString, family, port));
				}
			} else {
				IPComponentList.add(copyable(LOCAL_IP_KEY, addrString, family, port));
			}
			IPList.add(addrString);
		}

		String ipv4 = GlobalIPs.fetchGlobalIP(StandardProtocolFamily.INET);
		if (ipv4 != null && !IPList.contains(ipv4)) {
			IPComponentList.add(copyable(GLOBAL_IP_KEY, ipv4, StandardProtocolFamily.INET, port));
			IPList.add(ipv4);
		}

		String ipv6 = GlobalIPs.fetchGlobalIP(StandardProtocolFamily.INET6);
		if (ipv6 != null && !IPList.contains(ipv6)) {
			IPComponentList.add(copyable(GLOBAL_IP_KEY, ipv6, StandardProtocolFamily.INET6, port));
			IPList.add(ipv6);
		}

		String upnpIP = UPnP.getExternalIP();
		if (useUPnP && upnpIP != null && !IPList.contains(upnpIP)) {
			IPComponentList
					.add(ComponentUtils.wrapInSquareBrackets(copyable(Component.literal("UPnP IPv4"), upnpIP + ":" + port)));
			IPList.add(upnpIP);
		}

		if (IPComponentList.isEmpty()) {
			return IpCommand.CANNOT_GET_IP;
		} else {
			MutableComponent component = Component.empty();
			IPComponentList.forEach(component::append);
			return Component.translatable("mcwifipnp.upnp.clipboard", component);
		}
	}

	public static MutableComponent copyable(String ipTypeKey, String ip, StandardProtocolFamily family, int port) {
		return copyable(ipTypeKey, ip, family, ":" + port);
	}

	public static String formatIPString(String ip, StandardProtocolFamily family) {
		if (family == StandardProtocolFamily.INET6) {
			return "[" + ip + "]";
		}
		return ip;
	}

	public static MutableComponent copyable(String ipTypeKey, String ip, StandardProtocolFamily family, String suffix) {
		String familystr = (family == StandardProtocolFamily.INET6) ? "IPv6" : "IPv4";
		return ComponentUtils.wrapInSquareBrackets(copyable(Component.translatable(ipTypeKey, familystr),
				formatIPString(ip, family) + suffix));
	}

	public static MutableComponent copyable(String content) {
		return copyable(Component.literal(content), content);
	}

	public static MutableComponent copyable(MutableComponent base, String content) {
		return base.withStyle(style -> style.withColor(ChatFormatting.GREEN)
				.withClickEvent(new ClickEvent.CopyToClipboard(content))
				.withHoverEvent(new HoverEvent.ShowText(
						Component.translatable("chat.copy.click").append("\n").append(content)))
				.withInsertion(content));
	}

	public static List<InetAddress> getHostIPs() {
		List<InetAddress> results = new LinkedList<>();

		Enumeration<NetworkInterface> ifaces = null;
		try {
			ifaces = NetworkInterface.getNetworkInterfaces();
		} catch (Throwable t) {
			return results;
		}

		while (ifaces.hasMoreElements()) {
			NetworkInterface iface = ifaces.nextElement();
			try {
				if (!iface.isUp() || iface.isLoopback() || iface.isVirtual() || iface.isPointToPoint()) {
					continue;
				}
			} catch (SocketException e) {
			}
			if (iface.getDisplayName().contains("Virtual") || iface.getDisplayName().contains("VMware")
					|| iface.getDisplayName().contains("VirtualBox") || iface.getDisplayName().contains("Bluetooth")
					|| iface.getDisplayName().contains("Hyper-V")) {
				continue;
			}
			Enumeration<InetAddress> addrs = iface.getInetAddresses();
			if (addrs == null) {
				continue;
			}
			while (addrs.hasMoreElements()) {
				InetAddress addr = addrs.nextElement();
				results.add(addr);
			}
		}

		return results;
	}

	public static String formatIPv6(String addrString) {
		addrString = addrString.replaceAll("(^|:)(0(:0)+)(:|$)", "$1::$3").replaceAll(":{3,}", "::");
		if (addrString.startsWith("0:"))
			addrString = "::" + addrString.substring(2);
		if (addrString.endsWith(":0"))
			addrString = addrString.substring(0, addrString.length() - 2) + "::";
		if (addrString.contains("%"))
			addrString = addrString.substring(0, addrString.indexOf("%"));
		return addrString;
	}

}
