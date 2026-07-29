package io.github.satxm.mcwifipnp.client;

import org.jspecify.annotations.Nullable;

import io.github.satxm.mcwifipnp.Config;
import io.github.satxm.mcwifipnp.MCWiFiPnPUnit;
import io.github.satxm.mcwifipnp.OnlineMode;
import io.github.satxm.mcwifipnp.commands.IpCommand;
import io.github.satxm.mcwifipnp.network.UPnPModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import net.minecraft.client.gui.screens.multiplayer.RestrictionsScreen;
import net.minecraft.client.gui.screens.options.InWorldGameRulesScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.server.MinecraftServer;
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

public class WorldOptionsScreenNew extends Screen {
	private final Config cfg;
	private final Screen lastScreen;
	private final Level level;
	private final boolean serverPublished;

	@Nullable
	private TabNavigationBar tabNavigationBar;

	public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private @Nullable ScrollableLayout scrollArea;

	protected Button applyChanges;

	private EditBox portEdit;
	private StringWidget portLabel;

	private final boolean oldUPnPEnabled;
	private final boolean oldCopyIP;
	private final String oldMotd;
	private final int oldPort;
	private final MinecraftServer.MultiplayerScope oldScope;
	private DifficultyButtons difficultyButtons;
	public Difficulty wantedDifficulty;
	public Difficulty initialDifficulty;
	public boolean wantedDifficultyLocked;
	public boolean initialDifficultyLocked;

	public WorldOptionsScreenNew(final Screen lastScreen, final Level level) {
		super(Component.translatable("lanServer.title"));
		this.lastScreen = lastScreen;
		this.level = level;
		this.difficultyButtons = DifficultyButtons.create(this.minecraft, level, this);
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();

		this.serverPublished = this.minecraft.hasSingleplayerServer()
				&& this.minecraft.getSingleplayerServer().isPublished();

		this.cfg = Config.read(server);

		if (serverPublished && server.getMultiplayerScope() == MinecraftServer.MultiplayerScope.LAN) {
			this.cfg.readFromRunningServer(server);
		} else if (this.cfg.usingDefaults) {
			this.cfg.readFromRunningServer(server);
			this.cfg.port = HttpUtil.getAvailablePort();
			this.cfg.allowHostCheat = server.getWorldData().isAllowCommands();
			this.cfg.multiplayerScope = server.getMultiplayerScope();
		}

		this.oldPort = cfg.port;
		this.oldMotd = cfg.motd;
		this.oldUPnPEnabled = cfg.useUPnP;
		this.oldCopyIP = cfg.getPublicIP;
		this.oldScope = server.getMultiplayerScope();
	}

