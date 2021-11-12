package io.github.satxm.mcwifipnp.mixin;

import io.github.satxm.mcwifipnp.OpenToLanScreen;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;

@Mixin(GameMenuScreen.class)
public class MixinGameMenuScreen {
	@Dynamic("lambda in initWidgets")
	@Redirect(method = "method_19838", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;openScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
	private void replaceOpenToLAN(MinecraftClient client, Screen toOpen) {
		client.openScreen(new OpenToLanScreen((Screen) (Object) this));
	}
}
