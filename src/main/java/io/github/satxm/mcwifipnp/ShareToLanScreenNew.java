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
  private final Config cfg;
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
    this.addRenderableWidget(EditPortEx
        .numerical(this.font, this.width / 2 - 154, 70, 96, 20, Component.translatable("mcwifipnp.gui.port"))
        .defaults(cfg.port, EditPortEx.TEXT_COLOR_HINT,
            Tooltip.create(Component.translatable("mcwifipnp.gui.port.info")))
        .invalid(EditPortEx.TEXT_COLOR_ERROR, Tooltip.create(Component.translatable("mcwifipnp.gui.port.invalid")))
        .validator((port) -> {
          if (port < 1024 || port > 65535) {
            throw new NumberFormatException("Port out of range:" + port);
          } else if (!HttpUtil.isPortAvailable(port)) {
            return new EditPortEx.ValidatorResult(EditPortEx.TEXT_COLOR_WARN,
                Tooltip.create(Component.translatable("mcwifipnp.gui.port.unavailable")), false, true);
          } else {
            return null;
          }
        }).responder((newState, newPort) -> {
          StartLanServer.active = newState.valid();
          if (newState.updateBackendValue())
            cfg.port = newPort;
        })
    ).setMaxLength(5);

    // Number of players field
    this.addRenderableWidget(EditPortEx
        .numerical(this.font, this.width / 2 - 48, 70, 96, 20, Component.translatable("mcwifipnp.gui.players"))
        .bistate(cfg.maxPlayers, Tooltip.create(Component.translatable("mcwifipnp.gui.players.info")),
            (maxPlayers) -> maxPlayers > 0)
        .responder((newState, maxPlayers) -> {
            StartLanServer.active = newState.valid();
            if (newState.updateBackendValue())
              cfg.maxPlayers = maxPlayers;
        })
    );

    // Motd field
    this.addRenderableWidget(EditPortEx
        .text(this.font, this.width / 2 + 58, 70, 96, 20, Component.translatable("mcwifipnp.gui.motd"))
        .bistate(cfg.motd, Tooltip.create(Component.translatable("mcwifipnp.gui.players.info")),
            (newMotd) -> true)
        .responder((newState, newMotd) -> {
            StartLanServer.active = newState.valid();
            if (newState.updateBackendValue())
              cfg.motd = newMotd;
        })
    ).setMaxLength(32);

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
