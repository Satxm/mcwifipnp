package io.github.satxm.mcwifipnp.mixin;

import io.github.satxm.mcwifipnp.ShareToLanScreen;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;

@Mixin(PauseScreen.class)
public class MixinPauseScreen {
	@Dynamic("lambda in init")
	@Redirect(method = "lambda$createPauseMenu$8" , at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V") , remap = false)
	private void replaceOpenToLAN(Minecraft client, Screen toOpen) {
		client.setScreen(new ShareToLanScreen((Screen) (Object) this));
	}
}
