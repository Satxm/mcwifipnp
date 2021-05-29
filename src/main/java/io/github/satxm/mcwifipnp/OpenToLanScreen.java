package io.github.satxm.mcwifipnp;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.GameMode;

public class OpenToLanScreen extends Screen {
	private final MCWiFiPnP.Config cfg;
	private final Screen parent;
	private ButtonWidget buttonStartLanServer;
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
		this.buttonStartLanServer = this.addDrawableChild(new ButtonWidget(this.width / 2 - 155, this.height - 28, 150, 20, new TranslatableText("lanServer.start"), (button) -> {
			IntegratedServer server = client.getServer();
			cfg.port = Integer.parseInt(portField.getText());
			MCWiFiPnP.openToLan(server);
			this.client.updateWindowTitle();
			this.client.openScreen((Screen) null);
		}));

		this.addDrawableChild(new ButtonWidget(this.width / 2 + 5, this.height - 28, 150, 20, ScreenTexts.CANCEL, (button) -> {
			this.client.openScreen(this.parent);
		}));

		this.addDrawableChild(CyclingButtonWidget.builder(GameMode::getSimpleTranslatableName).values(GameMode.SURVIVAL, GameMode.SPECTATOR, GameMode.CREATIVE, GameMode.ADVENTURE).initially(GameMode.byName(cfg.GameMode)).build(this.width / 2 - 155, 100, 150, 20, new TranslatableText("selectWorld.gameMode"), (button, gameMode) -> {
			cfg.GameMode = gameMode.getName();
		}));

		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.AllowCommands).build(this.width / 2 + 5, 100, 150, 20, new TranslatableText("selectWorld.allowCommands"), (button, AllowCommands) -> {
			cfg.AllowCommands = AllowCommands;
		}));

		this.portField=new TextFieldWidget(this.client.textRenderer,this.width / 2 - 155 , 134 , 150 , 20 , new TranslatableText("mcwifipnp.gui.port"));
		this.portField.setText(Integer.toString(cfg.port));
		this.portField.setMaxLength(5);
		this.addDrawableChild(portField);

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

		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.OnlineMode).build(this.width / 2 + 5, 134, 150, 20, new TranslatableText("mcwifipnp.gui.OnlineMode"), (button, OnlineMode) -> {
			cfg.OnlineMode = OnlineMode;
		}));

		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.UseUPnP).build(this.width / 2 - 155, 168, 150, 20, new TranslatableText("mcwifipnp.gui.forwardport"), (button, UseUPnP) -> {
			cfg.UseUPnP = UseUPnP;
		}));

		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.CopyToClipboard).build(this.width / 2 + 5, 168, 150, 20, new TranslatableText("mcwifipnp.gui.CopyIP"), (button, CopyToClipboard) -> {
			cfg.CopyToClipboard = CopyToClipboard;
		}));
		
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
