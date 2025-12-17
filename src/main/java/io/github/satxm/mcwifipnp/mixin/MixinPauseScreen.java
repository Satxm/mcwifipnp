package io.github.satxm.mcwifipnp.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import io.github.satxm.mcwifipnp.Config;
import io.github.satxm.mcwifipnp.client.GuiUtils;
import io.github.satxm.mcwifipnp.client.ShareToLanScreenNew;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {
	private Config cfg;

	protected MixinPauseScreen(Component title) {
		super(title);
	}

	@Inject(method = "createPauseMenu", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	protected void addOrReplaceButton(CallbackInfo ci, GridLayout gridLayout, GridLayout.RowHelper dummy) {

		IntegratedServer server = this.minecraft.getSingleplayerServer();
		if (this.minecraft.hasSingleplayerServer()) {
			this.cfg = Config.read(server);
		}

		Component MODIFY_LAN_OPTIONS = this.minecraft.hasSingleplayerServer()
				&& this.minecraft.getSingleplayerServer().isPublished()
						? Component.translatable("mcwifipnp.gui.lanServerOptions")
						: Component.translatable("menu.shareToLan");

		// Add a new button to the pause screen to open Lan Options Screen.
		if (this.minecraft.hasSingleplayerServer() && this.minecraft.getSingleplayerServer().isPublished()
				&& !cfg.removePlayerReportingButton) {
			Button optionButton = GuiUtils.findWidget(this.children(), Button.class, "menu.options");

			if (optionButton != null) {
				SpriteIconButton lanServerSettings = SpriteIconButton
						.builder(MODIFY_LAN_OPTIONS,
								(button) -> this.minecraft.setScreen(new ShareToLanScreenNew(this, true)), true)
						.width(20).sprite(Identifier.tryParse("icon/language"), 15, 15).build();
				lanServerSettings.setPosition(this.width / 2 - 124, optionButton.getY());
				lanServerSettings.setTooltip(Tooltip.create(MODIFY_LAN_OPTIONS));
				this.addRenderableWidget(lanServerSettings);
			}
		}

		// Replace the vanilla "Open to Lan" or "Player Reporting" button.
		final List<LayoutElement> elements = ((AccessorGridLayout) gridLayout).getChildren();
		Button oldButton = GuiUtils.findWidget(elements, Button.class, "menu.shareToLan");

		if (this.minecraft.hasSingleplayerServer() && cfg.removePlayerReportingButton && oldButton == null) {
			oldButton = GuiUtils.findWidget(elements, Button.class, "menu.playerReporting");
		}
		if (oldButton != null) {
			Button newButton = Button.builder(MODIFY_LAN_OPTIONS, btn -> {
				this.minecraft.setScreen(new ShareToLanScreenNew(this,
						(this.minecraft.hasSingleplayerServer() && this.minecraft.getSingleplayerServer().isPublished())));
			}).bounds(oldButton.getX(), oldButton.getY(), oldButton.getWidth(), oldButton.getHeight()).build();
			elements.set(elements.indexOf(oldButton), newButton);
			this.removeWidget(oldButton);
			this.addRenderableWidget(newButton);
		}
	}
}
