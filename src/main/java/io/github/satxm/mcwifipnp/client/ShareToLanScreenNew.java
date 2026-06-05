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
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.MultiplayerOptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

public class ShareToLanScreenNew extends Screen {
	private final Config cfg;
	private final Screen lastScreen;
	private final boolean serverPublished;

	private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);

	@Nullable
	private TabNavigationBar tabNavigationBar;

	public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

	protected Button applyChanges;

	private EditBox portEdit;
	private StringWidget portLabel;

	private final boolean oldUPnPEnabled;
	private final boolean oldCopyIP;
	private final String oldMotd;
	private final int oldPort;
	private final MinecraftServer.MultiplayerScope oldScope;

	public ShareToLanScreenNew(Screen screen) {
		super(Component.translatable("lanServer.title"));
		this.lastScreen = screen;
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
		this.cfg.save();

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
		this.tabNavigationBar = MenuTabBar.builder(this.tabManager, this.width)
				.addTabs(new DefaultTab1(), new DefaultTab2(), new DefaultTab3()).build();
		this.addRenderableWidget(this.tabNavigationBar);

		// Add footer widgets
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		this.applyChanges = Button
				.builder(Component.translatable("menu.multiplayerOptions.applyChanges"), button -> this.onConfirmClicked())
				.width(150).build();
		footer.addChild(this.applyChanges);
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.tabNavigationBar.selectTab(0, false);
		this.repositionElements();
	}

	private class DefaultTab1 extends GridLayoutTab {
		public DefaultTab1() {
			super(Component.translatable("mcwifipnp.gui.lanServerOptions"));
			GridLayout.RowHelper tabContents = this.layout.columnSpacing(8).rowSpacing(4).createRowHelper(4);

			// Row 1
			// Port field
			portEdit = new EditBox(ShareToLanScreenNew.this.font, Component.translatable("lanServer.port"));
			portEdit.setResponder(value -> {
				ShareToLanScreenNew.this.setPortError(ShareToLanScreenNew.this.tryParsePort(value));
				portEdit.setHint(Component.literal(String.valueOf(cfg.port)));
			});
			LinearLayout portRow = LinearLayout.vertical().spacing(4);
			portLabel = portRow
					.addChild(new StringWidget(Component.translatable("lanServer.port"), ShareToLanScreenNew.this.font));
			portRow.addChild(portEdit);
			ShareToLanScreenNew.this.updatePortControlsState();
			tabContents.addChild(portRow, 2);

			// // Number of players field
			EditBox maxPlayersEdit = new EditBox(ShareToLanScreenNew.this.font,
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
					.addChild(new StringWidget(Component.translatable("mcwifipnp.gui.players"), ShareToLanScreenNew.this.font));
			maxPlayersRow.addChild(maxPlayersEdit);
			tabContents.addChild(maxPlayersRow, 2);

			// Row2
			// Motd field
			EditBox motdEdit = new EditBox(ShareToLanScreenNew.this.font, 308, 20,
					Component.translatable("mcwifipnp.gui.motd"));
			motdEdit.setValue(cfg.motd);
			motdEdit.setHint(Component.literal(cfg.motd));
			motdEdit.setResponder(value -> {
				cfg.motd = value;
			});
			motdEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.motd.info")));
			tabContents.addChild(CommonLayouts.labeledElement(ShareToLanScreenNew.this.font, motdEdit,
					Component.translatable("mcwifipnp.gui.motd")), 4);

			// Row3
			// Allow Cheat button (for other joined players)
			tabContents.addChild(
					CycleButton
							.builder(MinecraftServer.MultiplayerScope::getDisplayName, cfg.multiplayerScope)
							.withValues(MinecraftServer.MultiplayerScope.values())
							.withTooltip(scope -> Tooltip.create(scope.getTooltip()))
							.create(Component.translatable("menu.multiplayerOptions.network"), (cycleButton, value) -> {
								cfg.multiplayerScope = value;
								ShareToLanScreenNew.this.updatePortControlsState();
							}),
					2);

			tabContents.addChild(CycleButton.onOffBuilder(cfg.enforceWhitelist)
					.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.Whitelist.info")))
					.create(Component.translatable("mcwifipnp.gui.Whitelist"), (cycleButton, enforceWhitelist) -> {
						cfg.enforceWhitelist = enforceWhitelist;
					}), 2);

			// Row 4
			tabContents
					.addChild(CycleButton.builder(OnlineMode::getDisplayName, OnlineMode.of(cfg.onlineMode, cfg.enableUUIDFixer))
							.withValues(OnlineMode.values())
							.withTooltip((OnlineMode) -> Tooltip.create(OnlineMode.gettoolTip()))
							.create(Component.translatable("mcwifipnp.gui.OnlineMode"), (cycleButton, onlineMode) -> {
								cfg.onlineMode = onlineMode.onlineMode;
								cfg.enableUUIDFixer = onlineMode.fixUUID;
							}), 2);

		}
	}

	private class DefaultTab2 extends GridLayoutTab {
		public DefaultTab2() {
			super(Component.translatable("menu.multiplayerOptions.otherPlayers.header"));
			GridLayout.RowHelper tabContents = this.layout.columnSpacing(8).rowSpacing(4).createRowHelper(4);

			// Row 1
			// GameMode toggle button
			tabContents.addChild(CycleButton.builder(GameType::getShortDisplayName, cfg.gameMode)
					.withValues(GameType.values())
					.create(Component.translatable("selectWorld.gameMode"), (cycleButton, gameMode) -> {
						cfg.gameMode = gameMode;
					}), 2);

			tabContents.addChild(CycleButton.onOffBuilder(cfg.enablePvP)
					.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.PvP.info")))
					.create(Component.translatable("mcwifipnp.gui.PvP"), (cycleButton, PvP) -> {
						cfg.enablePvP = PvP;
					}), 2);

			// Row 2
			if (!ShareToLanScreenNew.this.serverPublished) {
				tabContents.addChild(CycleButton.onOffBuilder(cfg.allowHostCheat)
						.create(Component.translatable("selectWorld.allowCommands"), (cycleButton, allowHostCheat) -> {
							cfg.allowHostCheat = allowHostCheat;
						}), 2);
			}

			// Allow Cheat button (for other joined players)
			tabContents.addChild(CycleButton.onOffBuilder(cfg.allowEveryoneCheat)
					.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.AllPlayersCheats.info")))
					.create(Component.translatable("mcwifipnp.gui.AllPlayersCheats"), (cycleButton, allowEveryoneCheat) -> {
						cfg.allowEveryoneCheat = allowEveryoneCheat;
					}), 2);
		}
	}

	private class DefaultTab3 extends GridLayoutTab {
		public DefaultTab3() {
			super(Component.translatable("mcwifipnp.gui.otherOptions"));
			GridLayout.RowHelper tabContents = this.layout.columnSpacing(8).rowSpacing(4).createRowHelper(4);

			// Row 1
			tabContents.addChild(CycleButton.onOffBuilder(cfg.useUPnP)
					.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.UseUPnP.info")))
					.create(Component.translatable("mcwifipnp.gui.UseUPnP"), (cycleButton, useUPnP) -> {
						cfg.useUPnP = useUPnP;
					}), 2);

			tabContents.addChild(CycleButton.onOffBuilder(cfg.getPublicIP)
					.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.CopyIP.info")))
					.create(Component.translatable("mcwifipnp.gui.CopyIP"), (cycleButton, getPublicIP) -> {
						cfg.getPublicIP = getPublicIP;
					}), 2);

			// Row 2
			tabContents
					.addChild(Button.builder(Component.translatable("mcwifipnp.gui.backToVanillaScreen"), button -> {
						ShareToLanScreenNew.this.minecraft.gui
								.setScreen(new MultiplayerOptionsScreen(ShareToLanScreenNew.this.lastScreen));
					}).build(), 2);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.lastScreen);
	}

	@Override
	protected void repositionElements() {
		if (this.tabNavigationBar != null) {
			this.tabNavigationBar.arrangeElements(this.width);
			int i = this.tabNavigationBar.getRectangle().bottom();
			ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width,
					this.height - this.layout.getFooterHeight() - i);
			this.tabManager.setTabArea(screenrectangle);
			this.layout.setHeaderHeight(i);
			this.layout.arrangeElements();
		}
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
			final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0,
				this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
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
		boolean published = server.publishServer(scope, cfg.gameMode, cfg.allowEveryoneCheat, cfg.port);
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

}
