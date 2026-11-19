package net.openid.conformance.security;

import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * The OpenID Providers users can sign in with.
 * <p>
 * Each provider is configured under {@code oidc.<id>.*} and is offered only when
 * both {@code oidc.<id>.clientid} and {@code oidc.<id>.secret} are set, so an
 * installation that has not registered a client with a provider shows no button
 * for it rather than one that fails at the provider.
 * <p>
 * {@code oidc.<id>.id-token-signed-response-alg} names the one JWS algorithm the
 * provider signs the suite's ID tokens with (the client's registered
 * {@code id_token_signed_response_alg}); RS256 when unset. It is configuration
 * because a provider's discovery document lists every algorithm it can sign with,
 * not the one it uses for this client.
 */
@Component
public class LoginProviders {

	/**
	 * A provider the suite knows how to offer, with the defaults that apply when
	 * its {@code oidc.<id>.*} properties do not override them.
	 *
	 * @param id                the registration id; also the {@code oidc.<id>.*}
	 *                          property prefix and the last path segment of
	 *                          {@code /oauth2/authorization/<id>}
	 * @param displayName       shown on the login button
	 * @param defaultIssuer     used unless {@code oidc.<id>.iss} is set
	 * @param userNameAttribute the claim Spring uses as the principal's name
	 */
	record Known(String id, String displayName, String defaultIssuer, String userNameAttribute) {
	}

	/** In the order the login page offers them. */
	static final List<Known> KNOWN = List.of(
		new Known("google", "Google", "https://accounts.google.com", "email"),
		new Known("gitlab", "GitLab", "https://gitlab.com", "preferred_username"));

	/**
	 * A provider that is configured and so offered for login.
	 *
	 * @param id                the registration id
	 * @param displayName       shown on the login button
	 * @param issuer            the issuer whose discovery document describes it
	 * @param clientId          the suite's client id at the provider
	 * @param clientSecret      the suite's client secret at the provider
	 * @param userNameAttribute the claim Spring uses as the principal's name
	 * @param idTokenSigningAlgorithm the algorithm the provider signs ID tokens with
	 */
	public record Provider(String id, String displayName, String issuer, String clientId, String clientSecret,
		String userNameAttribute, SignatureAlgorithm idTokenSigningAlgorithm) {

		/**
		 * Builds the client registration, fetching the issuer's discovery document.
		 * This is network I/O; see {@link LazyClientRegistrationRepository} for when it
		 * happens.
		 */
		public ClientRegistration toClientRegistration() {
			return ClientRegistrations.fromIssuerLocation(issuer)
				.registrationId(id)
				.clientName(displayName)
				.clientId(clientId)
				.clientSecret(clientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
				.scope("openid", "email")
				.userNameAttributeName(userNameAttribute)
				.build();
		}
	}

	private final List<Provider> configured;

	public LoginProviders(Environment environment) {
		this.configured = KNOWN.stream()
			.map(known -> configured(environment, known))
			.filter(Objects::nonNull)
			.toList();
	}

	/** The providers that are configured, in the order the login page offers them. */
	public List<Provider> configured() {
		return configured;
	}

	private static Provider configured(Environment environment, Known known) {
		String prefix = "oidc." + known.id() + ".";
		String clientId = environment.getProperty(prefix + "clientid");
		String clientSecret = environment.getProperty(prefix + "secret");
		if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
			return null;
		}
		String issuer = environment.getProperty(prefix + "iss");
		if (issuer == null || issuer.isBlank()) {
			issuer = known.defaultIssuer();
		}
		return new Provider(known.id(), known.displayName(), issuer.trim(), clientId.trim(), clientSecret.trim(),
			known.userNameAttribute(), idTokenSigningAlgorithm(environment, prefix + "id-token-signed-response-alg"));
	}

	private static SignatureAlgorithm idTokenSigningAlgorithm(Environment environment, String property) {
		String name = environment.getProperty(property);
		if (name == null || name.isBlank()) {
			return SignatureAlgorithm.RS256;
		}
		// SignatureAlgorithm covers only the RS*, ES* and PS* signature algorithms, so
		// "none" and the HS* MAC algorithms are rejected here.
		SignatureAlgorithm algorithm = SignatureAlgorithm.from(name.trim());
		if (algorithm == null) {
			throw new IllegalStateException(property + " must name one JWS signature algorithm, such as RS256 or"
				+ " ES256, but is '" + name + "'");
		}
		return algorithm;
	}
}
