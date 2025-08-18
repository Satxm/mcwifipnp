package io.github.satxm.mcwifipnp.revprox;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class TunnelList {
	private final List<TunnelData> tunnelList = new LinkedList<>();

	public TunnelList() {
		tunnelList.add(new TunnelData("SakuraFrp", "香港3", "frp-pet.com", "sakurafrp"));
		tunnelList.add(new TunnelData("OpenFrp", "香港-4", "cn-hk-bgp-4.ofalias.net:23836", "openfrp"));
		tunnelList.add(new TunnelData("Cloudflare Tunnel","" , "cyka-blayt.trycloudflare.com", "cloudflare"));
	}

	public void load() {

	}

	public void save() {

	}

	public void forEach(Consumer<? super TunnelData> action) {
		tunnelList.forEach(action);
	}

	public void swap(int from, int to) {
		TunnelData temp = this.tunnelList.get(from);
		this.tunnelList.set(from, this.tunnelList.get(to));
		this.tunnelList.set(to, temp);
		this.save();
	}

	public int size() {
		return tunnelList.size();
	}
}
