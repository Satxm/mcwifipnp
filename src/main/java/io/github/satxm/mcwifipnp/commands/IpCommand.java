package io.github.satxm.mcwifipnp.commands;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
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

import io.github.satxm.mcwifipnp.UPnPModule;
import io.github.satxm.mcwifipnp.network.GlobalIPs;
import io.netty.channel.socket.InternetProtocolFamily;
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
			.requires((cmdStack) -> cmdStack.hasPermission(4));

		cmdBuilder = cmdBuilder.then(Commands.literal("get")
			.then(EnumArgument.IP_FAMILY.appendTo(Commands.literal("local").executes(IpCommand::showLocalIPsAll),
				(enumOption) -> enumOption.executes(IpCommand::showLocalIPs)))
			.then(EnumArgument.IP_FAMILY.appendTo(Commands.literal("global").executes(IpCommand::showGlobalIPAll),
				(enumOption) -> enumOption.executes(IpCommand::showGlobalIP)))
			.then(Commands.literal("upnp").executes(IpCommand::showUPnPIP))
			.executes(IpCommand::showDetail)
		);

		// The default action
		cmdBuilder = cmdBuilder.executes(IpCommand::showBrief);

		commandDispatcher.register(cmdBuilder);
	}

	private static int showLocalIPs(CommandContext<CommandSourceStack> context) {
		InternetProtocolFamily family = EnumArgument.IP_FAMILY.valueOf(context, 0);

		return getAndReply(context, (components) -> getIPComponentLocal(components, family));
	}

	private static void showLocalIPsAllImpl(List<MutableComponent> results) {
		int labelIndex = results.size();

		int count = getIPComponentLocal(results, InternetProtocolFamily.IPv4);
		count += getIPComponentLocal(results, InternetProtocolFamily.IPv6);
		if (count > 0) {
			results.add(labelIndex, Component.translatable(LOCAL_IP_KEY, "IP").append(":"));
		}
	}

	private static int showLocalIPsAll(CommandContext<CommandSourceStack> context) {
		return getAndReply(context, (components) -> {
			getIPComponentLocal(components, InternetProtocolFamily.IPv4);
			getIPComponentLocal(components, InternetProtocolFamily.IPv6);
		});
	}

	private static int showGlobalIP(CommandContext<CommandSourceStack> context) {
		InternetProtocolFamily family = EnumArgument.IP_FAMILY.valueOf(context, 0);

		return getAndReply(context, (components) -> {
			getIPComponentGlobal(components, family);
		});
	}

	private static void showGlobalIPAllImpl(List<MutableComponent> results) {
		int labelIndex = results.size();

		int count = getIPComponentGlobal(results, InternetProtocolFamily.IPv4);
		count += getIPComponentGlobal(results, InternetProtocolFamily.IPv6);
		if (count > 0) {
			results.add(labelIndex, Component.translatable(GLOBAL_IP_KEY, "IP").append(":"));
		}
	}

	private static int showGlobalIPAll(CommandContext<CommandSourceStack> context) {
		return getAndReply(context, IpCommand::showGlobalIPAllImpl);
	}

	private static int showUPnPIP(CommandContext<CommandSourceStack> context) {
		return getAndReply(context, (components) -> getIPComponentUPnP(components, context));
	}

	private static int showDetail(CommandContext<CommandSourceStack> context) {
		return getAndReply(context, (components) -> {
			showGlobalIPAllImpl(components);
			showLocalIPsAllImpl(components);

			int labelIndex = components.size();

			if (getIPComponentUPnP(components, context) > 0) {
				components.add(labelIndex, Component.literal("UPnP IPv4:"));
			}
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

	private static int getIPComponentLocal(List<MutableComponent> results, InternetProtocolFamily family) {
		List<InetAddress> localIPs = getLocalIPs();
		if (localIPs.isEmpty()) {
			return 0;
		}

		for (InetAddress addr: localIPs) {
			if (family == InternetProtocolFamily.of(addr)) {
				String addrString = addr.getHostAddress();
				addrString = formatIPString(addrString, family);
				results.add(copyable(addrString));
			}
		}
		return results.size();
	}

 	private static int getIPComponentGlobal(List<MutableComponent> results, InternetProtocolFamily family) {
		String ip = GlobalIPs.fetchGlobalIP(family);
		if (ip == null) {
			return 0;
		}

		ip = formatIPString(ip, family);
		results.add(copyable(ip));
		return 1;
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

		List<InetAddress> localIPs = getLocalIPs();
		for (InetAddress addr: localIPs) {
			String addrString = addr.getHostAddress();
			InternetProtocolFamily family = InternetProtocolFamily.of(addr);
			IPComponentList.add(copyable(LOCAL_IP_KEY, addrString, family, port));
			IPList.add(addrString);
		}

		String ipv4 = GlobalIPs.fetchGlobalIP(InternetProtocolFamily.IPv4);
		if (ipv4 != null && !IPList.contains(ipv4)) {
			IPComponentList.add(copyable(GLOBAL_IP_KEY, ipv4, InternetProtocolFamily.IPv4, port));
			IPList.add(ipv4);
		}

		String ipv6 = GlobalIPs.fetchGlobalIP(InternetProtocolFamily.IPv6);
		if (ipv6 != null && !IPList.contains(ipv6)) {
			IPComponentList.add(copyable(GLOBAL_IP_KEY, ipv6, InternetProtocolFamily.IPv6, port));
			IPList.add(ipv6);
		}

		String upnpIP = UPnP.getExternalIP();
		if (useUPnP && upnpIP != null && !IPList.contains(upnpIP)) {
			IPComponentList.add(ComponentUtils.wrapInSquareBrackets(copyable(Component.literal("UPnP IPv4"), upnpIP)));
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

	public static MutableComponent copyable(String ipTypeKey, String ip, InternetProtocolFamily family, int port) {
		return copyable(ipTypeKey, ip, family, ":" + port);
	}

	public static String formatIPString(String ip, InternetProtocolFamily family) {
		if (family == InternetProtocolFamily.IPv6) {
			ip = "[" + ip + "]";
		}
		return ip;
	}

	public static MutableComponent copyable(String ipTypeKey, String ip, InternetProtocolFamily family, String suffix) {
		return copyable(ComponentUtils.wrapInSquareBrackets(Component.translatable(ipTypeKey, family.toString())),
			formatIPString(ip, family) + suffix);
	}

	public static MutableComponent copyable(String content) {
		return copyable(Component.literal(content), content);
	}

	public static MutableComponent copyable(MutableComponent base, String content) {
		return base.withStyle(style -> style.withColor(ChatFormatting.GREEN)
				.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, content))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					Component.translatable("chat.copy.click").append("\n").append(content)))
				.withInsertion(content)
		);
	}

	public static List<InetAddress> getLocalIPs() {
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
				if (addr.isLinkLocalAddress()) {
					continue;
				}

				results.add(addr);
			}
		}

		return results;
	}
}
