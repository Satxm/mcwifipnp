package io.github.satxm.mcwifipnp.network;

import javax.annotation.Nullable;

public interface IUPnPProvider {
	@Nullable
	UPnPModule getUPnPInstance();

	void setUPnPInstance(@Nullable UPnPModule uPnPInstance);
}