	protected void onConfirmClicked() {
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		PlayerList playerList = server.getPlayerList();
		NameAndId hostPlayer = new NameAndId(server.getSingleplayerProfile());
		updateDifficulty();
		this.cfg.save(server);

		if (cfg.multiplayerScope != oldScope || cfg.port != oldPort) {
			this.changeMultiplayerScope(server);
			if (cfg.multiplayerScope == MinecraftServer.MultiplayerScope.LAN) {
				UPnPModule.startIfEnabled(server, cfg);
				GetPublicIP(server);
			}
		}

		if (this.serverPublished) {
			if (!this.oldMotd.equals(this.cfg.motd) || this.cfg.useUPnP ^ oldUPnPEnabled) {
				// Motd has changed, update UPnP display name
				UPnPModule.stop(server);
				UPnPModule.startIfEnabled(server, cfg);
			}
			if (this.cfg.getPublicIP ^ oldCopyIP) {
				GetPublicIP(server);
			}
		}
		this.cfg.applyTo(server);

		if (!this.cfg.allowEveryoneCheat) {
			playerList.getOps().clear();
		}
		if (this.cfg.allowHostCheat) {
			playerList.getOps().add(new ServerOpListEntry(hostPlayer, LevelBasedPermissionSet.OWNER,
					playerList.canBypassPlayerLimit(hostPlayer)));
		} else {
			playerList.getOps().remove(hostPlayer);
		}
		for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
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
		IntegratedServer singleplayerServer = this.minecraft.getSingleplayerServer();
		this.layout.addTitleHeader(Component.translatable("options.worldOptions.title"), this.font);
		LinearLayout content = LinearLayout.vertical().spacing(8);
		content.defaultCellSetting().padding(8).alignHorizontallyCenter().alignVerticallyTop();
		this.scrollArea = this.layout
				.addToContents(new ScrollableLayout(this.minecraft, content, this.layout.getContentHeight()));

		this.generalOptions(content);
		if (singleplayerServer != null) {
			this.multiplayerOptions(content);
		}

		// Add footer widgets
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		this.applyChanges = Button
				.builder(Component.translatable("menu.multiplayerOptions.applyChanges"), button -> this.onConfirmClicked())
				.build();
		footer.addChild(this.applyChanges);
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	private void generalOptions(final LinearLayout content) {
		GridLayout grid = content.addChild(new GridLayout());
		GridLayout.RowHelper rowHelper = grid.columnSpacing(8).rowSpacing(4).createRowHelper(2);
		rowHelper.defaultCellSetting().alignHorizontallyCenter();

		rowHelper.addChild(new StringWidget(Component.translatable("options.worldOptions.general.title"), this.font), 2);

		Minecraft minecraft = Minecraft.getInstance();
		IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();

		Button gameRulesButton = Button.builder(Component.translatable("editGamerule.inGame.button"), var1 -> {
			if (minecraft.player != null) {
				minecraft.gui.setScreen(new InWorldGameRulesScreen(minecraft.player.connection,
						var1x -> minecraft.gui.setScreen(lastScreen), this));
			}
		}).build();
		rowHelper.addChild(gameRulesButton);
		boolean hardcore = singleplayerServer != null && singleplayerServer.isHardcore();
		boolean hasGameMasterPermission = minecraft.player != null
				&& minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
		gameRulesButton.active = !hardcore && hasGameMasterPermission;
		gameRulesButton
				.setTooltip(hardcore ? Tooltip.create(Component.translatable("editGamerule.inGame.disabled.hardcore.tooltip"))
						: (hasGameMasterPermission ? null
								: Tooltip.create(Component.translatable("editGamerule.inGame.disabled.tooltip"))));

		rowHelper.addChild(this.difficultyButtons.layout());
		// Row 1
		// GameMode toggle button
		rowHelper.addChild(CycleButton.builder(GameType::getShortDisplayName, cfg.gameMode)
				.withValues(GameType.values())
				.create(Component.translatable("selectWorld.gameMode"), (cycleButton, gameMode) -> {
					cfg.gameMode = gameMode;
				}));

		// Row 2
		// Allow Host Cheat button
		rowHelper.addChild(CycleButton.onOffBuilder(cfg.allowHostCheat)
				.create(Component.translatable("selectWorld.allowCommands"), (cycleButton, allowHostCheat) -> {
					cfg.allowHostCheat = allowHostCheat;
				}));

		rowHelper.addChild(CycleButton.onOffBuilder(cfg.enablePvP)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.PvP.info")))
				.create(Component.translatable("mcwifipnp.gui.PvP"), (cycleButton, PvP) -> {
					cfg.enablePvP = PvP;
				}));

		rowHelper.addChild(Button.builder(Component.translatable("restrictions_screen.button"), button -> {
			if (this.minecraft.player != null) {
				this.minecraft.gui.setScreen(new RestrictionsScreen(this, this.minecraft.player.chatAbilities()));
			}
		}).build());

		rowHelper.addChild(CycleButton.onOffBuilder(cfg.applyforallworld)
				.create(Component.translatable("mcwifipnp.gui.applyforallworld"), (cycleButton, applyforallworld) -> {
					cfg.applyforallworld = applyforallworld;
				}));

		rowHelper
				.addChild(Button.builder(Component.translatable("mcwifipnp.gui.backToVanillaScreen"), button -> {
					WorldOptionsScreenNew.this.minecraft.gui
							.setScreen(new WorldOptionsScreen(WorldOptionsScreenNew.this.lastScreen, minecraft.level));
				}).build());

	}

	private void multiplayerOptions(final LinearLayout content) {
		GridLayout grid = content.addChild(new GridLayout());
		GridLayout.RowHelper rowHelper = grid.columnSpacing(8).rowSpacing(4).createRowHelper(2);
		rowHelper.defaultCellSetting().alignHorizontallyCenter();

		rowHelper.addChild(new StringWidget(Component.translatable("options.worldOptions.multiplayer.title"), this.font),
				2);

		// Row3
		// Allow Cheat button (for other joined players)
		rowHelper.addChild(CycleButton.builder(MinecraftServer.MultiplayerScope::getDisplayName, cfg.multiplayerScope)
				.withValues(MinecraftServer.MultiplayerScope.values()).withTooltip(scope -> Tooltip.create(scope.getTooltip()))
				.create(Component.translatable("menu.multiplayerOptions.network"), (cycleButton, value) -> {
					cfg.multiplayerScope = value;
					WorldOptionsScreenNew.this.updatePortControlsState();
				}));

		rowHelper.addChild(CycleButton.onOffBuilder(cfg.allowEveryoneCheat)
				.withTooltip(
						(state) -> Tooltip.create(Component.translatable("options.worldOptions.guest.command_access.tooltip")))
				.create(Component.translatable("options.worldOptions.guest.command_access"),
						(cycleButton, allowEveryoneCheat) -> {
							cfg.allowEveryoneCheat = allowEveryoneCheat;
						}));

		// Row 1
		// Port field
		portEdit = new EditBox(WorldOptionsScreenNew.this.font, Component.translatable("lanServer.port"));
		portEdit.setResponder(value -> {
			WorldOptionsScreenNew.this.setPortError(WorldOptionsScreenNew.this.tryParsePort(value));
			portEdit.setHint(Component.literal(String.valueOf(cfg.port)));
		});
		LinearLayout portRow = LinearLayout.vertical().spacing(4);
		portLabel = portRow
				.addChild(new StringWidget(Component.translatable("lanServer.port"), WorldOptionsScreenNew.this.font));
		portRow.addChild(portEdit);
		WorldOptionsScreenNew.this.updatePortControlsState();
		rowHelper.addChild(portRow);

		// // Number of players field
		EditBox maxPlayersEdit = new EditBox(WorldOptionsScreenNew.this.font,
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
		maxPlayersRow
				.addChild(new StringWidget(Component.translatable("mcwifipnp.gui.players"), WorldOptionsScreenNew.this.font));
		maxPlayersRow.addChild(maxPlayersEdit);
		rowHelper.addChild(maxPlayersRow);

		// Row2
		// Motd field
		EditBox motdEdit = new EditBox(WorldOptionsScreenNew.this.font, 308, 20,
				Component.translatable("mcwifipnp.gui.motd"));
		motdEdit.setValue(cfg.motd);
		motdEdit.setHint(Component.literal(cfg.motd));
		motdEdit.setResponder(value -> {
			cfg.motd = value;
		});
		motdEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.motd.info")));
		rowHelper.addChild(CommonLayouts.labeledElement(WorldOptionsScreenNew.this.font, motdEdit,
				Component.translatable("mcwifipnp.gui.motd")), 2);

		rowHelper.addChild(CycleButton.onOffBuilder(cfg.enforceWhitelist)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.Whitelist.info")))
				.create(Component.translatable("mcwifipnp.gui.Whitelist"), (cycleButton, enforceWhitelist) -> {
					cfg.enforceWhitelist = enforceWhitelist;
				}));

		// Row 4
		rowHelper
				.addChild(CycleButton.builder(OnlineMode::getDisplayName, OnlineMode.of(cfg.onlineMode, cfg.enableUUIDFixer))
						.withValues(OnlineMode.values()).withTooltip((OnlineMode) -> Tooltip.create(OnlineMode.gettoolTip()))
						.create(Component.translatable("mcwifipnp.gui.OnlineMode"), (cycleButton, onlineMode) -> {
							cfg.onlineMode = onlineMode.onlineMode;
							cfg.enableUUIDFixer = onlineMode.fixUUID;
						}));

		// Row 1
		rowHelper.addChild(CycleButton.onOffBuilder(cfg.useUPnP)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.UseUPnP.info")))
				.create(Component.translatable("mcwifipnp.gui.UseUPnP"), (cycleButton, useUPnP) -> {
					cfg.useUPnP = useUPnP;
				}));

		rowHelper.addChild(CycleButton.onOffBuilder(cfg.getPublicIP)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.CopyIP.info")))
				.create(Component.translatable("mcwifipnp.gui.CopyIP"), (cycleButton, getPublicIP) -> {
					cfg.getPublicIP = getPublicIP;
				}));

	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
	}

	@Override
	protected void extractMenuBackground(final GuiGraphicsExtractor graphics) {
		super.extractMenuBackground(graphics);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				AbstractSelectionList.INWORLD_MENU_LIST_BACKGROUND,
				this.layout.getX(),
				this.layout.getHeaderHeight(),
				this.width,
				this.height - this.layout.getFooterHeight() + (int) this.scrollArea.getScrollAmount(),
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

	private void changeMultiplayerScope(final IntegratedServer server) {
		if (server.unpublishServer()) {
			this.sendPublishMessage(Component.translatable("menu.multiplayerOptions.publish.stopped"));
			UPnPModule.stop(server);
		}

		if (cfg.multiplayerScope != MinecraftServer.MultiplayerScope.OFF) {
			this.publish(server, cfg.multiplayerScope);
		}

		this.minecraft.getPlayerSocialManager().getPresenceHandler().tryUpdatePresence();
	}

	private void publish(final IntegratedServer server, final MinecraftServer.MultiplayerScope scope) {
		boolean published = server.publishServer(scope, cfg.allowEveryoneCheat, cfg.port);
		if (!published) {
			this.sendPublishMessage(Component.translatable("commands.publish.failed"));
		} else {
			Component message = scope == MinecraftServer.MultiplayerScope.LAN
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

	private void updatePortControlsState() {
		boolean lanWanted = cfg.multiplayerScope == MinecraftServer.MultiplayerScope.LAN;
		if (this.portEdit != null) {
			this.portEdit.setValue(lanWanted ? String.valueOf(cfg.port) : "");
			this.portEdit.setEditable(lanWanted);
			this.portEdit.active = lanWanted;
			this.portEdit.setHint(lanWanted ? Component.literal(String.valueOf(cfg.port)) : Component.empty());
			if (!lanWanted) {
				this.portEdit.setFocused(false);
				this.setPortError(null);
			}
			portEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.port.info")));
		}

		if (this.portLabel != null) {
			this.portLabel.setMessage(lanWanted ? Component.translatable("lanServer.port")
					: Component.translatable("lanServer.port").copy().withStyle(ChatFormatting.GRAY));
		}
	}

	@Nullable
	private Component tryParsePort(final String value) {
		if (value.isBlank()) {
			return null;
		} else {
			try {
				int parsed = Integer.parseInt(value);
				if (parsed < 1024 || parsed > 65535) {
					return Component.translatable("lanServer.port.invalid", 1024, 65535);
				} else if (parsed != this.oldPort && !HttpUtil.isPortAvailable(parsed)) {
					return Component.translatable("lanServer.port.unavailable", 1024, 65535);
				} else {
					cfg.port = parsed;
					return null;
				}
			} catch (NumberFormatException var3) {
				return Component.translatable("lanServer.port.invalid", 1024, 65535);
			}
		}
	}

	private void setPortError(@Nullable final Component errorMessage) {
		if (portEdit != null) {
			if (errorMessage == null) {
				portEdit.setTextColor(-2039584);
				portEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.port.info")));
			} else {
				portEdit.setTextColor(-2142128);
				portEdit.setTooltip(Tooltip.create(errorMessage));
			}
		}
	}

	private void GetPublicIP(MinecraftServer server) {
		NameAndId hostPlayer = new NameAndId(server.getSingleplayerProfile());
		if (this.cfg.getPublicIP) {
			new Thread(() -> {
				server.getPlayerList().getPlayer(hostPlayer.id()).sendSystemMessage(IpCommand.getBrief(server));
			}, "MCWiFiPnP").start();
		}
	}

	@Environment(EnvType.CLIENT)
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
					button -> minecraft.gui
							.setScreen(
									new PopupScreen.Builder(screen, DIFFICULTY_LOCK_TITLE)
											.addMessage(Component.translatable("difficulty.lock.question",
													level.getLevelData().getDifficulty().getDisplayName()))
											.addButton(CommonComponents.GUI_YES, var3x -> {
												minecraft.gui.setScreen(screen);
												if (button instanceof LockIconButton lockIconButton) {
													lockIconButton.setLocked(true);
												}

												screen.wantedDifficultyLocked = true;
											})
											.addButton(CommonComponents.GUI_NO, var3x -> {
												minecraft.gui.setScreen(screen);
												if (button instanceof LockIconButton lockIconButton) {
													lockIconButton.setLocked(false);
												}

												screen.wantedDifficultyLocked = false;
											})
											.build()));
			difficultyButton.setWidth(difficultyButton.getWidth() - lockButton.getWidth());
			lockButton.setLocked(isDifficultyLocked(level));
			updateDifficultyButtonsState(minecraft, level, difficultyButton, lockButton);
			EqualSpacingLayout linearLayout = new EqualSpacingLayout(150, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
			linearLayout.addChild(difficultyButton);
			linearLayout.addChild(lockButton);
			return new DifficultyButtons(linearLayout, difficultyButton, lockButton, level);
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
		if (this.wantedDifficulty != null && this.wantedDifficulty != this.initialDifficulty && this.minecraft.getConnection() != null) {
			this.minecraft.getConnection().send(new ServerboundChangeDifficultyPacket(this.wantedDifficulty));
		}

		if (this.wantedDifficultyLocked != this.initialDifficultyLocked && this.minecraft.getConnection() != null) {
			this.minecraft.getConnection().send(new ServerboundLockDifficultyPacket(true));
			this.difficultyButtons.lockButton.setLocked(true);
			DifficultyButtons.updateDifficultyButtonsState(
				this.minecraft, this.level, this.difficultyButtons.difficultyButton, this.difficultyButtons.lockButton
			);
		}
	}

}
