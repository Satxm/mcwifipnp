package io.github.satxm.mcwifipnp.mixin;

import java.util.UUID;
import io.github.satxm.mcwifipnp.UUIDFixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.UUIDUtil;


@Mixin(UUIDUtil.class)
public abstract class MixinUUIDUtil {
  @Inject(method = "createOfflinePlayerUUID", at = @At("HEAD"), cancellable = true)
  private static void detour_createOfflinePlayerUUID(String playerName, CallbackInfoReturnable<UUID> ci) {
    UUID uuid = UUIDFixer.hookEntry(playerName);
    if (uuid != null) {
      ci.setReturnValue(uuid);
      ci.cancel();
    }
  }
}