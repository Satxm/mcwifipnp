package io.github.satxm.mcwifipnp.revprox.sakurafrp;

import io.github.satxm.mcwifipnp.revprox.FetchedTunnel;

public class SakuraFrpTunnel implements FetchedTunnel {
	int id;
	String name;
	int node;
	String type;
	boolean online;

	String desc = "";
	String hostname = "";

	@Override
	public String name() {
		return String.valueOf(this.name);
	}
	@Override
	public String description() {
		return this.desc;
	}
	@Override
	public String hostname() {
		return this.hostname;
	}
	@Override
	public int tcpingPort() {
		return 80;
	}
}
