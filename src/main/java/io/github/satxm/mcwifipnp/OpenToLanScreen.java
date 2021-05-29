package io.github.satxm.mcwifipnp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.dimension.DimensionType;

public class OpenToLanScreen extends Screen {
	private final MCWiFiPnP.Config cfg;
	private final Screen parent;
	private ButtonWidget buttonStartLanServer;
	private ButtonWidget buttonAllowCommands;
	private ButtonWidget buttonGameMode;
	private ButtonWidget buttonOnlineMode;
	private ButtonWidget buttonUseUPnP;
	private ButtonWidget buttonCopyToClipboard;
	private TextFieldWidget portField;
	private String portinfo = "info";


	public OpenToLanScreen(Screen parent) {
		super(new TranslatableText("lanServer.title", new Object[0]));
		this.parent = parent;


		MinecraftClient client = MinecraftClient.getInstance();
		IntegratedServer server = client.getServer();
		this.cfg = MCWiFiPnP.getConfig(server);

		if (cfg.needsDefaults) {
			cfg.AllowCommands = server.getPlayerManager().areCheatsAllowed();
			cfg.GameMode = server.getWorld(DimensionType.OVERWORLD).getLevelProperties().getGameMode().getName();
			cfg.OnlineMode = server.isOnlineMode();
			cfg.needsDefaults = false;
		}
	}

	protected void init() {
		this.buttonStartLanServer = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 - 155, this.height - 28, 150, 20, I18n.translate("lanServer.start"), (buttonWidget) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			IntegratedServer server = client.getServer();
			cfg.port = Integer.parseInt(portField.getText());
			MCWiFiPnP.openToLan(server);
			this.minecraft.updateWindowTitle();
			this.minecraft.openScreen((Screen) null);
		}));

		this.addButton(new ButtonWidget(this.width / 2 + 5, this.height - 28, 150, 20, I18n.translate("gui.cancel"), (buttonWidget) -> {
			this.minecraft.openScreen(this.parent);
		}));

		this.buttonGameMode = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 - 155, 100, 150, 20, I18n.translate("selectWorld.gameMode"), (buttonWidget) -> {
			if ("spectator".equals(cfg.GameMode)) {
				cfg.GameMode = "creative";
			} else if ("creative".equals(cfg.GameMode)) {
				cfg.GameMode = "adventure";
			} else if ("adventure".equals(cfg.GameMode)) {
				cfg.GameMode = "survival";
			} else {
				cfg.GameMode = "spectator";
			}
			this.updateButtonText();
		}));

		this.buttonAllowCommands = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 + 5, 100, 150, 20, I18n.translate("selectWorld.AllowCommands"), (buttonWidget) -> {
			cfg.AllowCommands = !cfg.AllowCommands;
			this.updateButtonText();
		}));

		this.portField=new TextFieldWidget(this.font, this.width / 2 - 155 , 134 , 150 , 20,I18n.translate("mcwifipnp.gui.port"));
		this.portField.setText(Integer.toString(cfg.port));
		this.portField.setMaxLength(5);
		this.portField.setChangedListener((sPort) -> {
			this.buttonStartLanServer.active = !this.portField.getText().isEmpty();
			try {
				int port =Integer.parseInt(portField.getText());
				if (port < 1024) {
					this.portinfo = "small";
					this.buttonStartLanServer.active = false;
				} else if (port > 65535) {
					this.portinfo = "large";
					this.buttonStartLanServer.active = false;
				} else {
					this.portinfo = "info";
				}
			} catch (NumberFormatException ex) {
				this.buttonStartLanServer.active = false;
				this.portinfo = "null";
			}
		});

		this.buttonOnlineMode = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 + 5, 134, 150, 20, I18n.translate("mcwifipnp.gui.OnlineMode"), (buttonWidget) -> {
			cfg.OnlineMode = !cfg.OnlineMode;
			this.updateButtonText();
		}));

		this.buttonUseUPnP = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 - 155, 168, 150, 20, I18n.translate("mcwifipnp.gui.forwardport"), (buttonWidget) -> {
			cfg.UseUPnP = !cfg.UseUPnP;
			this.updateButtonText();
		}));

		this.buttonCopyToClipboard = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 + 5, 168, 150, 20, I18n.translate("mcwifipnp.gui.CopyIP"), (buttonWidget) -> {
			cfg.CopyToClipboard = !cfg.CopyToClipboard;
			this.updateButtonText();
		}));
		
		this.updateButtonText();
	}

	private void updateButtonText() {
		this.buttonGameMode.setMessage(I18n.translate("selectWorld.gameMode") + ": " + I18n.translate("selectWorld.gameMode." + cfg.GameMode));
		this.buttonAllowCommands.setMessage(I18n.translate("selectWorld.allowCommands") + ' ' + I18n.translate(cfg.AllowCommands ? "options.on" : "options.off"));
		this.buttonOnlineMode.setMessage(I18n.translate("mcwifipnp.gui.OnlineMode") + ": " + I18n.translate(cfg.OnlineMode ? "options.on" : "options.off"));
		this.buttonUseUPnP.setMessage(I18n.translate("mcwifipnp.gui.forwardport") + ": " + I18n.translate(cfg.UseUPnP ? "options.on" : "options.off"));
		this.buttonCopyToClipboard.setMessage(I18n.translate("mcwifipnp.gui.CopyIP") + ": " + I18n.translate(cfg.CopyToClipboard ? "options.on" : "options.off"));
	}

	public void render(int mouseX, int mouseY, float delta) {
		this.renderBackground();
		this.drawCenteredString(this.font, this.title.asFormattedString(), this.width / 2, 50, 16777215);
		this.drawCenteredString(this.font, I18n.translate("lanServer.otherPlayers"), this.width / 2, 82, 16777215);
		this.drawString(this.font, I18n.translate("mcwifipnp.gui.port"), this.width / 2 - 150, 122, 16777215);
		this.drawString(this.font, I18n.translate("selectWorld.allowCommands.info"), this.width / 2 + 10, 122, -6250336);
		this.drawString(this.font, I18n.translate("mcwifipnp.gui.port." + this.portinfo), this.width / 2 - 150, 156, -6250336);
		this.drawString(this.font, I18n.translate("mcwifipnp.gui.OnlineMode.info"), this.width / 2 + 10, 156, -6250336);
		this.drawString(this.font, I18n.translate("mcwifipnp.gui.UseUPnP"), this.width / 2 - 150, 190, -6250336);
		this.drawString(this.font, I18n.translate("mcwifipnp.gui.CopyToClipboard"), this.width / 2 + 10, 190, -6250336);
		portField.render(mouseX, mouseY, delta);
		super.render(mouseX, mouseY, delta);
	}

}
