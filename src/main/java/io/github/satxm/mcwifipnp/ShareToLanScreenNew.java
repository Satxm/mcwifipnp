package io.github.satxm.mcwifipnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;

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
			this.cfg.enableHostCheat = server.getWorldData().isAllowCommands();
		}

		this.oldMotd = this.cfg.motd;
		this.oldUPnPEnabled = this.cfg.UseUPnP;
	}

	protected void onConfirmClicked() {
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		this.cfg.save();

		if (this.serverPublished) {
			if (!this.oldMotd.equals(this.cfg.motd) || this.cfg.UseUPnP ^ oldUPnPEnabled) {
				// Motd has changed, update UPnP display name
				UPnPModule.stop(server);
				UPnPModule.startIfEnabled(server, cfg);
			}
		} else {
			MCWiFiPnPUnit.publishServer(this.cfg);
		}
		this.cfg.applyTo(server);

		this.minecraft.updateTitle();
		this.minecraft.setScreen((Screen) null);
		if (MCWiFiPnPUnit.convertOldUsers(this.minecraft.getSingleplayerServer()))
			this.minecraft.getSingleplayerServer().getProfileCache().save();
	}

	@Override
	protected void init() {
		this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
				.addTabs(new DefaultTab()).build();
		this.addRenderableWidget(this.tabNavigationBar);

		// Add footer widgets
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		this.confirmButton = Button.builder(
				this.serverPublished ? CommonComponents.GUI_DONE : Component.translatable("lanServer.start"),
				button -> this.onConfirmClicked()).width(150).build();
		footer.addChild(this.confirmButton);
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_232903_ -> this.onClose()).build());

		if (this.serverPublished) {
			this.backToVanillaScreenButton = null;
		} else {
			this.backToVanillaScreenButton = SpriteIconButton.builder(CommonComponents.GUI_BACK,
					(button) -> this.minecraft.setScreen(new ShareToLanScreen(this.lastScreen)), true)
				.width(20)
				.sprite(ResourceLocation.tryParse("icon/accessibility"), 15, 15)
				.build();
			this.backToVanillaScreenButton.setTooltip(Tooltip.create(CommonComponents.GUI_BACK));
			this.addRenderableWidget(this.backToVanillaScreenButton);
		}

		this.layout.visitWidgets(widget -> {
			widget.setTabOrderGroup(1);
			this.addRenderableWidget(widget);
		});
		this.tabNavigationBar.selectTab(0, false);
		this.repositionElements();
	}

	private class DefaultTab extends GridLayoutTab {
		public DefaultTab() {
			super(ShareToLanScreenNew.this.title);
			GridLayout.RowHelper tabContents = this.layout.columnSpacing(8).rowSpacing(4).createRowHelper(4);

			// Row 1
			// Port field
			EditBox portField;
			if (ShareToLanScreenNew.this.serverPublished) {
				portField = new EditBox(ShareToLanScreenNew.this.font, 0, 0, 40, 20, Component.translatable("lanServer.port"));
				portField.setEditable(false);
				portField.setValue(Integer.toString(cfg.port));
			} else {
				portField = EditBoxEx.numerical(ShareToLanScreenNew.this.font, 0, 0, 40, 20, Component.translatable("lanServer.port"))
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
				.numerical(ShareToLanScreenNew.this.font, 0, 0, 40, 20, Component.translatable("mcwifipnp.gui.players"))
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
				.text(ShareToLanScreenNew.this.font, 0, 0, 300, 20, Component.translatable("mcwifipnp.gui.motd"))
				.bistate(cfg.motd, Tooltip.create(Component.translatable("mcwifipnp.gui.motd.info")), (newMotd) -> true)
				.responder((newState, newMotd) -> {
					confirmButton.active = newState.valid();
					if (newState.updateBackendValue())
						cfg.motd = newMotd;
				}).maxLength(32), Component.translatable("mcwifipnp.gui.motd")), 4);


			// Row3
			// Allow Cheat button (for other joined players)
			if (!ShareToLanScreenNew.this.serverPublished) {
				tabContents.addChild(CycleButton.onOffBuilder(cfg.enableHostCheat)
					.create(Component.translatable("selectWorld.allowCommands"), (cycleButton, AllowCommands) -> {
						cfg.enableHostCheat = AllowCommands;
					}), 2);
			}

			tabContents.addChild(CycleButton.onOffBuilder(cfg.Whitelist)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.Whitelist.info")))
				.create(Component.translatable("mcwifipnp.gui.Whitelist"), (cycleButton, Whitelist) -> {
					cfg.Whitelist = Whitelist;
				}), 2);

			// Row 4
			tabContents.addChild(CycleButton.builder(OnlineMode::getDisplayName)
				.withValues(OnlineMode.values())
				.withInitialValue(OnlineMode.of(cfg.OnlineMode, cfg.EnableUUIDFixer))
				.withTooltip((OnlineMode) -> Tooltip.create(OnlineMode.gettoolTip()))
				.create(Component.translatable("mcwifipnp.gui.OnlineMode"), (cycleButton, OnlineMode) -> {
					cfg.OnlineMode = OnlineMode.getOnlieMode();
					cfg.EnableUUIDFixer = OnlineMode.getFixUUID();
				}), 2);

			tabContents.addChild(CycleButton.onOffBuilder(cfg.PvP)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.PvP.info")))
				.create(Component.translatable("mcwifipnp.gui.PvP"), (cycleButton, PvP) -> {
					cfg.PvP = PvP;
				}), 2);

			// Row 5
			tabContents.addChild(CycleButton.onOffBuilder(cfg.UseUPnP)
				.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.UseUPnP.info")))
				.create(Component.translatable("mcwifipnp.gui.UseUPnP"), (cycleButton, UseUPnP) -> {
					cfg.UseUPnP = UseUPnP;
				}), 2);

			if (!ShareToLanScreenNew.this.serverPublished) {
				tabContents.addChild(CycleButton.onOffBuilder(cfg.CopyToClipboard)
					.withTooltip((state) -> Tooltip.create(Component.translatable("mcwifipnp.gui.CopyIP.info")))
					.create(Component.translatable("mcwifipnp.gui.CopyIP"), (cycleButton, CopyToClipboard) -> {
						cfg.CopyToClipboard = CopyToClipboard;
					}), 2);
			}


			/*
			 * Settings here only affects newly joined players.
			 * Changing these settings wont affect already joined players.
			 */
			// Row 6
			tabContents.addChild(new StringWidget(Component.translatable("lanServer.otherPlayers"), ShareToLanScreenNew.this.font),
					4, this.layout.newCellSettings().alignHorizontallyCenter().paddingTop(8));

			// Row 7
			// GameMode toggle button
			tabContents.addChild(CycleButton.builder(GameType::getShortDisplayName)
					.withValues(GameType.values()).withInitialValue(cfg.GameMode)
					.create(Component.translatable("selectWorld.gameMode"), (cycleButton, gameMode) -> {
						cfg.GameMode = gameMode;
					}), 2);

			// Allow Cheat button (for other joined players)
			tabContents.addChild(CycleButton.onOffBuilder(cfg.AllPlayersCheats)
					.withTooltip((state) ->Tooltip.create(Component.translatable("mcwifipnp.gui.AllPlayersCheats.info")))
					.create(Component.translatable("mcwifipnp.gui.AllPlayersCheats"), (cycleButton, AllPlayersCheats) -> {
						cfg.AllPlayersCheats = AllPlayersCheats;
					}), 2);
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

		if (this.backToVanillaScreenButton != null) {
			this.backToVanillaScreenButton.setPosition(5, this.confirmButton.getY());
		}
	}
}
