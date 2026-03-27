package io.github.satxm.mcwifipnp.network;

import org.jspecify.annotations.Nullable;

public interface IUPnPProvider {
	@Nullable
	UPnPModule getUPnPInstance();

	void setUPnPInstance(@Nullable UPnPModule uPnPInstance);
}
