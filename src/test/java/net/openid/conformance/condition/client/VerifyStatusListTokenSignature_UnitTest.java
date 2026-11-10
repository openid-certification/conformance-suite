package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
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

import static net.openid.conformance.condition.client.ResolveStatusListTokenKeyFromJwtOrCredential_UnitTest.JWK_WITH_X5C;
import static net.openid.conformance.condition.client.ResolveStatusListTokenKeyFromJwtOrCredential_UnitTest.createSignedStatusListToken;
import static net.openid.conformance.condition.client.ResolveStatusListTokenKeyFromJwtOrCredential_UnitTest.didJwk;
import static net.openid.conformance.condition.client.ResolveStatusListTokenKeyFromJwtOrCredential_UnitTest.generateKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VerifyStatusListTokenSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ResolveStatusListTokenKeyFromJwtOrCredential resolve;

	private VerifyStatusListTokenSignature cond;

	@BeforeEach
	public void setUp() {
		resolve = new ResolveStatusListTokenKeyFromJwtOrCredential();
		resolve.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		cond = new VerifyStatusListTokenSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_skipsWhenNoStatusListToken() {
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingHeaderJwk() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(signingKey.getKeyID()).jwk(signingKey.toPublicJWK()));
		resolve.execute(env);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsHeaderJwkThatDoesNotVerify() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		ECKey otherKey = generateKey("status-list-key");
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(signingKey.getKeyID()).jwk(otherKey.toPublicJWK()));
		resolve.execute(env);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingX5cEndEntityCertificate() throws Exception {
		ECKey signingKey = ECKey.parse(JWK_WITH_X5C);
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(signingKey.getKeyID()).x509CertChain(signingKey.getX509CertChain()));
		resolve.execute(env);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsX5cCertificateThatDoesNotVerify() throws Exception {
		ECKey certificateKey = ECKey.parse(JWK_WITH_X5C);
		ECKey signingKey = generateKey(certificateKey.getKeyID());
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(signingKey.getKeyID()).x509CertChain(certificateKey.getX509CertChain()));
		resolve.execute(env);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingDidJwkKid() throws Exception {
		ECKey signingKey = generateKey(null);
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(didJwk(signingKey) + "#0"));
		resolve.execute(env);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsDidJwkKidThatDoesNotVerify() throws Exception {
		ECKey signingKey = generateKey(null);
		ECKey otherKey = generateKey(null);
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(didJwk(otherKey)));
		resolve.execute(env);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingKeyEmbeddedInReferencedCredential() throws Exception {
		ECKey signingKey = generateKey("issuer-key");
		putCredentialHeader(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("issuer-key").jwk(signingKey.toPublicJWK()).build());
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("issuer-key"));
		resolve.execute(env);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingJwtVcIssuerMetadataKeys() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		env.putObject("jwt_vc_issuer_metadata_jwks", publicJwks(signingKey));
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_prefersResolvedKeyOverJwtVcIssuerMetadataKeys() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		ECKey metadataKey = generateKey("status-list-key");
		env.putObject("jwt_vc_issuer_metadata_jwks", publicJwks(metadataKey));
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256)
			.keyID(signingKey.getKeyID()).jwk(signingKey.toPublicJWK()));
		JsonObject resolvedKey = new JsonObject();
		resolvedKey.add("jwks", publicJwks(signingKey));
		resolvedKey.addProperty("source", "JWT header jwk");
		env.putObject("status_list_token_verification_key", resolvedKey);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_prefersJwtVcIssuerMetadataKeysOverServerJwks() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		ECKey serverKey = generateKey("status-list-key");
		env.putObject("jwt_vc_issuer_metadata_jwks", publicJwks(signingKey));
		env.putObject("status_list_token_server_jwks", publicJwks(serverKey));
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_verifiesSignatureUsingServerJwks() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		env.putObject("status_list_token_server_jwks", publicJwks(signingKey));
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsInvalidSignatureAgainstServerJwks() throws Exception {
		ECKey trustedKey = generateKey("status-list-key");
		ECKey attackerKey = generateKey("status-list-key");
		env.putObject("status_list_token_server_jwks", publicJwks(trustedKey));
		putStatusListToken(attackerKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(attackerKey.getKeyID()));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_ignoresAuthorizationServerJwksOutsideTheStatusListResolution() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		env.putObject("server_jwks", publicJwks(signingKey));
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signingKey.getKeyID()));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("no verification key could be resolved");
	}

	@Test
	public void testEvaluate_rejectsWhenNoKeySourceIsAvailable() throws Exception {
		ECKey signingKey = generateKey("status-list-key");
		putCredentialHeader(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("issuer-key").jwk(signingKey.toPublicJWK()).build());
		putStatusListToken(signingKey, new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("status-list-key"));
		resolve.execute(env);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("no verification key could be resolved");
	}

	private JsonObject publicJwks(ECKey key) {
		return JsonParser.parseString(new JWKSet(key.toPublicJWK()).toString()).getAsJsonObject();
	}

	private void putStatusListToken(ECKey signingKey, JWSHeader.Builder header) throws Exception {
		env.putObject("status_list_token", JWTUtil.jwtStringToJsonObjectForEnvironment(
			createSignedStatusListToken(signingKey, header, null)));
	}

	private void putCredentialHeader(JWSHeader credentialHeader) {
		JsonObject credential = new JsonObject();
		credential.add("header", JsonParser.parseString(credentialHeader.toString()).getAsJsonObject());
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
	}
}
