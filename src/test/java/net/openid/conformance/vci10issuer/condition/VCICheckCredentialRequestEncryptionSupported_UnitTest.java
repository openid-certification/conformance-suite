package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCICheckCredentialRequestEncryptionSupported_UnitTest {

	private VCICheckCredentialRequestEncryptionSupported cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCICheckCredentialRequestEncryptionSupported();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void testEvaluate_acceptsMetadataWithUsableEncryptionKey() throws Exception {
		ECKey encryptionKey = new ECKeyGenerator(Curve.P_256)
			.algorithm(JWEAlgorithm.ECDH_ES)
			.keyUse(KeyUse.ENCRYPTION)
			.keyID(UUID.randomUUID().toString())
			.generate();

		putRequestEncryptionMetadata(JWKUtil.getPublicJwksAsJsonObject(new JWKSet(encryptionKey)));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsEmptyJwksKeysArray() {
		JsonObject jwks = new JsonObject();
		jwks.add("keys", new com.google.gson.JsonArray());

		putRequestEncryptionMetadata(jwks);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsJwksWithoutUsableEncryptionKey() throws Exception {
		ECKey encryptionKey = new ECKeyGenerator(Curve.P_256)
			.algorithm(JWEAlgorithm.ECDH_ES)
			.keyUse(KeyUse.ENCRYPTION)
			.keyID(UUID.randomUUID().toString())
			.generate();

		JsonObject jwks = JWKUtil.getPublicJwksAsJsonObject(new JWKSet(encryptionKey));
		JsonObject key = jwks.getAsJsonArray("keys").get(0).getAsJsonObject();
		key.addProperty("use", "sig");

		putRequestEncryptionMetadata(jwks);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putRequestEncryptionMetadata(JsonObject jwks) {
		JsonObject requestEncryption = new JsonObject();
		requestEncryption.add("jwks", jwks);

		com.google.gson.JsonArray encValuesSupported = new com.google.gson.JsonArray();
		encValuesSupported.add("A256GCM");
		requestEncryption.add("enc_values_supported", encValuesSupported);
		requestEncryption.addProperty("encryption_required", false);

		env.putObject("vci", "credential_issuer_metadata.credential_request_encryption", requestEncryption);
	}
}
