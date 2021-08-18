package io.github.satxm.mcwifipnp;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;

public class ShareToLanScreen extends Screen {
	private final MCWiFiPnPUnit.Config cfg;
	private final Screen lastScreen;
	private EditBox EditPort;
	private EditBox EditMotd;
	private Button StartLanServer;
	private Button GameModeButton;
	private Button AllowCommandsButton;
	private Button OnlineModeButton;
	private Button EnablePvPButton;
	private Button UseUPnPButton;
	private Button CopyToClipboardButton;
	private String portinfo = "info";
	private String pvpinfo = "info";

	public ShareToLanScreen(Screen screen) {
		super(new TranslatableComponent("lanServer.title"));
		this.lastScreen = screen;

		Minecraft client = Minecraft.getInstance();
		MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
		this.cfg = MCWiFiPnPUnit.getConfig(server);

		if (cfg.needsDefaults) {
			cfg.AllowCommands = client.getSingleplayerServer().getWorldData().getAllowCommands();
			cfg.GameMode = client.getSingleplayerServer().getWorldData().getGameType().getName();
			cfg.OnlineMode = client.getSingleplayerServer().usesAuthentication();
			cfg.needsDefaults = false;
		}
	}

	protected void init() {
		this.StartLanServer = this.addButton(new Button(this.width / 2 - 155, this.height - 28, 150, 20, new TranslatableComponent("lanServer.start"), (button) -> {
			cfg.port = Integer.parseInt(EditPort.getValue());
			cfg.motd = EditMotd.getValue();
			MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
			MCWiFiPnPUnit.openToLan(server);
			this.minecraft.updateTitle();
			this.minecraft.setScreen((Screen) null);
		}));

		this.addButton(new Button(this.width / 2 + 5, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, (button) -> {
			this.minecraft.setScreen(this.lastScreen);
		}));

		this.GameModeButton = (Button)this.addButton(new Button(this.width / 2 - 155, 32, 150, 20, TextComponent.EMPTY, (button) -> {
			if ("spectator".equals(cfg.GameMode)) {
				cfg.GameMode = "creative";
			} else if ("creative".equals(cfg.GameMode)) {
				cfg.GameMode = "adventure";
			} else if ("adventure".equals(cfg.GameMode)) {
				cfg.GameMode = "survival";
			} else {
				cfg.GameMode = "spectator";
			}
			this.updateSelectionStrings();
		}));

		this.AllowCommandsButton = (Button)this.addButton(new Button(this.width / 2 + 5, 32, 150, 20, new TranslatableComponent("selectWorld.allowCommands"), (button) -> {
			cfg.AllowCommands = !cfg.AllowCommands;
			this.updateSelectionStrings();
		}));

		this.EditPort = new EditBox(this.font, this.width / 2 - 155 , 66 , 150 , 20 , new TextComponent(Integer.toString(cfg.port)));
		this.EditPort.setValue(Integer.toString(cfg.port));
		this.EditPort.setMaxLength(5);
		this.addButton(EditPort);

		EditPort.setResponder((sPort)->{
			this.StartLanServer.active = !this.EditPort.getValue().isEmpty();
			try {
				int port =Integer.parseInt(EditPort.getValue());
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
				this.portinfo = "null";
			}
		});

		this.EditMotd = new EditBox(this.font, this.width / 2 + 5 , 66 , 150 , 20 , new TextComponent(cfg.motd));
		this.EditMotd.setValue(cfg.motd);
		this.addButton(EditMotd);

		EditMotd.setResponder((sMotd)->{
			this.StartLanServer.active = !this.EditMotd.getValue().isEmpty();
		});

		this.OnlineModeButton = (Button)this.addButton(new Button(this.width / 2 - 155, 130, 150, 20, new TranslatableComponent("mcwifipnp.gui.OnlineMode"), (button) -> {
			cfg.OnlineMode = !cfg.OnlineMode;
			this.updateSelectionStrings();
		}));

		this.EnablePvPButton = (Button)this.addButton(new Button(this.width / 2 + 5, 130, 150, 20, new TranslatableComponent("mcwifipnp.gui.EnablePvP"), (button) -> {
			cfg.EnablePvP = !cfg.EnablePvP;
			if (cfg.EnablePvP) {
				this.pvpinfo = "ture";
			} else {
				this.pvpinfo = "false";
			}
			this.updateSelectionStrings();
		}));

		this.UseUPnPButton = (Button)this.addButton(new Button(this.width / 2 - 155, 164, 150, 20, new TranslatableComponent("mcwifipnp.gui.UseUPnP"), (button) -> {
			cfg.UseUPnP = !cfg.UseUPnP;
			this.updateSelectionStrings();
		}));

		this.CopyToClipboardButton = (Button)this.addButton(new Button(this.width / 2 + 5, 164, 150, 20, new TranslatableComponent("mcwifipnp.gui.CopyIP"), (button) -> {
			cfg.CopyToClipboard = !cfg.CopyToClipboard;
			this.updateSelectionStrings();
		}));
		
		this.updateSelectionStrings();
	}

