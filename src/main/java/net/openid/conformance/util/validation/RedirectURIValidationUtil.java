package net.openid.conformance.util.validation;

import java.net.URI;
import java.net.URISyntaxException;

public class RedirectURIValidationUtil {

	/**
	 *
	 * @param applicationType
	 * @param responseType
	 * @param redirectUri
	 * @return false if invalid
	 */
	public static boolean requireHttpsIfWebAndResponseTypeNotCode(String applicationType, String responseType, String redirectUri) {
		if("web".equals(applicationType) || applicationType == null) {
			if (redirectUri.toLowerCase().startsWith("http://")) {
				if("code".equals(responseType)) {
					return true;
				} else {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isLocalhost(String hostname) {
		if(hostname.equals("localhost")) {
			return true;
		}
		if(hostname.equals("127.0.0.1")) {
			return true;
		}
		if(hostname.equals("::1") || hostname.equals("[::1]")) {
			return true;
		}
		return false;
	}

	/**
	 *
	 * @param applicationType
	 * @param redirectUri
	 * @return false if invalid
	 */
	public static boolean dontAllowHttpIfNativeAndNotLocalhost(String applicationType, String redirectUri) throws URISyntaxException
	{
		if("native".equals(applicationType)) {
			String actualLower = redirectUri.toLowerCase();
			if (actualLower.startsWith("http://")) {
				URI uri = new URI(redirectUri);
				if (!isLocalhost(uri.getHost())) {
					return false;
				}
			}
		}
		return true;
	}
}
