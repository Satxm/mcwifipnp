package io.github.satxm.mcwifipnp.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.StandardProtocolFamily;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.jspecify.annotations.Nullable;

import io.netty.util.NetUtil;

public enum GlobalIPs {
	IP_SB_4("https://api-ipv4.ip.sb/ip", StandardProtocolFamily.INET),
	IP_SB_6("https://api-ipv6.ip.sb/ip", StandardProtocolFamily.INET6),
	IPW_CN_4("https://4.ipw.cn", StandardProtocolFamily.INET),
	IPW_CN_6("https://6.ipw.cn", StandardProtocolFamily.INET6);

	public final String apiEndPoint;
	public final StandardProtocolFamily family;

	private GlobalIPs(String apiEndPoint, StandardProtocolFamily family) {
		this.apiEndPoint = apiEndPoint;
		this.family = family;
	}

	/**
	 * Fetch the global IP address from the given API provider then validate the
	 * result.
	 *
	 * @return the IP, or null if failed or the IP is invalid
	 */
	@Nullable
	public String fetch() {
		return GlobalIPs.fetchGlobalIP(this.apiEndPoint, this.family);
	}

	/**
	 * Fetch the global IP address from a given API provider then validate the
	 * result.
	 *
	 * @param apiProvider the API provider's URL
	 * @param family      IP family to be verify against, can be INET or INET6
	 * @return the IP, or null if failed or the IP is invalid
	 */
	@Nullable
	public static String fetchGlobalIP(String apiProvider, StandardProtocolFamily family) {
		String ip = null;
		try {
			URL url = URI.create(apiProvider).toURL();
			URLConnection URLconnection = url.openConnection();
			InputStreamReader isr = new InputStreamReader(URLconnection.getInputStream());
			BufferedReader bufferReader = new BufferedReader(isr);

			String line;
			while ((line = bufferReader.readLine()) != null) {
				ip = line;
			}

			bufferReader.close();
		} catch (Exception e) {
		}

		if (ip == null)
			return null;

		switch (family) {
			case INET:
				if (!NetUtil.isValidIpV4Address(ip))
					ip = null;
				break;

			case INET6:
				if (!NetUtil.isValidIpV6Address(ip))
					ip = null;
				break;

			default:
				ip = null;
				break;
		}

		return ip;
	}

	/**
	 * Attempt to find a global IP of a given IP family by fetching each of the
	 * known API end points defined in {@link GlobalIPs}. The order follows the enum
	 * order. The first successful result will be returned, and no more fetch will
	 * be performed.
	 *
	 * @param family the expected IP family
	 * @return the IP, or null if failed or the IP is invalid
	 */
	@Nullable
	public static String fetchGlobalIP(StandardProtocolFamily family) {
		for (GlobalIPs api : GlobalIPs.values()) {
			if (api.family != family)
				continue;

			String ip = api.fetch();
			if (ip != null) {
				return ip;
			}
		}

		return null;
	}
}
