package io.github.satxm.mcwifipnp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dosse.upnp.UPnP;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class UPnPModule implements Runnable {
	private static final Logger LOGGER = MCWiFiPnPUnit.LOGGER;

	public final int port;
	public final String displayName;

	private UPnPModule(int port, String motd) {
		this.port = port;
		this.displayName = motd;
	}

	public static void startIfEnabled(MinecraftServer server, Config cfg) {
		if (!cfg.UseUPnP) {
			((IUPnPProvider) server).setUPnPInstance(null);
			return;
		}

		UPnPModule instance = new UPnPModule(cfg.port, cfg.motd);
		((IUPnPProvider) server).setUPnPInstance(instance);
		new Thread(instance, "MCWiFiPnP_UPnP").start();
	}

	public static void stop(MinecraftServer server) {
		UPnPModule uPnPModule = ((IUPnPProvider) server).getUPnPInstance();
		if (uPnPModule != null) {
			uPnPModule.stop();
		}
	}

	public void stop() {
        UPnP.closePortTCP(this.port);
        LOGGER.info("Stopped forwarded port " + this.port + ".");
	}

	@Override
	public void run() {
		ChatComponent chat = Minecraft.getInstance().gui.getChat();

		if (UPnP.isUPnPAvailable()) {
			if (UPnP.isMappedTCP(this.port)) {
				chat.addMessage(Component.translatable("mcwifipnp.upnp.failed.mapped", this.port));
			} else if (UPnP.openPortTCP(this.port, this.displayName)) {
				chat.addMessage(Component.translatable("mcwifipnp.upnp.success", this.port));
				LOGGER.info("Started forwarded port " + this.port + ".");
			} else {
				chat.addMessage(Component.translatable("mcwifipnp.upnp.failed", this.port));
			}
		} else {
			chat.addMessage(Component.translatable("mcwifipnp.upnp.failed.disabled", this.port));
		}
	}
}
