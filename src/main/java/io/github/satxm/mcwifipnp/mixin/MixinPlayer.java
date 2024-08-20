package io.github.satxm.mcwifipnp.mixin;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.github.satxm.mcwifipnp.UUIDFixer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayer {
  @Inject(method = "createPlayerUUID(Lcom/mojang/authlib/GameProfile;)Ljava/util/UUID;", at = @At("HEAD"), cancellable = true)
  private static void detour_createOfflinePlayerUUID(GameProfile gameProfile, CallbackInfoReturnable<UUID> ci) {
    UUID uuid = UUIDFixer.hookEntry(gameProfile.getName());
    if (uuid != null) {
      ci.setReturnValue(uuid);
      ci.cancel();
    }
  }
}