package io.github.satxm.mcwifipnp.client;

import org.jspecify.annotations.Nullable;

import io.github.satxm.mcwifipnp.Config;
import io.github.satxm.mcwifipnp.MCWiFiPnPUnit;
import io.github.satxm.mcwifipnp.OnlineMode;
import io.github.satxm.mcwifipnp.commands.IpCommand;
import io.github.satxm.mcwifipnp.network.UPnPModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.components.PopupScreen.Builder;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.EqualSpacingLayout.Orientation;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.GridLayout.RowHelper;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.MultiplayerOptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.RestrictionsScreen;
import net.minecraft.client.gui.screens.options.HasDifficultyReaction;
import net.minecraft.client.gui.screens.options.HasGamemasterPermissionReaction;
import net.minecraft.client.gui.screens.options.InWorldGameRulesScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.MinecraftServer.MultiplayerScope;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

public class WorldOptionsScreenNew extends Screen implements HasGamemasterPermissionReaction, HasDifficultyReaction {
	private final Config cfg;
	private final Screen lastScreen;
	private final Level level;
	private final boolean serverPublished;

	public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private @Nullable ScrollableLayout scrollArea;
	private @Nullable CycleButton<Boolean> guestCommandAccessButton;
	private @Nullable Button applyChanges;
	private @Nullable Button gameRulesButton;
	private @Nullable CycleButton<GameType> defaultGameModeButton;
	private @Nullable CycleButton<GameType> personalGameModeButton;
	private @Nullable CycleButton<Boolean> pvpButton;
	private @Nullable CycleButton<Boolean> forceGameModeButton;
	private @Nullable CycleButton<Boolean> getPublicIPButton;
	private @Nullable CycleButton<Boolean> useUPnPButton;
	private @Nullable CycleButton<Boolean> enforceWhitelistButton;
	private @Nullable CycleButton<OnlineMode> onlineModeButton;

	private @Nullable EditBox portEdit;
	private @Nullable EditBox motdEdit;
	private @Nullable EditBox maxPlayersEdit;
	private @Nullable StringWidget portLabel;
	private @Nullable StringWidget motdLabel;
	private @Nullable StringWidget maxPlayersLabel;
	private DifficultyButtons difficultyButtons;

	private final boolean initialUseUPnP;
	private final boolean initialGetPublicIP;
	private final String initialMotd;
	private final int initialPort;
	private final MultiplayerScope initialMultiplayerScope;
	private Difficulty wantedDifficulty;
	private Difficulty initialDifficulty;
	private @Nullable Boolean initialDifficultyLocked;
	private @Nullable Boolean wantedDifficultyLocked;
	private boolean initialallowGuestCommands;
	private boolean initialForceGameMode;

	private static final Identifier INWORLD_MENU_LIST_BACKGROUND = Identifier
			.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");

	public WorldOptionsScreenNew(final Screen lastScreen, final Level level) {
		super(Component.translatable("lanServer.title"));
		this.lastScreen = lastScreen;
		this.level = level;
		this.difficultyButtons = DifficultyButtons.create(this.minecraft, level, this);
		IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();

		this.serverPublished = this.minecraft.hasSingleplayerServer()
				&& singleplayerServer.isPublished();

		cfg = Config.read(singleplayerServer);

		if (serverPublished && singleplayerServer.getMultiplayerScope() == MultiplayerScope.LAN) {
			cfg.readFromRunningServer(singleplayerServer);
		} else if (cfg.usingDefaults) {
			cfg.readFromRunningServer(singleplayerServer);
			cfg.port = HttpUtil.getAvailablePort();
			cfg.allowHostCommands = singleplayerServer.getWorldData().isAllowCommands();
			cfg.multiplayerScope = singleplayerServer.getMultiplayerScope();
		}

		this.initialPort = cfg.port;
		this.initialMotd = cfg.motd;
		this.initialUseUPnP = cfg.useUPnP;
		this.initialGetPublicIP = cfg.getPublicIP;
		this.initialMultiplayerScope = singleplayerServer.getMultiplayerScope();
		this.initialallowGuestCommands = cfg.allowGuestCommands;
		this.initialForceGameMode = cfg.forceGameMode;
	}

