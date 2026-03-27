package io.github.satxm.mcwifipnp.mixin;

import org.jspecify.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import io.github.satxm.mcwifipnp.network.IUPnPProvider;
import io.github.satxm.mcwifipnp.network.UPnPModule;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IUPnPProvider {
	@Unique
	@Nullable
	private UPnPModule uPnPInstance;

	@Override
	public UPnPModule getUPnPInstance() {
		return this.uPnPInstance;
	}

	@Override
	public void setUPnPInstance(UPnPModule uPnPInstance) {
		this.uPnPInstance = uPnPInstance;
	}
}
