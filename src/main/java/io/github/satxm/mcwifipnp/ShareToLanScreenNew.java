package io.github.satxm.mcwifipnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import java.util.Collections;

import javax.annotation.Nullable;

public class ShareToLanScreenNew extends Screen {
  private final Config cfg;
  private final Screen lastScreen;

  public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
  @Nullable
  protected OptionsList list;

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

    this.layout.addTitleHeader(this.title, this.font);
    this.list = this.layout.addToContents(new OptionsList(this.minecraft, this.width, this.layout, this, this.font));
    this.addOptions();
    this.layout.visitWidgets(this::addRenderableWidget);
    this.repositionElements();
  }

  protected void addOptions() {
    // Footer
    LinearLayout linearlayout = layout.addToFooter(LinearLayout.horizontal().spacing(8));
    // Start server button.
    Button StartLanServer = Button.builder(Component.translatable("lanServer.start"), button -> {
      cfg.save();
      MCWiFiPnPUnit.OpenToLan();
      this.minecraft.updateTitle();
      this.minecraft.setScreen((Screen) null);
      if (MCWiFiPnPUnit.convertOldUsers(this.minecraft.getSingleplayerServer()))
        this.minecraft.getSingleplayerServer().getProfileCache().save();
    }).width(150).build();
    linearlayout.addChild(StartLanServer);



    // Cancel button
    linearlayout.addChild(
        Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
            .width(150).build());

    // Row 1
    // GameMode toggle button
    CycleButton<GameType> gameModeButton = CycleButton.builder(GameType::getShortDisplayName)
        .withValues(GameType.values())
        .withInitialValue(cfg.GameMode).create(
            Component.translatable("selectWorld.gameMode"), (cycleButton, gameMode) -> {
              cfg.GameMode = gameMode;
            });

    // Allow Cheat button (for other joined players)
    CycleButton<Boolean> allowCheatButton = CycleButton.onOffBuilder(cfg.AllowCommands).create(
        Component.translatable("selectWorld.allowCommands"), (cycleButton, AllowCommands) -> {
          cfg.AllowCommands = AllowCommands;
        });

    this.list.add(gameModeButton, allowCheatButton);

    // Row 2
    // Port field
    EditPortEx<Integer> portField = EditPortEx
        .numerical(this.font, 0, 0, 60, 20, Component.translatable("mcwifipnp.gui.port"))
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
        .maxLength(5);

    // Number of players field
    EditPortEx<Integer> maxPlayersField = EditPortEx
        .numerical(this.font, 0, 0, 60, 20, Component.translatable("mcwifipnp.gui.players"))
        .bistate(cfg.maxPlayers, Tooltip.create(Component.translatable("mcwifipnp.gui.players.info")),
            (maxPlayers) -> maxPlayers > 0)
        .responder((newState, maxPlayers) -> {
            StartLanServer.active = newState.valid();
            if (newState.updateBackendValue())
              cfg.maxPlayers = maxPlayers;
        })
    ;

    this.list.add(
        Component.translatable("mcwifipnp.gui.port"), portField,
        Component.translatable("mcwifipnp.gui.players"), maxPlayersField);

    // Row3

    // Motd field
    this.list.add(Component.translatable("mcwifipnp.gui.motd"), EditPortEx
        .text(this.font, 0, 0, 220, 20, Component.translatable("mcwifipnp.gui.motd"))
        .bistate(cfg.motd, Tooltip.create(Component.translatable("mcwifipnp.gui.players.info")),
            (newMotd) -> true)
        .responder((newState, newMotd) -> {
            StartLanServer.active = newState.valid();
            if (newState.updateBackendValue())
              cfg.motd = newMotd;
        })
    .maxLength(32));

    // Row4
    this.list.add(new StringWidget(310, 20, Component.translatable("lanServer.otherPlayers"), this.font));

    // Row5
    CycleButton<Boolean> otherCheats = CycleButton.onOffBuilder(cfg.AllPlayersCheats).create(
            Component.translatable("mcwifipnp.gui.AllPlayersCheats"), (cycleButton, AllPlayersCheats) -> {
              cfg.AllPlayersCheats = AllPlayersCheats;
            });
    otherCheats.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.AllPlayersCheats.info")));

    CycleButton<Boolean> whiteListEnabled = CycleButton.onOffBuilder(cfg.Whitelist).create(
        Component.translatable("mcwifipnp.gui.Whitelist"), (cycleButton, Whitelist) -> {
          cfg.Whitelist = Whitelist;
        });
    whiteListEnabled.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.Whitelist.info")));

    this.list.add(otherCheats, whiteListEnabled);

    // Row 6
    CycleButton<OnlineMode> onlineMode = CycleButton.builder(OnlineMode::getDisplayName)
        .withValues(OnlineMode.values())
        .withInitialValue(OnlineMode.of(cfg.OnlineMode, cfg.EnableUUIDFixer)).withTooltip((OnlineMode) -> Tooltip.create(OnlineMode.gettoolTip()))
        .create(
            Component.translatable("mcwifipnp.gui.OnlineMode"), (cycleButton, OnlineMode) -> {
              cfg.OnlineMode = OnlineMode.getOnlieMode();
              cfg.EnableUUIDFixer = OnlineMode.getFixUUID();
        });

    CycleButton<Boolean> pvp = CycleButton.onOffBuilder(cfg.PvP).create(
        Component.translatable("mcwifipnp.gui.PvP"), (cycleButton, PvP) -> {
          cfg.PvP = PvP;
        });
    pvp.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.PvP.info")));
    this.list.add(onlineMode, pvp);

    // Row 7
    CycleButton<Boolean> useUPnP = CycleButton.onOffBuilder(cfg.UseUPnP).create(
        Component.translatable("mcwifipnp.gui.UseUPnP"), (cycleButton, UseUPnP) -> {
          cfg.UseUPnP = UseUPnP;
        });
    useUPnP.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.UseUPnP.info")));

    CycleButton<Boolean> copyIP = CycleButton.onOffBuilder(cfg.CopyToClipboard).create(
        Component.translatable("mcwifipnp.gui.CopyIP"), (cycleButton, CopyToClipboard) -> {
          cfg.CopyToClipboard = CopyToClipboard;
        });
    copyIP.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.CopyIP.info")));
    this.list.add(useUPnP, copyIP);
  }

  @Override
  public void onClose() {
    this.minecraft.setScreen(this.lastScreen);
  }

  @Override
  protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.list != null) {
          this.list.updateSize(this.width, this.layout);
      }
  }
}