	protected void applyGeneralChanges() {
		IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();
		PlayerList playerList = singleplayerServer.getPlayerList();
		NameAndId hostPlayer = new NameAndId(singleplayerServer.getSingleplayerProfile());
		cfg.save(singleplayerServer);

		updateDifficulty();
		playerList.getPlayer(singleplayerServer.getSingleplayerProfile().id()).setGameMode(cfg.personalGameMode);
		singleplayerServer.setDefaultGameType(cfg.defaultGameMode);
		singleplayerServer.getWorldData().setAllowCommands(cfg.allowHostCommands);
		singleplayerServer.getGameRules().set(GameRules.PVP, cfg.enablePvP, singleplayerServer);

		if (cfg.allowHostCommands) {
			playerList.getOps().add(new ServerOpListEntry(hostPlayer, LevelBasedPermissionSet.OWNER,
					playerList.canBypassPlayerLimit(hostPlayer)));
		} else {
			playerList.getOps().remove(hostPlayer);
		}
		for (ServerPlayer serverPlayer : singleplayerServer.getPlayerList().getPlayers()) {
			playerList.sendPlayerPermissionLevel(serverPlayer);
		}
	}

	protected void applyChanges() {
		IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();
		PlayerList playerList = singleplayerServer.getPlayerList();
		NameAndId hostPlayer = new NameAndId(singleplayerServer.getSingleplayerProfile());

		cfg.save(singleplayerServer);
		updateDifficulty();

		if (cfg.multiplayerScope != initialMultiplayerScope || cfg.port != initialPort
				|| cfg.allowGuestCommands != initialallowGuestCommands) {
			this.changeMultiplayerScope(singleplayerServer);
			if (cfg.multiplayerScope == MultiplayerScope.LAN) {
				UPnPModule.startIfEnabled(singleplayerServer, cfg);
				GetPublicIP(singleplayerServer);
			}
		}

		if (this.serverPublished) {
			if (!this.initialMotd.equals(cfg.motd) || cfg.useUPnP ^ initialUseUPnP) {
				// Motd has changed, update UPnP display name
				UPnPModule.stop(singleplayerServer);
				UPnPModule.startIfEnabled(singleplayerServer, cfg);
			}
			if (cfg.getPublicIP ^ initialGetPublicIP) {
				GetPublicIP(singleplayerServer);
			}
		}
		cfg.applyTo(singleplayerServer);

		if (!cfg.allowGuestCommands) {
			playerList.getOps().clear();
		}
		if (cfg.allowHostCommands) {
			playerList.getOps().add(new ServerOpListEntry(hostPlayer, LevelBasedPermissionSet.OWNER,
					playerList.canBypassPlayerLimit(hostPlayer)));
		} else {
			playerList.getOps().remove(hostPlayer);
		}
		for (ServerPlayer serverPlayer : singleplayerServer.getPlayerList().getPlayers()) {
			playerList.sendPlayerPermissionLevel(serverPlayer);
		}

		if (playerList.isUsingWhitelist()) {
			playerList.getWhiteList().add(new UserWhiteListEntry(hostPlayer));
			playerList.reloadWhiteList();
		}
		if (MCWiFiPnPUnit.convertOldUsers(this.minecraft.getSingleplayerServer()))
			this.minecraft.getSingleplayerServer().services().nameToIdCache().add(hostPlayer);

		this.minecraft.updateTitle();
		this.minecraft.gui.setScreen((Screen) null);
	}

	@Override
	protected void init() {
		this.layout.addTitleHeader(Component.translatable("options.worldOptions.title"), this.font);
		IntegratedServer singleplayerServer = this.minecraft.getSingleplayerServer();
		LinearLayout content = LinearLayout.vertical().spacing(8);
		content.defaultCellSetting().padding(8).alignHorizontallyCenter().alignVerticallyTop();
		this.scrollArea = this.layout
				.addToContents(new ScrollableLayout(this.minecraft, content, this.layout.getContentHeight()));

		this.generalOptions(content, singleplayerServer);
		if (singleplayerServer != null) {
			this.multiplayerOptions(content, singleplayerServer);
		}

		GridLayout footer = this.layout.addToFooter(new GridLayout().columnSpacing(4).rowSpacing(4));
		footer.defaultCellSetting().alignHorizontallyCenter();
		GridLayout.RowHelper rowHelper = footer.createRowHelper(3);

		rowHelper.addChild(Button
				.builder(Component.translatable("mcwifipnp.gui.applyGeneralChanges"), button -> this.applyGeneralChanges())
				.width(100).build());
		rowHelper.addChild(Button
				.builder(Component.translatable("menu.multiplayerOptions.applyChanges"),
						button -> this.applyChanges())
				.width(100).build());
		rowHelper.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).width(100).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	private void generalOptions(final LinearLayout content, final @Nullable IntegratedServer singleplayerServer) {
		GridLayout grid = content.addChild(new GridLayout());
		grid.defaultCellSetting().alignHorizontallyCenter();
		RowHelper rowHelper = grid.columnSpacing(8).rowSpacing(4).createRowHelper(2);
		rowHelper.defaultCellSetting().alignHorizontallyCenter();

		rowHelper.addChild(new StringWidget(Component.translatable("options.worldOptions.general.title")
				.withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD), this.font), 2);

