package net.openid.conformance.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.openid.conformance.security.idp.RolesAuthoritiesConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Drives the provider that WebSecurityResourceServerConfig puts in the API chain
 * with correctly signed tokens, against a local IdP that serves a discovery
 * document and a JWKS. A forged token stops at signature verification, so only
 * tokens signed with the IdP's key reach the checks that keep an ID token, or a
 * token for another audience, from authenticating.
 */
public class IdpJwtDecoderChain_UnitTest {

	private static final String AUDIENCE = "conformance-suite-api";

	private static final String CLIENT_ID = "conformance-suite";

	private static final String ADMIN_ROLE = "conformance-admin";

	private HttpServer server;

	private boolean stopped;

	private String issuer;

	private ECKey idpKey;

	private final AtomicInteger idpRequests = new AtomicInteger();

	private JwtAuthenticationProvider provider;

	@BeforeEach
	public void startIdp() throws Exception {
		idpKey = new ECKeyGenerator(Curve.P_256).keyID("idp-key").generate();
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		issuer = "http://127.0.0.1:" + server.getAddress().getPort();
		server.createContext("/.well-known/openid-configuration", exchange -> respond(exchange,
			"{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer + "/jwks\"}"));
		server.createContext("/jwks", exchange -> respond(exchange,
			new JWKSet(idpKey.toPublicJWK()).toString()));
		server.start();

		provider = WebSecurityResourceServerConfig.createIdpJwtAuthenticationProvider(
			IdpJwtDecoder.forIssuer(new IdpProperties(issuer, AUDIENCE, "ES256")),
			new RolesAuthoritiesConverter(ADMIN_ROLE));
	}

	@AfterEach
	public void stopIdp() {
		if (!stopped) {
			server.stop(0);
		}
	}

	private void respond(HttpExchange exchange, String json) throws IOException {
		idpRequests.incrementAndGet();
		byte[] body = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(body);
		}
	}

	private JWTClaimsSet.Builder accessTokenClaims() {
		Instant now = Instant.now();
		return new JWTClaimsSet.Builder()
			.issuer(issuer)
			.subject("a-user")
			.audience(AUDIENCE)
			.claim("azp", CLIENT_ID)
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plusSeconds(300)));
	}

	private String sign(JOSEObjectType type, ECKey key, Consumer<JWTClaimsSet.Builder> claims) throws Exception {
		JWTClaimsSet.Builder builder = accessTokenClaims();
		claims.accept(builder);
		JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(key.getKeyID());
		if (type != null) {
			header.type(type);
		}
		SignedJWT jwt = new SignedJWT(header.build(), builder.build());
		jwt.sign(new ECDSASigner(key));
		return jwt.serialize();
	}

	private String atJwt(Consumer<JWTClaimsSet.Builder> claims) throws Exception {
		return sign(new JOSEObjectType("at+jwt"), idpKey, claims);
	}

	private Authentication authenticate(String token) {
		return provider.authenticate(new BearerTokenAuthenticationToken(token));
	}

	private void assertRejected(String token) {
		InvalidBearerTokenException e = Assertions.assertThrows(InvalidBearerTokenException.class,
			() -> authenticate(token));
		Assertions.assertEquals(IdpJwtDecoder.REJECTED, e.getMessage(),
			"the reason a token was refused must not reach the caller");
	}

	@Test
	public void an_rfc9068_access_token_authenticates_as_a_user() throws Exception {
		Authentication authentication = authenticate(atJwt(claims -> {
		}));

		Assertions.assertTrue(authentication.isAuthenticated());
		Set<String> authorities = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
		Assertions.assertTrue(authorities.contains("ROLE_USER"), authorities.toString());
		Assertions.assertFalse(authorities.contains("ROLE_ADMIN"), authorities.toString());
	}

	@Test
	public void a_keycloak_style_access_token_authenticates() throws Exception {
		Authentication authentication = authenticate(
			sign(JOSEObjectType.JWT, idpKey, claims -> claims.claim("typ", "Bearer")));

		Assertions.assertTrue(authentication.isAuthenticated());
	}

	@Test
	public void an_unmarked_access_token_with_a_jwt_header_authenticates() throws Exception {
		Authentication authentication = authenticate(sign(JOSEObjectType.JWT, idpKey, claims -> {
		}));

		Assertions.assertTrue(authentication.isAuthenticated());
	}

	/**
	 * A JWT header with no typ claim passes the type check, so an ID token of that
	 * shape is kept out by its audience, the client id.
	 */
	@Test
	public void an_id_token_with_a_jwt_header_is_rejected_by_its_audience() throws Exception {
		assertRejected(sign(JOSEObjectType.JWT, idpKey,
			claims -> claims.audience(CLIENT_ID).claim("roles", List.of(ADMIN_ROLE))));
	}

	@Test
	public void the_admin_role_grants_admin() throws Exception {
		Authentication authentication = authenticate(atJwt(claims -> claims.claim("roles", List.of(ADMIN_ROLE))));

		Assertions.assertTrue(AuthorityUtils.authorityListToSet(authentication.getAuthorities()).contains("ROLE_ADMIN"));
	}

	/**
	 * The ID token the suite receives at login is sent back to the IdP in the
	 * logout redirect, so it can end up in browser history and access logs. It is
	 * signed by the same keys and carries the user's roles.
	 */
	@Test
	public void an_id_token_is_rejected() throws Exception {
		assertRejected(sign(null, idpKey, claims -> claims.audience(CLIENT_ID).claim("roles", List.of(ADMIN_ROLE))));
		assertRejected(sign(JOSEObjectType.JWT, idpKey, claims -> claims.claim("typ", "ID")));
		assertRejected(sign(null, idpKey, claims -> {
		}));
	}

	/**
	 * A token the IdP issued to one of its other clients, or for another resource,
	 * must not authenticate here even when it names this client as its authorized
	 * party.
	 */
	@Test
	public void a_token_for_another_audience_is_rejected() throws Exception {
		assertRejected(atJwt(claims -> claims.audience("some-other-api")));
		assertRejected(atJwt(claims -> claims.audience(CLIENT_ID)));
		assertRejected(atJwt(claims -> claims.audience((String) null)));
	}

	@Test
	public void an_expired_token_is_rejected() throws Exception {
		assertRejected(atJwt(claims -> claims.expirationTime(Date.from(Instant.now().minusSeconds(3600)))));
	}

	@Test
	public void a_token_without_an_expiry_is_rejected() throws Exception {
		assertRejected(atJwt(claims -> claims.expirationTime(null)));
	}

	/**
	 * The suite identifies a user by (iss, sub); without a subject the request
	 * would authenticate and then fail with a server error when the principal is
	 * read.
	 */
	@Test
	public void a_token_without_a_subject_is_rejected() throws Exception {
		assertRejected(atJwt(claims -> claims.subject(null)));
	}

	@Test
	public void a_token_signed_by_another_key_is_rejected() throws Exception {
		ECKey impostor = new ECKeyGenerator(Curve.P_256).keyID(idpKey.getKeyID()).generate();
		assertRejected(sign(new JOSEObjectType("at+jwt"), impostor, claims -> {
		}));
	}

	@Test
	public void a_token_from_another_issuer_is_rejected_without_contacting_the_idp() throws Exception {
		assertRejected(atJwt(claims -> claims.issuer("https://somewhere-else.example.com")));

		Assertions.assertEquals(0, idpRequests.get());
	}

	@Test
	public void an_unreachable_idp_gives_a_401() throws Exception {
		String token = atJwt(claims -> {
		});
		server.stop(0);
		stopped = true;

		assertRejected(token);
	}
}
