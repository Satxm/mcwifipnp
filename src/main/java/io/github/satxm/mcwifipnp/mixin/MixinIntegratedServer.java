package io.github.satxm.mcwifipnp.mixin;

import io.github.satxm.mcwifipnp.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "getMaxPlayers", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void setMaxPlayers(CallbackInfoReturnable<Integer> ci) {
        int i = Config.read(this.minecraft.getSingleplayerServer()).maxPlayers;
        ci.setReturnValue(i);
        ci.cancel();
    }
}
