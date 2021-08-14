package io.github.satxm.mcwifipnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.dimension.DimensionType;

public class ShareToLanScreen extends Screen {
	private final MCWiFiPnP.Config cfg;
	private final Screen lastScreen;
	private TextFieldWidget EditPort;
	private Button StartLanServer;
	private Button GameModeButton;
	private Button AllowCommandsButton;
	private Button OnlineModeButton;
	private Button UseUPnPButton;
	private Button CopyToClipboardButton;
	private String portinfo = "info";

	public ShareToLanScreen(Screen screen) {
		super(new TranslationTextComponent("lanServer.title"));
		this.lastScreen = screen;

		Minecraft client = Minecraft.getInstance();
		MinecraftServer server = client.getIntegratedServer();
		this.cfg = MCWiFiPnP.getConfig(server);

		if (cfg.needsDefaults) {
			cfg.AllowCommands = server.getWorld(DimensionType.OVERWORLD).getWorldInfo().areCommandsAllowed();
			cfg.GameMode = server.getWorld(DimensionType.OVERWORLD).getWorldInfo().getGameType().getName();
			cfg.OnlineMode = server.isServerInOnlineMode();
			cfg.needsDefaults = false;
		}
	}

	protected void init() {
		this.StartLanServer = this.addButton(new Button(this.width / 2 - 155, this.height - 28, 150, 20, I18n.format("lanServer.start"), (button) -> {
			cfg.port = Integer.parseInt(EditPort.getText());
			MinecraftServer server = Minecraft.getInstance().getIntegratedServer();
			MCWiFiPnP.openToLan(server);
			this.minecraft.func_230150_b_();
			this.minecraft.displayGuiScreen((Screen) null);
		}));

		this.addButton(new Button(this.width / 2 + 5, this.height - 28, 150, 20, I18n.format("gui.cancel"), (button) -> {
			this.minecraft.displayGuiScreen(this.lastScreen);
		}));

		this.GameModeButton = (Button)this.addButton(new Button(this.width / 2 - 155, 100, 150, 20, I18n.format("selectWorld.gameMode"), (button) -> {
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

		this.AllowCommandsButton = (Button)this.addButton(new Button(this.width / 2 + 5, 100, 150, 20, I18n.format("selectWorld.allowCommands"), (button) -> {
			cfg.AllowCommands = !cfg.AllowCommands;
			this.updateSelectionStrings();
		}));

		this.EditPort = new TextFieldWidget(this.font, this.width / 2 - 155 , 134 , 150 , 20 , I18n.format("mcwifipnp.gui.port"));
		this.EditPort.setText(Integer.toString(cfg.port));
		this.EditPort.setMaxStringLength(5);
		this.addButton(EditPort);

		EditPort.setResponder((sPort)->{
			this.StartLanServer.active = !this.EditPort.getText().isEmpty();
			try {
				int port =Integer.parseInt(EditPort.getText());
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

		this.OnlineModeButton = (Button)this.addButton(new Button(this.width / 2 + 5, 134, 150, 20, I18n.format("mcwifipnp.gui.OnlineMode"), (button) -> {
			cfg.OnlineMode = !cfg.OnlineMode;
			this.updateSelectionStrings();
		}));

		this.UseUPnPButton = (Button)this.addButton(new Button(this.width / 2 - 155, 168, 150, 20, I18n.format("mcwifipnp.gui.forwardport"), (button) -> {
			cfg.UseUPnP = !cfg.UseUPnP;
			this.updateSelectionStrings();
		}));

		this.CopyToClipboardButton = (Button)this.addButton(new Button(this.width / 2 + 5, 168, 150, 20, I18n.format("mcwifipnp.gui.CopyIP"), (button) -> {
			cfg.CopyToClipboard = !cfg.CopyToClipboard;
			this.updateSelectionStrings();
		}));
		
		this.updateSelectionStrings();
	}

	private void updateSelectionStrings() {
		this.GameModeButton.setMessage(I18n.format("selectWorld.gameMode") + ": " + I18n.format("selectWorld.gameMode." + cfg.GameMode));
		this.AllowCommandsButton.setMessage(I18n.format("selectWorld.allowCommands") + ' ' + I18n.format(cfg.AllowCommands ? "options.on" : "options.off"));
		this.OnlineModeButton.setMessage(I18n.format("mcwifipnp.gui.OnlineMode") + ": " + I18n.format(cfg.OnlineMode ? "options.on" : "options.off"));
		this.UseUPnPButton.setMessage(I18n.format("mcwifipnp.gui.forwardport") + ": " + I18n.format(cfg.UseUPnP ? "options.on" : "options.off"));
		this.CopyToClipboardButton.setMessage(I18n.format("mcwifipnp.gui.CopyIP") + ": " + I18n.format(cfg.CopyToClipboard ? "options.on" : "options.off"));
	}

	public void render(int i, int j, float f) {
		this.renderBackground();
		drawCenteredString(this.font, this.title.getFormattedText(), this.width / 2, 50, 16777215);
		drawCenteredString(this.font, I18n.format("lanServer.otherPlayers"), this.width / 2, 82, 16777215);
		drawString(this.font, I18n.format("mcwifipnp.gui.port"), this.width / 2 - 150, 122, 16777215);
		drawString(this.font, I18n.format("selectWorld.allowCommands.info"), this.width / 2 + 10, 122, -6250336);
		drawString(this.font, I18n.format("mcwifipnp.gui.port." + this.portinfo), this.width / 2 - 150, 156, -6250336);
		drawString(this.font, I18n.format("mcwifipnp.gui.OnlineMode.info"), this.width / 2 + 10, 156, -6250336);
		drawString(this.font, I18n.format("mcwifipnp.gui.UseUPnP"), this.width / 2 - 150, 190, -6250336);
		drawString(this.font, I18n.format("mcwifipnp.gui.CopyToClipboard"), this.width / 2 + 10, 190, -6250336);
		EditPort.render(i, j, f);
		super.render(i, j, f);
	}
}
