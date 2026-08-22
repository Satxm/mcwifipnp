package io.github.satxm.mcwifipnp.client;

import javax.annotation.Nullable;

import io.github.satxm.mcwifipnp.Config;
import io.github.satxm.mcwifipnp.MCWiFiPnPUnit;
import io.github.satxm.mcwifipnp.OnlineMode;
import io.github.satxm.mcwifipnp.commands.IpCommand;
import io.github.satxm.mcwifipnp.network.UPnPModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.server.level.ServerPlayer;
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

	protected Button confirmButton;

	protected Checkbox removePlayerReportingButtonBox;
	protected EditBox portEdit;

	@Nullable
	protected Button backToVanillaScreenButton;

	private boolean oldUPnPEnabled;
	private boolean oldCopyIP;
	private String oldMotd;

	public ShareToLanScreenNew(Screen screen, boolean serverPublished) {
		super(Component.translatable("lanServer.title"));
		this.lastScreen = screen;
		this.serverPublished = serverPublished;

		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();

		this.cfg = Config.read(server);

		if (serverPublished) {
			this.cfg.readFromRunningServer(server);
		} else if (this.cfg.usingDefaults) {
			this.cfg.readFromRunningServer(server);
			this.cfg.port = HttpUtil.getAvailablePort();
			this.cfg.allowHostCommands = server.getWorldData().isAllowCommands();
		}

		this.oldMotd = this.cfg.motd;
		this.oldUPnPEnabled = this.cfg.useUPnP;
		this.oldCopyIP = this.cfg.getPublicIP;
	}

	protected void onConfirmClicked() {
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		PlayerList playerList = server.getPlayerList();
		NameAndId hostPlayer = new NameAndId(server.getSingleplayerProfile());
		this.cfg.save(server);

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
		} else {
			// Publish server
			MutableComponent component = server.publishServer(this.cfg.gameType, this.cfg.allowGuestCommands, this.cfg.port)
					? PublishCommand.getSuccessMessage(this.cfg.port)
					: Component.translatable("commands.publish.failed");
			this.minecraft.gui.getChat().addMessage(component);

			UPnPModule.startIfEnabled(server, cfg);
			if (this.cfg.getPublicIP) {
				new Thread(() -> {
					server.getPlayerList().getPlayer(hostPlayer.id()).sendSystemMessage(IpCommand.getBrief(server));
				}, "MCWiFiPnP").start();
			}
		}
		this.cfg.applyTo(server);

		if (!this.cfg.allowGuestCommands) {
			playerList.getOps().clear();
		}
		if (this.cfg.allowHostCommands) {
			playerList.getOps().add(new ServerOpListEntry(hostPlayer, 4, playerList.canBypassPlayerLimit(hostPlayer)));
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
		this.minecraft.setScreen((Screen) null);
	}

	@Override
	protected void init() {
		this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
				.addTabs(new DefaultTab1(), new DefaultTab2(), new DefaultTab3()).build();
		this.addRenderableWidget(this.tabNavigationBar);

		// Add footer widgets
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		this.confirmButton = Button.builder(
				this.serverPublished ? CommonComponents.GUI_DONE : Component.translatable("lanServer.start"),
				button -> this.onConfirmClicked()).width(150).build();
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
			portEdit = new EditBox(ShareToLanScreenNew.this.font, 150, 20, Component.translatable("lanServer.port"));
			portEdit.setValue(Integer.toString(cfg.port));
			portEdit.setResponder(value -> {
				setPortError(tryParsePort(value));
				portEdit.setHint(Component.literal(String.valueOf(cfg.port)));
			});
			portEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.port.info")));

			LinearLayout portRow = LinearLayout.vertical().spacing(4);
			portRow.addChild(new StringWidget(Component.translatable("lanServer.port"), ShareToLanScreenNew.this.font));
			portRow.addChild(portEdit);
			if (ShareToLanScreenNew.this.serverPublished)
				portEdit.setEditable(false);
			tabContents.addChild(portRow, 2);

			// Number of players field
			EditBox maxPlayersEdit = new EditBox(ShareToLanScreenNew.this.font, 150, 20,
					Component.translatable("mcwifipnp.gui.players"));
			maxPlayersEdit.setResponder(value -> {
				try {
					int parsed = Integer.parseInt(value);
					if (parsed >= 0) {
						cfg.maxPlayers = Integer.parseInt(value);
					}
				} catch (NumberFormatException e) {
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
				if (!value.isBlank()) {
					cfg.motd = value;
				}
			});
			motdEdit.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.motd.info")));
			LinearLayout motdRow = LinearLayout.vertical().spacing(4);
			motdRow.addChild(new StringWidget(Component.translatable("mcwifipnp.gui.motd"), ShareToLanScreenNew.this.font));
			motdRow.addChild(motdEdit);
			tabContents.addChild(motdRow, 4);

			// Row3
			// Allow Cheat button (for other joined players)
			if (!ShareToLanScreenNew.this.serverPublished) {
				tabContents.addChild(CycleButton.onOffBuilder(cfg.allowHostCommands)
						.create(Component.translatable("selectWorld.allowCommands"), (cycleButton, allowHostCommands) -> {
							cfg.allowHostCommands = allowHostCommands;
						}), 2);
			}

			tabContents.addChild(CycleButton.onOffBuilder(cfg.enforceWhitelist)
					.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.Whitelist.info")))
					.create(Component.translatable("mcwifipnp.gui.Whitelist"), (cycleButton, enforceWhitelist) -> {
						cfg.enforceWhitelist = enforceWhitelist;
					}), 2);

			// Row 4
			tabContents.addChild(CycleButton.builder(OnlineMode::getDisplayName)
					.withValues(OnlineMode.values())
					.withInitialValue(OnlineMode.of(cfg.onlineMode, cfg.enableUUIDFixer))
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
			tabContents.addChild(CycleButton.builder(GameType::getShortDisplayName)
					.withValues(GameType.values()).withInitialValue(cfg.gameType)
					.create(Component.translatable("selectWorld.gameMode"), (cycleButton, gameType) -> {
						cfg.gameType = gameType;
					}), 2);

			// Allow Cheat button (for other joined players)
			tabContents.addChild(CycleButton.onOffBuilder(cfg.allowGuestCommands)
					.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.AllPlayersCheats.info")))
					.create(Component.translatable("mcwifipnp.gui.AllPlayersCheats"), (cycleButton, allowGuestCommands) -> {
						cfg.allowGuestCommands = allowGuestCommands;
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
			tabContents.addChild(CycleButton.onOffBuilder(cfg.removePlayerReportingButton)
					.create(Component.translatable("mcwifipnp.gui.removePlayerReportingButton"),
							(cycleButton, removePlayerReportingButton) -> {
								cfg.removePlayerReportingButton = removePlayerReportingButton;
							}),
					2);

			// Apply for All World button
			tabContents.addChild(CycleButton.onOffBuilder(cfg.applyforallworld)
					.create(Component.translatable("mcwifipnp.gui.applyforallworld"), (cycleButton, applyforallworld) -> {
						cfg.applyforallworld = applyforallworld;
					}), 2);

			if (!ShareToLanScreenNew.this.serverPublished) {
				tabContents
						.addChild(Button.builder(Component.translatable("mcwifipnp.gui.backToVanillaScreen"), button -> {
							ShareToLanScreenNew.this.minecraft.setScreen(new ShareToLanScreen(ShareToLanScreenNew.this.lastScreen));
						}).build(), 2);
			}
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.lastScreen);
	}

	@Override
	protected void repositionElements() {
		if (this.tabNavigationBar != null) {
			this.tabNavigationBar.setWidth(this.width);
			this.tabNavigationBar.arrangeElements();
			int i = this.tabNavigationBar.getRectangle().bottom();
			ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width,
					this.height - this.layout.getFooterHeight() - i);
			this.tabManager.setTabArea(screenrectangle);
			this.layout.setHeaderHeight(i);
			this.layout.arrangeElements();
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int i, int j, float f) {
		super.render(guiGraphics, i, j, f);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0,
				this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
	}

	@Nullable
	private Component tryParsePort(final String value) {
		if (value.isBlank()) {
			return null;
		} else {
			try {
				int parsed = Integer.parseInt(value);
				if (parsed < 1024 || parsed > 65535) {
					return Component.translatable("mcwifipnp.gui.port.invalid");
				} else if (!HttpUtil.isPortAvailable(parsed)) {
					return Component.translatable("mcwifipnp.gui.port.unavailable");
				} else {
					cfg.port = parsed;
					return null;
				}
			} catch (NumberFormatException var3) {
				return Component.translatable("mcwifipnp.gui.port.invalid");
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

}
