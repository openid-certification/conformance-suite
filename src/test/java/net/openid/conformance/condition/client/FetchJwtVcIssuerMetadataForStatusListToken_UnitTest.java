package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.notFound;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(HoverflyExtension.class)
@ExtendWith(MockitoExtension.class)
public class FetchJwtVcIssuerMetadataForStatusListToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FetchJwtVcIssuerMetadataForStatusListToken cond;

	private ECKey signingKey;

	private JsonObject jwks;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) throws Exception {
		signingKey = new ECKeyGenerator(Curve.P_256).keyID("status-list-key").algorithm(JWSAlgorithm.ES256).generate();
		jwks = JsonParser.parseString(new JWKSet(signingKey.toPublicJWK()).toString()).getAsJsonObject();

		JsonObject inlineMetadata = new JsonObject();
		inlineMetadata.addProperty("issuer", "https://inline.example.com/issuer");
		inlineMetadata.add("jwks", jwks);

		JsonObject uriMetadata = new JsonObject();
		uriMetadata.addProperty("issuer", "https://byuri.example.com");
		uriMetadata.addProperty("jwks_uri", "https://byuri.example.com/keys.json");

		JsonObject mismatchMetadata = new JsonObject();
		mismatchMetadata.addProperty("issuer", "https://somebody-else.example.com");
		mismatchMetadata.add("jwks", jwks);

		JsonObject noKeysMetadata = new JsonObject();
		noKeysMetadata.addProperty("issuer", "https://nokeys.example.com");

		hoverfly.simulate(dsl(
			service("inline.example.com")
				.get("/.well-known/jwt-vc-issuer/issuer")
				.willReturn(success(inlineMetadata.toString(), "application/json")),
			service("byuri.example.com")
				.get("/.well-known/jwt-vc-issuer")
				.willReturn(success(uriMetadata.toString(), "application/json"))
				.get("/keys.json")
				.willReturn(success(jwks.toString(), "application/json")),
			service("mismatch.example.com")
				.get("/.well-known/jwt-vc-issuer")
				.willReturn(success(mismatchMetadata.toString(), "application/json")),
			service("nokeys.example.com")
				.get("/.well-known/jwt-vc-issuer")
				.willReturn(success(noKeysMetadata.toString(), "application/json")),
			service("notjson.example.com")
				.get("/.well-known/jwt-vc-issuer")
				.willReturn(success("This is not JSON!", "text/plain")),
			service("unpublished.example.com")
				.get("/.well-known/jwt-vc-issuer")
				.willReturn(notFound())));
		hoverfly.resetJournal();

		cond = new FetchJwtVcIssuerMetadataForStatusListToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_skipsWhenNoStatusListToken() {
		cond.execute(env);
		assertFalse(env.containsObject("status_list_token_issuer_jwks"));
	}

	@Test
	public void testEvaluate_resolvesInlineJwksUnderIssPath(Hoverfly hoverfly) throws Exception {
		putStatusListToken("https://inline.example.com/issuer", null);

		cond.execute(env);

		hoverfly.verify(service("inline.example.com").get("/.well-known/jwt-vc-issuer/issuer"));
		assertThat(env.getObject("status_list_token_issuer_jwks")).isEqualTo(jwks);
	}

	@Test
	public void testEvaluate_resolvesJwksUri(Hoverfly hoverfly) throws Exception {
		putStatusListToken("https://byuri.example.com", null);

		cond.execute(env);

		hoverfly.verify(service("byuri.example.com").get("/keys.json"));
		assertThat(env.getObject("status_list_token_issuer_jwks")).isEqualTo(jwks);
	}

	@Test
	public void testEvaluate_fallsBackToCredentialIssWhenTokenHasNone(Hoverfly hoverfly) throws Exception {
		putStatusListToken(null, null);
		putCredentialIss("https://byuri.example.com");

		cond.execute(env);

		hoverfly.verify(service("byuri.example.com").get("/.well-known/jwt-vc-issuer"));
		assertThat(env.getObject("status_list_token_issuer_jwks")).isEqualTo(jwks);
	}

	@Test
	public void testEvaluate_doesNothingWhenMetadataIsNotPublished(Hoverfly hoverfly) throws Exception {
		putStatusListToken("https://unpublished.example.com", null);

		cond.execute(env);

		hoverfly.verify(service("unpublished.example.com").get("/.well-known/jwt-vc-issuer"));
		assertFalse(env.containsObject("status_list_token_issuer_jwks"));
	}

	@Test
	public void testEvaluate_doesNothingWhenIssIsNotHttps(Hoverfly hoverfly) throws Exception {
		putStatusListToken("did:web:inline.example.com", null);

		cond.execute(env);

		hoverfly.verifyZeroRequestTo(service("inline.example.com"));
		assertFalse(env.containsObject("status_list_token_issuer_jwks"));
	}

	@Test
	public void testEvaluate_doesNothingWhenNoIssAtAll(Hoverfly hoverfly) throws Exception {
		putStatusListToken(null, null);

		cond.execute(env);

		assertFalse(env.containsObject("status_list_token_issuer_jwks"));
	}

	@Test
	public void testEvaluate_doesNotFetchWhenTokenEmbedsItsKey(Hoverfly hoverfly) throws Exception {
		putStatusListToken("https://inline.example.com/issuer", signingKey.toPublicJWK());

		cond.execute(env);

		hoverfly.verifyZeroRequestTo(service("inline.example.com"));
		assertFalse(env.containsObject("status_list_token_issuer_jwks"));
	}

	@Test
	public void testEvaluate_doesNotFetchWhenKidIsDidJwk(Hoverfly hoverfly) throws Exception {
		putStatusListToken("https://inline.example.com/issuer", null, "did:jwk:eyJrdHkiOiJFQyJ9#0");

		cond.execute(env);

		hoverfly.verifyZeroRequestTo(service("inline.example.com"));
		assertFalse(env.containsObject("status_list_token_issuer_jwks"));
	}

	@Test
	public void testEvaluate_rejectsIssuerMismatch() throws Exception {
		putStatusListToken("https://mismatch.example.com", null);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("not identical to the iss claim");
	}

	@Test
	public void testEvaluate_rejectsMetadataWithoutKeys() throws Exception {
		putStatusListToken("https://nokeys.example.com", null);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("neither 'jwks' nor 'jwks_uri'");
	}

	@Test
	public void testEvaluate_rejectsMetadataThatIsNotJson() throws Exception {
		putStatusListToken("https://notjson.example.com", null);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testJwtVcIssuerMetadataUrl() {
		assertThat(FetchJwtVcIssuerMetadataForStatusListToken.jwtVcIssuerMetadataUrl("https://issuer.example.com"))
			.isEqualTo("https://issuer.example.com/.well-known/jwt-vc-issuer");
		assertThat(FetchJwtVcIssuerMetadataForStatusListToken.jwtVcIssuerMetadataUrl("https://issuer.example.com:8443/tenant/a/"))
			.isEqualTo("https://issuer.example.com:8443/.well-known/jwt-vc-issuer/tenant/a");
		assertThat(FetchJwtVcIssuerMetadataForStatusListToken.jwtVcIssuerMetadataUrl("http://issuer.example.com")).isNull();
		assertThat(FetchJwtVcIssuerMetadataForStatusListToken.jwtVcIssuerMetadataUrl("https://issuer.example.com/?x=1")).isNull();
		assertThat(FetchJwtVcIssuerMetadataForStatusListToken.jwtVcIssuerMetadataUrl("https://issuer.example.com/#frag")).isNull();
		assertThat(FetchJwtVcIssuerMetadataForStatusListToken.jwtVcIssuerMetadataUrl("did:jwk:abc")).isNull();
		assertThat(FetchJwtVcIssuerMetadataForStatusListToken.jwtVcIssuerMetadataUrl("not a url")).isNull();
	}

	private void putCredentialIss(String iss) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iss", iss);
		JsonObject credential = new JsonObject();
		credential.add("claims", claims);
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
	}

	private void putStatusListToken(String iss, com.nimbusds.jose.jwk.JWK embeddedJwk) throws Exception {
		putStatusListToken(iss, embeddedJwk, signingKey.getKeyID());
	}

	private void putStatusListToken(String iss, com.nimbusds.jose.jwk.JWK embeddedJwk, String kid) throws Exception {
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
			.subject("https://issuer.example.com/statuslists/1");
		if (iss != null) {
			claims.issuer(iss);
		}
		JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(kid);
		if (embeddedJwk != null) {
			header.jwk(embeddedJwk);
		}
		SignedJWT jwt = new SignedJWT(header.build(), claims.build());
		jwt.sign(new ECDSASigner(signingKey.toECPrivateKey()));
		env.putObject("status_list_token", JWTUtil.jwtStringToJsonObjectForEnvironment(jwt.serialize()));
	}
}
