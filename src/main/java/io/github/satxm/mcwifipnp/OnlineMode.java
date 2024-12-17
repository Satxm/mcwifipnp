package io.github.satxm.mcwifipnp;

import net.minecraft.network.chat.Component;

public enum OnlineMode {
	ONLINE(true, false, "online"),
	OFFLINE(false, false, "offline"),
	FIX_UUID(false, true, "fixuuid");

	public final boolean onlineMode, fixUUID;
	private final Component displayName, toolTip;

	OnlineMode(boolean onlineModeEnabled, boolean tryOnlineUUIDFirst, final String name) {
		this.onlineMode = onlineModeEnabled;
		this.fixUUID = tryOnlineUUIDFirst;
		this.displayName = Component.translatable("mcwifipnp.gui.OnlineMode." + name);
		this.toolTip = Component.translatable("mcwifipnp.gui.OnlineMode." + name + ".info");
	}

	public Component getDisplayName() {
		return this.displayName;
	}

	public Component gettoolTip() {
		return this.toolTip;
	}

	public static OnlineMode of(boolean onlineModeEnabled, boolean tryOnlineUUIDFirst) {
		if (onlineModeEnabled) {
			return ONLINE;
		} else {
			return tryOnlineUUIDFirst ? FIX_UUID : OFFLINE;
		}
	}
}
