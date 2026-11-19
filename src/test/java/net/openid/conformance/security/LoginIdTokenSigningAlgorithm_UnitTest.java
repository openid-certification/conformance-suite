package net.openid.conformance.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Each login provider's ID tokens are verified with the algorithm configured for it
 * in oidc.&lt;id&gt;.id-token-signed-response-alg, RS256 by default, and are otherwise
 * validated as Spring validates any ID token. Signs real tokens against a JWKS served
 * on the loopback interface, and decodes them with the decoder the login uses.
 */
public class LoginIdTokenSigningAlgorithm_UnitTest {

	private static final String ISSUER = "https://op.example.com";

	private static HttpServer jwksServer;

	private static ECKey ecKey;

	private static RSAKey rsaKey;

	@BeforeAll
	public static void serveJwks() throws IOException, JOSEException {
		ecKey = new ECKeyGenerator(Curve.P_256).keyID("ec").generate();
		rsaKey = new RSAKeyGenerator(2048).keyID("rsa").generate();
		byte[] jwks = new JWKSet(List.<JWK>of(ecKey, rsaKey)).toString(true).getBytes(StandardCharsets.UTF_8);

		jwksServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		jwksServer.createContext("/jwks", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, jwks.length);
			exchange.getResponseBody().write(jwks);
			exchange.close();
		});
		jwksServer.start();
	}

	@AfterAll
	public static void stopJwks() {
		jwksServer.stop(0);
	}

	/** Google on the default, GitLab configured for ES256. */
	private static LoginProviders providers() {
		return new LoginProviders(new MockEnvironment()
			.withProperty("oidc.google.clientid", "google-client")
			.withProperty("oidc.google.secret", "google-secret")
			.withProperty("oidc.gitlab.clientid", "gitlab-client")
			.withProperty("oidc.gitlab.secret", "gitlab-secret")
			.withProperty("oidc.gitlab.id-token-signed-response-alg", "ES256"));
	}

	private static JwtDecoder decoder(String registrationId) {
		ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
			.clientId(registrationId + "-client")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.authorizationUri(ISSUER + "/authorize")
			.tokenUri(ISSUER + "/token")
			.issuerUri(ISSUER)
			.jwkSetUri("http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/jwks")
			.build();
		return WebSecurityOidcLoginConfig.idTokenDecoderFactoryFor(providers()).createDecoder(registration);
	}

	private static String idToken(JWSAlgorithm algorithm, String keyId, JWSSigner signer, String issuer,
		String audience) throws JOSEException {
		Instant now = Instant.now();
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(keyId).build(),
			new JWTClaimsSet.Builder()
				.issuer(issuer)
				.subject("a-user")
				.audience(audience)
				.issueTime(Date.from(now))
				.expirationTime(Date.from(now.plusSeconds(60)))
				.build());
		jwt.sign(signer);
		return jwt.serialize();
	}

	private static String es256(String audience) throws JOSEException {
		return idToken(JWSAlgorithm.ES256, "ec", new ECDSASigner(ecKey), ISSUER, audience);
	}

	private static String rs256(String audience) throws JOSEException {
		return idToken(JWSAlgorithm.RS256, "rsa", new RSASSASigner(rsaKey), ISSUER, audience);
	}

	@Test
	public void a_provider_configured_for_es256_accepts_es256_only() throws JOSEException {
		JwtDecoder decoder = decoder("gitlab");

		Assertions.assertEquals("a-user", decoder.decode(es256("gitlab-client")).getSubject());
		String rs256 = rs256("gitlab-client");
		Assertions.assertThrows(JwtException.class, () -> decoder.decode(rs256));
	}

	@Test
	public void a_provider_without_configuration_accepts_rs256_only() throws JOSEException {
		JwtDecoder decoder = decoder("google");

		Assertions.assertEquals("a-user", decoder.decode(rs256("google-client")).getSubject());
		String es256 = es256("google-client");
		Assertions.assertThrows(JwtException.class, () -> decoder.decode(es256));
	}

	@Test
	public void an_id_token_for_another_client_is_rejected() throws JOSEException {
		JwtDecoder decoder = decoder("gitlab");
		String forAnotherClient = es256("another-client");

		Assertions.assertThrows(JwtException.class, () -> decoder.decode(forAnotherClient));
	}

	@Test
	public void an_id_token_from_another_issuer_is_rejected() throws JOSEException {
		JwtDecoder decoder = decoder("gitlab");
		String fromAnotherIssuer = idToken(JWSAlgorithm.ES256, "ec", new ECDSASigner(ecKey),
			"https://elsewhere.example.com", "gitlab-client");

		Assertions.assertThrows(JwtException.class, () -> decoder.decode(fromAnotherIssuer));
	}

	@Test
	public void a_blank_setting_means_rs256() {
		LoginProviders providers = new LoginProviders(new MockEnvironment()
			.withProperty("oidc.google.clientid", "google-client")
			.withProperty("oidc.google.secret", "google-secret")
			.withProperty("oidc.google.id-token-signed-response-alg", " "));

		Assertions.assertEquals(SignatureAlgorithm.RS256, providers.configured().get(0).idTokenSigningAlgorithm());
	}

	@Test
	public void an_unknown_or_mac_algorithm_stops_startup_with_the_property_name() {
		for (String alg : List.of("none", "HS256", "ES256,RS256")) {
			MockEnvironment environment = new MockEnvironment()
				.withProperty("oidc.google.clientid", "google-client")
				.withProperty("oidc.google.secret", "google-secret")
				.withProperty("oidc.google.id-token-signed-response-alg", alg);

			IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
				() -> new LoginProviders(environment), alg);
			Assertions.assertTrue(e.getMessage().contains("oidc.google.id-token-signed-response-alg"), e.getMessage());
		}
	}
}
