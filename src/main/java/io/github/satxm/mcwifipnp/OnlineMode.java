package io.github.satxm.mcwifipnp;

import net.minecraft.network.chat.Component;

public enum OnlineMode {
  ONLINE(true, false,"online"),
  OFFLINE(false, false,"offline"),
  FIXUUID(false, true,"fixuuid");

  private final boolean onlinemode, fixuuid;
  private final Component displayName, toolTip;

  OnlineMode(boolean onlinemode, boolean fixuuid, final String string) {
    this.onlinemode = onlinemode;
    this.fixuuid = fixuuid;
    this.displayName = Component.translatable("mcwifipnp.gui.OnlineMode." + string);
    this.toolTip = Component.translatable("mcwifipnp.gui.OnlineMode." + string +".info");
  }

  public Component getDisplayName() {
    return this.displayName;
  }

  public Component gettoolTip() {
    return this.toolTip;
  }

  public static OnlineMode of(boolean onlinemode, boolean fixuuid) {
    if (onlinemode) {
      return ONLINE;
    } else {
      return fixuuid ? FIXUUID : OFFLINE;
    }
  }

  public boolean getOnlieMode() {
    return this.onlinemode;
  }

  public boolean getFixUUID() {
    return this.fixuuid;
  }


}