	private void updateSelectionStrings() {
		this.GameModeButton.setMessage(new TranslatableComponent("options.generic_value", new Object[]{new TranslatableComponent("selectWorld.gameMode"), new TranslatableComponent("selectWorld.gameMode." + cfg.GameMode)}));
		this.AllowCommandsButton.setMessage(CommonComponents.optionStatus(new TranslatableComponent("selectWorld.allowCommands"), cfg.AllowCommands));
		this.OnlineModeButton.setMessage(CommonComponents.optionStatus(new TranslatableComponent("mcwifipnp.gui.OnlineMode"), cfg.OnlineMode));
		this.EnablePvPButton.setMessage(CommonComponents.optionStatus(new TranslatableComponent("mcwifipnp.gui.EnablePvP"), cfg.EnablePvP));
		this.UseUPnPButton.setMessage(CommonComponents.optionStatus(new TranslatableComponent("mcwifipnp.gui.UseUPnP"), cfg.UseUPnP));
		this.CopyToClipboardButton.setMessage(CommonComponents.optionStatus(new TranslatableComponent("mcwifipnp.gui.CopyIP"), cfg.CopyToClipboard));
	}

	public void render(PoseStack poseStack, int i, int j, float f) {
		this.renderBackground(poseStack);
		drawCenteredString(poseStack, this.font, this.title, this.width / 2, 15, 16777215);
		drawString(poseStack, this.font, new TranslatableComponent("mcwifipnp.gui.port"), this.width / 2 - 150, 54, 16777215);
		drawString(poseStack, this.font, new TranslatableComponent("mcwifipnp.gui.motd"), this.width / 2 + 10, 54, 16777215);
		drawString(poseStack, this.font, new TranslatableComponent("mcwifipnp.gui.port." + this.portinfo), this.width / 2 - 150, 88, -6250336);
		drawString(poseStack, this.font, new TranslatableComponent("mcwifipnp.gui.motd.info"), this.width / 2 + 10, 88, -6250336);
		drawCenteredString(poseStack, this.font, new TranslatableComponent("lanServer.otherPlayers"), this.width / 2, 112, 16777215);
		drawString(poseStack, this.font, new TranslatableComponent("mcwifipnp.gui.OnlineMode.info"), this.width / 2 - 150, 152, -6250336);
		drawString(poseStack, this.font, new TranslatableComponent("mcwifipnp.gui.EnablePvP." + this.pvpinfo), this.width / 2 + 10, 152, -6250336);
		drawString(poseStack, this.font, new TranslatableComponent("mcwifipnp.gui.UseUPnP.info"), this.width / 2 - 150, 186, -6250336);
		drawString(poseStack, this.font, new TranslatableComponent("mcwifipnp.gui.CopyToClipboard"), this.width / 2 + 10, 186, -6250336);
		EditPort.render(poseStack, i, j, f);
		super.render(poseStack, i, j, f);
	}
}
