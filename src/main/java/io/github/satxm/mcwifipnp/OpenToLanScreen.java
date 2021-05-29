package io.github.satxm.mcwifipnp;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

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
		super(new TranslatableText("lanServer.title"));
		this.parent = parent;

		MinecraftClient client = MinecraftClient.getInstance();
		IntegratedServer server = client.getServer();
		this.cfg = MCWiFiPnP.getConfig(server);

		if (cfg.needsDefaults) {
			cfg.AllowCommands = server.getPlayerManager().areCheatsAllowed();
			cfg.GameMode = server.getSaveProperties().getLevelInfo().getGameMode().getName();
			cfg.OnlineMode = server.isOnlineMode();
			cfg.needsDefaults = false;
		}
	}

	protected void init() {
		this.buttonStartLanServer = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 - 155, this.height - 28, 150, 20, new TranslatableText("lanServer.start"), (buttonWidget) -> {
			IntegratedServer server = client.getServer();
			cfg.port = Integer.parseInt(portField.getText());
			MCWiFiPnP.openToLan(server);
			this.client.updateWindowTitle();
			this.client.openScreen((Screen) null);
		}));

		this.addButton(new ButtonWidget(this.width / 2 + 5, this.height - 28, 150, 20, ScreenTexts.CANCEL, (buttonWidget) -> {
			this.client.openScreen(this.parent);
		}));

		this.buttonGameMode = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 - 155, 100, 150, 20, LiteralText.EMPTY, (buttonWidget) -> {
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

		this.buttonAllowCommands = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 + 5, 100, 150, 20, new TranslatableText("selectWorld.AllowCommands"), (buttonWidget) -> {
			cfg.AllowCommands = !cfg.AllowCommands;
			this.updateButtonText();
		}));

		this.portField=new TextFieldWidget(this.client.textRenderer,this.width / 2 - 155,134,150,20,new TranslatableText("mcwifipnp.gui.port"));
		this.portField.setText(Integer.toString(cfg.port));
		this.portField.setMaxLength(5);
		this.addChild(portField);

		portField.setChangedListener((sPort)->{
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

		this.buttonOnlineMode = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 + 5, 134, 150, 20, new TranslatableText("mcwifipnp.gui.OnlineMode"), (buttonWidget) -> {
			cfg.OnlineMode = !cfg.OnlineMode;
			this.updateButtonText();
		}));

		this.buttonUseUPnP = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 - 155, 168, 150, 20, new TranslatableText("mcwifipnp.gui.forwardport"), (buttonWidget) -> {
			cfg.UseUPnP = !cfg.UseUPnP;
			this.updateButtonText();
		}));

		this.buttonCopyToClipboard = (ButtonWidget)this.addButton(new ButtonWidget(this.width / 2 + 5, 168, 150, 20, new TranslatableText("mcwifipnp.gui.CopyIP"), (buttonWidget) -> {
			cfg.CopyToClipboard = !cfg.CopyToClipboard;
			this.updateButtonText();
		}));
		
		this.updateButtonText();
	}

	private void updateButtonText() {
		this.buttonGameMode.setMessage(new TranslatableText("options.generic_value", new Object[]{new TranslatableText("selectWorld.gameMode"), new TranslatableText("selectWorld.gameMode." + cfg.GameMode)}));
		this.buttonAllowCommands.setMessage(ScreenTexts.composeToggleText(new TranslatableText("selectWorld.allowCommands"), cfg.AllowCommands));
		this.buttonOnlineMode.setMessage(ScreenTexts.composeToggleText(new TranslatableText("mcwifipnp.gui.OnlineMode"), cfg.OnlineMode));
		this.buttonUseUPnP.setMessage(ScreenTexts.composeToggleText(new TranslatableText("mcwifipnp.gui.forwardport"), cfg.UseUPnP));
		this.buttonCopyToClipboard.setMessage(ScreenTexts.composeToggleText(new TranslatableText("mcwifipnp.gui.CopyIP"), cfg.CopyToClipboard));
	}

	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 50, 16777215);
		drawCenteredText(matrices, this.textRenderer, new TranslatableText("lanServer.otherPlayers"), this.width / 2, 82, 16777215);
		drawTextWithShadow(matrices, this.textRenderer, new TranslatableText("mcwifipnp.gui.port"), this.width / 2 - 150, 122, 16777215);
		drawTextWithShadow(matrices, this.textRenderer, new TranslatableText("selectWorld.allowCommands.info"), this.width / 2 + 10, 122, -6250336);
		drawTextWithShadow(matrices, this.textRenderer, new TranslatableText("mcwifipnp.gui.port." + this.portinfo), this.width / 2 - 150, 156, -6250336);
		drawTextWithShadow(matrices, this.textRenderer, new TranslatableText("mcwifipnp.gui.OnlineMode.info"), this.width / 2 + 10, 156, -6250336);
		drawTextWithShadow(matrices, this.textRenderer, new TranslatableText("mcwifipnp.gui.UseUPnP"), this.width / 2 - 150, 190, -6250336);
		drawTextWithShadow(matrices, this.textRenderer, new TranslatableText("mcwifipnp.gui.CopyToClipboard"), this.width / 2 + 10, 190, -6250336);
		portField.render(matrices, mouseX, mouseY, delta);
		super.render(matrices, mouseX, mouseY, delta);
	}

}
