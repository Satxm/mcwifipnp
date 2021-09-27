package io.github.satxm.mcwifipnp;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ShareToLanScreen extends Screen {
	private final MCWiFiPnPUnit.Config cfg;
	private final Screen lastScreen;
	private TextFieldWidget EditPort;
	private TextFieldWidget EditMotd;
	private Button StartLanServer;
	private Button GameModeButton;
	private Button AllowCommandsButton;
	private Button OnlineModeButton;
	private Button EnablePvPButton;
	private Button UseUPnPButton;
	private Button CopyToClipboardButton;
	private String portinfo = "info";

	public ShareToLanScreen(Screen screen) {
		super(new TranslationTextComponent("lanServer.title"));
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
		this.StartLanServer = this.addButton(new Button(this.width / 2 - 155, this.height - 28, 150, 20, new TranslationTextComponent("lanServer.start"), (button) -> {
			cfg.port = Integer.parseInt(EditPort.getValue());
			cfg.motd = EditMotd.getValue();
			MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
			MCWiFiPnPUnit.openToLan(server);
			this.minecraft.updateTitle();
			this.minecraft.setScreen((Screen) null);
		}));

		this.addButton(new Button(this.width / 2 + 5, this.height - 28, 150, 20, DialogTexts.GUI_CANCEL, (button) -> {
			this.minecraft.setScreen(this.lastScreen);
		}));

		this.GameModeButton = (Button)this.addButton(new Button(this.width / 2 - 155, 32, 150, 20, StringTextComponent.EMPTY, (button) -> {
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

		this.AllowCommandsButton = (Button)this.addButton(new Button(this.width / 2 + 5, 32, 150, 20, new TranslationTextComponent("selectWorld.allowCommands"), (button) -> {
			cfg.AllowCommands = !cfg.AllowCommands;
			this.updateSelectionStrings();
		}));

		this.EditPort = new TextFieldWidget(this.font, this.width / 2 - 155 , 66 , 150 , 20 , new StringTextComponent(Integer.toString(cfg.port)));
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

		this.EditMotd = new TextFieldWidget(this.font, this.width / 2 + 5 , 66 , 150 , 20 , new StringTextComponent(cfg.motd));
		this.EditMotd.setValue(cfg.motd);
		this.addButton(EditMotd);

		EditMotd.setResponder((sMotd)->{
			this.StartLanServer.active = !this.EditMotd.getValue().isEmpty();
		});

		this.OnlineModeButton = (Button)this.addButton(new Button(this.width / 2 - 155, 130, 150, 20, new TranslationTextComponent("mcwifipnp.gui.OnlineMode"), (button) -> {
			cfg.OnlineMode = !cfg.OnlineMode;
			this.updateSelectionStrings();
		}));

		this.EnablePvPButton = (Button)this.addButton(new Button(this.width / 2 + 5, 130, 150, 20, new TranslationTextComponent("mcwifipnp.gui.EnablePvP"), (button) -> {
			cfg.EnablePvP = !cfg.EnablePvP;
			this.updateSelectionStrings();
		}));

		this.UseUPnPButton = (Button)this.addButton(new Button(this.width / 2 - 155, 164, 150, 20, new TranslationTextComponent("mcwifipnp.gui.UseUPnP"), (button) -> {
			cfg.UseUPnP = !cfg.UseUPnP;
			this.updateSelectionStrings();
		}));

		this.CopyToClipboardButton = (Button)this.addButton(new Button(this.width / 2 + 5, 164, 150, 20, new TranslationTextComponent("mcwifipnp.gui.CopyIP"), (button) -> {
			cfg.CopyToClipboard = !cfg.CopyToClipboard;
			this.updateSelectionStrings();
		}));
		
		this.updateSelectionStrings();
	}

	private void updateSelectionStrings() {
		this.GameModeButton.setMessage(new TranslationTextComponent("options.generic_value", new Object[]{new TranslationTextComponent("selectWorld.gameMode"), new TranslationTextComponent("selectWorld.gameMode." + cfg.GameMode)}));
		this.AllowCommandsButton.setMessage(DialogTexts.optionStatus(new TranslationTextComponent("selectWorld.allowCommands"), cfg.AllowCommands));
		this.OnlineModeButton.setMessage(DialogTexts.optionStatus(new TranslationTextComponent("mcwifipnp.gui.OnlineMode"), cfg.OnlineMode));
		this.EnablePvPButton.setMessage(DialogTexts.optionStatus(new TranslationTextComponent("mcwifipnp.gui.EnablePvP"), cfg.EnablePvP));
		this.UseUPnPButton.setMessage(DialogTexts.optionStatus(new TranslationTextComponent("mcwifipnp.gui.UseUPnP"), cfg.UseUPnP));
		this.CopyToClipboardButton.setMessage(DialogTexts.optionStatus(new TranslationTextComponent("mcwifipnp.gui.CopyIP"), cfg.CopyToClipboard));
	}

	public void render(MatrixStack poseStack, int i, int j, float f) {
		this.renderBackground(poseStack);
		drawCenteredString(poseStack, this.font, this.title, this.width / 2, 15, 16777215);
		drawString(poseStack, this.font, new TranslationTextComponent("mcwifipnp.gui.port"), this.width / 2 - 150, 54, 16777215);
		drawString(poseStack, this.font, new TranslationTextComponent("mcwifipnp.gui.motd"), this.width / 2 + 10, 54, 16777215);
		drawString(poseStack, this.font, new TranslationTextComponent("mcwifipnp.gui.port." + this.portinfo), this.width / 2 - 150, 88, -6250336);
		drawString(poseStack, this.font, new TranslationTextComponent("mcwifipnp.gui.motd.info"), this.width / 2 + 10, 88, -6250336);
		drawCenteredString(poseStack, this.font, new TranslationTextComponent("lanServer.otherPlayers"), this.width / 2, 112, 16777215);
		drawString(poseStack, this.font, new TranslationTextComponent("mcwifipnp.gui.OnlineMode.info"), this.width / 2 - 150, 152, -6250336);
		drawString(poseStack, this.font, new TranslationTextComponent("mcwifipnp.gui.EnablePvP.info"), this.width / 2 + 10, 152, -6250336);
		drawString(poseStack, this.font, new TranslationTextComponent("mcwifipnp.gui.UseUPnP.info"), this.width / 2 - 150, 186, -6250336);
		drawString(poseStack, this.font, new TranslationTextComponent("mcwifipnp.gui.CopyToClipboard"), this.width / 2 + 10, 186, -6250336);
		EditPort.render(poseStack, i, j, f);
		super.render(poseStack, i, j, f);
	}
}
