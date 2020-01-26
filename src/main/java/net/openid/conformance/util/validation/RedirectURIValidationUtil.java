package net.openid.conformance.util.validation;

public class RedirectURIValidationUtil {

	/**
	 *
	 * @param applicationType
	 * @param responseType
	 * @param redirectUri
	 * @return false if invalid
	 */
	public static boolean requireHttpsIfWebAndResponseTypeNotCode(String applicationType, String responseType, String redirectUri) {
		if(!(("web".equals(applicationType) || applicationType == null) && "code".equals(responseType))) {
			if(redirectUri.toLowerCase().startsWith("http://")) {
				return false;
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
	public static boolean dontAllowHttpIfNativeAndNotLocalhost(String applicationType, String redirectUri) {
		if("native".equals(applicationType)) {
			String actualLower = redirectUri.toLowerCase();
			if (actualLower.startsWith("http://")) {
				if (!(actualLower.startsWith("http://localhost/")
					|| actualLower.startsWith("http://localhost:")
					|| actualLower.equals("http://localhost")
					|| actualLower.startsWith("http://127.0.0.1/")
					|| actualLower.startsWith("http://127.0.0.1:")
					|| actualLower.equals("http://127.0.0.1")
					|| actualLower.startsWith("http://::1/")
					|| actualLower.startsWith("http://[::1]:")
					|| actualLower.equals("http://::1")
					)
				) {
					return false;
				}
			}
		}
		return true;
	}
}
