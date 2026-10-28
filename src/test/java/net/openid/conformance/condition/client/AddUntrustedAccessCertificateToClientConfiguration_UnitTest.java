package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AddUntrustedAccessCertificateToClientConfiguration_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private AddUntrustedAccessCertificateToClientConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new AddUntrustedAccessCertificateToClientConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	@Test
	public void fillsInTheUntrustedAccessCertificateWhenTheClientHasNoJwks() throws Exception {
		JsonObject config = JsonParser.parseString("{\"client\":{\"client_id\":\"x\"}}").getAsJsonObject();
		env.putObject("config", config);

		cond.execute(env);

		JsonObject jwks = env.getElementFromObject("config", "client.jwks").getAsJsonObject();
		assertThat(jwks.getAsJsonArray("keys")).hasSize(1);
		JWK jwk = JWK.parse(jwks.getAsJsonArray("keys").get(0).getAsJsonObject().toString());
		assertThat(jwk.isPrivate()).isTrue();
		assertThat(jwk.getX509CertChain()).hasSize(1);
		assertThat(jwk.getX509CertChain().get(0).decode())
			.isEqualTo(bytes(TestKeysAndCerts.getUntrustedWrpac().getCert().getEncoded()))
			.isNotEqualTo(bytes(TestKeysAndCerts.getWrpac().getCert().getEncoded()));
		assertThat(OIDFJSON.getString(env.getElementFromObject("config", "client.client_id"))).isEqualTo("x");
	}

	@Test
	public void createsTheClientSectionWhenTheConfigurationHasNone() {
		env.putObject("config", new JsonObject());

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client.jwks")).isNotNull();
	}

	@Test
	public void replacesTheConfiguredSigningKeyWithAnUntrustedTwinKeepingOtherKeys() throws Exception {
		JsonObject signingKey = configuredSigningKey("tester-sig");
		JsonObject encryptionKey = JsonParser.parseString(
			new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.ENCRYPTION).keyID("tester-enc").generate().toJSONString()).getAsJsonObject();
		env.putObject("config", config("client", signingKey, encryptionKey));

		cond.execute(env);

		JsonArray keys = env.getElementFromObject("config", "client.jwks.keys").getAsJsonArray();
		assertThat(keys).hasSize(2);
		assertThat(keys.get(1)).isEqualTo(encryptionKey);
		assertIsUntrustedTwin(keys.get(0).getAsJsonObject(), signingKey, "tester-sig");
	}

	@Test
	public void replacesTheSecondClientsSigningKeyToo() throws Exception {
		JsonObject signingKey = configuredSigningKey("tester-sig");
		JsonObject secondSigningKey = configuredSigningKey("tester-sig-2");
		JsonObject config = config("client", signingKey);
		config.add("client2", config("client2", secondSigningKey).getAsJsonObject("client2"));
		env.putObject("config", config);

		cond.execute(env);

		assertIsUntrustedTwin(env.getElementFromObject("config", "client.jwks.keys").getAsJsonArray().get(0).getAsJsonObject(),
			signingKey, "tester-sig");
		assertIsUntrustedTwin(env.getElementFromObject("config", "client2.jwks.keys").getAsJsonArray().get(0).getAsJsonObject(),
			secondSigningKey, "tester-sig-2");
	}

	@Test
	public void failsWhenTheConfiguredSigningKeyHasNoCertificate() throws Exception {
		JsonObject signingKey = JsonParser.parseString(new ECKeyGenerator(Curve.P_256).generate().toJSONString()).getAsJsonObject();
		env.putObject("config", config("client", signingKey));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		assertThat(env.getElementFromObject("config", "client.jwks.keys").getAsJsonArray().get(0)).isEqualTo(signingKey);
	}

	/** A signing key whose certificate is the suite's built-in access certificate, standing in for a tester's. */
	private static JsonObject configuredSigningKey(String kid) {
		JsonObject key = TestKeysAndCerts.getWrpac().jwksJson().getAsJsonArray("keys").get(0).getAsJsonObject();
		key.addProperty("kid", kid);
		return key;
	}

	private static JsonObject config(String section, JsonObject... keys) {
		JsonArray keyArray = new JsonArray();
		for (JsonObject key : keys) {
			keyArray.add(key.deepCopy());
		}
		JsonObject jwks = new JsonObject();
		jwks.add("keys", keyArray);
		JsonObject client = new JsonObject();
		client.add("jwks", jwks);
		JsonObject config = new JsonObject();
		config.add(section, client);
		return config;
	}

	private static void assertIsUntrustedTwin(JsonObject twinJson, JsonObject originalJson, String kid) throws Exception {
		JWK twin = JWK.parse(twinJson.toString());
		JWK original = JWK.parse(originalJson.toString());
		assertThat(twin.isPrivate()).isTrue();
		assertThat(twin.getKeyID()).isEqualTo(kid);
		assertThat(twin.toPublicJWK()).isNotEqualTo(original.toPublicJWK());
		X509Certificate twinCert = X509CertUtils.parse(twin.getX509CertChain().get(0).decode());
		X509Certificate originalCert = X509CertUtils.parse(original.getX509CertChain().get(0).decode());
		assertThat(twinCert.getSubjectX500Principal()).isEqualTo(originalCert.getSubjectX500Principal());
		assertThat(twinCert.getIssuerX500Principal()).isEqualTo(TestKeysAndCerts.getUntrustedAccessCa().getJavaCert().getSubjectX500Principal());
		assertThat(twinCert.getPublicKey()).isEqualTo(twin.toECKey().toECPublicKey());
	}

	private static byte[] bytes(kotlinx.io.bytestring.ByteString byteString) {
		return byteString.toByteArray(0, byteString.getSize());
	}
}
