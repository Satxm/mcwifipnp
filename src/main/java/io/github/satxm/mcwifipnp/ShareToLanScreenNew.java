package io.github.satxm.mcwifipnp;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.util.HttpUtil;

public class ShareToLanScreenNew extends Screen {
	private final MCWiFiPnPUnit.Config cfg;
	private EditBox EditPort;
	private EditBox EditMotd;
	private EditBox EditPlayers;
	private Button StartLanServer;
	private String portinfo = "info";

	public ShareToLanScreenNew(Screen screen) {
		super(Component.translatable("lanServer.title"));
		
		Minecraft client = Minecraft.getInstance();
		MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
		MCWiFiPnPUnit.ReadingConfig(server);
		this.cfg = MCWiFiPnPUnit.getConfig(server);

		if (cfg.needsDefaults) {
			cfg.port = HttpUtil.getAvailablePort();
			cfg.AllowCommands = client.getSingleplayerServer().getWorldData().getAllowCommands();
			cfg.GameMode = client.getSingleplayerServer().getWorldData().getGameType().getName();
			cfg.OnlineMode = client.getSingleplayerServer().usesAuthentication();
			cfg.needsDefaults = false;
		}
	}

	protected void init() {
		this.StartLanServer = this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 28, 150, 20,
				Component.translatable("lanServer.start"), (button) -> {
					cfg.port = Integer.parseInt(EditPort.getValue());
					cfg.motd = EditMotd.getValue();
					cfg.maxPlayers = Integer.parseInt(EditPlayers.getValue());
					MCWiFiPnPUnit.saveConfig(cfg);
					MCWiFiPnP.openToLan();
					this.minecraft.updateTitle();
					this.minecraft.setScreen((Screen) null);
				}));

		this.addRenderableWidget(
				new Button(this.width / 2 + 5, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, (button) -> {
					this.minecraft.setScreen((Screen) null);
				}));

		this.addRenderableWidget(CycleButton.builder(GameType::getShortDisplayName)
				.withValues(GameType.SURVIVAL, GameType.SPECTATOR, GameType.CREATIVE, GameType.ADVENTURE)
				.withInitialValue(GameType.byName(cfg.GameMode)).create(this.width / 2 - 155, 32, 150, 20,
						Component.translatable("selectWorld.gameMode"), (button, gameMode) -> {
							cfg.GameMode = gameMode.getName();
						}));

		this.addRenderableWidget(CycleButton.onOffBuilder(cfg.AllowCommands).create(this.width / 2 + 5, 32, 150, 20,
				Component.translatable("selectWorld.allowCommands"), (button, AllowCommands) -> {
					cfg.AllowCommands = AllowCommands;
				}));

		this.EditPort = new EditBox(this.font, this.width / 2 - 154, 66, 96, 20,
				Component.literal(Integer.toString(cfg.port)));
		this.EditPort.setValue(Integer.toString(cfg.port));
		this.EditPort.setMaxLength(5);
		this.addRenderableWidget(EditPort);

		this.EditPort.setResponder((sPort) -> {
			this.StartLanServer.active = !this.EditPort.getValue().isEmpty();
			try {
				int port = Integer.parseInt(EditPort.getValue());
				if (port < 1024) {
					this.portinfo = "small";
					this.StartLanServer.active = false;
				} else if (port > 65535) {
					this.portinfo = "large";
					this.StartLanServer.active = false;
				} else {
					this.portinfo = "info";
				}
			} catch (NumberFormatException ex) {
				this.StartLanServer.active = false;
			}
		});

		this.EditPlayers = new EditBox(this.font, this.width / 2 - 48, 66, 96, 20,
				Component.literal(Integer.toString(cfg.maxPlayers)));
		this.EditPlayers.setValue(Integer.toString(cfg.maxPlayers));
		this.addRenderableWidget(EditPlayers);

		this.EditPlayers.setResponder((sPlayers) -> {
			this.StartLanServer.active = !this.EditPlayers.getValue().isEmpty();
			try {
				int players = Integer.parseInt(EditPlayers.getValue());
				if (players <= 0) {
					this.StartLanServer.active = false;
				}
			} catch (NumberFormatException ex) {
				this.StartLanServer.active = false;
			}
		});

		this.EditMotd = new EditBox(this.font, this.width / 2 + 58, 66, 96, 20, Component.literal(cfg.motd));
		this.EditMotd.setValue(cfg.motd);
		this.EditMotd.setMaxLength(32);
		this.addRenderableWidget(EditMotd);

		this.EditMotd.setResponder((sMotd) -> {
			this.StartLanServer.active = !this.EditMotd.getValue().isEmpty();
		});

		this.addRenderableWidget(CycleButton.onOffBuilder(cfg.OnlineMode).create(this.width / 2 - 155, 130, 150, 20,
				Component.translatable("mcwifipnp.gui.OnlineMode"), (button, OnlineMode) -> {
					cfg.OnlineMode = OnlineMode;
				}));

		this.addRenderableWidget(CycleButton.onOffBuilder(cfg.EnablePvP).create(this.width / 2 + 5, 130, 150, 20,
				Component.translatable("mcwifipnp.gui.EnablePvP"), (button, EnablePvP) -> {
					cfg.EnablePvP = EnablePvP;
				}));

		this.addRenderableWidget(CycleButton.onOffBuilder(cfg.UseUPnP).create(this.width / 2 - 155, 164, 150, 20,
				Component.translatable("mcwifipnp.gui.UseUPnP"), (button, UseUPnP) -> {
					cfg.UseUPnP = UseUPnP;
				}));

		this.addRenderableWidget(CycleButton.onOffBuilder(cfg.CopyToClipboard).create(this.width / 2 + 5, 164, 150, 20,
				Component.translatable("mcwifipnp.gui.CopyIP"), (button, CopyToClipboard) -> {
					cfg.CopyToClipboard = CopyToClipboard;
				}));
	}

	public void render(PoseStack poseStack, int i, int j, float f) {
		this.renderBackground(poseStack);
		drawCenteredString(poseStack, this.font, this.title, this.width / 2, 15, 16777215);
		drawString(poseStack, this.font, Component.translatable("mcwifipnp.gui.port"), this.width / 2 - 149, 54,
				16777215);
		drawString(poseStack, this.font, Component.translatable("mcwifipnp.gui.players"), this.width / 2 - 43, 54,
				16777215);
		drawString(poseStack, this.font, Component.translatable("mcwifipnp.gui.motd"), this.width / 2 + 63, 54,
				16777215);
		drawString(poseStack, this.font, Component.translatable("mcwifipnp.gui.port." + this.portinfo),
				this.width / 2 - 150, 88, -6250336);
		drawString(poseStack, this.font, Component.translatable("mcwifipnp.gui.players.info"), this.width / 2 - 43,
				88, -6250336);
		drawString(poseStack, this.font, Component.translatable("mcwifipnp.gui.motd.info"), this.width / 2 + 63, 88,
				-6250336);
		drawCenteredString(poseStack, this.font, Component.translatable("lanServer.otherPlayers"), this.width / 2,
				112, 16777215);
		drawString(poseStack, this.font, Component.translatable("mcwifipnp.gui.OnlineMode.info"),
				this.width / 2 - 150, 152, -6250336);
		drawString(poseStack, this.font, Component.translatable("mcwifipnp.gui.EnablePvP.info"), this.width / 2 + 10,
				152, -6250336);
		drawString(poseStack, this.font, Component.translatable("mcwifipnp.gui.UseUPnP.info"), this.width / 2 - 150,
				186, -6250336);
		drawString(poseStack, this.font, Component.translatable("mcwifipnp.gui.CopyToClipboard"),
				this.width / 2 + 10, 186, -6250336);
		EditPort.render(poseStack, i, j, f);
		EditPlayers.render(poseStack, i, j, f);
		EditMotd.render(poseStack, i, j, f);
		super.render(poseStack, i, j, f);
	}
}
