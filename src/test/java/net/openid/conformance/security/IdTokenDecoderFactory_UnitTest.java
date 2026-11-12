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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Which algorithms an ID token may be signed with depends on the login
 * registration: the "idp" registration accepts each of IdpJwsAlgorithms.ID_TOKEN,
 * every other registration only RS256.
 */
public class IdTokenDecoderFactory_UnitTest {

	private static final String ISSUER = "https://idp.example.com";

	private static final String CLIENT_ID = "a-client";

	private static HttpServer jwksServer;

	private static ECKey ecKey;

	private static RSAKey rsaKey;

	@BeforeAll
	public static void serveJwks() throws IOException, JOSEException {
		ecKey = new ECKeyGenerator(Curve.P_256).keyID("ec").generate();
		rsaKey = new RSAKeyGenerator(2048).keyID("rsa").generate();
		byte[] jwks = new JWKSet(java.util.List.<JWK>of(ecKey, rsaKey)).toString(true).getBytes(StandardCharsets.UTF_8);

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

	private static ClientRegistration registration(String registrationId) {
		return ClientRegistration.withRegistrationId(registrationId)
			.clientId(CLIENT_ID)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
			.authorizationUri(ISSUER + "/authorize")
			.tokenUri(ISSUER + "/token")
			.issuerUri(ISSUER)
			.jwkSetUri("http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/jwks")
			.build();
	}

	private static String idToken(JWSAlgorithm algorithm, String keyId, JWSSigner signer, String audience) throws JOSEException {
		Instant now = Instant.now();
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(keyId).build(),
			new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.subject("a-user")
				.audience(audience)
				.issueTime(Date.from(now))
				.expirationTime(Date.from(now.plusSeconds(60)))
				.build());
		jwt.sign(signer);
		return jwt.serialize();
	}

	private static String es256IdToken() throws JOSEException {
		return idToken(JWSAlgorithm.ES256, "ec", new ECDSASigner(ecKey), CLIENT_ID);
	}

	private static String rs256IdToken() throws JOSEException {
		return idToken(JWSAlgorithm.RS256, "rsa", new RSASSASigner(rsaKey), CLIENT_ID);
	}

	@Test
	public void the_idp_registration_accepts_es256_and_rs256() throws JOSEException {
		var decoder = new IdTokenDecoderFactory().createDecoder(registration("idp"));

		Jwt es256 = decoder.decode(es256IdToken());
		Assertions.assertEquals("a-user", es256.getSubject());
		Assertions.assertNotNull(es256.getIssuedAt(), "claims are converted as for any other ID token");
		Assertions.assertEquals("a-user", decoder.decode(rs256IdToken()).getSubject());
	}

	@Test
	public void the_idp_registration_still_validates_the_id_token() throws JOSEException {
		var decoder = new IdTokenDecoderFactory().createDecoder(registration("idp"));
		String forAnotherClient = idToken(JWSAlgorithm.ES256, "ec", new ECDSASigner(ecKey), "another-client");

		Assertions.assertThrows(JwtException.class, () -> decoder.decode(forAnotherClient));
	}

	@Test
	public void the_idp_registration_rejects_other_algorithms() throws JOSEException {
		var decoder = new IdTokenDecoderFactory().createDecoder(registration("idp"));
		String ps256 = idToken(JWSAlgorithm.PS256, "rsa", new RSASSASigner(rsaKey), CLIENT_ID);

		Assertions.assertThrows(JwtException.class, () -> decoder.decode(ps256));
	}

	@Test
	public void other_registrations_accept_only_rs256() throws JOSEException {
		var decoder = new IdTokenDecoderFactory().createDecoder(registration("gitlab"));
		String es256 = es256IdToken();

		Assertions.assertEquals("a-user", decoder.decode(rs256IdToken()).getSubject());
		Assertions.assertThrows(JwtException.class, () -> decoder.decode(es256));
	}
}
