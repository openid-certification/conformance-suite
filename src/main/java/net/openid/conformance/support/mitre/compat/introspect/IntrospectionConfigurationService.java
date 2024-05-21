package net.openid.conformance.support.mitre.compat.introspect;

import net.openid.conformance.support.mitre.compat.model.RegisteredClient;

/**
 * @author jricher
 *
 */
public interface IntrospectionConfigurationService {

	/**
	 * Get the introspection URL based on the access token.
	 * @param accessToken
	 * @return
	 */
	String getIntrospectionUrl(String accessToken);


	/**
	 * Get the client configuration to use to connect to the
	 * introspection endpoint. In particular, this cares about
	 * the clientId, clientSecret, and tokenEndpointAuthMethod
	 * fields.
	 */
	RegisteredClient getClientConfiguration(String accessToken);

}
