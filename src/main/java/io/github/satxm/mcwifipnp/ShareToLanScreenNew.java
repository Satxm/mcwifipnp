package io.github.satxm.mcwifipnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import java.util.Collections;

public class ShareToLanScreenNew extends Screen {
  private final static Component EDITPORT_INFO = Component.translatable("mcwifipnp.gui.port.info");
  private final static int TEXT_COLOR_HINT = 7368816;
  private final static int TEXT_COLOR_NORMAL = 0xE0E0E0;
  private final static int TEXT_COLOR_WARN = 0xFFFF55;
  private final static int TEXT_COLOR_ERROR = 0xFF5555;

  private final Config cfg;
  private EditBox EditPort;
  private EditBox EditMotd;
  private EditBox EditPlayers;
  private final Screen lastScreen;

  public ShareToLanScreenNew(Screen screen) {
    super(Component.translatable("lanServer.title"));
    this.lastScreen = screen;

    MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
    MCWiFiPnPUnit.ReadingConfig(server);
    this.cfg = MCWiFiPnPUnit.getConfig(server);
  }

  @Override
  protected void init() {
    Minecraft client = Minecraft.getInstance();
    if (cfg.usingDefaults) {
      cfg.port = HttpUtil.getAvailablePort();
      cfg.AllowCommands = client.getSingleplayerServer().getWorldData().isAllowCommands();
      cfg.GameMode = client.getSingleplayerServer().getDefaultGameType();
      cfg.OnlineMode = client.getSingleplayerServer().usesAuthentication();
      cfg.ForceOfflinePlayers = Collections.emptyList();
    }

    // Start server button.
    Button StartLanServer = Button.builder(Component.translatable("lanServer.start"), button -> {
      cfg.save();
      MCWiFiPnPUnit.OpenToLan();
      this.minecraft.updateTitle();
      this.minecraft.setScreen((Screen) null);
      if (MCWiFiPnPUnit.convertOldUsers(this.minecraft.getSingleplayerServer()))
        this.minecraft.getSingleplayerServer().getProfileCache().save();
    }).bounds(this.width / 2 - 155, this.height - 32, 150, 20).build();
    this.addRenderableWidget(StartLanServer);

    // Cancel button
    this.addRenderableWidget(
        Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
            .bounds(this.width / 2 + 5, this.height - 32, 150, 20).build());

    // GameMode toggle button
    this.addRenderableWidget(CycleButton.builder(GameType::getShortDisplayName)
        .withValues(GameType.values())
        .withInitialValue(cfg.GameMode).create(this.width / 2 - 155, 36, 150, 20,
            Component.translatable("selectWorld.gameMode"), (cycleButton, gameMode) -> {
              cfg.GameMode = gameMode;
            }));

    // Allow Cheat button (for other joined players)
    this.addRenderableWidget(CycleButton.onOffBuilder(cfg.AllowCommands).create(this.width / 2 + 5, 36, 150, 20,
        Component.translatable("selectWorld.allowCommands"), (cycleButton, AllowCommands) -> {
          cfg.AllowCommands = AllowCommands;
        }));

    // Port field
    final int defaultPort = cfg.port;
    this.EditPort = new EditBox(this.font, this.width / 2 - 154, 70, 96, 20,
        Component.translatable("mcwifipnp.gui.port")) {
      @Override
      public void setFocused(boolean newValue) {
        super.setFocused(newValue);

        if (!newValue && this.getValue().isBlank()) {
          this.setValue(Integer.toString(defaultPort));
        }
      }
    };
    this.EditPort.setValue(Integer.toString(cfg.port));
    this.EditPort.setTextColor(TEXT_COLOR_HINT);
    this.EditPort.setMaxLength(5);
    this.EditPort.setTooltip(Tooltip.create(EDITPORT_INFO));
    this.addRenderableWidget(EditPort);

    this.EditPort.setResponder(sPort -> {
      if (sPort.isBlank()) {
        this.EditPort.setTextColor(TEXT_COLOR_HINT);
        this.EditPort.setTooltip(Tooltip.create(EDITPORT_INFO));
        StartLanServer.active = true;
        cfg.port = defaultPort;
      } else {
        try {
          int port = Integer.parseInt(sPort);
          if (port < 1024 || port > 65535) {
            throw new NumberFormatException("Port out of range:" + sPort);
          } else if (!HttpUtil.isPortAvailable(port)) {
            this.EditPort.setTextColor(TEXT_COLOR_WARN);
            this.EditPort.setTooltip(
                Tooltip.create(Component.translatable("mcwifipnp.gui.port.unavailable")));
            StartLanServer.active = false;
          } else if (defaultPort == port) {
            this.EditPort.setTextColor(TEXT_COLOR_HINT);
            this.EditPort.setTooltip(Tooltip.create(EDITPORT_INFO));
            StartLanServer.active = true;
          } else {
            this.EditPort.setTextColor(TEXT_COLOR_NORMAL);
            this.EditPort.setTooltip(
                Tooltip.create(EDITPORT_INFO));
            StartLanServer.active = true;
          }
          cfg.port = port;
        } catch (NumberFormatException ex) {
          this.EditPort.setTextColor(TEXT_COLOR_ERROR);
          this.EditPort.setTooltip(
              Tooltip.create(Component.translatable("mcwifipnp.gui.port.invalid")));
          StartLanServer.active = false;
        }
      }
    });

    // Number of players field
    final int defaultPlayers = cfg.maxPlayers;
    this.EditPlayers = new EditBox(this.font, this.width / 2 - 48, 70, 96, 20,
        Component.translatable("mcwifipnp.gui.players")) {
      @Override
      public void setFocused(boolean newValue) {
        super.setFocused(newValue);

        if (!newValue && this.getValue().isBlank()) {
          this.setValue(Integer.toString(defaultPlayers));
        }
      }
    };
    this.EditPlayers.setValue(Integer.toString(cfg.maxPlayers));
    this.EditPlayers.setTextColor(TEXT_COLOR_HINT);
    this.EditPlayers.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.players.info")));
    this.addRenderableWidget(EditPlayers);

    this.EditPlayers.setResponder((sPlayers) -> {
      if (sPlayers.isBlank()) {
        this.EditPlayers.setTextColor(TEXT_COLOR_HINT);
        StartLanServer.active = true;
        cfg.port = defaultPlayers;
      } else {
        try {
          int players = Integer.parseInt(sPlayers);
          if (players < 0) {
            throw new NumberFormatException();
          } else if (defaultPlayers == players) {
            this.EditPlayers.setTextColor(TEXT_COLOR_HINT);
          } else {
            this.EditPlayers.setTextColor(TEXT_COLOR_NORMAL);
          }
          StartLanServer.active = true;
          cfg.maxPlayers = players;
        } catch (NumberFormatException ex) {
          StartLanServer.active = false;
        }
      }
    });

    // Motd field
    final String defaultMotd = cfg.motd;
    this.EditMotd = new EditBox(this.font, this.width / 2 + 58, 70, 96, 20,
        Component.translatable("mcwifipnp.gui.motd")) {
      @Override
      public void setFocused(boolean newValue) {
        super.setFocused(newValue);

        if (!newValue && this.getValue().isBlank()) {
          this.setValue(defaultMotd);
        }
      }
    };
    this.EditMotd.setValue(defaultMotd);
    this.EditMotd.setTextColor(TEXT_COLOR_HINT);
    this.EditMotd.setMaxLength(32);
    this.EditMotd.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.motd.info")));
    this.EditMotd.setResponder((sMotd) -> {
      if (sMotd.isBlank() || defaultMotd.equals(sMotd)) {
        this.EditMotd.setTextColor(TEXT_COLOR_HINT);
      } else {
        this.EditMotd.setTextColor(TEXT_COLOR_NORMAL);
      }
      cfg.motd = sMotd;
    });
    this.addRenderableWidget(EditMotd);

    this.addRenderableWidget(
        CycleButton.onOffBuilder(cfg.AllPlayersCheats).create(this.width / 2 - 155, 124, 150, 20,
            Component.translatable("mcwifipnp.gui.AllPlayersCheats"), (cycleButton, AllPlayersCheats) -> {
              cfg.AllPlayersCheats = AllPlayersCheats;
            }))
        .setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.AllPlayersCheats.info")));

    this.addRenderableWidget(CycleButton.onOffBuilder(cfg.Whitelist).create(this.width / 2 + 5, 124, 150, 20,
        Component.translatable("mcwifipnp.gui.Whitelist"), (cycleButton, Whitelist) -> {
          cfg.Whitelist = Whitelist;
        })).setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.Whitelist.info")));

    this.addRenderableWidget(CycleButton.builder(OnlineMode::getDisplayName)
        .withValues(OnlineMode.values())
        .withInitialValue(OnlineMode.of(cfg.OnlineMode, cfg.EnableUUIDFixer)).withTooltip((OnlineMode) -> Tooltip.create(OnlineMode.gettoolTip()))
        .create(this.width / 2 - 155, 148, 150, 20,
            Component.translatable("mcwifipnp.gui.OnlineMode"), (cycleButton, OnlineMode) -> {
              cfg.OnlineMode = OnlineMode.getOnlieMode();
              cfg.EnableUUIDFixer = OnlineMode.getFixUUID();
        }));

    this.addRenderableWidget(CycleButton.onOffBuilder(cfg.PvP).create(this.width / 2 + 5, 148, 150, 20,
        Component.translatable("mcwifipnp.gui.PvP"), (cycleButton, PvP) -> {
          cfg.PvP = PvP;
        })).setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.PvP.info")));

    this.addRenderableWidget(CycleButton.onOffBuilder(cfg.UseUPnP).create(this.width / 2 - 155, 172, 150, 20,
        Component.translatable("mcwifipnp.gui.UseUPnP"), (cycleButton, UseUPnP) -> {
          cfg.UseUPnP = UseUPnP;
        })).setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.UseUPnP.info")));

    this.addRenderableWidget(CycleButton.onOffBuilder(cfg.CopyToClipboard).create(this.width / 2 + 5, 172, 150, 20,
        Component.translatable("mcwifipnp.gui.CopyIP"), (cycleButton, CopyToClipboard) -> {
          cfg.CopyToClipboard = CopyToClipboard;
        })).setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.CopyIP.info")));
  }

  @Override
  public void onClose() {
    this.minecraft.setScreen(this.lastScreen);
  }

  @Override
  public void render(GuiGraphics guiGraphics, int i, int j, float f) {
    super.render(guiGraphics, i, j, f);
    guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 16777215);
    guiGraphics.drawString(this.font, Component.translatable("mcwifipnp.gui.port"), this.width / 2 - 149, 58,
        16777215);
    guiGraphics.drawString(this.font, Component.translatable("mcwifipnp.gui.players"), this.width / 2 - 43, 58,
        16777215);
    guiGraphics.drawString(this.font, Component.translatable("mcwifipnp.gui.motd"), this.width / 2 + 63, 58,
        16777215);
    guiGraphics.drawCenteredString(this.font, Component.translatable("lanServer.otherPlayers"), this.width / 2, 104,
        16777215);

  }
}
