package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDHEncrypter;
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
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIDecryptCredentialRequest_UnitTest {

	private VCIDecryptCredentialRequest cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private ECKey encryptionKey;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new VCIDecryptCredentialRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();

		encryptionKey = new ECKeyGenerator(Curve.P_256)
			.algorithm(JWEAlgorithm.ECDH_ES)
			.keyUse(KeyUse.ENCRYPTION)
			.keyID(UUID.randomUUID().toString())
			.generate();

		JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(new JWKSet(encryptionKey));
		env.putObject("vci", "credential_request_encryption_jwks", privateJwks);
	}

	@Test
	public void testEvaluate_decryptsValidJweAndPopulatesBodyJson() throws Exception {
		String plaintext = "{\"credential_configuration_id\":\"UniversityDegreeCredential\"}";
		String jwe = encrypt(plaintext);

		putIncomingRequest("application/jwt", jwe);

		assertDoesNotThrow(() -> cond.execute(env));

		assertEquals(plaintext, env.getString("incoming_request", "body"));

		JsonObject bodyJson = (JsonObject) env.getElementFromObject("incoming_request", "body_json");
		assertNotNull(bodyJson);
		assertEquals("UniversityDegreeCredential",
			OIDFJSON.getString(bodyJson.get("credential_configuration_id")));
	}

	@Test
	public void testEvaluate_selectsMatchingKidFromMultiKeyJwks() throws Exception {
		ECKey otherKey = new ECKeyGenerator(Curve.P_256)
			.algorithm(JWEAlgorithm.ECDH_ES)
			.keyUse(KeyUse.ENCRYPTION)
			.keyID(UUID.randomUUID().toString())
			.generate();
		JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(new JWKSet(List.of(otherKey, encryptionKey)));
		env.putObject("vci", "credential_request_encryption_jwks", privateJwks);

		String plaintext = "{\"credential_configuration_id\":\"UniversityDegreeCredential\"}";
		String jwe = encrypt(plaintext);

		putIncomingRequest("application/jwt", jwe);

		assertDoesNotThrow(() -> cond.execute(env));
		assertEquals(plaintext, env.getString("incoming_request", "body"));
	}

	@Test
	public void testEvaluate_failsOnEmptyBody() {
		putIncomingRequest("application/jwt", "");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsOnMalformedJwe() {
		putIncomingRequest("application/jwt", "not-a-jwe");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenDecryptedPayloadIsNotJson() throws Exception {
		String jwe = encrypt("this is not JSON");
		putIncomingRequest("application/jwt", jwe);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private String encrypt(String plaintext) throws Exception {
		JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES, EncryptionMethod.A256GCM)
			.contentType("json")
			.keyID(encryptionKey.getKeyID())
			.build();

		JWEObject jweObject = new JWEObject(header, new Payload(plaintext));
		jweObject.encrypt(new ECDHEncrypter(encryptionKey.toPublicJWK()));
		return jweObject.serialize();
	}

	private void putIncomingRequest(String contentType, String body) {
		JsonObject headers = new JsonObject();
		if (contentType != null) {
			headers.addProperty("content-type", contentType);
		}

		JsonObject incomingRequest = new JsonObject();
		incomingRequest.add("headers", headers);
		incomingRequest.addProperty("body", body);
		env.putObject("incoming_request", incomingRequest);
	}
}
