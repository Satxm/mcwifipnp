package io.github.satxm.mcwifipnp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.satxm.mcwifipnp.client.TunnelScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {
	protected MixinTitleScreen(Component p_96550_) {
		super(p_96550_);
	}

	@Inject(method = "createTestWorldButton", at = @At("HEAD"))
	private void createTestWorldButton(int y, int height, CallbackInfoReturnable<Integer> cir) {
		if (SharedConstants.IS_RUNNING_IN_IDE) {
			SpriteIconButton button = SpriteIconButton.builder(Component.literal("Reverse Proxy"),
				(btn) -> this.minecraft.setScreen(new TunnelScreen(this)), true)
					.width(20)
					.sprite(ResourceLocation.withDefaultNamespace("icon/new_realm"), 15, 15)
					.build();

			button.setPosition(this.width / 2 - 124 , y + height);
			this.addRenderableWidget(button);
		}
	}
}
