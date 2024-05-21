package net.openid.conformance.support.mitre.compat.oidc.auth;

import net.openid.conformance.support.mitre.compat.clients.ServerConfiguration;
import net.openid.conformance.support.mitre.compat.model.RegisteredClient;

import java.util.Map;

/**
 * Builds a URL string to the IdP's authorization endpoint.
 *
 * @author jricher
 *
 */
public interface AuthRequestUrlBuilder {

	/**
	 * @param serverConfig
	 * @param clientConfig
	 * @param redirectUri
	 * @param nonce
	 * @param state
	 * @param loginHint
	 * @return
	 */
	String buildAuthRequestUrl(ServerConfiguration serverConfig, RegisteredClient clientConfig, String redirectUri, String nonce, String state, Map<String, String> options, String loginHint);

}
