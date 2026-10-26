package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckClientIdMatchesOnTokenRequestIfPresent_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckClientIdMatchesOnTokenRequestIfPresent cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckClientIdMatchesOnTokenRequestIfPresent();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject client = new JsonObject();
		client.addProperty("client_id", "client-a");
		env.putObject("client", client);
	}

	private void putRequestWithClientId(String clientId) {
		JsonObject form = new JsonObject();
		form.addProperty("request", "eyJ...");
		if (clientId != null) {
			form.addProperty("client_id", clientId);
		}
		JsonObject request = new JsonObject();
		request.add("body_form_params", form);
		env.putObject("token_endpoint_request", request);
	}

	@Test
	public void testEvaluate_matchingClientId() {
		putRequestWithClientId("client-a");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_clientIdAbsent() {
		putRequestWithClientId(null);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_clientIdEmpty() {
		putRequestWithClientId("");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_mismatchedClientId() {
		putRequestWithClientId("client-b");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
