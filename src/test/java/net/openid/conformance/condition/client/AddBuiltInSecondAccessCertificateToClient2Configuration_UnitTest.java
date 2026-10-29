package net.openid.conformance.condition.client;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AddBuiltInSecondAccessCertificateToClient2Configuration_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private AddBuiltInSecondAccessCertificateToClient2Configuration cond;

	@BeforeEach
	public void setUp() {
		cond = new AddBuiltInSecondAccessCertificateToClient2Configuration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void fillsInTheSecondBuiltInAccessCertificateWhenTheConfigurationHasNoSecondClient() throws Exception {
		env.putObject("config", new JsonObject());

		cond.execute(env);

		JWK jwk = onlyKey(env.getElementFromObject("config", "client2.jwks").getAsJsonObject());
		assertThat(jwk.isPrivate()).isTrue();
		assertThat(jwk.getX509CertChain().get(0).decode())
			.isEqualTo(bytes(TestKeysAndCerts.getSecondWrpac().getCert().getEncoded()));
	}

	@Test
	public void fillsInAJwksClearedToNull() {
		putClient2Jwks(JsonNull.INSTANCE);

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client2.jwks").isJsonObject()).isTrue();
	}

	@Test
	public void fillsInAJwksClearedToAnEmptyString() {
		putClient2Jwks(new JsonPrimitive(""));

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client2.jwks").isJsonObject()).isTrue();
	}

	private void putClient2Jwks(com.google.gson.JsonElement jwks) {
		JsonObject client2 = new JsonObject();
		client2.add("jwks", jwks);
		JsonObject config = new JsonObject();
		config.add("client2", client2);
		env.putObject("config", config);
	}

	@Test
	public void rejectsASecondClientSectionThatIsNotAnObject() {
		JsonObject config = new JsonObject();
		config.add("client2", new JsonPrimitive("{\"keys\":[]}"));
		env.putObject("config", config);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Second client' section");
	}

	@Test
	public void leavesAConfiguredJwksAlone() {
		JsonObject config = JsonParser.parseString("{\"client2\":{\"jwks\":{\"keys\":[{\"kty\":\"oct\",\"k\":\"abc\"}]}}}").getAsJsonObject();
		env.putObject("config", config);

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client2.jwks").toString())
			.isEqualTo("{\"keys\":[{\"kty\":\"oct\",\"k\":\"abc\"}]}");
	}

	@Test
	public void theTwoBuiltInSignersHaveDistinctKeysAndX509HashClientIdentifiers() throws Exception {
		env.putObject("config", new JsonObject());
		AddBuiltInAccessCertificateToClientConfiguration first = new AddBuiltInAccessCertificateToClientConfiguration();
		first.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		first.execute(env);

		cond.execute(env);

		JWK firstKey = onlyKey(env.getElementFromObject("config", "client.jwks").getAsJsonObject());
		JWK secondKey = onlyKey(env.getElementFromObject("config", "client2.jwks").getAsJsonObject());
		assertThat(secondKey.getKeyID()).isNotEqualTo(firstKey.getKeyID());
		assertThat(secondKey.toPublicJWK()).isNotEqualTo(firstKey.toPublicJWK());

		X509Certificate firstCert = X509CertUtils.parse(firstKey.getX509CertChain().get(0).decode());
		X509Certificate secondCert = X509CertUtils.parse(secondKey.getX509CertChain().get(0).decode());
		assertThat(X509CertUtils.computeSHA256Thumbprint(secondCert))
			.isNotEqualTo(X509CertUtils.computeSHA256Thumbprint(firstCert));
		assertThat(secondCert.getSubjectX500Principal()).isNotEqualTo(firstCert.getSubjectX500Principal());
		assertThat(secondCert.getIssuerX500Principal()).isEqualTo(firstCert.getIssuerX500Principal());
	}

	private static JWK onlyKey(JsonObject jwks) throws Exception {
		assertThat(jwks.getAsJsonArray("keys")).hasSize(1);
		return JWK.parse(jwks.getAsJsonArray("keys").get(0).getAsJsonObject().toString());
	}

	private static byte[] bytes(kotlinx.io.bytestring.ByteString byteString) {
		return byteString.toByteArray(0, byteString.getSize());
	}
}
