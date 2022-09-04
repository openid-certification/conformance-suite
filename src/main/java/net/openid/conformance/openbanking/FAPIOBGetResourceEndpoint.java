package net.openid.conformance.openbanking;

import com.google.common.base.Strings;
import net.openid.conformance.testmodule.Environment;

public final class FAPIOBGetResourceEndpoint {

	public static enum Endpoint {
		ACCOUNT_REQUESTS,
		ACCOUNTS_RESOURCE
	}


	/**
	 * Private constructor.
	 */
	private FAPIOBGetResourceEndpoint() {

	}

	/**
	 * Returns the required Endpoint from the current running environment.
	 * Added to allow the user to specify different base Endpoint URIs
	 * for both the Accounts Requests server and the Accounts Resource servers.
	 *
	 * Defaults to returning the "resourceUrl" string.
	 *
	 * @param requiredEndpoint -- AccountRequest or AccountsResource
	 * @return the required Endpoint as a string.
	 */
	public static String getBaseResourceURL(Environment env, Endpoint requiredEndpoint) {
		String resourceEndpoint = env.getString("resource", "resourceUrl");
		String resourceAccountRequest = env.getString("resource","resourceUrlAccountRequests");
		String resourceAccountsResource = env.getString("resource","resourceUrlAccountsResource");

		switch (requiredEndpoint) {
			case ACCOUNT_REQUESTS:
				if (!Strings.isNullOrEmpty(resourceAccountRequest)) {
					return resourceAccountRequest;
				} else {
					return resourceEndpoint;
				}
			case ACCOUNTS_RESOURCE:
				if(!Strings.isNullOrEmpty(resourceAccountsResource)) {
					return resourceAccountsResource;
				} else {
					return resourceEndpoint;
				}
			default:
				return resourceEndpoint;
		}
	}

}
