package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreateKSAConsentRequest_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateKSAConsentRequest cond;
	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new CreateKSAConsentRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();

		JsonObject client = new JsonObject();
		client.addProperty("client_id", "client-1234");
		env.putObject("client", client);
		JsonObject server = new JsonObject();
		server.addProperty("issuer", "https://op.example/");
		env.putObject("server", server);
	}

	@Test
	public void testClaimsShape() {
		env = cond.evaluate(env);

		JsonObject req = env.getObject("account_requests_endpoint_request");
		assertThat(req, notNullValue());
		assertThat(OIDFJSON.getString(req.get("iss")), is("client-1234"));
		assertThat(OIDFJSON.getString(req.get("aud").getAsJsonArray().get(0)), is("https://op.example/"));
		assertThat(req.has("iat"), is(true));
		assertThat(req.has("nbf"), is(true));
		assertThat(req.has("exp"), is(true));
		JsonObject message = req.getAsJsonObject("message");
		assertThat(message, notNullValue());
		assertThat(message.getAsJsonObject("Data").has("Permissions"), is(true));
	}

	@Test
	public void testMissingIssuerThrows() {
		env.putObject("server", new JsonObject());

		ConditionError err = assertThrows(ConditionError.class, () -> cond.evaluate(env));
		assertThat(err.getMessage(), containsString("The OP issuer is not available"));
	}

	@Test
	public void testEmptyIssuerThrows() {
		JsonObject server = new JsonObject();
		server.addProperty("issuer", "");
		env.putObject("server", server);

		ConditionError err = assertThrows(ConditionError.class, () -> cond.evaluate(env));
		assertThat(err.getMessage(), containsString("The OP issuer is not available"));
	}
}
