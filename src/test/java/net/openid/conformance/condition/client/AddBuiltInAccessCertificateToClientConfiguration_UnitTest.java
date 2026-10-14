package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AddBuiltInAccessCertificateToClientConfiguration_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private AddBuiltInAccessCertificateToClientConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new AddBuiltInAccessCertificateToClientConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void fillsInTheBuiltInAccessCertificateWhenTheClientHasNoJwks() throws Exception {
		JsonObject config = JsonParser.parseString("{\"client\":{\"client_id\":\"x\"}}").getAsJsonObject();
		env.putObject("config", config);

		cond.execute(env);

		JsonObject jwks = env.getElementFromObject("config", "client.jwks").getAsJsonObject();
		assertThat(jwks.getAsJsonArray("keys")).hasSize(1);
		JWK jwk = JWK.parse(jwks.getAsJsonArray("keys").get(0).getAsJsonObject().toString());
		assertThat(jwk.isPrivate()).isTrue();
		assertThat(jwk.getX509CertChain().get(0).decode())
			.isEqualTo(bytes(TestKeysAndCerts.getWrpac().getCert().getEncoded()));
		assertThat(OIDFJSON.getString(env.getElementFromObject("config", "client.client_id"))).isEqualTo("x");
	}

	@Test
	public void mintsACertificateForTheModulesHostWhenItDiffersFromTheBaseUrl() throws Exception {
		env.putObject("config", new JsonObject());
		env.putString("base_url", "https://tunnel.example.net/test/a/alias");

		cond.execute(env);

		JsonObject jwks = env.getElementFromObject("config", "client.jwks").getAsJsonObject();
		JWK jwk = JWK.parse(jwks.getAsJsonArray("keys").get(0).getAsJsonObject().toString());
		assertThat(jwk.getParsedX509CertChain().get(0).getSubjectAlternativeNames())
			.anyMatch(san -> "tunnel.example.net".equals(san.get(1)));
		assertThat(jwk.getParsedX509CertChain().get(0).getIssuerX500Principal().getName()).contains("Access CA");
	}

	@Test
	public void anExternalUrlOverrideWinsOverTheBaseUrl() throws Exception {
		env.putObject("config", new JsonObject());
		env.putString("base_url", "https://localhost.example:8443/test/a/alias");
		env.putString("external_url_override", "https://tunnel.example.net");

		cond.execute(env);

		JWK jwk = JWK.parse(env.getElementFromObject("config", "client.jwks").getAsJsonObject().getAsJsonArray("keys").get(0).getAsJsonObject().toString());
		assertThat(jwk.getParsedX509CertChain().get(0).getSubjectAlternativeNames())
			.anyMatch(san -> "tunnel.example.net".equals(san.get(1)));
	}

	@Test
	public void createsTheClientSectionWhenTheConfigurationHasNone() {
		env.putObject("config", new JsonObject());

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client.jwks")).isNotNull();
	}

	@Test
	public void leavesAConfiguredJwksAlone() {
		JsonObject config = JsonParser.parseString("{\"client\":{\"jwks\":{\"keys\":[{\"kty\":\"oct\",\"k\":\"abc\"}]}}}").getAsJsonObject();
		env.putObject("config", config);

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client.jwks").toString())
			.isEqualTo("{\"keys\":[{\"kty\":\"oct\",\"k\":\"abc\"}]}");
	}

	private static byte[] bytes(kotlinx.io.bytestring.ByteString byteString) {
		return byteString.toByteArray(0, byteString.getSize());
	}
}