		// Default GameMode toggle button
		this.defaultGameModeButton = rowHelper
				.addChild(CycleButton.builder(GameType::getShortDisplayName, cfg.defaultGameMode)
						.withValues(GameType.values())
						.create(0, 0, 308, 20, Component.translatable("options.worldOptions.game_mode"),
								(cycleButton, gameMode) -> {
									cfg.defaultGameMode = gameMode;
								}),
						2);
		this.updateButton(defaultGameModeButton, singleplayerServer,
				Tooltip.create(Component.translatable("options.worldOptions.game_mode.tooltip")),
				Tooltip.create(Component.translatable("options.worldOptions.game_mode.disabled.operator.tooltip")),
				Tooltip.create(Component.translatable("options.worldOptions.game_mode.disabled.tooltip")));

		// Personal GameMode toggle button
		this.personalGameModeButton = rowHelper
				.addChild(CycleButton.builder(GameType::getShortDisplayName, cfg.personalGameMode)
						.withValues(GameType.values())
						.create(0, 0, 308, 20, Component.translatable("options.worldOptions.personal_game_mode"),
								(cycleButton, gameMode) -> {
									cfg.personalGameMode = gameMode;
								}),
						2);
		this.updateButton(personalGameModeButton, singleplayerServer,
				Tooltip.create(Component.translatable("options.worldOptions.personal_game_mode.tooltip")),
				Tooltip.create(Component.translatable("options.worldOptions.game_mode.disabled.operator.tooltip")),
				Tooltip.create(Component.translatable("options.worldOptions.game_mode.disabled.tooltip")));

		// Allow Host Cheat button
		rowHelper.addChild(CycleButton.onOffBuilder(cfg.allowHostCommands)
				.create(Component.translatable("selectWorld.allowCommands"), (cycleButton, allowHostCommands) -> {
					cfg.allowHostCommands = allowHostCommands;
					this.updateGuestCommandAccessButton(singleplayerServer);
					this.updateForceGameModeButton(singleplayerServer);
				}));

		// Difficulty buttons
		rowHelper.addChild(this.difficultyButtons.layout());

		// GameRules button
		gameRulesButton = rowHelper
				.addChild(Button.builder(Component.translatable("editGamerule.inGame.button"), button -> {
					if (this.minecraft.player != null) {
						this.minecraft.gui.setScreen(new InWorldGameRulesScreen(this.minecraft.player.connection,
								var1x -> this.minecraft.gui.setScreen(this), this));
					}
				}).build());
		this.updateButton(gameRulesButton, singleplayerServer, null,
				Tooltip.create(Component.translatable("editGamerule.inGame.disabled.tooltip")),
				Tooltip.create(Component.translatable("editGamerule.inGame.disabled.hardcore.tooltip")));

		// Restrictions button
		rowHelper.addChild(Button.builder(Component.translatable("restrictions_screen.button"), button -> {
			if (this.minecraft.player != null) {
				this.minecraft.gui.setScreen(new RestrictionsScreen(this, this.minecraft.player.chatAbilities()));
			}
		}).build());

		// Enable PvP button
		this.pvpButton = rowHelper.addChild(CycleButton.onOffBuilder(cfg.enablePvP)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.PvP.info")))
				.create(Component.translatable("mcwifipnp.gui.PvP"), (cycleButton, PvP) -> {
					cfg.enablePvP = PvP;
				}));
		this.updateButton(pvpButton, singleplayerServer, 
				Tooltip.create(Component.translatable("mcwifipnp.gui.PvP.info")),
				Tooltip.create(Component.translatable("editGamerule.inGame.disabled.tooltip")),
				Tooltip.create(Component.translatable("editGamerule.inGame.disabled.hardcore.tooltip")));

		// Apply for All World button
		rowHelper.addChild(CycleButton.onOffBuilder(cfg.applyforallworld)
				.create(Component.translatable("mcwifipnp.gui.applyforallworld"), (cycleButton, applyforallworld) -> {
					cfg.applyforallworld = applyforallworld;
				}));

