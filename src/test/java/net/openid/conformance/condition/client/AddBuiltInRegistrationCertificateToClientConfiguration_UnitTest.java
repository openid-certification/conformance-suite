package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AddBuiltInRegistrationCertificateToClientConfiguration_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private AddBuiltInRegistrationCertificateToClientConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new AddBuiltInRegistrationCertificateToClientConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void addsTheRegistrationCertificateWhenRequestsAreSignedWithTheBuiltInAccessCertificate() {
		JsonObject client = new JsonObject();
		client.add("jwks", TestKeysAndCerts.getWrpac().jwksJson());
		env.putObject("config", config(client));

		cond.execute(env);

		JsonArray verifierInfo = env.getElementFromObject("config", "client.verifier_info").getAsJsonArray();
		assertThat(verifierInfo).hasSize(2);
		assertThat(OIDFJSON.getString(verifierInfo.get(1).getAsJsonObject().get("format"))).isEqualTo("registration_cert");
	}

	@Test
	public void leavesAConfiguredVerifierInfoAlone() {
		JsonObject client = new JsonObject();
		client.add("jwks", TestKeysAndCerts.getWrpac().jwksJson());
		JsonArray configured = JsonParser.parseString("[{\"format\":\"registration_cert\",\"data\":\"x\"}]").getAsJsonArray();
		client.add("verifier_info", configured);
		env.putObject("config", config(client));

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client.verifier_info")).isEqualTo(configured);
	}

	@Test
	public void addsNothingWhenTheRequestsAreSignedWithAnotherOrganizationsCertificate() {
		JsonObject client = new JsonObject();
		client.add("jwks", JsonParser.parseString("{\"keys\":[{\"kty\":\"EC\",\"crv\":\"P-256\","
			+ "\"x\":\"f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU\",\"y\":\"x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0\"}]}"));
		env.putObject("config", config(client));

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client.verifier_info")).isNull();
	}

	@Test
	public void addsNothingWithoutAClientSection() {
		env.putObject("config", new JsonObject());

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client")).isNull();
	}

	private static JsonObject config(JsonObject client) {
		JsonObject config = new JsonObject();
		config.add("client", client);
		return config;
	}
}
