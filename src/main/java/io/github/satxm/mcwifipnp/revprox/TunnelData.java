package io.github.satxm.mcwifipnp.revprox;

public class TunnelData {
	public String name;
	public String desc;
	public String host;
	public boolean enabled = true;

	public final TunnelType tunnelType;

	public TunnelData(String name, String desc, String host, String type) {
		this.name = name;
		this.desc = desc;
		this.host = host;

		this.tunnelType = TunnelType.get(type);
	}
}
