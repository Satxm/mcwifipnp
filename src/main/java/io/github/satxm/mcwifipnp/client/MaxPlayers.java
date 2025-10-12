package io.github.satxm.mcwifipnp.client;


import javax.annotation.Nullable;

public interface MaxPlayers {
	@Nullable
    int maxPlayers = 8;

	void setMaxPlayers(@Nullable int maxPlayers);

    int getMaxPlayers();

}
