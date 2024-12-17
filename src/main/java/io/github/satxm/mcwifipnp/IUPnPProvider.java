package io.github.satxm.mcwifipnp;

import javax.annotation.Nullable;

public interface IUPnPProvider {
	@Nullable
	UPnPModule getUPnPInstance();

	void setUPnPInstance(@Nullable UPnPModule uPnPInstance);
}
