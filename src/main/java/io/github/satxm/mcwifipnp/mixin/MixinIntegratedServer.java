package io.github.satxm.mcwifipnp.mixin;

import io.github.satxm.mcwifipnp.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.Serial;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "getMaxPlayers", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void setMaxPlayers(CallbackInfoReturnable<Integer> ci) {
        MinecraftServer server = this.minecraft.getSingleplayerServer();
        if (server != null) {
        int i = Config.read(server).maxPlayers;
        ci.setReturnValue(i);
        ci.cancel();
        }
    }
}