		// Back to Vanilla Screen button
		rowHelper.addChild(Button.builder(Component.translatable("mcwifipnp.gui.backToVanillaScreen"), button -> {
			this.minecraft.gui
					.setScreen(new MultiplayerOptionsScreen(this));
		}).build());

	}

	private void multiplayerOptions(final LinearLayout content, final @Nullable IntegratedServer singleplayerServer) {
		GridLayout grid = content.addChild(new GridLayout());
		grid.defaultCellSetting().alignHorizontallyCenter();
		RowHelper rowHelper = grid.columnSpacing(8).rowSpacing(4).createRowHelper(2);
		rowHelper.defaultCellSetting().alignHorizontallyCenter();

		rowHelper.addChild(new StringWidget(Component.translatable("options.worldOptions.multiplayer.title")
				.withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD), this.font),
				2);

		// Multiplayer Scope button
		rowHelper.addChild(CycleButton.builder(MultiplayerScope::getDisplayName, cfg.multiplayerScope)
				.withValues(MultiplayerScope.values()).withTooltip(scope -> Tooltip.create(scope.getTooltip()))
				.create(Component.translatable("menu.multiplayerOptions.network"), (cycleButton, value) -> {
					cfg.multiplayerScope = value;
					this.updateGuestCommandAccessButton(singleplayerServer);
					this.updateForceGameModeButton(singleplayerServer);
					this.updateMultiplayerOptions(singleplayerServer);
				}));

		// Guest Command Access button
		this.guestCommandAccessButton = rowHelper.addChild(CycleButton.onOffBuilder(cfg.allowGuestCommands)
				.withTooltip(
						(state) -> Tooltip.create(Component.translatable("options.worldOptions.guest.command_access.tooltip")))
				.create(Component.translatable("options.worldOptions.guest.command_access"),
						(cycleButton, allowGuestCommands) -> {
							cfg.allowGuestCommands = allowGuestCommands;
							initialallowGuestCommands = allowGuestCommands;
							this.updateForceGameModeButton(singleplayerServer);
						}));
		this.updateGuestCommandAccessButton(singleplayerServer);

		// Port field
		portEdit = new EditBox(this.font, Component.translatable("lanServer.port"));
		portEdit.setResponder(value -> {
			this.setPortError(this.portEdit, this.tryParsePort(value));
			portEdit.setHint(Component.literal(String.valueOf(cfg.port)));
		});
		LinearLayout portRow = LinearLayout.vertical().spacing(4);
		portLabel = portRow
				.addChild(new StringWidget(Component.translatable("lanServer.port"), this.font));
		portRow.addChild(portEdit);
		rowHelper.addChild(portRow);

		// Number of players field
		maxPlayersEdit = new EditBox(this.font,
				Component.translatable("mcwifipnp.gui.players"));
		maxPlayersEdit.setResponder(value -> {
			try {
				int parsed = Integer.parseInt(value);
				if (parsed >= 0) {
					cfg.maxPlayers = Integer.parseInt(value);
				}
			} catch (NumberFormatException _) {
			}
		});
		maxPlayersEdit.setHint(Component.literal(String.valueOf(cfg.maxPlayers)));
		maxPlayersEdit.setValue(String.valueOf(cfg.maxPlayers));
		maxPlayersEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.players.info")));
		LinearLayout maxPlayersRow = LinearLayout.vertical().spacing(4);
		maxPlayersLabel = maxPlayersRow
				.addChild(new StringWidget(Component.translatable("mcwifipnp.gui.players"), this.font));
		maxPlayersRow.addChild(maxPlayersEdit);
		rowHelper.addChild(maxPlayersRow);

		// Motd field
		motdEdit = new EditBox(this.font, 308, 20,
				Component.translatable("mcwifipnp.gui.motd"));
		motdEdit.setValue(cfg.motd);
		motdEdit.setHint(Component.literal(cfg.motd));
		motdEdit.setResponder(value -> {
			if (!value.isBlank()) {
				cfg.motd = value;
			}
		});
		motdEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.motd.info")));
		LinearLayout motdRow = LinearLayout.vertical().spacing(4);
		motdLabel = motdRow.addChild(new StringWidget(Component.translatable("mcwifipnp.gui.motd"), this.font));
		motdRow.addChild(motdEdit);
		rowHelper.addChild(motdRow, 2);

		// Force GameMode button
		this.forceGameModeButton = rowHelper.addChild(
				CycleButton.onOffBuilder(cfg.forceGameMode)
						.withTooltip(value -> value
								? Tooltip.create(Component.translatable("options.worldOptions.guest.force_game_mode.on.tooltip"))
								: Tooltip.create(Component.translatable("options.worldOptions.guest.force_game_mode.off.tooltip")))
						.create(Component.translatable("options.worldOptions.guest.force_game_mode"),
								(cycleButton, forceGameMode) -> {
									cfg.forceGameMode = forceGameMode;
								}));
		this.updateForceGameModeButton(singleplayerServer);

		// Enforce Whitelist button
		this.enforceWhitelistButton = rowHelper.addChild(CycleButton.onOffBuilder(cfg.enforceWhitelist)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.Whitelist.info")))
				.create(Component.translatable("mcwifipnp.gui.Whitelist"), (cycleButton, enforceWhitelist) -> {
					cfg.enforceWhitelist = enforceWhitelist;
				}));

		// Online Mode button
		this.onlineModeButton = rowHelper
				.addChild(CycleButton.builder(OnlineMode::getDisplayName, OnlineMode.of(cfg.onlineMode, cfg.enableUUIDFixer))
						.withValues(OnlineMode.values()).withTooltip((OnlineMode) -> Tooltip.create(OnlineMode.gettoolTip()))
						.create(0, 0, 308, 20, Component.translatable("mcwifipnp.gui.OnlineMode"), (cycleButton, onlineMode) -> {
							cfg.onlineMode = onlineMode.onlineMode;
							cfg.enableUUIDFixer = onlineMode.fixUUID;
						}), 2);

		// Use UPnP button
		this.useUPnPButton = rowHelper.addChild(CycleButton.onOffBuilder(cfg.useUPnP)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.UseUPnP.info")))
				.create(Component.translatable("mcwifipnp.gui.UseUPnP"), (cycleButton, useUPnP) -> {
					cfg.useUPnP = useUPnP;
				}));

		// Get Public IP button
		this.getPublicIPButton = rowHelper.addChild(CycleButton.onOffBuilder(cfg.getPublicIP)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.CopyIP.info")))
				.create(Component.translatable("mcwifipnp.gui.CopyIP"), (cycleButton, getPublicIP) -> {
					cfg.getPublicIP = getPublicIP;
				}));

		this.updateMultiplayerOptions(singleplayerServer);

	}

	@Override
	protected void repositionElements() {
		this.scrollArea.arrangeElements();
		this.scrollArea.setMaxHeight(this.layout.getContentHeight());
		this.scrollArea.setMinHeight(this.layout.getContentHeight());
		this.layout.arrangeElements();
	}

	@Override
	protected void extractMenuBackground(final GuiGraphicsExtractor graphics) {
		super.extractMenuBackground(graphics);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				INWORLD_MENU_LIST_BACKGROUND,
				this.layout.getX(),
				this.layout.getHeaderHeight(),
				this.width,
				this.height - this.layout.getFooterHeight(),
				this.width,
				this.layout.getContentHeight(),
				32,
				32);
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int xm, final int ym, final float a) {
		super.extractRenderState(graphics, xm, ym, a);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED, Screen.INWORLD_HEADER_SEPARATOR, this.layout.getX(),
				this.layout.getHeaderHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				Screen.INWORLD_FOOTER_SEPARATOR,
				this.layout.getX(),
				this.height - this.layout.getFooterHeight(),
				0.0F,
				0.0F,
				this.width,
				2,
				32,
				2);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.lastScreen);
	}

	@Override
	public void onGamemasterPermissionChanged(final boolean hasGamemasterPermission) {
		IntegratedServer singleplayerServer = this.minecraft.getSingleplayerServer();
		this.updateButton(this.defaultGameModeButton, singleplayerServer,
				Tooltip.create(Component.translatable("options.worldOptions.game_mode.tooltip")),
				Tooltip.create(Component.translatable("options.worldOptions.game_mode.disabled.operator.tooltip")),
				Tooltip.create(Component.translatable("options.worldOptions.game_mode.disabled.tooltip")));
		this.updateButton(this.personalGameModeButton, singleplayerServer,
				Tooltip.create(Component.translatable("options.worldOptions.personal_game_mode.tooltip")),
				Tooltip.create(Component.translatable("options.worldOptions.game_mode.disabled.operator.tooltip")),
				Tooltip.create(Component.translatable("options.worldOptions.game_mode.disabled.tooltip")));
		this.updateButton(this.gameRulesButton, singleplayerServer, null,
				Tooltip.create(Component.translatable("editGamerule.inGame.disabled.tooltip")),
				Tooltip.create(Component.translatable("editGamerule.inGame.disabled.hardcore.tooltip")));
		this.updateButton(this.pvpButton, singleplayerServer, 
				Tooltip.create(Component.translatable("mcwifipnp.gui.PvP.info")),
				Tooltip.create(Component.translatable("editGamerule.inGame.disabled.tooltip")),
				Tooltip.create(Component.translatable("editGamerule.inGame.disabled.hardcore.tooltip")));
		this.difficultyButtons.refresh(this.minecraft, this);
		if (!hasGamemasterPermission && !this.minecraft.hasSingleplayerServer()) {
			this.minecraft.gui.setScreen(this.lastScreen);
			if (this.minecraft.gui.screen() instanceof HasGamemasterPermissionReaction screen) {
				screen.onGamemasterPermissionChanged(hasGamemasterPermission);
			}
		}
	}

	private void updateButton(
			final @Nullable AbstractWidget widget,
			final @Nullable IntegratedServer singleplayerServer,
			final @Nullable Tooltip tooltip,
			final Tooltip disabledTooltip,
			final Tooltip hardcoreTooltip) {
		if (widget != null) {
			boolean hardcore = singleplayerServer != null && singleplayerServer.isHardcore();
			boolean hasGameMasterPermission = this.minecraft.player != null
					&& this.minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
			widget.active = !hardcore && hasGameMasterPermission;
			widget.setTooltip(hardcore ? hardcoreTooltip : (hasGameMasterPermission ? tooltip : disabledTooltip));
		}
	}

	@Override
	public void added() {
		this.difficultyButtons.refresh(this.minecraft, this);
	}

	@Override
	public void onDifficultyChanged() {
		this.difficultyButtons.refresh(this.minecraft, this);
	}

	private void changeMultiplayerScope(final IntegratedServer singleplayerServer) {
		if (cfg.multiplayerScope != null) {
			if (singleplayerServer.unpublishServer()) {
				this.sendPublishMessage(Component.translatable("menu.multiplayerOptions.publish.stopped"));
				UPnPModule.stop(singleplayerServer);
			}

			if (cfg.multiplayerScope != MultiplayerScope.OFF) {
				this.publish(singleplayerServer, cfg.multiplayerScope);
			}

			this.minecraft.getPlayerSocialManager().getPresenceHandler().tryUpdatePresence();
		}
	}

	private void publish(final IntegratedServer singleplayerServer, final MultiplayerScope scope) {
		if (!singleplayerServer.publishServer(scope, cfg.port)) {
			this.sendPublishMessage(Component.translatable("commands.publish.failed"));
		} else {
			Component message = scope == MultiplayerScope.LAN
					? Component.translatable("menu.multiplayerOptions.publish.started.lan",
							ComponentUtils.copyOnClickText(String.valueOf(cfg.port)))
					: Component.translatable("menu.multiplayerOptions.publish.started.online");
			this.sendPublishMessage(message);
		}
	}

	private void sendPublishMessage(final Component message) {
		this.minecraft.gui.hud.getChat().addClientSystemMessage(message);
		this.minecraft.getNarrator().saySystemQueued(message);
		this.minecraft.updateTitle();
	}

	private void updateMultiplayerOptions(final IntegratedServer singleplayerServer) {
		boolean lanWanted = cfg.multiplayerScope == MultiplayerScope.LAN;
		if (this.portEdit != null) {
			this.portEdit.setValue(lanWanted ? String.valueOf(cfg.port) : "");
			this.portEdit.setEditable(lanWanted);
			this.portEdit.active = lanWanted;
			this.portEdit.setHint(lanWanted ? Component.literal(String.valueOf(cfg.port)) : Component.empty());
			if (!lanWanted) {
				this.portEdit.setFocused(false);
				this.setPortError(this.portEdit, null);
			}
			portEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.port.info")));
		}
		if (this.motdEdit != null) {
			this.motdEdit.setValue(lanWanted ? String.valueOf(cfg.motd) : "");
			this.motdEdit.setEditable(lanWanted);
			this.motdEdit.active = lanWanted;
			this.motdEdit.setHint(lanWanted ? Component.literal(String.valueOf(cfg.motd)) : Component.empty());
			if (!lanWanted) {
				this.motdEdit.setFocused(false);
			}
			motdEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.motd.info")));
		}
		if (this.maxPlayersEdit != null) {
			this.maxPlayersEdit.setValue(lanWanted ? String.valueOf(cfg.maxPlayers) : "");
			this.maxPlayersEdit.setEditable(lanWanted);
			this.maxPlayersEdit.active = lanWanted;
			this.maxPlayersEdit.setHint(lanWanted ? Component.literal(String.valueOf(cfg.maxPlayers)) : Component.empty());
			if (!lanWanted) {
				this.maxPlayersEdit.setFocused(false);
			}
			maxPlayersEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.players.info")));
		}

		if (this.portLabel != null) {
			this.portLabel.setMessage(lanWanted ? Component.translatable("lanServer.port")
					: Component.translatable("lanServer.port").copy().withStyle(ChatFormatting.GRAY));
		}
		if (this.maxPlayersLabel != null) {
			this.maxPlayersLabel.setMessage(lanWanted ? Component.translatable("mcwifipnp.gui.players")
					: Component.translatable("mcwifipnp.gui.players").copy().withStyle(ChatFormatting.GRAY));
		}
		if (this.motdLabel != null) {
			this.motdLabel.setMessage(lanWanted ? Component.translatable("mcwifipnp.gui.motd")
					: Component.translatable("mcwifipnp.gui.motd").copy().withStyle(ChatFormatting.GRAY));
		}
		if (this.getPublicIPButton != null) {
			this.getPublicIPButton.active = lanWanted;
		}
		if (this.useUPnPButton != null) {
			this.useUPnPButton.active = lanWanted;
		}
		if (this.enforceWhitelistButton != null) {
			this.enforceWhitelistButton.active = lanWanted;
		}
		if (this.onlineModeButton != null) {
			this.onlineModeButton.active = lanWanted;
		}
	}

	@Nullable
	private Component tryParsePort(final String value) {
		if (value.isBlank()) {
			return null;
		}
		try {
			int parsed = Integer.parseInt(value);
			if (parsed < 1024 || parsed > 65535) {
				return Component.translatable("lanServer.port.invalid", 1024, 65535);
			}
			if (parsed != this.initialPort && !HttpUtil.isPortAvailable(parsed)) {
				return Component.translatable("lanServer.port.unavailable", 1024, 65535);
			}

			cfg.port = parsed;
			return null;
		} catch (NumberFormatException e) {
			cfg.port = HttpUtil.getAvailablePort();
			return Component.translatable("lanServer.port.invalid", 1024, 65535);
		}
	}

	private void setPortError(final EditBox portEdit, final @Nullable Component errorMessage) {
		if (errorMessage == null) {
			portEdit.setTextColor(-2039584);
			portEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.port.info")));
		} else {
			portEdit.setTextColor(-2142128);
			portEdit.setTooltip(Tooltip.create(errorMessage));
		}
	}

	private void updateGuestCommandAccessButton(final IntegratedServer singleplayerServer) {
		if (this.guestCommandAccessButton != null) {
			boolean lanScope = cfg.multiplayerScope == MultiplayerScope.LAN;
			boolean allowCommands = Boolean.TRUE.equals(cfg.allowHostCommands);
			Tooltip tooltip;
			if (!lanScope) {
				cfg.allowGuestCommands = false;
				tooltip = Tooltip
						.create(Component.translatable("options.worldOptions.guest.command_access.disabled.scope.tooltip"));
			} else if (!allowCommands) {
				cfg.allowGuestCommands = false;
				tooltip = Tooltip
						.create(Component.translatable("options.worldOptions.guest.command_access.disabled.commands.tooltip"));
			} else {
				cfg.allowGuestCommands = initialallowGuestCommands;
				tooltip = Tooltip.create(Component.translatable("options.worldOptions.guest.command_access.tooltip"));
			}

			this.guestCommandAccessButton.setValue(cfg.allowGuestCommands);
			this.guestCommandAccessButton.setTooltip(tooltip);
			this.guestCommandAccessButton.active = lanScope && allowCommands;
		}
	}

	private void updateForceGameModeButton(final IntegratedServer singleplayerServer) {
		if (this.forceGameModeButton != null) {
			boolean lanScope = cfg.multiplayerScope == MultiplayerScope.LAN;
			boolean guestCommandAccess = Boolean.TRUE.equals(cfg.allowGuestCommands);
			Tooltip tooltip;
			if (!lanScope || guestCommandAccess) {
				cfg.forceGameMode = false;
				tooltip = Tooltip
						.create(Component.translatable("options.worldOptions.guest.force_game_mode.off.commands.tooltip"));
			} else {
				cfg.forceGameMode = initialForceGameMode ? false : true;
				tooltip = cfg.forceGameMode
						? Tooltip.create(Component.translatable("options.worldOptions.guest.force_game_mode.on.tooltip"))
						: Tooltip.create(Component.translatable("options.worldOptions.guest.force_game_mode.off.tooltip"));
			}

			this.forceGameModeButton.setValue(cfg.forceGameMode);
			this.forceGameModeButton.setTooltip(tooltip);
			this.forceGameModeButton.active = lanScope && !guestCommandAccess;
		}
	}

	private void GetPublicIP(MinecraftServer server) {
		NameAndId hostPlayer = new NameAndId(server.getSingleplayerProfile());
		if (cfg.getPublicIP) {
			new Thread(() -> {
				server.getPlayerList().getPlayer(hostPlayer.id()).sendSystemMessage(IpCommand.getBrief(server));
			}, "MCWiFiPnP").start();
		}
	}

	private record DifficultyButtons(LayoutElement layout, CycleButton<Difficulty> difficultyButton,
			LockIconButton lockButton, Level level) {
		private static final Component DIFFICULTY_TITLE = Component.translatable("options.difficulty");
		private static final Tooltip DIFFICULTY_DISABLED_HARDCORE_TOOLTIP = Tooltip.create(
				Component.translatable("options.worldOptions.difficulty.disabled.hardcore.tooltip"));
		private static final Tooltip DIFFICULTY_DISABLED_LOCKED_TOOLTIP = Tooltip.create(
				Component.translatable("options.worldOptions.difficulty.disabled.locked.tooltip"));
		private static final Tooltip DIFFICULTY_DISABLED_OPERATOR_TOOLTIP = Tooltip.create(
				Component.translatable("options.worldOptions.difficulty.disabled.operator.tooltip"));
		private static final Component DIFFICULTY_LOCK_TITLE = Component.translatable("difficulty.lock.title");

		public static DifficultyButtons create(final Minecraft minecraft, final Level level,
				final WorldOptionsScreenNew screen) {
			screen.wantedDifficulty = level.getDifficulty();
			screen.initialDifficulty = screen.wantedDifficulty;
			CycleButton<Difficulty> difficultyButton = CycleButton.builder(Difficulty::getDisplayName, level.getDifficulty())
					.withValues(Difficulty.values())
					.create(0, 0, 150, 20, DIFFICULTY_TITLE, (var1, value) -> {
						screen.wantedDifficulty = value;
					});
			screen.wantedDifficultyLocked = isDifficultyLocked(level);
			screen.initialDifficultyLocked = screen.wantedDifficultyLocked;
			LockIconButton lockButton = new LockIconButton(
					0,
					0,
					button -> {
						Component difficultyDisplayName = screen.wantedDifficulty != null ? screen.wantedDifficulty.getDisplayName()
								: level.getDifficulty().getDisplayName();
						minecraft.gui
								.setScreen(
										new Builder(screen, DIFFICULTY_LOCK_TITLE)
												.addMessage(Component.translatable("difficulty.lock.question", difficultyDisplayName))
												.addButton(CommonComponents.GUI_YES, var3x -> {
													if (button instanceof LockIconButton lockIconButton) {
														lockIconButton.setLocked(true);
													}

													screen.wantedDifficultyLocked = true;
													minecraft.gui.setScreen(screen);
												})
												.addButton(CommonComponents.GUI_NO, var3x -> {
													if (button instanceof LockIconButton lockIconButton) {
														lockIconButton.setLocked(false);
													}

													screen.wantedDifficultyLocked = false;
													minecraft.gui.setScreen(screen);
												})
												.build());
					});
			difficultyButton.setWidth(difficultyButton.getWidth() - lockButton.getWidth());
			lockButton.setLocked(isDifficultyLocked(level));
			updateDifficultyButtonsState(minecraft, level, difficultyButton, lockButton);
			EqualSpacingLayout linearLayout = new EqualSpacingLayout(150, 0, Orientation.HORIZONTAL);
			linearLayout.addChild(difficultyButton);
			linearLayout.addChild(lockButton);
			return new DifficultyButtons(linearLayout, difficultyButton, lockButton, level);
		}

		private void refresh(final Minecraft minecraft, final WorldOptionsScreenNew screen) {
			this.difficultyButton
					.setValue(screen.wantedDifficulty != null ? screen.wantedDifficulty : this.level.getDifficulty());
			this.lockButton
					.setLocked(
							screen.wantedDifficultyLocked != null ? screen.wantedDifficultyLocked : isDifficultyLocked(this.level));
			updateDifficultyButtonsState(minecraft, this.level, this.difficultyButton, this.lockButton);
		}

		private static void updateDifficultyButtonsState(
				final Minecraft minecraft, final Level level, final CycleButton<Difficulty> difficultyButton,
				final LockIconButton lockButton) {
			if (minecraft.player == null || !minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
				lockButton.active = false;
				difficultyButton.active = false;
				difficultyButton.setTooltip(DIFFICULTY_DISABLED_OPERATOR_TOOLTIP);
			} else if (level.getLevelData().isDifficultyLocked()) {
				lockButton.active = false;
				difficultyButton.active = false;
				difficultyButton.setTooltip(DIFFICULTY_DISABLED_LOCKED_TOOLTIP);
			} else if (level.getLevelData().isHardcore()) {
				lockButton.active = false;
				difficultyButton.active = false;
				difficultyButton.setTooltip(DIFFICULTY_DISABLED_HARDCORE_TOOLTIP);
			} else {
				lockButton.active = true;
				difficultyButton.active = true;
				difficultyButton.setTooltip(null);
			}
		}

		private static boolean isDifficultyLocked(final Level level) {
			return level.getLevelData().isDifficultyLocked() || level.getLevelData().isHardcore();
		}
	}

	protected void updateDifficulty() {
		if (this.wantedDifficulty != null && this.wantedDifficulty != this.initialDifficulty
				&& this.minecraft.getConnection() != null) {
			this.minecraft.getConnection().send(new ServerboundChangeDifficultyPacket(this.wantedDifficulty));
		}

		if (this.wantedDifficultyLocked != null && this.wantedDifficultyLocked != this.initialDifficultyLocked
				&& this.minecraft.getConnection() != null) {
			this.minecraft.getConnection().send(new ServerboundLockDifficultyPacket(true));
			this.difficultyButtons.lockButton.setLocked(true);
			DifficultyButtons.updateDifficultyButtonsState(
					this.minecraft, this.level, this.difficultyButtons.difficultyButton, this.difficultyButtons.lockButton);
		}
	}

}
