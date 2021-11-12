package io.github.satxm.mcwifipnp;

import com.dosse.upnp.UPnP;

public class UPnPUtil {
	public enum UPnPResult {
		SUCCESS,
		FAILED_GENERIC,
		FAILED_MAPPED,
		FAILED_DISABLED
	}
	
	public static UPnPResult init(int port, String display) {
		if (!UPnP.isUPnPAvailable()) {
			return UPnPResult.FAILED_DISABLED;
		}
		if (UPnP.isMappedTCP(port)) {
			return UPnPResult.FAILED_MAPPED;
		}
		if (!UPnP.openPortTCP(port , display)) {
			return UPnPResult.FAILED_GENERIC;
		}
		return UPnPResult.SUCCESS;
	}
}