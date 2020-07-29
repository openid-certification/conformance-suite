package net.openid.conformance.security;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.apache.http.client.utils.URIBuilder;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.service.AuthRequestUrlBuilder;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.security.authentication.AuthenticationServiceException;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

/**
 * Code copied from PlainAuthRequestUrlBuilder
 * See comments starting with "Workaround:" below
 */
public class AuthRequestUrlBuilderWithFixedScopes implements AuthRequestUrlBuilder {
	public static final Set<String> SCOPES = ImmutableSet.of("openid", "email", "address", "profile", "phone");

	@Override
	public String buildAuthRequestUrl(ServerConfiguration serverConfig, RegisteredClient clientConfig, String redirectUri, String nonce, String state, Map<String, String> options, String loginHint)
	{
		try {

			URIBuilder uriBuilder = new URIBuilder(serverConfig.getAuthorizationEndpointUri());
			uriBuilder.addParameter("response_type", "code");
			uriBuilder.addParameter("client_id", clientConfig.getClientId());

			//Workaround: clientConfig.getScope() returns empty set for dynamic clients
			//and PlainAuthRequestUrlBuilder sends an empty scope
			if(!clientConfig.getScope().isEmpty()) {
				uriBuilder.addParameter("scope", Joiner.on(" ").join(clientConfig.getScope()));
			} else {
				uriBuilder.addParameter("scope", Joiner.on(" ").join(SCOPES));
			}

			uriBuilder.addParameter("redirect_uri", redirectUri);

			uriBuilder.addParameter("nonce", nonce);

			uriBuilder.addParameter("state", state);

			// Optional parameters:
			for (Map.Entry<String, String> option : options.entrySet()) {
				uriBuilder.addParameter(option.getKey(), option.getValue());
			}

			// if there's a login hint, send it
			if (!Strings.isNullOrEmpty(loginHint)) {
				//Workaround: remove acct: prefix from login_hint
				if(loginHint.startsWith("acct:")) {
					uriBuilder.addParameter("login_hint", loginHint.substring(5));
				} else {
					uriBuilder.addParameter("login_hint", loginHint);
				}
			}

			return uriBuilder.build().toString();

		} catch (URISyntaxException e) {
			throw new AuthenticationServiceException("Malformed Authorization Endpoint Uri", e);

		}
	}
}
