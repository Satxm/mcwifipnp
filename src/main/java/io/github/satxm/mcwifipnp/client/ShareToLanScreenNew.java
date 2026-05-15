package io.github.satxm.mcwifipnp.client;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.logging.LogManager;

import org.jspecify.annotations.Nullable;

import io.github.satxm.mcwifipnp.Config;
import io.github.satxm.mcwifipnp.MCWiFiPnPUnit;
import io.github.satxm.mcwifipnp.OnlineMode;
import io.github.satxm.mcwifipnp.commands.IpCommand;
import io.github.satxm.mcwifipnp.network.UPnPModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.MultiplayerOptionsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.server.IntegratedServer.MultiplayerScope;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.resources.Identifier;

public class ShareToLanScreenNew extends Screen {
	private final Config cfg;
	private final Screen lastScreen;
	private final boolean serverPublished;

	private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);

	@Nullable
	private TabNavigationBar tabNavigationBar;

	public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

	protected Button confirmButton;

	@Nullable
	protected Button backToVanillaScreenButton;

	private boolean oldUPnPEnabled;
	private boolean oldCopyIP;
	private String oldMotd;

	@Nullable
	private MultiplayerScope oldScope;

	public ShareToLanScreenNew(Screen screen) {
		super(Component.translatable("lanServer.title"));
		this.lastScreen = screen;
		this.serverPublished = this.minecraft.hasSingleplayerServer()
				&& this.minecraft.getSingleplayerServer().isPublished();

		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();

		this.cfg = Config.read(server);

		if (serverPublished) {
			this.cfg.readFromRunningServer(server);
		} else if (this.cfg.usingDefaults) {
			this.cfg.readFromRunningServer(server);
			this.cfg.port = HttpUtil.getAvailablePort();
			this.cfg.allowHostCheat = server.getWorldData().isAllowCommands();
		}

		this.oldMotd = this.cfg.motd;
		this.oldUPnPEnabled = this.cfg.useUPnP;
		this.oldCopyIP = this.cfg.getPublicIP;
		this.oldScope = server.getMultiplayerScope();
	}

	protected void onConfirmClicked() {
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		PlayerList playerList = server.getPlayerList();
		NameAndId hostPlayer = new NameAndId(server.getSingleplayerProfile());
		this.cfg.save();

		if (cfg.multiplayerScope != oldScope) {
			this.changeMultiplayerScope(server);
		}

		if (this.serverPublished) {
			if (!this.oldMotd.equals(this.cfg.motd) || this.cfg.useUPnP ^ oldUPnPEnabled) {
				// Motd has changed, update UPnP display name
				UPnPModule.stop(server);
				UPnPModule.startIfEnabled(server, cfg);
			}
			if (this.cfg.getPublicIP && this.cfg.getPublicIP ^ oldCopyIP) {
				new Thread(() -> {
					server.getPlayerList().getPlayer(hostPlayer.id()).sendSystemMessage(IpCommand.getBrief(server));
				}, "MCWiFiPnP").start();
			}
		} else if (cfg.multiplayerScope != IntegratedServer.MultiplayerScope.OFF) {
			UPnPModule.startIfEnabled(server, cfg);
			if (this.cfg.getPublicIP) {
				new Thread(() -> {
					server.getPlayerList().getPlayer(hostPlayer.id()).sendSystemMessage(IpCommand.getBrief(server));
				}, "MCWiFiPnP").start();
			}
		}
		this.cfg.applyTo(server);

		if (!this.cfg.allowEveryoneCheat) {
			playerList.getOps().clear();
		}
		if (this.cfg.allowHostCheat) {
			playerList.getOps().add(new ServerOpListEntry(hostPlayer, LevelBasedPermissionSet.OWNER, playerList.canBypassPlayerLimit(hostPlayer)));
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
		this.confirmButton = Button.builder(Component.translatable("menu.multiplayerOptions.applyChanges"), button -> this.onConfirmClicked()).width(150).build();
		footer.addChild(this.confirmButton);
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
			EditBox portField;
			if (ShareToLanScreenNew.this.serverPublished) {
				portField = new EditBox(ShareToLanScreenNew.this.font, 0, 0, 70, 20, Component.translatable("lanServer.port"));
				portField.setEditable(false);
				portField.setValue(Integer.toString(cfg.port));
			} else {
				portField = EditBoxEx
						.numerical(ShareToLanScreenNew.this.font, 0, 0, 70, 20, Component.translatable("lanServer.port"))
						.defaults(cfg.port, EditBoxEx.TEXT_COLOR_HINT,
								Tooltip.create(Component.translatable("mcwifipnp.gui.port.info")))
						.invalid(EditBoxEx.TEXT_COLOR_ERROR,
								Tooltip.create(Component.translatable("mcwifipnp.gui.port.invalid")))
						.validator((port) -> {
							if (port < 1024 || port > 65535) {
								throw new NumberFormatException("Port out of range:" + port);
							} else if (!HttpUtil.isPortAvailable(port)) {
								return new EditBoxEx.ValidatorResult(EditBoxEx.TEXT_COLOR_WARN,
										Tooltip.create(Component.translatable("mcwifipnp.gui.port.unavailable")), false,
										true);
							} else {
								return null;
							}
						}).responder((newState, newPort) -> {
							confirmButton.active = newState.valid();
							if (newState.updateBackendValue())
								cfg.port = newPort;
						});
				portField.setMaxLength(5);
			}
			tabContents.addChild(new StringWidget(portField.getMessage(), ShareToLanScreenNew.this.font),
					1, this.layout.newCellSettings().alignHorizontallyLeft().paddingTop(6));
			tabContents.addChild(portField,
					1, this.layout.newCellSettings().alignHorizontallyRight());

			// Number of players field
			EditBoxEx<Integer> maxPlayersField = EditBoxEx
					.numerical(ShareToLanScreenNew.this.font, 0, 0, 70, 20, Component.translatable("mcwifipnp.gui.players"))
					.bistate(cfg.maxPlayers, Tooltip.create(Component.translatable("mcwifipnp.gui.players.info")),
							(maxPlayers) -> maxPlayers > 0)
					.responder((newState, maxPlayers) -> {
						confirmButton.active = newState.valid();
						if (newState.updateBackendValue())
							cfg.maxPlayers = maxPlayers;
					});
			tabContents.addChild(new StringWidget(maxPlayersField.getMessage(), ShareToLanScreenNew.this.font),
					1, this.layout.newCellSettings().alignHorizontallyLeft().paddingTop(6));
			tabContents.addChild(maxPlayersField,
					1, this.layout.newCellSettings().alignHorizontallyRight());

			// Row2
			// Motd field
			tabContents.addChild(CommonLayouts.labeledElement(ShareToLanScreenNew.this.font, EditBoxEx
					.text(ShareToLanScreenNew.this.font, 0, 0, 308, 20, Component.translatable("mcwifipnp.gui.motd"))
					.bistate(cfg.motd, Tooltip.create(Component.translatable("mcwifipnp.gui.motd.info")), (newMotd) -> true)
					.responder((newState, newMotd) -> {
						confirmButton.active = newState.valid();
						if (newState.updateBackendValue())
							cfg.motd = newMotd;
					}).maxLength(32), Component.translatable("mcwifipnp.gui.motd")), 4);

			// Row3
			// Allow Cheat button (for other joined players)
			tabContents.addChild(
					CycleButton
							.builder(IntegratedServer.MultiplayerScope::getDisplayName, cfg.multiplayerScope)
							.withValues(GetScopeValues())
							.withTooltip(IntegratedServer.MultiplayerScope::getTooltip)
							.create(Component.translatable("menu.multiplayerOptions.network"), (cycleButton, value) -> {
								cfg.multiplayerScope = value;
							}),
					2);

			if (!ShareToLanScreenNew.this.serverPublished) {
				tabContents.addChild(CycleButton.onOffBuilder(cfg.allowHostCheat)
						.create(Component.translatable("selectWorld.allowCommands"), (cycleButton, allowHostCheat) -> {
							cfg.allowHostCheat = allowHostCheat;
						}), 2);
			}

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

			tabContents.addChild(CycleButton.onOffBuilder(cfg.enablePvP)
					.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.PvP.info")))
					.create(Component.translatable("mcwifipnp.gui.PvP"), (cycleButton, PvP) -> {
						cfg.enablePvP = PvP;
					}), 2);
		}
	}

	private class DefaultTab2 extends GridLayoutTab {
		public DefaultTab2() {
			super(Component.translatable("lanServer.otherPlayers"));
			GridLayout.RowHelper tabContents = this.layout.columnSpacing(8).rowSpacing(4).createRowHelper(4);

			// Row 1
			// GameMode toggle button
			tabContents.addChild(CycleButton.builder(GameType::getShortDisplayName, cfg.gameType)
					.withValues(GameType.values())
					.create(Component.translatable("selectWorld.gameMode"), (cycleButton, gameType) -> {
						cfg.gameType = gameType;
					}), 2);

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
			if (!ShareToLanScreenNew.this.serverPublished) {
				tabContents
						.addChild(Button.builder(Component.translatable("mcwifipnp.gui.backToVanillaScreen"), button -> {
							ShareToLanScreenNew.this.minecraft.gui
									.setScreen(new MultiplayerOptionsScreen(ShareToLanScreenNew.this.lastScreen));
						}).build(), 2);
			}
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

	private MultiplayerScope[] GetScopeValues() {
		return this.minecraft.getPlayerSocialManager().isFriendListEnabled()
				? IntegratedServer.MultiplayerScope.values()
				: (IntegratedServer.MultiplayerScope[]) Arrays.stream(IntegratedServer.MultiplayerScope.values())
						.filter(scope -> scope != IntegratedServer.MultiplayerScope.ONLINE)
						.toArray(IntegratedServer.MultiplayerScope[]::new);
	}

	private void changeMultiplayerScope(final IntegratedServer server) {
		switch (cfg.multiplayerScope) {
			case OFF:
				if (server.unpublishServer()) {
					this.sendPublishMessage(Component.translatable("menu.multiplayerOptions.publish.stopped"));
					this.minecraft.getPlayerSocialManager().getPresenceHandler().clearInvites();
					UPnPModule.stop(server);
				}
				break;
			case LAN:
				if (server.unpublishServer()) {
					this.sendPublishMessage(Component.translatable("menu.multiplayerOptions.publish.stopped"));
					this.minecraft.getPlayerSocialManager().getPresenceHandler().clearInvites();
				}

				this.publish(server, IntegratedServer.MultiplayerScope.LAN);
				break;
			case ONLINE:
				if (server.unpublishServer()) {
					this.sendPublishMessage(Component.translatable("menu.multiplayerOptions.publish.stopped"));
					this.minecraft.getPlayerSocialManager().getPresenceHandler().clearInvites();
				}

				this.publish(server, IntegratedServer.MultiplayerScope.ONLINE);
		}

		this.minecraft.getPlayerSocialManager().getPresenceHandler().tryUpdatePresence();
	}

	private void publish(final IntegratedServer server, final IntegratedServer.MultiplayerScope scope) {
		String key = scope == IntegratedServer.MultiplayerScope.LAN
				? "menu.multiplayerOptions.publish.started.lan"
				: "menu.multiplayerOptions.publish.started.online";
		if (server.isPublished()) {
			server.setMultiplayerScope(scope);
			this.sendPublishMessage(
					Component.translatable(key, ComponentUtils.copyOnClickText(String.valueOf(server.getPort()))));
		} else {
			boolean published = server.publishServer(cfg.gameType, cfg.allowEveryoneCheat, cfg.port);
			Component message = published
					? Component.translatable(key, ComponentUtils.copyOnClickText(String.valueOf(cfg.port)))
					: Component.translatable("commands.publish.failed");
			if (published) {
				server.setMultiplayerScope(scope);
			}

			this.sendPublishMessage(message);
		}
	}

	private void sendPublishMessage(final Component message) {
		this.minecraft.gui.hud.getChat().addClientSystemMessage(message);
		this.minecraft.getNarrator().saySystemQueued(message);
		this.minecraft.updateTitle();
	}

}
