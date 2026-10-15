package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.SuiteWrprc;
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
		env.putString(LoadBuiltInDcqlQuery.RESOURCE_ENV_KEY, "dcql/built-in.json");

		cond.execute(env);

		JsonArray verifierInfo = env.getElementFromObject("config", "client.verifier_info").getAsJsonArray();
		assertThat(verifierInfo).hasSize(2);
		assertThat(OIDFJSON.getString(verifierInfo.get(1).getAsJsonObject().get("format"))).isEqualTo("registration_cert");
	}

	/** A DCQL query for the first registered credential, asking for its first registered claim, or {@code path}. */
	private static JsonObject dcqlForRegisteredCredential(JsonArray path) {
		JsonObject registered = SuiteWrprc.credentials().get(0).getAsJsonObject();
		JsonObject query = new JsonObject();
		query.addProperty("id", "q");
		query.add("format", registered.get("format"));
		query.add("meta", registered.get("meta"));
		JsonObject claim = new JsonObject();
		claim.add("path", path != null ? path : registered.getAsJsonArray("claim").get(0).getAsJsonObject().get("path"));
		JsonArray claims = new JsonArray();
		claims.add(claim);
		query.add("claims", claims);
		JsonArray credentials = new JsonArray();
		credentials.add(query);
		JsonObject dcql = new JsonObject();
		dcql.add("credentials", credentials);
		return dcql;
	}

	@Test
	public void addsTheRegistrationCertificateWhenTheConfiguredDcqlOnlyAsksForRegisteredClaims() {
		JsonObject client = new JsonObject();
		client.add("jwks", TestKeysAndCerts.getWrpac().jwksJson());
		client.add("dcql", dcqlForRegisteredCredential(null));
		env.putObject("config", config(client));

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client.verifier_info")).isNotNull();
	}

	@Test
	public void addsNothingWhenTheConfiguredDcqlAsksForAnUnregisteredClaim() {
		JsonObject client = new JsonObject();
		client.add("jwks", TestKeysAndCerts.getWrpac().jwksJson());
		client.add("dcql", dcqlForRegisteredCredential(JsonParser.parseString("[\"not_registered\"]").getAsJsonArray()));
		env.putObject("config", config(client));

		cond.execute(env);

		assertThat(env.getElementFromObject("config", "client.verifier_info")).isNull();
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
