package io.github.satxm.mcwifipnp;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

public class ShareToLanScreenNew extends Screen {
	private final MCWiFiPnPUnit.Config cfg;
	private EditBox EditPort;
	private EditBox EditMotd;
	private EditBox EditPlayers;

	public ShareToLanScreenNew(Screen screen) {
		super(Component.translatable("lanServer.title"));

		Minecraft client = Minecraft.getInstance();
		MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
		MCWiFiPnPUnit.ReadingConfig(server);
		this.cfg = MCWiFiPnPUnit.getConfig(server);

		if (cfg.needsDefaults) {
			cfg.port = HttpUtil.getAvailablePort();
			cfg.AllowCommands = client.getSingleplayerServer().getWorldData().getAllowCommands();
			cfg.GameMode = client.getSingleplayerServer().getDefaultGameType().getName();
			cfg.OnlineMode = client.getSingleplayerServer().usesAuthentication();
			cfg.needsDefaults = false;
		}
	}

	protected void init() {
		Button StartLanServer = Button.builder(Component.translatable("lanServer.start"), button -> {
			if (!EditPort.getValue().isEmpty())
				cfg.port = Integer.parseInt(EditPort.getValue());
			if (!EditPlayers.getValue().isEmpty())
				cfg.maxPlayers = Integer.parseInt(EditPlayers.getValue());
			if (!EditMotd.getValue().isEmpty())
				cfg.motd = EditMotd.getValue();
			MCWiFiPnPUnit.saveConfig(cfg);
			MCWiFiPnPUnit.OpenToLan();
			this.minecraft.updateTitle();
			this.minecraft.setScreen((Screen) null);
			if (MCWiFiPnPUnit.convertOldUsers(this.minecraft.getSingleplayerServer()))
				this.minecraft.getSingleplayerServer().getProfileCache().save();
		}).bounds(this.width / 2 - 155, this.height - 32, 150, 20).build();
		this.addRenderableWidget(StartLanServer);

		this.addRenderableWidget(
				Button.builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen((Screen) null))
						.bounds(this.width / 2 + 5, this.height - 32, 150, 20).build());

		this.addRenderableWidget(CycleButton.builder(GameType::getShortDisplayName)
				.withValues((GameType[]) new GameType[] { GameType.SURVIVAL, GameType.SPECTATOR, GameType.CREATIVE,
						GameType.ADVENTURE })
				.withInitialValue(GameType.byName(cfg.GameMode)).create(this.width / 2 - 155, 36, 150, 20,
						Component.translatable("selectWorld.gameMode"), (cycleButton, gameMode) -> {
							cfg.GameMode = gameMode.getName();
						}));

		this.addRenderableWidget(CycleButton.onOffBuilder(cfg.AllowCommands).create(this.width / 2 + 5, 36, 150, 20,
				Component.translatable("selectWorld.allowCommands"), (cycleButton, AllowCommands) -> {
					cfg.AllowCommands = AllowCommands;
				}));

		this.EditPort = new EditBox(this.font, this.width / 2 - 154, 70, 96, 20,
				Component.translatable("mcwifipnp.gui.port"));
		this.EditPort.setHint(Component.literal(Integer.toString(cfg.port)).withStyle(ChatFormatting.DARK_GRAY));
		this.EditPort.setMaxLength(5);
		this.EditPort.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.port.info")));
		this.addRenderableWidget(EditPort);

