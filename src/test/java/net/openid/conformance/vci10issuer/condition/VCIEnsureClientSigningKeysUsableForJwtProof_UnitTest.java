package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureClientSigningKeysUsableForJwtProof_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureClientSigningKeysUsableForJwtProof cond;

	private JWK p256Key;
	private JWK p384Key;
	private JWK rsaKey;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new VCIEnsureClientSigningKeysUsableForJwtProof();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		p256Key = new ECKeyGenerator(Curve.P_256).keyID("p256").algorithm(JWSAlgorithm.ES256).keyUse(KeyUse.SIGNATURE).generate();
		p384Key = new ECKeyGenerator(Curve.P_384).keyID("p384").algorithm(JWSAlgorithm.ES384).keyUse(KeyUse.SIGNATURE).generate();
		rsaKey = new RSAKeyGenerator(2048).keyID("fapi-jwt-assertion-20180817-2").algorithm(JWSAlgorithm.PS256).generate();

		env.putBoolean("vci_requires_cryptographic_binding", true);
		env.putString("vci_proof_type_key", "jwt");
	}

	@Test
	public void testEvaluate_acceptsP256SigningKey() {
		putClientJwks(p256Key);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsRsaSigningKey() {
		putClientJwks(rsaKey);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("EC P-256").contains("client2");
	}

	@Test
	public void testEvaluate_rejectsRsaKeyNextToP256Key() {
		putClientJwks(p256Key, rsaKey);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsOtherCurve() {
		putClientJwks(p384Key);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_ignoresEncryptionKeys() throws Exception {
		JWK rsaEncryptionKey = new RSAKeyGenerator(2048).keyID("enc").keyUse(KeyUse.ENCRYPTION).generate();
		putClientJwks(p256Key, rsaEncryptionKey);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsJwksWithoutSigningKeys() throws Exception {
		JWK rsaEncryptionKey = new RSAKeyGenerator(2048).keyID("enc").keyUse(KeyUse.ENCRYPTION).generate();
		putClientJwks(rsaEncryptionKey);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_notApplicableWithoutCryptographicBinding() {
		env.putBoolean("vci_requires_cryptographic_binding", false);
		putClientJwks(rsaKey);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_notApplicableForOtherProofTypes() {
		env.putString("vci_proof_type_key", "attestation");
		putClientJwks(rsaKey);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rejectsMissingJwks() {
		env.putObject("client", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putClientJwks(JWK... keys) {
		JsonObject client = new JsonObject();
		client.add("jwks", JsonParser.parseString(new JWKSet(java.util.List.of(keys)).toString(false)).getAsJsonObject());
		env.putObject("client", client);
	}
}
