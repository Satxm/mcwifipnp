package io.github.satxm.mcwifipnp.mixin;

import java.util.UUID;
import io.github.satxm.mcwifipnp.UUIDFixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.network.NetHandlerLoginServer;

@Mixin(NetHandlerLoginServer.class)
public abstract class MixinUUIDUtil {
	@Inject(method = "getOfflineProfile", at = @At("HEAD"), cancellable = true, require = 1)
	private void detour_getOfflineProfile(GameProfile original, CallbackInfoReturnable<GameProfile> ci) {
		String playerName = original.getName();
		UUID uuid = UUIDFixer.hookEntry(playerName);

		if (uuid != null) {
			System.out.println("Set uuid of " + playerName + " to " + uuid);
			ci.setReturnValue(new GameProfile(uuid, playerName));
			ci.cancel();
		}
	}
}