		this.EditPort.setResponder(sPort -> {
			if (sPort.isBlank()) {
				this.EditPort.setTextColor(0xE0E0E0);
				this.EditPort
						.setHint(Component.literal(Integer.toString(cfg.port)).withStyle(ChatFormatting.DARK_GRAY));
				this.EditPort
						.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.port.info")));
				StartLanServer.active = true;
			} else {
				try {
					int port = Integer.parseInt(sPort);
					if (port < 1024 || port > 65535) {
						this.EditPort.setTextColor(0xFF5555);
						this.EditPort.setTooltip(
								Tooltip.create(Component.translatable("mcwifipnp.gui.port.invalid")));
						StartLanServer.active = false;
					} else if (!HttpUtil.isPortAvailable(port)) {
						this.EditPort.setTextColor(0xFF5555);
						this.EditPort.setTooltip(
								Tooltip.create(Component.translatable("mcwifipnp.gui.port.unavailable")));
						StartLanServer.active = false;
					} else {
						this.EditPort.setTextColor(0xE0E0E0);
						this.EditPort.setTooltip(
								Tooltip.create(Component.translatable("mcwifipnp.gui.port.info")));
						StartLanServer.active = true;
					}
				} catch (NumberFormatException ex) {
					this.EditPort.setTextColor(0xFF5555);
					this.EditPort.setTooltip(
							Tooltip.create(Component.translatable("mcwifipnp.gui.port.invalid")));
					StartLanServer.active = false;
				}
			}
		});

		this.EditPlayers = new EditBox(this.font, this.width / 2 - 48, 70, 96, 20,
				Component.translatable("mcwifipnp.gui.players"));
		this.EditPlayers
				.setHint(Component.literal(Integer.toString(cfg.maxPlayers)).withStyle(ChatFormatting.DARK_GRAY));
		this.EditPlayers.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.players.info")));
		this.addRenderableWidget(EditPlayers);

		this.EditPlayers.setResponder((sPlayers) -> {
			if (sPlayers.isBlank()) {
				this.EditPlayers.setHint(
						Component.literal(Integer.toString(cfg.maxPlayers)).withStyle(ChatFormatting.DARK_GRAY));
			} else {
				try {
					int players = Integer.parseInt(EditPlayers.getValue());
					if (players < 0) {
						StartLanServer.active = false;
					}
					StartLanServer.active = true;
				} catch (NumberFormatException ex) {
					StartLanServer.active = false;
				}
			}
		});

		this.EditMotd = new EditBox(this.font, this.width / 2 + 58, 70, 96, 20,
				Component.translatable("mcwifipnp.gui.motd"));
		this.EditMotd.setHint(Component.literal(cfg.motd).withStyle(ChatFormatting.DARK_GRAY));
		this.EditMotd.setMaxLength(32);
		this.EditMotd.setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.motd.info")));
		this.addRenderableWidget(EditMotd);

		this.EditMotd.setResponder((sMotd) -> {
			if (sMotd.isBlank()) {
				this.EditMotd.setHint(Component.literal(cfg.motd).withStyle(ChatFormatting.DARK_GRAY));
			}
		});

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

		this.addRenderableWidget(CycleButton.onOffBuilder(cfg.OnlineMode).create(this.width / 2 - 155, 148, 150, 20,
				Component.translatable("mcwifipnp.gui.OnlineMode"), (cycleButton, OnlineMode) -> {
					cfg.OnlineMode = OnlineMode;
				})).setTooltip(Tooltip.create(Component.translatable("mcwifipnp.gui.OnlineMode.info")));

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

	public void render(GuiGraphics guiGraphics, int i, int j, float f) {
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 16777215);
		guiGraphics.drawString(this.font, Component.translatable("mcwifipnp.gui.port"), this.width / 2 - 149, 58,
				16777215);
		guiGraphics.drawString(this.font, Component.translatable("mcwifipnp.gui.players"), this.width / 2 - 43, 58,
				16777215);
		guiGraphics.drawString(this.font, Component.translatable("mcwifipnp.gui.motd"), this.width / 2 + 63, 58,
				16777215);
		guiGraphics.drawCenteredString(this.font, Component.translatable("lanServer.otherPlayers"), this.width / 2, 104,
				16777215);
		EditPort.render(guiGraphics, i, j, f);
		EditPlayers.render(guiGraphics, i, j, f);
		EditMotd.render(guiGraphics, i, j, f);
		super.render(guiGraphics, i, j, f);
	}

}
