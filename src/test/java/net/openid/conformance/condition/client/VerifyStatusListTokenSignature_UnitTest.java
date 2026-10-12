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
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VerifyStatusListTokenSignature_UnitTest {

	/** An EC signing key whose x5c holds a certificate for it (issued by a test root, not self-signed). */
	private static final String JWK_WITH_X5C = """
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

	private VerifyStatusListTokenSignature cond;

	@BeforeEach
	public void setUp() {
		cond = new VerifyStatusListTokenSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_skipsWhenNoStatusListToken() {
		cond.execute(env);
		assertFalse(env.containsObject("status_list_token"));
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingEmbeddedJwk() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()).jwk(signingKey.toPublicJWK())));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingEmbeddedJwkWithoutKidWhenHeaderHasKid() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		JWK jwkWithoutKid = JWK.parse(signingKey.toPublicJWK().toJSONObject().entrySet().stream()
			.filter(e -> !e.getKey().equals("kid"))
			.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("header-kid").jwk(jwkWithoutKid)));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsEmbeddedJwkThatDoesNotVerify() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		ECKey otherKey = generateKey("status-list-key");
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()).jwk(otherKey.toPublicJWK())));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingX5cEndEntityCertificate() throws Exception {
		ECKey signingKey = ECKey.parse(JWK_WITH_X5C);
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()).x509CertChain(signingKey.getX509CertChain())));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsX5cCertificateThatDoesNotVerify() throws Exception {
		ECKey certificateKey = ECKey.parse(JWK_WITH_X5C);
		ECKey signingKey = generateKey(certificateKey.getKeyID());
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()).x509CertChain(certificateKey.getX509CertChain())));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingDidJwkKid() throws Exception {
		ECKey signingKey = generateKey(null);
		String didJwk = "did:jwk:" + Base64URL.encode(signingKey.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8)) + "#0";
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(didJwk)));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsDidJwkKidThatDoesNotVerify() throws Exception {
		ECKey signingKey = generateKey(null);
		ECKey otherKey = generateKey(null);
		String didJwk = "did:jwk:" + Base64URL.encode(otherKey.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8));
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(didJwk)));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsDidJwkKidThatIsNotAJwk() throws Exception {
		ECKey signingKey = generateKey(null);
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("did:jwk:bm90LWEtandr")));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("did:jwk");
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingKeyEmbeddedInReferencedCredential() throws Exception {
		ECKey signingKey = generateKey("issuer-key");
		putCredentialHeader(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("issuer-key").jwk(signingKey.toPublicJWK()).build());
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("issuer-key")));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingX5cEmbeddedInReferencedCredential() throws Exception {
		ECKey signingKey = ECKey.parse(JWK_WITH_X5C);
		putCredentialHeader(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()).x509CertChain(signingKey.getX509CertChain()).build());
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID())));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_ignoresReferencedCredentialKeyWhenKidDiffers() throws Exception {
		ECKey signingKey = generateKey("issuer-key");
		putCredentialHeader(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("issuer-key").jwk(signingKey.toPublicJWK()).build());
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("status-list-key")));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("no verification key could be resolved");
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingJwtVcIssuerMetadataKeys() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		env.putObject("status_list_token_issuer_jwks", publicJwks(signingKey));
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID())));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_prefersJwtVcIssuerMetadataKeysOverServerJwks() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		ECKey serverKey = generateKey("status-list-key");
		env.putObject("status_list_token_issuer_jwks", publicJwks(signingKey));
		env.putObject("server_jwks", publicJwks(serverKey));
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID())));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingServerJwks() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		env.putObject("server_jwks", publicJwks(signingKey));
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID())));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsInvalidSignatureAgainstServerJwks() throws Exception {
		ECKey trustedKey = generateKey("status-list-key");
		ECKey attackerKey = generateKey("status-list-key");
		env.putObject("server_jwks", publicJwks(trustedKey));
		putStatusListToken(createSignedStatusListToken(attackerKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(attackerKey.getKeyID())));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsWhenNoKeySourceIsAvailable() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		putStatusListToken(createSignedStatusListToken(signingKey,
			new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID())));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("no verification key could be resolved");
	}

	private ECKey generateKey(String kid) throws Exception {
		return new ECKeyGenerator(Curve.P_256)
			.keyID(kid)
			.algorithm(JWSAlgorithm.ES256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();
	}

	private JsonObject publicJwks(ECKey key) {
		return JsonParser.parseString(new JWKSet(key.toPublicJWK()).toString()).getAsJsonObject();
	}

	private void putStatusListToken(String jwt) throws Exception {
		env.putObject("status_list_token", JWTUtil.jwtStringToJsonObjectForEnvironment(jwt));
	}

	private void putCredentialHeader(JWSHeader credentialHeader) {
		JsonObject credential = new JsonObject();
		credential.add("header", JsonParser.parseString(credentialHeader.toString()).getAsJsonObject());
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
	}

	private String createSignedStatusListToken(ECKey signingKey, JWSHeader.Builder headerBuilder) throws Exception {
		TokenStatusList tokenStatusList = TokenStatusList.create(
			new byte[] { (byte) TokenStatusList.Status.VALID.getTypeValue() }, 1);
		Instant now = Instant.now();

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.subject("https://issuer.example.com/statuslists/1")
			.issueTime(java.util.Date.from(now))
			.expirationTime(java.util.Date.from(now.plusSeconds(300)))
			.claim("status_list", Map.of("bits", 1L, "lst", tokenStatusList.encodeStatusList()))
			.build();

		SignedJWT jwt = new SignedJWT(headerBuilder.build(), claimsSet);
		jwt.sign(new ECDSASigner(signingKey.toECPrivateKey()));
		return jwt.serialize();
	}
}
