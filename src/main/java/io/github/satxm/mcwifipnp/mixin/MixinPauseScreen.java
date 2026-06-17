package io.github.satxm.mcwifipnp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import io.github.satxm.mcwifipnp.client.GuiUtils;
import io.github.satxm.mcwifipnp.client.ShareToLanScreenNew;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {

	protected MixinPauseScreen(Component title) {
		super(title);
	}

	@Inject(method = "createPauseMenu", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	protected void addOrReplaceButton(CallbackInfo ci, GridLayout gridLayout, GridLayout.RowHelper dummy) {

		Component MODIFY_LAN_OPTIONS = this.minecraft.hasSingleplayerServer()
				&& this.minecraft.getSingleplayerServer().isPublished()
						? Component.translatable("mcwifipnp.gui.lanServerOptions")
						: Component.translatable("menu.multiplayerOptions.button");

		// Replace the vanilla "Multiplayer" button.
		Button oldButton = GuiUtils.findWidget(this.children(), Button.class, "menu.multiplayerOptions.button");
		if (oldButton != null) {
			Button newButton = Button.builder(MODIFY_LAN_OPTIONS, btn -> {
				this.minecraft.gui.setScreen(new ShareToLanScreenNew(this));
			}).bounds(oldButton.getX(), oldButton.getY(), oldButton.getWidth(), oldButton.getHeight()).build();
			this.children().remove(oldButton);
			this.addRenderableWidget(newButton);
		}
	}
}
