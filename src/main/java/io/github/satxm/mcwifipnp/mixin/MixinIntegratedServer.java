package io.github.satxm.mcwifipnp.mixin;

import io.github.satxm.mcwifipnp.client.MaxPlayers;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

@Mixin(IntegratedServer.class)
abstract class MixinIntegratedServer implements MaxPlayers {
	@Unique
	@Nullable
	private int maxPlayers = 8;

	@Override
    public int getMaxPlayers() {
		return this.maxPlayers;
	}

	@Override
    public void setMaxPlayers(int i) {
		this.maxPlayers = i;
	}
}
