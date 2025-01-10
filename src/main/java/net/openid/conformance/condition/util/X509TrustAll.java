package net.openid.conformance.condition.util;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class X509TrustAll implements X509TrustManager {

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) {
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) {
	}
}
