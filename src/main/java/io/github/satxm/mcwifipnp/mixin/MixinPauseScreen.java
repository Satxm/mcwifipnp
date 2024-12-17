package io.github.satxm.mcwifipnp.mixin;

import io.github.satxm.mcwifipnp.GuiUtils;
import io.github.satxm.mcwifipnp.MCWiFiPnPUnit;
import io.github.satxm.mcwifipnp.ShareToLanScreenNew;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {
	protected MixinPauseScreen(Component title) {
		super(title);
	}

	@Inject(method = "createPauseMenu", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	protected void addOrReplaceButton(CallbackInfo ci, GridLayout gridLayout, GridLayout.RowHelper dummy) {
		// If there is a published server, add a new button to the pause screen.
		if (this.minecraft.hasSingleplayerServer() && this.minecraft.getSingleplayerServer().isPublished()) {
			Button optionButton = GuiUtils.findWidget(this.children(), Button.class, "menu.options");

			if (optionButton != null) {
				SpriteIconButton lanServerSettings = SpriteIconButton
						.builder(MCWiFiPnPUnit.MODIFY_LAN_OPTIONS,
								(button) -> this.minecraft.setScreen(new ShareToLanScreenNew(this, true)), true)
						.width(20).sprite(ResourceLocation.tryParse("icon/language"), 15, 15).build();
				lanServerSettings.setPosition(this.width / 2 - 124, optionButton.getY());
				lanServerSettings.setTooltip(Tooltip.create(MCWiFiPnPUnit.MODIFY_LAN_OPTIONS));
				this.addRenderableWidget(lanServerSettings);
			}
		} else {
			// Otherwise, replace the vanilla "Open to Lan" button.
			final List<LayoutElement> elements = ((AccessorGridLayout) gridLayout).getChildren();
			Button oldButton = GuiUtils.findWidget(elements, Button.class, "menu.shareToLan");

			if (oldButton != null) {
				Button newButton = Button.builder(Component.translatable("menu.shareToLan"), btn -> {
					this.minecraft.setScreen(new ShareToLanScreenNew(this, false));
				}).bounds(oldButton.getX(), oldButton.getY(), oldButton.getWidth(), oldButton.getHeight()).build();
				elements.set(elements.indexOf(oldButton), newButton);
				this.removeWidget(oldButton);
				this.addRenderableWidget(newButton);
			}
		}
	}
}