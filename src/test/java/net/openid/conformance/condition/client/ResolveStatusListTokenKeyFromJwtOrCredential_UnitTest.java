package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ResolveStatusListTokenKeyFromJwtOrCredential_UnitTest {

	/** An EC signing key whose x5c holds a certificate for it (issued by a test root, not self-signed). */
	static final String JWK_WITH_X5C = """
		{
			"kty": "EC",
			"crv": "P-256",
			"alg": "ES256",
			"use": "sig",
			"x": "p-_IHEO9b_XZIyW2SHyYrRyMndwcWjGhnhS-yF6HRiY",
			"y": "_6IzIjawRYvQLdrypBlCqeBh27jR2tLNUq8h86deoe8",
			"d": "Gkmh-vjcuC8QStQqLqM_PhJQUp8KepSGGL2-stl79Bs",
			"kid": "status-list-x5c-key",
			"x5c": [
				"MIICTDCCAdKgAwIBAgIUPlAaWKujE4TvY8sCwXmyDMGgOIwwCgYIKoZIzj0EAwIwLDEqMCgGA1UEAwwhT3BlbklENFZDSSBDb25mb3JtYW5jZSBUZXN0cyBSb290MB4XDTI2MDExNTE2NTQyNFoXDTI4MDQxOTE2NTQyNFowUTELMAkGA1UEBhMCREUxFzAVBgNVBAoMDkV4YW1wbGUgSXNzdWVyMRAwDgYDVQQLDAdPSUQ0VkNJMRcwFQYDVQQDDA5pc3N1ZXIuZXhhbXBsZTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABKfvyBxDvW/12SMltkh8mK0cjJ3cHFoxoZ4Uvsheh0Ym/6IzIjawRYvQLdrypBlCqeBh27jR2tLNUq8h86deoe+jgawwgakwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCB4AwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMB0GA1UdDgQWBBSNQHXEutjrfQDfbTgLG0mHepGesjAfBgNVHSMEGDAWgBTgt/z+s54ZDXsVA/YQLaW4RI7WajAqBgNVHREEIzAhgg5pc3N1ZXIuZXhhbXBsZYIJbG9jYWxob3N0hwR/AAABMAoGCCqGSM49BAMCA2gAMGUCMQC24WF0JjXEH0MuirdaXckJuxQUR2N7m3CO2WnUvnmnvEVUfgrUB0G78SFL0LDbuHECMByQ90GH0dB94Z2/4D6f4uDm0j9m6LHTEM0XrW9JcGT2fDMfVEMgUYrMod6yHWbgSw=="
			]
		}
		""";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ResolveStatusListTokenKeyFromJwtOrCredential cond;

	@BeforeEach
	public void setUp() {
		cond = new ResolveStatusListTokenKeyFromJwtOrCredential();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_skipsWhenNoStatusListToken() {
		cond.execute(env);
		assertFalse(env.containsObject("status_list_token_verification_key"));
		assertFalse(env.containsObject("status_list_token_jwt_vc_issuer"));
	}

	@Test
	public void testEvaluate_clearsStateOfPreviousCredential() {
		env.putObject("status_list_token_verification_key", new JsonObject());
		env.putObject("status_list_token_jwt_vc_issuer", new JsonObject());
		env.putObject("jwt_vc_issuer_metadata_endpoint_response", new JsonObject());
		env.putObject("jwt_vc_issuer_metadata", new JsonObject());
		env.putString("jwt_vc_issuer_metadata_jwks_uri", "https://issuer.example.com/jwks");
		env.putObject("jwt_vc_issuer_metadata_jwks_uri_endpoint_response", new JsonObject());
		env.putObject("jwt_vc_issuer_metadata_jwks", new JsonObject());
		env.putObject("status_list_token_server_jwks", new JsonObject());

		cond.execute(env);

		assertFalse(env.containsObject("status_list_token_verification_key"));
		assertFalse(env.containsObject("status_list_token_jwt_vc_issuer"));
		assertFalse(env.containsObject("jwt_vc_issuer_metadata_endpoint_response"));
		assertFalse(env.containsObject("jwt_vc_issuer_metadata"));
		assertThat(env.getString("jwt_vc_issuer_metadata_jwks_uri")).isNull();
		assertFalse(env.containsObject("jwt_vc_issuer_metadata_jwks_uri_endpoint_response"));
		assertFalse(env.containsObject("jwt_vc_issuer_metadata_jwks"));
		assertFalse(env.containsObject("status_list_token_server_jwks"));
	}

	@Test
	public void testEvaluate_resolvesHeaderJwk() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(signingKey.getKeyID()).jwk(signingKey.toPublicJWK()), "https://issuer.example.com");

		cond.execute(env);

		assertResolvedKey(signingKey, signingKey.getKeyID(), "JWT header jwk");
		assertFalse(env.containsObject("status_list_token_jwt_vc_issuer"));
	}

	@Test
	public void testEvaluate_givesHeaderJwkWithoutKidTheHeaderKid() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		JWK jwkWithoutKid = JWK.parse(signingKey.toPublicJWK().toJSONObject().entrySet().stream()
			.filter(e -> !e.getKey().equals("kid"))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID("header-kid").jwk(jwkWithoutKid), null);

		cond.execute(env);

		assertResolvedKey(signingKey, "header-kid", "JWT header jwk");
	}

	@Test
	public void testEvaluate_resolvesX5cEndEntityCertificate() throws Exception {
		ECKey signingKey = ECKey.parse(JWK_WITH_X5C);
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(signingKey.getKeyID()).x509CertChain(signingKey.getX509CertChain()), null);

		cond.execute(env);

		assertResolvedKey(signingKey, signingKey.getKeyID(), "JWT header x5c end-entity certificate");
	}

	@Test
	public void testEvaluate_resolvesDidJwkKid() throws Exception {
		ECKey signingKey = generateKey(null);
		String didJwk = didJwk(signingKey) + "#0";
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(didJwk), null);

		cond.execute(env);

		assertResolvedKey(signingKey, didJwk, "did:jwk in JWT header kid");
	}

	@Test
	public void testEvaluate_rejectsDidJwkKidThatIsNotAJwk() throws Exception {
		ECKey signingKey = generateKey(null);
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("did:jwk:bm90LWEtandr"), null);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("did:jwk");
	}

	@Test
	public void testEvaluate_resolvesJwkEmbeddedInReferencedCredential() throws Exception {
		ECKey signingKey = generateKey("issuer-key");
		putCredential(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("issuer-key").jwk(signingKey.toPublicJWK()).build(), null);
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("issuer-key"),
			"https://issuer.example.com");

		cond.execute(env);

		assertResolvedKey(signingKey, "issuer-key", "referenced credential's embedded key");
		assertFalse(env.containsObject("status_list_token_jwt_vc_issuer"));
	}

	@Test
	public void testEvaluate_resolvesX5cEmbeddedInReferencedCredential() throws Exception {
		ECKey signingKey = ECKey.parse(JWK_WITH_X5C);
		putCredential(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID())
			.x509CertChain(signingKey.getX509CertChain()).build(), null);
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()), null);

		cond.execute(env);

		assertResolvedKey(signingKey, signingKey.getKeyID(), "referenced credential's embedded key");
	}

	@Test
	public void testEvaluate_locatesJwtVcIssuerMetadataWhenCredentialKeyHasOtherKid() throws Exception {
		ECKey signingKey = generateKey("issuer-key");
		putCredential(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("issuer-key").jwk(signingKey.toPublicJWK()).build(), null);
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("status-list-key"),
			"https://issuer.example.com/tenant");

		cond.execute(env);

		assertFalse(env.containsObject("status_list_token_verification_key"));
		assertThat(env.getString("status_list_token_jwt_vc_issuer", "iss")).isEqualTo("https://issuer.example.com/tenant");
		assertThat(env.getString("status_list_token_jwt_vc_issuer", "iss_source")).isEqualTo("status list token");
		assertThat(env.getString("status_list_token_jwt_vc_issuer", "metadata_url"))
			.isEqualTo("https://issuer.example.com/.well-known/jwt-vc-issuer/tenant");
	}

	@Test
	public void testEvaluate_locatesJwtVcIssuerMetadataFromCredentialIssWhenTokenHasNone() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		putCredential(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("issuer-key").build(), "https://issuer.example.com");
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()), null);

		cond.execute(env);

		assertThat(env.getString("status_list_token_jwt_vc_issuer", "iss_source")).isEqualTo("referenced credential");
		assertThat(env.getString("status_list_token_jwt_vc_issuer", "metadata_url"))
			.isEqualTo("https://issuer.example.com/.well-known/jwt-vc-issuer");
	}

	@Test
	public void testEvaluate_doesNotLocateMetadataWhenIssIsNotHttps() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()),
			"did:web:issuer.example.com");

		cond.execute(env);

		assertFalse(env.containsObject("status_list_token_verification_key"));
		assertFalse(env.containsObject("status_list_token_jwt_vc_issuer"));
	}

	@Test
	public void testEvaluate_doesNotLocateMetadataWithoutIss() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()), null);

		cond.execute(env);

		assertFalse(env.containsObject("status_list_token_verification_key"));
		assertFalse(env.containsObject("status_list_token_jwt_vc_issuer"));
	}

	@Test
	public void testJwtVcIssuerMetadataUrl() {
		assertThat(ResolveStatusListTokenKeyFromJwtOrCredential.jwtVcIssuerMetadataUrl("https://issuer.example.com"))
			.isEqualTo("https://issuer.example.com/.well-known/jwt-vc-issuer");
		assertThat(ResolveStatusListTokenKeyFromJwtOrCredential.jwtVcIssuerMetadataUrl("https://issuer.example.com:8443/tenant/a/"))
			.isEqualTo("https://issuer.example.com:8443/.well-known/jwt-vc-issuer/tenant/a");
		assertThat(ResolveStatusListTokenKeyFromJwtOrCredential.jwtVcIssuerMetadataUrl("http://issuer.example.com")).isNull();
		assertThat(ResolveStatusListTokenKeyFromJwtOrCredential.jwtVcIssuerMetadataUrl("https://issuer.example.com/?x=1")).isNull();
		assertThat(ResolveStatusListTokenKeyFromJwtOrCredential.jwtVcIssuerMetadataUrl("https://issuer.example.com/#frag")).isNull();
		assertThat(ResolveStatusListTokenKeyFromJwtOrCredential.jwtVcIssuerMetadataUrl("did:jwk:abc")).isNull();
		assertThat(ResolveStatusListTokenKeyFromJwtOrCredential.jwtVcIssuerMetadataUrl("not a url")).isNull();
	}

	private void assertResolvedKey(ECKey signingKey, String expectedKid, String expectedSource) throws Exception {
		assertTrue(env.containsObject("status_list_token_verification_key"));
		assertThat(env.getString("status_list_token_verification_key", "source")).isEqualTo(expectedSource);
		JWKSet jwks = JWKSet.parse(env.getElementFromObject("status_list_token_verification_key", "jwks").toString());
		assertThat(jwks.getKeys()).hasSize(1);
		JWK key = jwks.getKeys().get(0);
		assertFalse(key.isPrivate());
		assertThat(key.getKeyID()).isEqualTo(expectedKid);
		assertThat(key.toECKey().toECPublicKey()).isEqualTo(signingKey.toECPublicKey());
	}

	static ECKey generateKey(String kid) throws Exception {
		return new ECKeyGenerator(Curve.P_256)
			.keyID(kid)
			.algorithm(JWSAlgorithm.ES256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();
	}

	static String didJwk(ECKey key) {
		return "did:jwk:" + Base64URL.encode(key.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8));
	}

	private void putStatusListToken(ECKey signingKey, JWSHeader.Builder header, String iss) throws Exception {
		env.putObject("status_list_token", JWTUtil.jwtStringToJsonObjectForEnvironment(
			createSignedStatusListToken(signingKey, header, iss)));
	}

	static String createSignedStatusListToken(ECKey signingKey, JWSHeader.Builder header, String iss) throws Exception {
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().subject("https://issuer.example.com/statuslists/1");
		if (iss != null) {
			claims.issuer(iss);
		}
		SignedJWT jwt = new SignedJWT(header.build(), claims.build());
		jwt.sign(new ECDSASigner(signingKey.toECPrivateKey()));
		return jwt.serialize();
	}

	private void putCredential(JWSHeader credentialHeader, String iss) {
		JsonObject credential = new JsonObject();
		credential.add("header", JsonParser.parseString(credentialHeader.toString()).getAsJsonObject());
		JsonObject claims = new JsonObject();
		if (iss != null) {
			claims.addProperty("iss", iss);
		}
		credential.add("claims", claims);
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
	}
}
